package `in`.srikanthk.devlabs.kchopdebugger.service

interface Constants {
    companion object {
        const val JAVA_BASE_PATH = "/src/test/java"
        const val MAVEN_BUILDER_COMMAND = "mvn clean package -DskipTests=true"
    }
}
