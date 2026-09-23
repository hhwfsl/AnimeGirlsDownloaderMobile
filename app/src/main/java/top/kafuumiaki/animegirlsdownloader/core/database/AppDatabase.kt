package top.kafuumiaki.animegirlsdownloader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus

class DatabaseConverters {
    @TypeConverter fun downloadStatusToString(value: DownloadStatus): String = value.name
    @TypeConverter fun stringToDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}

@Database(entities = [DownloadTaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
