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
import com.example.laporandeti.ui.components.MyBottomNavigationBar
import com.example.laporandeti.ui.theme.LaporanDetiTheme
// Impor AppScreenRoutes dan AppNavGraph
// Pastikan MyBottomNavigationBar juga diimpor jika berada di file lain
// import com.example.laporandeti.ui.components.MyBottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaporanDetiTheme {
                val navController = rememberNavController() // NavController dibuat di sini
                var currentScreenTitle by rememberSaveable { mutableStateOf("Beranda") }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoutePattern = navBackStackEntry?.destination?.route // Ini adalah pola rute (mis. "feature1_webview_base/{reportTitle}")
                val currentRouteArgs = navBackStackEntry?.arguments

                LaunchedEffect(currentRoutePattern, currentRouteArgs) {
                    Log.d("MainActivity", "Current Route Pattern: $currentRoutePattern")
                    currentScreenTitle = when (currentRoutePattern) {
                        AppScreenRoutes.Home -> "Beranda"
                        AppScreenRoutes.Feature1 -> "Pilih Laporan"
                        AppScreenRoutes.Camera -> "Kamera"
                        // Untuk Gallery, kita bisa membedakan berdasarkan argumen pickerMode jika perlu
                        AppScreenRoutes.Gallery -> {
                            val isPickerMode = currentRouteArgs?.getBoolean(AppScreenRoutes.GALLERY_PICKER_MODE_ARG) ?: false
                            if (isPickerMode) "Pilih Foto Laporan" else "Galeri Foto"
                        }
                        // Untuk rute dengan argumen, kita perlu mencocokkan pola dasar
                        // dan kemudian mengambil argumen jika perlu
                        AppScreenRoutes.Feature1WebView -> { // Ini adalah pola "feature1_webview_base/{reportTitle}"
                            val titleArg = currentRouteArgs?.getString(REPORT_TITLE_ARG)
                            titleArg ?: "Detail Laporan" // Default jika argumen tidak ada (seharusnya tidak terjadi jika navigasi benar)
                        }
                        AppScreenRoutes.PhotoDetail -> { // Ini adalah pola "photo_detail_base/{photoUri}?..."
                            // Anda bisa mengambil argumen tertentu dari PhotoDetail jika ingin judul yang lebih dinamis
                            "Detail Foto"
                        }
                        else -> "Laporan Deti" // Judul default
                    }
                    Log.d("MainActivity", "Updated Screen Title: $currentScreenTitle")
                }

                // Daftar pola rute di mana BottomBar seharusnya TIDAK ditampilkan
                val routesWithoutBottomBar = listOf(
                    AppScreenRoutes.Camera, // Cocok dengan "camera_screen"
                    AppScreenRoutes.Feature1WebView, // Cocok dengan "feature1_webview_base/{reportTitle}"
                    AppScreenRoutes.PhotoDetail      // Cocok dengan "photo_detail_base/{photoUri}?..."
                    // Tambahkan pola rute lain jika perlu
                )

                // Daftar pola rute di mana TopAppBar default TIDAK ditampilkan
                // (karena layar tersebut mungkin memiliki TopAppBar sendiri atau tidak memerlukannya)
                val routesWithoutDefaultTopBar = listOf(
                    AppScreenRoutes.Camera,
                    AppScreenRoutes.Feature1WebView, // Jika WebViewScreen memiliki TopAppBar sendiri
                    AppScreenRoutes.PhotoDetail,     // Jika PhotoDetailScreen memiliki TopAppBar sendiri
                    AppScreenRoutes.Gallery          // Jika GalleryScreen memiliki TopAppBar sendiri
                )

                val showBottomBar = currentRoutePattern !in routesWithoutBottomBar
                val showDefaultTopBar = currentRoutePattern !in routesWithoutDefaultTopBar

                Scaffold(
                    topBar = {
                        if (showDefaultTopBar) {
                            TopAppBar(
                                title = { Text(currentScreenTitle) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                // Tambahkan navigationIcon (misalnya tombol kembali) jika diperlukan
                                // Logika untuk tombol kembali mungkin lebih baik ditangani di TopAppBar masing-masing layar
                                // atau dengan memeriksa navBackStackEntry.destination.id != navController.graph.startDestinationId
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            MyBottomNavigationBar( // Pastikan MyBottomNavigationBar diimpor
                                navController = navController,
                                currentRoutePattern = currentRoutePattern, // Kirim pola rute
                                onLaporanButtonClick = {
                                    navController.navigate(AppScreenRoutes.Feature1) {
                                        popUpTo(navController.graph.startDestinationId) { // Gunakan startDestinationId
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
                    AppNavGraph( // Panggil AppNavGraph yang sudah dipisah
                        navController = navController, // Teruskan instance NavController
                        modifier = Modifier.padding(innerPadding)
                        // startDestination tidak perlu di-pass lagi jika default-nya sudah benar di AppNavGraph
                    )
                }
            }
        }
    }
}

// Preview (tetap sama)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LaporanDetiTheme {
        Text("Preview Aplikasi Laporan Deti")
    }
}

// Asumsi MyBottomNavigationBar.kt ada (contoh sederhana)
// Jika belum ada, buat file baru misalnya di ui/components/MyBottomNavigationBar.kt
/*
package com.example.laporandeti.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.example.laporandeti.AppScreenRoutes // Impor AppScreenRoutes

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String // Gunakan rute dasar untuk perbandingan di BottomBar
)

@Composable
fun MyBottomNavigationBar(
    navController: NavController,
    currentRoute: String?, // Sekarang menerima pola rute
    onLaporanButtonClick: () -> Unit // Tetap pertahankan ini jika logika navigasi Laporan khusus
) {
    val items = listOf(
        BottomNavItem("Beranda", Icons.Filled.Home, AppScreenRoutes.Home),
        BottomNavItem("Laporan", Icons.Filled.Description, AppScreenRoutes.Feature1), // Target rute untuk tombol Laporan
        BottomNavItem("Galeri", Icons.Filled.PhotoLibrary, AppScreenRoutes.GalleryBase) // Cocokkan dengan GalleryBase
    )

    NavigationBar {
        items.forEach { item ->
            val isSelected = when (item.route) {
                // Untuk Galeri, kita anggap terpilih jika currentRoute dimulai dengan GalleryBase
                // karena GalleryScreen sekarang bisa memiliki argumen.
                AppScreenRoutes.GalleryBase -> currentRoute?.startsWith(AppScreenRoutes.GalleryBase) == true
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (item.route == AppScreenRoutes.Feature1) {
                        onLaporanButtonClick() // Panggil lambda khusus untuk tombol Laporan
                    } else {
                        // Untuk item lain, navigasi seperti biasa
                        if (currentRoute != item.route) { // Hindari navigasi ke rute yang sama
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}
*/
