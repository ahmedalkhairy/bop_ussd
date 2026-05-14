package com.pal.ussd.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.pal.ussd.R
import com.pal.ussd.databinding.FragmentTransferBinding

class TransferFragment : Fragment() {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransferViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Redirect to setup if not configured
        if (!viewModel.isConfigured()) {
            findNavController().navigate(R.id.action_transfer_to_setup)
            return
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_transfer_to_setup)
        }

        binding.btnTransfer.setOnClickListener {
            viewModel.submitTransfer(
                recipient = binding.etRecipient.text?.toString()?.trim() ?: "",
                amount = binding.etAmount.text?.toString()?.trim() ?: ""
            )
        }

        viewModel.validationError.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.navigateToProgress.observe(viewLifecycleOwner) { pair ->
            if (pair != null) {
                val bundle = Bundle().apply {
                    putString("recipient", pair.first)
                    putString("amount", pair.second)
                }
                findNavController().navigate(R.id.action_transfer_to_progress, bundle)
                viewModel.onNavigated()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
