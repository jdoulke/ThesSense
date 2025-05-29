package com.example.thessense.ui.watercompare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.thessense.R
import com.example.thessense.databinding.FragmentWaterBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class WaterCompareFragment : Fragment() {

    private var _binding: FragmentWaterBinding? = null
    private val binding get() = _binding!!

    // State variables
    private var selectedArea: String = ""
    private var selectedElement: String = ""
    private var selectedYear1: Int = 2023
    private var selectedMonth1: Int = 1
    private var selectedYear2: Int = 2024
    private var selectedMonth2: Int = 1

    private lateinit var waterData: JSONObject

    // Greek months (label & file search)
    private val monthNames = arrayOf(
        "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
        "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος",
        "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
    )

    // Τα στοιχεία που συγκρίνεις (μπορείς να προσθέσεις ό,τι άλλο θες)
    private val elementNames = arrayOf(
        "Θολότητα NTU", "Αργίλιο", "Αγωγιμότητα", "Υπολειμματικό χλώριο"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaterBinding.inflate(inflater, container, false)
        val root = binding.root

        // 1. Load JSON data
        val jsonStr = loadJSONFromAsset("water_data.json")
        waterData = JSONObject(jsonStr)

        // 2. Περιοχές από το JSON
        val areaList = waterData.keys().asSequence().toList().sorted()
        val areaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, areaList)
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.areaSpinner.adapter = areaAdapter

        // 3. Στοιχεία για επιλογή (το elementSpinner)
        val elementAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, elementNames)
        elementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.elementSpinner.adapter = elementAdapter

        // 4. Defaults
        selectedArea = areaList.first()
        selectedElement = elementNames.first()

        // 5. Listeners για spinners
        binding.areaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedArea = areaList[position]
                updateComparison()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        binding.elementSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedElement = elementNames[position]
                updateComparison()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 6. Year & Month pickers (4 pickers συνολικά)
        binding.year1Picker.minValue = 2017
        binding.year1Picker.maxValue = 2024
        binding.year1Picker.value = selectedYear1
        binding.month1Picker.minValue = 1
        binding.month1Picker.maxValue = 12
        binding.month1Picker.displayedValues = monthNames
        binding.month1Picker.value = selectedMonth1

        binding.year2Picker.minValue = 2017
        binding.year2Picker.maxValue = 2024
        binding.year2Picker.value = selectedYear2
        binding.month2Picker.minValue = 1
        binding.month2Picker.maxValue = 12
        binding.month2Picker.displayedValues = monthNames
        binding.month2Picker.value = selectedMonth2

        // Listeners στα pickers
        binding.year1Picker.setOnValueChangedListener { _, _, newVal ->
            selectedYear1 = newVal
            updateComparison()
        }
        binding.month1Picker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth1 = newVal
            updateComparison()
        }
        binding.year2Picker.setOnValueChangedListener { _, _, newVal ->
            selectedYear2 = newVal
            updateComparison()
        }
        binding.month2Picker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth2 = newVal
            updateComparison()
        }

        // First update
        updateComparison()

        return root
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

    private fun updateComparison() {
        try {
            // Απόκτησε τα δεδομένα για την περιοχή
            val areaArray = waterData.optJSONArray(selectedArea) ?: return

            // Month string (π.χ. "Ιανουάριος")
            val monthStr1 = monthNames[selectedMonth1 - 1]
            val monthStr2 = monthNames[selectedMonth2 - 1]

            // Βρες το αντικείμενο του κάθε μήνα-έτους
            val obj1 = findEntry(areaArray, selectedYear1, monthStr1)
            val obj2 = findEntry(areaArray, selectedYear2, monthStr2)
            if (obj1 == null || obj2 == null) return

            val value1 = parseNumber(obj1.optString(selectedElement))
            val value2 = parseNumber(obj2.optString(selectedElement))

            // Chart entries (μία μπάρα ανά περίοδο)
            val entries1 = listOf(BarEntry(0f, value1))
            val entries2 = listOf(BarEntry(1f, value2))

            val dataSet1 = BarDataSet(entries1, "${selectedYear1}/${selectedMonth1}").apply {
                color = ContextCompat.getColor(requireContext(), R.color.green)
            }
            val dataSet2 = BarDataSet(entries2, "${selectedYear2}/${selectedMonth2}").apply {
                color = ContextCompat.getColor(requireContext(), R.color.petrol)
            }

            val barData = BarData(dataSet1, dataSet2)
            barData.barWidth = 0.35f

            val chart = binding.comparisonChart
            chart.data = barData

            chart.xAxis.apply {
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(arrayOf("Περίοδος 1", "Περίοδος 2"))
                labelRotationAngle = 0f
                axisMinimum = -0.5f
                axisMaximum = 1.5f
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.blue)
                axisLineWidth = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.blue)
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_medium)
            }

            chart.axisLeft.apply {
                axisMinimum = 0f
                axisLineWidth = 1f
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.blue)
                textColor = ContextCompat.getColor(requireContext(), R.color.blue)
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_medium)
                gridColor = ContextCompat.getColor(requireContext(), R.color.blue)
                gridLineWidth = 0.5f
            }

            chart.axisRight.isEnabled = false
            chart.description.isEnabled = false
            chart.legend.isEnabled = true

            // Formatter
            val floatFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.2f", value)
                }
            }
            dataSet1.valueTextColor = ContextCompat.getColor(requireContext(), R.color.blue)
            dataSet1.valueTextSize = 10f
            dataSet1.valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_light)
            dataSet1.valueFormatter = floatFormatter

            dataSet2.valueTextColor = ContextCompat.getColor(requireContext(), R.color.blue)
            dataSet2.valueTextSize = 10f
            dataSet2.valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_light)
            dataSet2.valueFormatter = floatFormatter

            chart.invalidate()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findEntry(array: JSONArray, year: Int, month: String): JSONObject? {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            if (obj != null && obj.optString("Year") == year.toString() && obj.optString("Month") == month) {
                return obj
            }
        }
        return null
    }

    private fun parseNumber(str: String?): Float {
        if (str.isNullOrBlank()) return 0f
        // Αφαίρεση <, ΔΠ, κενά, σύμβολα, , --> .
        val clean = str.replace(Regex("[^0-9,\\.]"), "").replace(",", ".")
        return clean.toFloatOrNull() ?: 0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
