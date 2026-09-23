package top.kafuumiaki.animegirlsdownloader.core.network

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkClientTest {
    @Test
    fun serverAuthenticationIsNotAddedToExternalHosts() {
        val request = Request.Builder()
            .url("https://api.github.com/repos/example/project/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        val result = request.withApiHeaders("kafuumiaki.top", "server-token")

        assertNull(result.header("Authorization"))
        assertEquals("application/vnd.github+json", result.header("Accept"))
    }

    @Test
    fun serverAuthenticationIsAddedOnlyToTheApiHost() {
        val request = Request.Builder()
            .url("https://kafuumiaki.top/api/Image")
            .build()

        val result = request.withApiHeaders("kafuumiaki.top", "server-token")

        assertEquals("Bearer server-token", result.header("Authorization"))
        assertEquals("application/json", result.header("Accept"))
    }
}
