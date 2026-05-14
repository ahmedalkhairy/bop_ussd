package com.pal.ussd.ui.progress

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pal.ussd.R
import com.pal.ussd.databinding.FragmentProgressBinding
import com.pal.ussd.ussd.UssdAccessibilityService

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
        if (granted) checkAccessibilityAndStart()
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
            checkAccessibilityAndStart()
        } else {
            requestPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun checkAccessibilityAndStart() {
        if (isAccessibilityEnabled()) {
            startExecution()
        } else {
            showAccessibilityDialog()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expectedComponent = ComponentName(requireContext(), UssdAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedComponent.flattenToString(), ignoreCase = true) }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.accessibility_required_title))
            .setMessage(getString(R.string.accessibility_required_msg))
            .setPositiveButton(getString(R.string.btn_open_accessibility)) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            .setNegativeButton("تخطي — جرب بدونها") { _, _ ->
                // Try anyway with TelephonyManager only
                startExecution()
            }
            .setCancelable(false)
            .show()
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
