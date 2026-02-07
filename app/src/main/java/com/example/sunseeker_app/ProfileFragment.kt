package com.example.sunseeker_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sunseeker_app.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // For now, we use a hardcoded ID until Firebase Auth is fully integrated.
        val currentUserId = "default_user" 

        userViewModel.getUserById(currentUserId).observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.userNameText.text = user.name
                binding.userEmailText.text = user.email
                // TODO: Load profile image using Glide or COIL
            } else {
                // If user doesn't exist locally, we could trigger a fetch or show a placeholder
                binding.userNameText.text = "New SunSeeker"
                binding.userEmailText.text = "Tap edit to set up profile"
            }
        }

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}