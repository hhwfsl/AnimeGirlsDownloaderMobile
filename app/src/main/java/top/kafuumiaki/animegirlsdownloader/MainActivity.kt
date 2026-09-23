package top.kafuumiaki.animegirlsdownloader

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import top.kafuumiaki.animegirlsdownloader.ui.AnimeGirlsApp
import top.kafuumiaki.animegirlsdownloader.ui.AppViewModel
import top.kafuumiaki.animegirlsdownloader.ui.browse.BrowseViewModel
import top.kafuumiaki.animegirlsdownloader.ui.downloads.DownloadsViewModel
import top.kafuumiaki.animegirlsdownloader.ui.upload.UploadViewModel

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val container = (application as AnimeGirlsApplication).container
        val factory = ContainerViewModelFactory(container)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = factory)
            val browseViewModel: BrowseViewModel = viewModel(factory = factory)
            val downloadsViewModel: DownloadsViewModel = viewModel(factory = factory)
            val uploadViewModel: UploadViewModel = viewModel(factory = factory)
            AnimeGirlsApp(
                appViewModel = appViewModel,
                browseViewModel = browseViewModel,
                downloadsViewModel = downloadsViewModel,
                uploadViewModel = uploadViewModel,
                container = container,
                initialDownloads = intent?.getStringExtra("destination") == "downloads",
            )
        }
    }
}

private class ContainerViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(container)
        modelClass.isAssignableFrom(BrowseViewModel::class.java) -> BrowseViewModel(container)
        modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> DownloadsViewModel(container)
        modelClass.isAssignableFrom(UploadViewModel::class.java) -> UploadViewModel(container)
        else -> error("Unknown ViewModel: ${modelClass.name}")
    } as T
}
