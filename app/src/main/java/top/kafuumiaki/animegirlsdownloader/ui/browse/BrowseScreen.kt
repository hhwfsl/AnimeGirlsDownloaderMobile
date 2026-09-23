package top.kafuumiaki.animegirlsdownloader.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import top.kafuumiaki.animegirlsdownloader.R
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ImageInfo
import top.kafuumiaki.animegirlsdownloader.ui.AppViewModel

@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    appViewModel: AppViewModel,
    wideNavigation: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (state is BrowseState.Empty) viewModel.load("", settings.contentFilter, settings.aiFilter) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
        if (expanded) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PreviewPane(
                    state = state,
                    viewModel = viewModel,
                    onDownload = viewModel::enqueueDownload,
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                )
                BrowseControls(
                    state = state,
                    content = settings.contentFilter,
                    ai = settings.aiFilter,
                    onContent = { appViewModel.setFilters(it, settings.aiFilter); viewModel.load("", it, settings.aiFilter) },
                    onAi = { appViewModel.setFilters(settings.contentFilter, it); viewModel.load("", settings.contentFilter, it) },
                    onSearch = { showSearch = true },
                    onRandom = { viewModel.load("", settings.contentFilter, settings.aiFilter) },
                    onCopyLink = viewModel::copyLink,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    scrollable = true,
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewPane(
                    state = state,
                    viewModel = viewModel,
                    onDownload = viewModel::enqueueDownload,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).aspectRatio(1f),
                )
                BrowseControls(
                    state = state,
                    content = settings.contentFilter,
                    ai = settings.aiFilter,
                    onContent = { appViewModel.setFilters(it, settings.aiFilter); viewModel.load("", it, settings.aiFilter) },
                    onAi = { appViewModel.setFilters(settings.contentFilter, it); viewModel.load("", settings.contentFilter, it) },
                    onSearch = { showSearch = true },
                    onRandom = { viewModel.load("", settings.contentFilter, settings.aiFilter) },
                    onCopyLink = viewModel::copyLink,
                    scrollable = false,
                )
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            onDismiss = { showSearch = false },
            onSearch = {
                showSearch = false
                viewModel.load(it, settings.contentFilter, settings.aiFilter)
            },
        )
    }
}

@Composable
private fun PreviewPane(
    state: BrowseState,
    viewModel: BrowseViewModel,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = when (state) {
        is BrowseState.Previewing -> state.image
        is BrowseState.Ready -> state.image
        else -> null
    }
    val ready = state is BrowseState.Ready
    var showFullScreen by remember { mutableStateOf(false) }
    LaunchedEffect(image?.id) { showFullScreen = false }

    ElevatedCard(modifier) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                val requestId = when (state) {
                    is BrowseState.Previewing -> state.requestId
                    is BrowseState.Ready -> state.requestId
                    else -> -1L
                }
                AsyncImage(
                    model = image.previewUrl,
                    contentDescription = stringResource(R.string.content_description_preview),
                    contentScale = ContentScale.Fit,
                    onSuccess = { viewModel.previewLoaded(requestId) },
                    onError = { viewModel.previewFailed(requestId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = ready) { showFullScreen = true }
                        .padding(4.dp),
                )
            }
            when (state) {
                is BrowseState.Loading, is BrowseState.Previewing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.browse_loading), Modifier.padding(top = 12.dp))
                }
                is BrowseState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.browse_error), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (state.error.code) {
                            top.kafuumiaki.animegirlsdownloader.core.model.AppError.Code.NOT_FOUND -> stringResource(R.string.error_not_found)
                            top.kafuumiaki.animegirlsdownloader.core.model.AppError.Code.NETWORK -> stringResource(R.string.error_network)
                            else -> stringResource(R.string.error_unknown)
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                BrowseState.Empty -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(stringResource(R.string.browse_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.browse_empty_body), Modifier.padding(top = 8.dp))
                }
                is BrowseState.Ready -> Unit
            }
            FilledIconButton(
                onClick = onDownload,
                enabled = ready,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Default.Download, stringResource(R.string.action_download))
            }
            if (ready) {
                FilledTonalIconButton(
                    onClick = { showFullScreen = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Icon(Icons.Default.ZoomIn, stringResource(R.string.action_view_fullscreen))
                }
            }
        }
    }

    if (showFullScreen && image != null) {
        FullScreenPreview(image.previewUrl) { showFullScreen = false }
    }
}

@Composable
private fun FullScreenPreview(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.content_description_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(12.dp).clickable(onClick = {}),
            )
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.action_close))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowseControls(
    state: BrowseState,
    content: ContentFilter,
    ai: AiFilter,
    onContent: (ContentFilter) -> Unit,
    onAi: (AiFilter) -> Unit,
    onSearch: () -> Unit,
    onRandom: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean,
) {
    val ready = state as? BrowseState.Ready
    val containerModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(containerModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.filter_content), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContentFilter.entries.forEach { value ->
                FilterChip(
                    selected = content == value,
                    onClick = { onContent(value) },
                    label = { Text(stringResource(when (value) {
                        ContentFilter.SFW -> R.string.filter_sfw
                        ContentFilter.NSFW -> R.string.filter_nsfw
                        ContentFilter.ALL -> R.string.filter_all
                    })) },
                )
            }
        }
        Text(stringResource(R.string.filter_ai), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiFilter.entries.forEach { value ->
                FilterChip(
                    selected = ai == value,
                    onClick = { onAi(value) },
                    label = { Text(stringResource(when (value) {
                        AiFilter.EXCLUDE_AI -> R.string.filter_no_ai
                        AiFilter.AI_ONLY -> R.string.filter_ai_only
                        AiFilter.ALL -> R.string.filter_all
                    })) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Search, null)
                Text(stringResource(R.string.action_search), Modifier.padding(start = 8.dp))
            }
            Button(onClick = onRandom, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, null)
                Text(stringResource(R.string.action_random), Modifier.padding(start = 8.dp))
            }
        }
        ready?.image?.let { ImageDetails(it) }
        OutlinedButton(onClick = onCopyLink, enabled = ready != null, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Link, null)
            Text(stringResource(R.string.action_copy_link), Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageDetails(image: ImageInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.image_id, image.id), style = MaterialTheme.typography.titleMedium)
        AssistChip(onClick = {}, label = { Text(stringResource(if (image.isAiGenerated) R.string.image_ai else R.string.image_not_ai)) })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            image.tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
    }
}

@Composable
private fun SearchDialog(onDismiss: () -> Unit, onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_title)) },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        confirmButton = { Button(onClick = { onSearch(query) }) { Text(stringResource(R.string.action_search)) } },
    )
}
