package com.example.skills.data.remote

import com.example.skills.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Groq OpenAI-compatible request/response models ──

data class GroqMessage(
    val role: String,    // "system", "user", or "assistant"
    val content: String
)

data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 4096
)

data class GroqChoice(
    val message: GroqMessage
)

data class GroqResponse(
    val choices: List<GroqChoice>?
)

// ── Service ──

@Singleton
class GeminiService @Inject constructor() {

    private val apiKey = BuildConfig.GROQ_API_KEY
    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val model = "openai/gpt-oss-120b"
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── System prompts ──

    private val chatSystemPrompt = """
        You are a structured requirements-gatherer for a Conversational Skill Builder.
        Your goal is to collect information through a sequential Q&A flow before autonomously generating a complete SKILL.md file.

        RULES:
        1. Ask exactly ONE clarifying question per message. Never a list.
        2. Follow this dependency chain: Domain -> Task -> Tone -> Constraints -> Examples -> Edge Cases.
        3. Refuse off-topic requests gracefully.
        4. When you have sufficient context (typically 6-8 questions), output the exact sentinel token [READY_TO_GENERATE] at the very end of your response.
    """.trimIndent()

    private val validatorSystemPrompt = """
        You are an expert AI Prompt Engineer and Security Validator. When given a skill definition in Markdown, provide a comprehensive review.
        
        Format your response exactly using this Markdown structure. The very first line MUST be the score.
        
        SCORE: [0-100]
        
        ### 🎯 Skill Summary
        *Brief 1-sentence summary of the skill's quality.*
        
        ### 🛡️ Safety & Security
        *   **Status**: [Safe / Warning / Critical]
        *   **Analysis**: Check for prompt injection vulnerabilities, jailbreaks, malicious shell commands, or harmful content.
        
        ### 💡 Suggestions for Improvement
        1.  **[Area 1]**: Actionable advice.
        2.  **[Area 2]**: Actionable advice.
        3.  **[Area 3]**: Actionable advice.
        
        ### 📝 Token Optimization
        *Suggest ways to make the prompt more concise without losing context to save on token costs.*
    """.trimIndent()

    // ── Core API call ──

    private suspend fun callGroq(messages: List<GroqMessage>): String = withContext(Dispatchers.IO) {
        val body = gson.toJson(GroqRequest(model = model, messages = messages))
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from Groq")

        if (!response.isSuccessful) {
            throw Exception("Groq API error ${response.code}: $responseBody")
        }

        val groqResponse = gson.fromJson(responseBody, GroqResponse::class.java)
        groqResponse.choices?.firstOrNull()?.message?.content
            ?: throw Exception("No choices returned from Groq")
    }

    // ── Public API ──

    /**
     * Validates a skill's markdown content using AI.
     */
    suspend fun validateSkill(markdown: String): String {
        val messages = listOf(
            GroqMessage(role = "system", content = validatorSystemPrompt),
            GroqMessage(role = "user", content = "Markdown Content to Analyze:\n---\n$markdown\n---")
        )
        return callGroq(messages)
    }

    /**
     * Sends a message in a conversational chat flow.
     * The caller is responsible for maintaining the message history.
     */
    suspend fun chat(conversationHistory: List<GroqMessage>): String {
        val messages = listOf(
            GroqMessage(role = "system", content = chatSystemPrompt)
        ) + conversationHistory
        return callGroq(messages)
    }

    /**
     * Compiles the full chat history into a final SKILL.md document.
     */
    suspend fun compileSkillMarkdown(chatHistory: String): String {
        val messages = listOf(
            GroqMessage(
                role = "system",
                content = "You are an expert Prompt Engineer. Generate a fully formed SKILL.md from the conversation. Output ONLY raw Markdown, no pleasantries."
            ),
            GroqMessage(
                role = "user",
                content = """
                    Based on the following conversation history, generate a SKILL.md:
                    ---
                    $chatHistory
                    ---
                    
                    Required format:
                    # [Skill Name]
                    ## Persona
                    [Description of AI personality]
                    ## Context
                    [Constraints and background info]
                    ## Instructions
                    [Step-by-step logic]
                    ## Example
                    **User:** [Sample Prompt]
                    **AI:** [Sample Response]
                """.trimIndent()
            )
        )
        return callGroq(messages)
    }
}
