package top.kafuumiaki.animegirlsdownloader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.kafuumiaki.animegirlsdownloader.BuildConfig
import top.kafuumiaki.animegirlsdownloader.AppContainer
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.AppSettings
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ThemeMode
import top.kafuumiaki.animegirlsdownloader.core.model.UserProfile
import top.kafuumiaki.animegirlsdownloader.data.UpdateInfo

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object Latest : UpdateUiState
    data object Failed : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
}

class AppViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.preferences.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val user: StateFlow<UserProfile?> = container.authRepository.currentUser
    private val _updateState = kotlinx.coroutines.flow.MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState

    init {
        viewModelScope.launch { container.authRepository.restore() }
        viewModelScope.launch {
            val last = container.preferences.lastUpdateCheck()
            if (System.currentTimeMillis() - last > 24 * 60 * 60 * 1000L) checkUpdate(silent = true)
        }
    }

    fun login(userName: String, password: String, register: Boolean, complete: (Boolean) -> Unit) {
        viewModelScope.launch {
            complete(container.authRepository.login(userName.trim(), password, register) is AppResult.Success)
        }
    }

    fun logout() { viewModelScope.launch { container.authRepository.logout() } }
    fun setTheme(value: ThemeMode) { viewModelScope.launch { container.preferences.setTheme(value) } }
    fun setDynamicColor(value: Boolean) { viewModelScope.launch { container.preferences.setDynamicColor(value) } }

    fun setLocale(tag: String) {
        viewModelScope.launch { container.preferences.setLocale(tag) }
    }

    fun updateUserName(value: String, complete: (Boolean) -> Unit) {
        viewModelScope.launch {
            complete(container.authRepository.updateName(value.trim()) is AppResult.Success)
        }
    }

    fun updateAvatar(pngData: ByteArray, complete: (Boolean) -> Unit) {
        viewModelScope.launch {
            complete(container.authRepository.updateAvatar(pngData) is AppResult.Success)
        }
    }

    fun setFilters(content: ContentFilter, ai: AiFilter) {
        viewModelScope.launch {
            container.preferences.setFilters(ownerId(), content, ai)
        }
    }

    fun setDownloadTree(uri: String?) {
        viewModelScope.launch { container.preferences.setDownloadTree(ownerId(), uri) }
    }

    fun checkUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _updateState.value = UpdateUiState.Checking
            val result = container.updateRepository.check()
            container.preferences.markUpdateChecked()
            _updateState.value = when {
                result.isFailure && silent -> UpdateUiState.Idle
                result.isFailure -> UpdateUiState.Failed
                result.getOrNull() != null -> UpdateUiState.Available(result.getOrThrow()!!)
                silent -> UpdateUiState.Idle
                else -> UpdateUiState.Latest
            }
        }
    }

    fun dismissUpdate() { _updateState.value = UpdateUiState.Idle }
    fun ownerId(): String = user.value?.id?.toString() ?: "anonymous"
    fun versionName(): String = BuildConfig.VERSION_NAME
}
