package com.example.skills.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skills.data.model.SkillTemplate
import com.example.skills.databinding.ItemBuildTemplateBinding

class TemplateAdapter(
    private val templates: List<SkillTemplate>,
    private val onUseClick: (SkillTemplate) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(private val binding: ItemBuildTemplateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(template: SkillTemplate) {
            binding.tvTemplateName.text = template.name
            binding.tvTemplateDesc.text = template.description
            binding.ivTemplateIcon.setImageResource(template.iconResId)
            binding.btnUse.setOnClickListener { onUseClick(template) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemBuildTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TemplateViewHolder(binding)
    }

    override fun getItemCount() = templates.size

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templates[position])
    }
}
