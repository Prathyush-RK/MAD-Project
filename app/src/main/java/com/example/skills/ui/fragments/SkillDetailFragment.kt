package com.example.skills.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.skills.R
import com.example.skills.data.model.Skill
import com.example.skills.data.repository.SkillRepository
import com.example.skills.databinding.FragmentSkillDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

@AndroidEntryPoint
class SkillDetailFragment(private val skill: Skill) : BottomSheetDialogFragment() {

    private var _binding: FragmentSkillDetailBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var repository: SkillRepository

    companion object {
        const val TAG = "SkillDetailFragment"
        fun newInstance(skill: Skill) = SkillDetailFragment(skill)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadMarkdown()
    }

    private fun setupUI() {
        binding.tvSkillTitle.text = skill.title.replace(Regex("^#+\\s*"), "")
        binding.chipCategory.text = skill.category
        binding.tvSkillDescription.text = skill.description

        val df = DecimalFormat("#.1")
        val rating = df.format(skill.rating)
        val installs = if (skill.installCount >= 1000) {
            "${df.format(skill.installCount / 1000.0)}k"
        } else {
            skill.installCount.toString()
        }
        binding.tvSkillStats.text = "By ${skill.authorName}  ·  ★★★★★ $rating  ·  $installs installs"

        if (skill.iconUrl.isNotEmpty()) {
            binding.ivSkillIcon.load(skill.iconUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_edit)
            }
        }


        binding.btnDownload.setOnClickListener {
            if (skill.fileUrl.isNotEmpty()) {
                try {
                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(skill.fileUrl))
                        .setTitle("Downloading ${skill.title}")
                        .setDescription("SkillForge Skill")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "${skill.id}.md")
                    
                    val downloadManager = requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    downloadManager.enqueue(request)
                    Toast.makeText(requireContext(), "Download started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No file available to download", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            if (skill.fileUrl.isNotEmpty()) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this AI Skill!")
                    putExtra(android.content.Intent.EXTRA_TEXT, "I found this awesome skill on SkillForge: ${skill.title}\n\nDownload it here: ${skill.fileUrl}")
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Skill via"))
            } else {
                Toast.makeText(requireContext(), "No link available to share", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMarkdown() {
        binding.shimmerContent.startShimmer()
        binding.shimmerContent.visibility = View.VISIBLE
        binding.tvMarkdownContent.visibility = View.GONE

        lifecycleScope.launch {
            val markdown = repository.getSkillMarkdown(skill)
            
            binding.shimmerContent.stopShimmer()
            binding.shimmerContent.visibility = View.GONE
            binding.tvMarkdownContent.visibility = View.VISIBLE

            val markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(TaskListPlugin.create(requireContext()))
                .build()

            markwon.setMarkdown(binding.tvMarkdownContent, markdown)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
