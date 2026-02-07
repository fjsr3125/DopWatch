package com.sora.dopwatch.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sora.dopwatch.R
import com.sora.dopwatch.api.BeeminderClient
import com.sora.dopwatch.api.LineMessagingClient
import com.sora.dopwatch.data.SettingsRepository
import com.sora.dopwatch.data.UsageRepository
import com.sora.dopwatch.domain.CheckUsageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class UsageCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository,
    private val checkUsageUseCase: CheckUsageUseCase,
    private val lineClient: LineMessagingClient,
    private val beeminderClient: BeeminderClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "UsageCheckWorker"
        const val CHANNEL_ID = "dopwatch_alerts"
        private const val PREFS_NAME = "dopwatch_worker_prefs"
        private const val KEY_LAST_ALERT_COUNT = "last_alert_count"
        private const val KEY_HEARTBEAT_PREFIX = "heartbeat_sent"
        private const val MAX_LINE_ALERTS_PER_DAY = 3
        private const val HEARTBEAT_HOUR_MORNING = 9
        private const val HEARTBEAT_HOUR_NIGHT = 21
    }

    override suspend fun doWork(): Result {
        try {
            // 使用時間を取得・保存
            usageRepository.refreshAndSave()

            val usages = usageRepository.queryTodayUsage()
            val settings = settingsRepository.getSettings()
            val config = settingsRepository.getThresholdConfig(settings)
            val totalMs = usageRepository.getTodayTotalMs()

            // ハートビート（1日2回: 朝・夜）
            if (settings.isLineConfigured) {
                sendHeartbeatIfNeeded(settings.lineToken, settings.lineGroupId, totalMs)
            }

            // Beeminder（毎回最新値を送信、requestidで冪等）
            if (settings.isBeeminderConfigured) {
                val totalHours = totalMs.toDouble() / (1000 * 60 * 60)
                beeminderClient.sendDatapoint(
                    username = settings.beeminderUser,
                    goalSlug = settings.beeminderGoal,
                    authToken = settings.beeminderToken,
                    valueHours = totalHours
                ).onFailure { e ->
                    Log.e(TAG, "Beeminder送信失敗", e)
                }
            }

            // 閾値チェック
            val alerts = checkUsageUseCase.check(usages, config)
            if (alerts.isEmpty()) return Result.success()

            // ローカル通知
            createNotificationChannel()
            alerts.forEach { alert ->
                val message = checkUsageUseCase.buildAlertMessage(alert)
                showNotification(alert.type.name, message)
            }

            // LINE警告通知（1日3回まで、ハートビートとは別枠）
            if (settings.isLineConfigured && canSendLineAlert()) {
                val mainAlert = alerts.first()
                val message = checkUsageUseCase.buildAlertMessage(mainAlert)
                lineClient.sendMessage(
                    channelAccessToken = settings.lineToken,
                    groupId = settings.lineGroupId,
                    text = message
                ).onFailure { e ->
                    Log.e(TAG, "LINE送信失敗", e)
                }
                incrementLineAlertCount()
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker実行エラー", e)
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "スクリーンタイム警告",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "使用時間が制限を超えた時の通知"
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(tag: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ DopWatch")
            .setContentText(message.lines().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(tag.hashCode(), notification)
    }

    private suspend fun sendHeartbeatIfNeeded(token: String, groupId: String, totalMs: Long) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val slot = when {
            hour in HEARTBEAT_HOUR_MORNING..HEARTBEAT_HOUR_MORNING + 2 -> "morning"
            hour in HEARTBEAT_HOUR_NIGHT..HEARTBEAT_HOUR_NIGHT + 2 -> "night"
            else -> return
        }

        val today = usageRepository.getTodayString()
        val key = "${KEY_HEARTBEAT_PREFIX}_${today}_$slot"
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return // 既に送信済み

        val totalFormatted = checkUsageUseCase.formatDuration(totalMs)
        val emoji = if (slot == "morning") "\uD83C\uDF05" else "\uD83C\uDF19" // 🌅 or 🌙
        val message = "$emoji DopWatch 稼働中\n\n" +
            "現在のスクリーンタイム: $totalFormatted\n" +
            "監視は正常に動作しています。\n" +
            "※ この通知が届かなくなったらアプリが無効化されています"

        lineClient.sendMessage(
            channelAccessToken = token,
            groupId = groupId,
            text = message
        ).onSuccess {
            prefs.edit().putBoolean(key, true).apply()
            Log.i(TAG, "ハートビート送信: $slot")
        }.onFailure { e ->
            Log.e(TAG, "ハートビート送信失敗", e)
        }
    }

    private fun canSendLineAlert(): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = usageRepository.getTodayString()
        val key = "${KEY_LAST_ALERT_COUNT}_$today"
        val count = prefs.getInt(key, 0)
        return count < MAX_LINE_ALERTS_PER_DAY
    }

    private fun incrementLineAlertCount() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = usageRepository.getTodayString()
        val key = "${KEY_LAST_ALERT_COUNT}_$today"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
    }
}
