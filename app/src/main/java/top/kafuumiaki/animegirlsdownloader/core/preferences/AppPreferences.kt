package top.kafuumiaki.animegirlsdownloader.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import top.kafuumiaki.animegirlsdownloader.core.model.AiFilter
import top.kafuumiaki.animegirlsdownloader.core.model.AppSettings
import top.kafuumiaki.animegirlsdownloader.core.model.ContentFilter
import top.kafuumiaki.animegirlsdownloader.core.model.ThemeMode
import top.kafuumiaki.animegirlsdownloader.core.model.UserProfile

private val Context.dataStore by preferencesDataStore("app_preferences")

class AppPreferences(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val locale = stringPreferencesKey("locale")
        val activeUserId = longPreferencesKey("active_user_id")
        val lastUpdateCheck = longPreferencesKey("last_update_check")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { values ->
        val owner = values[Keys.activeUserId]?.toString() ?: "anonymous"
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(values[Keys.theme] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = values[Keys.dynamicColor] ?: true,
            localeTag = values[Keys.locale] ?: "",
            contentFilter = runCatching { ContentFilter.entries[values[intPreferencesKey("content_$owner")] ?: 0] }.getOrDefault(ContentFilter.SFW),
            aiFilter = runCatching { AiFilter.entries[values[intPreferencesKey("ai_$owner")] ?: 0] }.getOrDefault(AiFilter.EXCLUDE_AI),
            downloadTreeUri = values[stringPreferencesKey("download_tree_$owner")],
        )
    }

    val activeUserId: Flow<Long?> = context.dataStore.data.map { it[Keys.activeUserId] }

    suspend fun setTheme(value: ThemeMode) { context.dataStore.edit { it[Keys.theme] = value.name } }
    suspend fun setDynamicColor(value: Boolean) { context.dataStore.edit { it[Keys.dynamicColor] = value } }
    suspend fun setLocale(value: String) { context.dataStore.edit { it[Keys.locale] = value } }
    suspend fun setActiveUser(id: Long?) {
        context.dataStore.edit { values -> if (id == null) values.remove(Keys.activeUserId) else values[Keys.activeUserId] = id }
    }

    suspend fun setFilters(ownerId: String, content: ContentFilter, ai: AiFilter) {
        context.dataStore.edit {
            it[intPreferencesKey("content_$ownerId")] = content.ordinal
            it[intPreferencesKey("ai_$ownerId")] = ai.ordinal
        }
    }

    suspend fun setDownloadTree(ownerId: String, uri: String?) {
        context.dataStore.edit {
            val key = stringPreferencesKey("download_tree_$ownerId")
            if (uri == null) it.remove(key) else it[key] = uri
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit {
            it[stringPreferencesKey("profile_name_${profile.id}")] = profile.name
            val avatarKey = stringPreferencesKey("profile_avatar_${profile.id}")
            if (profile.avatarPath == null) it.remove(avatarKey) else it[avatarKey] = profile.avatarPath
        }
    }

    fun profile(userId: Long): Flow<UserProfile> = context.dataStore.data.map {
        UserProfile(
            id = userId,
            name = it[stringPreferencesKey("profile_name_$userId")] ?: userId.toString(),
            avatarPath = it[stringPreferencesKey("profile_avatar_$userId")],
        )
    }

    suspend fun lastUpdateCheck(): Long = context.dataStore.data.first()[Keys.lastUpdateCheck] ?: 0L
    suspend fun markUpdateChecked(now: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.lastUpdateCheck] = now }
    }
}
