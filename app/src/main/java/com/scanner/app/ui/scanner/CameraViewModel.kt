package com.scanner.app.ui.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel : ViewModel() {
    private val _barcode = MutableStateFlow<String?>(null)
    val barcode: StateFlow<String?> = _barcode

    fun onBarcodeDetected(value: String) { _barcode.value = value }
    fun resetBarcode() { _barcode.value = null }
}
