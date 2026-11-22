package com.alisa.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var splashOverlay: FrameLayout? = null
    private val homeUrl = "https://sites.google.com/view/malisa-1/home"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen Immersive Mode
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Root layout with WebView + Top Loading ProgressBar
        val rootLayout = FrameLayout(this)
        webView = WebView(this)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                10
            )
            visibility = View.GONE
        }

        rootLayout.addView(webView)
        rootLayout.addView(progressBar)
        
        // Custom Splash / Welcome Overlay with user's Background Image & Custom Texts
        splashOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#709bf0"))
            
            val bgView = ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.app_bg)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(bgView)

            // Semi-transparent scrim so text stays readable
            val scrimView = View(this@MainActivity).apply {
                setBackgroundColor(Color.argb(51, 15, 23, 42))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(scrimView)

            val contentBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(64, 64, 64, 64)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            val iconView = ImageView(this@MainActivity).apply {
                setImageResource(R.mipmap.ic_launcher)
                val sizePx = (88 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    bottomMargin = 32
                }
            }
            contentBox.addView(iconView)

            val titleText = "ນ້ອງ ມາລິສາ ພ້ອມຮັບໃຊ້ ແລ້ວຈ້າວ !"
            if (titleText.isNotEmpty()) {
                val titleView = TextView(this@MainActivity).apply {
                    text = titleText
                    setTextColor(Color.WHITE)
                    textSize = 20f
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                contentBox.addView(titleView)
            }

            val descText = "ທ່ານຕ້ອງການ ຜູ້ຊ່ວຍ ອັດສະລິຍະ ບໍ, ເຂົ້າແອບ ນີ້ເລີຍ"
            if (descText.isNotEmpty()) {
                val descView = TextView(this@MainActivity).apply {
                    text = descText
                    setTextColor(Color.parseColor("#E2E8F0"))
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(0, 16, 0, 28)
                }
                contentBox.addView(descView)
            }

            // Action Button configured by user (In-App WebView vs External Browser)
            val actionBtn = Button(this@MainActivity).apply {
                text = "ເຂົ້າລະບົບ ↗"
                setTextColor(Color.WHITE)
                textSize = 14f
                isAllCaps = false
                setTypeface(null, Typeface.BOLD)
                setPadding(48, 28, 48, 28)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#0284c7"))
                    cornerRadius = 32f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
                setOnClickListener {
                    
                    // Open real website in external Android Browser (Chrome / Default Browser)
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(homeUrl))
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "ບໍ່ພົບບຣາວເຊີໃນເຄື່ອງ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            contentBox.addView(actionBtn)
            

            addView(contentBox)
        }
        rootLayout.addView(splashOverlay)
        
        setContentView(rootLayout)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        
        // Fix Google Sites / Google Login / Embedded Forms compatibility (remove '; wv')
        webSettings.userAgentString = webSettings.userAgentString.replace("; wv", "")
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Keep HTTP/HTTPS links inside the app WebView
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // Open tel:, mailto:, whatsapp:, intent: with external Android apps
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // External browser mode keeps welcome screen visible
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    hideSplash()
                    val offlineHtml = """
                        <html>
                        <body style="font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;background:#0f172a;color:#fff;text-align:center;padding:24px;">
                            <h2 style="margin-bottom:8px;">ບໍ່ສາມາດເຊື່ອມຕໍ່ອິນເຕີເນັດໄດ້</h2>
                            <p style="color:#94a3b8;font-size:14px;">ກະລຸນາກວດສອບການເຊື່ອມຕໍ່ເນັດຂອງທ່ານ ແລ້ວລອງໃໝ່ອີກຄັ້ງ</p>
                            <button onclick="window.location.href='$homeUrl'" style="margin-top:16px;padding:12px 24px;border:none;border-radius:12px;background:#709bf0;color:#fff;font-size:15px;font-weight:bold;">ລອງໂຫຼດໃໝ່</button>
                        </body>
                        </html>
                    """.trimIndent()
                    view?.loadDataWithBaseURL(null, offlineHtml, "text/html", "UTF-8", null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }
        
        // Enable downloading PDF/Excel reports directly to Android Downloads folder
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimetype)
                request.addRequestHeader("User-Agent", userAgent)
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimetype)
                )
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, "ກຳລັງດາວໂຫຼດໄຟລ໌...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // Load Target Website or Local HTML Asset
        // External browser mode: website opens via external browser button
        
        // Handle Android Hardware Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hideSplash() {
        splashOverlay?.let { overlay ->
            if (overlay.visibility == View.VISIBLE) {
                overlay.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .withEndAction { overlay.visibility = View.GONE }
                    .start()
            }
        }
    }
}
