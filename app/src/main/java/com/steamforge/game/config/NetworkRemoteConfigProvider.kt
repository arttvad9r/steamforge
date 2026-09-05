package com.steamforge.game.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_REMOTE_CONFIG_BYTES = 64 * 1024
private const val DEFAULT_CONNECT_TIMEOUT_MS = 3_000
private const val DEFAULT_READ_TIMEOUT_MS = 3_000

private val Context.remoteConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "steamforge_remote_config",
)

private val remoteConfigJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Small cache boundary so provider behavior stays unit-testable without Android storage. */
interface RemoteConfigCache {
    suspend fun read(): String?
    suspend fun write(payload: String)
}

class PreferencesRemoteConfigCache(context: Context) : RemoteConfigCache {
    private val dataStore = context.applicationContext.remoteConfigDataStore

    private object Keys {
        val payload = stringPreferencesKey("validated_payload")
    }

    override suspend fun read(): String? = dataStore.data.first()[Keys.payload]

    override suspend fun write(payload: String) {
        dataStore.edit { prefs -> prefs[Keys.payload] = payload }
    }
}

/** Fetch boundary separated from parsing/caching so network behavior can be tested with a fake. */
fun interface RemoteConfigFetcher {
    suspend fun fetch(): String?
}

/**
 * Minimal HTTPS JSON transport. Redirects are disabled so a configured HTTPS endpoint cannot
 * silently downgrade to cleartext HTTP. Payload size and timeouts are intentionally bounded.
 */
class HttpsRemoteConfigFetcher(
    url: String,
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
) : RemoteConfigFetcher {
    private val endpoint: URL? = runCatching { URL(url.trim()) }
        .getOrNull()
        ?.takeIf { it.protocol.equals("https", ignoreCase = true) && it.host.isNotBlank() }

    override suspend fun fetch(): String? {
        val target = endpoint ?: return null
        return withContext(Dispatchers.IO) {
            val connection = runCatching { target.openConnection() as HttpURLConnection }.getOrNull()
                ?: return@withContext null
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = connectTimeoutMs.coerceAtLeast(1)
                connection.readTimeout = readTimeoutMs.coerceAtLeast(1)
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode !in 200..299) return@withContext null
                connection.inputStream.use(::readUtf8Limited)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }
}

/**
 * Cache-first provider. Startup begins with compiled defaults, then refresh may promote a validated
 * cached snapshot and finally a validated remote snapshot. Invalid network payloads never overwrite
 * the last known-good cache.
 */
class CachingRemoteConfigProvider(
    private val cache: RemoteConfigCache,
    private val fetcher: RemoteConfigFetcher,
    private val defaults: RemoteGameConfig = LocalDefaultConfig.value,
) : RemoteConfigProvider {
    private val safeDefaults = defaults.sanitized(LocalDefaultConfig.value)
    private val _snapshot = MutableStateFlow(
        RemoteConfigSnapshot(
            config = safeDefaults,
            source = RemoteConfigSource.LOCAL_DEFAULT,
            revision = "local-schema-$REMOTE_CONFIG_SCHEMA_VERSION",
        ),
    )

    override val snapshot: StateFlow<RemoteConfigSnapshot> = _snapshot.asStateFlow()

    override suspend fun refresh(): RemoteConfigRefreshResult {
        readValidatedCache()?.let { cached ->
            _snapshot.value = snapshotOf(cached, RemoteConfigSource.CACHE)
        }

        val remotePayload = try {
            fetcher.fetch()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val remoteConfig = decodeValidated(remotePayload)
            ?: return RemoteConfigRefreshResult.FAILED_USING_FALLBACK

        val encoded = remoteConfigJson.encodeToString(remoteConfig)
        try {
            cache.write(encoded)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Remote snapshot can still be used for this process; cache remains last-known-good.
        }
        _snapshot.value = snapshotOf(remoteConfig, RemoteConfigSource.REMOTE)
        return RemoteConfigRefreshResult.UPDATED
    }

    private suspend fun readValidatedCache(): RemoteGameConfig? {
        val payload = try {
            cache.read()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return decodeValidated(payload)
    }

    private fun decodeValidated(payload: String?): RemoteGameConfig? {
        if (payload.isNullOrBlank() || payload.toByteArray(Charsets.UTF_8).size > MAX_REMOTE_CONFIG_BYTES) {
            return null
        }
        val parsed = runCatching { remoteConfigJson.decodeFromString<RemoteGameConfig>(payload) }
            .getOrNull()
            ?: return null
        if (parsed.schemaVersion != REMOTE_CONFIG_SCHEMA_VERSION) return null
        return parsed.sanitized(safeDefaults)
    }

    private fun snapshotOf(
        config: RemoteGameConfig,
        source: RemoteConfigSource,
    ): RemoteConfigSnapshot {
        val encoded = remoteConfigJson.encodeToString(config)
        return RemoteConfigSnapshot(
            config = config,
            source = source,
            revision = "${source.name.lowercase()}-${Integer.toHexString(encoded.hashCode())}",
        )
    }
}

private fun readUtf8Limited(input: InputStream): String? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(4 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > MAX_REMOTE_CONFIG_BYTES) return null
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
