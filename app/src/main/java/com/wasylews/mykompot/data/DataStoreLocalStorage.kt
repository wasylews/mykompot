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
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import java.time.LocalDate

@Singleton
class DataStoreLocalStorage(
    @Named("profile") private val profileDataStore: DataStore<ProfileEntity?>,
    @Named("session") private val sessionDataStore: DataStore<String?>,
    @Named("tariffPlans") private val tariffPlansDataStore: DataStore<List<TariffPlanEntity>?>,
) : LocalStorage {
    override suspend fun saveProfile(profile: Profile) {
        profileDataStore.updateData { profile.toEntity() }
    }

    override fun profileFlow(): Flow<Profile?> = profileDataStore.data.map { it?.toDomain() }

    override fun sessionFlow(): Flow<String?> = sessionDataStore.data

    override suspend fun saveSession(sid: String) {
        sessionDataStore.updateData { sid }
    }

    override suspend fun clearProfile() {
        profileDataStore.updateData { null }
    }

    override suspend fun clearSession() {
        sessionDataStore.updateData { null }
    }

    override fun tariffPlansFlow(): Flow<List<TariffPlan>?> = tariffPlansDataStore.data.map { entities -> entities?.map { it.toDomain() } }

    override suspend fun saveTariffPlans(plans: List<TariffPlan>) {
        tariffPlansDataStore.updateData { plans.map { it.toEntity() } }
    }
}

@Serializable
data class ProfileEntity(
    val isActive: Boolean,
    val nextPaymentDate: String?,
    val tariffPlan: TariffPlanEntity,
)

@Serializable
data class TariffPlanEntity(
    val id: String,
    val name: String,
    val speed: Int,
    val price: Int,
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
