package top.kafuumiaki.animegirlsdownloader.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import top.kafuumiaki.animegirlsdownloader.MainActivity
import top.kafuumiaki.animegirlsdownloader.R

class DownloadNotifications(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "image_downloads"
        const val ONGOING_ID = 4101
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.download_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = context.getString(R.string.download_notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun ongoing(fileName: String, taskId: String? = null, progress: Int? = null): Notification {
        val builder = baseBuilder()
            .setContentTitle(context.getString(R.string.download_notification_title, fileName))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress ?: 0, progress == null)
        if (taskId != null) {
            val cancelIntent = Intent(context, DownloadCancelReceiver::class.java)
                .setAction(DownloadCancelReceiver.ACTION_CANCEL)
                .putExtra(DownloadCancelReceiver.EXTRA_TASK_ID, taskId)
            val cancelPending = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.action_cancel), cancelPending)
        }
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun update(fileName: String, taskId: String, progress: Int?) {
        if (canPostNotifications()) NotificationManagerCompat.from(context).notify(ONGOING_ID, ongoing(fileName, taskId, progress))
    }

    @SuppressLint("MissingPermission")
    fun success(fileName: String, destination: String) {
        val notification = baseBuilder()
            .setContentTitle(context.getString(R.string.download_success, fileName, destination))
            .setAutoCancel(true)
            .build()
        if (canPostNotifications()) NotificationManagerCompat.from(context).notify(fileName.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    fun failure(fileName: String) {
        val notification = baseBuilder()
            .setContentTitle(context.getString(R.string.download_failed, fileName))
            .setAutoCancel(true)
            .build()
        if (canPostNotifications()) NotificationManagerCompat.from(context).notify(fileName.hashCode(), notification)
    }

    private fun baseBuilder(): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).putExtra("destination", "downloads")
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
