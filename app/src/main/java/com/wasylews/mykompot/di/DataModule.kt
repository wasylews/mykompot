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

package com.wasylews.mykompot.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.wasylews.mykompot.BuildConfig
import com.wasylews.mykompot.data.AuthApi
import com.wasylews.mykompot.data.PlansApi
import com.wasylews.mykompot.data.PortalApi
import com.wasylews.mykompot.data.PortalCookieJar
import com.wasylews.mykompot.data.SessionCookieStore
import com.wasylews.mykompot.data.SharedPreferencesSessionCookieStore
import com.wasylews.mykompot.data.model.PaymentsEntity
import com.wasylews.mykompot.data.model.PaymentsSerializer
import com.wasylews.mykompot.data.model.ProfileEntity
import com.wasylews.mykompot.data.model.ProfileEntitySerializer
import com.wasylews.mykompot.data.model.SessionSerializer
import com.wasylews.mykompot.data.model.TariffPlansEntity
import com.wasylews.mykompot.data.model.TariffPlansSerializer
import dev.burnoo.kspoon.Kspoon
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@ComponentScan("com.wasylews.mykompot.data")
@Configuration
class DataModule {
    @Singleton
    fun provideAuthApi(
        @Named("auth") retrofit: Retrofit,
    ): AuthApi = retrofit.create()

    @Singleton
    fun providePortalApi(
        @Named("portal") retrofit: Retrofit,
    ): PortalApi = retrofit.create()

    @Singleton
    fun providePlansApi(
        @Named("plans") retrofit: Retrofit,
    ): PlansApi = retrofit.create()

    @Singleton
    fun provideSessionCookieStore(context: Context): SessionCookieStore = SharedPreferencesSessionCookieStore(context)

    @Singleton
    fun provideCookieJar(store: SessionCookieStore): CookieJar = PortalCookieJar(store)

    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(cookieJar: CookieJar): OkHttpClient = okHttpClient(cookieJar)

    @Singleton
    @Named("portal")
    fun providePortalOkHttpClient(cookieJar: CookieJar): OkHttpClient = okHttpClient(cookieJar)

    @Singleton
    @Named("plans")
    fun providePlansOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(CookieJar.NO_COOKIES)
        .build()

    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") client: OkHttpClient,
    ): Retrofit = htmlRetrofit(client, BuildConfig.BASE_URL)

    @Singleton
    @Named("portal")
    fun providePortalRetrofit(
        @Named("portal") client: OkHttpClient,
    ): Retrofit = htmlRetrofit(client, BuildConfig.BASE_URL)

    @Singleton
    @Named("plans")
    fun providePlansRetrofit(
        @Named("plans") client: OkHttpClient,
    ): Retrofit = htmlRetrofit(client, BuildConfig.PLANS_BASE_URL + "/")

    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Singleton
    @Named("profile")
    fun provideProfileDataStore(context: Context): DataStore<ProfileEntity?> = DataStoreFactory.create(
        serializer = ProfileEntitySerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        produceFile = { context.dataStoreFile("profile.json") },
    )

    @Singleton
    @Named("session")
    fun provideSessionDataStore(context: Context): DataStore<String?> = DataStoreFactory.create(
        serializer = SessionSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        produceFile = { context.dataStoreFile("session.json") },
    )

    @Singleton
    @Named("tariffPlans")
    fun provideTariffPlansDataStore(context: Context): DataStore<TariffPlansEntity?> = DataStoreFactory.create(
        serializer = TariffPlansSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        produceFile = { context.dataStoreFile("tariff_plans.json") },
    )

    @Singleton
    @Named("payments")
    fun providePaymentsDataStore(context: Context): DataStore<PaymentsEntity?> = DataStoreFactory.create(
        serializer = PaymentsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        produceFile = { context.dataStoreFile("payments.json") },
    )
}

private fun okHttpClient(cookieJar: CookieJar): OkHttpClient = OkHttpClient.Builder()
    .cookieJar(cookieJar)
    .build()

private fun htmlRetrofit(
    client: OkHttpClient,
    baseUrl: String,
): Retrofit = Retrofit.Builder()
    .client(client)
    .addConverterFactory(Kspoon.toFormat().asConverterFactory("text/html; charset=UTF8".toMediaType()))
    .baseUrl(baseUrl)
    .build()
