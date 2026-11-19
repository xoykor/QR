package com.xoykor.qr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.IntentCompat
import com.xoykor.qr.data.ScanHistoryRepository
import com.xoykor.qr.ui.screens.CameraScannerScreen
import com.xoykor.qr.ui.screens.HistoryScreen
import com.xoykor.qr.ui.screens.ImageScannerScreen
import com.xoykor.qr.ui.theme.LeitorQrTheme

enum class AppTab(val label: String) {
    CAMERA("Câmera"),
    IMAGE("Imagem"),
    HISTORY("Histórico")
}

class MainActivity : ComponentActivity() {

    private var incomingImageUri by mutableStateOf<Uri?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        val historyRepo = ScanHistoryRepository(applicationContext)

        setContent {
            LeitorQrTheme {
                var currentTab by remember {
                    mutableStateOf(if (incomingImageUri != null) AppTab.IMAGE else AppTab.CAMERA)
                }
                var history by remember { mutableStateOf(historyRepo.loadHistory()) }

                // Automatically switch to Image tab if a shared image arrives while app is open
                val pendingUri = incomingImageUri
                if (pendingUri != null && currentTab != AppTab.IMAGE) {
                    currentTab = AppTab.IMAGE
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Leitor QR",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == AppTab.CAMERA,
                                onClick = { currentTab = AppTab.CAMERA },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = AppTab.CAMERA.label
                                    )
                                },
                                label = { Text(AppTab.CAMERA.label) }
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.IMAGE,
                                onClick = { currentTab = AppTab.IMAGE },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ImageSearch,
                                        contentDescription = AppTab.IMAGE.label
                                    )
                                },
                                label = { Text(AppTab.IMAGE.label) }
                            )
                            NavigationBarItem(
                                selected = currentTab == AppTab.HISTORY,
                                onClick = { currentTab = AppTab.HISTORY },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = AppTab.HISTORY.label
                                    )
                                },
                                label = { Text(AppTab.HISTORY.label) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.CAMERA -> CameraScannerScreen(
                                onResultCaptured = { result ->
                                    history = historyRepo.addScan(result)
                                },
                                onSwitchToImageTab = { currentTab = AppTab.IMAGE }
                            )
                            AppTab.IMAGE -> ImageScannerScreen(
                                pendingImageUri = incomingImageUri,
                                onPendingImageConsumed = { incomingImageUri = null },
                                onResultsCaptured = { results ->
                                    results.forEach { res ->
                                        history = historyRepo.addScan(res)
                                    }
                                }
                            )
                            AppTab.HISTORY -> HistoryScreen(
                                items = history,
                                onDeleteItem = { id ->
                                    history = historyRepo.deleteScan(id)
                                },
                                onClearAll = {
                                    history = historyRepo.clearHistory()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        if (uri != null) {
            incomingImageUri = uri
        }
    }
}
