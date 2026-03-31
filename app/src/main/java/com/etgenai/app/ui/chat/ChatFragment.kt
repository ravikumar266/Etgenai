package com.etgenai.app.ui.chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etgenai.app.R
import com.etgenai.app.databinding.FragmentChatBinding
import com.etgenai.app.ui.history.ThreadAdapter
import java.io.File

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    private var pendingPdfFile: File? = null

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handlePdfSelection(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val incomingThreadId: String? = arguments?.getString("threadId")

        viewModel = if (incomingThreadId != null) {
            ViewModelProvider(this)[ChatViewModel::class.java]
        } else {
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

        viewModel.chatItems.observe(viewLifecycleOwner) { items ->
            adapter.setItems(items)
            if (items.isNotEmpty()) {
                binding.rvChat.scrollToPosition(items.size - 1)
            }
        }

        viewModel.threadId.observe(viewLifecycleOwner) { id ->
            binding.tvThreadId.text = if (id.isNullOrEmpty()) "" else "THREAD: ${id.take(8)}"
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            updateSendButton(loading)
        }

        binding.btnSend.setOnClickListener {
            if (viewModel.isLoading.value == true) {
                viewModel.stopResponse()
                return@setOnClickListener
            }

            val txt = binding.etMessage.text.toString().trim()
            if (txt.isNotEmpty() || pendingPdfFile != null) {
                viewModel.sendMessageWithPdf(txt, pendingPdfFile)
                binding.etMessage.text.clear()
                clearPendingPdf()
            }
        }

        binding.btnAttach.setOnClickListener {
            if (viewModel.isLoading.value == true) return@setOnClickListener
            pickPdfLauncher.launch("application/pdf")
        }

        binding.btnRemoveAttachment.setOnClickListener {
            if (viewModel.isLoading.value == true) return@setOnClickListener
            clearPendingPdf()
        }

        if (incomingThreadId != null && viewModel.currentThreadId != incomingThreadId) {
            viewModel.loadThread(incomingThreadId)
        }

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
            viewModel.fetchRecentThreads()
        }

        val btnNewChat = binding.navView.findViewById<LinearLayout>(R.id.btnNewChat)
        val rvDrawerThreads = binding.navView.findViewById<RecyclerView>(R.id.rvDrawerThreads)

        btnNewChat.setOnClickListener {
            if (viewModel.isLoading.value == true) return@setOnClickListener
            viewModel.resetThread()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        val drawerAdapter = ThreadAdapter { threadId ->
            if (viewModel.isLoading.value == true) return@ThreadAdapter
            viewModel.loadThread(threadId)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        rvDrawerThreads.layoutManager = LinearLayoutManager(requireContext())
        rvDrawerThreads.adapter = drawerAdapter

        viewModel.recentThreads.observe(viewLifecycleOwner) { threads ->
            drawerAdapter.setItems(threads)
        }

        viewModel.fetchRecentThreads()
        updateSendButton(viewModel.isLoading.value == true)
    }

    private fun updateSendButton(loading: Boolean) {
        binding.btnSend.setImageResource(
            if (loading) R.drawable.ic_stop_square
            else R.drawable.ic_send
        )
    }

    private fun handlePdfSelection(uri: Uri) {
        if (viewModel.isLoading.value == true) return

        val file = copyUriToTempFile(uri)
        if (file != null) {
            pendingPdfFile = file
            binding.tvAttachmentName.text = file.name
            binding.attachmentPreview.visibility = View.VISIBLE
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to prepare file for upload",
                Toast.LENGTH_SHORT
            ).show()
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