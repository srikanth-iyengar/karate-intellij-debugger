package `in`.srikanthk.devlabs.kchopdebugger.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intuit.karate.Runner
import `in`.srikanthk.devlabs.kchopdebugger.configuration.KarateDebuggerConfiguration
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import io.ktor.util.collections.ConcurrentMap
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.*
import javax.xml.parsers.DocumentBuilderFactory


@Service(Service.Level.PROJECT)
class KarateExecutionService(val project: Project) {

    private val responsePublisher = project.messageBus.syncPublisher(DebuggerInfoResponseTopic.TOPIC)
    val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup("Karate Chop Debugger Notification")

    companion object {
        // key of filename and breakpoint line
        val BREAKPOINTS: MutableMap<String, ConcurrentSkipListSet<Int>> = ConcurrentMap();
    }

    fun executeSuite(fileName: String) {
        val isProjectBuilt = buildMavenProject();
        if (!isProjectBuilt) {
            return
        }

        val projectBasePath = project.basePath + Constants.JAVA_BASE_PATH
        val featureClasspath = fileName.substring(projectBasePath.length + 1)

        val classLoader = getDynamicClassLoader()
        val executor = getThreadPool(classLoader)

        val future = CompletableFuture.supplyAsync({
            val builder = Runner
                .path("classpath:${featureClasspath}")
                .hook(DebugHook(BREAKPOINTS, project))
                .classLoader(classLoader)
            builder.parallel(1)
        }, executor)

        future.get()
    }

    fun addBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS.computeIfAbsent(file, { ConcurrentSkipListSet() }).add(lineNumber)
    }

    fun removeBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS[file]?.remove(lineNumber)
    }


    private fun buildMavenProject(): Boolean {
        val projectPath = project.basePath;
        val mavenState = KarateDebuggerConfiguration.getInstance()?.state;

        if (mavenState?.mavenPath == null || mavenState.mavenPath.isBlank()) {
            notificationGroup.createNotification("Maven home is not set", NotificationType.ERROR).notify(project)
            return false
        }

        val command = "${mavenState?.mavenPath}/bin/${Constants.MAVEN_BUILDER_COMMAND}";
        val process =
            ProcessBuilder(*command.split(" ").toTypedArray()).directory(File(projectPath))
                .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.PIPE).start()

        Thread {
            process.errorStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    responsePublisher.appendLog(line, true)
                }
            }
        }.start()

        Thread {
            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    responsePublisher.appendLog(line, false)
                }
            }
        }.start()

        return process.waitFor() == 0
    }

    private fun getThreadPool(classloader: ClassLoader): ThreadPoolExecutor = ThreadPoolExecutor(
        4,
        8,
        60,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(100)
    ) { thread ->
        Thread(thread).apply {
            contextClassLoader = classloader
            name = "karate-chop-debugger-${System.currentTimeMillis()}"
        }
    }

    private fun getJarPath(): String {
        val pomPath = "${project.basePath}/pom.xml"
        val file = File(pomPath);
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)

        val artifactId = doc.getElementsByTagName("artifactId").item(0).textContent
        val version = doc.getElementsByTagName("version").item(0).textContent

        val testJarFileName = "$artifactId-$version-tests.jar"
        return "${project.basePath}/target/${testJarFileName}"
    }

    private fun getDynamicClassLoader(): ClassLoader {
        return URLClassLoader(arrayOf(File(getJarPath()).toURI().toURL()), Thread.currentThread().contextClassLoader)
    }

    fun isBreakpointPlaced(file: String, lineNumber: Int): Boolean {
        return BREAKPOINTS[file]?.contains(lineNumber) ?: false
    }
}