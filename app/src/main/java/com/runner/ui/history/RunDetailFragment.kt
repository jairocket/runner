package com.runner.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.runner.R
import com.runner.databinding.FragmentRunDetailBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

@Suppress("DEPRECATION")
class RunDetailFragment : Fragment() {

    private var _binding: FragmentRunDetailBinding? = null
    private val binding get() = _binding!!

    private val runId: String by lazy { requireArguments().getString(ARG_RUN_ID)!! }
    private val viewModel: RunDetailViewModel by viewModels { RunDetailViewModel.factory(runId) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentRunDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val run = viewModel.run ?: return

        binding.textDetailDate.text = run.date
        binding.textDetailDistance.text = run.distanceKm
        binding.textDetailDuration.text = run.duration
        binding.textDetailPace.text = getString(R.string.run_detail_pace_value, run.paceMinKm)
        binding.textDetailAvgSpeed.text = getString(R.string.run_detail_speed_value, avgSpeedKmh(run.distanceKm, run.duration))

        val points = run.positions.map { GeoPoint(it.lat, it.lon) }
        val polyline = Polyline().apply { setPoints(points) }

        binding.mapViewDetail.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            overlays.add(polyline)
            if (points.isNotEmpty()) {
                controller.setCenter(points[points.size / 2])
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapViewDetail?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapViewDetail?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun avgSpeedKmh(distanceKm: String, duration: String): String {
        val km = distanceKm.removeSuffix(" km").toDoubleOrNull() ?: return "--"
        val parts = duration.split(":")
        if (parts.size != 2) return "--"
        val mins = parts[0].toLongOrNull() ?: return "--"
        val secs = parts[1].toLongOrNull() ?: return "--"
        val hours = (mins * 60 + secs) / 3600.0
        if (hours == 0.0) return "--"
        return "%.1f".format(km / hours)
    }

    companion object {
        const val ARG_RUN_ID = "runId"
    }
}
