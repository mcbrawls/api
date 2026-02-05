@file:Suppress("LocalVariableName")

dependencies {
    api(project(":core"))
    api(project(":database"))

    api("org.slf4j:slf4j-simple:2.0.12")

    val ktor_version by properties
    val ktor_swagger_version by properties

    api("io.ktor:ktor-server-core:$ktor_version")
    api("io.ktor:ktor-server-netty:$ktor_version")
    api("io.ktor:ktor-server-auth:$ktor_version")
    api("io.github.smiley4:ktor-swagger-ui:$ktor_swagger_version")

    api("com.mysql:mysql-connector-j:9.6.0")
}

val fatJar = tasks.register("fatJar", type = Jar::class) {
    archiveBaseName = "${project.name}-fat"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "net.mcbrawls.api.server.Main"
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
