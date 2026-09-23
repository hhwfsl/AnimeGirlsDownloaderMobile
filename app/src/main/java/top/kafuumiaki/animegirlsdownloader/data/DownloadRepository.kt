package top.kafuumiaki.animegirlsdownloader.data

import kotlinx.coroutines.flow.Flow
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadDao
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadTaskEntity
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus
import top.kafuumiaki.animegirlsdownloader.core.model.ImageInfo
import top.kafuumiaki.animegirlsdownloader.download.DownloadQueueProcessor
import top.kafuumiaki.animegirlsdownloader.download.DownloadQueueScheduler
import java.util.UUID

class DownloadRepository(
    private val dao: DownloadDao,
    private val scheduler: DownloadQueueScheduler,
) {
    fun observe(ownerId: String): Flow<List<DownloadTaskEntity>> = dao.observeForOwner(ownerId)

    suspend fun enqueue(ownerId: String, image: ImageInfo, destinationTreeUri: String?) {
        dao.upsert(
            DownloadTaskEntity(
                taskId = UUID.randomUUID().toString(),
                ownerId = ownerId,
                imageId = image.id,
                previewUrl = image.previewUrl,
                sourceUrl = image.downloadUrl,
                fileName = "${image.id}.img",
                destinationTreeUri = destinationTreeUri,
                status = DownloadStatus.PENDING,
            )
        )
        scheduler.schedule()
    }

    suspend fun cancel(taskId: String) {
        val task = dao.byId(taskId) ?: return
        if (task.status == DownloadStatus.DOWNLOADING) {
            dao.requestCancel(taskId)
            DownloadQueueProcessor.cancelActive(taskId)
        } else {
            dao.deleteById(taskId)
        }
    }

    suspend fun retry(taskId: String) {
        dao.retry(taskId)
        scheduler.schedule()
    }

    suspend fun remove(taskId: String) {
        cancel(taskId)
        dao.deleteById(taskId)
    }
}
