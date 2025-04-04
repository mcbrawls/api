package net.mcbrawls.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path

@PublishedApi
internal val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(2))

inline fun runAsync(crossinline block: suspend CoroutineScope.() -> Unit) {
    scope.launch {
        block.invoke(this)
    }
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
