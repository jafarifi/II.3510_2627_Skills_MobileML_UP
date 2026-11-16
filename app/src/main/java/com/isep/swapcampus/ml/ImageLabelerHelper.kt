package com.isep.swapcampus.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.isep.shared.domain.DetectedLabel

object ImageLabelerHelper {

    fun labelImage(
        context: Context,
        uri: Uri,
        onResult: (List<DetectedLabel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onError(e)
            return
        }

        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                // TODO 2: convert each ImageLabel into DetectedLabel(text, confidence).
                onResult(emptyList())
            }
            .addOnFailureListener { e ->
                onError(e)
            }
            .addOnCompleteListener {
                labeler.close()
            }
    }
}
