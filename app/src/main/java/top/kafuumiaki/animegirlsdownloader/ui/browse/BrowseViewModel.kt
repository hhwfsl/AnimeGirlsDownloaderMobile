package top.kafuumiaki.animegirlsdownloader.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.kafuumiaki.animegirlsdownloader.AppContainer
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.AppError
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ImageInfo

sealed interface BrowseState {
    data object Empty : BrowseState
    data class Loading(val requestId: Long) : BrowseState
    data class Previewing(val image: ImageInfo, val requestId: Long) : BrowseState
    data class Ready(val image: ImageInfo, val requestId: Long) : BrowseState
    data class Error(val error: AppError, val requestId: Long) : BrowseState
}

sealed interface BrowseEvent {
    data class Added(val imageId: String) : BrowseEvent
    data object Copied : BrowseEvent
}

class BrowseViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<BrowseState>(BrowseState.Empty)
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<BrowseEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    private var requestId = 0L
    private var requestJob: Job? = null

    fun load(query: String, content: ContentFilter, ai: AiFilter) {
        requestJob?.cancel()
        val id = ++requestId
        _state.value = BrowseState.Loading(id)
        requestJob = viewModelScope.launch {
            when (val result = container.imageRepository.find(query, content, ai)) {
                is AppResult.Success -> if (id == requestId) _state.value = BrowseState.Previewing(result.value, id)
                is AppResult.Failure -> if (id == requestId) _state.value = BrowseState.Error(result.error, id)
            }
        }
    }

    fun previewLoaded(request: Long) {
        val current = _state.value
        if (current is BrowseState.Previewing && current.requestId == request) {
            _state.value = BrowseState.Ready(current.image, request)
        }
    }

    fun previewFailed(request: Long) {
        val current = _state.value
        if (current is BrowseState.Previewing && current.requestId == request) {
            _state.value = BrowseState.Error(AppError(AppError.Code.NETWORK), request)
        }
    }

    fun enqueueDownload() {
        val image = (_state.value as? BrowseState.Ready)?.image ?: return
        viewModelScope.launch {
            val settings = container.preferences.settings.first()
            val owner = container.authRepository.currentUser.value?.id?.toString() ?: "anonymous"
            container.downloadRepository.enqueue(owner, image, settings.downloadTreeUri)
            _events.emit(BrowseEvent.Added(image.id))
        }
    }

    fun copyLink() {
        val image = (_state.value as? BrowseState.Ready)?.image ?: return
        container.clipboardHelper.copyLink(container.imageRepository.publicImageUrl(image.id))
        _events.tryEmit(BrowseEvent.Copied)
    }

}
