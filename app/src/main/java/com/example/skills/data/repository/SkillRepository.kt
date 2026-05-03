package com.example.skills.data.repository

import android.util.Log
import com.example.skills.data.local.SkillDao
import com.example.skills.data.model.InstalledSkill
import com.example.skills.data.model.Skill
import com.example.skills.data.model.SkillDraft
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val skillDao: SkillDao,
    private val storage: FirebaseStorage
) {
    private val SKILLS_COLLECTION = "skills"
    private val DRAFTS_COLLECTION = "drafts"
    private val INSTALLED_SKILLS = "installed_skills"

    // Fetch all marketplace skills
    suspend fun getTrendingSkills(): List<Skill> {
        return try {
            val snapshot = firestore.collection(SKILLS_COLLECTION)
                .whereEqualTo("isTrending", true)
                .get()
                .await()
            snapshot.toObjects(Skill::class.java)
        } catch (e: Exception) {
            Log.e("SkillRepository", "Error fetching trending skills", e)
            emptyList()
        }
    }

    suspend fun getAllSkills(limit: Long = 20, lastVisible: DocumentSnapshot? = null): Pair<List<Skill>, DocumentSnapshot?> {
        return try {
            var query = firestore.collection(SKILLS_COLLECTION)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val skills = snapshot.toObjects(Skill::class.java)
            val lastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            Log.d("SkillRepository", "Successfully fetched ${skills.size} skills from Firestore")
            if (skills.isEmpty()) {
                Log.w("SkillRepository", "Firestore returned 0 documents for the 'skills' collection.")
            }

            Pair(skills, lastDoc)
        } catch (e: Exception) {
            Log.e("SkillRepository", "Error fetching all skills: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    suspend fun getSkillsByCategory(category: String, limit: Long = 20, lastVisible: DocumentSnapshot? = null): Pair<List<Skill>, DocumentSnapshot?> {
        return try {
            var query = firestore.collection(SKILLS_COLLECTION)
                .whereEqualTo("category", category)
                .orderBy("title", Query.Direction.ASCENDING)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val skills = snapshot.toObjects(Skill::class.java)
            val lastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            Pair(skills, lastDoc)
        } catch (e: Exception) {
            Log.e("SkillRepository", "Error fetching skills by category", e)
            Pair(emptyList(), null)
        }
    }

    // Install a skill (save to local Room DB and update Firestore tracker)
    suspend fun installSkill(skill: Skill): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val mdContent = getSkillMarkdown(skill)
            
            // Save to Room DB
            val installedSkill = InstalledSkill(
                id = skill.id,
                title = skill.title,
                description = skill.description,
                iconUrl = skill.iconUrl,
                category = skill.category,
                mdContent = mdContent,
                isDraft = false,
                installedAt = System.currentTimeMillis()
            )
            skillDao.insertSkill(installedSkill)
            
            // Sync with Firestore
            val installRecord = hashMapOf(
                "skillId" to skill.id,
                "installedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid)
                .collection(INSTALLED_SKILLS).document(skill.id)
                .set(installRecord)
                .await()
            true
        } catch (e: Exception) {
            Log.e("SkillRepository", "Failed to install skill", e)
            false
        }
    }

    suspend fun getSkillMarkdown(skill: Skill): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (skill.promptTemplate.isNotBlank()) {
            return@withContext skill.promptTemplate
        }
        if (skill.fileUrl.isNotBlank()) {
            return@withContext try {
                // Try Firebase Storage first
                val ref = storage.getReferenceFromUrl(skill.fileUrl)
                val maxDownloadSizeBytes: Long = 1024 * 1024 * 5 // 5MB
                val bytes = ref.getBytes(maxDownloadSizeBytes).await()
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                // Fallback: Try downloading as a regular public URL
                try {
                    Log.d("SkillRepository", "Firebase ref failed, trying direct download: ${skill.fileUrl}")
                    java.net.URL(skill.fileUrl).readText()
                } catch (e2: Exception) {
                    Log.e("SkillRepository", "Both download methods failed", e2)
                    "Error loading skill content."
                }
            }
        }
        return@withContext "No content available for this skill."
    }

    // Get current user drafts
    suspend fun getMyDrafts(): List<SkillDraft> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(user.uid)
                .collection(DRAFTS_COLLECTION)
                .get()
                .await()
            snapshot.toObjects(SkillDraft::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Local Persistence Methods
    fun getInstalledSkills() = skillDao.getAllInstalledSkills()

    suspend fun recordSkillUsage(skillId: String) {
        val skill = skillDao.getInstalledSkillById(skillId)
        if (skill != null) {
            val updatedSkill = skill.copy(
                lastUsedAt = System.currentTimeMillis(),
                usageCount = skill.usageCount + 1
            )
            skillDao.insertSkill(updatedSkill)
        }
    }
}
