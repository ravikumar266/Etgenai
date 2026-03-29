package com.etgenai.app.ui.scheduler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.etgenai.app.R
import com.etgenai.app.databinding.FragmentSchedulerBinding

class SchedulerFragment : Fragment() {

    private var _binding: FragmentSchedulerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SchedulerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchedulerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SchedulerViewModel::class.java]

        viewModel.schedulerStatus.observe(viewLifecycleOwner) { status ->
            val running = status.running
            binding.tvSchedulerRunning.text = if (running) "RUNNING" else "STOPPED"
            binding.tvSchedulerRunning.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (running) R.color.primary else R.color.error
                )
            )
            // Display individual job info
            val jobsText = status.jobs.joinToString("\n") { job ->
                "${job.name}: ${job.nextRun ?: "No scheduled run"}"
            }.ifEmpty { "No jobs configured" }
            binding.tvNextEmail.text = jobsText
            binding.tvNextBriefing.text = if (running) "Active" else "Inactive"
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnRunEmailNow.setOnClickListener {
            Toast.makeText(requireContext(), "Manual trigger not available on this backend", Toast.LENGTH_SHORT).show()
        }
        binding.btnRunBriefingNow.setOnClickListener {
            Toast.makeText(requireContext(), "Manual trigger not available on this backend", Toast.LENGTH_SHORT).show()
        }

        viewModel.fetchSchedulerStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
