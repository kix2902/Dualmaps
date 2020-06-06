package com.redinput.dualmaps.data

import com.redinput.dualmaps.BuildConfig
import com.redinput.dualmaps.GeoNames
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


object NetworkRepository : NetworkSource {

    private const val GEONAMES_RANDOM_URL = "https://api.3geonames.org/?randomland=yes&json=1"

    private val client: OkHttpClient

    private val moshi = Moshi.Builder().build()
    private val adapterGeoNamesResponse = moshi.adapter(GeoNames.Response::class.java)

    init {
        val logging = HttpLoggingInterceptor().apply {
            when (BuildConfig.DEBUG) {
                true -> setLevel(HttpLoggingInterceptor.Level.BODY)
                false -> setLevel(HttpLoggingInterceptor.Level.NONE)
            }
        }
        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    override fun getRandomLocation(): GeoNames.Response {
        val request = Request.Builder()
            .url(GEONAMES_RANDOM_URL)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            return adapterGeoNamesResponse.fromJson(response.body!!.source())!!

        } else {
            throw IOException("Unexpected response ${response.code}")
        }
    }
}