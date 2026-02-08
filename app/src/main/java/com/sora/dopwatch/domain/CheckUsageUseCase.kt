package com.sora.dopwatch.domain

import com.sora.dopwatch.data.AppUsageInfo
import javax.inject.Inject

data class UsageAlert(
    val type: AlertType,
    val currentMs: Long,
    val limitMs: Long,
    val topApps: List<AppUsageInfo>
)

enum class AlertType {
    TOTAL_EXCEEDED,
    SNS_EXCEEDED,
    VIDEO_EXCEEDED
}

class CheckUsageUseCase @Inject constructor() {

    fun check(usages: List<AppUsageInfo>, config: ThresholdConfig): List<UsageAlert> {
        val alerts = mutableListOf<UsageAlert>()

        // 総使用時間チェック
        val totalMs = usages.sumOf { it.usageTimeMs }
        if (totalMs > config.totalDailyLimitMs) {
            alerts.add(
                UsageAlert(
                    type = AlertType.TOTAL_EXCEEDED,
                    currentMs = totalMs,
                    limitMs = config.totalDailyLimitMs,
                    topApps = usages.take(5)
                )
            )
        }

        // SNS使用時間チェック
        val snsUsages = usages.filter { it.packageName in config.snsPackages }
        val snsMs = snsUsages.sumOf { it.usageTimeMs }
        if (snsMs > config.snsLimitMs) {
            alerts.add(
                UsageAlert(
                    type = AlertType.SNS_EXCEEDED,
                    currentMs = snsMs,
                    limitMs = config.snsLimitMs,
                    topApps = snsUsages
                )
            )
        }

        // 動画使用時間チェック
        val videoUsages = usages.filter { it.packageName in config.videoPackages }
        val videoMs = videoUsages.sumOf { it.usageTimeMs }
        if (videoMs > config.videoLimitMs) {
            alerts.add(
                UsageAlert(
                    type = AlertType.VIDEO_EXCEEDED,
                    currentMs = videoMs,
                    limitMs = config.videoLimitMs,
                    topApps = videoUsages
                )
            )
        }

        return alerts
    }

    fun formatDuration(ms: Long): String {
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}h${minutes}m" else "${minutes}m"
    }

    fun buildAlertMessage(alert: UsageAlert): String {
        val typeLabel = when (alert.type) {
            AlertType.TOTAL_EXCEEDED -> "総スクリーンタイム"
            AlertType.SNS_EXCEEDED -> "SNS使用時間"
            AlertType.VIDEO_EXCEEDED -> "動画使用時間"
        }
        val current = formatDuration(alert.currentMs)
        val limit = formatDuration(alert.limitMs)
        val appList = alert.topApps.joinToString("\n") { app ->
            "  ${app.appName}: ${formatDuration(app.usageTimeMs)}"
        }
        return """
            |⚠️ DopWatch: ${typeLabel}超過
            |
            |📱 現在: $current
            |🎯 制限: $limit
            |
            |上位アプリ:
            |$appList
        """.trimMargin()
    }

    fun buildFollowUpMessage(alerts: List<UsageAlert>, alertNumber: Int): String {
        val header = "⚠️ DopWatch: ${alertNumber}回目の警告\n引き続き使用超過中です"
        val details = alerts.joinToString("\n") { alert ->
            val icon = when (alert.type) {
                AlertType.TOTAL_EXCEEDED -> "📱"
                AlertType.SNS_EXCEEDED -> "💬"
                AlertType.VIDEO_EXCEEDED -> "🎬"
            }
            val typeLabel = when (alert.type) {
                AlertType.TOTAL_EXCEEDED -> "スクリーンタイム"
                AlertType.SNS_EXCEEDED -> "SNS"
                AlertType.VIDEO_EXCEEDED -> "動画"
            }
            "$icon $typeLabel: ${formatDuration(alert.currentMs)} / ${formatDuration(alert.limitMs)}"
        }
        return "$header\n\n$details"
    }
}
