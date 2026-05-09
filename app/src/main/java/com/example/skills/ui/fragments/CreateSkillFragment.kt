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
import com.example.skills.R
import com.example.skills.databinding.FragmentCreateSkillBinding
import com.example.skills.ui.viewmodels.CreateSkillViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import io.noties.markwon.Markwon

@AndroidEntryPoint
class CreateSkillFragment : Fragment(R.layout.fragment_create_skill) {

    private var _binding: FragmentCreateSkillBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateSkillViewModel by viewModels()

    private val stepDots by lazy {
        listOf(
            binding.step1Dot,
            binding.step2Dot,
            binding.step3Dot,
            binding.step4Dot
        )
    }

    private val stepLayouts by lazy {
        listOf(
            binding.layoutStep1,
            binding.layoutStep2,
            binding.layoutStep3,
            binding.layoutStep4
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateSkillBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnNext.setOnClickListener {
            saveCurrentStepData()
            if (viewModel.currentStep.value == 4) {
                viewModel.publishSkill()
            } else {
                viewModel.nextStep()
            }
        }

        binding.btnDraft.setOnClickListener {
            saveCurrentStepData()
            viewModel.saveDraft()
        }

        binding.btnPrevious.setOnClickListener {
            saveCurrentStepData()
            viewModel.previousStep()
        }

        binding.btnValidateAI.setOnClickListener {
            saveCurrentStepData()
            viewModel.validateSkillWithAI()
            binding.btnValidateAI.isEnabled = false
            binding.btnValidateAI.text = "Validating..."
        }

        observeViewModel()
    }

    private fun saveCurrentStepData() {
        when (viewModel.currentStep.value) {
            1 -> {
                viewModel.skillName = binding.etSkillName.text.toString()
                viewModel.persona = binding.etPersona.text.toString()
            }
            2 -> {
                viewModel.context = binding.etContext.text.toString()
            }
            3 -> {
                viewModel.instructions = binding.etInstructions.text.toString()
            }
            4 -> {
                viewModel.examplePrompt = binding.etExamplePrompt.text.toString()
                viewModel.exampleResponse = binding.etExampleResponse.text.toString()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.currentStep.collect { step ->
                        updateStepUI(step)
                    }
                }

                launch {
                    viewModel.publishResult.collect { result ->
                        if (result != null) {
                            val (score, wasPublished) = result
                            val title = if (wasPublished) "Skill Published!" else "Saved to Drafts"
                            val message = if (wasPublished) {
                                "Your skill has been published to the Marketplace."
                            } else {
                                "Your skill has been saved to your drafts."
                            }

                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle(title)
                                .setMessage(message)
                                .setPositiveButton("OK") { _, _ ->
                                    findNavController().navigateUp()
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }

                launch {
                    viewModel.isSaving.collect { saving ->
                        binding.btnNext.isEnabled = !saving
                        binding.btnDraft.isEnabled = !saving
                        
                        if (saving) {
                            binding.btnNext.text = "Publishing..."
                        } else {
                            binding.btnNext.text = if (viewModel.currentStep.value == 4) "Publish Skill" else "Next"
                        }
                        
                        // Keep validate button synced with saving state if we aren't validating
                        if (!saving && binding.btnValidateAI.text == "Validating...") {
                            binding.btnValidateAI.isEnabled = true
                            binding.btnValidateAI.text = "Validate with AI"
                        }
                    }
                }

                launch {
                    viewModel.validationFeedback.collect { feedback ->
                        if (feedback != null) {
                            binding.tvValidationFeedback.visibility = View.VISIBLE
                            Markwon.create(requireContext()).setMarkdown(binding.tvValidationFeedback, feedback)
                            binding.btnValidateAI.isEnabled = true
                            binding.btnValidateAI.text = "Validate with AI"
                        } else {
                            binding.tvValidationFeedback.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun updateStepUI(step: Int) {
        // Update step dots
        stepDots.forEachIndexed { index, dot ->
            dot.setBackgroundColor(
                if (index < step) resources.getColor(R.color.brand_primary, null)
                else resources.getColor(R.color.surface_variant, null)
            )
        }

        // Show/hide step layouts
        stepLayouts.forEachIndexed { index, layout ->
            layout.visibility = if (index == step - 1) View.VISIBLE else View.GONE
        }

        // Update buttons
        binding.btnPrevious.visibility = if (step > 1) View.VISIBLE else View.INVISIBLE
        if (step == 4) {
            binding.btnNext.text = "Publish Skill"
            binding.btnDraft.visibility = View.VISIBLE
        } else {
            binding.btnNext.text = "Next"
            binding.btnDraft.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
