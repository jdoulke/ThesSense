package com.example.thessense.ui.gallery

import android.R
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.thessense.databinding.FragmentGalleryBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.io.InputStream
import java.lang.reflect.Field

class GalleryFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    private var isMapExpanded = false
    private lateinit var mapContainer: FrameLayout
    private lateinit var fullscreenIcon: ImageView

    private var selectedYear: Int = 2023
    private var selectedMonth: Int = 1

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

        val yearPicker: NumberPicker = root.findViewById(com.example.thessense.R.id.year_picker)
        val monthPicker: NumberPicker = root.findViewById(com.example.thessense.R.id.month_picker)

        yearPicker.minValue = 2023
        yearPicker.maxValue = 2024
        yearPicker.value = 2023

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = 1

        val months = arrayOf("Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος", "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος", "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος")
        monthPicker.displayedValues = months

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            addMarkersFromJson()
        }

        monthPicker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal
            addMarkersFromJson()
        }

        return root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val thessaloniki = LatLng(40.6401, 22.9444)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(thessaloniki, 13f))

        val bounds = LatLngBounds(LatLng(40.62, 22.93), LatLng(40.65, 23.04))
        map.setLatLngBoundsForCameraTarget(bounds)

        map.setMinZoomPreference(13f)
        map.setMaxZoomPreference(16f)

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isIndoorLevelPickerEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = false

        val style = MapStyleOptions.loadRawResourceStyle(requireContext(), com.example.thessense.R.raw.map_style)
        map.setMapStyle(style)
        addMarkersFromJson()
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

    private fun loadJSONFromAsset(): String {
        val json: String?
        try {
            val inputStream: InputStream = requireContext().assets.open("water_data.json")
            json = inputStream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return json
    }

    private fun addMarkersFromJson() {
        googleMap?.let { map ->
            map.clear()
            try {
                val jsonData = loadJSONFromAsset()
                val jsonObject = JSONObject(jsonData)

                val targetMonth = when (selectedMonth) {
                    1 -> "Ιανουάριος"
                    2 -> "Φεβρουάριος"
                    3 -> "Μάρτιος"
                    4 -> "Απρίλιος"
                    5 -> "Μάιος"
                    6 -> "Ιούνιος"
                    7 -> "Ιούλιος"
                    8 -> "Αύγουστος"
                    9 -> "Σεπτέμβριος"
                    10 -> "Οκτώβριος"
                    11 -> "Νοέμβριος"
                    12 -> "Δεκέμβριος"
                    else -> ""
                }

                val location = jsonObject.getJSONArray("40 Εκκλησίες")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val snippet = "Θολότητα NTU: $tholotita"
                        val ekklisiesLocation = LatLng(40.6457, 22.9597)
                        map.addMarker(
                            MarkerOptions()
                                .position(ekklisiesLocation)
                                .title("40 Εκκλησίες")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}