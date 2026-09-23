package top.kafuumiaki.animegirlsdownloader.ui.upload

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import top.kafuumiaki.animegirlsdownloader.R
import top.kafuumiaki.animegirlsdownloader.core.model.SourceKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(viewModel: UploadViewModel, wideNavigation: Boolean, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        viewModel.addImages(uris)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.addFolder(it)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
        if (expanded) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                UploadPreview(state, Modifier.weight(1f).fillMaxHeight())
                UploadForm(state, viewModel, onAdd = { showAdd = true }, Modifier.weight(1f).fillMaxHeight(), scrollable = true)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UploadPreview(state, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp))
                UploadForm(state, viewModel, onAdd = { showAdd = true }, scrollable = false)
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.upload_add_images)) },
                leadingContent = { Icon(Icons.Default.Image, null) },
                trailingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.fillMaxWidth().clickable {
                    showAdd = false
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.upload_add_folder)) },
                leadingContent = { Icon(Icons.Default.Folder, null) },
                trailingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.fillMaxWidth().clickable { showAdd = false; folderPicker.launch(null) },
            )
        }
    }
}

@Composable
private fun UploadPreview(state: UploadUiState, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val uri = state.previewUri
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.preview_single_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Image, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.preview_unavailable), Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadForm(
    state: UploadUiState,
    viewModel: UploadViewModel,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean,
) {
    val containerModifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(containerModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.upload_sources), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = onAdd, enabled = !state.uploading) {
                Icon(Icons.Default.Add, null)
                Text(stringResource(R.string.action_add), Modifier.padding(start = 6.dp))
            }
        }
        if (state.sources.isEmpty()) {
            Text(stringResource(R.string.upload_sources_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(Modifier.fillMaxWidth()) {
                state.sources.forEach { source ->
                    ListItem(
                        leadingContent = { Icon(if (source.kind == SourceKind.IMAGE) Icons.Default.Image else Icons.Default.Folder, null) },
                        headlineContent = { Text(source.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(source.uri.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.remove(source.uri) }, enabled = !state.uploading) {
                                Icon(Icons.Default.Close, stringResource(R.string.action_remove))
                            }
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.creator,
            onValueChange = viewModel::setCreator,
            label = { Text(stringResource(R.string.upload_creator)) },
            enabled = !state.uploading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.tags,
            onValueChange = viewModel::setTags,
            label = { Text(stringResource(R.string.upload_tags)) },
            supportingText = if (state.singleImageMode) null else {
                { Text(stringResource(R.string.upload_tags_single_only)) }
            },
            enabled = state.singleImageMode && !state.uploading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.suggestions.forEach { tag -> AssistChip(onClick = { viewModel.applySuggestion(tag) }, label = { Text(tag) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.isNsfw, onCheckedChange = viewModel::setNsfw, enabled = !state.uploading)
                Text(stringResource(R.string.upload_nsfw))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.isAi, onCheckedChange = viewModel::setAi, enabled = !state.uploading)
                Text(stringResource(R.string.upload_ai))
            }
        }
        if (state.uploading) {
            LinearProgressIndicator(progress = { if (state.total == 0) 0f else state.current.toFloat() / state.total }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.upload_progress, state.current, state.total))
        }
        Button(
            onClick = viewModel::upload,
            enabled = state.sources.isNotEmpty() && !state.uploading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.uploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Upload, null)
            Text(stringResource(R.string.action_upload), Modifier.padding(start = 8.dp))
        }
    }
}
