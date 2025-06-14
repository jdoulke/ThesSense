package com.example.thessense.ui.gallery

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject
import java.io.InputStream
import android.util.Log
import com.example.thessense.R
import java.util.Locale

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

        val currentLang = Locale.getDefault().language
        val months = if (currentLang == "el") {
            resources.getStringArray(R.array.months_gr)
        } else {
            resources.getStringArray(R.array.months_en)
        }
        monthPicker.minValue = 1
        monthPicker.maxValue = months.size
        monthPicker.displayedValues = null
        monthPicker.displayedValues = months

        monthPicker.value = selectedMonth

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

    private fun parseValue(input: String): Float {
        val clean = input.replace(",", ".")
            .replace(Regex("[^0-9.]"), "")
        return clean.toFloatOrNull() ?: 0f
    }

    private fun calculateWQI(tholotita: Float, agogimotita: Float, sygkentrosi: Float, ypoleimmatiko: Float): Int {
        var score = 0f
        var totalWeight = 0f

        val tholotitaScore = when {
            tholotita <= 0.25 -> 100f
            tholotita <= 0.5 -> 75f
            tholotita <= 1 -> 50f
            else -> 25f
        }
        score += tholotitaScore * 0.3f
        totalWeight += 0.3f

        val agogimotitaScore = when {
            agogimotita <= 400 -> 100f
            agogimotita <= 800 -> 75f
            agogimotita <= 1200 -> 50f
            else -> 25f
        }
        score += agogimotitaScore * 0.25f
        totalWeight += 0.25f

        val sygkentrosiScore = if (sygkentrosi in 7.0..8.5) 100f else 50f
        score += sygkentrosiScore * 0.25f
        totalWeight += 0.25f

        val ypoleimmatikoScore = if (ypoleimmatiko in 0.3..0.5) 100f else 50f
        score += ypoleimmatikoScore * 0.2f
        totalWeight += 0.2f

        return (score / totalWeight).toInt()
    }

    private fun addMarkersFromJson() {
        googleMap?.let { map ->
            map.clear()
            try {
                val jsonData = loadJSONFromAsset()
                val jsonObject = JSONObject(jsonData)
                val currentLang = Locale.getDefault().language
                var snippet = ""

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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val ekklisiesLocation = LatLng(40.6328, 22.9677)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(ekklisiesLocation)
                                .title("40 Εκκλησίες")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val analipsiLocation = LatLng(40.6051, 22.9598)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(analipsiLocation)
                                .title("Ανάληψη")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val anopoliLocation = LatLng(40.6419, 22.9460)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(anopoliLocation)
                                .title("Άνω Πόλη")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val anotoumpaLocation = LatLng(40.6179, 22.9756)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(anotoumpaLocation)
                                .title("Άνω Τούμπα")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val dethhanthLocation = LatLng(40.6264, 22.9526)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(dethhanthLocation)
                                .title("ΔΕΘ-ΧΑΝΘ")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val harilaouLocation = LatLng(40.6001, 22.9702)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(harilaouLocation)
                                .title("Χαριλάου")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val katotoumpaLocation = LatLng(40.6110, 22.9657)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(katotoumpaLocation)
                                .title("Κάτω Τούμπα")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val kentropolisLocation = LatLng(40.6325, 22.9407)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(kentropolisLocation)
                                .title("Κέντρο Πόλης")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val neaparaliaLocation = LatLng(40.6149, 22.9518)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(neaparaliaLocation)
                                .title("Νέα Παραλία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val ntepoLocation = LatLng(40.5935, 22.9593)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(ntepoLocation)
                                .title("Ντεπώ")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val panagiaLocation = LatLng(40.6467, 22.9383)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(panagiaLocation)
                                .title("Παναγίας Φανερωμένης")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val plateiaLocation = LatLng(40.6407, 22.9347)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(plateiaLocation)
                                .title("Πλατεία Δημοκρατίας")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val sfageiaLocation = LatLng(40.6418, 22.9105)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(sfageiaLocation)
                                .title("Σφαγεία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val sholiLocation = LatLng(40.6136, 22.9536)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(sholiLocation)
                                .title("Σχολή Τυφλών")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val triandriaLocation = LatLng(40.6240, 22.9734)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(triandriaLocation)
                                .title("Τριανδρία")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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
                        snippet = if (currentLang == "el") {
                            "Θολότητα: $tholotita \nΧρώμα: $hroma \nΑργίλιο: $argilio \nΧλωριούχα: $hloriouha \nΑγωγιμότητα: $agogimotita \nΣυγκέντρωση ιόντων υδρογόνου: $sygkentrosi \nΥπολειμματικό χλώριο: $ypoleimmatiko"
                        } else {
                            "Turbidity: $tholotita \nColor: $hroma \nAluminium: $argilio \nChlorides: $hloriouha \nConductivity: $agogimotita \nHydrogen ion concentration: $sygkentrosi \nResidual Chlorine: $ypoleimmatiko"
                        }
                        val ksirokriniLocation = LatLng(40.6484, 22.9285)

                        //Υπολογισμός WQI
                        val tholotitaValue = parseValue(tholotita)
                        val agogimotitaValue = Regex("""\d+(\.\d+)?""").find(agogimotita)?.value?.toFloatOrNull() ?: 0f
                        val sygkentrosiValue = parseValue(sygkentrosi)
                        val ypoleimmatikoValue = parseValue(ypoleimmatiko)
                        val wqi = calculateWQI(tholotitaValue, agogimotitaValue, sygkentrosiValue, ypoleimmatikoValue)
                        val hue = 120f * (wqi / 100f)

                        map.addMarker(
                            MarkerOptions()
                                .position(ksirokriniLocation)
                                .title("Ξηροκρήνη")
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
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