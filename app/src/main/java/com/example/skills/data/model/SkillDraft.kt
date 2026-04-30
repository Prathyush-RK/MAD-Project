package com.example.skills.data.model

import com.google.firebase.firestore.DocumentId

data class SkillDraft(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val category: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = 0L
)
