package com.pal.ussd.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pal.ussd.R
import com.pal.ussd.databinding.FragmentMacrodroidGuideBinding

class MacroDroidGuideFragment : Fragment() {

    private var _binding: FragmentMacrodroidGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMacrodroidGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOpenMacrodroid.setOnClickListener {
            val intent = requireContext().packageManager
                .getLaunchIntentForPackage("com.arlosoft.macrodroid")
            if (intent != null) startActivity(intent)
        }

        binding.btnDone.setOnClickListener {
            findNavController().navigate(R.id.action_macroDroidGuide_to_transfer)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
