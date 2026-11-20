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

package com.wasylews.mykompot.presentation.home

import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.LoadMorePayments
import com.wasylews.mykompot.domain.ObservePayments
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.RefreshPayments
import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.main.FakeObserveSession
import com.wasylews.mykompot.presentation.payments.paymentMethods
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsViewModelTest : BaseViewModelTest() {
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-11T12:00:00Z")
    }
    private val zoneId = ZoneId.of("UTC")

    private fun createViewModel(
        observePayments: ObservePayments = FakeObservePayments(),
        refreshPayments: RefreshPayments = FakeRefreshPayments(),
        loadMorePayments: LoadMorePayments = FakeLoadMorePayments(),
        observeSession: ObserveSession = FakeObserveSession(SessionState.LoggedIn),
    ) = PaymentsViewModel(
        observePayments = observePayments,
        refreshPayments = refreshPayments,
        loadMorePaymentsUseCase = loadMorePayments,
        observeSession = observeSession,
        clock = fixedClock,
        zoneId = zoneId,
    )

    @Test
    fun `refreshes payments on init`() = runTest {
        // GIVEN
        val refreshPayments = FakeRefreshPayments()

        // WHEN
        createViewModel(refreshPayments = refreshPayments)
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshPayments.calls)
    }

    @Test
    fun `maps payments with tariff name payment method and formatted date`() = runTest {
        // GIVEN payments from different sources and years
        val payments = Payments(
            items = listOf(
                Payment(
                    dateTime = LocalDateTime.of(2026, 10, 1, 10, 5),
                    amount = 300,
                    tariffName = "Standart 100",
                    source = PaymentSource.PRIVAT24,
                ),
                Payment(
                    dateTime = LocalDateTime.of(2024, 10, 15, 9, 39),
                    amount = 250,
                    tariffName = "Econom 50",
                    source = PaymentSource.EASYPAY,
                ),
                Payment(
                    dateTime = LocalDateTime.of(2026, 8, 6, 0, 0),
                    amount = 300,
                    tariffName = "Standart 100",
                    source = PaymentSource.UNKNOWN,
                ),
            ),
            hasMore = false,
        )
        val observePayments = FakeObservePayments(payments)

        // WHEN
        val vm = createViewModel(observePayments = observePayments)
        advanceUntilIdle()

        // THEN
        val privat24 = paymentMethods.first { it.titleRes == R.string.payment_method_privat24 }
        val easyPay = paymentMethods.first { it.titleRes == R.string.payment_method_easypay }
        assertEquals(
            listOf(
                PaymentItemUi(
                    amount = 300,
                    tariffName = "Standart 100",
                    paymentMethod = privat24,
                    dateTimeLabel = "1 Oct, 10:05",
                ),
                PaymentItemUi(
                    amount = 250,
                    tariffName = "Econom 50",
                    paymentMethod = easyPay,
                    dateTimeLabel = "15 Oct 2024, 09:39",
                ),
                PaymentItemUi(
                    amount = 300,
                    tariffName = "Standart 100",
                    paymentMethod = null,
                    dateTimeLabel = "6 Aug, 00:00",
                ),
            ),
            vm.uiState.value.items,
        )
    }

    @Test
    fun `refresh reloads payments from first page`() = runTest {
        // GIVEN a screen with payments loaded
        val refreshPayments = FakeRefreshPayments()
        val vm = createViewModel(refreshPayments = refreshPayments)
        advanceUntilIdle()
        assertEquals(1, refreshPayments.calls)

        // WHEN I refresh
        vm.refresh()
        advanceUntilIdle()

        // THEN payments are reloaded
        assertEquals(2, refreshPayments.calls)
    }

    @Test
    fun `shows server error when payments stream fails`() = runTest {
        // GIVEN a payments stream that fails
        val observePayments = ObservePayments {
            flow { throw IOException("Broken storage") }
        }

        // WHEN
        val vm = createViewModel(observePayments = observePayments)
        advanceUntilIdle()

        // THEN the list is empty and the error is shown instead of staying silent
        assertEquals(PaymentsUiState(errorId = R.string.error_server), vm.uiState.value)
    }

    @Test
    fun `keeps payments stream error after a successful refresh`() = runTest {
        // GIVEN a payments stream that fails
        val observePayments = ObservePayments {
            flow { throw IOException("Broken storage") }
        }
        val vm = createViewModel(observePayments = observePayments)
        advanceUntilIdle()

        // WHEN a refresh succeeds
        vm.refresh()
        advanceUntilIdle()

        // THEN the stream error still explains why the list is empty
        assertEquals(R.string.error_server, vm.uiState.value.errorId)
    }

    @Test
    fun `loads more payments when available`() = runTest {
        // GIVEN payments with more pages
        val observePayments = FakeObservePayments(Payments(items = emptyList(), hasMore = true))
        val loadMorePayments = FakeLoadMorePayments().apply { pause() }
        val vm = createViewModel(
            observePayments = observePayments,
            loadMorePayments = loadMorePayments,
        )
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoadingMore)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasMore)

        // WHEN I request more payments
        vm.loadMorePayments()
        advanceUntilIdle()

        // THEN loading-more is shown while loading
        assertEquals(1, loadMorePayments.calls)
        assertTrue(vm.uiState.value.isLoadingMore)

        // WHEN loading completes
        loadMorePayments.resume()
        advanceUntilIdle()

        // THEN loading-more is hidden
        assertFalse(vm.uiState.value.isLoadingMore)
    }

    @Test
    fun `does not load more payments when none available or already loading`() = runTest {
        // GIVEN no more payments
        val observePayments = FakeObservePayments(Payments(items = emptyList(), hasMore = false))
        val loadMorePayments = FakeLoadMorePayments()
        val vm = createViewModel(
            observePayments = observePayments,
            loadMorePayments = loadMorePayments,
        )
        advanceUntilIdle()

        // WHEN I request more payments
        vm.loadMorePayments()
        advanceUntilIdle()

        // THEN nothing is loaded
        assertEquals(0, loadMorePayments.calls)
    }

    @Test
    fun `shows load more error and retries`() = runTest {
        // GIVEN payments with more pages and a failing load-more
        val observePayments = FakeObservePayments(Payments(items = emptyList(), hasMore = true))
        val loadMorePayments = FakeLoadMorePayments().apply { result = PaymentsActionResult.Failed.Server }
        val vm = createViewModel(
            observePayments = observePayments,
            loadMorePayments = loadMorePayments,
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMore)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isLoadingMore)

        // WHEN loading more fails
        vm.loadMorePayments()
        advanceUntilIdle()

        // THEN an inline error is shown
        assertEquals(1, loadMorePayments.calls)
        assertEquals(R.string.error_server, vm.uiState.value.loadMoreErrorId)
        assertFalse(vm.uiState.value.isLoadingMore)

        // WHEN I retry successfully
        loadMorePayments.result = PaymentsActionResult.Success
        vm.retryLoadMorePayments()
        advanceUntilIdle()

        // THEN the error is cleared
        assertNull(vm.uiState.value.loadMoreErrorId)
        assertEquals(2, loadMorePayments.calls)
    }
}
