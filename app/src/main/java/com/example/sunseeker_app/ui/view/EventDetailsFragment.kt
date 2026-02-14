package com.example.sunseeker_app.ui.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentEventDetailsBinding
import com.example.sunseeker_app.ui.viewmodel.EventViewModel
import com.example.sunseeker_app.ui.viewmodel.ActionState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

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
            binding.textAttendees.text =
                if (event.attendeeIds.isEmpty()) "No attendees yet"
                else "${event.attendeeIds.size} people attending" // Simplify for now

            // Image
            if (event.imageUrl.isNotBlank()) {
                 com.bumptech.glide.Glide.with(this)
                    .load(event.imageUrl)
                    .centerCrop()
                    .placeholder(com.example.sunseeker_app.R.drawable.event_placeholder)
                    .into(binding.imageEventDetail)
            } else {
                 binding.imageEventDetail.setImageResource(com.example.sunseeker_app.R.drawable.event_placeholder)
            }

            // Owner actions
            val isOwner = viewModel.isOwner(event)
            binding.ownerActionsContainer.visibility = if (isOwner) View.VISIBLE else View.GONE
            
            // Join/Leave button logic
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isJoined = event.attendeeIds.contains(userId)
            
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
            val errorColor = com.google.android.material.color.MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorError)

            binding.buttonJoinEvent.text = if (isJoined) "Leave Event" else "Join Event"
            binding.buttonJoinEvent.setBackgroundColor(
                if (isJoined) errorColor 
                else primaryColor
            )
            
            binding.buttonJoinEvent.setOnClickListener {
                if (isJoined) {
                    viewModel.leaveEvent(eventId)
                } else {
                    viewModel.joinEvent(eventId)
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
                is ActionState.Loading -> setLoading(true)
                is ActionState.Success -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is ActionState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
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
