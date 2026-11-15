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

package com.wasylews.mykompot.presentation.payments

import com.wasylews.mykompot.presentation.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodsViewModelTest : BaseViewModelTest() {
    @Test
    fun `open shows payment methods sheet`() = runTest {
        // GIVEN
        val vm = PaymentMethodsViewModel()
        advanceUntilIdle()

        // WHEN
        vm.open()
        advanceUntilIdle()

        // THEN
        assertTrue(vm.uiState.value.isVisible)
    }

    @Test
    fun `dismiss hides payment methods sheet`() = runTest {
        // GIVEN
        val vm = PaymentMethodsViewModel()
        advanceUntilIdle()
        vm.open()
        advanceUntilIdle()

        // WHEN
        vm.dismiss()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.uiState.value.isVisible)
    }
}
