package com.isep.swapcampus.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

object BarcodeHelper {
    fun scanImage(
        context: Context,
        uri: Uri,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onError(e)
            return
        }
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // TODO 3: extract rawValue, ignore null/blank values and remove duplicates.
                onResult(emptyList())
            }
            .addOnFailureListener { e -> onError(e) }
            .addOnCompleteListener { scanner.close() }
    }
}
