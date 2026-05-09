package com.example.skills.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.skills.R
import com.example.skills.databinding.FragmentMySkillsBinding
import com.example.skills.ui.adapters.ActiveSkillAdapter
import com.example.skills.ui.viewmodels.MySkillsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MySkillsFragment : Fragment(R.layout.fragment_my_skills) {

    private var _binding: FragmentMySkillsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MySkillsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMySkillsBinding.bind(view)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.userStats.collect { stats ->
                        binding.tvAvatar.text = stats.initials
                        binding.tvUserName.text = stats.name
                        binding.tvBuilderLevel.text = "Builder · Level ${stats.level}"
                        binding.tvInstalledCount.text = stats.installed.toString()
                        binding.tvBuiltCount.text = stats.built.toString()
                        binding.tvSharedUses.text = if (stats.sharedUses >= 1000) {
                            "${stats.sharedUses / 1000.0}k"
                        } else {
                            stats.sharedUses.toString()
                        }
                    }
                }

                launch {
                    viewModel.activeSkills.collect { skills ->
                        binding.rvActiveSkills.adapter = ActiveSkillAdapter(skills) { installedSkill ->
                            // Record usage
                            viewModel.useSkill(installedSkill.id)

                            // Open detail (map InstalledSkill -> Skill)
                            val skill = com.example.skills.data.model.Skill(
                                id = installedSkill.id,
                                title = installedSkill.title,
                                description = installedSkill.description,
                                iconUrl = installedSkill.iconUrl,
                                category = installedSkill.category,
                                promptTemplate = installedSkill.mdContent
                            )
                            val detailFragment = SkillDetailFragment.newInstance(skill)
                            detailFragment.show(childFragmentManager, SkillDetailFragment.TAG)
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
