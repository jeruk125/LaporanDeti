package com.example.laporandeti.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.laporandeti.util.APP_PUBLIC_DOCUMENT_SUBFOLDER
import com.example.laporandeti.util.TAG_WEBVIEW
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    navController: NavController,
    urlToLoad: String,
    screenTitle: String?,
    onPageFinished: (String) -> Unit = {},
    onShouldOverrideUrlLoading: (url: String) -> Boolean = { false },
    // Tambahkan parameter untuk mengontrol perilaku tombol kembali
    // Defaultnya adalah popBackStack, Anda bisa ubah di AppNavGraph
    onBackButtonClicked: () -> Unit = { /* default ke navController.popBackStack() */ }
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    var filePathCallbackHolder by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val systemFileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (filePathCallbackHolder == null) {
            Log.d(TAG_WEBVIEW, "filePathCallbackHolder is null on system chooser result, returning.")
            return@rememberLauncherForActivityResult
        }
        var uris: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val clipData = data.clipData!!
                uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (data?.data != null) {
                uris = arrayOf(data.data!!)
            }
        }
        Log.d(TAG_WEBVIEW, "System file chooser result: ${uris?.joinToString() ?: "null"}")
        filePathCallbackHolder?.onReceiveValue(uris)
        filePathCallbackHolder = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle ?: "Laporan") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Gunakan lambda onBackButtonClicked yang baru
                        onBackButtonClicked()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { factoryContext ->
                    WebView(factoryContext).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                Log.d(TAG_WEBVIEW, "Page started loading: $url")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let { currentUrl ->
                                    onPageFinished(currentUrl)
                                    Log.d(TAG_WEBVIEW, "Page finished loading: $url")
                                    cookieManager.flush()
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val requestedUrl = request?.url?.toString()
                                return if (requestedUrl != null) onShouldOverrideUrlLoading(requestedUrl)
                                else super.shouldOverrideUrlLoading(view, request)
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode else "N/A"
                                val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description else "N/A"
                                Log.e(TAG_WEBVIEW, "WebView Error: Code $errorCode - $description for ${request?.url}")
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                Log.d(TAG_WEBVIEW, "onShowFileChooser triggered. Current filePathCallbackHolder: $filePathCallbackHolder")
                                if (filePathCallbackHolder != null) {
                                    Log.w(TAG_WEBVIEW, "Previous filePathCallback was not null, cancelling it with null.")
                                    filePathCallbackHolder?.onReceiveValue(null)
                                }
                                filePathCallbackHolder = filePathCallback

                                Log.d(TAG_WEBVIEW, "Always launching system file chooser via createIntent or fallback.")
                                try {
                                    val intent = fileChooserParams?.createIntent()
                                    if (intent != null) {
                                        systemFileChooserLauncher.launch(intent)
                                    } else {
                                        Log.w(TAG_WEBVIEW, "fileChooserParams or createIntent() was null. Creating a basic fallback intent.")
                                        var fallbackIntent = Intent(Intent.ACTION_GET_CONTENT)
                                        fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE)
                                        val types = fileChooserParams?.acceptTypes
                                        fallbackIntent.type = if (!types.isNullOrEmpty() && types.all { !it.isNullOrBlank() }) {
                                            types.joinToString(",")
                                        } else {
                                            "*/*" // Default ke semua jenis file jika tidak ada yang ditentukan
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                            if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                                                fallbackIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                            }
                                        }
                                        Log.d(TAG_WEBVIEW, "Launching system chooser with fallback intent: $fallbackIntent, extras: ${fallbackIntent.extras}")
                                        systemFileChooserLauncher.launch(fallbackIntent)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG_WEBVIEW, "Cannot open file chooser", e)
                                    Toast.makeText(
                                        webView?.context ?: factoryContext,
                                        "Tidak dapat membuka pemilih file.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    filePathCallbackHolder?.onReceiveValue(null)
                                    filePathCallbackHolder = null
                                }
                                return true
                            }

                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                Log.d(TAG_WEBVIEW, "Loading progress: $newProgress%")
                                if (newProgress == 100) {
                                    isLoading = false
                                } else if (!isLoading && newProgress > 0 && newProgress < 100) {
                                    isLoading = true
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    Log.d(TAG_WEBVIEW, "JS Console (${it.sourceId()}:${it.lineNumber()}): ${it.message()}")
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        // --- Integrasi DownloadManager ---
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            Log.d(TAG_WEBVIEW, "Download triggered:")
                            Log.d(TAG_WEBVIEW, "  URL: $url")
                            Log.d(TAG_WEBVIEW, "  UserAgent: $userAgent")
                            Log.d(TAG_WEBVIEW, "  ContentDisposition: $contentDisposition")
                            Log.d(TAG_WEBVIEW, "  Mimetype: $mimetype")
                            Log.d(TAG_WEBVIEW, "  ContentLength: $contentLength")

                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)

                            val cookies = CookieManager.getInstance().getCookie(url)
                            if (cookies != null) {
                                request.addRequestHeader("cookie", cookies)
                                Log.d(TAG_WEBVIEW, "  Cookies added to download request.")
                            }
                            request.addRequestHeader("User-Agent", userAgent)
                            request.setDescription("Mengunduh file Laporan Deti...")

                            var fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            Log.d(TAG_WEBVIEW, "  Suggested FileName by URLUtil: $fileName")

                            if (fileName.isEmpty() || fileName.equals("downloadfile", ignoreCase = true) || fileName.equals("attachment", ignoreCase = true)) {
                                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype)
                                fileName = "DokumenLaporanDeti_${System.currentTimeMillis()}${if (extension != null) ".$extension" else ""}"
                                Log.d(TAG_WEBVIEW, "  Generated fallback FileName: $fileName")
                            }

                            val subFolderToUse = if (APP_PUBLIC_DOCUMENT_SUBFOLDER.isBlank()) {
                                Log.w(TAG_WEBVIEW, "APP_PUBLIC_DOCUMENT_SUBFOLDER is blank, downloading to root of Downloads.")
                                ""
                            } else {
                                APP_PUBLIC_DOCUMENT_SUBFOLDER + File.separator
                            }
                            val destinationPath = subFolderToUse + fileName

                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                destinationPath
                            )
                            Log.d(TAG_WEBVIEW, "  Download destination: ${Environment.DIRECTORY_DOWNLOADS}/$destinationPath")

                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setTitle(fileName)
                            request.allowScanningByMediaScanner()

                            val downloadManager = factoryContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            try {
                                val downloadId = downloadManager.enqueue(request)
                                Log.i(TAG_WEBVIEW, "Download enqueued. ID: $downloadId, File: $fileName")
                                Toast.makeText(factoryContext, "Mulai mengunduh: $fileName", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Log.e(TAG_WEBVIEW, "Gagal memulai unduhan: ${e.message}", e)
                                if (e.message?.contains("java.lang.IllegalArgumentException: Unknown URL content") == true) {
                                    Toast.makeText(factoryContext, "Tidak dapat mengunduh: URL tidak valid atau tidak didukung.", Toast.LENGTH_LONG).show()
                                } else if (e.message?.lowercase()?.contains("download manager is disabled") == true ||
                                    e.message?.lowercase()?.contains("cannot resolve destination") == true ) {
                                    Toast.makeText(factoryContext, "Gagal: Layanan Download Manager mungkin dinonaktifkan.", Toast.LENGTH_LONG).show()
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        factoryContext.startActivity(intent)
                                    } catch (activityEx: ActivityNotFoundException) {
                                        Toast.makeText(factoryContext, "Tidak ada aplikasi browser untuk membuka tautan.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(factoryContext, "Gagal memulai unduhan. Silakan coba lagi.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        if (urlToLoad.isNotBlank()) loadUrl(urlToLoad)
                        else {
                            Log.e(TAG_WEBVIEW, "urlToLoad is blank.")
                            isLoading = false
                        }
                    }
                },
                update = { webViewInstance ->
                    webViewRef = webViewInstance
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webViewInstance, true)
                    val currentWebViewUrl = webViewInstance.url
                    if (currentWebViewUrl != urlToLoad && urlToLoad.isNotBlank()) {
                        if (!isLoading || (currentWebViewUrl == null && urlToLoad.isNotBlank())) {
                            Log.d(TAG_WEBVIEW, "AndroidView update: current URL: $currentWebViewUrl, new URL: $urlToLoad. Reloading.")
                            webViewInstance.loadUrl(urlToLoad)
                        }
                    } else if (currentWebViewUrl == null && urlToLoad.isNotBlank() && !isLoading) {
                        Log.d(TAG_WEBVIEW, "AndroidView update, initial URL load in update block (URL was null): $urlToLoad")
                        webViewInstance.loadUrl(urlToLoad)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}