plugins {
    kotlin("jvm")
    application
}

group = "in.srikanthk.devlabs.kchopdebugger"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    implementation("com.intuit.karate:karate-junit5:1.4.1")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("in.srikanthk.devlabs.kchopdebugger.agent.Main")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "in.srikanthk.devlabs.kchopdebugger.agent.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}

