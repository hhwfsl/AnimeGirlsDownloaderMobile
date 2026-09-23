package top.kafuumiaki.animegirlsdownloader.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.SourceKind
import top.kafuumiaki.animegirlsdownloader.core.model.UploadSource
import top.kafuumiaki.animegirlsdownloader.core.network.AnimeGirlsApi
import top.kafuumiaki.animegirlsdownloader.core.network.TagDto
import top.kafuumiaki.animegirlsdownloader.core.network.UploadImageRequest
import top.kafuumiaki.animegirlsdownloader.core.network.toAppError

class UploadRepository(
    private val context: Context,
    private val resolver: ContentResolver,
    private val api: AnimeGirlsApi,
) {
    companion object {
        const val MAX_RAW_BYTES = 22L * 1024L * 1024L
    }

    fun source(uri: Uri, kind: SourceKind): UploadSource = UploadSource(uri, kind, displayName(uri))

    suspend fun expand(sources: List<UploadSource>): List<UploadSource> = withContext(Dispatchers.IO) {
        buildList {
            val seen = mutableSetOf<String>()
            sources.forEach { source ->
                if (source.kind == SourceKind.IMAGE) {
                    if (seen.add(source.uri.toString())) add(source)
                } else {
                    val root = DocumentFile.fromTreeUri(context, source.uri)
                    root?.let { collectImages(it, seen, this) }
                }
            }
        }
    }

    suspend fun upload(
        source: UploadSource,
        creator: String,
        tags: List<String>,
        isNsfw: Boolean,
        isAi: Boolean,
    ): AppResult<String> {
        return try {
        val length = resolver.openAssetFileDescriptor(source.uri, "r")?.use { it.length } ?: -1
        if (length > MAX_RAW_BYTES) {
            return AppResult.Failure(top.kafuumiaki.animegirlsdownloader.core.model.AppError(top.kafuumiaki.animegirlsdownloader.core.model.AppError.Code.TOO_LARGE, source.displayName))
        }
        val bytes = resolver.openInputStream(source.uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_RAW_BYTES) throw IllegalArgumentException("Image is too large")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Unable to read image")

        val response = api.upload(
            UploadImageRequest(
                creator = creator.ifBlank { "Unknown" },
                tags = tags.takeIf { it.isNotEmpty() }?.map { TagDto(name = it) },
                data = Base64.encodeToString(bytes, Base64.NO_WRAP),
                isNsfw = isNsfw,
                isAiGenerated = isAi,
            )
        )
        AppResult.Success(response.message)
        } catch (error: Throwable) {
            AppResult.Failure(error.toAppError())
        }
    }

    private fun collectImages(file: DocumentFile, seen: MutableSet<String>, output: MutableList<UploadSource>) {
        if (file.isDirectory) {
            file.listFiles().forEach { collectImages(it, seen, output) }
        } else if (file.isFile && file.type?.startsWith("image/") == true && seen.add(file.uri.toString())) {
            output += UploadSource(file.uri, SourceKind.IMAGE, file.name ?: file.uri.lastPathSegment.orEmpty())
        }
    }

    private fun displayName(uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: uri.lastPathSegment.orEmpty()
        }
        return uri.lastPathSegment ?: uri.toString()
    }
}
