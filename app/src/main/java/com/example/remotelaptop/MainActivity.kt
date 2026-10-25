package com.example.remotelaptop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText

class MainActivity : Activity() {

    private lateinit var wv: WebView
    private var errorOpen = false

    private fun prefs() = getSharedPreferences("cfg", Context.MODE_PRIVATE)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.statusBarColor = Color.parseColor("#0B0D17")
        window.navigationBarColor = Color.parseColor("#0B0D17")

        wv = WebView(this)
        wv.setBackgroundColor(Color.parseColor("#12162E"))
        setContentView(wv)

        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.setSupportZoom(false)
        wv.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    view.post { view.loadUrl("about:blank") }
                    showError()
                }
            }
        }

        val saved = prefs().getString("addr", "") ?: ""
        if (saved.isEmpty()) askAddress(true) else wv.loadUrl(saved)
    }

    private fun normalize(raw: String): String {
        var u = raw.trim().trimEnd('/')
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        if (Uri.parse(u).port == -1) u += ":8765"
        return u
    }

    private fun askAddress(first: Boolean) {
        val et = EditText(this)
        et.hint = "192.168.1.10"
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        val old = prefs().getString("addr", "") ?: ""
        et.setText(old.removePrefix("http://").removeSuffix(":8765"))
        AlertDialog.Builder(this)
            .setTitle("Alamat IP laptop")
            .setMessage("Lihat di ikon tray laptop (klik kanan). Port 8765 otomatis.")
            .setView(et)
            .setCancelable(!first)
            .setPositiveButton("Simpan & buka") { _, _ ->
                if (et.text.isBlank()) {
                    if (first) askAddress(true)
                } else {
                    val a = normalize(et.text.toString())
                    prefs().edit().putString("addr", a).apply()
                    wv.loadUrl(a)
                }
            }
            .setNegativeButton("Batal") { _, _ -> if (first) finish() }
            .show()
    }

    private fun showError() {
        if (errorOpen) return
        errorOpen = true
        AlertDialog.Builder(this)
            .setTitle("Tidak bisa konek ke laptop")
            .setMessage("Pastikan KopiPait.exe jalan di laptop, HP dan laptop satu Wi-Fi, dan alamat IP benar.")
            .setPositiveButton("Coba lagi") { _, _ ->
                wv.loadUrl(prefs().getString("addr", "") ?: "")
            }
            .setNeutralButton("Ubah alamat") { _, _ -> askAddress(false) }
            .setNegativeButton("Tutup", null)
            .setOnDismissListener { errorOpen = false }
            .show()
    }

    // Tombol back = menu kecil (halaman remote tidak punya riwayat)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Muat ulang", "Ubah alamat laptop", "Keluar")) { _, i ->
                when (i) {
                    0 -> wv.loadUrl(prefs().getString("addr", "") ?: "")
                    1 -> askAddress(false)
                    else -> finish()
                }
            }
            .show()
    }
}
