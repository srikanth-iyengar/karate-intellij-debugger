package `in`.srikanthk.devlabs.kchopdebugger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import `in`.srikanthk.devlabs.kchopdebugger.agent.DebugMessageBus
import `in`.srikanthk.devlabs.kchopdebugger.agent.DebuggerState
import `in`.srikanthk.devlabs.kchopdebugger.agent.communication.DebugServer
import `in`.srikanthk.devlabs.kchopdebugger.agent.topic.DebugRequest
import `in`.srikanthk.devlabs.kchopdebugger.agent.topic.DebugResponse
import `in`.srikanthk.devlabs.kchopdebugger.configuration.KaratePropertiesState
import `in`.srikanthk.devlabs.kchopdebugger.topic.BreakpointUpdatedTopic
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoRequestTopic
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import io.ktor.util.collections.*
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentSkipListSet
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class KarateExecutionService(val project: Project) {
    val mapper = ObjectMapper()
    private val responsePublisher = project.messageBus.syncPublisher(DebuggerInfoResponseTopic.TOPIC)
    private val runPropertiesService = KaratePropertiesState.getInstance()
    val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup("Karate Chop Debugger Notification")
    val breakpointUpdatePublisher: BreakpointUpdatedTopic? =
        project.messageBus.syncPublisher(BreakpointUpdatedTopic.TOPIC)
    val projectBasePath = project.basePath + Constants.JAVA_BASE_PATH

    companion object {
        // key of filename and breakpoint line
        val BREAKPOINTS: MutableMap<String, ConcurrentSkipListSet<Int>> = ConcurrentMap();
    }

    fun executeSuite(fileName: String) {
        buildMavenProject {
            val featureClasspath = fileName.substring(projectBasePath.length + 1)

            val urls = getMavenDependenciesURL().joinToString(";");
            val breakpointJson = mapper.writeValueAsString(BREAKPOINTS)
            val breakpointPath = Files.createTempFile("breakpoints_", ".json")
            Files.writeString(
                breakpointPath,
                breakpointJson,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )

            val subscriber = createRemoteCallSubscriber();
            DebugMessageBus.getInstance().subscribe(DebugResponse.TOPIC, subscriber);

            val remotePublisher = DebugMessageBus.getInstance().publisher(DebugRequest.TOPIC);

            val messageBus = project.messageBus.connect();
            messageBus.subscribe(DebuggerInfoRequestTopic.TOPIC, object : DebuggerInfoRequestTopic {
                override fun publishKarateVariables() {
                    remotePublisher.publishKarateVariables();
                }

                override fun stepForward() {
                    remotePublisher.stepOver()
                }

                override fun resume() {
                    remotePublisher.resume()
                }

                override fun evaluateExpression(expression: String) {
                    remotePublisher.evaluateExpression(expression)
                }

                override fun addBreakpoint(fileName: String, lineNumber: Int) {
                    remotePublisher.addBreakpoint(fileName, lineNumber)
                }

                override fun removeBreakpoint(fileName: String, lineNumber: Int) {
                    remotePublisher.removeBreakpoint(fileName, lineNumber)
                }
            })

            val debugServer = DebugServer.getInstance().start()

            val vmOptions = buildString {
                runPropertiesService?.state?.entries?.forEach { entry ->
                    append("-D${entry.key}=${entry.value} ")
                }
                append("-Ddebug.port=${debugServer.port}")
            }.trim()

            val command =
                "${getProjectJavaExecutable(project)} $vmOptions -jar ${getAgentJarFile().path} $featureClasspath ${project.basePath} $breakpointPath $urls"
            val process =
                ProcessBuilder(*command.split(" ").toTypedArray())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            // wait for debug program to complete
            process.waitFor()

            // stop the server
            debugServer.stop()

            // clear all the message bus subscribers
            DebugMessageBus.getInstance().clearAll()
            messageBus.disconnect()
            Files.deleteIfExists(breakpointPath)
        }
    }

    fun getProjectJavaExecutable(project: Project): String {
        val sdk = ProjectRootManager.getInstance(project).projectSdk
            ?: throw IllegalStateException("❌ No SDK configured for the project")

        val javaHome = sdk.homePath
            ?: throw IllegalStateException("❌ SDK has no home path")

        return Paths.get(javaHome, "bin", "java").toString()
    }

    fun addBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS.computeIfAbsent(file) { ConcurrentSkipListSet() }.add(lineNumber)
        breakpointUpdatePublisher?.updatedBreakpoint()
        val remotePublisher = DebugMessageBus.getInstance().publisher(DebugRequest.TOPIC)
        remotePublisher.addBreakpoint(file, lineNumber)
    }

    fun removeBreakpoint(file: String, lineNumber: Int) {
        BREAKPOINTS[file]?.remove(lineNumber)
        breakpointUpdatePublisher?.updatedBreakpoint()
        val remotePublisher = DebugMessageBus.getInstance().publisher(DebugRequest.TOPIC)
        remotePublisher.removeBreakpoint(file, lineNumber)
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
            pomFile.parent,
            pomFile.name,
            true,
            listOf("clean", "package", "-DskipTests=true"),
            emptyMap()
        )
        mavenProject.dependencies

        runner.run(parameters, null, callback)
        return true
    }

    private fun getMavenDependenciesURL(): List<String> {
        val mavenProjectManager = MavenProjectsManager.getInstance(project);
        val dependencies = ArrayList(mavenProjectManager.projects[0].dependencies.map { dep -> dep.file.path });
        dependencies.add(File(getJarPath()).path);

        return dependencies
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

    fun isBreakpointPlaced(file: String, lineNumber: Int): Boolean {
        return BREAKPOINTS[file]?.contains(lineNumber) ?: false
    }

    fun createRemoteCallSubscriber(): DebugResponse {
        return object : DebugResponse {
            override fun updateKarateVariable(vars: HashMap<String, String>) {
                val objectMapper = ObjectMapper()
                val parsedVars = HashMap<String, Map<String, Object>>();
                for ((key, json) in vars) {
                    try {
                        val parsed = objectMapper.readValue(json, object : TypeReference<Map<String, Object>>() {})
                        parsedVars[key] = parsed
                    } catch (e: Exception) {
                        // Optional: log or handle the malformed JSON case
                    }
                }
                responsePublisher.updateKarateVariables(parsedVars);
            }

            override fun updateState(state: DebuggerState) {
                responsePublisher.updateState(state)
            }

            override fun navigateTo(filePath: String, lineNumber: Int) {
                responsePublisher.navigateTo(filePath, lineNumber);
            }

            override fun appendLog(log: String, isSuccess: Boolean) {
                responsePublisher.appendLog(log, isSuccess)
            }

            override fun evaluationResult(
                result: String,
                error: String
            ) {
                responsePublisher.evaluateExpressionResult(result, error)
            }

        }
    }

    fun getAgentJarFile(): File {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("in.srikanthk.devlabs.karate-chop-debugger"))
        val jarFileName = "debug-agent-${plugin?.version}.jar"
        val pluginPath = plugin?.pluginPath
        return File(File(pluginPath?.pathString, "lib"), jarFileName)
    }
}
