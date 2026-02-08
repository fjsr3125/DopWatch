package com.sora.dopwatch.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteConfig(
    val totalLimitHours: Float? = null,
    val snsLimitHours: Float? = null,
    val videoLimitHours: Float? = null,
    val maxAlertsPerDay: Int? = null,
    val cooldownMinutes: Int? = null
)

@Singleton
class RemoteConfigClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "RemoteConfigClient"
        private const val DRIVE_DOWNLOAD_URL =
            "https://drive.google.com/uc?export=download&id="
    }

    suspend fun fetchConfig(driveFileId: String): Result<RemoteConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$DRIVE_DOWNLOAD_URL$driveFileId"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw RuntimeException("Drive fetch error: ${response.code}")
                }

                val body = response.body?.string()
                    ?: throw RuntimeException("Empty response")
                val json = JSONObject(body)

                RemoteConfig(
                    totalLimitHours = json.optDouble("totalLimitHours")
                        .takeIf { !it.isNaN() }?.toFloat(),
                    snsLimitHours = json.optDouble("snsLimitHours")
                        .takeIf { !it.isNaN() }?.toFloat(),
                    videoLimitHours = json.optDouble("videoLimitHours")
                        .takeIf { !it.isNaN() }?.toFloat(),
                    maxAlertsPerDay = json.optInt("maxAlertsPerDay", -1)
                        .takeIf { it >= 0 },
                    cooldownMinutes = json.optInt("cooldownMinutes", -1)
                        .takeIf { it >= 0 }
                )
            }.onFailure { e ->
                Log.e(TAG, "リモート設定の取得に失敗", e)
            }
        }
}
