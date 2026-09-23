package top.kafuumiaki.animegirlsdownloader

import android.content.Context
import androidx.room.Room
import top.kafuumiaki.animegirlsdownloader.core.database.AppDatabase
import top.kafuumiaki.animegirlsdownloader.core.network.NetworkClient
import top.kafuumiaki.animegirlsdownloader.core.network.SessionTokenProvider
import top.kafuumiaki.animegirlsdownloader.core.preferences.AppPreferences
import top.kafuumiaki.animegirlsdownloader.core.security.SecureTokenStore
import top.kafuumiaki.animegirlsdownloader.data.AuthRepository
import top.kafuumiaki.animegirlsdownloader.data.ClipboardHelper
import top.kafuumiaki.animegirlsdownloader.data.DownloadRepository
import top.kafuumiaki.animegirlsdownloader.data.ImageRepository
import top.kafuumiaki.animegirlsdownloader.data.UpdateRepository
import top.kafuumiaki.animegirlsdownloader.data.UploadRepository
import top.kafuumiaki.animegirlsdownloader.download.DownloadEvents
import top.kafuumiaki.animegirlsdownloader.download.DownloadNotifications
import top.kafuumiaki.animegirlsdownloader.download.DownloadQueueProcessor
import top.kafuumiaki.animegirlsdownloader.download.DownloadQueueScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val preferences = AppPreferences(appContext)
    private val tokenProvider = SessionTokenProvider()
    private val secureTokens = SecureTokenStore(appContext)
    private val network = NetworkClient.create(tokenProvider)
    val api = network.first
    val httpClient = network.second
    val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "downloads.db").build()

    val authRepository = AuthRepository(appContext, api, preferences, secureTokens, tokenProvider)
    val imageRepository = ImageRepository(api)
    val uploadRepository = UploadRepository(appContext, appContext.contentResolver, api)
    val updateRepository = UpdateRepository(httpClient)
    val clipboardHelper = ClipboardHelper(appContext)

    val downloadEvents = DownloadEvents()
    val notifications = DownloadNotifications(appContext)
    val downloadScheduler = DownloadQueueScheduler(appContext)
    val downloadProcessor = DownloadQueueProcessor(
        appContext,
        httpClient,
        database.downloadDao(),
        notifications,
        downloadEvents,
    )
    val downloadRepository = DownloadRepository(database.downloadDao(), downloadScheduler)
}
