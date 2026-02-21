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
import com.example.sunseeker_app.ui.viewmodel.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeedBinding.bind(view)

        val currentUserId = viewModel.currentUserId

        adapter = FeedAdapter(
            onJoinClick = { event ->
                val isJoined = currentUserId != null && event.attendeeIds.contains(currentUserId)
                if (isJoined) viewModel.leaveEvent(event.id) else viewModel.joinEvent(event.id)
            },
            onItemClick = { event ->
                val action = FeedFragmentDirections.actionFeedFragmentToEventDetailsFragment(event.id)
                findNavController().navigate(action)
            },
            onSectionToggle = { title ->
                if (title == "Past Events") viewModel.togglePastSection()
                else viewModel.toggleUpcomingSection()
            },
            currentUserId = currentUserId
        )
        binding.recyclerEvents.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.fabCreateEvent.setOnClickListener {
            val action = FeedFragmentDirections.actionFeedFragmentToCreateEventFragment(null)
            findNavController().navigate(action)
        }

        viewModel.feedItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressFeed.visibility = View.VISIBLE
                    }
                }
                is FeedState.Idle -> {
                    binding.progressFeed.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                is FeedState.Error -> {
                    binding.progressFeed.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.joinState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.progressFeed.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressFeed.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is UiState.Error -> {
                    binding.progressFeed.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
