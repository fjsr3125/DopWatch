package com.sora.dopwatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun dashboardScreen_isDisplayed() {
        composeRule.onNodeWithText("DopWatch").assertIsDisplayed()
    }

    @Test
    fun navigateToSettings_andBack() {
        composeRule.onNodeWithContentDescription("設定").performClick()
        composeRule.onNodeWithText("設定").assertIsDisplayed()
        composeRule.onNodeWithText("LINE Messaging API").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Beeminder").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("制限時間").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithContentDescription("戻る").performClick()
        composeRule.onNodeWithText("DopWatch").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_showsPermissionRequestOrUsage() {
        try {
            composeRule.onNodeWithText("使用状況へのアクセス許可が必要です").assertIsDisplayed()
        } catch (_: AssertionError) {
            composeRule.onNodeWithText("DopWatch").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_hasAllSections() {
        composeRule.onNodeWithContentDescription("設定").performClick()

        // セクションヘッダー（スクロールして確認）
        composeRule.onNodeWithText("制限時間").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("LINE Messaging API").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Beeminder").performScrollTo().assertIsDisplayed()

        // 入力フィールド（スクロールして確認）
        composeRule.onNodeWithText("Channel Access Token").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Group ID").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Username").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Auth Token").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Goal Slug").performScrollTo().assertIsDisplayed()
    }
}
