package top.kafuumiaki.animegirlsdownloader.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.kafuumiaki.animegirlsdownloader.AppContainer
import top.kafuumiaki.animegirlsdownloader.core.model.AppResult
import top.kafuumiaki.animegirlsdownloader.core.model.SourceKind
import top.kafuumiaki.animegirlsdownloader.core.model.UploadSource

data class UploadUiState(
    val sources: List<UploadSource> = emptyList(),
    val creator: String = "",
    val tags: String = "",
    val isNsfw: Boolean = false,
    val isAi: Boolean = false,
    val uploading: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val success: Int = 0,
    val failed: Int = 0,
    val suggestions: List<String> = emptyList(),
) {
    val singleImageMode: Boolean get() = sources.size == 1 && sources.single().kind == SourceKind.IMAGE
    val previewUri: Uri? get() = sources.singleOrNull()?.takeIf { it.kind == SourceKind.IMAGE }?.uri
}

sealed interface UploadEvent {
    data class Finished(val success: Int, val failed: Int) : UploadEvent
    data object LoginRequired : UploadEvent
}

class UploadViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UploadUiState())
    val state = _state.asStateFlow()
    val events = kotlinx.coroutines.flow.MutableSharedFlow<UploadEvent>(extraBufferCapacity = 4)
    private var tagJob: Job? = null

    fun addImages(uris: List<Uri>) {
        val additions = uris.map { container.uploadRepository.source(it, SourceKind.IMAGE) }
        merge(additions)
    }

    fun addFolder(uri: Uri) { merge(listOf(container.uploadRepository.source(uri, SourceKind.FOLDER))) }

    private fun merge(additions: List<UploadSource>) {
        val all = (_state.value.sources + additions).distinctBy { it.uri.toString() }
        _state.value = _state.value.copy(sources = all, tags = if (all.size == 1 && all[0].kind == SourceKind.IMAGE) _state.value.tags else "", suggestions = emptyList())
    }

    fun remove(uri: Uri) {
        val next = _state.value.sources.filterNot { it.uri == uri }
        _state.value = _state.value.copy(sources = next, tags = if (next.size == 1 && next[0].kind == SourceKind.IMAGE) _state.value.tags else "", suggestions = emptyList())
    }

    fun setCreator(value: String) { _state.value = _state.value.copy(creator = value) }
    fun setNsfw(value: Boolean) { _state.value = _state.value.copy(isNsfw = value) }
    fun setAi(value: Boolean) { _state.value = _state.value.copy(isAi = value) }

    fun setTags(value: String) {
        _state.value = _state.value.copy(tags = value)
        tagJob?.cancel()
        if (!_state.value.singleImageMode || container.authRepository.currentUser.value == null) return
        val partial = value.substringAfterLast(' ').trim()
        if (partial.isBlank()) {
            _state.value = _state.value.copy(suggestions = emptyList())
            return
        }
        tagJob = viewModelScope.launch {
            delay(300)
            _state.value = _state.value.copy(suggestions = container.imageRepository.completeTags(partial))
        }
    }

    fun applySuggestion(value: String) {
        val prefix = _state.value.tags.substringBeforeLast(' ', "")
        setTags(listOf(prefix, value).filter { it.isNotBlank() }.joinToString(" ") + " ")
        _state.value = _state.value.copy(suggestions = emptyList())
    }

    fun upload() {
        if (container.authRepository.currentUser.value == null) {
            events.tryEmit(UploadEvent.LoginRequired)
            return
        }
        val snapshot = _state.value
        if (snapshot.uploading || snapshot.sources.isEmpty()) return
        viewModelScope.launch {
            val items = container.uploadRepository.expand(snapshot.sources)
            _state.value = snapshot.copy(uploading = true, current = 0, total = items.size, success = 0, failed = 0)
            var success = 0
            var failed = 0
            items.forEachIndexed { index, source ->
                _state.value = _state.value.copy(current = index + 1)
                val tags = if (snapshot.singleImageMode) snapshot.tags.split(Regex("\\s+")).filter(String::isNotBlank) else emptyList()
                when (container.uploadRepository.upload(source, snapshot.creator, tags, snapshot.isNsfw, snapshot.isAi)) {
                    is AppResult.Success -> success++
                    is AppResult.Failure -> failed++
                }
                _state.value = _state.value.copy(success = success, failed = failed)
            }
            _state.value = _state.value.copy(uploading = false, success = success, failed = failed)
            events.emit(UploadEvent.Finished(success, failed))
        }
    }
}
