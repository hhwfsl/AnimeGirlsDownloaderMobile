package top.kafuumiaki.animegirlsdownloader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import top.kafuumiaki.animegirlsdownloader.core.model.DownloadStatus

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tasks WHERE ownerId = :ownerId ORDER BY createdAt")
    fun observeForOwner(ownerId: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'PENDING' ORDER BY createdAt LIMIT 1")
    suspend fun nextPending(): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun byId(taskId: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, bytesRead = :bytesRead, totalBytes = :totalBytes, error = :error, updatedAt = :now WHERE taskId = :taskId")
    suspend fun updateProgress(taskId: String, status: DownloadStatus, bytesRead: Long, totalBytes: Long, error: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET cancelRequested = 1, updatedAt = :now WHERE taskId = :taskId")
    suspend fun requestCancel(taskId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET status = 'PENDING', error = NULL, cancelRequested = 0, bytesRead = 0, totalBytes = -1, updatedAt = :now WHERE taskId = :taskId")
    suspend fun retry(taskId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET status = 'PENDING', cancelRequested = 0 WHERE status = 'DOWNLOADING'")
    suspend fun recoverInterrupted()

    @Query("DELETE FROM download_tasks WHERE taskId = :taskId")
    suspend fun deleteById(taskId: String)
}
