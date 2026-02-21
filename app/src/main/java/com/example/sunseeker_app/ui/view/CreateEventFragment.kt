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
    private var selectedDateTime: java.util.Calendar? = null
    private var selectedSunType: String? = null



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventBinding.bind(view)

        // Listen for result from MapPickerFragment
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Double>("picked_lat")?.observe(viewLifecycleOwner) { lat ->
            val lng = savedStateHandle.get<Double>("picked_lng")
            val address = savedStateHandle.get<String>("picked_address")
            if (lng != null) {
                currentLat = lat
                currentLng = lng
                selectedLocationName = address ?: "Selected Location"
                binding.textSelectedLocation.text = selectedLocationName
                binding.textSelectedLocation.setTextColor(resources.getColor(android.R.color.black, null))
                viewModel.fetchSunTimes(lat, lng)
                // Clear to avoid re-triggering
                savedStateHandle.remove<Double>("picked_lat")
                savedStateHandle.remove<Double>("picked_lng")
                savedStateHandle.remove<String>("picked_address")
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
                binding.textSelectedTime.text = event.time
                binding.textSelectedTime.setTextColor(resources.getColor(android.R.color.black, null))
                
                existingImageUrl = event.imageUrl
                if (existingImageUrl.isNotBlank() && selectedImageUri == null) {
                    Glide.with(this)
                        .load(existingImageUrl)
                        .into(binding.imagePreview)
                    binding.textAddPhotoHint.visibility = View.GONE
                }

                if (event.sunType.isNotBlank()) {
                    selectedSunType = event.sunType
                    binding.chipGroupSunTimes.visibility = View.VISIBLE
                    when (event.sunType) {
                        "sunrise" -> binding.chipSunrise.isChecked = true
                        "sunset" -> binding.chipSunset.isChecked = true
                    }
                }
            }
        }

        binding.imagePreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.buttonPickLocation.setOnClickListener {
            findNavController().navigate(
                CreateEventFragmentDirections.actionCreateEventFragmentToMapPickerFragment()
            )
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

        binding.chipSunrise.setOnClickListener {
            selectedSunType = if (binding.chipSunrise.isChecked) "sunrise" else null
        }
        binding.chipSunset.setOnClickListener {
            selectedSunType = if (binding.chipSunset.isChecked) "sunset" else null
        }

        binding.buttonPickTime.setOnClickListener {
            showDateTimePicker()
        }

        binding.buttonSubmit.setOnClickListener {
            val title = binding.editTitle.text?.toString().orEmpty()
            val location = selectedLocationName.orEmpty()
            val description = binding.editDescription.text?.toString().orEmpty()
            val timeString = binding.textSelectedTime.text.toString()
            
            if (title.isBlank()) {
                Snackbar.make(binding.root, "Please enter a title", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (location.isBlank()) {
                Snackbar.make(binding.root, "Please select a location", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedDateTime == null && eventId == null) {
                 Snackbar.make(binding.root, "Please select a time", Snackbar.LENGTH_SHORT).show()
                 return@setOnClickListener
            }

            if (selectedSunType == null) {
                Snackbar.make(binding.root, "Please select Sunrise or Sunset", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalTime = if (selectedDateTime != null) {
                val format = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault())
                format.format(selectedDateTime!!.time)
            } else {
               if (eventId != null) binding.textSelectedTime.text.toString() else "TBD"
            }

            val sunType = selectedSunType!!

            if (eventId == null) {
                viewModel.createEvent(title, location, finalTime, description, selectedImageUri, sunType)
            } else {
                viewModel.updateEvent(
                    eventId,
                    title,
                    location,
                    finalTime,
                    description,
                    selectedImageUri,
                    existingImageUrl,
                    sunType
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

    private fun showDateTimePicker() {
        val currentDateTime = java.util.Calendar.getInstance()
        val startYear = currentDateTime.get(java.util.Calendar.YEAR)
        val startMonth = currentDateTime.get(java.util.Calendar.MONTH)
        val startDay = currentDateTime.get(java.util.Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(java.util.Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(java.util.Calendar.MINUTE)

        android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                val pickedDateTime = java.util.Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)
                selectedDateTime = pickedDateTime
                
                val format = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault())
                binding.textSelectedTime.text = format.format(pickedDateTime.time)
                binding.textSelectedTime.setTextColor(resources.getColor(android.R.color.black, null))
                
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
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
