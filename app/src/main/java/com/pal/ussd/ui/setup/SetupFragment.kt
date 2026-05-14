package com.pal.ussd.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.pal.ussd.R
import com.pal.ussd.databinding.FragmentSetupBinding

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        // Prevent screenshots on this sensitive screen
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (menuPin, transferPin, accountIndex) = viewModel.loadExisting()
        if (menuPin.isNotEmpty()) binding.etMenuPin.setText(menuPin)
        if (transferPin.isNotEmpty()) binding.etTransferPin.setText(transferPin)
        binding.etAccountIndex.setText(accountIndex)

        binding.btnSave.setOnClickListener {
            viewModel.save(
                menuPin = binding.etMenuPin.text?.toString() ?: "",
                transferPin = binding.etTransferPin.text?.toString() ?: "",
                accountIndex = binding.etAccountIndex.text?.toString() ?: "1"
            )
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Snackbar.make(binding.root, R.string.setup_saved, Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_setup_to_transfer)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        _binding = null
    }
}
