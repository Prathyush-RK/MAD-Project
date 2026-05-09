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
                
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            binding.rvChat.smoothScrollToPosition(messages.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.isTyping.collect { isTyping ->
                        binding.btnSend.isEnabled = !isTyping
                    }
                }

                launch {
                    viewModel.isGeneratingSkill.collect { isGenerating ->
                        if (isGenerating) {
                            binding.rvChat.visibility = View.GONE
                            binding.layoutInput.visibility = View.GONE
                            binding.layoutGenerating.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.generatedSkillMarkdown.collect { markdown ->
                        if (markdown != null) {
                            binding.layoutGenerating.visibility = View.GONE
                            binding.layoutResult.visibility = View.VISIBLE
                            Markwon.create(requireContext()).setMarkdown(binding.tvGeneratedMarkdown, markdown)
                        }
                    }
                }

                launch {
                    viewModel.saveResult.collect { result ->
                        if (result != null) {
                            val (success, published) = result
                            if (success) {
                                val msg = if (published) "Skill published to Marketplace!" else "Skill saved to Drafts!"
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            } else {
                                Toast.makeText(requireContext(), "Failed to save skill.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.isSaving.collect { saving ->
                        binding.btnPublish.isEnabled = !saving
                        binding.btnDraft.isEnabled = !saving
                        binding.btnPublish.text = if (saving) "Saving..." else "Publish"
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
