package com.example.laporandeti.ui.screens



import android.net.Uri

import android.util.Log

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

// Hapus import androidx.compose.foundation.lazy.grid.items yang lama jika hanya menggunakan paging items

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

import com.example.laporandeti.R

import com.example.laporandeti.data.model.MediaStoreImage // Impor data class

import com.example.laporandeti.ui.gallery.GalleryViewModel // Impor ViewModel

import com.example.laporandeti.ui.theme.LaporanDetiTheme

import com.example.laporandeti.util.APP_PUBLIC_IMAGE_SUBFOLDER // Impor konstanta



private const val TAG_GALLERY = "GalleryScreen"



// Fungsi getImageUrisFromAppDirectory sudah tidak diperlukan di sini lagi

// karena logika pemuatan gambar dipindahkan ke ViewModel dan PagingSource



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun GalleryScreen(

    navController: NavController,

    galleryViewModel: GalleryViewModel = viewModel(), // Injeksi ViewModel

    isPickerMode: Boolean = false

) {

    val galleryViewModel: GalleryViewModel = viewModel()

    val appImages = galleryViewModel.appImages.collectAsLazyPagingItems()



    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text(if (isPickerMode) "Pilih Foto Laporan" else "Galeri Foto") },

                navigationIcon = {

                    IconButton(onClick = {

                        if (isPickerMode) {

                            Log.d(TAG_GALLERY, "Picker Mode: Navigating back, selection cancelled.")

                            navController.previousBackStackEntry

                                ?.savedStateHandle

                                ?.set<String?>(AppScreenRoutes.SELECTED_IMAGE_URI_RESULT_KEY, null)

                        }

                        navController.popBackStack()

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

        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(paddingValues)

        ) {

// Handle Paging LoadStates

            when {

// Initial Load (Refresh)

                appImages.loadState.refresh is LoadState.Loading -> {

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        CircularProgressIndicator()

                        Log.d(TAG_GALLERY, "Paging Refresh: Loading...")

                    }

                }

                appImages.loadState.refresh is LoadState.Error -> {

                    val error = (appImages.loadState.refresh as LoadState.Error).error

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        Text(

                            "Gagal memuat gambar: ${error.localizedMessage}",

                            textAlign = TextAlign.Center,

                            modifier = Modifier.padding(16.dp)

                        )

                        Log.e(TAG_GALLERY, "Paging Refresh Error: ${error.localizedMessage}", error)

                    }

                }

// Initial load selesai dan tidak ada item (setelah append juga selesai)

                appImages.loadState.refresh is LoadState.NotLoading &&

                        appImages.loadState.append.endOfPaginationReached &&

                        appImages.itemCount == 0 -> {

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        Text(

                            if (isPickerMode) "Tidak ada foto laporan yang bisa dipilih dari folder '$APP_PUBLIC_IMAGE_SUBFOLDER'."

                            else "Tidak ada gambar ditemukan di folder '$APP_PUBLIC_IMAGE_SUBFOLDER'.",

                            fontSize = 18.sp,

                            textAlign = TextAlign.Center,

                            modifier = Modifier.padding(16.dp)

                        )

                        Log.d(TAG_GALLERY, "Paging: No items found in '$APP_PUBLIC_IMAGE_SUBFOLDER'.")

                    }

                }

// Data tersedia

                else -> {

                    LazyVerticalGrid(

                        columns = GridCells.Fixed(3), // Atau GridCells.Adaptive(minSize = 120.dp)

                        contentPadding = PaddingValues(4.dp),

                        verticalArrangement = Arrangement.spacedBy(4.dp),

                        horizontalArrangement = Arrangement.spacedBy(4.dp),

                        modifier = Modifier.fillMaxSize()

                    ) {

                        items(

                            count = appImages.itemCount,

// key = { index -> appImages.peek(index)?.id ?: index } // Gunakan ID unik dari MediaStoreImage

// Atau jika itemSnapshotList tidak null

                            key = { index -> appImages.peek(index)?.contentUri?.toString() ?: index.toString() }



                        ) { index ->

                            val image = appImages[index] // Bisa null jika placeholder aktif

                            image?.let { mediaStoreImage ->

                                GalleryImageItem(

                                    mediaStoreImage = mediaStoreImage,

                                    onClick = {

                                        if (isPickerMode) {

                                            Log.d(TAG_GALLERY, "Picker Mode: Image selected: ${mediaStoreImage.contentUri}")

                                            navController.previousBackStackEntry

                                                ?.savedStateHandle

                                                ?.set(AppScreenRoutes.SELECTED_IMAGE_URI_RESULT_KEY, mediaStoreImage.contentUri.toString())

                                            navController.popBackStack()

                                        } else {

                                            Log.d(TAG_GALLERY, "Browse Mode: Image clicked: ${mediaStoreImage.contentUri}")

// TODO: Kumpulkan daftar URI dari appImages.snapshot().items jika perlu

// Untuk navigasi ke detail, Anda perlu cara yang lebih baik untuk mendapatkan daftar URI

// karena PagingData dimuat secara bertahap.

// Untuk sementara, kita hanya akan mengirim URI yang diklik.

// Atau, jika Anda selalu ingin PhotoDetailScreen bisa swipe,

// Anda perlu mengambil seluruh daftar URI (mungkin tidak dari PagingData secara langsung untuk navigasi)

// atau memuat ulang data di PhotoDetailScreen.



                                            val encodedClickedPhotoUri = Uri.encode(mediaStoreImage.contentUri.toString())

// Untuk photoListStringForNav dan currentPhotoIndex dengan paging, ini menjadi lebih kompleks.

// Salah satu cara adalah dengan hanya mengirim URI yang dipilih ke detail,

// dan detail akan memuat ulang daftar jika perlu swipe, atau hanya menampilkan satu gambar.

// Untuk contoh sederhana, kita modifikasi agar hanya mengirim URI tunggal.

                                            val routeToDetail = AppScreenRoutes.photoDetailWithArgs(

                                                photoUri = encodedClickedPhotoUri,

                                                photoListJson = null, // Atau hanya URI saat ini jika detail diubah

                                                currentPhotoIndex = 0 // Atau -1 jika tidak ada list

                                            )

                                            Log.d(TAG_GALLERY, "Navigasi ke: $routeToDetail")

                                            navController.navigate(routeToDetail)

                                        }

                                    }

                                )

                            }

                        }



// Handle Append (Loading more items)

                        item {

                            if (appImages.loadState.append is LoadState.Loading) {

                                Box(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(16.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    CircularProgressIndicator()

                                    Log.d(TAG_GALLERY, "Paging Append: Loading more...")

                                }

                            }

                        }

                        item {

                            if (appImages.loadState.append is LoadState.Error) {

                                val error = (appImages.loadState.append as LoadState.Error).error

                                Box(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(16.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Text("Error: ${error.localizedMessage}")

                                    Log.e(TAG_GALLERY, "Paging Append Error: ${error.localizedMessage}", error)

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

fun GalleryImageItem(mediaStoreImage: MediaStoreImage, onClick: () -> Unit) {

    AsyncImage(

        model = ImageRequest.Builder(LocalContext.current)

            .data(mediaStoreImage.contentUri) // Gunakan contentUri dari MediaStoreImage

            .crossfade(true)

            .placeholder(R.drawable.ic_image_placeholder)

            .error(R.drawable.ic_broken_image)

            .build(),

        contentDescription = mediaStoreImage.displayName, // Gunakan displayName

        contentScale = ContentScale.Crop,

        modifier = Modifier

            .aspectRatio(1f)

            .background(MaterialTheme.colorScheme.surfaceVariant)

            .clickable(onClick = onClick)

    )

}



@Preview(showBackground = true, name = "Gallery Screen Browse Mode")

@Composable

fun GalleryScreenBrowsePreview() {

    LaporanDetiTheme {

// Untuk preview, ViewModel tidak akan benar-benar memuat data dari MediaStore.

// Anda mungkin perlu membuat mock ViewModel atau data PagingData palsu untuk preview yang lebih baik.

// Cara sederhana adalah membiarkannya menampilkan state loading atau empty.

        GalleryScreen(navController = rememberNavController(), isPickerMode = false)

    }

}



@Preview(showBackground = true, name = "Gallery Screen Picker Mode")

@Composable

fun GalleryScreenPickerPreview() {

    LaporanDetiTheme {

        GalleryScreen(navController = rememberNavController(), isPickerMode = true)

    }

}



// Preview untuk kondisi kosong menjadi lebih sulit dengan Paging,

// karena state kosong dikelola oleh PagingSource.

// Anda bisa membuat ViewModel palsu khusus untuk preview jika ini penting.