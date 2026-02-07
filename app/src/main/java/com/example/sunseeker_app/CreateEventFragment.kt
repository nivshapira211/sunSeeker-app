package com.example.sunseeker_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.sunseeker_app.databinding.FragmentCreateEventBinding

class CreateEventFragment : Fragment() {
    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!
    private val args: CreateEventFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val eventId = args.eventId
        if (eventId != null) {
            binding.saveEventButton.text = "Update Event"
            // TODO: Load event data and populate fields
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}