@file:Suppress("VulnerableLibrariesLocal", "LocalVariableName")

plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

group = "net.mcbrawls"
version = project.findProperty("version") as String

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://libraries.minecraft.net/")
    maven("https://maven.andante.dev/releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    val ktor_version = property("ktor_version")

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.github.smiley4:ktor-swagger-ui:3.2.0")

    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.12")

    implementation("com.mojang:datafixerupper:7.0.14")
    implementation("com.mojang:brigadier:1.0.18")
    implementation("dev.andante:codex:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)

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

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName = "${project.name}-fat"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "net.mcbrawls.api.MainKt"
    }

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }

    with(tasks["jar"] as CopySpec)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }

    repositories {
        val env = System.getenv()
        val envUsername = env["MAVEN_USERNAME_ANDANTE"]
        val envPassword = env["MAVEN_PASSWORD_ANDANTE"]
        if (envUsername != null && envPassword != null) {
            maven {
                url = uri("https://maven.andante.dev/releases/")
                credentials {
                    username = envUsername
                    password = envPassword
                }
            }
        }
    }
}
