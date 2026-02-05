@file:Suppress("LocalVariableName")

dependencies {
    val exposed_version by properties
    val hikari_version by properties

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    api("org.jetbrains.exposed:exposed-core:$exposed_version")
    api("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    api("com.zaxxer:HikariCP:$hikari_version")

    api("com.mojang:datafixerupper:7.0.14")
    api("com.mojang:brigadier:1.0.18")
}
