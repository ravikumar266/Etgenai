package com.etgenai.app.ui.chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.etgenai.app.databinding.FragmentChatBinding
import java.io.File
import androidx.core.view.GravityCompat
import com.etgenai.app.ui.history.ThreadAdapter
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etgenai.app.R

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter
    private var pendingPdfFile: File? = null

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePdfSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use activity-scoped ViewModel so the thread survives tab switches,
        // UNLESS we came from History with a specific thread to open.
        val incomingThreadId: String? = arguments?.getString("threadId")

        viewModel = if (incomingThreadId != null) {
            // New scope per thread when loading from history
            ViewModelProvider(this)[ChatViewModel::class.java]
        } else {
            // Shared scope so chat state survives nav tab switches
            ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        }

        adapter = ChatAdapter(
            onApproveEmail = { viewModel.approveEmail("approve") },
            onDenyEmail = { viewModel.approveEmail("deny") },
        )

        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter

        // Observe combined item list (messages + inline approval cards)
        viewModel.chatItems.observe(viewLifecycleOwner) { items ->
            adapter.setItems(items)
            if (items.isNotEmpty()) binding.rvChat.scrollToPosition(items.size - 1)
        }

        // Show thread ID in header
        viewModel.threadId.observe(viewLifecycleOwner) { id ->
            binding.tvThreadId.text = if (id.isNullOrEmpty()) "" else "THREAD: ${id.take(8)}"
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrEmpty()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }

        binding.btnSend.setOnClickListener {
            val txt = binding.etMessage.text.toString().trim()
            if (txt.isNotEmpty() || pendingPdfFile != null) {
                viewModel.sendMessageWithPdf(txt, pendingPdfFile)
                binding.etMessage.text.clear()
                clearPendingPdf()
            }
        }

        binding.btnAttach.setOnClickListener {
            pickPdfLauncher.launch("application/pdf")
        }

        binding.btnRemoveAttachment.setOnClickListener {
            clearPendingPdf()
        }

        // Load history if opened from history screen
        if (incomingThreadId != null && viewModel.currentThreadId != incomingThreadId) {
            viewModel.loadThread(incomingThreadId)
        }

        // ── Drawer UI Setup ──────────────────────────────────────────

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
            viewModel.fetchRecentThreads() // Refresh on open
        }

        val btnNewChat = binding.navView.findViewById<LinearLayout>(R.id.btnNewChat)
        val rvDrawerThreads = binding.navView.findViewById<RecyclerView>(R.id.rvDrawerThreads)

        btnNewChat.setOnClickListener {
            viewModel.resetThread()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        val drawerAdapter = ThreadAdapter { threadId ->
            viewModel.loadThread(threadId)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        rvDrawerThreads.layoutManager = LinearLayoutManager(requireContext())
        rvDrawerThreads.adapter = drawerAdapter

        viewModel.recentThreads.observe(viewLifecycleOwner) { threads ->
            drawerAdapter.setItems(threads)
        }

        viewModel.fetchRecentThreads()
    }

    private fun handlePdfSelection(uri: Uri) {
        val file = copyUriToTempFile(uri)
        if (file != null) {
            pendingPdfFile = file
            binding.tvAttachmentName.text = file.name
            binding.attachmentPreview.visibility = View.VISIBLE
        } else {
            Toast.makeText(requireContext(), "Failed to prepare file for upload", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearPendingPdf() {
        pendingPdfFile = null
        binding.attachmentPreview.visibility = View.GONE
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
