package com.scanner.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
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
                                if (packed != -1) navController.navigate("product/${packed}")
                                cameraViewModel.resetBarcode()
                            }
                        }
                        CameraScreen(viewModel = cameraViewModel, onBack = { navController.popBackStack() })
                    }
                    composable("product/{raw}") { backStackEntry ->
                        val raw = backStackEntry.arguments?.getString("raw")?.toIntOrNull() ?: 0
                        val flags = ProductFlags(raw)
                        ProductCard(flags = flags)
                        Button(onClick = { navController.popBackStack() }) {
                            Text("← Назад")
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
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
            ProductCard(flags = productFlags!!)
        }
    }
}
