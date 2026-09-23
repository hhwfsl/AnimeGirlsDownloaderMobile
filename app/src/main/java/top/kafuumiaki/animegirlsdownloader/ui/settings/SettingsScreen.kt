package top.kafuumiaki.animegirlsdownloader.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import top.kafuumiaki.animegirlsdownloader.BuildConfig
import top.kafuumiaki.animegirlsdownloader.R
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ThemeMode
import top.kafuumiaki.animegirlsdownloader.data.AvatarImageProcessor
import top.kafuumiaki.animegirlsdownloader.ui.AppViewModel
import top.kafuumiaki.animegirlsdownloader.ui.UpdateUiState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(appViewModel: AppViewModel, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val user by appViewModel.user.collectAsStateWithLifecycle()
    val update by appViewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var editName by remember { mutableStateOf(false) }
    var pendingAvatar by remember { mutableStateOf<Uri?>(null) }
    var profileSaving by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingAvatar = uri
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            appViewModel.setDownloadTree(it.toString())
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSection(stringResource(R.string.settings_account)) {
                val profile = user
                if (profile == null) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.AccountCircle, null, Modifier.size(44.dp)) },
                        headlineContent = { Text(stringResource(R.string.account_signed_out)) },
                        trailingContent = { Button(onClick = onLogin) { Text(stringResource(R.string.action_login)) } },
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(52.dp).clickable(enabled = !profileSaving) {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            if (profile.avatarPath != null) {
                                AsyncImage(
                                    model = File(profile.avatarPath),
                                    contentDescription = stringResource(R.string.content_description_avatar),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                )
                            } else Icon(Icons.Default.AccountCircle, null, Modifier.size(48.dp))
                            Icon(
                                Icons.Default.PhotoCamera,
                                stringResource(R.string.action_change_avatar),
                                modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { editName = true }, enabled = !profileSaving) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.action_edit_name))
                                }
                            }
                            Text("ID: ${profile.id}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = appViewModel::logout, enabled = !profileSaving) {
                            Text(stringResource(R.string.action_logout))
                        }
                    }
                    profileMessage?.let { (message, error) ->
                        Text(
                            stringResource(message),
                            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_content)) {
                Text(stringResource(R.string.filter_content), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ContentFilter.entries.forEach { value ->
                        FilterChip(
                            selected = settings.contentFilter == value,
                            onClick = { appViewModel.setFilters(value, settings.aiFilter) },
                            label = { Text(stringResource(when (value) {
                                ContentFilter.SFW -> R.string.filter_sfw
                                ContentFilter.NSFW -> R.string.filter_nsfw
                                ContentFilter.ALL -> R.string.filter_all
                            })) },
                        )
                    }
                }
                Text(stringResource(R.string.filter_ai), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiFilter.entries.forEach { value ->
                        FilterChip(
                            selected = settings.aiFilter == value,
                            onClick = { appViewModel.setFilters(settings.contentFilter, value) },
                            label = { Text(stringResource(when (value) {
                                AiFilter.EXCLUDE_AI -> R.string.filter_no_ai
                                AiFilter.AI_ONLY -> R.string.filter_ai_only
                                AiFilter.ALL -> R.string.filter_all
                            })) },
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_appearance)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { value ->
                        FilterChip(
                            selected = settings.themeMode == value,
                            onClick = { appViewModel.setTheme(value) },
                            label = { Text(stringResource(when (value) {
                                ThemeMode.SYSTEM -> R.string.settings_theme_system
                                ThemeMode.LIGHT -> R.string.settings_theme_light
                                ThemeMode.DARK -> R.string.settings_theme_dark
                            })) },
                        )
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = appViewModel::setDynamicColor) },
                )
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to R.string.language_system, "en" to R.string.language_english, "zh-CN" to R.string.language_chinese, "ja" to R.string.language_japanese).forEach { (tag, label) ->
                        FilterChip(selected = settings.localeTag == tag, onClick = { appViewModel.setLocale(tag) }, label = { Text(stringResource(label)) })
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_download)) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Folder, null) },
                    headlineContent = { Text(stringResource(R.string.settings_download_location)) },
                    supportingContent = { Text(settings.downloadTreeUri ?: stringResource(R.string.settings_download_location_default)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    modifier = Modifier.clickable { folderPicker.launch(null) },
                )
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                val updateStatus = when (update) {
                    UpdateUiState.Checking -> R.string.action_check_update to false
                    UpdateUiState.Latest -> R.string.update_latest to false
                    UpdateUiState.Failed -> R.string.update_failed to true
                    else -> null
                }
                val updateSupportingContent: (@Composable () -> Unit)? = updateStatus?.let { (message, error) ->
                    {
                        if (error) {
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(message))
                        }
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version, appViewModel.versionName())) },
                    supportingContent = updateSupportingContent,
                    trailingContent = {
                        IconButton(onClick = { appViewModel.checkUpdate() }, enabled = update !is UpdateUiState.Checking) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.action_check_update))
                        }
                    },
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp, horizontal = 48.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { uriHandler.openUri(BuildConfig.GITHUB_PROJECT_URL) }.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Source, stringResource(R.string.content_description_github))
                    Text(stringResource(R.string.settings_project), Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    val profile = user
    if (editName && profile != null) {
        EditNameDialog(
            initialName = profile.name,
            saving = profileSaving,
            onDismiss = { if (!profileSaving) editName = false },
            onSave = { value ->
                profileSaving = true
                profileMessage = null
                appViewModel.updateUserName(value) { success ->
                    profileSaving = false
                    profileMessage = (if (success) R.string.profile_update_success else R.string.profile_update_failed) to !success
                    if (success) editName = false
                }
            },
        )
    }

    pendingAvatar?.let { uri ->
        AvatarCropDialog(
            uri = uri,
            saving = profileSaving,
            onDismiss = { if (!profileSaving) pendingAvatar = null },
            onSave = { pngData ->
                profileSaving = true
                profileMessage = null
                appViewModel.updateAvatar(pngData) { success ->
                    profileSaving = false
                    profileMessage = (if (success) R.string.profile_update_success else R.string.profile_update_failed) to !success
                    if (success) pendingAvatar = null
                }
            },
        )
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val valid = name.trim().length in 1..50
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text(stringResource(R.string.login_username)) },
                singleLine = true,
                enabled = !saving,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.action_cancel)) }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim()) }, enabled = valid && !saving) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.action_save))
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AvatarCropDialog(
    uri: Uri,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaded by produceState<Result<Bitmap>?>(initialValue = null, uri) {
        value = runCatching { withContext(Dispatchers.IO) { AvatarImageProcessor.decode(context.contentResolver, uri) } }
    }
    val bitmap = loaded?.getOrNull()
    DisposableEffect(bitmap) {
        onDispose { bitmap?.recycle() }
    }
    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }
    var encoding by remember(uri) { mutableStateOf(false) }
    var cropError by remember(uri) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving && !encoding) onDismiss() },
        title = { Text(stringResource(R.string.profile_crop_avatar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    loaded == null -> CircularProgressIndicator()
                    bitmap == null -> Text(stringResource(R.string.profile_avatar_invalid), color = MaterialTheme.colorScheme.error)
                    else -> {
                        val image = remember(bitmap) { bitmap.asImageBitmap() }
                        Canvas(
                            Modifier
                                .size(260.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(bitmap, saving, encoding) {
                                    if (saving || encoding) return@pointerInput
                                    detectTransformGestures { _, pan, zoomChange, _ ->
                                        zoom = (zoom * zoomChange).coerceIn(1f, 3f)
                                        val width = size.width.coerceAtLeast(1).toFloat()
                                        val height = size.height.coerceAtLeast(1).toFloat()
                                        offsetX = (offsetX + pan.x * 2f / width).coerceIn(-1f, 1f)
                                        offsetY = (offsetY + pan.y * 2f / height).coerceIn(-1f, 1f)
                                    }
                                }
                                .pointerInput(saving, encoding) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Scroll && !saving && !encoding) {
                                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                if (delta != 0f) {
                                                    val factor = if (delta < 0f) 1.12f else 0.89f
                                                    zoom = (zoom * factor).coerceIn(1f, 3f)
                                                }
                                            }
                                        }
                                    }
                                }
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {
                            val crop = AvatarImageProcessor.cropRect(bitmap, zoom, offsetX, offsetY)
                            drawImage(
                                image = image,
                                srcOffset = IntOffset(crop.left, crop.top),
                                srcSize = IntSize(crop.width(), crop.height()),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                filterQuality = FilterQuality.High,
                            )
                        }
                        Text(
                            stringResource(R.string.profile_crop_gesture_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (cropError) {
                            Text(stringResource(R.string.profile_avatar_invalid), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving && !encoding) { Text(stringResource(R.string.action_cancel)) }
        },
        confirmButton = {
            Button(
                onClick = {
                    val source = bitmap ?: return@Button
                    encoding = true
                    cropError = false
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.Default) {
                                AvatarImageProcessor.encodeCircularPng(source, zoom, offsetX, offsetY)
                            }
                        }
                        encoding = false
                        result.onSuccess(onSave).onFailure { cropError = true }
                    }
                },
                enabled = bitmap != null && !saving && !encoding,
            ) {
                if (saving || encoding) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.action_save))
            }
        },
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}
