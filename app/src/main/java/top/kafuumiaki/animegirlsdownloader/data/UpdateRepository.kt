package top.kafuumiaki.animegirlsdownloader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import top.kafuumiaki.animegirlsdownloader.BuildConfig

data class UpdateInfo(val version: String, val releaseUrl: String, val downloadUrl: String?)

class UpdateRepository(private val client: OkHttpClient) {
    suspend fun check(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AnimeGirlsDownloaderMobile/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("GitHub returned HTTP ${response.code}")
                val json = JSONObject(response.body?.string().orEmpty())
                val version = json.optString("tag_name").trimStart('v', 'V')
                if (!isNewer(version, BuildConfig.VERSION_NAME)) return@runCatching null
                val assets = json.optJSONArray("assets")
                val apk = (0 until (assets?.length() ?: 0))
                    .map { assets!!.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                UpdateInfo(version, json.getString("html_url"), apk)
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
