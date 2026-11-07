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

package com.wasylews.mykompot

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wasylews.mykompot.data.ProfileEntity
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
abstract class BaseAcceptanceTest : KoinTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    protected val server = MockWebServer()

    protected val clock = MutableClock(Instant.parse("2026-10-01T10:00:00Z"))
    private val testModule = module {
        single<Clock> { clock }
    }

    @Before
    fun setup() {
        runBlocking {
            getKoin().get<DataStore<ProfileEntity?>>(named("profile")).updateData { null }
            getKoin().get<DataStore<String?>>(named("session")).updateData { null }
        }
        loadKoinModules(testModule)
        server.start(8080)
    }

    @After
    fun teardown() {
        server.close()
        unloadKoinModules(testModule)
    }

    protected fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    protected fun performLogin() {
        waitForNodeWithStringRes(R.string.username).performTextInput("user")
        waitForNodeWithStringRes(R.string.password).performTextInput("pass")
        waitForNodeWithStringRes(R.string.log_in).performClick()
    }

    protected fun waitForNodeWithStringRes(
        res: Int,
        timeoutMillis: Long = 5_000,
    ): SemanticsNodeInteraction {
        val string = stringRes(res)
        composeRule.waitUntilExactlyOneExists(hasText(string), timeoutMillis)
        return composeRule.onNodeWithText(string)
    }

    protected fun stringRes(res: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res)
}
