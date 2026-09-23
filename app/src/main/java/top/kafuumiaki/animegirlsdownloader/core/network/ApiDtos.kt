package top.kafuumiaki.animegirlsdownloader.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("UserName") val userName: String,
    @SerialName("Password") val password: String,
)

@Serializable
data class TokenResponse(@SerialName("Token") val token: String)

@Serializable
data class UserProfileResponse(
    @SerialName("Id") val id: Long,
    @SerialName("Name") val name: String,
    @SerialName("AvatarUrl") val avatarUrl: String? = null,
)

@Serializable
data class UpdateUserNameRequest(@SerialName("Name") val name: String)

@Serializable
data class TagDto(
    @SerialName("Id") val id: Long = 0,
    @SerialName("Name") val name: String,
)

@Serializable
data class GetImageRequest(
    @SerialName("Id") val id: Long? = null,
    @SerialName("Tags") val tags: List<TagDto>? = null,
    @SerialName("Type") val type: Int,
    @SerialName("IsAllowAiGenerated") val isAllowAiGenerated: Int,
)

@Serializable
data class GetImageResponse(
    @SerialName("Id") val id: String,
    @SerialName("PreviewUrl") val previewUrl: String,
    @SerialName("DownloadUrl") val downloadUrl: String,
    @SerialName("Tags") val tags: List<TagDto>? = null,
    @SerialName("IsAiGenerate") val isAiGenerated: Boolean,
)

@Serializable
data class TagRequest(@SerialName("PartialTag") val partialTag: String)

@Serializable
data class TagResponse(@SerialName("Tags") val tags: List<TagDto>? = null)

@Serializable
data class UploadImageRequest(
    @SerialName("Creator") val creator: String,
    @SerialName("Tags") val tags: List<TagDto>? = null,
    @SerialName("Data") val data: String,
    @SerialName("IsNSFW") val isNsfw: Boolean,
    @SerialName("IsAiGenerate") val isAiGenerated: Boolean,
)

@Serializable
data class MessageResponse(@SerialName("Message") val message: String)
