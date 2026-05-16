package com.runner.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
    private val routePolyline = Polyline()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            overlays.add(routePolyline)
        }

        drawHistory()

        viewModel.locationLiveData.observe(viewLifecycleOwner) { location ->
            if (viewModel.isTracking.value == true) {
                val point = GeoPoint(location.latitude, location.longitude)
                routePolyline.addPoint(point)
                binding.mapView.controller.animateTo(point)
                binding.mapView.invalidate()
            }
        }

        binding.buttonMapBack.setOnClickListener { findNavController().navigateUp() }
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
        _binding = null
    }
}
