package com.example.skills.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.skills.R
import com.example.skills.databinding.FragmentBuildBinding
import com.example.skills.ui.adapters.DraftAdapter
import com.example.skills.ui.adapters.TemplateAdapter
import com.example.skills.ui.viewmodels.BuildViewModel
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@AndroidEntryPoint
class BuildFragment : Fragment(R.layout.fragment_build) {

    private var _binding: FragmentBuildBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuildViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            readAndImportFile(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBuildBinding.bind(view)

        binding.btnNewSkill.setOnClickListener {
            findNavController().navigate(R.id.action_build_to_createSkill)
        }

        binding.btnImportSkill.setOnClickListener {
            // Open the document picker for any file type (specifically looking for markdown)
            filePickerLauncher.launch("*/*")
        }

        observeViewModel()
    }

    private fun readAndImportFile(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            
            // Get filename
            var filename = "imported_skill.md"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        filename = cursor.getString(nameIndex)
                    }
                }
            }

            // Read content
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
            inputStream?.close()

            val markdownContent = stringBuilder.toString()
            if (markdownContent.isNotBlank()) {
                Toast.makeText(requireContext(), "Importing $filename...", Toast.LENGTH_SHORT).show()
                viewModel.importMarkdownSkill(filename, markdownContent)
            } else {
                Toast.makeText(requireContext(), "File is empty", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.templates.collect { templates ->
                        binding.rvTemplates.adapter = TemplateAdapter(templates) { template ->
                            Toast.makeText(requireContext(), "Using: ${template.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                launch {
                    viewModel.drafts.collect { drafts ->
                        binding.tvMyDraftsHeader.visibility =
                            if (drafts.isEmpty()) View.GONE else View.VISIBLE
                        binding.rvDrafts.adapter = DraftAdapter(
                            drafts = drafts,
                            onEditClick = { draft ->
                                Toast.makeText(requireContext(), "Editing: ${draft.title}", Toast.LENGTH_SHORT).show()
                            },
                            onPublishClick = { draft ->
                                viewModel.publishDraft(draft)
                                Toast.makeText(requireContext(), "Published ${draft.title} to Marketplace!", Toast.LENGTH_SHORT).show()
                            }
                        )
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
