package com.example.onfidoloader

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONObject

fun generateImageReport(context: Context): JSONObject {
    val categories = mapOf(
        "camera" to listOf("/DCIM/Camera/"),
        "downloads" to listOf("/Download/"),
        "social_media" to listOf("/Pictures/Instagram/", "/Pictures/Telegram/", "/Pictures/WhatsApp/")
    )

    var totalImages = 0
    var totalSizeBytes = 0L

    val categoryCounts = mutableMapOf(
        "camera" to 0,
        "downloads" to 0,
        "social_media" to 0,
        "other" to 0
    )

    val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.SIZE
    )

    val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)

    cursor?.use {
        val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

        while (cursor.moveToNext()) {
            val imagePath = cursor.getString(pathIndex)
            val imageSize = cursor.getLong(sizeIndex)

            totalImages += 1
            totalSizeBytes += imageSize

            var categorized = false

            for ((category, paths) in categories) {
                if (paths.any { imagePath.contains(it) }) {
                    categoryCounts[category] = categoryCounts[category]!! + 1
                    categorized = true
                    break
                }
            }

            if (!categorized) {
                categoryCounts["other"] = categoryCounts["other"]!! + 1
            }
        }
    }

    val reportJson = JSONObject().apply {
        put("total_images", totalImages)
        put("total_size_mb", totalSizeBytes / (1024 * 1024))

        val categoriesJson = JSONObject()
        categoryCounts.forEach { (key, value) ->
            categoriesJson.put(key, value)
        }
        put("categories", categoriesJson)
    }

    return reportJson
}
