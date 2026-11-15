package dev.neverpaid.health.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import dev.neverpaid.health.core.platform.PlatformBridge
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Immutable
data class RemoteImageState(
    val bitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

/**
 * Shared KMP in-memory image fetcher & decoder for exercise photos and base64 attachments.
 */
object ExerciseImageLoader {
    private const val MAX_CACHED_IMAGES = 160
    private const val JSDELIVR_PREFIX = "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises/"
    private const val RAW_GITHUB_PREFIX = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    private val client = HttpClient {
        followRedirects = true
    }

    private val cacheMutex = Mutex()
    private val bitmapCache = LinkedHashMap<String, ImageBitmap>(MAX_CACHED_IMAGES)
    private val failedUrls = HashSet<String>()
    private val inFlight = HashMap<String, CompletableDeferred<ImageBitmap?>>()

    suspend fun load(url: String): ImageBitmap? {
        if (url.isBlank()) return null

        val deferredToJoin: CompletableDeferred<ImageBitmap?>?
        val ownedDeferred: CompletableDeferred<ImageBitmap?>?

        cacheMutex.withLock {
            val cached = bitmapCache.remove(url)
            if (cached != null) {
                // Touch LRU order
                bitmapCache[url] = cached
                return cached
            }
            if (url in failedUrls) return null

            val existing = inFlight[url]
            if (existing != null) {
                deferredToJoin = existing
                ownedDeferred = null
            } else {
                val created = CompletableDeferred<ImageBitmap?>()
                inFlight[url] = created
                deferredToJoin = null
                ownedDeferred = created
            }
        }

        if (deferredToJoin != null) {
            return deferredToJoin.await()
        }

        val deferred = ownedDeferred ?: return null
        val decoded = try {
            fetchAndDecodeWithFallback(url)
        } catch (_: Throwable) {
            null
        }

        cacheMutex.withLock {
            inFlight.remove(url)
            if (decoded != null) {
                if (bitmapCache.size >= MAX_CACHED_IMAGES) {
                    val oldestKey = bitmapCache.keys.firstOrNull()
                    if (oldestKey != null) bitmapCache.remove(oldestKey)
                }
                bitmapCache[url] = decoded
                failedUrls.remove(url)
            } else {
                if (failedUrls.size > 300) failedUrls.clear()
                failedUrls.add(url)
            }
        }

        deferred.complete(decoded)
        return decoded
    }

    fun getCached(url: String?): ImageBitmap? {
        if (url.isNullOrBlank()) return null
        return bitmapCache[url]
    }

    private suspend fun fetchAndDecodeWithFallback(url: String): ImageBitmap? {
        val primaryBytes = fetchBytes(url)
        if (primaryBytes != null) {
            val decoded = withContext(Dispatchers.Default) {
                PlatformBridge.decodeImageBitmap(primaryBytes)
            }
            if (decoded != null) return decoded
        }

        val fallbackUrl = if (url.startsWith(JSDELIVR_PREFIX)) {
            RAW_GITHUB_PREFIX + url.removePrefix(JSDELIVR_PREFIX)
        } else {
            null
        }

        if (fallbackUrl != null) {
            val fallbackBytes = fetchBytes(fallbackUrl)
            if (fallbackBytes != null) {
                return withContext(Dispatchers.Default) {
                    PlatformBridge.decodeImageBitmap(fallbackBytes)
                }
            }
        }
        return null
    }

    private suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val response = client.get(url)
            if (!response.status.isSuccess()) return@withContext null
            response.body<ByteArray>()
        } catch (_: Throwable) {
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun decodeBase64Image(rawBase64: String?): ImageBitmap? {
        val raw = rawBase64?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cleanPayload = raw.substringAfter("base64,", raw)
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        return withContext(Dispatchers.Default) {
            try {
                val bytes = Base64.Default.decode(cleanPayload)
                PlatformBridge.decodeImageBitmap(bytes)
            } catch (_: Throwable) {
                try {
                    val bytes = Base64.UrlSafe.decode(cleanPayload)
                    PlatformBridge.decodeImageBitmap(bytes)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}

@Composable
fun rememberRemoteImageBitmap(url: String?): RemoteImageState {
    var state by remember(url) {
        val immediate = ExerciseImageLoader.getCached(url)
        mutableStateOf(
            when {
                immediate != null -> RemoteImageState(bitmap = immediate, isLoading = false, isError = false)
                url.isNullOrBlank() -> RemoteImageState(bitmap = null, isLoading = false, isError = true)
                else -> RemoteImageState(bitmap = null, isLoading = true, isError = false)
            }
        )
    }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            state = RemoteImageState(bitmap = null, isLoading = false, isError = true)
            return@LaunchedEffect
        }
        val cached = ExerciseImageLoader.getCached(url)
        if (cached != null) {
            state = RemoteImageState(bitmap = cached, isLoading = false, isError = false)
            return@LaunchedEffect
        }
        state = RemoteImageState(bitmap = null, isLoading = true, isError = false)
        val loaded = ExerciseImageLoader.load(url)
        state = if (loaded != null) {
            RemoteImageState(bitmap = loaded, isLoading = false, isError = false)
        } else {
            RemoteImageState(bitmap = null, isLoading = false, isError = true)
        }
    }

    return state
}

@Composable
fun rememberBase64ImageBitmap(base64: String?): ImageBitmap? {
    var bitmap by remember(base64) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(base64) {
        bitmap = ExerciseImageLoader.decodeBase64Image(base64)
    }
    return bitmap
}
