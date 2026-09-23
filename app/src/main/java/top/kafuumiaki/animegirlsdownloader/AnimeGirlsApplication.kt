package top.kafuumiaki.animegirlsdownloader

import android.app.Application
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AnimeGirlsApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setJobSchedulerJobIdRange(30_000, 30_999)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannel()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.database.downloadDao().recoverInterrupted()
            if (container.database.downloadDao().nextPending() != null) {
                container.downloadScheduler.schedule()
            }
        }
    }
}
