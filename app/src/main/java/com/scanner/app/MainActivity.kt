package com.scanner.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanner.app.data.NativeLib
import com.scanner.app.data.ProductFlags
import com.scanner.app.ui.product.ProductCard
import com.scanner.app.ui.scanner.CameraScreen
import com.scanner.app.ui.scanner.CameraViewModel
import com.scanner.app.ui.theme.AppTheme
import java.io.File
import java.nio.ByteBuffer
import java.util.HashMap

class MainActivity : ComponentActivity() {
    private val productNames = HashMap<Long, String>()

    private fun loadNames() {
        try {
            val nameFile = File(filesDir, "product_names.bin")
            if (!nameFile.exists()) {
                assets.open("product_names.bin").use { input ->
                    nameFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val bytes = nameFile.readBytes()
            val buf = ByteBuffer.wrap(bytes)
            while (buf.remaining() >= 108) {
                val barcode = buf.getLong()
                val nameBytes = ByteArray(100)
                buf.get(nameBytes)
                val name = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000')
                productNames[barcode] = name
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки названий", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbFile = File(filesDir, "products.bin")
        try {
            if (!dbFile.exists()) {
                assets.open("products.bin").use { input ->
                    dbFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки базы данных", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val dbPath = dbFile.absolutePath

        loadNames()

        setContent {
            AppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(dbPath = dbPath, onOpenScanner = { navController.navigate("scanner") })
                    }
                    composable("scanner") {
                        val cameraViewModel: CameraViewModel = viewModel()
                        val barcode by cameraViewModel.barcode.collectAsState()
                        LaunchedEffect(barcode) {
                            barcode?.let { code ->
                                val packed = NativeLib.lookupProduct(code, dbPath)
                                if (packed != -1) {
                                    navController.navigate("product/${packed}/${code}")
                                }
                                cameraViewModel.resetBarcode()
                            }
                        }
                        CameraScreen(viewModel = cameraViewModel, onBack = { navController.popBackStack() })
                    }
                    composable("product/{raw}/{barcode}") { backStackEntry ->
                        val raw = backStackEntry.arguments?.getString("raw")?.toIntOrNull() ?: 0
                        val barcode = backStackEntry.arguments?.getString("barcode")?.toLongOrNull() ?: 0L
                        val flags = ProductFlags(raw)
                        val name = productNames[barcode] ?: "Неизвестный продукт"
                        Scaffold(
                            bottomBar = {
                                Button(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text("← Назад к сканеру")
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    ProductCard(flags = flags, productName = name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(dbPath: String, onOpenScanner: () -> Unit) {
    var barcodeInput by remember { mutableStateOf("") }
    var productFlags by remember { mutableStateOf<ProductFlags?>(null) }
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Button(onClick = onOpenScanner, modifier = Modifier.fillMaxWidth()) {
            Text("📷 Открыть сканер")
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = barcodeInput,
            onValueChange = { barcodeInput = it },
            label = { Text("Введите штрих-код") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val code = barcodeInput.trim()
                if (code.isNotEmpty()) {
                    val packed = NativeLib.lookupProduct(code, dbPath)
                    productFlags = if (packed != -1) ProductFlags(packed) else null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить продукт")
        }

        Spacer(Modifier.height(16.dp))

        if (productFlags != null) {
            ProductCard(flags = productFlags!!, productName = "Введённый товар")
        }
    }
}
