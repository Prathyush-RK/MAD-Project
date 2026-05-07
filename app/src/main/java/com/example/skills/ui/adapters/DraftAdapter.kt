package com.example.skills.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skills.data.model.SkillDraft
import com.example.skills.databinding.ItemDraftBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DraftAdapter(
    private val drafts: List<SkillDraft>,
    private val onEditClick: (SkillDraft) -> Unit,
    private val onPublishClick: (SkillDraft) -> Unit
) : RecyclerView.Adapter<DraftAdapter.DraftViewHolder>() {

    inner class DraftViewHolder(private val binding: ItemDraftBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(draft: SkillDraft) {
            binding.tvDraftTitle.text = draft.title.ifEmpty { "Untitled Draft" }
            binding.tvDraftStatus.text = "Draft · ${draft.category.ifEmpty { "General" }}"
            binding.btnEdit.setOnClickListener { onEditClick(draft) }
            binding.btnPublish.setOnClickListener { onPublishClick(draft) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val binding = ItemDraftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DraftViewHolder(binding)
    }

    override fun getItemCount() = drafts.size

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(drafts[position])
    }
}
