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

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _isGeneratingSkill = MutableStateFlow(false)
    val isGeneratingSkill: StateFlow<Boolean> = _isGeneratingSkill.asStateFlow()

    private val _generatedSkillMarkdown = MutableStateFlow<String?>(null)
    val generatedSkillMarkdown: StateFlow<String?> = _generatedSkillMarkdown.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<Pair<Boolean, Boolean>?>(null) // Pair of <Success, WasPublished>
    val saveResult: StateFlow<Pair<Boolean, Boolean>?> = _saveResult.asStateFlow()

    init {
        startNewChat()
    }

    private fun startNewChat() {
        viewModelScope.launch {
            _isTyping.value = true
            try {
                // Kickstart the conversation
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
        if (userText.isBlank() || _isTyping.value) return

        addMessage(ChatMessage(text = userText, isFromUser = true))
        conversationHistory.add(GroqMessage(role = "user", content = userText))
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val reply = geminiService.chat(conversationHistory)
                conversationHistory.add(GroqMessage(role = "assistant", content = reply))

                // Check for sentinel token
                if (reply.contains("[READY_TO_GENERATE]")) {
                    val cleanReply = reply.replace("[READY_TO_GENERATE]", "").trim()
                    if (cleanReply.isNotEmpty()) {
                        addMessage(ChatMessage(text = cleanReply, isFromUser = false))
                    }
                    generateFinalSkill()
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

    private fun generateFinalSkill() {
        viewModelScope.launch {
            _isGeneratingSkill.value = true
            try {
                val historyText = _messages.value.joinToString("\n") {
                    if (it.isFromUser) "User: ${it.text}" else "AI: ${it.text}"
                }
                val markdown = geminiService.compileSkillMarkdown(historyText)
                _generatedSkillMarkdown.value = markdown
            } catch (e: Exception) {
                _generatedSkillMarkdown.value = "# Error\nFailed to generate skill: ${e.message}"
            } finally {
                _isGeneratingSkill.value = false
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun saveSkill(publishToMarketplace: Boolean) {
        val user = auth.currentUser ?: return
        val markdown = _generatedSkillMarkdown.value ?: return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val skillId = java.util.UUID.randomUUID().toString()
                
                // Upload to Storage
                val storageRef = storage.reference.child("skills/$skillId/SKILL.md")
                val bytes = markdown.toByteArray(Charsets.UTF_8)
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Derive title from markdown
                var title = "Generated Skill"
                val firstLine = markdown.lines().firstOrNull { it.trim().startsWith("#") }
                if (firstLine != null) {
                    title = firstLine.replace("#", "").trim()
                }

                val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Anonymous"

                val skillData = hashMapOf(
                    "id" to skillId,
                    "title" to title,
                    "description" to "Generated via conversational builder.",
                    "category" to "code", // Generic fallback
                    "fileUrl" to downloadUrl,
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "installCount" to 0,
                    "rating" to 0.0,
                    "isTrending" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                if (publishToMarketplace) {
                    firestore.collection("skills").document(skillId).set(skillData).await()
                } else {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("drafts")
                        .document(skillId)
                        .set(skillData)
                        .await()
                }
                
                _saveResult.value = Pair(true, publishToMarketplace)
            } catch (e: Exception) {
                _saveResult.value = Pair(false, publishToMarketplace)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
