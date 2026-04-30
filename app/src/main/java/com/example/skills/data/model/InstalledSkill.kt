package com.example.skills.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_skills")
data class InstalledSkill(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val category: String = "",
    val mdContent: String = "",
    val isDraft: Boolean = false,
    val installedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L,
    val usageCount: Int = 0
)
