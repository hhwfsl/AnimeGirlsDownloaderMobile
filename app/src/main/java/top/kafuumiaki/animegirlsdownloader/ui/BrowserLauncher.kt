package top.kafuumiaki.animegirlsdownloader.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

object BrowserLauncher {
    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        val browserPackage = CustomTabsClient.getPackageName(context, null)
        if (browserPackage != null) {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .apply { intent.setPackage(browserPackage) }
                .launchUrl(context, uri)
            return
        }

        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
            .apply { data = uri }
            .also(context::startActivity)
    }
}
