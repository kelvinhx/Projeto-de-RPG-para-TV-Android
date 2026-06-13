package com.example.rpg

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// --- Models for Gemini API ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    @Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    @Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    @Json(name = "voiceConfig") val voiceConfig: VoiceConfig
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "schema") val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "text") val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseFormat") val responseFormat: ResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "speechConfig") val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearchTool

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "googleSearch") val googleSearch: GoogleSearchTool? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @Json(name = "tools") val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

// --- Engine-specific Structured JSON Response ---

@JsonClass(generateAdapter = true)
data class PlayerUpdate(
    @Json(name = "level") val level: Int?,
    @Json(name = "experience") val experience: Int?,
    @Json(name = "unassignedPoints") val unassignedPoints: Int?,
    @Json(name = "hp") val hp: Int?,
    @Json(name = "maxHp") val maxHp: Int?,
    @Json(name = "mp") val mp: Int?,
    @Json(name = "maxMp") val maxMp: Int?,
    @Json(name = "gold") val gold: Int?,
    @Json(name = "strength") val strength: Int?,
    @Json(name = "agility") val agility: Int?,
    @Json(name = "intelligence") val intelligence: Int?,
    @Json(name = "vitality") val vitality: Int?,
    @Json(name = "perception") val perception: Int?,
    @Json(name = "willpower") val willpower: Int?,
    @Json(name = "itemsGained") val itemsGained: List<Item>?,
    @Json(name = "itemsLost") val itemsLost: List<String>?, // list of item names
    @Json(name = "skillsGained") val skillsGained: List<Skill>?,
    @Json(name = "titlesGained") val titlesGained: List<String>?,
    @Json(name = "scarsGained") val scarsGained: List<String>?
)

@JsonClass(generateAdapter = true)
data class WorldUpdate(
    @Json(name = "region") val region: String?,
    @Json(name = "timeOfDay") val timeOfDay: String?,
    @Json(name = "rotLevel") val rotLevel: Int?,
    @Json(name = "locationDescription") val locationDescription: String?,
    @Json(name = "questEvents") val questEvents: List<String>?
)

@JsonClass(generateAdapter = true)
data class NpcUpdate(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "affinityChange") val affinityChange: Int?, // change to apply e.g. +5, -10
    @Json(name = "emotion") val emotion: String?,
    @Json(name = "memoryAddition") val memoryAddition: String?
)

@JsonClass(generateAdapter = true)
data class GMResponse(
    @Json(name = "narrative") val narrative: String,
    @Json(name = "player_update") val playerUpdate: PlayerUpdate?,
    @Json(name = "world_update") val worldUpdate: WorldUpdate?,
    @Json(name = "npc_updates") val npcUpdates: List<NpcUpdate>?,
    @Json(name = "curated_options") val curatedOptions: List<String>?,
    @Json(name = "combat_active") val combatActive: Boolean = false
)

// --- Retrofit API Client ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val moshiInstance: Moshi get() = moshi
}

// --- Gemini Repository for RPG calls ---

class GeminiRepository {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    private val moshi = RetrofitClient.moshiInstance

    init {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiRepository", "Warning: GEMINI_API_KEY is not configured or uses placeholder.")
        }
    }

    /**
     * Transcribe player audio recorded from TV remote using gemini-3.5-flash
     */
    suspend fun transcribeAudio(audioFileBytes: ByteArray): String {
        val base64Data = Base64.encodeToString(audioFileBytes, Base64.NO_WRAP)
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(inlineData = InlineData(mimeType = "audio/wav", data = base64Data)),
                        Part(text = "Transcreva este áudio em português com altíssima exatidão. O áudio contém um comando de voz de um jogador de RPG clássico. Forneça APENAS a transcrição em texto puro, sem aspas, preâmbulos, observações ou explicações de IA. Se o áudio estiver vazio ou ininteligível, retorne uma string vazia.")
                    )
                )
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            val txt = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            Log.d("GeminiRepository", "Transcription outcome: '$txt'")
            txt
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error during audio transcription: ${e.message}", e)
            ""
        }
    }

    /**
     * Call general narrator turn logic
     */
    suspend fun processTurn(
        gameStateJson: String,
        action: String,
        systemPrompt: String
    ): GMResponse? {
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "ESTADO DO JOGO ATUAL (JSON):\n$gameStateJson\n\nACAO DO JOGADOR:\n$action")
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.8f,
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            ),
            tools = listOf(Tool(googleSearch = GoogleSearchTool()))
        )

        return try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            val jsonTxt = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d("GeminiRepository", "RAW GM Response: $jsonTxt")
            
            val adapter = moshi.adapter(GMResponse::class.java)
            adapter.fromJson(jsonTxt)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error during GM turn processing: ${e.message}", e)
            null
        }
    }

    /**
     * Converts a piece of narration into beautiful speech audio via model gemini-3.1-flash-tts-preview
     * Returns the raw bytes of the generated WAV file, or null on failure.
     */
    suspend fun convertTextToSpeech(narratorText: String): ByteArray? {
        // Prepare speechConfig to get real audio
        val voiceName = "Kore" // Prebuilt Gemini voices: Kore, Puck, Fenrir, etc.
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = narratorText)))
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                    )
                )
            )
        )

        return try {
            Log.d("GeminiRepository", "Requesting TTS from gemini-3.1-flash-tts-preview for text: '$narratorText'")
            val response = RetrofitClient.service.generateContent("gemini-3.1-flash-tts-preview", apiKey, request)
            val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
            val inlineData = part?.inlineData
            if (inlineData != null && inlineData.data.isNotEmpty()) {
                val decoded = Base64.decode(inlineData.data, Base64.DEFAULT)
                Log.d("GeminiRepository", "Successfully received TTS bytes: ${decoded.size} bytes")
                decoded
            } else {
                Log.w("GeminiRepository", "No audio inlineData found in response candidates.")
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error during gemini-3.1-flash-tts-preview TTS generation: ${e.message}", e)
            null
        }
    }
}
