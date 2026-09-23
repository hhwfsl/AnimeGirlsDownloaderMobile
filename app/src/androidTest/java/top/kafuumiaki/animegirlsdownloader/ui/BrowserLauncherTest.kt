package top.kafuumiaki.animegirlsdownloader.ui

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserLauncherTest {
    private val projectUri = Uri.parse("https://github.com/hhwfsl/AnimeGirlsDownloaderMobile")

    @Test
    fun defaultBrowserIntentTargetsBrowserAndStartsANewTask() {
        val intent = BrowserLauncher.createBrowserIntent(projectUri, "com.example.browser")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(projectUri, intent.data)
        assertEquals("com.example.browser", intent.`package`)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun browserSelectorFallbackStartsANewTask() {
        val intent = BrowserLauncher.createBrowserIntent(projectUri, null)

        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals(projectUri, intent.data)
        assertTrue(intent.hasCategory(Intent.CATEGORY_APP_BROWSER))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
