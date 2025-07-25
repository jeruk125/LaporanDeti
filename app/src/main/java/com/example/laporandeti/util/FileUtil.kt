package com.example.laporandeti.util // Atau package utama Anda jika lebih suka

import android.content.Context
import android.util.Log
import com.example.laporandeti.R // Impor R jika menggunakan string resource
import java.io.File

// Tag bisa ditambahkan jika perlu logging dari sini
// private const val TAG_FILE_UTIL = "FileUtil"

/**
 * Fungsi tunggal untuk mendapatkan direktori penyimpanan gambar aplikasi.
 * Digunakan oleh CameraScreen untuk menyimpan dan GalleryScreen untuk membaca.
 */
fun getAppOutputDirectory(context: Context, subFolder: String = APP_IMAGE_SUBFOLDER): File {
    // ContextCompat.getExternalMediaDirs() direkomendasikan untuk mendapatkan akses
    // ke direktori media khusus aplikasi (Android/media/[nama_paket]).
    // Ini mengembalikan array karena perangkat mungkin memiliki beberapa volume penyimpanan eksternal.
    // Biasanya, yang pertama adalah penyimpanan utama.
    val externalMediaDirs = context.externalMediaDirs

    val targetDirectory: File
    if (externalMediaDirs.isNotEmpty()) {
        val appSpecificMediaDir = externalMediaDirs[0]?.let {
            File(it, subFolder) // Buat subfolder di dalam Android/media/[nama_paket]/
        }
        if (appSpecificMediaDir != null) {
            targetDirectory = appSpecificMediaDir
        } else {
            // Fallback jika externalMediaDirs[0] null (jarang terjadi tapi mungkin)
            Log.w(TAG_FILE_UTIL, "External media directory utama null, fallback ke direktori file internal.")
            targetDirectory = context.filesDir.resolve(subFolder)
        }
    } else {
        // Fallback jika tidak ada direktori media eksternal yang tersedia (misalnya, tidak ada SD card atau penyimpanan emulated)
        // atau jika ada masalah izin (meskipun untuk direktori khusus aplikasi, izin biasanya tidak masalah).
        Log.w(TAG_FILE_UTIL, "Tidak ada direktori media eksternal, fallback ke direktori file internal.")
        targetDirectory = context.filesDir.resolve(subFolder) // Menyimpan di penyimpanan internal aplikasi
    }

    // Buat direktori jika belum ada
    if (!targetDirectory.exists()) {
        if (targetDirectory.mkdirs()) {
            Log.d(TAG_FILE_UTIL, "Direktori berhasil dibuat: ${targetDirectory.absolutePath}")
        } else {
            Log.e(TAG_FILE_UTIL, "Gagal membuat direktori: ${targetDirectory.absolutePath}")
            // Jika gagal membuat di lokasi yang diinginkan, mungkin fallback ke context.filesDir langsung
            // atau throw exception, tergantung kebutuhan error handling.
            // Untuk sekarang, kita biarkan saja dan CameraScreen akan menangani error mkdirs.
        }
    } else {
        Log.d(TAG_FILE_UTIL, "Direktori sudah ada: ${targetDirectory.absolutePath}")
    }

    Log.d(TAG_FILE_UTIL, "Output Directory yang digunakan: ${targetDirectory.absolutePath}, Ada: ${targetDirectory.exists()}")
    return targetDirectory
}