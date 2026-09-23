package top.kafuumiaki.animegirlsdownloader.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
class ClipboardHelper(private val context: Context) {
    fun copyLink(url: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Image URL", url))
    }

}
