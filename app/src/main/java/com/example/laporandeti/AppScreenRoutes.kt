package com.example.laporandeti // Sesuaikan dengan package Anda

import android.net.Uri

// Konstanta untuk argumen navigasi (jika belum ada di file lain)
const val PHOTO_URI_ARG = "photoUri"
const val PHOTO_LIST_ARG = "photoListJson"
const val CURRENT_PHOTO_INDEX_ARG = "currentPhotoIndex"
const val REPORT_TITLE_ARG = "reportTitle" // Untuk Feature1WebView

object AppScreenRoutes {
    // Rute dasar
    const val Home = "home_screen"
    const val Feature1 = "feature1_screen"
    const val Camera = "camera_screen"
    // Rute untuk GalleryScreen akan dimodifikasi untuk menerima argumen
    const val GalleryBase = "gallery_screen_base" // Rute dasar untuk galeri
    const val PhotoDetailBase = "photo_detail_base" // Rute dasar untuk detail foto
    const val Feature1WebViewBase = "feature1_webview_base" // Rute dasar untuk WebView fitur 1

    // Argumen untuk GalleryScreen (untuk mode picker)
    const val GALLERY_PICKER_MODE_ARG = "gallery_screen_base"
    // Kunci untuk mengembalikan hasil URI terpilih dari GalleryScreen
    const val SELECTED_IMAGE_URI_RESULT_KEY = "selectedImageUriResult"

    // Fungsi untuk membangun rute Feature1WebView dengan argumen
    fun feature1WebViewWithArgs(reportTitle: String): String {
        return "$Feature1WebViewBase/${Uri.encode(reportTitle)}"
    }

    // Fungsi untuk membangun rute GalleryScreen dengan argumen
    fun galleryScreenWithArgs(isPickerMode: Boolean = false): String {
        return "$GalleryBase?$GALLERY_PICKER_MODE_ARG=$isPickerMode"
    }

    // Fungsi untuk membangun rute PhotoDetail dengan argumen
    fun photoDetailWithArgs(
        photoUri: String, // Ini adalah URI yang sudah di-encode
        photoListJson: String?, // Ini adalah JSON list URI yang sudah di-encode
        currentPhotoIndex: Int
    ): String {
        // photoUri sudah di-encode sebelum dipanggil ke sini
        var route = "$PhotoDetailBase/$photoUri?$CURRENT_PHOTO_INDEX_ARG=$currentPhotoIndex"
        photoListJson?.let {
            // photoListJson juga sudah di-encode sebelum dipanggil ke sini
            route += "&$PHOTO_LIST_ARG=$it"
        }
        return route
    }

    // --- Definisi Rute Lengkap untuk NavHost ---
    // Ini digunakan di NavHost untuk mendefinisikan pola rute dan argumennya

    val Feature1WebView = "$Feature1WebViewBase/{$REPORT_TITLE_ARG}"

    val Gallery = "$GalleryBase?$GALLERY_PICKER_MODE_ARG={$GALLERY_PICKER_MODE_ARG}"

    val PhotoDetail = "$PhotoDetailBase/{$PHOTO_URI_ARG}?$CURRENT_PHOTO_INDEX_ARG={$CURRENT_PHOTO_INDEX_ARG}&$PHOTO_LIST_ARG={$PHOTO_LIST_ARG}"
}
