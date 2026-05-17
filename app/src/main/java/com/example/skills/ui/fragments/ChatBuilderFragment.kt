package com.example.skills.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skills.R
import com.example.skills.databinding.FragmentChatBuilderBinding
import com.example.skills.ui.adapters.ChatAdapter
import com.example.skills.ui.viewmodels.ChatBuilderViewModel
import com.example.skills.ui.viewmodels.ChatPhase
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatBuilderFragment : Fragment(R.layout.fragment_chat_builder) {

    private var _binding: FragmentChatBuilderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatBuilderViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatBuilderBinding.bind(view)

        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text.clear()
            }
        }

        binding.btnPublish.setOnClickListener {
            viewModel.saveSkill(publishToMarketplace = true)
        }

        binding.btnDraft.setOnClickListener {
            viewModel.saveSkill(publishToMarketplace = false)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Phase transitions: toggle which section is visible
                launch {
                    viewModel.phase.collect { phase ->
                        when (phase) {
                            ChatPhase.CHATTING -> {
                                binding.tvTitle.text = "Chat with AI"
                                binding.rvChat.visibility = View.VISIBLE
                                binding.layoutInput.visibility = View.VISIBLE
                                binding.layoutGenerating.visibility = View.GONE
                                binding.layoutPreview.visibility = View.GONE
                            }
                            ChatPhase.GENERATING -> {
                                binding.tvTitle.text = "Generating…"
                                binding.rvChat.visibility = View.GONE
                                binding.layoutInput.visibility = View.GONE
                                binding.layoutGenerating.visibility = View.VISIBLE
                                binding.layoutPreview.visibility = View.GONE
                            }
                            ChatPhase.PREVIEW -> {
                                binding.tvTitle.text = "Preview"
                                binding.rvChat.visibility = View.GONE
                                binding.layoutInput.visibility = View.GONE
                                binding.layoutGenerating.visibility = View.GONE
                                binding.layoutPreview.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // Chat messages
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            binding.rvChat.smoothScrollToPosition(messages.size - 1)
                        }
                    }
                }

                // Typing indicator
                launch {
                    viewModel.isTyping.collect { isTyping ->
                        binding.btnSend.isEnabled = !isTyping
                    }
                }

                // Render preview markdown
                launch {
                    viewModel.generatedMarkdown.collect { markdown ->
                        if (markdown != null) {
                            val markwon = Markwon.create(requireContext())
                            markwon.setMarkdown(binding.tvPreviewMarkdown, markdown)
                        }
                    }
                }

                // Render validation results
                launch {
                    viewModel.validationScore.collect { score ->
                        if (score >= 0) {
                            binding.cardValidation.visibility = View.VISIBLE
                            binding.tvScoreBadge.text = "Score: $score"

                            // Color the badge based on score
                            val color = when {
                                score >= 70 -> android.graphics.Color.parseColor("#4CAF50") // Green
                                score >= 40 -> android.graphics.Color.parseColor("#FF9800") // Orange
                                else -> android.graphics.Color.parseColor("#F44336")         // Red
                            }
                            binding.tvScoreBadge.background.setTint(color)

                            // Update Publish button label based on score
                            if (score < 70) {
                                binding.btnPublish.text = "Publish (score < 70)"
                                binding.btnPublish.isEnabled = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.validationFeedback.collect { feedback ->
                        if (feedback != null) {
                            val markwon = Markwon.create(requireContext())
                            markwon.setMarkdown(binding.tvValidationFeedback, feedback)
                        }
                    }
                }

                // Save result
                launch {
                    viewModel.saveResult.collect { result ->
                        if (result != null) {
                            val (success, published) = result
                            if (success) {
                                val msg = if (published) "Skill published to Marketplace!" else "Skill saved to Drafts!"
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                                findNavController().navigateUp()
                            } else {
                                Toast.makeText(requireContext(), "Failed to save skill.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                // Saving state — disable buttons while uploading
                launch {
                    viewModel.isSaving.collect { saving ->
                        binding.btnDraft.isEnabled = !saving
                        binding.btnDraft.text = if (saving) "Saving…" else "Save Draft"

                        // Only update Publish if score allows it
                        val score = viewModel.validationScore.value
                        if (score >= 70 || score < 0) {
                            binding.btnPublish.isEnabled = !saving
                            binding.btnPublish.text = if (saving) "Saving…" else "Publish"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

