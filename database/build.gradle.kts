@file:Suppress("LocalVariableName")

dependencies {
    api(project(":core"))

    val exposed_version by properties

    api("org.jetbrains.exposed:exposed-r2dbc:$exposed_version")
    api("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    api("org.jetbrains.exposed:exposed-json:$exposed_version")
    api("org.jetbrains.exposed:exposed-dao:$exposed_version")
}
