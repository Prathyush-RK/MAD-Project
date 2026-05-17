package com.example.skills.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.skills.R
import com.example.skills.data.model.Skill
import com.example.skills.databinding.ItemSkillCardBinding
import java.text.DecimalFormat

class SkillAdapter(
    private val onItemClick: (Skill) -> Unit
) : ListAdapter<Skill, SkillAdapter.SkillViewHolder>(SkillDiffCallback()) {

    inner class SkillViewHolder(private val binding: ItemSkillCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: Skill) {
            binding.tvTitle.text = skill.title.replace(Regex("^#+\\s*"), "")
            binding.tvDescription.text = skill.description

            val df = DecimalFormat("#.1")
            val rating = df.format(skill.rating)
            
            // Format install count (e.g., 1200 -> 1.2k)
            val installs = if (skill.installCount >= 1000) {
                "${df.format(skill.installCount / 1000.0)}k"
            } else {
                skill.installCount.toString()
            }

            binding.tvStats.text = "★★★★★  $rating · $installs installs"

            // Load icon
            if (skill.iconUrl.isNotEmpty()) {
                binding.ivIcon.load(skill.iconUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_edit)
                }
            } else {
                binding.ivIcon.setImageResource(android.R.drawable.ic_menu_edit)
            }

            binding.root.setOnClickListener {
                onItemClick(skill)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SkillDiffCallback : DiffUtil.ItemCallback<Skill>() {
        override fun areItemsTheSame(oldItem: Skill, newItem: Skill): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Skill, newItem: Skill): Boolean {
            return oldItem == newItem
        }
    }
}
