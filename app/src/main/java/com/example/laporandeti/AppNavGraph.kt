// File: AppNavGraph.kt
package com.example.laporandeti

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination // Import ini

// Impor Composable layar Anda
import com.example.laporandeti.ui.screens.PhotoDetailScreen
import com.example.laporandeti.ui.screens.CameraScreen
import com.example.laporandeti.ui.screens.GalleryScreen
import com.example.laporandeti.ui.screens.WebViewScreen
import com.example.laporandeti.ui.screens.HomeScreen
import com.example.laporandeti.ui.screens.Feature1Screen
// Impor konstanta argumen dan AppScreenRoutes jika belum
import com.example.laporandeti.AppScreenRoutes
import com.example.laporandeti.CURRENT_PHOTO_INDEX_ARG
import com.example.laporandeti.PHOTO_LIST_ARG
import com.example.laporandeti.PHOTO_URI_ARG
import com.example.laporandeti.REPORT_TITLE_ARG


@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = AppScreenRoutes.Home
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
            route = AppScreenRoutes.Feature1WebView,
            arguments = listOf(
                navArgument(REPORT_TITLE_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportTitle = backStackEntry.arguments?.getString(REPORT_TITLE_ARG)
            val laporanViewModel: com.example.laporandeti.LaporanViewModel = viewModel()
            val urlToLoad = laporanViewModel.lastSelectedReportTypeUrl.value


            if (urlToLoad != null) {
                WebViewScreen(
                    navController = navController,
                    urlToLoad = urlToLoad,
                    screenTitle = reportTitle ?: "Laporan",
                    onPageFinished = { currentLoadedUrl ->
                        Log.i("AppNavGraph", "WebView page finished loading: $currentLoadedUrl")
                    },
                    onShouldOverrideUrlLoading = { requestedUrl ->
                        Log.i("AppNavGraph", "WebView trying to load: $requestedUrl")
                        false
                    },
                    onBackButtonClicked = { // <--- Inilah perubahannya
                        // Kembali ke Feature1Screen dan hapus semua rute di atasnya
                        navController.navigate(AppScreenRoutes.Feature1) {
                            popUpTo(AppScreenRoutes.Feature1) {
                                inclusive = true // Hapus juga Feature1 dari back stack jika sudah ada
                                saveState = false // Tidak perlu menyimpan state Feature1
                            }
                            launchSingleTop = true // Hindari membuat instance baru jika sudah ada di atas
                        }
                    }
                )
            } else {
                Log.e("AppNavGraph", "URL untuk WebViewScreen adalah null! Navigasi kembali.")
                navController.popBackStack()
            }
        }

        composable(AppScreenRoutes.Camera) {
            CameraScreen(navController = navController)
        }

        composable(
            route = AppScreenRoutes.Gallery,

        ) { backStackEntry ->
            GalleryScreen(
                navController = navController
            )
        }

        composable(
            route = AppScreenRoutes.PhotoDetail,
            arguments = listOf(
                navArgument(PHOTO_URI_ARG) { type = NavType.StringType },
                navArgument(PHOTO_LIST_ARG) { type = NavType.StringType; nullable = true },
                navArgument(CURRENT_PHOTO_INDEX_ARG) { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val encodedInitialPhotoUriString = backStackEntry.arguments?.getString(PHOTO_URI_ARG)
            val encodedPhotoListJsonString = backStackEntry.arguments?.getString(PHOTO_LIST_ARG)
            val currentPhotoIndex = backStackEntry.arguments?.getInt(CURRENT_PHOTO_INDEX_ARG) ?: 0

            if (encodedInitialPhotoUriString != null) {
                val decodedInitialPhotoUriString = try { Uri.decode(encodedInitialPhotoUriString) } catch (e: Exception) { Log.e("AppNavGraph", "Decode URI gagal", e); null }
                val initialPhotoUri = decodedInitialPhotoUriString?.let { try { Uri.parse(it) } catch (e: Exception) { Log.e("AppNavGraph", "Parse URI gagal", e); null } }

                val photoUrisList: List<Uri>? = encodedPhotoListJsonString?.let { encodedJson ->
                    try {
                        val decodedJsonString = Uri.decode(encodedJson)
                        decodedJsonString.split(",").mapNotNull { encodedUriStringItem ->
                            try { Uri.parse(Uri.decode(encodedUriStringItem.trim())) } catch (e: Exception) { Log.e("AppNavGraph", "Decode/Parse item list URI gagal", e); null }
                        }.takeIf { it.isNotEmpty() }
                    } catch (e: Exception) { Log.e("AppNavGraph", "Proses JSON list URI gagal", e); null }
                }

                if (initialPhotoUri != null) {
                    PhotoDetailScreen(
                        navController = navController,
                        initialPhotoUri = initialPhotoUri,
                        photoUris = photoUrisList,
                        initialIndex = currentPhotoIndex
                    )
                } else {
                    Log.e("AppNavGraph", "initialPhotoUri null untuk PhotoDetail. Arg: $encodedInitialPhotoUriString")
                    navController.popBackStack()
                }
            } else {
                Log.e("AppNavGraph", "Argumen PHOTO_URI_ARG hilang untuk PhotoDetail.")
                navController.popBackStack()
            }
        }
    }
}