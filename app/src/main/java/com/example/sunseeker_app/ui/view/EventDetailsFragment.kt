package com.example.sunseeker_app.ui.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentEventDetailsBinding
import com.example.sunseeker_app.ui.viewmodel.EventViewModel
import com.example.sunseeker_app.ui.viewmodel.UiState
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EventDetailsFragment : Fragment(R.layout.fragment_event_details) {

    private var _binding: FragmentEventDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by viewModels()
    private val args: EventDetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEventDetailsBinding.bind(view)

        val eventId = args.eventId
        
        // Setup toolbar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        viewModel.getEvent(eventId).observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            
            // Text info
            binding.textTitle.text = event.title
            binding.textLocation.text = event.location
            binding.textTime.text = event.time
            binding.textDescription.text = event.description.ifBlank { "No description provided." }
            binding.textAttendeesCount.text =
                if (event.attendeeIds.isEmpty()) getString(R.string.event_attendees_none)
                else "${event.attendeeIds.size} people attending"

            // Show attendee names as chips
            binding.chipGroupAttendees.removeAllViews()
            if (event.attendeeNames.isNotEmpty()) {
                event.attendeeNames.values.forEach { name ->
                    val chip = Chip(requireContext()).apply {
                        text = name
                        isClickable = false
                        isCheckable = false
                    }
                    binding.chipGroupAttendees.addView(chip)
                }
                binding.chipGroupAttendees.visibility = View.VISIBLE
            } else if (event.attendeeIds.isNotEmpty()) {
                // Fallback: show count for old events without names
                binding.chipGroupAttendees.visibility = View.GONE
            } else {
                binding.chipGroupAttendees.visibility = View.GONE
            }

            if (event.sunType.isNotBlank()) {
                binding.textSunType.visibility = View.VISIBLE
                binding.textSunType.text = if (event.sunType == "sunrise") "\u2600\uFE0F Sunrise" else "\uD83C\uDF05 Sunset"
            } else {
                binding.textSunType.visibility = View.GONE
            }

            // Image
            if (event.imageUrl.isNotBlank()) {
                 com.bumptech.glide.Glide.with(this)
                    .load(event.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.event_placeholder)
                    .into(binding.imageEventDetail)
            } else {
                 binding.imageEventDetail.setImageResource(R.drawable.event_placeholder)
            }

            // Owner actions
            val isOwner = viewModel.isOwner(event)
            binding.ownerActionsContainer.visibility = if (isOwner) View.VISIBLE else View.GONE
            
            // Hide join/leave button for past events
            val timeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
            val eventTime = try { timeFormat.parse(event.time) } catch (_: Exception) { null }
            val isPast = eventTime != null && eventTime.before(Date())

            if (isPast) {
                binding.buttonJoinEvent.visibility = View.GONE
            } else {
                binding.buttonJoinEvent.visibility = View.VISIBLE

                val userId = viewModel.currentUserId
                val isJoined = event.attendeeIds.contains(userId)

                val primaryColor = com.google.android.material.color.MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
                val errorColor = com.google.android.material.color.MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorError)

                binding.buttonJoinEvent.text = getString(
                    if (isJoined) R.string.action_leave_event else R.string.action_join_event
                )
                binding.buttonJoinEvent.backgroundTintList = ColorStateList.valueOf(
                    if (isJoined) errorColor else primaryColor
                )

                binding.buttonJoinEvent.setOnClickListener {
                    if (isJoined) {
                        viewModel.leaveEvent(eventId)
                    } else {
                        viewModel.joinEvent(eventId)
                    }
                }
            }
        }

        binding.buttonEditEvent.setOnClickListener {
            val action = EventDetailsFragmentDirections
                .actionEventDetailsFragmentToCreateEventFragment(eventId)
            findNavController().navigate(action)
        }

        binding.buttonDeleteEvent.setOnClickListener {
            viewModel.deleteEvent(eventId)
        }

        viewModel.joinState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> setLoading(true)
                is UiState.Success -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearJoinState()
                }
                is UiState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearJoinState()
                }
                null -> Unit
            }
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> setLoading(true)
                is UiState.Success -> {
                    viewModel.clearDeleteState()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearDeleteState()
                }
                null -> Unit
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressJoin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonJoinEvent.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
