package com.example.laporandeti.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf // Penting: Ini untuk membuat daftar yang bisa diobservasi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.laporandeti.util.APP_IMAGE_SUBFOLDER
import com.example.laporandeti.util.getAppOutputDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File // Penting: Ini untuk tipe File

const val TAG_GALLERY_SCREEN = "GalleryScreen"

@Composable
fun GalleryScreen(
    navController: NavController
) {
    val context = LocalContext.current
    // --- PERBAIKAN PENTING DI SINI ---
    // Baris ini adalah penyebab utama error Anda.
    // Kita harus secara eksplisit menentukan bahwa daftar ini akan berisi objek 'File'.
    val imageFiles = remember { mutableStateListOf<File>() } // Ini sekarang baris 52

    // Memuat ulang daftar file saat komponen ini pertama kali di-compose
    // atau jika ada perubahan signifikan yang memerlukan pembaruan daftar file.
    LaunchedEffect(Unit) {
        val outputDirectory = getAppOutputDirectory(context, APP_IMAGE_SUBFOLDER)
        if (outputDirectory.exists() && outputDirectory.isDirectory) {
            val files = outputDirectory.listFiles { file ->
                file.isFile && file.extension.lowercase() == "jpg"
            }?.sortedByDescending { it.lastModified() } ?: emptyArray()
            imageFiles.clear()
            imageFiles.addAll(files)
            Log.d(TAG_GALLERY_SCREEN, "Loaded ${imageFiles.size} images from gallery.")
        } else {
            Log.w(TAG_GALLERY_SCREEN, "Gallery directory does not exist or is not a directory: ${outputDirectory.absolutePath}")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Galeri Laporan Deteksi",
            modifier = Modifier.padding(16.dp)
        )

        if (imageFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada foto yang diambil.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // Tampilkan 3 kolom gambar
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // --- PERBAIKAN JUGA BERLAKU DI SINI ---
                // Setelah 'imageFiles' di baris 52 diketahui sebagai 'MutableList<File>',
                // maka panggilan 'items' ini akan otomatis mengenali tipenya sebagai 'Collection<File>'.
                items(imageFiles, key = { it.absolutePath }) { file -> // Ini sekarang baris 54
                    // Menggunakan key untuk LazyVerticalGrid agar performa lebih baik
                    // saat item ditambahkan/dihapus
                    GalleryItem(file = file) { clickedFile ->
                        // Implementasi navigasi ke detail gambar jika ada
                        Log.d(TAG_GALLERY_SCREEN, "Image clicked: ${clickedFile.name}")
                        // Contoh: navController.navigate("imageDetail/${clickedFile.name}")
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryItem(file: File, onClick: (File) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Efek samping untuk memuat bitmap secara asinkron
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                // Optimasi: Decode gambar dengan ukuran yang disampling
                // Ini akan mencegah loading bitmap besar ke memori jika tidak diperlukan
                val options = BitmapFactory.Options().apply {
                    // Coba atur ukuran target yang masuk akal untuk thumbnail di grid
                    inSampleSize = calculateInSampleSize(this, 150, 150) // Target 150x150 dp
                    inJustDecodeBounds = false // Sekarang kita ingin mendekode bitmap
                }
                bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                Log.d(TAG_GALLERY_SCREEN, "Loaded bitmap for ${file.name} with dimensions ${bitmap?.width}x${bitmap?.height}")
            } catch (e: Exception) {
                Log.e(TAG_GALLERY_SCREEN, "Error loading bitmap for ${file.name}: ${e.message}", e)
                bitmap = null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Menjaga aspek rasio kotak
            .clickable { onClick(file) }
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Memotong agar pas dalam kotak
            )
        } else {
            // Teks placeholder jika gambar belum dimuat atau gagal
            Text(text = "Memuat...", modifier = Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Menghitung inSampleSize yang optimal untuk BitmapFactory.Options
 * agar gambar didekode menjadi ukuran yang lebih kecil dan hemat memori.
 *
 * @param options Opsi BitmapFactory saat ini (dengan inJustDecodeBounds = true)
 * @param reqWidth Lebar yang dibutuhkan
 * @param reqHeight Tinggi yang dibutuhkan
 * @return Nilai inSampleSize (faktor penskalaan)
 */
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Dimensi gambar asli
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        // Hitung inSampleSize terbesar yang merupakan pangkat 2
        // dan menjaga dimensi hasil akhir lebih besar dari lebar & tinggi yang diminta
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}