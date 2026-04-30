package com.example.skills.data.model

data class SkillTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int = android.R.drawable.ic_menu_report_image,
    val promptTemplate: String = ""
)
