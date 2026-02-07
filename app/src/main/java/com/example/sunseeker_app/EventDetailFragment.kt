package com.example.sunseeker_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.sunseeker_app.databinding.FragmentEventDetailBinding

class EventDetailFragment : Fragment() {
    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private val args: EventDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.eventIdText.text = "Event ID: ${args.eventId}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}