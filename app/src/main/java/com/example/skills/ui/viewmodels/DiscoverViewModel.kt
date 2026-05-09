package com.example.skills.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skills.data.model.Category
import com.example.skills.data.model.Skill
import com.example.skills.data.repository.SkillRepository
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: SkillRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private var selectedCategory: String = "all"
    private var searchQuery: String = ""
    
    // Pagination state
    private var lastVisible: DocumentSnapshot? = null
    private var isLastPage = false
    private var loadedSkills: MutableList<Skill> = mutableListOf()
    
    private var searchJob: Job? = null

    init {
        loadCategories()
        loadSkills(reset = true)
    }

    private fun loadCategories() {
        _categories.value = listOf(
            Category("all", "All"),
            Category("writing", "Writing"),
            Category("code", "Code"),
            Category("research", "Research"),
            Category("finance", "Finance"),
            Category("design", "Design"),
            Category("devops", "DevOps"),
            Category("legal", "Legal"),
            Category("healthcare", "Healthcare")
        )
    }

    fun loadSkills(reset: Boolean = false) {
        if (reset) {
            lastVisible = null
            isLastPage = false
            loadedSkills.clear()
            _skills.value = emptyList()
            _isLoading.value = true
        }

        if (isLastPage || (_isLoading.value && !reset)) return

        viewModelScope.launch {
            if (!reset) _isLoading.value = true

            val result = if (selectedCategory == "all") {
                repository.getAllSkills(limit = 20, lastVisible = lastVisible)
            } else {
                repository.getSkillsByCategory(category = selectedCategory, limit = 20, lastVisible = lastVisible)
            }

            val fetchedSkills = result.first
            val newLastVisible = result.second

            if (fetchedSkills.isEmpty()) {
                isLastPage = true
            } else {
                loadedSkills.addAll(fetchedSkills)
                lastVisible = newLastVisible
                applySearchFilter()
            }
            
            _isLoading.value = false
        }
    }

    fun filterByCategory(category: Category) {
        if (selectedCategory != category.id) {
            selectedCategory = category.id
            loadSkills(reset = true)
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            searchQuery = query
            applySearchFilter()
        }
    }

    private fun applySearchFilter() {
        val filtered = if (searchQuery.isNotBlank()) {
            loadedSkills.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        } else {
            loadedSkills
        }

        _skills.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun installSkill(skill: Skill) {
        viewModelScope.launch {
            repository.installSkill(skill)
        }
    }
}
