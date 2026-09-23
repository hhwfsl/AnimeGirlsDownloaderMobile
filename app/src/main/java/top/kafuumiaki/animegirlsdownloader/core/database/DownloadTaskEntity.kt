package top.kafuumiaki.animegirlsdownloader.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val taskId: String,
    val ownerId: String,
    val imageId: String,
    val previewUrl: String,
    val sourceUrl: String,
    val fileName: String,
    val destinationTreeUri: String?,
    val status: DownloadStatus,
    val bytesRead: Long = 0,
    val totalBytes: Long = -1,
    val error: String? = null,
    val cancelRequested: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
