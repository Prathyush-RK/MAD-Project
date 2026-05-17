package com.example.skills.data.model

import com.google.firebase.firestore.DocumentId

data class SkillDraft(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val category: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val installCount: Int = 0,
    val rating: Double = 0.0,
    val isTrending: Boolean = false,
    val createdAt: Long = 0L
)
