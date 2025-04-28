package com.example.thessense.ui.gallery

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thessense.databinding.FragmentGalleryBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.internal.ViewUtils.dpToPx

class GalleryFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private var isMapExpanded = false
    private lateinit var mapContainer: FrameLayout
    private lateinit var fullscreenIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val mapFragment = childFragmentManager.findFragmentById(com.example.thessense.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fullscreenIcon = root.findViewById<ImageView>(com.example.thessense.R.id.fullscreen_icon)
        mapContainer = root.findViewById(com.example.thessense.R.id.map_container)

        fullscreenIcon.setOnClickListener{toggleMapSize()}


        return root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val thessaloniki = LatLng(40.6401, 22.9444)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thessaloniki, 12f))

        val bounds = LatLngBounds(LatLng(40.62, 22.93), LatLng(40.65, 23.04))
        googleMap.setLatLngBoundsForCameraTarget(bounds)

        googleMap.setMinZoomPreference(11f)
        googleMap.setMaxZoomPreference(16f)

        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isIndoorLevelPickerEnabled = false
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isCompassEnabled = false

        val style = MapStyleOptions.loadRawResourceStyle(requireContext(), com.example.thessense.R.raw.map_style)
        googleMap.setMapStyle(style)
    }

    private fun toggleMapSize() {
        val initialHeight = mapContainer.height
        val targetHeight = if (isMapExpanded) dpToPx(300) else ViewGroup.LayoutParams.MATCH_PARENT

        if (targetHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            val params = mapContainer.layoutParams
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            mapContainer.layoutParams = params
        } else {
            val animator = ValueAnimator.ofInt(initialHeight, targetHeight)
            animator.addUpdateListener { animation ->
                val params = mapContainer.layoutParams
                params.height = animation.animatedValue as Int
                mapContainer.layoutParams = params
            }
            animator.duration = 300
            animator.start()
        }

        if (isMapExpanded) {
            fullscreenIcon.setImageResource(com.example.thessense.R.drawable.ic_fullscreen) // your expand icon
        } else {
            fullscreenIcon.setImageResource(com.example.thessense.R.drawable.ic_fullscreen_close) // your collapse icon
        }

        isMapExpanded = !isMapExpanded
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}