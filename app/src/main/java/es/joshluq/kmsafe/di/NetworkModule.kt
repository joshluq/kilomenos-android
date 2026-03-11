package es.joshluq.kmsafe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.authkit.network.sdk.NetworkKit
import es.joshluq.authkit.sdk.AuthKit
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.data.remote.api.AuthApiService
import es.joshluq.kmsafe.data.remote.api.AuthenticatedAuthApiService
import es.joshluq.kmsafe.data.remote.api.RentingApiService
import es.joshluq.kmsafe.data.remote.api.StorageApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val apiKeyInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("apikey", BuildConfig.API_KEY)
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addNetworkInterceptor(loggingInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedOkHttpClient(
        baseClient: OkHttpClient,
        authKit: AuthKit
    ): OkHttpClient {
        val networkKit = authKit.plugin<NetworkKit>() ?: error("NetworkKit plugin not installed")
        return baseClient.newBuilder()
            .addInterceptor(networkKit.interceptor())
            .authenticator(networkKit.authenticator())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedRetrofit(
        @Authenticated okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticatedAuthApiService(@Authenticated retrofit: Retrofit): AuthenticatedAuthApiService {
        return retrofit.create(AuthenticatedAuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRentingApiService(@Authenticated retrofit: Retrofit): RentingApiService {
        return retrofit.create(RentingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStorageApiService(@Authenticated retrofit: Retrofit): StorageApiService {
        return retrofit.create(StorageApiService::class.java)
    }
}
