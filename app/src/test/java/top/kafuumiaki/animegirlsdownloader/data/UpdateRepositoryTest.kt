package top.kafuumiaki.animegirlsdownloader.data

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    private val repository = UpdateRepository(OkHttpClient())

    @Test
    fun newerSemanticVersionIsDetected() {
        assertTrue(repository.isNewer("1.1.0", "1.0.9"))
        assertTrue(repository.isNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun equalOrOlderVersionIsRejected() {
        assertFalse(repository.isNewer("1.0.0", "1.0.0"))
        assertFalse(repository.isNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun leadingVAndPreReleaseSuffixAreAccepted() {
        assertTrue(repository.isNewer("v1.0.1", "1.0.0-debug"))
    }

    @Test
    fun publishedReleaseAndApkAreParsed() = runTest {
        val client = respondingClient(
            code = 200,
            body = """
                {
                  "tag_name": "v1.2.4",
                  "html_url": "https://github.com/example/project/releases/tag/v1.2.4",
                  "assets": [
                    {
                      "name": "AnimeGirlsDownloaderMobile-v1.2.4.apk",
                      "browser_download_url": "https://github.com/example/project/releases/download/v1.2.4/app.apk"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val result = UpdateRepository(
            client = client,
            releaseUrl = "https://api.github.com/repos/example/project/releases/latest",
            currentVersion = "1.2.3",
        ).check().getOrThrow()

        assertEquals("1.2.4", result?.version)
        assertEquals("https://github.com/example/project/releases/download/v1.2.4/app.apk", result?.downloadUrl)
    }

    @Test
    fun missingReleaseIsTreatedAsNoUpdate() = runTest {
        val result = UpdateRepository(
            client = respondingClient(404),
            releaseUrl = "https://api.github.com/repos/example/project/releases/latest",
            currentVersion = "1.2.3",
        ).check()

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    private fun respondingClient(code: Int, body: String = ""): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                assertEquals("application/vnd.github+json", chain.request().header("Accept"))
                assertEquals("2022-11-28", chain.request().header("X-GitHub-Api-Version"))
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 404) "Not Found" else "OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
}
