package com.example.skills.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skills.data.model.InstalledSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM installed_skills ORDER BY installedAt DESC")
    fun getAllInstalledSkills(): Flow<List<InstalledSkill>>

    @Query("SELECT * FROM installed_skills WHERE id = :skillId LIMIT 1")
    suspend fun getInstalledSkillById(skillId: String): InstalledSkill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: InstalledSkill)

    @Query("DELETE FROM installed_skills WHERE id = :skillId")
    suspend fun deleteSkill(skillId: String)
}
