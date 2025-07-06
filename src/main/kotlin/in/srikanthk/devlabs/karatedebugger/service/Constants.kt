package `in`.srikanthk.devlabs.karatedebugger.service

import com.intuit.karate.core.Variable

interface Constants {
    companion object {
        const val JAVA_BASE_PATH = "/src/test/java"
        const val MAVEN_BUILDER_COMMAND = "mvn clean package -DskipTests=true"
    }
}

interface DebugState {
    companion object {
        val DEBUG_VARS = HashMap<String, Variable>()
    }
}

enum class DebuggerState {
    Halted,
    Finished,
    Started
}