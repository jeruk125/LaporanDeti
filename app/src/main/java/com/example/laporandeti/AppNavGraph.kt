// File: AppNavGraph.kt
package com.example.laporandeti

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // Pastikan LaporanViewModel diimpor jika digunakan
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// import androidx.navigation.NavGraph.Companion.findStartDestination // Tidak digunakan secara langsung di sini

// Impor Composable layar Anda
import com.example.laporandeti.ui.screens.PhotoDetailScreen
import com.example.laporandeti.ui.screens.CameraScreen
import com.example.laporandeti.ui.screens.GalleryScreen
import com.example.laporandeti.ui.screens.WebViewScreen
import com.example.laporandeti.ui.screens.HomeScreen
import com.example.laporandeti.ui.screens.Feature1Screen
// Impor konstanta argumen dan AppScreenRoutes sudah ada di atas

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = AppScreenRoutes.Home // Default ke Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppScreenRoutes.Home) {
            HomeScreen(navController = navController)
        }

        composable(AppScreenRoutes.Feature1) {
            Feature1Screen(navController = navController)
        }

        composable(
            route = AppScreenRoutes.Feature1WebView, // Menggunakan definisi rute dari AppScreenRoutes
            arguments = listOf(
                navArgument(REPORT_TITLE_ARG) {
                    type = NavType.StringType
                    // nullable = false // Default, title diharapkan ada
                }
            )
        ) { backStackEntry ->
            // Mengambil reportTitle yang sudah di-decode secara otomatis oleh Navigation Compose
            val reportTitle = backStackEntry.arguments?.getString(REPORT_TITLE_ARG)
            val laporanViewModel: com.example.laporandeti.LaporanViewModel = viewModel() // Pastikan ViewModel ini ada
            val urlToLoad = laporanViewModel.lastSelectedReportTypeUrl.value // Asumsi ini masih relevan

            if (urlToLoad != null) {
                WebViewScreen(
                    navController = navController,
                    urlToLoad = urlToLoad,
                    screenTitle = reportTitle ?: "Detail Laporan", // Fallback jika title null
                    onPageFinished = { currentLoadedUrl ->
                        Log.i("AppNavGraph", "WebView page finished loading: $currentLoadedUrl")
                    },
                    onShouldOverrideUrlLoading = { requestedUrl ->
                        Log.i("AppNavGraph", "WebView trying to load: $requestedUrl")
                        false // Biarkan WebView menangani semua URL kecuali ada logika khusus
                    },
                    onBackButtonClicked = {
                        // Kembali ke Feature1Screen dan hapus semua rute di atasnya
                        navController.navigate(AppScreenRoutes.Feature1) {
                            popUpTo(AppScreenRoutes.Feature1) { // Pop up ke Feature1
                                inclusive = true // Hapus juga Feature1 dari back stack jika sudah ada di atasnya
                            }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                Log.e("AppNavGraph", "URL untuk WebViewScreen adalah null! Navigasi kembali.")
                navController.popBackStack() // Kembali jika URL tidak ada
            }
        }

        composable(AppScreenRoutes.Camera) {
            CameraScreen(navController = navController)
        }

        // GalleryScreen sekarang tidak lagi memiliki argumen mode picker
        composable(
            route = AppScreenRoutes.Gallery // Menggunakan AppScreenRoutes.Gallery yang sudah disederhanakan
        ) {
            // Tidak ada argumen yang perlu diambil dari backStackEntry untuk GalleryScreen
            GalleryScreen(
                navController = navController
            )
        }

        composable(
            route = AppScreenRoutes.PhotoDetail, // Menggunakan definisi rute dari AppScreenRoutes
            arguments = listOf(
                navArgument(PHOTO_URI_ARG) { type = NavType.StringType }, // Argumen path, wajib ada
                navArgument(PHOTO_LIST_ARG) {
                    type = NavType.StringType
                    nullable = true // Argumen query, bisa null
                },
                navArgument(CURRENT_PHOTO_INDEX_ARG) {
                    type = NavType.IntType
                    defaultValue = -1 // Default jika tidak ada (misalnya, jika hanya 1 foto)
                    // Atau 0 jika selalu dimulai dari foto pertama dalam daftar
                    nullable = true // Argumen query, bisa null atau menggunakan default
                }
            )
        ) { backStackEntry ->
            // URI dan JSON string sudah di-decode secara otomatis oleh Navigation Compose
            // saat diambil dari arguments Bundle jika dikirim melalui NavController.navigate()
            // dan argumen didefinisikan dengan benar di composable().
            // Yang perlu di-decode manual adalah jika Anda membangun string URI secara manual
            // dengan nilai yang mengandung karakter khusus.
            val encodedPhotoUriString = backStackEntry.arguments?.getString(PHOTO_URI_ARG)
            val encodedPhotoListJsonString = backStackEntry.arguments?.getString(PHOTO_LIST_ARG)
            // Jika Anda set defaultValue untuk IntType dan nullable=true,
            // getInt akan mengembalikan defaultValue jika argumen tidak ada atau null.
            val currentPhotoIndex = backStackEntry.arguments?.getInt(CURRENT_PHOTO_INDEX_ARG) ?: -1


            if (encodedPhotoUriString != null) {
                val initialPhotoUri = try {
                    Uri.parse(Uri.decode(encodedPhotoUriString)) // Decode terlebih dahulu
                } catch (e: Exception) {
                    Log.e("AppNavGraph", "Gagal parse atau decode PHOTO_URI_ARG: $encodedPhotoUriString", e)
                    null
                }

                val photoUrisList: List<Uri>? = encodedPhotoListJsonString?.let { encodedJson ->
                    try {
                        val decodedJsonString = Uri.decode(encodedJson) // Decode JSON string
                        // Asumsi format JSON adalah array string URI yang sudah di-encode
                        // atau string dipisahkan koma dari URI yang sudah di-encode
                        // Logika parsing JSON Anda mungkin perlu disesuaikan di sini
                        // Contoh sederhana jika formatnya adalah list string URI yang dipisahkan koma:
                        decodedJsonString.split(",").mapNotNull { encodedUriStringItem ->
                            try {
                                Uri.parse(Uri.decode(encodedUriStringItem.trim()))
                            } catch (e: Exception) {
                                Log.e("AppNavGraph", "Gagal parse atau decode item URI dalam list: $encodedUriStringItem", e)
                                null
                            }
                        }.takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        Log.e("AppNavGraph", "Gagal proses JSON list URI: $encodedJson", e)
                        null
                    }
                }

                if (initialPhotoUri != null) {
                    PhotoDetailScreen(
                        navController = navController,
                        initialPhotoUri = initialPhotoUri,
                        photoUris = photoUrisList, // Bisa null jika tidak ada daftar
                        initialIndex = currentPhotoIndex // Bisa -1 atau 0 tergantung logika Anda
                    )
                } else {
                    Log.e("AppNavGraph", "initialPhotoUri null setelah parsing/decoding. Arg: $encodedPhotoUriString")
                    navController.popBackStack()
                }
            } else {
                Log.e("AppNavGraph", "Argumen PHOTO_URI_ARG hilang untuk PhotoDetail.")
                navController.popBackStack()
            }
        }
    }
}
