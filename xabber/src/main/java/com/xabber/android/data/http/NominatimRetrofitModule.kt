package com.xabber.android.data.http

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

object NominatimRetrofitModule {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    private val client = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api : NominatimApi = retrofit.create(NominatimApi::class.java)

    interface NominatimApi {
        @GET("reverse?format=jsonv2")
        suspend fun fromLonLat(@Query("lon") lon: Double, @Query("lat") lat: Double): Place

        @GET("search?format=jsonv2")
        suspend fun search(@Query("q") searchString: String): List<Place>
    }
}

@Serializable
data class Place(
    @SerialName("display_name")
    val displayName: String,

    val lon: Double,
    val lat: Double,
)