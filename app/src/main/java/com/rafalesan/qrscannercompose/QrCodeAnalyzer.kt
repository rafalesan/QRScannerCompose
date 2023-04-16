package com.rafalesan.qrscannercompose

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@ExperimentalGetImage
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit,
): ImageAnalysis.Analyzer {

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(scannerOptions)

    override fun analyze(image: ImageProxy) {
        image.image ?: return image.close()

        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    it.exception?.printStackTrace()
                    image.close()
                    return@addOnCompleteListener
                }

                it.result.let { barcodes ->
                    barcodes.forEach { barcode ->
                        barcode.rawValue?.let(onQrCodeDetected)
                    }
                }

                image.close()
            }
    }

}