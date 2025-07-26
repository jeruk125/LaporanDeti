package com.example.laporandeti.ui.screens // Sesuaikan package Anda

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.laporandeti.AppScreenRoutes // Pastikan ini benar
import com.example.laporandeti.R // Pastikan drawable placeholder ada
import com.example.laporandeti.data.model.MediaStoreImage
import com.example.laporandeti.ui.gallery.GalleryViewModel
import com.example.laporandeti.ui.theme.LaporanDetiTheme
// import com.example.laporandeti.util.APP_PUBLIC_IMAGE_SUBFOLDER // Jika masih digunakan untuk pesan

private const val TAG_GALLERY = "GalleryScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val appImages = galleryViewModel.appImages.collectAsLazyPagingItems()
    // val context = LocalContext.current // Tidak digunakan secara langsung di sini lagi

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galeri Foto") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val refreshState = appImages.loadState.refresh) {
                is LoadState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Log.d(TAG_GALLERY, "Paging Refresh: Loading...")
                    }
                }
                is LoadState.Error -> {
                    val error = refreshState.error
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Gagal memuat gambar: ${error.localizedMessage}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                        Log.e(TAG_GALLERY, "Paging Refresh Error", error)
                    }
                }
                is LoadState.NotLoading -> {
                    if (appImages.itemCount == 0 && appImages.loadState.append.endOfPaginationReached) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Tidak ada gambar ditemukan di folder aplikasi.",
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            Log.d(TAG_GALLERY, "Paging: No items found.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 128.dp),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Handle Prepend state
                            appImages.loadState.prepend.let { prependState ->
                                when (prependState) {
                                    is LoadState.Loading -> {
                                        item(key = "prepend_loading_indicator", contentType = "loading_indicator") {
                                            CenteredItemContainer { CircularProgressIndicator() }
                                            Log.d(TAG_GALLERY, "Paging Prepend: Loading more...")
                                        }
                                    }
                                    is LoadState.Error -> {
                                        item(key = "prepend_error_message", contentType = "error_message") {
                                            CenteredItemContainer {
                                                Text("Error memuat sebelumnya", color = MaterialTheme.colorScheme.error)
                                            }
                                            Log.e(TAG_GALLERY, "Paging Prepend Error", prependState.error)
                                        }
                                    }
                                    else -> Unit
                                }
                            }

                            // Image items
                            items(
                                count = appImages.itemCount,
                                key = { index ->
                                    val image = appImages.peek(index)
                                    image?.let { "image_${it.id}" } ?: "placeholder_$index"
                                },
                                contentType = { index ->
                                    appImages.peek(index)?.let { "image_item" } ?: "placeholder_item"
                                }
                            ) { index ->
                                val image = appImages[index]
                                image?.let { mediaStoreImage ->
                                    GalleryImageItem(
                                        mediaStoreImage = mediaStoreImage,
                                        onClick = {
                                            Log.d(TAG_GALLERY, "Image clicked: ${mediaStoreImage.contentUri}")
                                            // Menggunakan photoUri mentah (belum di-encode)
                                            val clickedPhotoUriString = mediaStoreImage.contentUri.toString()

                                            // Jika PhotoDetailScreen tidak membutuhkan photoListJson atau currentPhotoIndex
                                            // dari GalleryScreen, Anda bisa mengirimkan null atau nilai default.
                                            val routeToDetail = AppScreenRoutes.photoDetailWithArgs(
                                                photoUri = clickedPhotoUriString, // Akan di-encode di dalam photoDetailWithArgs
                                                photoListJson = null, // Atau logika untuk mendapatkan list jika diperlukan
                                                currentPhotoIndex = null // Atau 0 jika selalu ada satu item
                                            )
                                            Log.d(TAG_GALLERY, "Navigating to: $routeToDetail")
                                            navController.navigate(routeToDetail)
                                        }
                                    )
                                }
                            }

                            // Handle Append state
                            appImages.loadState.append.let { appendState ->
                                when (appendState) {
                                    is LoadState.Loading -> {
                                        item(key = "append_loading_indicator", contentType = "loading_indicator") {
                                            CenteredItemContainer { CircularProgressIndicator() }
                                            Log.d(TAG_GALLERY, "Paging Append: Loading more...")
                                        }
                                    }
                                    is LoadState.Error -> {
                                        item(key = "append_error_message", contentType = "error_message") {
                                            CenteredItemContainer {
                                                Text("Error memuat berikutnya", color = MaterialTheme.colorScheme.error)
                                            }
                                            Log.e(TAG_GALLERY, "Paging Append Error", appendState.error)
                                        }
                                    }
                                    is LoadState.NotLoading -> {
                                        if (appendState.endOfPaginationReached && appImages.itemCount > 0) {
                                            item(key = "end_of_list_message", contentType = "end_of_list_message") {
                                                CenteredItemContainer { Text("Semua gambar telah dimuat.") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredItemContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GalleryImageItem(mediaStoreImage: MediaStoreImage, onClick: () -> Unit) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(mediaStoreImage.contentUri)
            .crossfade(true)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .size(384) // Sesuaikan ukuran
            .build(),
        contentDescription = mediaStoreImage.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, name = "Gallery Screen Preview")
@Composable
fun GalleryScreenPreview() {
    LaporanDetiTheme {
        GalleryScreen(navController = rememberNavController())
    }
}
