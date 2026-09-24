package top.kafuumiaki.animegirlsdownloader.download

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.core.net.toUri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadDao
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadTaskEntity
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadQueueProcessor(
    private val context: Context,
    private val client: OkHttpClient,
    private val dao: DownloadDao,
    private val notifications: DownloadNotifications,
    private val events: DownloadEvents,
) {
    companion object {
        private val queueMutex = Mutex()
        private val activeCalls = ConcurrentHashMap<String, Call>()
        fun cancelActive(taskId: String) { activeCalls[taskId]?.cancel() }
        fun cancelAllActive() { activeCalls.values.forEach(Call::cancel) }
    }

    suspend fun processAll() = queueMutex.withLock {
        while (true) {
            val task = dao.nextPending() ?: break
            dao.updateProgress(task.taskId, DownloadStatus.DOWNLOADING, 0, -1, null)
            try {
                val result = download(task)
                notifications.success(result.fileName, result.destination)
                events.emit(DownloadEvent.Success(result.fileName, result.destination))
                dao.deleteById(task.taskId)
            } catch (error: Throwable) {
                val current = dao.byId(task.taskId)
                if (current == null || current.cancelRequested) {
                    dao.deleteById(task.taskId)
                } else {
                    val detail = error.message ?: error.javaClass.simpleName
                    dao.updateProgress(task.taskId, DownloadStatus.FAILED, current.bytesRead, current.totalBytes, detail)
                    val fileName = outputFileName(task)
                    notifications.failure(fileName)
                    events.emit(DownloadEvent.Failure(fileName, detail))
                }
            } finally {
                activeCalls.remove(task.taskId)
            }
        }
        NotificationManagerCompatCompat.cancel(context, DownloadNotifications.ONGOING_ID)
    }

    private suspend fun download(task: DownloadTaskEntity): DownloadResult {
        val request = Request.Builder().url(task.sourceUrl).get().build()
        val call = client.newCall(request)
        activeCalls[task.taskId] = call
        call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            val total = body.contentLength()
            val mime = body.contentType()?.toString() ?: "application/octet-stream"
            // The server can send both filename and RFC 5987 filename* parameters.
            // The client owns the stable download name, so response headers must not
            // be able to append parameters or change the image ID based filename.
            val fileName = outputFileName(task)
            val target = createTarget(task.destinationTreeUri, fileName, mime)
            try {
                target.stream.use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var readTotal = 0L
                        var lastUpdate = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            readTotal += count
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate >= 250) {
                                if (dao.byId(task.taskId)?.cancelRequested == true) throw IOException("Canceled")
                                dao.updateProgress(task.taskId, DownloadStatus.DOWNLOADING, readTotal, total, null)
                                notifications.update(fileName, task.taskId, if (total > 0) ((readTotal * 100) / total).toInt() else null)
                                lastUpdate = now
                            }
                        }
                        output.flush()
                        dao.updateProgress(task.taskId, DownloadStatus.DOWNLOADING, readTotal, total, null)
                    }
                }
                target.commit()
                return DownloadResult(fileName, target.destination)
            } catch (error: Throwable) {
                target.cleanup()
                throw error
            }
        }
    }

    private fun outputFileName(task: DownloadTaskEntity): String = "${task.imageId}.png"

    private fun createTarget(treeUri: String?, fileName: String, mime: String): OutputTarget {
        if (!treeUri.isNullOrBlank()) {
            val tree = DocumentFile.fromTreeUri(context, treeUri.toUri()) ?: throw IOException("Download folder is unavailable")
            val document = tree.createFile(mime, fileName) ?: throw IOException("Unable to create destination file")
            val stream = context.contentResolver.openOutputStream(document.uri, "w") ?: throw IOException("Unable to open destination")
            return OutputTarget(stream, tree.name ?: tree.uri.toString(), commit = {}, cleanup = { document.delete() })
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AnimeGirlsDownloader")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Unable to create media item")
        val stream = context.contentResolver.openOutputStream(uri, "w") ?: run {
            context.contentResolver.delete(uri, null, null)
            throw IOException("Unable to open media item")
        }
        return OutputTarget(
            stream = stream,
            destination = "${Environment.DIRECTORY_PICTURES}/AnimeGirlsDownloader",
            commit = { context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) },
            cleanup = { context.contentResolver.delete(uri, null, null) },
        )
    }

    private data class DownloadResult(val fileName: String, val destination: String)
    private data class OutputTarget(
        val stream: OutputStream,
        val destination: String,
        val commit: () -> Unit,
        val cleanup: () -> Unit,
    )
}

private object NotificationManagerCompatCompat {
    fun cancel(context: Context, id: Int) {
        androidx.core.app.NotificationManagerCompat.from(context).cancel(id)
    }
}
