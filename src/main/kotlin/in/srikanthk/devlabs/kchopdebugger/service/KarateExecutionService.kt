package `in`.srikanthk.devlabs.kchopdebugger.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intuit.karate.Runner
import `in`.srikanthk.devlabs.kchopdebugger.topic.BreakpointUpdatedTopic
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import io.ktor.util.collections.ConcurrentMap
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.*
import javax.xml.parsers.DocumentBuilderFactory


@Service(Service.Level.PROJECT)
class KarateExecutionService(val project: Project) {

    private val responsePublisher = project.messageBus.syncPublisher(DebuggerInfoResponseTopic.TOPIC)
    val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup("Karate Chop Debugger Notification")
    val breakpointUpdatePublisher: BreakpointUpdatedTopic? =
        project.messageBus.syncPublisher(BreakpointUpdatedTopic.TOPIC)

    companion object {
        // key of filename and breakpoint line
        val BREAKPOINTS: MutableMap<String, ConcurrentSkipListSet<Int>> = ConcurrentMap();
    }

    fun executeSuite(fileName: String) {
        buildMavenProject({
            val projectBasePath = project.basePath + Constants.JAVA_BASE_PATH
            val featureClasspath = fileName.substring(projectBasePath.length + 1)


            val classLoader = getDynamicClassLoader()
            try {
                classLoader.use { cl ->
                    val executor = getThreadPool(cl)
                    val future = CompletableFuture.supplyAsync({
                        PatchKarateRunner().emptyFun()
                        val builder = Runner
                            .path("classpath:${featureClasspath}")
                            .hook(DebugHook(BREAKPOINTS, project))
                            .reportDir(File(project.basePath, "karate-report").path.toString())
                            .classLoader(cl)
                        builder.parallel(1)
                    }, executor)
                    future.get()
                    executor.shutdown()
                }
            } finally {

            }
        })
    }

    fun addBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS.computeIfAbsent(file) { ConcurrentSkipListSet() }.add(lineNumber)
        breakpointUpdatePublisher?.updatedBreakpoint()
    }

    fun removeBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS[file]?.remove(lineNumber)
        breakpointUpdatePublisher?.updatedBreakpoint()
    }


    private fun buildMavenProject(callback: Runnable): Boolean {
        val mavenProjectManager = MavenProjectsManager.getInstance(project);
        val mavenProjects = mavenProjectManager.projects;

        if (mavenProjects.isEmpty()) {
            notificationGroup.createNotification(
                "Karate chop error",
                "No maven projects detected",
                NotificationType.ERROR
            ).notify(project)
            return false
        }

        val runner = MavenRunner.getInstance(project)
        val mavenProject = mavenProjects.first()

        val pomFile = File(mavenProject.file.path)
        val parameters = MavenRunnerParameters(
            true,
            pomFile.parent,
            listOf("clean", "package", "-DskipTests=true"),
            emptyList<String>(),
            null
        )

        runner.run(parameters, null, callback)
        return true
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
        val file = File("${project.basePath}", "pom.xml")
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)

        val artifactId = doc.getElementsByTagName("artifactId").item(0).textContent
        val version = doc.getElementsByTagName("version").item(0).textContent

        val testJarFileName = "$artifactId-$version-tests.jar"

        // Correct and portable file path resolution
        val jarFile = File(File(project.basePath, "target"), testJarFileName)

        return jarFile.path.toString()
    }


    private fun getDynamicClassLoader(): URLClassLoader {
        return URLClassLoader(arrayOf(File(getJarPath()).toURI().toURL()), Thread.currentThread().contextClassLoader)
    }

    fun isBreakpointPlaced(file: String, lineNumber: Int): Boolean {
        return BREAKPOINTS[file]?.contains(lineNumber) ?: false
    }
}