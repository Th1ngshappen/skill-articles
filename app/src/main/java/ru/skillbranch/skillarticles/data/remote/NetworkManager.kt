package ru.skillbranch.skillarticles.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.skillbranch.skillarticles.AppConfig
import ru.skillbranch.skillarticles.data.JsonConverter.moshi
import ru.skillbranch.skillarticles.data.remote.interceptors.ErrorStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.NetworkStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.TokenAuthenticator
import java.util.concurrent.TimeUnit

object NetworkManager {
    val api: RestService by lazy {
        // client
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient().newBuilder()
            // по умолчанию таймауты = 10 секунд
            .readTimeout(2, TimeUnit.SECONDS) // socket timeout (GET)
            .writeTimeout(5, TimeUnit.SECONDS) // socket timeout (POST, PUT, etc)
            .authenticator(TokenAuthenticator())
            .addInterceptor(NetworkStatusInterceptor()) // intercept network status
            .addInterceptor(logging) // intercept req/res for logging
            .addInterceptor(ErrorStatusInterceptor()) // intercept status errors
            .build()

        // retrofit
        val retrofit = Retrofit.Builder()
            .client(client) // set http client
            .addConverterFactory(MoshiConverterFactory.create(moshi)) // set json converter/parser
            .baseUrl(AppConfig.BASE_URL)
            .build()

        retrofit.create(RestService::class.java)

    }
}