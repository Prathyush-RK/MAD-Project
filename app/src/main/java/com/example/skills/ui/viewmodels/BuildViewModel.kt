package com.example.skills.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skills.data.model.SkillDraft
import com.example.skills.data.model.SkillTemplate
import com.example.skills.data.repository.SkillRepository
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

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val repository: SkillRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _templates = MutableStateFlow<List<SkillTemplate>>(emptyList())
    val templates: StateFlow<List<SkillTemplate>> = _templates.asStateFlow()

    private val _drafts = MutableStateFlow<List<SkillDraft>>(emptyList())
    val drafts: StateFlow<List<SkillDraft>> = _drafts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTemplates()
        loadDrafts()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("templates")
                    .get()
                    .await()
                if (snapshot.isEmpty) {
                    _templates.value = getDefaultTemplates()
                } else {
                    val liveTemplates = snapshot.documents.map { doc ->
                        SkillTemplate(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            promptTemplate = doc.getString("promptTemplate") ?: ""
                        )
                    }
                    _templates.value = liveTemplates
                }
            } catch (e: Exception) {
                // Firestore templates not seeded yet — show defaults
                _templates.value = getDefaultTemplates()
            }
        }
    }

    private fun getDefaultTemplates(): List<SkillTemplate> {
        return listOf(
            SkillTemplate(
                id = "t1",
                name = "Design Assistant",
                description = "Generate UI/UX components and design tokens",
                iconResId = android.R.drawable.ic_menu_edit
            ),
            SkillTemplate(
                id = "t2",
                name = "Security Auditor",
                description = "Scan code for vulnerabilities and best practices",
                iconResId = android.R.drawable.ic_lock_idle_lock
            ),
            SkillTemplate(
                id = "t3",
                name = "Research Assistant",
                description = "Summarize papers, cite sources, and gather facts",
                iconResId = android.R.drawable.ic_menu_search
            )
        )
    }

    private fun loadDrafts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedDrafts = repository.getMyDrafts()
                _drafts.value = fetchedDrafts
            } catch (e: Exception) {
                _drafts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshDrafts() {
        loadDrafts()
    }

    fun importMarkdownSkill(filename: String, markdownContent: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val skillId = java.util.UUID.randomUUID().toString()
                
                // Upload to Storage
                val storageRef = storage.reference.child("skills/$skillId/SKILL.md")
                val bytes = markdownContent.toByteArray(Charsets.UTF_8)
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Derive title from filename or first line
                var title = filename.substringBeforeLast(".")
                val firstLine = markdownContent.lines().firstOrNull { it.trim().startsWith("#") }
                if (firstLine != null) {
                    title = firstLine.replace("#", "").trim()
                }

                val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Anonymous"

                // Save to Firestore as a Draft by default
                val skillData = hashMapOf(
                    "id" to skillId,
                    "title" to title,
                    "description" to "Imported from file.",
                    "category" to "code", // Generic fallback
                    "fileUrl" to downloadUrl,
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "installCount" to 0,
                    "rating" to 0.0,
                    "isTrending" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(user.uid)
                    .collection("drafts")
                    .document(skillId)
                    .set(skillData)
                    .await()
                    
                // Refresh drafts
                loadDrafts()
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun publishDraft(draft: SkillDraft, onResult: ((Boolean) -> Unit)? = null) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get draft from drafts collection
                val draftRef = firestore.collection("users").document(user.uid).collection("drafts").document(draft.id)
                val snapshot = draftRef.get().await()
                
                if (snapshot.exists()) {
                    // 2. Add to skills collection
                    val data = snapshot.data ?: return@launch
                    firestore.collection("skills").document(draft.id).set(data).await()
                    
                    // 3. Delete from drafts
                    draftRef.delete().await()
                    
                    // 4. Refresh drafts
                    loadDrafts()
                    onResult?.invoke(true)
                } else {
                    onResult?.invoke(false)
                }
            } catch (e: Exception) {
                onResult?.invoke(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
