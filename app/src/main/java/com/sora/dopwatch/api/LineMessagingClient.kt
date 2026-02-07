package com.sora.dopwatch.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineMessagingClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val PUSH_URL = "https://api.line.me/v2/bot/message/push"
    }

    suspend fun sendMessage(
        channelAccessToken: String,
        groupId: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("to", groupId)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", text)
                    })
                })
            }

            val request = Request.Builder()
                .url(PUSH_URL)
                .addHeader("Authorization", "Bearer $channelAccessToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw RuntimeException("LINE API error: ${response.code} ${response.body?.string()}")
            }
        }
    }
}
