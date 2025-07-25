// src/main/java/com/example/laporandeti/ui/screens/GalleryScreen.kt
package com.example.laporandeti.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Pastikan ini items dari lazy.grid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.laporandeti.util.APP_IMAGE_SUBFOLDER
import com.example.laporandeti.util.getAppOutputDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import androidx.compose.runtime.mutableStateListOf

const val TAG_GALLERY_SCREEN = "GalleryScreen"
private const val THUMBNAIL_SIZE_DP = 120 // Ukuran thumbnail yang diinginkan dalam DP

@Composable
fun GalleryScreen(
    navController: NavController // Anda mungkin akan menggunakannya untuk navigasi ke detail gambar
) {
    val context = LocalContext.current
    // FIX: Secara eksplisit tentukan tipe argumen untuk mutableStateListOf
    val imageFiles = remember { mutableStateListOf<File>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect untuk memuat daftar file gambar dari direktori
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val outputDirectory = getAppOutputDirectory(context, APP_IMAGE_SUBFOLDER)
            if (outputDirectory.exists() && outputDirectory.isDirectory) {
                // Jalankan operasi file di Dispatchers.IO
                val files = withContext(Dispatchers.IO) {
                    outputDirectory.listFiles { file ->
                        file.isFile && file.extension.equals("jpg", ignoreCase = true)
                    }?.sortedByDescending { it.lastModified() } ?: emptyArray()
                }
                imageFiles.clear()
                imageFiles.addAll(files)
                Log.d(TAG_GALLERY_SCREEN, "Loaded ${imageFiles.size} images from gallery.")
                if (imageFiles.isEmpty()) {
                    errorMessage = "Belum ada foto yang diambil."
                }
            } else {
                Log.w(TAG_GALLERY_SCREEN, "Gallery directory does not exist or is not a directory: ${outputDirectory.absolutePath}")
                errorMessage = "Direktori galeri tidak ditemukan."
            }
        } catch (e: Exception) {
            Log.e(TAG_GALLERY_SCREEN, "Error loading image files: ${e.message}", e)
            errorMessage = "Gagal memuat gambar: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp) // Padding di atas untuk judul
    ) {
        Text(
            text = "Galeri Laporan Deteksi",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Memuat galeri...", modifier = Modifier.padding(top = 60.dp))
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = THUMBNAIL_SIZE_DP.dp), // Membuat grid lebih responsif
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(imageFiles, key = { it.absolutePath }) { file ->
                        GalleryItem(
                            file = file,
                            thumbnailSizePx = (THUMBNAIL_SIZE_DP * LocalContext.current.resources.displayMetrics.density).toInt(), // Konversi DP ke PX untuk bitmap
                            onClick = { clickedFile ->
                                Log.d(TAG_GALLERY_SCREEN, "Image clicked: ${clickedFile.name}")
                                // TODO: Navigasi ke layar detail gambar jika diperlukan
                                // navController.navigate("imageDetail/${Uri.encode(clickedFile.absolutePath)}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryItem(
    file: File,
    thumbnailSizePx: Int, // Terima ukuran thumbnail dalam Px
    onClick: (File) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }
    var errorLoadingBitmap by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope() // Untuk meluncurkan coroutine yang bisa dibatalkan
    var loadImageJob by remember { mutableStateOf<Job?>(null) }


    // LaunchedEffect untuk memuat bitmap thumbnail.
    // Dibatalkan dan dimulai ulang jika 'file' atau 'thumbnailSizePx' berubah.
    LaunchedEffect(file, thumbnailSizePx) {
        // Batalkan pekerjaan sebelumnya jika ada untuk menghindari pemuatan ganda atau pembaruan yang tidak perlu
        loadImageJob?.cancel()

        loadImageJob = coroutineScope.launch {
            isLoadingBitmap = true
            errorLoadingBitmap = null
            // Tambahkan delay kecil untuk debounce, berguna jika item cepat masuk/keluar dari viewport
            // delay(50) // Optional: Sesuaikan atau hapus jika tidak diperlukan
            if (!isActive) return@launch // Jangan lanjutkan jika coroutine sudah tidak aktif

            try {
                bitmap = loadSampledBitmapFromFile(file, thumbnailSizePx, thumbnailSizePx)
                if (bitmap == null && isActive) { // Periksa isActive lagi sebelum menetapkan error
                    errorLoadingBitmap = "Gagal memuat"
                }
                Log.d(TAG_GALLERY_SCREEN, "Loaded bitmap for ${file.name} with dimensions ${bitmap?.width}x${bitmap?.height}")
            } catch (e: FileNotFoundException) {
                Log.e(TAG_GALLERY_SCREEN, "Error loading bitmap: File not found for ${file.name}", e)
                if (isActive) errorLoadingBitmap = "File hilang"
            } catch (e: OutOfMemoryError) {
                Log.e(TAG_GALLERY_SCREEN, "OutOfMemoryError loading bitmap for ${file.name}", e)
                if (isActive) errorLoadingBitmap = "Memori penuh"
                // Pertimbangkan untuk tidak mencoba memuat ulang secara otomatis di sini
            } catch (e: Exception) {
                Log.e(TAG_GALLERY_SCREEN, "Error loading bitmap for ${file.name}: ${e.message}", e)
                if (isActive) errorLoadingBitmap = "Error"
            } finally {
                if (isActive) isLoadingBitmap = false
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Menjaga item tetap persegi
            .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background
            .clickable { if (bitmap != null) onClick(file) } // Hanya clickable jika bitmap berhasil dimuat
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingBitmap -> {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Gambar Galeri: ${file.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // Crop agar mengisi kotak dengan tetap menjaga aspek rasio
                )
            }
            errorLoadingBitmap != null -> {
                Text(
                    text = errorLoadingBitmap!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// Fungsi helper untuk memuat bitmap dengan downsampling, dijalankan di Dispatchers.IO
suspend fun loadSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
    // Jalankan operasi blocking di Dispatchers.IO
    return withContext(Dispatchers.IO) {
        if (!file.exists()) {
            Log.w(TAG_GALLERY_SCREEN, "File does not exist: ${file.absolutePath}")
            return@withContext null
        }
        try {
            // Langkah 1: Decode dengan inJustDecodeBounds=true untuk mendapatkan dimensi
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Langkah 2: Hitung inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Langkah 3: Decode bitmap dengan inSampleSize yang sudah dihitung
            options.inJustDecodeBounds = false
            return@withContext BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG_GALLERY_SCREEN, "OutOfMemoryError while decoding file ${file.name}", e)
            // Coba lagi dengan inSampleSize yang lebih besar jika OOM terjadi
            // Ini adalah upaya sederhana, strategi yang lebih baik mungkin diperlukan
            try {
                val fallbackOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, fallbackOptions)
                fallbackOptions.inSampleSize = calculateInSampleSize(fallbackOptions, reqWidth / 2, reqHeight / 2) // Kurangi target size
                fallbackOptions.inJustDecodeBounds = false
                return@withContext BitmapFactory.decodeFile(file.absolutePath, fallbackOptions)
            } catch (e2: Exception) {
                Log.e(TAG_GALLERY_SCREEN, "Failed to load bitmap even with fallback for ${file.name}", e2)
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG_GALLERY_SCREEN, "General error decoding file ${file.name}", e)
            return@withContext null
        }
    }
}


// Fungsi calculateInSampleSize (tetap sama seperti yang Anda miliki)
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        // Hitung inSampleSize terbesar yang merupakan pangkat 2 dan menjaga
        // kedua dimensi (tinggi dan lebar) lebih besar dari dimensi yang diminta.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    // Tambahan: pastikan inSampleSize minimal 1
    if (inSampleSize == 0) inSampleSize = 1;
    Log.d(TAG_GALLERY_SCREEN, "Calculated inSampleSize: $inSampleSize for original ${width}x${height} -> target ${reqWidth}x${reqHeight}")
    return inSampleSize
}

