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

import com.wasylews.mykompot.PaymentFixtures
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.LoadMorePayments
import com.wasylews.mykompot.domain.ObservePayments
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.RefreshPayments
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.main.FakeObserveSession
import com.wasylews.mykompot.presentation.payments.paymentMethods
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private fun paymentsWithPayment(hasMore: Boolean = false) = Payments(
        items = listOf(PaymentFixtures.payment()),
        hasMore = hasMore,
    )

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
    fun `shows skeleton while refreshing without cached payments`() = runTest {
        // GIVEN a cache that has not emitted yet and a refresh that is still in progress
        val paymentsFlow = MutableSharedFlow<Payments?>()
        val refreshPayments = FakeRefreshPayments().apply { pause() }
        val vm = createViewModel(
            observePayments = ObservePayments { paymentsFlow },
            refreshPayments = refreshPayments,
        )

        // WHEN the refresh starts before the cache is available
        advanceUntilIdle()

        // THEN the skeleton is shown until there are rows to display
        assertTrue(vm.uiState.value.isShowingSkeleton)
        assertTrue(vm.uiState.value.isLoading)

        // WHEN the cached payments arrive during the refresh
        paymentsFlow.emit(
            Payments(
                items = listOf(PaymentFixtures.payment()),
                hasMore = false,
            ),
        )
        advanceUntilIdle()

        // THEN the cached rows replace the skeleton
        assertFalse(vm.uiState.value.isShowingSkeleton)
        assertTrue(vm.uiState.value.isLoading)

        refreshPayments.resume()
        advanceUntilIdle()
    }

    @Test
    fun `maps payments with tariff name payment method and formatted date`() = runTest {
        // GIVEN payments from different sources and years
        val payments = Payments(
            items = listOf(
                PaymentFixtures.payment(),
                PaymentFixtures.payment(
                    dateTime = LocalDateTime.of(2024, 10, 15, 9, 39),
                    amount = 250,
                    tariffName = "Econom 50",
                    source = PaymentSource.EASYPAY,
                ),
                PaymentFixtures.payment(
                    dateTime = LocalDateTime.of(2026, 8, 6, 0, 0),
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
    fun `does not load more payments without cache when unavailable or already loading`() = runTest {
        // GIVEN no cached payments
        val noCache = FakeLoadMorePayments()
        val noCacheVm = createViewModel(
            observePayments = FakeObservePayments(null),
            loadMorePayments = noCache,
        )
        advanceUntilIdle()

        // WHEN I request more payments
        noCacheVm.loadMorePayments()
        advanceUntilIdle()

        // THEN nothing is loaded
        assertEquals(0, noCache.calls)

        // GIVEN a first page with no more payments
        val noMore = FakeLoadMorePayments()
        val noMoreVm = createViewModel(
            observePayments = FakeObservePayments(Payments(items = emptyList(), hasMore = false)),
            loadMorePayments = noMore,
        )
        advanceUntilIdle()

        // WHEN I request more payments
        noMoreVm.loadMorePayments()
        advanceUntilIdle()

        // THEN nothing is loaded
        assertEquals(0, noMore.calls)

        // GIVEN a first page with more payments and a request already in flight
        val loading = FakeLoadMorePayments().apply { pause() }
        val loadingVm = createViewModel(
            observePayments = FakeObservePayments(Payments(items = emptyList(), hasMore = true)),
            loadMorePayments = loading,
        )
        advanceUntilIdle()

        // WHEN I request more payments, then request again while the first has started
        loadingVm.loadMorePayments()
        advanceUntilIdle()
        loadingVm.loadMorePayments()
        advanceUntilIdle()

        // THEN only the first request runs
        assertEquals(1, loading.calls)
        assertTrue(loadingVm.uiState.value.isLoadingMore)
        loading.resume()
        advanceUntilIdle()
    }

    @Test
    fun `shows load more error and retries while keeping cached rows`() = runTest {
        // GIVEN payments with more pages and a failing load-more
        val observePayments = FakeObservePayments(paymentsWithPayment(hasMore = true))
        val loadMorePayments = FakeLoadMorePayments().apply { result = PaymentsActionResult.Failed(PortalFailure.Server) }
        val vm = createViewModel(
            observePayments = observePayments,
            loadMorePayments = loadMorePayments,
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMore)
        assertEquals(1, vm.uiState.value.items.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isLoadingMore)

        // WHEN loading more fails
        vm.loadMorePayments()
        advanceUntilIdle()

        // THEN an inline error is shown without dropping the cached rows
        assertEquals(1, loadMorePayments.calls)
        assertEquals(R.string.error_server, vm.uiState.value.loadMoreErrorId)
        assertEquals(1, vm.uiState.value.items.size)
        assertFalse(vm.uiState.value.isLoadingMore)

        // WHEN I retry successfully
        loadMorePayments.result = PaymentsActionResult.Success
        vm.retryLoadMorePayments()
        advanceUntilIdle()

        // THEN the error is cleared
        assertNull(vm.uiState.value.loadMoreErrorId)
        assertEquals(2, loadMorePayments.calls)
    }

    @Test
    fun `retry without cached payments clears error and shows skeleton then content`() = runTest {
        // GIVEN a failed first-page refresh with no cached payments
        val observePayments = FakeObservePayments(null)
        val refreshPayments = FakeRefreshPayments().apply {
            result = PaymentsActionResult.Failed(PortalFailure.Server)
            pause()
        }
        val vm = createViewModel(
            observePayments = observePayments,
            refreshPayments = refreshPayments,
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isShowingSkeleton)

        refreshPayments.resume()
        advanceUntilIdle()
        assertEquals(R.string.error_server, vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isShowingSkeleton)

        // WHEN a retry starts
        refreshPayments.result = PaymentsActionResult.Success
        refreshPayments.pause()
        vm.refresh()
        advanceUntilIdle()

        // THEN the stale error is replaced by skeleton rows
        assertNull(vm.uiState.value.errorId)
        assertTrue(vm.uiState.value.isShowingSkeleton)

        // AND the rows emitted by the refresh replace the skeleton
        observePayments.emit(paymentsWithPayment())
        refreshPayments.resume()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.items.size)
        assertNull(vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isShowingSkeleton)
    }

    @Test
    fun `refresh failure with cached payments keeps rows without blocking error`() = runTest {
        // GIVEN cached payments and a failing background refresh
        val vm = createViewModel(
            observePayments = FakeObservePayments(paymentsWithPayment()),
            refreshPayments = FakeRefreshPayments().apply {
                result = PaymentsActionResult.Failed(PortalFailure.Network)
            },
        )
        advanceUntilIdle()

        // THEN cached rows stay visible without a blocking error
        assertEquals(1, vm.uiState.value.items.size)
        assertNull(vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isShowingSkeleton)
    }

    @Test
    fun `new refresh cancels previous refresh and only latest completion clears loading`() = runTest {
        // GIVEN a first-page refresh that is still running
        val refreshPayments = FakeRefreshPayments().apply { repeat(2) { pause() } }
        val vm = createViewModel(refreshPayments = refreshPayments)
        advanceUntilIdle()
        assertEquals(1, refreshPayments.calls)
        assertTrue(vm.uiState.value.isLoading)

        // WHEN a second refresh starts
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, refreshPayments.calls)

        // THEN the cancelled first refresh does not clear the loading flag
        refreshPayments.resume(call = 1)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoading)

        // AND only the latest refresh clears it
        refreshPayments.resume(call = 2)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }
}
