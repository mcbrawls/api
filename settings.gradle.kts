@file:Suppress("LocalVariableName")

pluginManagement {
    val kotlin_version: String by settings

    plugins {
        kotlin("jvm") version kotlin_version
        kotlin("plugin.serialization") version kotlin_version
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "brawls-api"
