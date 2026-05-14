package com.pal.ussd.ui.progress

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pal.ussd.R
import com.pal.ussd.databinding.FragmentProgressBinding

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgressViewModel by viewModels()
    private val adapter = StepsAdapter()

    private val recipient: String by lazy { arguments?.getString("recipient") ?: "" }
    private val amount: String by lazy { arguments?.getString("amount") ?: "" }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startExecution()
        else showError(getString(R.string.error_permission))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSteps.adapter = adapter

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_progress_to_transfer)
        }

        viewModel.steps.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
        }

        viewModel.finished.observe(viewLifecycleOwner) { success ->
            if (success != null) {
                binding.progressBar.visibility = View.GONE
                binding.tvResult.visibility = View.VISIBLE
                binding.btnBack.visibility = View.VISIBLE

                if (success) {
                    binding.tvResult.text = getString(R.string.progress_success)
                    binding.tvResult.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.step_done)
                    )
                } else {
                    binding.tvResult.text = getString(R.string.progress_failed)
                    binding.tvResult.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.step_failed)
                    )
                }
            }
        }

        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startExecution()
        } else {
            requestPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun startExecution() {
        viewModel.startTransfer(recipient, amount)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvResult.text = message
        binding.tvResult.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.step_failed)
        )
        binding.tvResult.visibility = View.VISIBLE
        binding.btnBack.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
