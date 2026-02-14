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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

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
            binding.textAddPhotoHint.visibility = View.GONE
        }
    }

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var selectedLocationName: String? = null

    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(intent)
                val latLng = place.latLng
                if (latLng != null) {
                    currentLat = latLng.latitude
                    currentLng = latLng.longitude
                    
                    val address = place.address ?: place.name ?: "Unknown Location"
                    selectedLocationName = place.name ?: place.address
                    
                    binding.textSelectedLocation.text = address
                    binding.textSelectedLocation.setTextColor(resources.getColor(android.R.color.black, null))
                    
                    viewModel.fetchSunTimes(latLng.latitude, latLng.longitude)
                }
            }
        } else if (result.resultCode == com.google.android.libraries.places.widget.AutocompleteActivity.RESULT_ERROR) {
            val intent = result.data
            if (intent != null) {
                val status = com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(intent)
                Snackbar.make(binding.root, "Error: ${status.statusMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventBinding.bind(view)

        // Initialize Places
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
             // Ensure you have google_maps_key in strings.xml
             try {
                com.google.android.libraries.places.api.Places.initialize(requireContext(), getString(R.string.google_maps_key))
             } catch (e: Exception) {
                Snackbar.make(binding.root, "Places initialization failed. Check API Key.", Snackbar.LENGTH_LONG).show()
             }
        }

        val eventId = args.eventId
        if (eventId != null) {
            binding.textTitle.text = "Edit Event"
            binding.buttonSubmit.text = "Save Changes"
            viewModel.loadEvent(eventId).observe(viewLifecycleOwner) { event ->
                if (event == null) return@observe
                binding.editTitle.setText(event.title)
                binding.editDescription.setText(event.description)
                
                selectedLocationName = event.location
                binding.textSelectedLocation.text = event.location
                
                existingImageUrl = event.imageUrl
                if (existingImageUrl.isNotBlank() && selectedImageUri == null) {
                    Glide.with(this)
                        .load(existingImageUrl)
                        .into(binding.imagePreview)
                    binding.textAddPhotoHint.visibility = View.GONE
                }
            }
        }

        binding.imagePreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonPickLocation.setOnClickListener {
            val fields = listOf(
                com.google.android.libraries.places.api.model.Place.Field.ID,
                com.google.android.libraries.places.api.model.Place.Field.NAME,
                com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
                com.google.android.libraries.places.api.model.Place.Field.LAT_LNG
            )
            val intent = com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
                com.google.android.libraries.places.widget.model.AutocompleteActivityMode.FULLSCREEN, fields
            ).build(requireContext())
            startAutocomplete.launch(intent)
        }

        binding.buttonFetchSunTimes.setOnClickListener {
            val lat = currentLat
            val lng = currentLng
            if (lat == null || lng == null) {
                Snackbar.make(binding.root, "Please pick a location first", Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.fetchSunTimes(lat, lng)
            }
        }

        binding.buttonSubmit.setOnClickListener {
            val title = binding.editTitle.text?.toString().orEmpty()
            val location = selectedLocationName.orEmpty()
            val description = binding.editDescription.text?.toString().orEmpty()
            
            if (title.isBlank()) {
                Snackbar.make(binding.root, "Please enter a title", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (location.isBlank()) {
                Snackbar.make(binding.root, "Please select a location", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                     binding.buttonFetchSunTimes.text = "Loading..."
                     binding.buttonFetchSunTimes.isEnabled = false
                }
                is SolarState.Success -> {
                    binding.buttonFetchSunTimes.visibility = View.GONE
                    binding.chipGroupSunTimes.visibility = View.VISIBLE
                    
                    val data = state.data
                    binding.chipSunrise.text = "Sunrise: ${formatSolarTime(data.sunrise)}"
                    binding.chipSunset.text = "Sunset: ${formatSolarTime(data.sunset)}"
                }
                is SolarState.Error -> {
                    binding.buttonFetchSunTimes.text = "Retry Sun Times"
                    binding.buttonFetchSunTimes.isEnabled = true
                    binding.buttonFetchSunTimes.visibility = View.VISIBLE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatSolarTime(isoString: String): String {
        return try {
            val zdt = ZonedDateTime.parse(isoString)
            val localZdt = zdt.withZoneSameInstant(ZoneId.systemDefault())
            localZdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: Exception) {
            isoString // Fallback to original string if parsing fails
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressSubmit.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !isLoading
        binding.imagePreview.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
