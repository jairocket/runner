package com.runner.ui.tracking

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.runner.R
import com.runner.databinding.FragmentTrackingBinding

class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isTracking.observe(viewLifecycleOwner) { tracking ->
            applyTrackingState(tracking)
        }

        viewModel.elapsedSeconds.observe(viewLifecycleOwner) { secs ->
            binding.textTimerDisplay.text = formatTime(secs)
            if (viewModel.isTracking.value != true) {
                applyTrackingState(false)
            }
        }

        viewModel.distanceKm.observe(viewLifecycleOwner) { km ->
            binding.textDistanceValue.text = "%.2f".format(km)
        }

        viewModel.paceSecPerKm.observe(viewLifecycleOwner) { pace ->
            binding.textPaceValue.text = if (pace != null) formatPace(pace) else "--:--"
        }

        binding.buttonStart.setOnClickListener { viewModel.startTracking() }
        binding.buttonStop.setOnClickListener { viewModel.stopTracking() }
        binding.buttonResume.setOnClickListener { viewModel.resumeTracking() }
        binding.buttonSave.setOnClickListener { viewModel.resetTimer() }
        binding.textButtonMap.setOnClickListener {
            findNavController().navigate(R.id.action_TrackingFragment_to_MapFragment)
        }
        binding.textButtonHistory.setOnClickListener {
            findNavController().navigate(R.id.action_TrackingFragment_to_HistoryFragment)
        }
    }

    private fun applyTrackingState(isTracking: Boolean) {
        val lime = Color.parseColor("#C6FF00")
        val white = Color.parseColor("#F0F0F0")
        val muted = Color.parseColor("#555566")
        val danger = Color.parseColor("#FF3B30")

        val hasStopped = !isTracking && (viewModel.elapsedSeconds.value ?: 0L) > 0L

        when {
            isTracking -> {
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(lime)
                binding.textStatusLabel.text = "RUNNING"
                binding.textTimerDisplay.setTextColor(lime)
                binding.textPaceValue.setTextColor(lime)
                binding.textDistanceValue.setTextColor(lime)
                binding.rowStartResume.visibility = View.GONE
                binding.buttonStop.visibility = View.VISIBLE
                binding.buttonSave.visibility = View.GONE
            }
            hasStopped -> {
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(danger)
                binding.textStatusLabel.text = "STOPPED"
                binding.textTimerDisplay.setTextColor(white)
                binding.textPaceValue.setTextColor(white)
                binding.textDistanceValue.setTextColor(white)
                binding.rowStartResume.visibility = View.VISIBLE
                binding.buttonResume.visibility = View.VISIBLE
                binding.buttonStop.visibility = View.GONE
                binding.buttonSave.visibility = View.VISIBLE
            }
            else -> {
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(muted)
                binding.textStatusLabel.text = "IDLE"
                binding.textTimerDisplay.setTextColor(white)
                binding.textPaceValue.setTextColor(white)
                binding.textDistanceValue.setTextColor(white)
                binding.rowStartResume.visibility = View.VISIBLE
                binding.buttonResume.visibility = View.GONE
                binding.buttonStop.visibility = View.GONE
                binding.buttonSave.visibility = View.GONE
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun formatPace(secsPerKm: Double): String {
        val m = (secsPerKm / 60).toLong()
        val s = (secsPerKm % 60).toLong()
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
