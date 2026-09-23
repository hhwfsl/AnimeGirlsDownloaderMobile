package top.kafuumiaki.animegirlsdownloader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.UserProfile
import top.kafuumiaki.animegirlsdownloader.core.network.AnimeGirlsApi
import top.kafuumiaki.animegirlsdownloader.core.network.LoginRequest
import top.kafuumiaki.animegirlsdownloader.core.network.SessionTokenProvider
import top.kafuumiaki.animegirlsdownloader.core.network.UpdateUserNameRequest
import top.kafuumiaki.animegirlsdownloader.core.network.toAppError
import top.kafuumiaki.animegirlsdownloader.core.preferences.AppPreferences
import top.kafuumiaki.animegirlsdownloader.core.security.SecureTokenStore
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository(
    private val context: Context,
    private val api: AnimeGirlsApi,
    private val preferences: AppPreferences,
    private val tokens: SecureTokenStore,
    private val sessionToken: SessionTokenProvider,
) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    suspend fun restore(): Boolean {
        val userId = preferences.activeUserId.first() ?: return false
        val token = tokens.read(userId) ?: return false
        sessionToken.set(token)
        return runCatching {
            val refreshed = api.loginWithToken().token
            sessionToken.set(refreshed)
            tokens.save(userId, refreshed)
            syncProfile()
            true
        }.getOrElse {
            sessionToken.set(null)
            _currentUser.value = null
            false
        }
    }

    suspend fun login(userName: String, password: String, register: Boolean): AppResult<UserProfile> = try {
        val token = if (register) api.register(LoginRequest(userName, password)).token else api.login(LoginRequest(userName, password)).token
        sessionToken.set(token)
        val remote = api.me()
        tokens.save(remote.id, token)
        preferences.setActiveUser(remote.id)
        val profile = saveRemoteProfile(remote.id, remote.name, remote.avatarUrl != null)
        AppResult.Success(profile)
    } catch (error: Throwable) {
        sessionToken.set(null)
        AppResult.Failure(error.toAppError())
    }

    suspend fun syncProfile(): UserProfile {
        val remote = api.me()
        return saveRemoteProfile(remote.id, remote.name, remote.avatarUrl != null)
    }

    suspend fun updateName(name: String): AppResult<UserProfile> = try {
        val remote = api.updateProfile(UpdateUserNameRequest(name.trim()))
        AppResult.Success(saveRemoteProfile(remote.id, remote.name, remote.avatarUrl != null))
    } catch (error: Throwable) {
        AppResult.Failure(error.toAppError())
    }

    suspend fun updateAvatar(pngData: ByteArray): AppResult<UserProfile> = try {
        val body = pngData.toRequestBody("image/png".toMediaType())
        val part = MultipartBody.Part.createFormData("avatar", "avatar.png", body)
        val remote = api.updateAvatar(part)
        AppResult.Success(saveRemoteProfile(remote.id, remote.name, remote.avatarUrl != null))
    } catch (error: Throwable) {
        AppResult.Failure(error.toAppError())
    }

    suspend fun logout() {
        val id = _currentUser.value?.id ?: preferences.activeUserId.first()
        if (id != null) tokens.delete(id)
        sessionToken.set(null)
        preferences.setActiveUser(null)
        _currentUser.value = null
    }

    private suspend fun saveRemoteProfile(id: Long, name: String, hasAvatar: Boolean): UserProfile {
        val avatarPath = if (hasAvatar) downloadAvatar(id) else null
        val profile = UserProfile(id, name, avatarPath)
        preferences.saveProfile(profile)
        _currentUser.value = profile
        return profile
    }

    private suspend fun downloadAvatar(userId: Long): String? = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.avatar()
            if (!response.isSuccessful) return@runCatching null
            val body = response.body() ?: return@runCatching null
            val directory = File(context.filesDir, "users/$userId/avatar").apply { mkdirs() }
            val temporary = File(directory, "avatar.tmp")
            val target = File(directory, "avatar.img")
            body.use { responseBody -> temporary.outputStream().use { responseBody.byteStream().copyTo(it) } }
            if (target.exists() && !target.delete()) error("Unable to replace the cached avatar.")
            check(temporary.renameTo(target))
            target.absolutePath
        }.getOrNull()
    }
}
