package com.etgenai.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etgenai.app.R
import com.etgenai.app.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: ThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        adapter = ThreadAdapter { threadId ->
            // Navigate to ChatFragment and load this thread's history
            val bundle = bundleOf("threadId" to threadId)
            findNavController().navigate(
                R.id.action_history_to_chat,
                bundle
            )
        }

        binding.rvThreads.layoutManager = LinearLayoutManager(requireContext())
        binding.rvThreads.adapter = adapter

        viewModel.threads.observe(viewLifecycleOwner) { threads ->
            adapter.setItems(threads)
            binding.tvEmptyState.visibility = if (threads.isEmpty()) View.VISIBLE else View.GONE
            binding.rvThreads.visibility = if (threads.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.fetchThreads()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
