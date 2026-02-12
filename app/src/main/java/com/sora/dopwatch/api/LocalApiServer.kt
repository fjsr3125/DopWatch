package com.sora.dopwatch.api

import com.sora.dopwatch.data.AppUsageDao
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalApiServer @Inject constructor(
    private val dao: AppUsageDao
) : NanoHTTPD(8080) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.GET) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "GET only")
        }

        return when (session.uri) {
            "/api/usage" -> handleUsage(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun handleUsage(session: IHTTPSession): Response {
        val date = session.parameters["date"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "date parameter required")

        val usages = runBlocking { dao.getUsageListByDate(date) }
        val totalMinutes = usages.sumOf { it.usageTimeMs } / 60_000

        val appsArray = JSONArray()
        for (usage in usages) {
            appsArray.put(JSONObject().apply {
                put("name", usage.appName)
                put("packageName", usage.packageName)
                put("minutes", usage.usageTimeMs / 60_000)
            })
        }

        val json = JSONObject().apply {
            put("date", date)
            put("totalMinutes", totalMinutes)
            put("apps", appsArray)
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString(2))
    }
}
