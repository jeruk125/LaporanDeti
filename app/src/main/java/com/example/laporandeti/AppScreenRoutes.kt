package com.example.laporandeti // Sesuaikan dengan package Anda

import android.net.Uri

// Konstanta untuk argumen navigasi
const val PHOTO_URI_ARG = "photoUri"
const val PHOTO_LIST_ARG = "photoListJson" // Pastikan nama ini konsisten dengan NavGraph Anda
const val CURRENT_PHOTO_INDEX_ARG = "currentPhotoIndex"
const val REPORT_TITLE_ARG = "reportTitle" // Untuk Feature1WebView

object AppScreenRoutes {
    // Rute dasar
    const val Home = "home_screen"
    const val Feature1 = "feature1_screen"
    const val Camera = "camera_screen"
    const val GalleryBase = "gallery_screen_base" // Rute dasar untuk galeri (tetap berguna untuk pencocokan)
    const val PhotoDetailBase = "photo_detail_base" // Rute dasar untuk detail foto
    const val Feature1WebViewBase = "feature1_webview_base" // Rute dasar untuk WebView fitur 1

    // --- Tidak ada lagi argumen khusus mode picker untuk Gallery ---

    // Fungsi untuk membangun rute Feature1WebView dengan argumen
    fun feature1WebViewWithArgs(reportTitle: String): String {
        return "$Feature1WebViewBase/${Uri.encode(reportTitle)}"
    }

    // Fungsi untuk membangun rute PhotoDetail dengan argumen
    fun photoDetailWithArgs(
        photoUri: String, // Sebaiknya URI String yang belum di-encode, agar encoding dilakukan sekali di sini
        photoListJson: String?, // String JSON, juga sebaiknya belum di-encode
        currentPhotoIndex: Int? // Jadikan nullable jika tidak selalu ada atau default 0
    ): String {
        val encodedPhotoUri = Uri.encode(photoUri)
        var route = "$PhotoDetailBase/$encodedPhotoUri"
        val params = mutableListOf<String>()

        currentPhotoIndex?.let { params.add("$CURRENT_PHOTO_INDEX_ARG=$it") }
        photoListJson?.let { params.add("$PHOTO_LIST_ARG=${Uri.encode(it)}") } // Encode JSON string

        if (params.isNotEmpty()) {
            route += "?${params.joinToString("&")}"
        }
        return route
    }

    // --- Definisi Rute Lengkap untuk NavHost ---
    // Ini digunakan di NavHost untuk mendefinisikan pola rute dan argumennya

    val Feature1WebView = "$Feature1WebViewBase/{$REPORT_TITLE_ARG}"

    // Gallery sekarang adalah rute sederhana tanpa argumen query eksplisit di sini
    val Gallery = GalleryBase // Rute Gallery adalah GalleryBase, tanpa argumen tambahan

    // PhotoDetail - pastikan argumen opsional ditangani dengan benar di NavGraph
    // Argumen query adalah opsional secara default jika tidak ada defaultValue di NavGraph
    // dan tipenya nullable.
    val PhotoDetail = "$PhotoDetailBase/{$PHOTO_URI_ARG}" +
            "?$CURRENT_PHOTO_INDEX_ARG={$CURRENT_PHOTO_INDEX_ARG}" +
            "&$PHOTO_LIST_ARG={$PHOTO_LIST_ARG}" // Pastikan NavGraph Anda menangani ini sebagai nullable
}
