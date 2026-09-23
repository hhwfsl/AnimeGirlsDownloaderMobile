package top.kafuumiaki.animegirlsdownloader.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object BrowserLauncher {
    private const val BROWSER_PROBE_URL = "https://www.example.com/"

    fun open(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") return false

        val browserPackage = resolveDefaultBrowserPackage(context)
        val primary = createBrowserIntent(uri, browserPackage)
        if (runCatching { context.startActivity(primary) }.isSuccess) return true

        if (browserPackage != null) {
            val fallback = createBrowserIntent(uri, null)
            return runCatching { context.startActivity(fallback) }.isSuccess
        }
        return false
    }

    internal fun createBrowserIntent(uri: Uri, browserPackage: String?): Intent =
        if (browserPackage != null) {
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(browserPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

    private fun resolveDefaultBrowserPackage(context: Context): String? {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse(BROWSER_PROBE_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return context.packageManager
            .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?.takeUnless { it == "android" }
    }
}
