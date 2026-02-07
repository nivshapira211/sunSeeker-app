package com.example.sunseeker_app.ui.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.DialogEditProfileBinding
import com.example.sunseeker_app.databinding.FragmentProfileBinding
import com.example.sunseeker_app.ui.viewmodel.ProfileState
import com.example.sunseeker_app.ui.viewmodel.ProfileUi
import com.example.sunseeker_app.ui.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var adapter: EventsAdapter

    private var dialogBinding: DialogEditProfileBinding? = null
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            dialogBinding?.dialogProfileImage?.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        adapter = EventsAdapter(
            onJoinClick = { event ->
                val action = ProfileFragmentDirections.actionProfileFragmentToEventDetailsFragment(event.id)
                findNavController().navigate(action)
            },
            onItemClick = { event ->
                val action = ProfileFragmentDirections.actionProfileFragmentToEventDetailsFragment(event.id)
                findNavController().navigate(action)
            },
            joinLabel = "View"
        )
        binding.recyclerMyEvents.adapter = adapter

        viewModel.myEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }

        viewModel.refreshProfile()
        viewModel.profileUi.observe(viewLifecycleOwner) { ui ->
            bindProfileInfo(ui)
        }

        binding.buttonEditProfile.setOnClickListener { showEditDialog() }
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            val action = ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.Loading -> binding.buttonEditProfile.isEnabled = false
                is ProfileState.Success -> {
                    binding.buttonEditProfile.isEnabled = true
                    Snackbar.make(binding.root, "Profile updated", Snackbar.LENGTH_SHORT).show()
                }
                is ProfileState.Error -> {
                    binding.buttonEditProfile.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindProfileInfo(ui: ProfileUi) {
        binding.textName.text = ui.name
        if (!ui.photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(ui.photoUrl)
                .into(binding.imageProfile)
        } else {
            binding.imageProfile.setImageResource(R.drawable.avatar_placeholder)
        }
    }

    private fun showEditDialog() {
        selectedImageUri = null
        dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()))
        val ui = viewModel.profileUi.value
        dialogBinding?.dialogNameEdit?.setText(ui?.name.orEmpty())
        val photoUrl = ui?.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this).load(photoUrl).into(dialogBinding!!.dialogProfileImage)
        }

        dialogBinding?.dialogChangePhoto?.setOnClickListener {
            pickImage.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogBinding!!.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding?.dialogNameEdit?.text?.toString().orEmpty()
                viewModel.updateProfile(name, selectedImageUri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialogBinding = null
    }
}
