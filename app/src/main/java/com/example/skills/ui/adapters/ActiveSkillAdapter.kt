package com.example.skills.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.skills.R
import com.example.skills.data.model.InstalledSkill
import com.example.skills.databinding.ItemActiveSkillBinding

class ActiveSkillAdapter(
    private val skills: List<InstalledSkill>,
    private val onItemClick: (InstalledSkill) -> Unit
) : RecyclerView.Adapter<ActiveSkillAdapter.ActiveSkillViewHolder>() {

    inner class ActiveSkillViewHolder(private val binding: ItemActiveSkillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: InstalledSkill) {
            binding.tvActiveSkillTitle.text = skill.title
            
            // Load icon via Coil
            if (skill.iconUrl.isNotEmpty()) {
                binding.ivActiveSkillIcon.load(skill.iconUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_edit)
                }
            } else {
                binding.ivActiveSkillIcon.setImageResource(android.R.drawable.ic_menu_edit)
            }

            // Status Logic (PRD 3.5.2)
            val now = System.currentTimeMillis()
            val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
            val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000L
            
            val diff = now - skill.lastUsedAt

            when {
                skill.isDraft -> {
                    binding.tvActiveSkillUsage.text = "Custom · Draft"
                    binding.ivStatusDot.setBackgroundResource(R.drawable.shape_status_dot_draft)
                }
                skill.lastUsedAt == 0L -> {
                    binding.tvActiveSkillUsage.text = "Not used yet"
                    binding.ivStatusDot.setBackgroundResource(R.drawable.shape_status_dot_inactive)
                }
                diff < sevenDaysMs -> {
                    binding.tvActiveSkillUsage.text = "Active this week"
                    binding.ivStatusDot.setBackgroundResource(R.drawable.shape_status_dot)
                }
                diff < thirtyDaysMs -> {
                    binding.tvActiveSkillUsage.text = "Last used recently"
                    binding.ivStatusDot.setBackgroundResource(R.drawable.shape_status_dot_draft) // Amber/Draft for recent
                }
                else -> {
                    binding.tvActiveSkillUsage.text = "Inactive"
                    binding.ivStatusDot.setBackgroundResource(R.drawable.shape_status_dot_inactive)
                }
            }

            binding.root.setOnClickListener {
                onItemClick(skill)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveSkillViewHolder {
        val binding = ItemActiveSkillBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActiveSkillViewHolder(binding)
    }

    override fun getItemCount() = skills.size

    override fun onBindViewHolder(holder: ActiveSkillViewHolder, position: Int) {
        holder.bind(skills[position])
    }
}
