package com.scanner.app.data

object NativeLib {
    init { System.loadLibrary("product_lib") }
    @JvmStatic external fun lookupProduct(barcode: String, dbPath: String): Int
}
