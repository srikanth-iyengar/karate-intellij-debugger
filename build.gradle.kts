import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "in.srikanthk.devlabs"
version = "1.2.0-alpha"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("org.jetbrains.idea.maven")
    }

    implementation("com.intuit.karate:karate-junit5:1.4.1")
    implementation(project(":debug-agent"))
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    signPlugin {
        val certPath = System.getenv("CERTIFICATE_CHAIN_PATH")
        val keyPath = System.getenv("PRIVATE_KEY_PATH")
        val certPassword = System.getenv("PRIVATE_KEY_PASSWORD")

        println("🔐 Starting plugin signing process...")
        println("🔍 CERTIFICATE_CHAIN_PATH = $certPath")
        println("🔍 PRIVATE_KEY_PATH = $keyPath")
        println("🔍 PRIVATE_KEY_PASSWORD = ${if (certPassword != null) "***" else "NOT SET"}")

        if(certPath != null && keyPath != null) {
            val certFile = Paths.get(certPath)
            val keyFile = Paths.get(keyPath)

            val certContent = Files.readString(certFile).trim()
            val keyContent = Files.readString(keyFile).trim()

            println("✅ Certificate and key files loaded successfully.")
            certificateChain.set(certContent)
            privateKey.set(keyContent)
            password.set(certPassword)
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(":debug-agent:build")

    from("debug-agent/build/libs/debug-agent-${project.version}.jar") {
        into("lib")
        rename { "agent.jar" }
    }
}