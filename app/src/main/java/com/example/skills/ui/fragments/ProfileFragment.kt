package com.example.skills.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.skills.R
import com.example.skills.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        displayUserInfo()

        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
            // Re-sign in anonymously
            auth.signInAnonymously().addOnSuccessListener {
                displayUserInfo()
                Toast.makeText(requireContext(), "New anonymous session started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val isAnonymous = user.isAnonymous

            binding.tvProfileAvatar.text = uid.take(2).uppercase()
            binding.tvProfileName.text = if (isAnonymous) "Anonymous User" else (user.displayName ?: "User")
            binding.tvProfileAuthType.text = if (isAnonymous) "Anonymous Session" else "Signed In"
            binding.tvProfileUid.text = uid
            binding.tvProfileProvider.text = if (isAnonymous) "Anonymous" else {
                user.providerData.joinToString(", ") { it.providerId }
            }
        } else {
            binding.tvProfileAvatar.text = "?"
            binding.tvProfileName.text = "Not signed in"
            binding.tvProfileAuthType.text = "—"
            binding.tvProfileUid.text = "—"
            binding.tvProfileProvider.text = "—"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
