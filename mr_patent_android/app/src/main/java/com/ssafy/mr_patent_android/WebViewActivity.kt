package com.ssafy.mr_patent_android

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private const val TAG = "WebViewActivity"
class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
        val btn = findViewById<TextView>(R.id.webView_title)
        btn.setOnClickListener {
            finish()
        }

        webView.settings.javaScriptEnabled = true




        val url = intent.getStringExtra("url") ?: "https://www.patent.go.kr/smart/portal/Main.do"
        val file = intent.getIntExtra("file",-1)

        if (file == -1){
            webView.loadUrl(url)
        }
        else{
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=${Uri.encode(url)}"
            Log.d(TAG, "onCreate: $googleDocsUrl")
            webView.loadUrl(googleDocsUrl)
        }
    }
}