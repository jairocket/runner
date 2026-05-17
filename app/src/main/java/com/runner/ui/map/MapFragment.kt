package com.runner.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.runner.databinding.FragmentMapBinding
import com.runner.ui.tracking.LocationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

@Suppress("DEPRECATION")
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LocationViewModel by activityViewModels()
    internal val routePolyline = Polyline()
    private var hasInitialCenter = false
    private var overlayTimeoutRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.5)
            overlays.add(routePolyline)
        }

        drawHistory()

        when {
            viewModel.locationHistory.isNotEmpty() -> {
                // drawHistory() already centered on points.last() via animateTo
                hideOverlay()
            }
            else -> viewModel.locationLiveData.value?.let { loc ->
                binding.mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                hideOverlay()
            }
        }

        if (!hasInitialCenter) {
            val timeout = Runnable { hideOverlay() }
            overlayTimeoutRunnable = timeout
            view.postDelayed(timeout, 10_000L)
        }

        viewModel.locationLiveData.observe(viewLifecycleOwner) { location ->
            if (!hasInitialCenter) {
                binding.mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                hideOverlay()
            }
            if (viewModel.isTracking.value == true) {
                val point = GeoPoint(location.latitude, location.longitude)
                routePolyline.addPoint(point)
                binding.mapView.controller.animateTo(point)
                binding.mapView.invalidate()
            }
        }

        viewModel.trajectorySaved.observe(viewLifecycleOwner) {
            routePolyline.setPoints(emptyList())
            binding.mapView.invalidate()
        }

    }

    private fun hideOverlay() {
        _binding ?: return
        binding.locationLoadingOverlay.visibility = View.GONE
        hasInitialCenter = true
        overlayTimeoutRunnable?.let { binding.root.removeCallbacks(it) }
        overlayTimeoutRunnable = null
    }

    private fun drawHistory() {
        val points = viewModel.locationHistory.map { GeoPoint(it.latitude, it.longitude) }
        routePolyline.setPoints(points)
        if (points.isNotEmpty()) {
            binding.mapView.controller.animateTo(points.last())
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        overlayTimeoutRunnable?.let { binding.root.removeCallbacks(it) }
        overlayTimeoutRunnable = null
        _binding = null
    }
}
