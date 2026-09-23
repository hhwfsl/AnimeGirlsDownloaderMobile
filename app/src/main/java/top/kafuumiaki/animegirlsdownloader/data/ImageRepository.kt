package top.kafuumiaki.animegirlsdownloader.data

import top.kafuumiaki.animegirlsdownloader.BuildConfig
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ImageInfo
import top.kafuumiaki.animegirlsdownloader.core.network.AnimeGirlsApi
import top.kafuumiaki.animegirlsdownloader.core.network.GetImageRequest
import top.kafuumiaki.animegirlsdownloader.core.network.TagDto
import top.kafuumiaki.animegirlsdownloader.core.network.TagRequest
import top.kafuumiaki.animegirlsdownloader.core.network.toAppError
import java.net.URI

class ImageRepository(private val api: AnimeGirlsApi) {
    suspend fun find(query: String, content: ContentFilter, ai: AiFilter): AppResult<ImageInfo> = try {
        val normalized = query.trim()
        val request = GetImageRequest(
            id = normalized.toLongOrNull(),
            tags = normalized.takeIf { it.isNotEmpty() && it.toLongOrNull() == null }
                ?.split(Regex("\\s+"))
                ?.filter(String::isNotBlank)
                ?.map { TagDto(name = it) },
            type = content.apiValue,
            isAllowAiGenerated = ai.apiValue,
        )
        val response = if (request.id != null) api.imageById(request) else api.randomImage(request)
        AppResult.Success(
            ImageInfo(
                id = response.id,
                previewUrl = resolve(response.previewUrl),
                downloadUrl = resolve(response.downloadUrl),
                tags = response.tags.orEmpty().map { it.name },
                isAiGenerated = response.isAiGenerated,
            )
        )
    } catch (error: Throwable) {
        AppResult.Failure(error.toAppError())
    }

    suspend fun completeTags(query: String): List<String> = runCatching {
        api.completeTags(TagRequest(query)).tags.orEmpty().map { it.name }
    }.getOrDefault(emptyList())

    fun publicImageUrl(id: String): String = BuildConfig.PUBLIC_IMAGE_BASE_URL + id

    private fun resolve(path: String): String = URI(BuildConfig.API_BASE_URL).resolve(path).toString()
}
