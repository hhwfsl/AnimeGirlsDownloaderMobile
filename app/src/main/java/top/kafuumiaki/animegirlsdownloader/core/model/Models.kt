package top.kafuumiaki.animegirlsdownloader.core.model

import android.net.Uri

enum class ContentFilter(val apiValue: Int) { SFW(0), NSFW(1), ALL(2) }
enum class AiFilter(val apiValue: Int) { EXCLUDE_AI(0), AI_ONLY(1), ALL(2) }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class SourceKind { IMAGE, FOLDER }
enum class DownloadStatus { PENDING, DOWNLOADING, FAILED, CANCELED }

data class ImageInfo(
    val id: String,
    val previewUrl: String,
    val downloadUrl: String,
    val tags: List<String>,
    val isAiGenerated: Boolean,
)

data class UserProfile(
    val id: Long,
    val name: String,
    val avatarPath: String?,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val localeTag: String = "",
    val contentFilter: ContentFilter = ContentFilter.SFW,
    val aiFilter: AiFilter = AiFilter.EXCLUDE_AI,
    val downloadTreeUri: String? = null,
)

data class UploadSource(
    val uri: Uri,
    val kind: SourceKind,
    val displayName: String,
)

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

data class AppError(
    val code: Code,
    val detail: String? = null,
) {
    enum class Code { NETWORK, UNAUTHORIZED, NOT_FOUND, TOO_LARGE, INVALID_DATA, STORAGE, UNKNOWN }
}
