package com.example.laporandeti.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.laporandeti.R
import com.example.laporandeti.ui.theme.LaporanDetiTheme
import java.io.File // Impor untuk kelas File

private const val TAG_PHOTO_DETAIL = "PhotoDetailScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoDetailScreen(
    navController: NavController,
    initialPhotoUri: Uri, // URI sudah di-decode oleh NavHost
    photoUris: List<Uri>?, // Daftar URI lain untuk Pager, juga sudah di-decode
    initialIndex: Int = 0
) {
    val context = LocalContext.current

    val displayableUris = remember(initialPhotoUri, photoUris) {
        photoUris?.takeIf { it.isNotEmpty() } ?: listOf(initialPhotoUri)
    }

    // Memastikan initialIndex berada dalam rentang yang valid
    val validInitialIndex = remember(displayableUris, initialIndex) {
        if (displayableUris.isEmpty()) {
            0
        } else {
            initialIndex.coerceIn(0, displayableUris.size - 1)
        }
    }

    Log.d(TAG_PHOTO_DETAIL, "Received Initial URI: $initialPhotoUri")
    Log.d(TAG_PHOTO_DETAIL, "Received Photo URIs Count: ${photoUris?.size ?: "null (using initial only)"}")
    Log.d(TAG_PHOTO_DETAIL, "Displayable URIs Count: ${displayableUris.size}, Valid Initial Index: $validInitialIndex")
    displayableUris.forEachIndexed { index, uri ->
        Log.d(TAG_PHOTO_DETAIL, "Displayable URI [$index]: $uri, Scheme: ${uri.scheme}, Path: ${uri.path}")
    }


    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { displayableUris.size }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Foto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (displayableUris.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    // Ambil URI untuk halaman saat ini dengan aman
                    val currentUri = displayableUris.getOrNull(pageIndex)

                    if (currentUri != null) {
                        Log.d(TAG_PHOTO_DETAIL, "Attempting to load URI for page $pageIndex: $currentUri")

                        // OPSI 1: Mencoba memuat langsung dengan URI (umumnya bekerja untuk content:// dan http://)
                        // Untuk file:/// ini juga seharusnya bekerja jika path valid dan ada izin
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentUri) // Menggunakan URI langsung
                                .crossfade(true)
                                .error(R.drawable.ic_broken_image) // Gambar X Anda
                                .placeholder(R.drawable.ic_image_placeholder)
                                .build(),
                            contentDescription = "Foto Detail ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            onError = { errorResult ->
                                Log.e(TAG_PHOTO_DETAIL, "AsyncImage Error (Option 1 - Direct URI): $currentUri", errorResult.result.throwable)
                                // Jika Opsi 1 gagal, Anda bisa mempertimbangkan untuk tidak mencoba Opsi 2 di sini
                                // atau memiliki logika untuk mencoba Opsi 2 hanya jika skema URI adalah 'file'
                            }
                        )

                        // --- ATAU ---

                        // OPSI 2: Mencoba memuat menggunakan objek File (khususnya jika URI adalah file:///)
                        // Anda mungkin ingin memilih salah satu dari opsi ini atau menambah logika
                        // untuk menggunakan Opsi 2 hanya jika Opsi 1 gagal dan scheme adalah 'file'

                        /* // Buka komentar ini untuk mencoba OPSI 2
                        if (currentUri.scheme == "file") {
                            val filePath = currentUri.path
                            if (filePath != null) {
                                val imageFile = File(filePath)
                                if (imageFile.exists()) {
                                    Log.d(TAG_PHOTO_DETAIL, "Attempting to load File for page $pageIndex: ${imageFile.absolutePath}")
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageFile) // Menggunakan objek File
                                            // Atau .data(Uri.fromFile(imageFile))
                                            .crossfade(true)
                                            .error(R.drawable.ic_broken_image)
                                            .placeholder(R.drawable.ic_image_placeholder)
                                            .build(),
                                        contentDescription = "Foto Detail ${pageIndex + 1} (File)",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                        onError = { errorResult ->
                                            Log.e(TAG_PHOTO_DETAIL, "AsyncImage Error (Option 2 - File Object): ${imageFile.absolutePath}", errorResult.result.throwable)
                                        }
                                    )
                                } else {
                                    Log.e(TAG_PHOTO_DETAIL, "File does not exist for page $pageIndex: ${imageFile.absolutePath}")
                                    Text("File gambar tidak ditemukan di path.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                                }
                            } else {
                                Log.e(TAG_PHOTO_DETAIL, "URI path is null for file scheme URI: $currentUri")
                                Text("Path URI tidak valid.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                        } else if (currentUri.scheme != "file") {
                            // Jika bukan 'file' dan Opsi 1 (di atas) gagal, logikanya mungkin sudah tercakup oleh onError Opsi 1.
                            // Atau Anda bisa memiliki tampilan error khusus di sini jika skema tidak dikenal
                             Log.w(TAG_PHOTO_DETAIL, "URI scheme is not 'file', relying on direct URI loading: $currentUri")
                        }
                        */

                    } else {
                        Log.w(TAG_PHOTO_DETAIL, "currentUri is null for pageIndex $pageIndex")
                        Text("URI gambar tidak tersedia.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }

                // Indikator halaman
                if (displayableUris.size > 1) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(displayableUris.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
            } else {
                Log.w(TAG_PHOTO_DETAIL, "displayableUris is empty.")
                Text("Tidak ada foto untuk ditampilkan.", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}


@Preview(showBackground = true, name = "Photo Detail Screen Single")
@Composable
fun PhotoDetailScreenSinglePreview() {
    LaporanDetiTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        // Gunakan placeholder yang valid untuk preview
        val placeholderUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_image_placeholder}")

        PhotoDetailScreen(
            navController = navController,
            initialPhotoUri = placeholderUri,
            photoUris = listOf( // Contoh dengan beberapa URI untuk menguji pager di preview jika diinginkan
                placeholderUri,
                Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_broken_image}") // Contoh URI lain
            ),
            initialIndex = 0
        )
    }
}

@Preview(showBackground = true, name = "Photo Detail Screen Empty")
@Composable
fun PhotoDetailScreenEmptyPreview() {
    LaporanDetiTheme {
        val navController = rememberNavController()
        // Menguji kasus ketika tidak ada URI yang diberikan (seharusnya tidak terjadi jika initialPhotoUri adalah wajib)
        // Untuk tujuan preview, kita bisa memaksanya dengan URI yang mungkin "kosong" atau invalid
        // Namun, logika PhotoDetailScreen sekarang memastikan displayableUris tidak pernah kosong
        // jika initialPhotoUri valid. Untuk menguji Text "Tidak ada foto...",
        // kita perlu mensimulasikan displayableUris yang kosong, yang sulit dicapai
        // dengan implementasi saat ini tanpa mengubah logika utama hanya untuk preview.
        // Cara terbaik adalah menguji dengan URI yang valid namun menunjuk ke file yang tidak ada
        // dan melihat log dari onError.

        // Untuk Preview ini, mari kita tetap gunakan placeholder agar tidak crash.
        // Kita tahu bahwa jika displayableUris kosong, teks "Tidak ada foto..." akan muncul.
        val context = LocalContext.current
        val placeholderUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_image_placeholder}")

        PhotoDetailScreen(
            navController = navController,
            initialPhotoUri = placeholderUri, // Wajib ada
            photoUris = emptyList(), // Mensimulasikan tidak ada URI tambahan
            initialIndex = 0
        )
    }
}
