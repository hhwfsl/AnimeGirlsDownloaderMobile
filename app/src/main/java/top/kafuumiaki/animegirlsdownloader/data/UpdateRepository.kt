package top.kafuumiaki.animegirlsdownloader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import top.kafuumiaki.animegirlsdownloader.BuildConfig

data class UpdateInfo(val version: String, val releaseUrl: String, val downloadUrl: String?)

class UpdateRepository(
    private val client: OkHttpClient,
    private val releaseUrl: String = BuildConfig.GITHUB_LATEST_RELEASE_URL,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) {
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(releaseUrl)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "AnimeGirlsDownloaderMobile/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@runCatching null
                if (!response.isSuccessful) error("GitHub returned HTTP ${response.code}")
                val release = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                val version = release["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().trimStart('v', 'V')
                if (!isNewer(version, currentVersion)) return@runCatching null
                val apk = release["assets"]?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull {
                        it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true
                    }
                    ?.get("browser_download_url")
                    ?.jsonPrimitive
                    ?.contentOrNull
                val pageUrl = release["html_url"]?.jsonPrimitive?.contentOrNull
                    ?: error("GitHub release does not include html_url")
                UpdateInfo(version, pageUrl, apk)
            }
        }
    }

    internal fun isNewer(remote: String, current: String): Boolean {
        fun parts(value: String) = value.trimStart('v', 'V').substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(remote)
        val b = parts(current)
        return (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { index ->
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            comparison.takeIf { it != 0 }
        }?.let { it > 0 } ?: false
    }
}
