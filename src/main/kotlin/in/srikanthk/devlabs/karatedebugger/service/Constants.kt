package `in`.srikanthk.devlabs.karatedebugger.service

interface Constants {
    companion object {
        const val JAVA_BASE_PATH = "/src/test/java"
        const val MAVEN_BUILDER_COMMAND = "mvn clean package -DskipTests=true"
    }
}

enum class DebuggerState {
    Halted,
    Finished,
    Started
}