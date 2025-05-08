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
import com.google.android.gms.maps.model.Marker
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

    private var selectedYear: Int = 2020
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

        yearPicker.minValue = 2020
        yearPicker.maxValue = 2024
        yearPicker.value = 2020

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

        val bounds = LatLngBounds(LatLng(40.60, 22.91), LatLng(40.64, 23.04))
        map.setLatLngBoundsForCameraTarget(bounds)

        map.setMinZoomPreference(13f)
        map.setMaxZoomPreference(16f)

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isIndoorLevelPickerEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = false

        val style = MapStyleOptions.loadRawResourceStyle(requireContext(), com.example.thessense.R.raw.map_style)
        map.setMapStyle(style)
        map.setInfoWindowAdapter(CustomInfoWindowAdapter())
        addMarkersFromJson()
    }

    private fun toggleMapSize() {
        val initialHeight = mapContainer.height
        val targetHeight = if (isMapExpanded) dpToPx(400) else ViewGroup.LayoutParams.MATCH_PARENT

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

                var location = jsonObject.getJSONArray("40 Εκκλησίες")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val ekklisiesLocation = LatLng(40.6328, 22.9677)
                        map.addMarker(
                            MarkerOptions()
                                .position(ekklisiesLocation)
                                .title("40 Εκκλησίες")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Ανάληψη")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val analipsiLocation = LatLng(40.6051, 22.9598)
                        map.addMarker(
                            MarkerOptions()
                                .position(analipsiLocation)
                                .title("Ανάληψη")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Άνω Πόλη")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val anopoliLocation = LatLng(40.6419, 22.9460)
                        map.addMarker(
                            MarkerOptions()
                                .position(anopoliLocation)
                                .title("Άνω Πόλη")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Άνω Τούμπα")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val anotoumpaLocation = LatLng(40.6179, 22.9756)
                        map.addMarker(
                            MarkerOptions()
                                .position(anotoumpaLocation)
                                .title("Άνω Τούμπα")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("ΔΕΘ-ΧΑΝΘ")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val dethhanthLocation = LatLng(40.6264, 22.9526)
                        map.addMarker(
                            MarkerOptions()
                                .position(dethhanthLocation)
                                .title("ΔΕΘ-ΧΑΝΘ")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Χαριλάου")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val harilaouLocation = LatLng(40.6001, 22.9702)
                        map.addMarker(
                            MarkerOptions()
                                .position(harilaouLocation)
                                .title("Χαριλάου")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Κάτω Τούμπα")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val katotoumpaLocation = LatLng(40.6110, 22.9657)
                        map.addMarker(
                            MarkerOptions()
                                .position(katotoumpaLocation)
                                .title("Κάτω Τούμπα")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Κέντρο Πόλης")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val kentropolisLocation = LatLng(40.6325, 22.9407)
                        map.addMarker(
                            MarkerOptions()
                                .position(kentropolisLocation)
                                .title("Κέντρο Πόλης")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Νέα Παραλία")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val neaparaliaLocation = LatLng(40.6149, 22.9518)
                        map.addMarker(
                            MarkerOptions()
                                .position(neaparaliaLocation)
                                .title("Νέα Παραλία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Ντεπώ")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val ntepoLocation = LatLng(40.5935, 22.9593)
                        map.addMarker(
                            MarkerOptions()
                                .position(ntepoLocation)
                                .title("Ντεπώ")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Παναγία Φανερωμένη")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val panagiaLocation = LatLng(40.6467, 22.9383)
                        map.addMarker(
                            MarkerOptions()
                                .position(panagiaLocation)
                                .title("Παναγίας Φανερωμένης")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Πλατεία Δημοκρατίας")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val plateiaLocation = LatLng(40.6407, 22.9347)
                        map.addMarker(
                            MarkerOptions()
                                .position(plateiaLocation)
                                .title("Πλατεία Δημοκρατίας")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Σφαγεία")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val sfageiaLocation = LatLng(40.6418, 22.9105)
                        map.addMarker(
                            MarkerOptions()
                                .position(sfageiaLocation)
                                .title("Σφαγεία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Σχολή Τυφλών")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val sholiLocation = LatLng(40.6136, 22.9536)
                        map.addMarker(
                            MarkerOptions()
                                .position(sholiLocation)
                                .title("Σχολή Τυφλών")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Τριανδρία")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val triandriaLocation = LatLng(40.6240, 22.9734)
                        map.addMarker(
                            MarkerOptions()
                                .position(triandriaLocation)
                                .title("Τριανδρία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                    }
                }

                location = jsonObject.getJSONArray("Ξηροκρήνη")
                for (i in 0 until location.length()) {
                    val locationData = location.getJSONObject(i)
                    if (locationData.getString("Year") == selectedYear.toString() && locationData.getString("Month") == targetMonth) {
                        val tholotita = locationData.getString("Θολότητα NTU")
                        val hroma = locationData.getString("Χρώμα")
                        val argilio = locationData.getString("Αργίλιο")
                        val hloriouha = locationData.getString("Χλωριούχα")
                        val agogimotita = locationData.getString("Αγωγιμότητα")
                        val sygkentrosi = locationData.getString("Συγκέντρωση ιόντων υδρογόνου")
                        val ypoleimmatiko = locationData.getString("Υπολειμματικό χλώριο")
                        val snippet = "Θολότητα NTU: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        val ksirokriniLocation = LatLng(40.6484, 22.9285)
                        map.addMarker(
                            MarkerOptions()
                                .position(ksirokriniLocation)
                                .title("Ξηροκρήνη")
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

    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val window: View = layoutInflater.inflate(com.example.thessense.R.layout.custom_info_window, null)

        override fun getInfoWindow(marker: Marker): View? {
            render(marker, window)
            return window
        }

        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        private fun render(marker: Marker, view: View) {
            val titleTextView = view.findViewById<TextView>(com.example.thessense.R.id.title)
            val snippetTextView = view.findViewById<TextView>(com.example.thessense.R.id.snippet)

            titleTextView.text = marker.title
            snippetTextView.text = marker.snippet
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}