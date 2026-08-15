package com.dearly.app.di

import android.content.Context
import androidx.room.Room
import com.dearly.app.BuildConfig
import com.dearly.app.data.local.ContactDao
import com.dearly.app.data.local.DearlyDatabase
import com.dearly.app.data.local.MedicationDao
import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.session.TokenStore
import com.dearly.app.data.session.SessionAuthenticator
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DearlyDatabase =
        Room.databaseBuilder(context, DearlyDatabase::class.java, "dearly.db").build()

    @Provides fun contactDao(database: DearlyDatabase): ContactDao = database.contactDao()
    @Provides fun medicationDao(database: DearlyDatabase): MedicationDao = database.medicationDao()
    @Provides @Singleton fun gson(): Gson = Gson()

    @Provides
    @Singleton
    fun httpClient(tokenStore: TokenStore, authenticator: SessionAuthenticator): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = tokenStore.accessToken()?.let { token ->
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } ?: chain.request()
                chain.proceed(request)
            }
            .authenticator(authenticator)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun api(client: OkHttpClient, gson: Gson): DearlyApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DearlyApi::class.java)
}
