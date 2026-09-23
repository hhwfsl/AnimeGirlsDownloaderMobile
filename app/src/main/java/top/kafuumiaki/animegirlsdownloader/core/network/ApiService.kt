package top.kafuumiaki.animegirlsdownloader.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.MultipartBody

interface AnimeGirlsApi {
    @POST("Auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("Auth/register")
    suspend fun register(@Body request: LoginRequest): TokenResponse

    @POST("Auth/login-with-token")
    suspend fun loginWithToken(): TokenResponse

    @GET("Auth/me")
    suspend fun me(): UserProfileResponse

    @Streaming
    @GET("Auth/avatar")
    suspend fun avatar(): Response<ResponseBody>

    @PUT("Auth/profile")
    suspend fun updateProfile(@Body request: UpdateUserNameRequest): UserProfileResponse

    @Multipart
    @PUT("Auth/avatar")
    suspend fun updateAvatar(@Part avatar: MultipartBody.Part): UserProfileResponse

    @POST("Image/random")
    suspend fun randomImage(@Body request: GetImageRequest): GetImageResponse

    @POST("Image/id")
    suspend fun imageById(@Body request: GetImageRequest): GetImageResponse

    @POST("Image/tag/fulltags")
    suspend fun completeTags(@Body request: TagRequest): TagResponse

    @POST("Image/upload")
    suspend fun upload(@Body request: UploadImageRequest): MessageResponse
}
