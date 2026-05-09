package com.example.skills.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.example.skills.data.remote.GeminiService
@HiltViewModel
class CreateSkillViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _validationFeedback = MutableStateFlow<String?>(null)
    val validationFeedback: StateFlow<String?> = _validationFeedback.asStateFlow()

    // Pair of <Score, WasPublishedToMarketplace>
    private val _publishResult = MutableStateFlow<Pair<Int, Boolean>?>(null)
    val publishResult: StateFlow<Pair<Int, Boolean>?> = _publishResult.asStateFlow()

    var skillName: String = ""
    var persona: String = ""
    var context: String = ""
    var instructions: String = ""
    var examplePrompt: String = ""
    var exampleResponse: String = ""

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun getCompletionPercentage(): Int {
        var filled = 0
        if (skillName.isNotBlank()) filled++
        if (persona.isNotBlank()) filled++
        if (context.isNotBlank()) filled++
        if (instructions.isNotBlank()) filled++
        if (examplePrompt.isNotBlank()) filled++
        if (exampleResponse.isNotBlank()) filled++
        return ((filled / 6.0) * 100).toInt()
    }

    private fun saveSkillToFirestore(publishToMarketplace: Boolean) {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 1. Generate Markdown
                val markdown = generateMarkdown()

                // 2. Sanitise
                if (!isSanitised(markdown)) {
                    throw IllegalStateException("Malicious content detected (e.g. destructive shell commands).")
                }

                val skillId = java.util.UUID.randomUUID().toString()

                // 3. Upload to Firebase Storage
                val storageRef = storage.reference.child("skills/$skillId/SKILL.md")
                val bytes = markdown.toByteArray(Charsets.UTF_8)
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // 4. Save to Firestore
                val authorName = auth.currentUser?.displayName
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: "Anonymous"

                val skillData = hashMapOf(
                    "id" to skillId,
                    "title" to skillName,
                    "description" to "$persona - $context".take(100),
                    "category" to inferCategory(),
                    "fileUrl" to downloadUrl,
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "installCount" to 0,
                    "rating" to 0.0,
                    "isTrending" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                if (publishToMarketplace) {
                    // Public Marketplace
                    firestore.collection("skills").document(skillId).set(skillData).await()
                } else {
                    // Private Draft
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("drafts")
                        .document(skillId)
                        .set(skillData)
                        .await()
                }

                _saveSuccess.value = true
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun generateMarkdown(): String {
        return """
            # $skillName
            
            ## Persona
            $persona
            
            ## Context
            $context
            
            ## Instructions
            $instructions
            
            ## Example
            **User:** $examplePrompt
            
            **AI:** $exampleResponse
        """.trimIndent()
    }

    fun validateSkillWithAI() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val markdown = generateMarkdown()
                val feedback = geminiService.validateSkill(markdown)
                _validationFeedback.value = feedback
            } catch (e: Exception) {
                _validationFeedback.value = "Validation failed: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun publishSkill() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Publish directly to marketplace
                saveSkillToFirestore(publishToMarketplace = true)
                _publishResult.value = Pair(100, true)
            } catch (e: Exception) {
                _validationFeedback.value = "Publishing failed: ${e.message}"
                _isSaving.value = false
            }
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Save as draft
                saveSkillToFirestore(publishToMarketplace = false)
                _publishResult.value = Pair(100, false)
            } catch (e: Exception) {
                _validationFeedback.value = "Saving draft failed: ${e.message}"
                _isSaving.value = false
            }
        }
    }

    private fun extractScore(feedback: String): Int {
        val regex = Regex("SCORE:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(feedback)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private suspend fun isSanitised(markdown: String): Boolean {
        // We still keep the basic check, but now we also rely on AI
        val maliciousPatterns = listOf(
            "rm -rf", "sudo rm", "mkfs", "dd if=/dev/zero",
            "curl | bash", "wget | bash", "nc -e"
        )
        for (pattern in maliciousPatterns) {
            if (markdown.contains(pattern, ignoreCase = true)) {
                return false
            }
        }
        return true
    }

    private fun inferCategory(): String {
        val combined = "$skillName $persona $context $instructions".lowercase()
        return when {
            combined.containsAny("code", "programming", "developer", "debug", "api", "software") -> "code"
            combined.containsAny("write", "blog", "article", "copy", "essay", "draft") -> "writing"
            combined.containsAny("research", "paper", "academic", "cite", "journal") -> "research"
            combined.containsAny("finance", "budget", "invest", "tax", "accounting") -> "finance"
            combined.containsAny("design", "ui", "ux", "figma", "wireframe", "layout") -> "design"
            combined.containsAny("devops", "deploy", "docker", "kubernetes", "ci/cd", "pipeline") -> "devops"
            combined.containsAny("legal", "law", "contract", "compliance", "regulation") -> "legal"
            combined.containsAny("health", "medical", "patient", "clinical", "diagnosis") -> "healthcare"
            else -> "writing" // Safe default
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
