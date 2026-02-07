package com.sora.dopwatch.domain

data class ThresholdConfig(
    val totalDailyLimitMs: Long = 3 * 60 * 60 * 1000L, // デフォルト3時間
    val snsLimitMs: Long = 1 * 60 * 60 * 1000L,         // SNS 1時間
    val snsPackages: Set<String> = setOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.facebook.katana",
        "com.facebook.orca",
        "com.snapchat.android",
        "jp.naver.line.android"
    ),
    val videoPackages: Set<String> = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "tv.abema",
        "jp.tver"
    ),
    val videoLimitMs: Long = 1 * 60 * 60 * 1000L // 動画 1時間
)
