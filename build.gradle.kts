import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    // https://mvnrepository.com/artifact/org.jetbrains.maven/info-maven3-plugin
//    implementation("org.jetbrains.maven:info-maven3-plugin:1.0.2")
}

group = "in.srikanthk.devlabs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }

}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }

    // https://mvnrepository.com/artifact/io.projectreactor/reactor-core
    implementation("io.projectreactor:reactor-core:3.7.7")
    // https://mvnrepository.com/artifact/com.intuit.karate/karate-junit5
    implementation("com.intuit.karate:karate-junit5:1.4.1")
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
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
}
