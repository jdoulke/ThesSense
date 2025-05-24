package com.example.thessense.ui.aircompare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AirViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is air Fragment"
    }
    val text: LiveData<String> = _text
}