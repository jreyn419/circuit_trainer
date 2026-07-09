package io.github.jreyn419.circuittrainer.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists a list of items as a small JSON file with atomic writes.
 * Shared by the workout and exercise-library repositories.
 */
class JsonListFile<T>(
    private val file: File,
    private val serializer: KSerializer<List<T>>,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    fun loadOr(default: () -> List<T>): List<T> {
        return try {
            if (file.exists()) json.decodeFromString(serializer, file.readText()) else default()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun persistAsync(snapshot: List<T>) {
        ioScope.launch {
            writeMutex.withLock {
                try {
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(json.encodeToString(serializer, snapshot))
                    if (!tmp.renameTo(file)) {
                        file.writeText(tmp.readText())
                        tmp.delete()
                    }
                } catch (_: Exception) {
                    // Persisting is best-effort; in-memory state stays authoritative.
                }
            }
        }
    }
}
