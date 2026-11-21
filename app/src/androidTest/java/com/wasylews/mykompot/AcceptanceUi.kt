/*
 * Copyright 2026 Vasyl Sabov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalTestApi::class)

package com.wasylews.mykompot

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.wasylews.mykompot.presentation.home.HOME_LOADING_TEST_TAG
import com.wasylews.mykompot.presentation.home.HOME_REFRESH_INDICATOR_TEST_TAG
import com.wasylews.mykompot.presentation.home.HOME_SCREEN_TEST_TAG
import com.wasylews.mykompot.presentation.login.REMEMBER_ME_TEST_TAG

internal fun BaseAcceptanceTest.performLogin(rememberMe: Boolean = false) {
    assertShows(R.string.username).performTextInput("user")
    assertShows(R.string.password).performTextInput("pass")
    if (rememberMe) {
        composeRule.onNodeWithTag(REMEMBER_ME_TEST_TAG).performClick()
    }
    assertShows(R.string.log_in).performClick()
}

internal fun BaseAcceptanceTest.assertShows(
    @StringRes res: Int,
    vararg formatArgs: Any,
): SemanticsNodeInteraction {
    val string = stringRes(res, *formatArgs)
    composeRule.waitUntilExactlyOneExists(hasText(string), 5_000)
    return composeRule.onNodeWithText(string).assertIsDisplayed()
}

internal fun BaseAcceptanceTest.assertDoesNotShow(
    @StringRes res: Int,
) {
    composeRule
        .onNodeWithText(stringRes(res))
        .assertDoesNotExist()
}

internal fun BaseAcceptanceTest.stringRes(
    res: Int,
    vararg formatArgs: Any,
): String = context.getString(res, *formatArgs)

internal fun BaseAcceptanceTest.pullToRefresh() {
    composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_SCREEN_TEST_TAG), 5_000)
    composeRule.onNodeWithTag(HOME_SCREEN_TEST_TAG).performTouchInput { swipeDown() }
}

internal fun BaseAcceptanceTest.assertShowsHomeLoading() {
    composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LOADING_TEST_TAG), 5_000)
    composeRule.onNodeWithTag(HOME_LOADING_TEST_TAG).assertIsDisplayed()
}

internal fun BaseAcceptanceTest.assertShowsRefreshIndicator() {
    composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_REFRESH_INDICATOR_TEST_TAG), 5_000)
    composeRule.onNodeWithTag(HOME_REFRESH_INDICATOR_TEST_TAG).assertIsDisplayed()
}

internal fun BaseAcceptanceTest.assertNoRefreshIndicator() {
    composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithTag(HOME_REFRESH_INDICATOR_TEST_TAG).fetchSemanticsNodes().isEmpty()
    }
}

internal fun BaseAcceptanceTest.assertShowsRefreshError(
    @StringRes res: Int,
) {
    assertShows(res)
}
