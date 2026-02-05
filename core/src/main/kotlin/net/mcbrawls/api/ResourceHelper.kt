package net.mcbrawls.api

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

object ResourceHelper

fun resource(path: String): File? {
    val url = ResourceHelper::class.java.getResource("/$path") ?: return null

    return try {
        File(url.toURI())
    } catch (e: IllegalArgumentException) {
        val tempDir = Files.createTempDirectory("resource-helper").toFile()
        tempDir.deleteOnExit()
        copyJarResourcesToTemp(path, tempDir)
        File(tempDir, path)
    }
}

private fun copyJarResourcesToTemp(folderPath: String, tempDir: File) {
    val jarPath = ResourceHelper::class.java.protectionDomain.codeSource.location.toURI().path
    val jar = JarFile(jarPath)

    jar.entries().asSequence()
        .filter { it.name.startsWith(folderPath) && !it.isDirectory }
        .forEach { entry ->
            val destFile = File(tempDir, entry.name)
            destFile.parentFile.mkdirs()
            jar.getInputStream(entry).use { input ->
                Files.copy(input, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
}

fun getResourceAsStream(path: String): InputStream? {
    return ResourceHelper::class.java.getResourceAsStream("/$path")
}
