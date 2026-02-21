package com.example.sunseeker_app.ui.view

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentRegisterBinding
import com.example.sunseeker_app.ui.viewmodel.RegisterState
import com.example.sunseeker_app.ui.viewmodel.RegisterViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    private var selectedPhotoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            binding.imageAvatar.setPadding(0, 0, 0, 0)
            binding.imageAvatar.imageTintList = null
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.imageAvatar)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.containerAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonRegister.setOnClickListener {
            val displayName = binding.editDisplayName.text?.toString().orEmpty()
            val email = binding.editEmail.text?.toString().orEmpty()
            val password = binding.editPassword.text?.toString().orEmpty()

            if (displayName.isBlank()) {
                Snackbar.make(binding.root, "Please enter a display name", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isBlank()) {
                Snackbar.make(binding.root, "Please enter your email", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Snackbar.make(binding.root, "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, displayName, selectedPhotoUri)
        }

        binding.buttonLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterState.Loading -> setLoading(true)
                is RegisterState.Success -> {
                    setLoading(false)
                    findNavController().navigate(
                        RegisterFragmentDirections.actionRegisterFragmentToFeedFragment()
                    )
                }
                is RegisterState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
        binding.buttonLogin.isEnabled = !isLoading
        binding.containerAvatar.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
