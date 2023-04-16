package com.rafalesan.qrscannercompose

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
        return
    } else {
        RequestCameraPermissionButton(
            cameraPermissionLauncher
        )
    }

}

@ExperimentalGetImage
@Composable
fun ScannerContent() {

    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val previewView = PreviewView(context)
            val preview = Preview.Builder().build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(
                    Size(previewView.width, previewView.height)
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                QrCodeAnalyzer { result ->
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                }
            )

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

            previewView
        }
    )

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