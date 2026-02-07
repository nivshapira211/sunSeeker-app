package com.example.sunseeker_app.ui.view

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentCreateEventBinding
import com.example.sunseeker_app.ui.viewmodel.CreateEventState
import com.example.sunseeker_app.ui.viewmodel.CreateEventViewModel
import com.example.sunseeker_app.ui.viewmodel.SolarState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateEventFragment : Fragment(R.layout.fragment_create_event) {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateEventViewModel by viewModels()
    private val args: CreateEventFragmentArgs by navArgs()

    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imagePreview.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventBinding.bind(view)

        val eventId = args.eventId
        if (eventId != null) {
            binding.textTitle.text = "Edit Event"
            binding.buttonSubmit.text = "Save"
            viewModel.loadEvent(eventId).observe(viewLifecycleOwner) { event ->
                if (event == null) return@observe
                binding.editTitle.setText(event.title)
                binding.editLocation.setText(event.location)
                binding.editDescription.setText(event.description)
                existingImageUrl = event.imageUrl
                if (existingImageUrl.isNotBlank() && selectedImageUri == null) {
                    Glide.with(this)
                        .load(existingImageUrl)
                        .into(binding.imagePreview)
                }
            }
        }

        binding.buttonPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonFetchSunTimes.setOnClickListener {
            val lat = binding.editLatitude.text?.toString()?.toDoubleOrNull()
            val lng = binding.editLongitude.text?.toString()?.toDoubleOrNull()
            if (lat == null || lng == null) {
                Snackbar.make(binding.root, "Enter valid coordinates", Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.fetchSunTimes(lat, lng)
            }
        }

        binding.buttonSubmit.setOnClickListener {
            val title = binding.editTitle.text?.toString().orEmpty()
            val location = binding.editLocation.text?.toString().orEmpty()
            val description = binding.editDescription.text?.toString().orEmpty()

            if (eventId == null) {
                viewModel.createEvent(title, location, description, selectedImageUri)
            } else {
                viewModel.updateEvent(
                    eventId,
                    title,
                    location,
                    description,
                    selectedImageUri,
                    existingImageUrl
                )
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateEventState.Loading -> setLoading(true)
                is CreateEventState.Success -> {
                    setLoading(false)
                    Snackbar.make(binding.root, "Saved", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is CreateEventState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.solarState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SolarState.Loading -> {
                    binding.textSunTimes.text = "Loading sun times..."
                }
                is SolarState.Success -> {
                    val data = state.data
                    binding.textSunTimes.text = "Sunrise: ${data.sunrise}\nSunset: ${data.sunset}"
                }
                is SolarState.Error -> {
                    binding.textSunTimes.text = state.message
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressSubmit.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !isLoading
        binding.buttonPickImage.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
