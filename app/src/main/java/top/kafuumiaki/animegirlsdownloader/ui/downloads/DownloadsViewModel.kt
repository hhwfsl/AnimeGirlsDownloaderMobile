package top.kafuumiaki.animegirlsdownloader.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import top.kafuumiaki.animegirlsdownloader.AppContainer
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadTaskEntity

data class DownloadListState(
    val tasks: List<DownloadTaskEntity> = emptyList(),
    val badgeCount: Int = 0,
    val hasFailures: Boolean = false,
    val hasActiveTasks: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<DownloadListState> = container.authRepository.currentUser
        .map { it?.id?.toString() ?: "anonymous" }
        .flatMapLatest { owner -> container.downloadRepository.observe(owner) }
        .map { tasks ->
            DownloadListState(
                tasks = tasks,
                badgeCount = tasks.size,
                hasFailures = tasks.any { it.status.name == "FAILED" },
                hasActiveTasks = tasks.any { it.status.name != "FAILED" },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DownloadListState())

    fun cancel(taskId: String) { viewModelScope.launch { container.downloadRepository.cancel(taskId) } }
    fun retry(taskId: String) { viewModelScope.launch { container.downloadRepository.retry(taskId) } }
    fun remove(taskId: String) { viewModelScope.launch { container.downloadRepository.remove(taskId) } }
}
