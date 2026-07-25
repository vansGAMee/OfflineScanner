package com.scanner.app.data

@androidx.compose.runtime.Stable
data class ProductFlags(val raw: Int) {
    val rating: Int get() = (raw shr 24) and 0xFF
    val sugar: Boolean get() = (raw and (1 shl 0)) != 0
    val gluten: Boolean get() = (raw and (1 shl 1)) != 0
    val lactose: Boolean get() = (raw and (1 shl 2)) != 0
    val palmOil: Boolean get() = (raw and (1 shl 3)) != 0
    val hazardousE: Boolean get() = (raw and (1 shl 4)) != 0
    val gmo: Boolean get() = (raw and (1 shl 5)) != 0
    val milkFatReplacer: Boolean get() = (raw and (1 shl 6)) != 0
    val artificialColors: Boolean get() = (raw and (1 shl 7)) != 0
    val sugarLevel: Int get() = (raw shr 8) and 0xF
    val saltLevel: Int get() = (raw shr 12) and 0xF
    val fatLevel: Int get() = (raw shr 16) and 0xF
}
