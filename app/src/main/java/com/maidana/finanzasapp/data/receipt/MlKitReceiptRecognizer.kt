package com.maidana.finanzasapp.data.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.maidana.finanzasapp.domain.ReceiptDraft
import com.maidana.finanzasapp.domain.ReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface ReceiptRecognizer {
    suspend fun recognize(uri: Uri): ReceiptDraft
}

class MlKitReceiptRecognizer(private val context: Context) : ReceiptRecognizer {
    override suspend fun recognize(uri: Uri): ReceiptDraft = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val recognized = recognizer.process(image).await()
            ReceiptParser.parse(recognized.text)
        } finally { recognizer.close() }
    }
}
