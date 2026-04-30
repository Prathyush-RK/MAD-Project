package com.example.skills.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.skills.data.model.InstalledSkill

@Database(entities = [InstalledSkill::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
}
