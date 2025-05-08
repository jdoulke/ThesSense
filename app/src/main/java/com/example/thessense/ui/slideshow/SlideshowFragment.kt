package com.example.thessense.ui.slideshow

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.thessense.databinding.FragmentSlideshowBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONObject
import java.io.InputStream

class SlideshowFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var overlayCircle: Circle? = null
    private var centerMarker: Marker? = null

    private var isMapExpanded = false
    private var selectedYear: Int = 2023
    private var selectedMonth: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root = binding.root

        val mapFragment = childFragmentManager.findFragmentById(com.example.thessense.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.fullscreenIcon.setOnClickListener { toggleMapSize() }

        val yearPicker = binding.yearPicker
        val monthPicker = binding.monthPicker

        yearPicker.minValue = 2017
        yearPicker.maxValue = 2024
        yearPicker.value = selectedYear

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.displayedValues = arrayOf(
            "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
            "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος",
            "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
        )
        monthPicker.value = selectedMonth

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            updateAirOverlay()
        }

        monthPicker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal
            updateAirOverlay()
        }

        return root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val thessaloniki = LatLng(40.6401, 22.9444)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(thessaloniki, 12f))

        val bounds = LatLngBounds(LatLng(40.60, 22.91), LatLng(40.64, 23.04))
        map.setLatLngBoundsForCameraTarget(bounds)

        map.setMinZoomPreference(11f)
        map.setMaxZoomPreference(16f)

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isIndoorLevelPickerEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = false

        val style = MapStyleOptions.loadRawResourceStyle(requireContext(), com.example.thessense.R.raw.map_style)
        map.setMapStyle(style)
        map.setInfoWindowAdapter(CustomInfoWindowAdapter())
        map.setOnCircleClickListener { showInfoWindow() }

        updateAirOverlay()
    }

    private fun toggleMapSize() {
        val initialHeight = binding.mapContainer.height
        val targetHeight = if (isMapExpanded) dpToPx(400) else ViewGroup.LayoutParams.MATCH_PARENT

        if (targetHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            val params = binding.mapContainer.layoutParams
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            binding.mapContainer.layoutParams = params
        } else {
            val animator = ValueAnimator.ofInt(initialHeight, targetHeight)
            animator.addUpdateListener { animation ->
                val params = binding.mapContainer.layoutParams
                params.height = animation.animatedValue as Int
                binding.mapContainer.layoutParams = params
            }
            animator.duration = 300
            animator.start()
        }

        binding.fullscreenIcon.setImageResource(
            if (isMapExpanded)
                com.example.thessense.R.drawable.ic_fullscreen
            else
                com.example.thessense.R.drawable.ic_fullscreen_close
        )

        isMapExpanded = !isMapExpanded
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun loadJSONFromAsset(filename: String): String {
        return try {
            val input: InputStream = requireContext().assets.open(filename)
            input.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun updateAirOverlay() {
        googleMap?.let { map ->
            overlayCircle?.remove()
            centerMarker?.remove()

            val jsonStr = loadJSONFromAsset("air_data.json")
            if (jsonStr.isBlank()) return

            val data = JSONObject(jsonStr)
            val yearArray = data.optJSONArray(selectedYear.toString()) ?: return
            val monthData = yearArray.optJSONObject(selectedMonth - 1) ?: return

            val co = monthData.optDouble("CO AVERAGE")
            val no = monthData.optDouble("NO AVERAGE")
            val no2 = monthData.optDouble("NO2 AVERAGE")
            val so2 = monthData.optDouble("SO2 AVERAGE")
            val o3 = monthData.optDouble("O3 AVERAGE")

            // Κανονικοποίηση με βάση τυπικά περιβαλλοντικά όρια σε μg/m³
            fun normalize(value: Double, max: Double): Double = (value / max).coerceIn(0.0, 1.0)

            val coNorm = normalize(co, 10000.0)   // CO: όριο ~10 mg/m³ => 10000 μg/m³
            val noNorm = normalize(no, 200.0)     // NO: ασφαλές όριο ~200 μg/m³
            val no2Norm = normalize(no2, 40.0)    // NO2: όριο ΕΕ
            val so2Norm = normalize(so2, 20.0)    // SO2: όριο ΠΟΥ
            val o3Norm = normalize(o3, 100.0)     // O3: όριο ΠΟΥ για 8ωρο

            val avgPollution = (coNorm + noNorm + no2Norm + so2Norm + o3Norm) / 5.0
            val norm = (1.0 - avgPollution).coerceIn(0.0, 1.0) // 1 = καθαρός αέρας

            val red = (255 * (1 - norm)).toInt()
            val green = (255 * norm).toInt()
            val circleColor = Color.argb(128, red, green, 0)

            val center = LatLng(40.6401, 22.9444)
            overlayCircle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(5000.0)
                    .strokeWidth(2f)
                    .strokeColor(circleColor)
                    .fillColor(circleColor)
                    .clickable(true)
            )

            val infoText = "CO: $co μg/m³\nNO: $no μg/m³\nNO₂: $no2 μg/m³\nSO₂: $so2 μg/m³\nO₃: $o3 μg/m³"
            centerMarker = map.addMarker(
                MarkerOptions()
                    .position(center)
                    .title("Ποιότητα Αέρα - $selectedMonth/$selectedYear")
                    .snippet(infoText)
                    .visible(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
        }
    }

    private fun showInfoWindow() {
        centerMarker?.showInfoWindow()
    }

    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val view = layoutInflater.inflate(com.example.thessense.R.layout.custom_info_window, null)

        override fun getInfoWindow(marker: Marker): View? {
            render(marker, view)
            return view
        }

        override fun getInfoContents(marker: Marker): View? = null

        private fun render(marker: Marker, view: View) {
            view.findViewById<TextView>(com.example.thessense.R.id.title).text = marker.title
            view.findViewById<TextView>(com.example.thessense.R.id.snippet).text = marker.snippet
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
