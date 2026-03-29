package com.etgenai.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.etgenai.app.databinding.FragmentSettingsBinding
import com.etgenai.app.ui.chat.ChatViewModel
import com.etgenai.app.ui.scheduler.SchedulerViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val schedulerViewModel = ViewModelProvider(this)[SchedulerViewModel::class.java]

        schedulerViewModel.healthStatus.observe(viewLifecycleOwner) { health ->
            binding.tvHealthStatus.text = health.status.uppercase()
            binding.tvHealthVersion.text = "v${health.version}"
            binding.tvHealthScheduler.text = if (health.scheduler) "running" else "stopped"
        }

        binding.btnCheckHealth.setOnClickListener {
            schedulerViewModel.fetchHealth()
        }

        schedulerViewModel.fetchHealth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
