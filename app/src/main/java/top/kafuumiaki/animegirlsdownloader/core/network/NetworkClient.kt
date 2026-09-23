package top.kafuumiaki.animegirlsdownloader.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import top.kafuumiaki.animegirlsdownloader.BuildConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SessionTokenProvider {
    private val token = AtomicReference<String?>(null)
    fun get(): String? = token.get()
    fun set(value: String?) = token.set(value)
}

object NetworkClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    fun create(tokenProvider: SessionTokenProvider): Pair<AnimeGirlsApi, OkHttpClient> {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val token = tokenProvider.get()
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(AnimeGirlsApi::class.java) to client
    }
}
