package com.example.laporandeti // Ganti dengan package Anda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.laporandeti.ui.components.MyBottomNavigationBar // Pastikan ini diimpor
import com.example.laporandeti.ui.theme.LaporanDetiTheme
// import com.example.laporandeti.AppNavGraph // Pastikan ini diimpor jika di file terpisah
import com.example.laporandeti.AppScreenRoutes

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaporanDetiTheme {
                val navController = rememberNavController()
                var currentScreenTitle by rememberSaveable { mutableStateOf("Beranda") }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoutePattern = navBackStackEntry?.destination?.route
                val currentRouteArgs = navBackStackEntry?.arguments

                LaunchedEffect(currentRoutePattern, currentRouteArgs) {
                    Log.d("MainActivity", "Current Route Pattern: $currentRoutePattern")
                    // Log.d("MainActivity", "Current Route Arguments: $currentRouteArgs")

                    currentScreenTitle = when (currentRoutePattern) { // Menggunakan when expression utama
                        AppScreenRoutes.Home -> "Beranda"
                        AppScreenRoutes.Feature1 -> "Pilih Laporan"
                        AppScreenRoutes.Camera -> "Kamera"
                        // Untuk rute yang mungkin memiliki argumen, lebih baik menggunakan startsWith
                        // atau mencocokkan dengan pola yang didefinisikan di AppScreenRoutes
                        // yang sudah menyertakan placeholder argumen jika itu yang dikembalikan oleh
                        // navBackStackEntry?.destination?.route
                        // Jika navBackStackEntry?.destination?.route mengembalikan POLA rute:
                        AppScreenRoutes.Gallery -> "Galeri Foto" // Jika currentRoutePattern adalah AppScreenRoutes.Gallery
                        AppScreenRoutes.Feature1WebView -> {     // Jika currentRoutePattern adalah AppScreenRoutes.Feature1WebView
                            val titleArg = currentRouteArgs?.getString(AppScreenRoutes.REPORT_TITLE_ARG)
                            titleArg ?: "Detail Laporan"
                        }
                        AppScreenRoutes.PhotoDetail -> "Detail Foto" // Jika currentRoutePattern adalah AppScreenRoutes.PhotoDetail
                        // Blok else ini untuk menangani kasus di mana currentRoutePattern mungkin
                        // tidak sama persis dengan konstanta (misalnya, jika berisi nilai argumen yang diisi)
                        // atau jika Anda ingin mencocokkan berdasarkan pola dasar.
                        else -> {
                            when {
                                currentRoutePattern?.startsWith(AppScreenRoutes.GalleryBase) == true -> "Galeri Foto"
                                currentRoutePattern?.startsWith(AppScreenRoutes.Feature1WebViewBase) == true -> {
                                    val titleArg = currentRouteArgs?.getString(AppScreenRoutes.REPORT_TITLE_ARG) // PERBAIKAN DI SINI
                                    titleArg ?: "Detail Laporan"
                                }
                                currentRoutePattern?.startsWith(AppScreenRoutes.PhotoDetailBase) == true -> "Detail Foto"
                                else -> "Laporan Deti" // Judul default jika tidak ada yang cocok
                            }
                        }
                    }
                    Log.d("MainActivity", "Updated Screen Title: $currentScreenTitle")
                }

                // Daftar pola dasar rute di mana BottomBar seharusnya TIDAK ditampilkan
                val routesWithoutBottomBar = listOf(
                    AppScreenRoutes.Camera,
                    AppScreenRoutes.Feature1WebViewBase,
                    AppScreenRoutes.PhotoDetailBase
                )

                // Daftar pola dasar rute di mana TopAppBar default TIDAK ditampilkan
                val routesWithoutDefaultTopBar = listOf(
                    AppScreenRoutes.Camera,
                    AppScreenRoutes.Feature1WebViewBase,
                    AppScreenRoutes.PhotoDetailBase,
                    AppScreenRoutes.GalleryBase // GalleryScreen akan punya TopAppBar sendiri
                )

                // Mencocokkan currentRoutePattern dengan daftar pola dasar
                val showBottomBar = routesWithoutBottomBar.none { baseRouteToExclude ->
                    currentRoutePattern?.startsWith(baseRouteToExclude) == true
                }

                val showDefaultTopBar = routesWithoutDefaultTopBar.none { baseRouteToExclude ->
                    currentRoutePattern?.startsWith(baseRouteToExclude) == true
                }

                Scaffold(
                    topBar = {
                        if (showDefaultTopBar) {
                            TopAppBar(
                                title = { Text(currentScreenTitle) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            MyBottomNavigationBar( // Pastikan MyBottomNavigationBar diimpor
                                navController = navController,
                                currentRoutePattern = currentRoutePattern, // Kirim pola rute lengkap
                                onLaporanButtonClick = {
                                    navController.navigate(AppScreenRoutes.Feature1) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavGraph( // Pastikan AppNavGraph diimpor atau didefinisikan
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LaporanDetiTheme {
        Text("Preview Aplikasi Laporan Deti")
    }
}

// Pastikan AppNavGraph dan MyBottomNavigationBar didefinisikan dan diimpor dengan benar.
// MyBottomNavigationBar mungkin perlu disesuaikan cara mencocokkan rute Galeri jika sebelumnya
// bergantung pada argumen. Sekarang, pencocokan dengan AppScreenRoutes.GalleryBase sudah cukup.
