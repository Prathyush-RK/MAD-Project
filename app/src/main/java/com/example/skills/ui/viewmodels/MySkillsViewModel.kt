package com.example.skills.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skills.data.model.InstalledSkill
import com.example.skills.data.repository.SkillRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserStats(
    val name: String = "",
    val initials: String = "",
    val level: Int = 1,
    val installed: Int = 0,
    val built: Int = 0,
    val sharedUses: Int = 0
)

@HiltViewModel
class MySkillsViewModel @Inject constructor(
    private val repository: SkillRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _activeSkills = MutableStateFlow<List<InstalledSkill>>(emptyList())
    val activeSkills: StateFlow<List<InstalledSkill>> = _activeSkills.asStateFlow()

    init {
        loadUserStats()
        observeInstalledSkills()
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch

            // Derive name + initials from Firebase Auth profile
            val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User"
            val initials = displayName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
                .ifEmpty { "U" }

            try {
                // Count installed skills from Firestore
                val installedSnapshot = firestore.collection("users").document(user.uid)
                    .collection("installed_skills")
                    .get().await()
                val installedCount = installedSnapshot.size()

                // Count built/published skills from Firestore
                val builtSnapshot = firestore.collection("skills")
                    .whereEqualTo("authorId", user.uid)
                    .get().await()
                val builtCount = builtSnapshot.size()

                // Count drafts as well
                val draftsSnapshot = firestore.collection("users").document(user.uid)
                    .collection("drafts")
                    .get().await()

                // Calculate level based on activity
                val totalActivity = installedCount + builtCount + draftsSnapshot.size()
                val level = when {
                    totalActivity >= 20 -> 5
                    totalActivity >= 12 -> 4
                    totalActivity >= 6 -> 3
                    totalActivity >= 2 -> 2
                    else -> 1
                }

                // Calculate shared uses (total installs on user's published skills)
                val sharedUses = builtSnapshot.documents.sumOf { doc ->
                    (doc.getLong("installCount") ?: 0).toInt()
                }

                _userStats.value = UserStats(
                    name = displayName,
                    initials = initials,
                    level = level,
                    installed = installedCount,
                    built = builtCount,
                    sharedUses = sharedUses
                )
            } catch (e: Exception) {
                // Fallback with just name info if Firestore fails
                _userStats.value = UserStats(
                    name = displayName,
                    initials = initials
                )
            }
        }
    }

    private fun observeInstalledSkills() {
        viewModelScope.launch {
            repository.getInstalledSkills().collect { skills ->
                _activeSkills.value = skills
            }
        }
    }

    fun useSkill(skillId: String) {
        viewModelScope.launch {
            repository.recordSkillUsage(skillId)
        }
    }
}
