package com.example.data.gemini

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import android.util.Log

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // In-development API Key default to environment BuildConfig
    var customApiKey: String = ""

    private val apiKey: String
        get() = customApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null,
        isCreative: Boolean = false,
        usePro: Boolean = false,
        jsonMode: Boolean = false
    ): String {
        val model = if (usePro) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = if (isCreative) 0.8f else 0.2f,
                responseMimeType = if (jsonMode) "application/json" else null
            ),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        val currentKey = apiKey
        if (currentKey.isEmpty() || currentKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is set to template placeholder. Please verify your keys.")
            return "API_KEY_ERROR: Gemini API Key is missing. Please setup your GEMINI_API_KEY secret in Google AI Studio panel."
        }

        return try {
            val response = apiService.generateContent(
                model = model,
                apiKey = currentKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response content from Gemini AI."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            "Error: ${e.localizedMessage ?: "Unknown server or connection error."}"
        }
    }
}
