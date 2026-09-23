package top.kafuumiaki.animegirlsdownloader.download

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.kafuumiaki.animegirlsdownloader.AnimeGirlsApplication

class DownloadJobService : JobService() {
    private var scope: CoroutineScope? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val container = (application as AnimeGirlsApplication).container
        if (Build.VERSION.SDK_INT >= 34) {
            setNotification(
                params,
                DownloadNotifications.ONGOING_ID,
                container.notifications.ongoing("…"),
                JOB_END_NOTIFICATION_POLICY_REMOVE,
            )
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also {
            it.launch {
                container.downloadProcessor.processAll()
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        DownloadQueueProcessor.cancelAllActive()
        scope?.cancel()
        scope = null
        return true
    }
}
