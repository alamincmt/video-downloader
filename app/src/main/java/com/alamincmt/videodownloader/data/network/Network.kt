package com.alamincmt.videodownloader.data.network

import com.alamincmt.videodownloader.data.api.FileDownloadApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

fun createRetrofitApi(): FileDownloadApi {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    // Retrofit's @Streaming doesn't work with [HttpLoggingInterceptor.Level.BODY]
    val loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
    return Retrofit.Builder()
        .client(
            OkHttpClient.Builder()
                .readTimeout(30L, TimeUnit.SECONDS)
                .writeTimeout(30L, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        )
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .baseUrl("https://file-examples.com/")
        .build()
        .create(FileDownloadApi::class.java)
}
