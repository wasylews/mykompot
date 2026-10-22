package com.wasylews.mykompot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class LoginAndSeeStatusTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginAndSeeAllGood() {
        // WHEN I log in
        composeRule.onNodeWithText("Username").performTextInput("user")
        composeRule.onNodeWithText("Password").performTextInput("pass")
        composeRule.onNodeWithText("Log in").performClick()

        // THEN I see the status
        composeRule.onNodeWithText("All Good").assertIsDisplayed()
    }
}
