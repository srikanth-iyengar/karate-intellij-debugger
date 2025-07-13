package `in`.srikanthk.devlabs.kchopdebugger.service

import com.intellij.openapi.application.PathManager
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object JarExecutor {

    /**
     * Extracts a resource (e.g. "lib/agent.jar") to plugin-specific persistent directory
     * and returns its absolute file path.
     */
    fun extractResourceToPersistentLocation(resourcePath: String, outputFileName: String): File {
        val resourceStream: InputStream = JarExecutor::class.java.classLoader
            .getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource $resourcePath not found in classpath")

        val pluginDir = Path.of(PathManager.getPluginsPath(), "karate-chop-debugger", "lib")
        Files.createDirectories(pluginDir)

        val targetFile = pluginDir.resolve(outputFileName).toFile()

        if (!targetFile.exists()) {
            resourceStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return targetFile
    }
}
