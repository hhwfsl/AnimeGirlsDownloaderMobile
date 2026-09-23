package top.kafuumiaki.animegirlsdownloader.download

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface DownloadEvent {
    data class Success(val fileName: String, val destination: String) : DownloadEvent
    data class Failure(val fileName: String, val reason: String) : DownloadEvent
}

class DownloadEvents {
    private val mutableEvents = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()
    fun emit(event: DownloadEvent) { mutableEvents.tryEmit(event) }
}
