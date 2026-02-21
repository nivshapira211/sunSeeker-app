package com.example.sunseeker_app.ui.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentLoginBinding
import com.example.sunseeker_app.ui.viewmodel.LoginState
import com.example.sunseeker_app.ui.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        if (viewModel.isUserLoggedIn()) {
            findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToFeedFragment()
            )
            return
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text?.toString().orEmpty()
            val password = binding.editPassword.text?.toString().orEmpty()

            if (email.isBlank()) {
                Snackbar.make(binding.root, "Please enter your email", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isBlank()) {
                Snackbar.make(binding.root, "Please enter your password", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.buttonRegister.setOnClickListener {
            findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            )
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Loading -> setLoading(true)
                is LoginState.Success -> {
                    setLoading(false)
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToFeedFragment()
                    )
                }
                is LoginState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonRegister.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
