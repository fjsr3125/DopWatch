package com.sora.dopwatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun saveLineConfig_showsSnackbar() {
        // 設定画面へ
        composeRule.onNodeWithContentDescription("設定").performClick()
        composeRule.waitForIdle()

        // LINE設定を入力
        composeRule.onNodeWithText("Channel Access Token").performScrollTo().performClick()
        composeRule.onNodeWithText("Channel Access Token").performTextInput("test_token_123")
        composeRule.onNodeWithText("Group ID").performScrollTo().performClick()
        composeRule.onNodeWithText("Group ID").performTextInput("C1234567890")

        // LINE設定の保存ボタンをタップ（index=1: 制限時間が0番目）
        val saveButtons = composeRule.onAllNodesWithText("保存")
        saveButtons[1].performScrollTo().performClick()

        // Snackbar表示を待つ
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("LINE設定を保存しました")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("LINE設定を保存しました").assertIsDisplayed()
    }

    @Test
    fun saveBeeminderConfig_showsSnackbar() {
        // 設定画面へ
        composeRule.onNodeWithContentDescription("設定").performClick()
        composeRule.waitForIdle()

        // Beeminder設定を入力
        composeRule.onNodeWithText("Username").performScrollTo().performClick()
        composeRule.onNodeWithText("Username").performTextInput("testuser")
        composeRule.onNodeWithText("Auth Token").performScrollTo().performClick()
        composeRule.onNodeWithText("Auth Token").performTextInput("test_auth_token")
        composeRule.onNodeWithText("Goal Slug").performScrollTo().performClick()
        composeRule.onNodeWithText("Goal Slug").performTextInput("screentime")

        // Beeminder設定の保存ボタンをタップ（index=2）
        val saveButtons = composeRule.onAllNodesWithText("保存")
        saveButtons[2].performScrollTo().performClick()

        // Snackbar表示を待つ
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Beeminder設定を保存しました")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Beeminder設定を保存しました").assertIsDisplayed()
    }
}
