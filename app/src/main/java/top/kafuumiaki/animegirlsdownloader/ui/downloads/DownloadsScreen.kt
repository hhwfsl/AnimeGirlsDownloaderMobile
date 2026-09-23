package top.kafuumiaki.animegirlsdownloader.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import top.kafuumiaki.animegirlsdownloader.R
import top.kafuumiaki.animegirlsdownloader.core.database.DownloadTaskEntity
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.tasks.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.downloads_empty_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.downloads_empty_body), modifier = Modifier.padding(top = 4.dp))
            }
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.tasks, key = { it.taskId }) { task ->
            DownloadRow(task, onCancel = { viewModel.cancel(task.taskId) }, onRetry = { viewModel.retry(task.taskId) }, onRemove = { viewModel.remove(task.taskId) })
        }
    }
}

@Composable
private fun DownloadRow(
    task: DownloadTaskEntity,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val percent = if (task.totalBytes > 0) ((task.bytesRead * 100) / task.totalBytes).toInt().coerceIn(0, 100) else 0
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = task.previewUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.status == DownloadStatus.DOWNLOADING && task.totalBytes <= 0) {
                        LinearProgressIndicator(Modifier.weight(1f))
                    } else {
                        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.weight(1f))
                    }
                    Text(
                        when (task.status) {
                            DownloadStatus.PENDING -> stringResource(R.string.download_waiting)
                            DownloadStatus.DOWNLOADING -> stringResource(R.string.download_running, percent)
                            DownloadStatus.FAILED -> stringResource(R.string.download_failed_status, task.error.orEmpty())
                            DownloadStatus.CANCELED -> stringResource(R.string.action_cancel)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            when (task.status) {
                DownloadStatus.FAILED -> {
                    IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, stringResource(R.string.action_retry)) }
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, stringResource(R.string.action_remove)) }
                }
                else -> IconButton(onClick = onCancel) { Icon(Icons.Default.Close, stringResource(R.string.action_cancel)) }
            }
        }
    }
}
