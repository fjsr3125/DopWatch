package com.sora.dopwatch.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeeminderClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val BASE_URL = "https://www.beeminder.com/api/v1/users"
    }

    suspend fun sendDatapoint(
        username: String,
        goalSlug: String,
        authToken: String,
        valueHours: Double,
        comment: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())
            val url = "$BASE_URL/$username/goals/$goalSlug/datapoints.json"

            val body = FormBody.Builder()
                .add("auth_token", authToken)
                .add("value", String.format(Locale.US, "%.2f", valueHours))
                .add("comment", comment.ifEmpty { "auto: $today" })
                .add("requestid", today) // 冪等性キー（1日1回のみ更新）
                .build()

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw RuntimeException("Beeminder API error: ${response.code} ${response.body?.string()}")
            }
        }
    }
}
