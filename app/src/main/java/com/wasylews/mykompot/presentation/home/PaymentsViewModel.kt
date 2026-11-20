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

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.presentation.toErrorId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.toJavaInstant

@KoinViewModel
class PaymentsViewModel(
    observePayments: ObservePayments,
    private val refreshPayments: RefreshPayments,
    private val loadMorePaymentsUseCase: LoadMorePayments,
    observeSession: ObserveSession,
    private val clock: Clock = Clock.System,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val paymentsLoading = MutableStateFlow(false)
    private val paymentsLoadingMore = MutableStateFlow(false)
    private val paymentsErrorId = MutableStateFlow<Int?>(null)
    private val paymentsLoadMoreErrorId = MutableStateFlow<Int?>(null)
    private val paymentsStreamErrorId = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    private val privat24Method = paymentMethods.first { it.titleRes == R.string.payment_method_privat24 }
    private val easyPayMethod = paymentMethods.first { it.titleRes == R.string.payment_method_easypay }
    private val monobankMethod = paymentMethods.first { it.titleRes == R.string.payment_method_monobank }

    private val latestPayments = observePayments()
        .catch { error ->
            if (error is CancellationException) throw error
            paymentsStreamErrorId.value = R.string.error_server
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private val paymentsError = combine(
        paymentsErrorId,
        paymentsStreamErrorId,
    ) { errorId, streamErrorId -> errorId ?: streamErrorId }

    init {
        observeSession.sessionStateFlow()
            .onEach { state ->
                if (state == SessionState.LoggedIn) {
                    refresh()
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        refreshJob?.cancel()
        val job = viewModelScope.launch {
            paymentsErrorId.value = null
            paymentsLoadMoreErrorId.value = null
            paymentsLoading.value = true
            try {
                when (val result = refreshPayments()) {
                    PaymentsActionResult.Success,
                    PaymentsActionResult.Expired,
                    -> Unit

                    is PaymentsActionResult.Failed -> {
                        if (latestPayments.value?.items.isNullOrEmpty()) {
                            paymentsErrorId.value = result.reason.toErrorId()
                        }
                    }
                }
            } finally {
                if (refreshJob == coroutineContext[Job]) {
                    paymentsLoading.value = false
                }
            }
        }
        refreshJob = job
    }

    fun loadMorePayments() {
        val payments = latestPayments.value ?: return
        if (!payments.hasMore || paymentsLoadingMore.value) return
        viewModelScope.launch {
            paymentsLoadingMore.value = true
            paymentsLoadMoreErrorId.value = null
            try {
                when (val result = loadMorePaymentsUseCase()) {
                    PaymentsActionResult.Success,
                    PaymentsActionResult.Expired,
                    -> Unit

                    is PaymentsActionResult.Failed -> {
                        paymentsLoadMoreErrorId.value = result.reason.toErrorId()
                    }
                }
            } finally {
                paymentsLoadingMore.value = false
            }
        }
    }

    fun retryLoadMorePayments() {
        paymentsLoadMoreErrorId.value = null
        loadMorePayments()
    }

    val uiState: StateFlow<PaymentsUiState> = combine(
        latestPayments,
        paymentsLoading,
        paymentsLoadingMore,
        paymentsError,
        paymentsLoadMoreErrorId,
    ) { payments, isLoading, isLoadingMore, errorId, loadMoreErrorId ->
        payments.toUiState(
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            errorId = errorId,
            loadMoreErrorId = loadMoreErrorId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PaymentsUiState(),
    )

    private fun Payments?.toUiState(
        isLoading: Boolean,
        isLoadingMore: Boolean,
        @StringRes errorId: Int?,
        @StringRes loadMoreErrorId: Int?,
    ): PaymentsUiState = PaymentsUiState(
        items = this?.items.orEmpty().map { it.toUi() },
        isLoading = isLoading,
        isLoadingMore = isLoadingMore,
        hasMore = this?.hasMore == true,
        errorId = errorId,
        loadMoreErrorId = loadMoreErrorId,
    )

    private fun Payment.toUi(): PaymentItemUi = PaymentItemUi(
        amount = amount,
        tariffName = tariffName,
        paymentMethod = source.toPaymentMethod(),
        dateTimeLabel = formatDateTime(dateTime),
    )

    private fun PaymentSource.toPaymentMethod(): PaymentMethod? = when (this) {
        PaymentSource.PRIVAT24 -> privat24Method
        PaymentSource.EASYPAY -> easyPayMethod
        PaymentSource.MONOBANK -> monobankMethod
        PaymentSource.UNKNOWN -> null
    }

    private fun formatDateTime(dateTime: LocalDateTime): String {
        val today = clock.now().toJavaInstant().atZone(zoneId).toLocalDate()
        val pattern = if (dateTime.year == today.year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
        return dateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }
}
