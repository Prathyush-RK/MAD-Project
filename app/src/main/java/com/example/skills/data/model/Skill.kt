package com.example.skills.data.model

import com.google.firebase.firestore.DocumentId

data class Skill(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val category: String = "",
    val rating: Double = 0.0,
    val installCount: Int = 0,
    val isTrending: Boolean = false,
    val fileUrl: String = "", // The URL to the markdown file in Firebase Storage
    val promptTemplate: String = "", // In-line markdown content (optional)
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = 0L
)
