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

import com.wasylews.mykompot.BuildConfig
import com.wasylews.mykompot.data.PortalApi
import dev.burnoo.kspoon.Kspoon
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

@Module
@ComponentScan("com.wasylews.mykompot.data")
@Configuration
class DataModule {
    @Singleton
    fun providePortalApi(retrofit: Retrofit) = retrofit.create<PortalApi>()

    @Singleton
    fun provideRetrofit() = Retrofit
        .Builder()
        .addConverterFactory(
            Kspoon.toFormat().asConverterFactory("text/html; charset=UTF8".toMediaType()),
        ).baseUrl(BuildConfig.BASE_URL)
        .build()

    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
