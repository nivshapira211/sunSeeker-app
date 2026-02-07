package com.example.sunseeker_app.ui.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sunseeker_app.R
import com.example.sunseeker_app.databinding.FragmentFeedBinding
import com.example.sunseeker_app.ui.viewmodel.FeedViewModel
import com.example.sunseeker_app.ui.viewmodel.FeedState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: EventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeedBinding.bind(view)

        adapter = EventsAdapter(onJoinClick = { event ->
            Snackbar.make(binding.root, "Joined ${event.location}", Snackbar.LENGTH_SHORT).show()
        })
        binding.recyclerEvents.adapter = adapter

        binding.fabCreateEvent.setOnClickListener {
            val action = FeedFragmentDirections.actionFeedFragmentToCreateEventFragment()
            findNavController().navigate(action)
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedState.Loading -> binding.progressFeed.visibility = View.VISIBLE
                is FeedState.Idle -> binding.progressFeed.visibility = View.GONE
                is FeedState.Error -> {
                    binding.progressFeed.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
