package com.example.skills.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.skills.R
import com.example.skills.databinding.FragmentDiscoverBinding
import com.example.skills.ui.adapters.CategoryAdapter
import com.example.skills.ui.adapters.SkillAdapter
import com.example.skills.ui.viewmodels.DiscoverViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var skillAdapter: SkillAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDiscoverBinding.bind(view)

        setupRecyclerViews()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        skillAdapter = SkillAdapter(
            onInstallClick = { skill ->
                viewModel.installSkill(skill)
                Toast.makeText(requireContext(), "${skill.title} Installed", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { skill ->
                val bottomSheet = SkillDetailFragment.newInstance(skill)
                bottomSheet.show(childFragmentManager, SkillDetailFragment.TAG)
            }
        )
        binding.rvSkills.adapter = skillAdapter

        binding.rvSkills.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!viewModel.isLoading.value) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        viewModel.loadSkills()
                    }
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        if (categories.isNotEmpty()) {
                            binding.rvCategories.adapter = CategoryAdapter(categories) { category ->
                                viewModel.filterByCategory(category)
                            }
                        }
                    }
                }

                launch {
                    viewModel.skills.collect { skills ->
                        skillAdapter.submitList(skills)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.shimmerLayout.startShimmer()
                            binding.rvSkills.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            if (!viewModel.isEmpty.value) {
                                binding.rvSkills.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.isEmpty.collect { isEmpty ->
                        if (!viewModel.isLoading.value) {
                            binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                            binding.rvSkills.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
