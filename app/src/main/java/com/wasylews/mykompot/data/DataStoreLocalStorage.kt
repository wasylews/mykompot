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

package com.wasylews.mykompot.data

import androidx.datastore.core.DataStore
import com.wasylews.mykompot.data.model.PaymentEntity
import com.wasylews.mykompot.data.model.PaymentsEntity
import com.wasylews.mykompot.data.model.ProfileEntity
import com.wasylews.mykompot.data.model.TariffPlanEntity
import com.wasylews.mykompot.data.model.TariffPlansEntity
import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import java.time.LocalDate
import java.time.LocalDateTime

@Singleton
class DataStoreLocalStorage(
    @Named("profile") private val profileDataStore: DataStore<ProfileEntity?>,
    @Named("tariffPlans") private val tariffPlansDataStore: DataStore<TariffPlansEntity?>,
    @Named("payments") private val paymentsDataStore: DataStore<PaymentsEntity?>,
) : LocalStorage {
    override suspend fun saveProfile(profile: Profile) {
        profileDataStore.updateData { profile.toEntity() }
    }

    override fun profileFlow(): Flow<Profile?> = profileDataStore.data.map { it?.toDomain() }

    override suspend fun clearProfile() {
        profileDataStore.updateData { null }
    }

    override fun tariffPlansFlow(): Flow<TariffPlans?> = tariffPlansDataStore.data.map { it?.toDomain() }

    override suspend fun saveTariffPlans(tariffPlans: TariffPlans) {
        tariffPlansDataStore.updateData { tariffPlans.toEntity() }
    }

    override fun paymentsFlow(): Flow<Payments?> = paymentsDataStore.data.map { it?.toDomain() }

    override suspend fun savePayments(payments: Payments) {
        paymentsDataStore.updateData { payments.toEntity() }
    }

    override suspend fun clearPayments() {
        paymentsDataStore.updateData { null }
    }
}

private fun PaymentsEntity.toDomain() = Payments(
    items = items.map { it.toDomain() },
    hasMore = hasMore,
)

private fun Payments.toEntity() = PaymentsEntity(
    items = items.map { it.toEntity() },
    hasMore = hasMore,
)

private fun PaymentEntity.toDomain() = Payment(
    dateTime = LocalDateTime.parse(dateTime),
    amount = amount,
    tariffName = tariffName,
    source = runCatching { PaymentSource.valueOf(source) }.getOrDefault(PaymentSource.UNKNOWN),
)

private fun Payment.toEntity() = PaymentEntity(
    dateTime = dateTime.toString(),
    amount = amount,
    tariffName = tariffName,
    source = source.name,
)

private fun TariffPlansEntity.toDomain() = TariffPlans(
    plans = plans.map { it.toDomain() },
    etag = etag,
    lastModified = lastModified,
)

private fun TariffPlans.toEntity() = TariffPlansEntity(
    plans = plans.map { it.toEntity() },
    etag = etag,
    lastModified = lastModified,
)

private fun Profile.toEntity() = ProfileEntity(
    isActive = isActive,
    nextPaymentDate = nextPaymentDate?.toString(),
    tariffPlan = tariffPlan.toEntity(),
)

private fun ProfileEntity.toDomain() = Profile(
    isActive = isActive,
    nextPaymentDate = nextPaymentDate?.let { LocalDate.parse(nextPaymentDate) },
    tariffPlan = tariffPlan.toDomain(),
)

private fun TariffPlan.toEntity() = TariffPlanEntity(
    id = id,
    name = name,
    speed = speed,
    price = price,
)

private fun TariffPlanEntity.toDomain() = TariffPlan(
    id = id,
    name = name,
    speed = speed,
    price = price,
)
