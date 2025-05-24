package com.example.thessense.ui.aircompare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.thessense.R
import com.example.thessense.databinding.FragmentAirBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONObject
import java.io.InputStream

class AirFragment : Fragment() {

    private var _binding: FragmentAirBinding? = null
    private val binding get() = _binding!!

    private var selectedYear1: Int = 2023
    private var selectedMonth1: Int = 1
    private var selectedYear2: Int = 2024
    private var selectedMonth2: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAirBinding.inflate(inflater, container, false)
        val root = binding.root

        val year1Picker = binding.year1Picker
        val month1Picker = binding.month1Picker
        val year2Picker = binding.year2Picker
        val month2Picker = binding.month2Picker

        year1Picker.minValue = 2017
        year1Picker.maxValue = 2024
        year1Picker.value = selectedYear1
        year2Picker.minValue = 2017
        year2Picker.maxValue = 2024
        year2Picker.value = selectedYear2

        month1Picker.minValue = 1
        month1Picker.maxValue = 12
        month1Picker.displayedValues = arrayOf(
            "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
            "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος",
            "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
        )
        month1Picker.value = selectedMonth1
        month2Picker.minValue = 1
        month2Picker.maxValue = 12
        month2Picker.displayedValues = arrayOf(
            "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
            "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος",
            "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
        )
        month2Picker.value = selectedMonth2

        updateComparison()

        year1Picker.setOnValueChangedListener { _, _, newVal ->
            selectedYear1 = newVal
            updateComparison()
        }

        month1Picker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth1 = newVal
            updateComparison()
        }

        year2Picker.setOnValueChangedListener { _, _, newVal ->
            selectedYear2 = newVal
            updateComparison()
        }

        month2Picker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth2 = newVal
            updateComparison()
        }

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
        val jsonStr = loadJSONFromAsset("air_data.json")
        if (jsonStr.isBlank()) return

        val data = JSONObject(jsonStr)
        val year1Array = data.optJSONArray(selectedYear1.toString()) ?: return
        val month1Data = year1Array.optJSONObject(selectedMonth1 - 1) ?: return
        val year2Array = data.optJSONArray(selectedYear2.toString()) ?: return
        val month2Data = year2Array.optJSONObject(selectedMonth2 - 1) ?: return

        val co_1 = month1Data.optDouble("CO AVERAGE")
        val no_1 = month1Data.optDouble("NO AVERAGE")
        val no2_1 = month1Data.optDouble("NO2 AVERAGE")
        val so2_1 = month1Data.optDouble("SO2 AVERAGE")
        val o3_1 = month1Data.optDouble("O3 AVERAGE")
        val co_2 = month2Data.optDouble("CO AVERAGE")
        val no_2 = month2Data.optDouble("NO AVERAGE")
        val no2_2 = month2Data.optDouble("NO2 AVERAGE")
        val so2_2 = month2Data.optDouble("SO2 AVERAGE")
        val o3_2 = month2Data.optDouble("O3 AVERAGE")

        val pollutants = listOf("CO AVERAGE", "NO AVERAGE", "NO2 AVERAGE", "SO2 AVERAGE", "O3 AVERAGE")
        val pollutantsAxis = listOf("CO", "NO", "NO2", "SO2", "O3")

        val values1 = pollutants.map { month1Data.optDouble(it, 0.0).toFloat() }
        val values2 = pollutants.map { month2Data.optDouble(it, 0.0).toFloat() }

        val entries1 = ArrayList<BarEntry>()
        val entries2 = ArrayList<BarEntry>()

        for (i in pollutants.indices) {
            entries1.add(BarEntry(i.toFloat(), values1[i]))
            entries2.add(BarEntry(i.toFloat(), values2[i]))
        }

        val dataSet1 = BarDataSet(entries1, "$selectedYear1/${selectedMonth1}").apply {
            color = ContextCompat.getColor(requireContext(), R.color.green)
        }

        val dataSet2 = BarDataSet(entries2, "$selectedYear2/${selectedMonth2}").apply {
            color = ContextCompat.getColor(requireContext(), R.color.petrol)
        }

        val groupSpace = 0.2f
        val barSpace = 0.05f
        val barWidth = 0.35f

        val barData = BarData(dataSet1, dataSet2)
        barData.barWidth = barWidth

        val chart = binding.comparisonChart
        chart.data = barData

        val groupCount = pollutantsAxis.size
        val groupWidth = barData.getGroupWidth(groupSpace, barSpace)

        chart.xAxis.apply {
            granularity = 1f
            isGranularityEnabled = true
            setCenterAxisLabels(true)
            setDrawGridLines(false)
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(pollutantsAxis)
            labelRotationAngle = -30f
            axisMinimum = 0f
            axisMaximum = 0f + groupWidth * groupCount
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
        chart.groupBars(0f, groupSpace, barSpace)

        val floatFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1f", value)
            }
        }

        dataSet1.apply {
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.blue)
            valueTextSize = 10f
            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_light)
            valueFormatter = floatFormatter
        }

        dataSet2.apply {
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.blue)
            valueTextSize = 10f
            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lexend_light)
            valueFormatter = floatFormatter
        }

        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}