package com.xoykor.qr.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.scanner.QrAnalyzer
import com.xoykor.qr.ui.components.QrResultCard
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    onResultCaptured: (QrScanResult) -> Unit,
    onSwitchToImageTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestedOnce = true
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionFallback(
            showSettingsHint = permissionRequestedOnce,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            onSwitchToImageTab = onSwitchToImageTab,
            modifier = modifier
        )
        return
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var activeResult by remember { mutableStateOf<QrScanResult?>(null) }
    var detectedBoxes by remember { mutableStateOf<List<QrScanResult>>(emptyList()) }
    var frameWidth by remember { mutableIntStateOf(1) }
    var frameHeight by remember { mutableIntStateOf(1) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        QrAnalyzer(
            isFrontCamera = { lensFacing == CameraSelector.LENS_FACING_FRONT },
            isPaused = { isPaused || activeResult != null },
            onQrDetected = { results, uprightW, uprightH ->
                ContextCompat.getMainExecutor(context).execute {
                    frameWidth = uprightW.coerceAtLeast(1)
                    frameHeight = uprightH.coerceAtLeast(1)
                    detectedBoxes = results
                    if (results.isNotEmpty() && activeResult == null && !isPaused) {
                        val first = results.first()
                        activeResult = first
                        isPaused = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResultCaptured(first)
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            analyzerExecutor.shutdown()
        }
    }

    LaunchedEffect(lensFacing) {
        torchEnabled = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(analyzerExecutor, analyzer)
                }

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
                boundCamera = camera
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()
            } catch (_: Exception) {
                boundCamera = null
                hasFlashUnit = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(torchEnabled)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder reticle & live QR bounding boxes
        CameraOverlayCanvas(
            results = detectedBoxes,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            accentColor = MaterialTheme.colorScheme.primary
        )

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (activeResult != null) "QR identificado" else if (isPaused) "Pausado" else "Aponte para o QR code",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasFlashUnit && lensFacing == CameraSelector.LENS_FACING_BACK) {
                    FilledIconButton(
                        onClick = { torchEnabled = !torchEnabled },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (torchEnabled) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = "Alternar lanterna"
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        if (activeResult != null) {
                            activeResult = null
                            detectedBoxes = emptyList()
                            isPaused = false
                        } else {
                            isPaused = !isPaused
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (isPaused) "Retomar leitura" else "Pausar leitura"
                    )
                }

                FilledIconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cameraswitch,
                        contentDescription = "Alternar câmera"
                    )
                }
            }
        }

        // Bottom result sheet when a QR code is detected
        AnimatedVisibility(
            visible = activeResult != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            activeResult?.let { result ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    QrResultCard(
                        result = result,
                        onRescanClick = {
                            activeResult = null
                            detectedBoxes = emptyList()
                            isPaused = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraOverlayCanvas(
    results: List<QrScanResult>,
    frameWidth: Int,
    frameHeight: Int,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val viewW = size.width
        val viewH = size.height

        // Subtle dark vignette outside the central scan guide when idle
        val guideSize = minOf(viewW, viewH) * 0.64f
        val left = (viewW - guideSize) / 2f
        val top = (viewH - guideSize) / 2f
        val right = left + guideSize
        val bottom = top + guideSize
        val cornerLen = 32.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()

        val bracketColor = if (results.isNotEmpty()) accentColor else Color.White.copy(alpha = 0.85f)
        val path = Path().apply {
            // Top-left
            moveTo(left, top + cornerLen)
            lineTo(left, top)
            lineTo(left + cornerLen, top)
            // Top-right
            moveTo(right - cornerLen, top)
            lineTo(right, top)
            lineTo(right, top + cornerLen)
            // Bottom-right
            moveTo(right, bottom - cornerLen)
            lineTo(right, bottom)
            lineTo(right - cornerLen, bottom)
            // Bottom-left
            moveTo(left + cornerLen, bottom)
            lineTo(left, bottom)
            lineTo(left, bottom - cornerLen)
        }
        drawPath(
            path = path,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw detected QR bounding boxes mapped from FILL_CENTER camera coordinates
        val fw = frameWidth.toFloat().coerceAtLeast(1f)
        val fh = frameHeight.toFloat().coerceAtLeast(1f)
        val scale = maxOf(viewW / fw, viewH / fh)
        val scaledW = fw * scale
        val scaledH = fh * scale
        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        results.forEach { res ->
            val box = res.normalizedBox ?: return@forEach
            val rect = box.boundingBox
            val rLeft = dx + rect.left * scaledW
            val rTop = dy + rect.top * scaledH
            val rWidth = (rect.right - rect.left) * scaledW
            val rHeight = (rect.bottom - rect.top) * scaledH
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(rLeft, rTop),
                size = androidx.compose.ui.geometry.Size(rWidth, rHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    showSettingsHint: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToImageTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Permissão da câmera necessária",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para identificar QR codes ao vivo pela câmera, autorize o acesso à câmera ou use a aba Imagem para ler a partir de fotos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Permitir câmera")
        }
        if (showSettingsHint) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenSettings) {
                Text("Abrir configurações do app")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onSwitchToImageTab) {
            Icon(
                imageVector = Icons.Rounded.ImageSearch,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Ler QR de uma imagem")
        }
    }
}
