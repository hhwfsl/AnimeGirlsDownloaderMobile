package top.kafuumiaki.animegirlsdownloader.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import top.kafuumiaki.animegirlsdownloader.AnimeGirlsApplication

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as AnimeGirlsApplication).container
        val notification = container.notifications.ongoing("…")
        val foregroundInfo = if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(DownloadNotifications.ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(DownloadNotifications.ONGOING_ID, notification)
        }
        setForeground(foregroundInfo)
        container.downloadProcessor.processAll()
        return Result.success()
    }
}
