package com.sora.dopwatch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sora.dopwatch.api.RemoteConfig
import com.sora.dopwatch.domain.ThresholdConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LINE_TOKEN = stringPreferencesKey("line_channel_access_token")
        val LINE_GROUP_ID = stringPreferencesKey("line_group_id")
        val BEEMINDER_USER = stringPreferencesKey("beeminder_username")
        val BEEMINDER_TOKEN = stringPreferencesKey("beeminder_auth_token")
        val BEEMINDER_GOAL = stringPreferencesKey("beeminder_goal_slug")
        val TOTAL_LIMIT_MS = longPreferencesKey("total_daily_limit_ms")
        val SNS_LIMIT_MS = longPreferencesKey("sns_limit_ms")
        val VIDEO_LIMIT_MS = longPreferencesKey("video_limit_ms")
        val DRIVE_FILE_ID = stringPreferencesKey("drive_file_id")
        val MAX_ALERTS_PER_DAY = intPreferencesKey("max_alerts_per_day")
        val COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            lineToken = prefs[LINE_TOKEN] ?: "",
            lineGroupId = prefs[LINE_GROUP_ID] ?: "",
            beeminderUser = prefs[BEEMINDER_USER] ?: "",
            beeminderToken = prefs[BEEMINDER_TOKEN] ?: "",
            beeminderGoal = prefs[BEEMINDER_GOAL] ?: "screentime",
            totalLimitMs = prefs[TOTAL_LIMIT_MS] ?: (3 * 60 * 60 * 1000L),
            snsLimitMs = prefs[SNS_LIMIT_MS] ?: (1 * 60 * 60 * 1000L),
            videoLimitMs = prefs[VIDEO_LIMIT_MS] ?: (1 * 60 * 60 * 1000L),
            driveFileId = prefs[DRIVE_FILE_ID] ?: "",
            maxAlertsPerDay = prefs[MAX_ALERTS_PER_DAY] ?: 3,
            cooldownMinutes = prefs[COOLDOWN_MINUTES] ?: 120
        )
    }

    suspend fun getSettings(): Settings = settingsFlow.first()

    suspend fun updateLineConfig(token: String, groupId: String) {
        context.dataStore.edit { prefs ->
            prefs[LINE_TOKEN] = token
            prefs[LINE_GROUP_ID] = groupId
        }
    }

    suspend fun updateBeeminderConfig(user: String, token: String, goal: String) {
        context.dataStore.edit { prefs ->
            prefs[BEEMINDER_USER] = user
            prefs[BEEMINDER_TOKEN] = token
            prefs[BEEMINDER_GOAL] = goal
        }
    }

    suspend fun updateThresholds(totalMs: Long, snsMs: Long, videoMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[TOTAL_LIMIT_MS] = totalMs
            prefs[SNS_LIMIT_MS] = snsMs
            prefs[VIDEO_LIMIT_MS] = videoMs
        }
    }

    suspend fun updateDriveFileId(fileId: String) {
        context.dataStore.edit { prefs ->
            prefs[DRIVE_FILE_ID] = fileId
        }
    }

    suspend fun applyRemoteConfig(config: RemoteConfig) {
        context.dataStore.edit { prefs ->
            config.totalLimitHours?.let {
                prefs[TOTAL_LIMIT_MS] = (it * 60 * 60 * 1000).toLong()
            }
            config.snsLimitHours?.let {
                prefs[SNS_LIMIT_MS] = (it * 60 * 60 * 1000).toLong()
            }
            config.videoLimitHours?.let {
                prefs[VIDEO_LIMIT_MS] = (it * 60 * 60 * 1000).toLong()
            }
            config.maxAlertsPerDay?.let {
                prefs[MAX_ALERTS_PER_DAY] = it
            }
            config.cooldownMinutes?.let {
                prefs[COOLDOWN_MINUTES] = it
            }
        }
    }

    fun getThresholdConfig(settings: Settings): ThresholdConfig {
        return ThresholdConfig(
            totalDailyLimitMs = settings.totalLimitMs,
            snsLimitMs = settings.snsLimitMs,
            videoLimitMs = settings.videoLimitMs
        )
    }
}

data class Settings(
    val lineToken: String = "",
    val lineGroupId: String = "",
    val beeminderUser: String = "",
    val beeminderToken: String = "",
    val beeminderGoal: String = "screentime",
    val totalLimitMs: Long = 3 * 60 * 60 * 1000L,
    val snsLimitMs: Long = 1 * 60 * 60 * 1000L,
    val videoLimitMs: Long = 1 * 60 * 60 * 1000L,
    val driveFileId: String = "",
    val maxAlertsPerDay: Int = 3,
    val cooldownMinutes: Int = 120
) {
    val isLineConfigured: Boolean get() = lineToken.isNotBlank() && lineGroupId.isNotBlank()
    val isBeeminderConfigured: Boolean get() = beeminderUser.isNotBlank() && beeminderToken.isNotBlank()
    val isDriveConfigured: Boolean get() = driveFileId.isNotBlank()
    val cooldownMs: Long get() = cooldownMinutes.toLong() * 60 * 1000
}
