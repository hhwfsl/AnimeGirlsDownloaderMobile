package top.kafuumiaki.animegirlsdownloader.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class DownloadQueueScheduler(private val context: Context) {
    fun schedule() {
        if (Build.VERSION.SDK_INT >= 34) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            val result = scheduler.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, DownloadJobService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setUserInitiated(true)
                    .build()
            )
            if (result == JobScheduler.RESULT_SUCCESS) return
        }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        private const val JOB_ID = 24501
        private const val WORK_NAME = "anime_girls_download_queue"
    }
}
