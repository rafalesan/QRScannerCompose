package com.rafalesan.qrscannercompose

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.toComposeRect
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.math.max
import kotlin.math.min

@ExperimentalGetImage
class QrCodeAnalyzer(
    private val previewView: PreviewView,
    private val onQrCodeDetected: (String, ComposeRect) -> Unit
): ImageAnalysis.Analyzer {

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(scannerOptions)

    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f

    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image ?: return imageProxy.close()

        val previewViewWidth = previewView.width.toFloat()
        val previewViewHeight = previewView.height.toFloat()

        val imageWidth = image.width.toFloat()
        val imageHeight = image.height.toFloat()

        val previewAspectRatio = previewViewWidth / previewViewHeight
        val imageAspectRatio = imageWidth / imageHeight

        if(previewAspectRatio > imageAspectRatio) {
            scaleFactor = previewViewWidth / imageWidth
            postScaleHeightOffset = (previewViewWidth / imageAspectRatio - previewViewHeight) / 2
        } else {
            scaleFactor = previewViewHeight / imageHeight
            postScaleWidthOffset = (previewViewHeight * imageAspectRatio - previewViewWidth) / 2
        }

        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    it.exception?.printStackTrace()
                    image.close()
                    return@addOnCompleteListener
                }

                it.result.let { barcodes ->
                    if (barcodes.isEmpty()) {
                        onQrCodeDetected("", ComposeRect.Zero)
                    }
                    barcodes.forEach { barcode ->
                        val rawValue = barcode.rawValue ?: return@addOnCompleteListener
                        val boundingBox = barcode.boundingBox ?: return@addOnCompleteListener
                        val rect = boundingBox.toRectF()
                        val x0 = translateX(rect.left)
                        val x1 = translateX(rect.right)
                        rect.left = min(x0, x1)
                        rect.right = max(x0, x1)
                        rect.top = translateY(rect.top)
                        rect.bottom = translateY(rect.bottom)
                        onQrCodeDetected(rawValue, rect.toRect().toComposeRect())
                    }
                }

                imageProxy.close()
            }
    }

    private fun translateX(x: Float): Float {
        return scale(x) - postScaleWidthOffset
    }

    private fun translateY(y: Float): Float {
        return scale(y) - postScaleHeightOffset
    }

    private fun scale(imagePixel: Float): Float {
        return imagePixel * scaleFactor
    }

}