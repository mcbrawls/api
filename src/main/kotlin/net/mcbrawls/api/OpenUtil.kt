package net.mcbrawls.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import java.nio.file.Path

@Suppress("DeferredResultUnused")
inline fun runAsync(crossinline block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.async { block.invoke(this) }
}

/**
 * Retrieves a file from the run directory.
 */
fun file(path: String): File {
    return Path.of(path).toFile()
}

fun generateEnumSqlType(ids: Collection<String>): String {
    val joined = ids.joinToString(transform = { "'$it'" })
    return "enum($joined)"
}
