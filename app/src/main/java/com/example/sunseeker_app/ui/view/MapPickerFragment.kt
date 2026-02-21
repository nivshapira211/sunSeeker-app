package com.example.sunseeker_app.ui.view

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ArrayAdapter
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentMapPickerBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MapPickerFragment : Fragment(R.layout.fragment_map_picker), OnMapReadyCallback {

    private var _binding: FragmentMapPickerBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String? = null

    private var placesClient: PlacesClient? = null
    private var sessionToken = AutocompleteSessionToken.newInstance()
    private var autocompleteAdapter: ArrayAdapter<String>? = null
    private val predictionPlaceIds = mutableListOf<String>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapPickerBinding.bind(view)

        // Initialize Places (autocomplete won't work without a valid API key)
        try {
            val apiKey = getString(R.string.google_maps_key)
            if (apiKey.isNotBlank()) {
                if (!Places.isInitialized()) {
                    Places.initialize(requireContext(), apiKey)
                }
                placesClient = Places.createClient(requireContext())
            }
        } catch (_: Exception) { }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup autocomplete adapter
        autocompleteAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.editSearch.setAdapter(autocompleteAdapter)

        // Fetch predictions as user types (debounced)
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim().orEmpty()
                if (query.length >= 2) {
                    searchRunnable = Runnable { fetchPredictions(query) }
                    searchHandler.postDelayed(searchRunnable!!, 300)
                }
            }
        })

        // When user selects a prediction from dropdown
        binding.editSearch.setOnItemClickListener { _, _, position, _ ->
            hideKeyboard()
            val selectedText = autocompleteAdapter?.getItem(position) ?: return@setOnItemClickListener
            geocodeAndNavigate(selectedText)
        }

        // Also handle keyboard search action
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                val query = binding.editSearch.text?.toString().orEmpty().trim()
                if (query.isNotEmpty()) geocodeAndNavigate(query)
                true
            } else false
        }

        binding.buttonConfirm.setOnClickListener {
            val latLng = selectedLatLng ?: return@setOnClickListener
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.apply {
                set("picked_lat", latLng.latitude)
                set("picked_lng", latLng.longitude)
                set("picked_address", selectedAddress ?: "Selected Location")
            }
            navController.popBackStack()
        }

        binding.fabMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun fetchPredictions(query: String) {
        val client = placesClient ?: return

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()

        client.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val suggestions = mutableListOf<String>()
            predictionPlaceIds.clear()
            for (prediction in response.autocompletePredictions) {
                suggestions.add(prediction.getFullText(null).toString())
                predictionPlaceIds.add(prediction.placeId)
            }
            autocompleteAdapter?.clear()
            autocompleteAdapter?.addAll(suggestions)
            autocompleteAdapter?.notifyDataSetChanged()
            if (suggestions.isNotEmpty() && binding.editSearch.hasFocus()) {
                binding.editSearch.showDropDown()
            }
        }.addOnFailureListener {
            // Silently fail — user can still tap the map
        }
    }

    private fun geocodeAndNavigate(query: String) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            if (!results.isNullOrEmpty()) {
                val addr = results[0]
                val latLng = LatLng(addr.latitude, addr.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                placeMarker(latLng)
            } else {
                Snackbar.make(binding.root, "No results found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Search failed", Snackbar.LENGTH_SHORT).show()
        }
        // Reset session token after a selection
        sessionToken = AutocompleteSessionToken.newInstance()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        map.setOnMapClickListener { latLng ->
            placeMarker(latLng)
        }

        if (hasLocationPermission()) {
            moveToCurrentLocation()
        } else {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun placeMarker(latLng: LatLng) {
        selectedLatLng = latLng
        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))

        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val addressLine = addr.getAddressLine(0)
                    ?: listOfNotNull(addr.featureName, addr.locality, addr.countryName)
                        .joinToString(", ")
                selectedAddress = addressLine
                binding.textSelectedAddress.text = addressLine
            } else {
                selectedAddress = "%.4f, %.4f".format(latLng.latitude, latLng.longitude)
                binding.textSelectedAddress.text = selectedAddress
            }
        } catch (e: Exception) {
            selectedAddress = "%.4f, %.4f".format(latLng.latitude, latLng.longitude)
            binding.textSelectedAddress.text = selectedAddress
        }

        binding.buttonConfirm.isEnabled = true
    }

    @Suppress("MissingPermission")
    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) return

        googleMap?.isMyLocationEnabled = true

        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}
