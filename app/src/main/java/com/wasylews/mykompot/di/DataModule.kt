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
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.wasylews.mykompot.BuildConfig
import com.wasylews.mykompot.data.AuthApi
import com.wasylews.mykompot.data.EncryptedCredentialsStore
import com.wasylews.mykompot.data.EncryptedSessionCookieStore
import com.wasylews.mykompot.data.LoginPageTo401Interceptor
import com.wasylews.mykompot.data.PlansApi
import com.wasylews.mykompot.data.PortalApi
import com.wasylews.mykompot.data.PortalAuthenticator
import com.wasylews.mykompot.data.PortalCookieJar
import com.wasylews.mykompot.data.PortalSessionTagInterceptor
import com.wasylews.mykompot.data.SessionCookieStore
import com.wasylews.mykompot.data.model.PaymentDueNotifiedEntity
import com.wasylews.mykompot.data.model.PaymentDueNotifiedSerializer
import com.wasylews.mykompot.data.model.PaymentsEntity
import com.wasylews.mykompot.data.model.PaymentsSerializer
import com.wasylews.mykompot.data.model.ProfileEntity
import com.wasylews.mykompot.data.model.ProfileEntitySerializer
import com.wasylews.mykompot.data.model.TariffPlansEntity
import com.wasylews.mykompot.data.model.TariffPlansSerializer
import com.wasylews.mykompot.domain.repository.CredentialsStore
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
import java.util.concurrent.TimeUnit

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
    fun provideSessionCookieStore(context: Context): SessionCookieStore = EncryptedSessionCookieStore(context)

    @Singleton
    fun provideCredentialsStore(context: Context): CredentialsStore = EncryptedCredentialsStore(context)

    @Singleton
    fun provideCookieJar(store: SessionCookieStore): CookieJar = PortalCookieJar(store)

    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(cookieJar: CookieJar): OkHttpClient = okHttpClient(cookieJar)

    @Singleton
    @Named("portal")
    fun providePortalOkHttpClient(
        cookieJar: CookieJar,
        sessionCookieStore: SessionCookieStore,
        authenticator: PortalAuthenticator,
    ): OkHttpClient = okHttpClient(cookieJar)
        .newBuilder()
        .addInterceptor(PortalSessionTagInterceptor(sessionCookieStore))
        // Must be a network interceptor: OkHttp's retry layer (and therefore the authenticator)
        // sits inside application interceptors and only reacts to 401s it observes itself.
        .addNetworkInterceptor(LoginPageTo401Interceptor())
        .authenticator(authenticator)
        .build()

    @Singleton
    @Named("plans")
    fun providePlansOkHttpClient(): OkHttpClient = okHttpClient(CookieJar.NO_COOKIES)

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
    fun provideProfileDataStore(context: Context): DataStore<ProfileEntity?> =
        dataStore(context, "profile.json", ProfileEntitySerializer)

    @Singleton
    @Named("tariffPlans")
    fun provideTariffPlansDataStore(context: Context): DataStore<TariffPlansEntity?> =
        dataStore(context, "tariff_plans.json", TariffPlansSerializer)

    @Singleton
    @Named("payments")
    fun providePaymentsDataStore(context: Context): DataStore<PaymentsEntity?> =
        dataStore(context, "payments.json", PaymentsSerializer)

    @Singleton
    @Named("paymentDueNotified")
    fun providePaymentDueNotifiedDataStore(context: Context): DataStore<PaymentDueNotifiedEntity?> =
        dataStore(context, "payment_due_notified.json", PaymentDueNotifiedSerializer)
}

private fun <T : Any> dataStore(
    context: Context,
    fileName: String,
    serializer: Serializer<T?>,
): DataStore<T?> = DataStoreFactory.create(
    serializer = serializer,
    corruptionHandler = ReplaceFileCorruptionHandler { null },
    produceFile = { context.dataStoreFile(fileName) },
)

internal fun okHttpClient(cookieJar: CookieJar): OkHttpClient = OkHttpClient.Builder()
    .cookieJar(cookieJar)
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private const val CONNECT_TIMEOUT_SECONDS = 15L
private const val READ_TIMEOUT_SECONDS = 20L
private const val WRITE_TIMEOUT_SECONDS = 20L
private const val CALL_TIMEOUT_SECONDS = 30L

private fun htmlRetrofit(
    client: OkHttpClient,
    baseUrl: String,
): Retrofit = Retrofit.Builder()
    .client(client)
    .addConverterFactory(Kspoon.toFormat().asConverterFactory("text/html; charset=UTF8".toMediaType()))
    .baseUrl(baseUrl)
    .build()
