package com.scanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbFile = File(filesDir, "products.bin")
        if (!dbFile.exists()) {
            assets.open("products.bin").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        val dbPath = dbFile.absolutePath

        setContent {
            MaterialTheme {
                Surface {
                    ScannerScreen(dbPath)
                }
            }
        }
    }
}

object NativeLib {
    init {
        System.loadLibrary("product_lib")
    }

    @JvmStatic
    external fun lookupProduct(barcode: String, dbPath: String): Int
}

@Stable
data class ProductFlags(val raw: Int) {
    val rating: Int get() = (raw shr 24) and 0xFF
    val sugar: Boolean    get() = (raw and (1 shl 0)) != 0
    val gluten: Boolean   get() = (raw and (1 shl 1)) != 0
    val lactose: Boolean  get() = (raw and (1 shl 2)) != 0
    val palmOil: Boolean  get() = (raw and (1 shl 3)) != 0
    val hazardousE: Boolean get() = (raw and (1 shl 4)) != 0
}

@Composable
fun ScannerScreen(dbPath: String) {
    var barcodeInput by remember { mutableStateOf("") }
    var productFlags by remember { mutableStateOf<ProductFlags?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = barcodeInput,
            onValueChange = { barcodeInput = it },
            label = { Text("Штрих-код") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val code = barcodeInput.trim()
                if (code.isNotEmpty()) {
                    scanning = true
                    productFlags = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val packed = NativeLib.lookupProduct(code, dbPath)
                            if (isActive) {
                                productFlags = if (packed != 0) ProductFlags(packed) else null
                            }
                        } catch (e: CancellationException) {
                            // отмена — нормально
                        } finally {
                            scanning = false
                        }
                    }
                }
            },
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (scanning) "Поиск..." else "Проверить продукт")
        }

        Spacer(modifier = Modifier.height(16.dp))

        productFlags?.let { flags ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Рейтинг: ${flags.rating}/100", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlagRow("Сахар", flags.sugar)
                    FlagRow("Глютен", flags.gluten)
                    FlagRow("Лактоза", flags.lactose)
                    FlagRow("Пальмовое масло", flags.palmOil)
                    FlagRow("Опасные E-добавки", flags.hazardousE)
                }
            }
        }
    }
}

@Composable
fun FlagRow(name: String, flagged: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val color = if (flagged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Text(text = "• $name", color = color, modifier = Modifier.padding(vertical = 2.dp))
    }
}
