package top.kafuumiaki.animegirlsdownloader.data

import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
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
}
