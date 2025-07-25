// File: app/src/main/java/com/example/laporandeti/util/MediaUtils.kt
package com.example.laporandeti.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Impor konstanta Anda (pastikan ini ada di Constants.kt)
// import com.example.laporandeti.util.APP_PUBLIC_IMAGE_SUBFOLDER // Sudah diimpor di parameter
// import com.example.laporandeti.util.TAG_MEDIA_STORE // Pastikan ini juga ada jika digunakan

// Jika Anda belum mendefinisikan TAG_MEDIA_STORE di Constants.kt, Anda bisa definisikan di sini:

fun saveImageToAppPublicGallery(
    context: Context,
    bitmap: Bitmap,
    displayName: String, // Nama file tanpa ekstensi, mis: "FotoLaporan_123"
    subFolder: String = APP_PUBLIC_IMAGE_SUBFOLDER, // Default dari Constants.kt
    compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 95
): Uri? {
    val mimeType = when (compressFormat) {
        Bitmap.CompressFormat.JPEG -> "image/jpeg"
        Bitmap.CompressFormat.PNG -> "image/png"
        // Tambahkan format lain jika perlu
        else -> "image/jpeg" // Default
    }
    // Tambahkan ekstensi ke displayName berdasarkan format kompresi
    val extension = when (compressFormat) {
        Bitmap.CompressFormat.JPEG -> ".jpg"
        Bitmap.CompressFormat.PNG -> ".png"
        else -> ".jpg"
    }
    val fileNameWithExtension = if (displayName.endsWith(extension, ignoreCase = true)) {
        displayName
    } else {
        displayName + extension
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExtension)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Untuk Android Q+, kita bisa menentukan RELATIVE_PATH
            // Pastikan subFolder tidak kosong sebelum menambahkannya
            val relativePath = if (subFolder.isNotBlank()) {
                Environment.DIRECTORY_PICTURES + File.separator + subFolder
            } else {
                Environment.DIRECTORY_PICTURES // Simpan di root Pictures jika subFolder kosong
            }
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // Set PENDING status sampai selesai ditulis
        }
    }

    val resolver = context.contentResolver
    var imageUri: Uri? = null
    var outputStream: OutputStream? = null

    try {
        imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
            Log.e(TAG_MEDIA_STORE, "Failed to create new MediaStore record.")
            return null // KEMBALIKAN null jika insert gagal
        }

        outputStream = resolver.openOutputStream(imageUri)
        if (outputStream == null) {
            Log.e(TAG_MEDIA_STORE, "Failed to get output stream for new MediaStore record.")
            // Jika outputStream gagal dibuka, hapus entri yang mungkin sudah dibuat (opsional, tergantung behavior yang diinginkan)
            // resolver.delete(imageUri, null, null)
            return null // KEMBALIKAN null
        }

        if (!bitmap.compress(compressFormat, quality, outputStream)) {
            Log.e(TAG_MEDIA_STORE, "Failed to save bitmap.")
            // Jika kompresi gagal, hapus entri URI yang mungkin tidak lengkap
            resolver.delete(imageUri, null, null)
            return null // KEMBALIKAN null
        }
        Log.d(TAG_MEDIA_STORE, "Bitmap saved successfully to MediaStore: $imageUri")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Setelah selesai, update IS_PENDING ke 0
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
        return imageUri // KEMBALIKAN Uri yang berhasil
    } catch (e: Exception) {
        Log.e(TAG_MEDIA_STORE, "Error saving image to MediaStore: ${e.message}", e)
        // Jika ada exception dan imageUri sudah ada, coba hapus entri yang tidak lengkap
        imageUri?.let {
            try {
                resolver.delete(it, null, null)
            } catch (deleteEx: Exception) {
                Log.e(TAG_MEDIA_STORE, "Error deleting incomplete MediaStore entry: ${deleteEx.message}", deleteEx)
            }
        }
        return null // KEMBALIKAN null jika ada error
    } finally {
        outputStream?.close()
    }
}

// --- Tambahkan juga saveDocumentToAppPublicFolder di sini jika relevan ---
// import java.io.InputStream // Pastikan ada jika di-uncomment
// import android.os.Environment // Pastikan ada jika di-uncomment
// import com.example.laporandeti.util.APP_PUBLIC_DOCUMENT_SUBFOLDER // Pastikan ada jika di-uncomment
// private const val TAG_DOCUMENT_SAVE = "DocumentSaveUtil" // Contoh Tag

// fun saveDocumentToAppPublicFolder(
//    context: Context,
//    inputStream: InputStream,
//    displayName: String, // Nama file lengkap dengan ekstensi, mis: "DokumenPenting.pdf"
//    mimeType: String, // Mis: "application/pdf"
//    subFolder: String = APP_PUBLIC_DOCUMENT_SUBFOLDER // Default dari Constants.kt
// ): Uri? {
//    val contentValues = ContentValues().apply {
//        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val relativePath = if (subFolder.isNotBlank()) {
//                Environment.DIRECTORY_DOCUMENTS + File.separator + subFolder
//            } else {
//                Environment.DIRECTORY_DOCUMENTS
//            }
//            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
//            put(MediaStore.MediaColumns.IS_PENDING, 1)
//        }
//    }
//
//    val resolver = context.contentResolver
//    var documentUri: Uri? = null
//    var outputStream: OutputStream? = null
//
//    try {
//        documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) // Atau MediaStore.Files.getContentUri("external")
//        if (documentUri == null) {
//            Log.e(TAG_DOCUMENT_SAVE, "Failed to create new MediaStore record for document.")
//            return null
//        }
//
//        outputStream = resolver.openOutputStream(documentUri)
//        if (outputStream == null) {
//            Log.e(TAG_DOCUMENT_SAVE, "Failed to get output stream for new MediaStore document record.")
//            return null
//        }
//
//        inputStream.copyTo(outputStream) // Salin data dari InputStream ke OutputStream
//        Log.d(TAG_DOCUMENT_SAVE, "Document saved successfully to MediaStore: $documentUri")
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            contentValues.clear()
//            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
//            resolver.update(documentUri, contentValues, null, null)
//        }
//        return documentUri
//    } catch (e: Exception) {
//        Log.e(TAG_DOCUMENT_SAVE, "Error saving document to MediaStore: ${e.message}", e)
//        documentUri?.let {
//            try {
//                resolver.delete(it, null, null)
//            } catch (deleteEx: Exception) {
//                Log.e(TAG_DOCUMENT_SAVE, "Error deleting incomplete MediaStore document entry: ${deleteEx.message}", deleteEx)
//            }
//        }
//        return null
//    } finally {
//        outputStream?.close()
//        inputStream.close() // Penting untuk menutup InputStream juga
//    }
// }

