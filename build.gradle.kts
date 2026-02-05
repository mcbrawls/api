@file:Suppress("VulnerableLibrariesLocal", "LocalVariableName")

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java")
    id("maven-publish")
}

val projectVersion = project.findProperty("version") as String

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "maven-publish")

    group = "net.mcbrawls.api"
    version = projectVersion

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://libraries.minecraft.net/")
        maven("https://maven.andante.dev/releases/")
        maven("https://libraries.minecraft.net/")
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            )
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }
}

subprojects {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = project.group as String
                artifactId = project.name
                version = projectVersion
            }
        }

        repositories {
            val mavenUrl = System.getenv("MAVEN_URL")
            if (mavenUrl != null) {
                maven {
                    name = "envmaven"
                    url = uri(mavenUrl)
                    credentials {
                        username = System.getenv("MAVEN_USERNAME")
                        password = System.getenv("MAVEN_PASSWORD")
                    }
                }
            }
        }
    }
}
