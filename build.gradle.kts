@file:Suppress("VulnerableLibrariesLocal", "LocalVariableName")

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    val kotlin_version by properties
    val ktor_version by properties
    val exposed_version by properties
    val hikari_version by properties
    val ktor_swagger_version by properties

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.github.smiley4:ktor-swagger-ui:$ktor_swagger_version")

    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.12")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")

    implementation("com.mojang:datafixerupper:7.0.14")
    implementation("com.mojang:brigadier:1.0.18")
}

tasks.test {
    useJUnitPlatform()
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

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName = "${project.name}-fat"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "net.mcbrawls.api.MainKt"
    }

    // Include all dependencies, including transitive ones, from runtimeClasspath
    from({
        configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file else zipTree(file)
        }
    }) {
        // Exclude signature and checksum files from META-INF directory
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.MF")
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
                url = uri("https://maven.mcbrawls.net/releases/")
                credentials {
                    username = envUsername
                    password = envPassword
                }
            }
        }
    }
}
