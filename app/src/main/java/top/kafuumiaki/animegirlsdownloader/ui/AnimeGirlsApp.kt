package top.kafuumiaki.animegirlsdownloader.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.kafuumiaki.animegirlsdownloader.AppContainer
import top.kafuumiaki.animegirlsdownloader.R
import top.kafuumiaki.animegirlsdownloader.download.DownloadEvent
import top.kafuumiaki.animegirlsdownloader.ui.browse.BrowseEvent
import top.kafuumiaki.animegirlsdownloader.ui.browse.BrowseScreen
import top.kafuumiaki.animegirlsdownloader.ui.browse.BrowseViewModel
import top.kafuumiaki.animegirlsdownloader.ui.downloads.DownloadsScreen
import top.kafuumiaki.animegirlsdownloader.ui.downloads.DownloadsViewModel
import top.kafuumiaki.animegirlsdownloader.ui.settings.SettingsScreen
import top.kafuumiaki.animegirlsdownloader.ui.theme.AnimeGirlsTheme
import top.kafuumiaki.animegirlsdownloader.ui.upload.UploadEvent
import top.kafuumiaki.animegirlsdownloader.ui.upload.UploadScreen
import top.kafuumiaki.animegirlsdownloader.ui.upload.UploadViewModel
import java.util.Locale

private enum class Destination(val label: Int, val icon: ImageVector) {
    BROWSE(R.string.nav_browse, Icons.Default.Explore),
    DOWNLOADS(R.string.nav_downloads, Icons.Default.Download),
    UPLOAD(R.string.nav_upload, Icons.Default.CloudUpload),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings),
}

@Composable
fun AnimeGirlsApp(
    appViewModel: AppViewModel,
    browseViewModel: BrowseViewModel,
    downloadsViewModel: DownloadsViewModel,
    uploadViewModel: UploadViewModel,
    container: AppContainer,
    initialDownloads: Boolean,
) {
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val downloads by downloadsViewModel.state.collectAsStateWithLifecycle()
    val user by appViewModel.user.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(if (initialDownloads) Destination.DOWNLOADS else Destination.BROWSE) }
    var showLogin by rememberSaveable { mutableStateOf(false) }
    var openUploadAfterLogin by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    // Preserve the Activity owner before replacing LocalContext with the locale-specific context.
    // Activity result launchers (photo/folder pickers) otherwise cannot register and crash as soon
    // as Settings or Upload enters composition.
    val activityResultRegistryOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "AnimeGirlsApp must be hosted by an ActivityResultRegistryOwner."
    }
    val systemLocaleTag = Resources.getSystem().configuration.locales[0].toLanguageTag()
    val localizedConfiguration = remember(settings.localeTag, systemLocaleTag) {
        Configuration(baseConfiguration).apply {
            val locale = if (settings.localeTag.isBlank()) Locale.forLanguageTag(systemLocaleTag)
            else Locale.forLanguageTag(settings.localeTag)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
    }
    val context = remember(baseContext, settings.localeTag, systemLocaleTag) {
        baseContext.createConfigurationContext(localizedConfiguration)
    }
    val uriHandler = LocalUriHandler.current
    val updateState by appViewModel.updateState.collectAsStateWithLifecycle()
    val navigate: (Destination) -> Unit = { target ->
        if (target == Destination.UPLOAD && user == null) {
            openUploadAfterLogin = true
            showLogin = true
        } else {
            openUploadAfterLogin = false
            destination = target
        }
    }

    LaunchedEffect(browseViewModel, context) {
        browseViewModel.events.collect { event ->
            val message = when (event) {
                is BrowseEvent.Added -> context.getString(R.string.download_added, event.imageId)
                BrowseEvent.Copied -> context.getString(R.string.copy_success)
            }
            snackbar.showSnackbar(message)
        }
    }
    LaunchedEffect(uploadViewModel, context) {
        uploadViewModel.events.collect { event ->
            when (event) {
                UploadEvent.LoginRequired -> showLogin = true
                is UploadEvent.Finished -> snackbar.showSnackbar(context.getString(R.string.upload_complete, event.success, event.failed))
            }
        }
    }
    LaunchedEffect(container, context) {
        container.downloadEvents.events.collect { event ->
            val message = when (event) {
                is DownloadEvent.Success -> context.getString(R.string.download_success, event.fileName, event.destination)
                is DownloadEvent.Failure -> context.getString(R.string.download_failed, event.fileName)
            }
            snackbar.showSnackbar(message)
        }
    }
    LaunchedEffect(user?.id, destination) {
        if (destination == Destination.UPLOAD && user == null) {
            destination = Destination.BROWSE
            showLogin = true
        }
    }

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides localizedConfiguration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
    ) {
      AnimeGirlsTheme(settings.themeMode, settings.dynamicColor) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            Destination.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = destination == item,
                                    onClick = { navigate(item) },
                                    icon = { NavigationIcon(item, downloads.badgeCount, downloads.hasFailures, downloads.hasActiveTasks) },
                                    label = { Text(stringResource(item.label)) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (useRail) {
                        NavigationRail {
                            Destination.entries.forEach { item ->
                                NavigationRailItem(
                                    selected = destination == item,
                                    onClick = { navigate(item) },
                                    icon = { NavigationIcon(item, downloads.badgeCount, downloads.hasFailures, downloads.hasActiveTasks) },
                                    label = { Text(stringResource(item.label)) },
                                )
                            }
                        }
                    }
                    when (destination) {
                        Destination.BROWSE -> BrowseScreen(browseViewModel, appViewModel, useRail, Modifier.weight(1f))
                        Destination.DOWNLOADS -> DownloadsScreen(downloadsViewModel, Modifier.weight(1f))
                        Destination.UPLOAD -> if (user != null) UploadScreen(uploadViewModel, useRail, Modifier.weight(1f))
                        else BrowseScreen(browseViewModel, appViewModel, useRail, Modifier.weight(1f))
                        Destination.SETTINGS -> SettingsScreen(appViewModel, onLogin = { showLogin = true }, Modifier.weight(1f))
                    }
                }
            }
        }

        if (showLogin) {
            LoginDialog(
                onDismiss = {
                    showLogin = false
                    openUploadAfterLogin = false
                },
                onSubmit = { name, password, register, complete ->
                    appViewModel.login(name, password, register) { success ->
                        complete(success)
                        if (success) {
                            showLogin = false
                            if (openUploadAfterLogin) destination = Destination.UPLOAD
                            openUploadAfterLogin = false
                        }
                    }
                },
            )
        }

        if (updateState is UpdateUiState.Available) {
            val update = (updateState as UpdateUiState.Available).info
            androidx.compose.material3.AlertDialog(
                onDismissRequest = appViewModel::dismissUpdate,
                title = { Text(stringResource(R.string.update_available, update.version)) },
                text = { Text(stringResource(R.string.update_available_body)) },
                dismissButton = { androidx.compose.material3.TextButton(onClick = appViewModel::dismissUpdate) { Text(stringResource(R.string.action_later)) } },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = {
                        uriHandler.openUri(update.downloadUrl ?: update.releaseUrl)
                        appViewModel.dismissUpdate()
                    }) { Text(stringResource(R.string.action_download_update)) }
                },
            )
        }
      }
    }
}

@Composable
private fun NavigationIcon(destination: Destination, count: Int, hasFailures: Boolean, hasActiveTasks: Boolean) {
    if (destination != Destination.DOWNLOADS || count == 0) {
        Icon(destination.icon, contentDescription = null)
        return
    }
    BadgedBox(
        badge = {
            Badge {
                val text = if (hasFailures && !hasActiveTasks) "×" else if (count > 99) "99+" else count.toString()
                Text(text)
            }
        }
    ) { Icon(destination.icon, contentDescription = null) }
}
