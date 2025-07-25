package com.example.laporandeti.ui.components // Pastikan package ini sesuai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook // Ikon buku untuk Laporan
import androidx.compose.material.icons.filled.PhotoCamera // Ikon untuk Kamera
import androidx.compose.material.icons.filled.PhotoLibrary // Ikon untuk Galeri
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.laporandeti.AppScreenRoutes // Impor definisi rute Anda

/**
 * Data class untuk merepresentasikan item di BottomNavigationBar.
 * @param label Teks yang akan ditampilkan untuk item.
 * @param icon Ikon yang akan ditampilkan untuk item.
 * @param routePattern Pola rute yang terkait dengan item ini, digunakan untuk menentukan apakah item terpilih.
 *                     Ini harus cocok dengan pola rute yang didefinisikan di AppScreenRoutes (misalnya, AppScreenRoutes.Home atau AppScreenRoutes.Gallery).
 * @param navigationTarget Rute aktual yang akan dinavigasi saat item diklik.
 *                         Untuk rute sederhana, ini akan sama dengan routePattern.
 *                         Untuk rute yang memerlukan argumen default (seperti Gallery non-picker), ini bisa menjadi rute yang dibangun.
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val routePattern: String,
    val navigationTarget: String
)

@Composable
fun MyBottomNavigationBar(
    navController: NavController,
    currentRoutePattern: String?, // Menerima pola rute saat ini (misalnya, "home_screen" atau "gallery_screen_base?isPickerMode={isPickerMode}")
    onLaporanButtonClick: () -> Unit
) {
    val items = listOf(
        BottomNavItem(
            label = "Beranda",
            icon = Icons.Filled.Home,
            routePattern = AppScreenRoutes.Home, // Pola rute untuk perbandingan
            navigationTarget = AppScreenRoutes.Home // Target navigasi
        ),
        BottomNavItem(
            label = "Laporan",
            icon = Icons.Filled.MenuBook,
            // Untuk "Laporan", routePattern adalah Feature1 karena tombol ini seharusnya
            // mengarahkan ke Feature1Screen, dan onLaporanButtonClick menangani navigasinya.
            routePattern = AppScreenRoutes.Feature1,
            navigationTarget = AppScreenRoutes.Feature1 // Meskipun navigasi ditangani oleh callback, ini untuk konsistensi
        ),
        BottomNavItem(
            label = "Kamera",
            icon = Icons.Filled.PhotoCamera,
            routePattern = AppScreenRoutes.Camera,
            navigationTarget = AppScreenRoutes.Camera
        ),
        BottomNavItem(
            label = "Galeri",
            icon = Icons.Filled.PhotoLibrary,
            routePattern = AppScreenRoutes.Gallery, // Pola rute Gallery (termasuk placeholder argumen)
            // Saat mengklik "Galeri" dari bottom bar, kita ingin membuka mode non-picker.
            navigationTarget = AppScreenRoutes.galleryScreenWithArgs(isPickerMode = false)
        )
    )

    NavigationBar {
        items.forEach { item ->
            // Logika seleksi item yang diperbarui:
            // Item terpilih jika `currentRoutePattern` cocok dengan `item.routePattern`.
            // Ini menangani rute sederhana (Home, Camera, Feature1) dan rute dengan argumen
            // seperti Gallery, karena `currentRoutePattern` dari `navBackStackEntry.destination.route`
            // akan menjadi pola rute (misalnya, "gallery_screen_base?isPickerMode={isPickerMode}").
            val isSelected = currentRoutePattern == item.routePattern

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (item.label == "Laporan") { // Atau cek: item.routePattern == AppScreenRoutes.Feature1
                        onLaporanButtonClick() // Panggil callback khusus untuk Laporan
                    } else {
                        // Navigasi ke item.navigationTarget
                        // Hanya navigasi jika belum terpilih untuk menghindari tumpukan navigasi yang sama
                        if (!isSelected) {
                            navController.navigate(item.navigationTarget) {
                                // Pop up ke start destination dari graph untuk menghindari penumpukan
                                // destinasi di back stack saat pengguna memilih item.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Hindari beberapa salinan dari destinasi yang sama saat
                                // memilih ulang item yang sama.
                                launchSingleTop = true
                                // Kembalikan state saat memilih ulang item yang dipilih sebelumnya.
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyBottomNavigationBarPreview() {
    val navController = rememberNavController()
    // Untuk preview, kita bisa simulasikan beberapa rute
    MyBottomNavigationBar(
        navController = navController,
        currentRoutePattern = AppScreenRoutes.Home, // Contoh: rute Home aktif
        onLaporanButtonClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MyBottomNavigationBarGallerySelectedPreview() {
    val navController = rememberNavController()
    MyBottomNavigationBar(
        navController = navController,
        currentRoutePattern = AppScreenRoutes.Gallery, // Contoh: rute Gallery aktif
        onLaporanButtonClick = {}
    )
}
