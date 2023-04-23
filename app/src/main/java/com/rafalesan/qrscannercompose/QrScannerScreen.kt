package com.rafalesan.qrscannercompose

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@ExperimentalGetImage
@Composable
fun QrScannerScreen() {
    val context = LocalContext.current

    var isCameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            isCameraPermissionGranted = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!isCameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (isCameraPermissionGranted) {
        ScannerContent()
    } else {
        RequestCameraPermissionButton(
            cameraPermissionLauncher
        )
    }

}

@ExperimentalGetImage
@Composable
fun ScannerContent() {

    var barcodeArea by remember {
        mutableStateOf(
            Rect.Zero
        )
    }

    var barcodeValue by remember {
        mutableStateOf("")
    }

    CameraContent(
        onUpdateBarcodeArea = { rect ->
            barcodeArea = rect
        },
        onUpdateBarcodeValue = { barcode ->
            barcodeValue = barcode
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            drawRect(
                color = Color.Red,
                topLeft = barcodeArea.topLeft,
                size = barcodeArea.size,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        if (barcodeValue.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                text = barcodeValue,
                color = Color.White
            )
        }

    }

}

@ExperimentalGetImage
@Composable
fun CameraContent(
    onUpdateBarcodeArea: (Rect) -> Unit,
    onUpdateBarcodeValue: (String) -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { androidViewContext ->
                val previewView = PreviewView(androidViewContext).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewView
            },
            update = { previewView ->

                val targetResolutionSize = Size(previewView.width, previewView.height)

                val preview = Preview.Builder()
                    .setTargetResolution(targetResolutionSize)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(targetResolutionSize)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    QrCodeAnalyzer(previewView) { barcode, barcodeRect ->
                        onUpdateBarcodeArea(barcodeRect)
                        onUpdateBarcodeValue(barcode)
                    }
                )

                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                try {
                    val processCameraProvider = cameraProviderFuture.get()
                    processCameraProvider.unbindAll()
                    processCameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        )
    }
}

@Composable
fun RequestCameraPermissionButton(
    cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
            Text(text = stringResource(R.string.request_camera_permission))
        }
    }
}