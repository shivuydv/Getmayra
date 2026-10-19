package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Maya Vision & Barcode/QR Scanner.
 * Uses ML Kit Vision Barcode Scanning (v17.3.0) to analyze QR codes and barcodes
 * from device photos or camera captures.
 */
object MayaVisionScanner {

    data class ScanResult(
        val rawValue: String,
        val typeLabel: String,
        val formattedSummary: String,
        val actionUrl: String? = null
    )

    fun scanUri(
        context: Context,
        uri: Uri,
        onSuccess: (ScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val bitmap = loadBitmap(context, uri)
        if (bitmap == null) {
            onError("छवि लोड नहीं हो सकी। कृपया दोबारा प्रयास करें।")
            return
        }
        scanBitmap(bitmap, onSuccess, onError)
    }

    fun scanBitmap(
        bitmap: Bitmap,
        onSuccess: (ScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    onError("इस इमेज में कोई QR कोड या बारकोड नहीं मिला।")
                    return@addOnSuccessListener
                }

                val primary = barcodes[0]
                val raw = primary.rawValue ?: primary.displayValue ?: "Unknown"

                when (primary.valueType) {
                    Barcode.TYPE_URL -> {
                        val url = primary.url?.url ?: raw
                        onSuccess(
                            ScanResult(
                                rawValue = raw,
                                typeLabel = "Website Link",
                                formattedSummary = "वेबसाइट लिंक: $url",
                                actionUrl = url
                            )
                        )
                    }
                    Barcode.TYPE_WIFI -> {
                        val wifi = primary.wifi
                        val ssid = wifi?.ssid ?: "Unknown"
                        val pass = wifi?.password ?: "None"
                        onSuccess(
                            ScanResult(
                                rawValue = raw,
                                typeLabel = "WiFi Network",
                                formattedSummary = "WiFi नेटवर्क: SSID '$ssid', पासवर्ड '$pass'",
                                actionUrl = null
                            )
                        )
                    }
                    Barcode.TYPE_PHONE -> {
                        val phone = primary.phone?.number ?: raw
                        onSuccess(
                            ScanResult(
                                rawValue = raw,
                                typeLabel = "Phone Number",
                                formattedSummary = "फ़ोन नंबर: $phone",
                                actionUrl = "tel:$phone"
                            )
                        )
                    }
                    Barcode.TYPE_SMS -> {
                        val sms = primary.sms
                        onSuccess(
                            ScanResult(
                                rawValue = raw,
                                typeLabel = "SMS Message",
                                formattedSummary = "SMS: ${sms?.phoneNumber} - ${sms?.message}",
                                actionUrl = "smsto:${sms?.phoneNumber}"
                            )
                        )
                    }
                    else -> {
                        val isUrl = raw.startsWith("http://") || raw.startsWith("https://")
                        onSuccess(
                            ScanResult(
                                rawValue = raw,
                                typeLabel = "Text / Code",
                                formattedSummary = "स्कैन किया गया डेटा: $raw",
                                actionUrl = if (isUrl) raw else null
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                onError("स्कैनिंग में त्रुटि: ${e.localizedMessage}")
            }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }
}
