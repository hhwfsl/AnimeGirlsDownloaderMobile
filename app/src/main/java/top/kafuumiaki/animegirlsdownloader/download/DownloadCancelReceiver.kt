package top.kafuumiaki.animegirlsdownloader.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.kafuumiaki.animegirlsdownloader.AnimeGirlsApplication

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        val container = (context.applicationContext as AnimeGirlsApplication).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.downloadRepository.cancel(taskId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL = "top.kafuumiaki.animegirlsdownloader.action.CANCEL_DOWNLOAD"
        const val EXTRA_TASK_ID = "task_id"
    }
}
