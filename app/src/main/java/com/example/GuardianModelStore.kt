package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

/**
 * GuardianModelStore: Downloads, verifies (SHA-256), and manages Voice Guardian models
 * in filesDir/guardian_models/<id>/ with .part + rename and .ok marker.
 */
object GuardianModelStore {
    private const val TAG = "GuardianModelStore"
    private const val MANIFEST_URL = "https://getmaya.online/models/guardian/manifest.json"

    fun getModelsDir(context: Context): File {
        return File(context.filesDir, "guardian_models")
    }

    fun isModelReady(context: Context, modelId: String): Boolean {
        val modelDir = File(getModelsDir(context), modelId)
        val okFile = File(modelDir, ".ok")
        return okFile.exists()
    }

    suspend fun downloadModelFiles(
        context: Context,
        modelId: String,
        tfliteUrl: String,
        tfliteSha256: String,
        cohortUrl: String,
        cohortSha256: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(getModelsDir(context), modelId)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            val tfliteFile = File(modelDir, "$modelId.tflite")
            val cohortFile = File(modelDir, if (modelId == "rdn2b6s") "cohort_rdn2b6s.bin" else "cohort.bin")
            val okFile = File(modelDir, ".ok")

            // If already .ok, verify hashes quickly or return true
            if (okFile.exists() && tfliteFile.exists() && cohortFile.exists()) {
                onProgress(1f)
                return@withContext true
            }

            // 1. Download tflite
            onProgress(0.1f)
            val tflitePart = File(modelDir, "$modelId.tflite.part")
            if (!downloadAndVerify(tfliteUrl, tflitePart, tfliteFile, tfliteSha256)) {
                return@withContext false
            }

            onProgress(0.6f)
            // 2. Download cohort
            val cohortPart = File(modelDir, "cohort.bin.part")
            if (!downloadAndVerify(cohortUrl, cohortPart, cohortFile, cohortSha256)) {
                return@withContext false
            }

            // 3. Create .ok marker
            okFile.writeText("OK")
            onProgress(1f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model $modelId: ${e.message}", e)
            false
        }
    }

    private fun downloadAndVerify(urlString: String, partFile: File, targetFile: File, expectedSha256: String): Boolean {
        try {
            if (partFile.exists()) partFile.delete()

            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != 200) {
                connection.disconnect()
                return false
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(partFile)
            val digest = MessageDigest.getInstance("SHA-256")

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!calculatedHash.equals(expectedSha256, ignoreCase = true)) {
                Log.e(TAG, "SHA-256 mismatch! Expected $expectedSha256, got $calculatedHash")
                if (partFile.exists()) partFile.delete()
                return false
            }

            if (targetFile.exists()) targetFile.delete()
            return partFile.renameTo(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download/Verify exception: ${e.message}", e)
            if (partFile.exists()) partFile.delete()
            return false
        }
    }
}
