package com.example.skills.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skills.data.remote.GeminiService
import com.example.skills.data.remote.GroqMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean
)

enum class ChatPhase { CHATTING, GENERATING, PREVIEW }

@HiltViewModel
class ChatBuilderViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    // Groq conversation history (role + content pairs)
    private val conversationHistory = mutableListOf<GroqMessage>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _phase = MutableStateFlow(ChatPhase.CHATTING)
    val phase: StateFlow<ChatPhase> = _phase.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // The compiled markdown, available after GENERATING phase
    private val _generatedMarkdown = MutableStateFlow<String?>(null)
    val generatedMarkdown: StateFlow<String?> = _generatedMarkdown.asStateFlow()

    // Validation results, available after GENERATING phase
    private val _validationScore = MutableStateFlow(-1) // -1 = not yet validated
    val validationScore: StateFlow<Int> = _validationScore.asStateFlow()

    private val _validationFeedback = MutableStateFlow<String?>(null)
    val validationFeedback: StateFlow<String?> = _validationFeedback.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var currentDraftId: String? = null

    private val _saveResult = MutableStateFlow<Pair<Boolean, Boolean>?>(null) // <Success, WasPublished>
    val saveResult: StateFlow<Pair<Boolean, Boolean>?> = _saveResult.asStateFlow()

    init {
        startNewChat()
    }

    private fun startNewChat() {
        viewModelScope.launch {
            _isTyping.value = true
            try {
                val initialMessage = GroqMessage(
                    role = "user",
                    content = "Hello! I want to build a new skill. Please ask me your first question about the domain or intent."
                )
                conversationHistory.add(initialMessage)

                val reply = geminiService.chat(conversationHistory)
                val cleanReply = reply.replace("[READY_TO_GENERATE]", "").trim()
                conversationHistory.add(GroqMessage(role = "assistant", content = reply))

                if (cleanReply.isNotEmpty()) {
                    addMessage(ChatMessage(text = cleanReply, isFromUser = false))
                }
            } catch (e: Exception) {
                addMessage(ChatMessage(text = "Error starting chat: ${e.message}", isFromUser = false))
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isTyping.value || _phase.value != ChatPhase.CHATTING) return

        addMessage(ChatMessage(text = userText, isFromUser = true))
        conversationHistory.add(GroqMessage(role = "user", content = userText))
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val reply = geminiService.chat(conversationHistory)
                conversationHistory.add(GroqMessage(role = "assistant", content = reply))

                // Check for sentinel token → auto-transition to generating
                if (reply.contains("[READY_TO_GENERATE]")) {
                    val cleanReply = reply.replace("[READY_TO_GENERATE]", "").trim()
                    if (cleanReply.isNotEmpty()) {
                        addMessage(ChatMessage(text = cleanReply, isFromUser = false))
                    }
                    _isTyping.value = false
                    generatePreview()
                } else {
                    addMessage(ChatMessage(text = reply.trim(), isFromUser = false))
                }
            } catch (e: Exception) {
                addMessage(ChatMessage(text = "Error: ${e.message}", isFromUser = false))
            } finally {
                _isTyping.value = false
            }
        }
    }

    /** Transition to GENERATING → compile markdown → validate → PREVIEW */
    private fun generatePreview() {
        _phase.value = ChatPhase.GENERATING

        viewModelScope.launch {
            try {
                // Step 1: Compile the conversation into SKILL.md
                val historyText = _messages.value.joinToString("\n") {
                    if (it.isFromUser) "User: ${it.text}" else "AI: ${it.text}"
                }
                val markdown = geminiService.compileSkillMarkdown(historyText)
                _generatedMarkdown.value = markdown

                // Step 2: Validate the generated skill
                try {
                    val feedback = geminiService.validateSkill(markdown)
                    _validationFeedback.value = feedback
                    val scoreMatch = Regex("SCORE:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(feedback)
                    _validationScore.value = scoreMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                } catch (e: Exception) {
                    // Validation failed but we still have the skill — show preview anyway
                    _validationFeedback.value = "Validation unavailable: ${e.message}"
                    _validationScore.value = -1
                }

                _phase.value = ChatPhase.PREVIEW
            } catch (e: Exception) {
                _generatedMarkdown.value = "# Error\nFailed to generate skill: ${e.message}"
                _validationScore.value = -1
                _phase.value = ChatPhase.PREVIEW
            }
        }
    }

    /** Save the already-generated markdown. Score gates Publish (>= 70). */
    fun saveSkill(publishToMarketplace: Boolean) {
        val user = auth.currentUser ?: return
        val markdown = _generatedMarkdown.value ?: return

        // Gate: if user wants to publish but score < 70, downgrade to draft
        val score = _validationScore.value
        val actuallyPublish = publishToMarketplace && (score < 0 || score >= 70)

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Derive title from markdown
                var title = "Generated Skill"
                val firstLine = markdown.lines().firstOrNull { it.trim().startsWith("#") }
                if (firstLine != null) {
                    title = firstLine.replace("#", "").trim()
                }

                val sanitizedTitle = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                val shortUuid = java.util.UUID.randomUUID().toString().substring(0, 8)
                val generatedId = if (sanitizedTitle.isNotEmpty()) "$sanitizedTitle-$shortUuid" else "skill-$shortUuid"
                val skillId = currentDraftId ?: generatedId

                // Upload to Storage
                val storageRef = storage.reference.child("skills/$skillId/SKILL.md")
                val bytes = markdown.toByteArray(Charsets.UTF_8)
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Anonymous"

                val skillData = hashMapOf(
                    "id" to skillId,
                    "title" to title,
                    "description" to "Generated via conversational builder.",
                    "category" to "code",
                    "fileUrl" to downloadUrl,
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "installCount" to 0,
                    "rating" to 0.0,
                    "isTrending" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                if (actuallyPublish) {
                    firestore.collection("skills").document(skillId).set(skillData).await()
                    
                    if (currentDraftId != null) {
                        firestore.collection("users").document(user.uid).collection("drafts").document(currentDraftId!!).delete().await()
                        currentDraftId = null
                    }
                } else {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("drafts")
                        .document(skillId)
                        .set(skillData)
                        .await()
                    currentDraftId = skillId
                }

                _saveResult.value = Pair(true, actuallyPublish)
            } catch (e: Exception) {
                _saveResult.value = Pair(false, publishToMarketplace)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
}
