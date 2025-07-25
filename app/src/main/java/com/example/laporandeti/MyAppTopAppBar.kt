package com.example.laporandeti // Sesuaikan dengan package Anda

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Untuk ikon kembali yang mendukung RTL
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.laporandeti.ui.theme.LaporanDetiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopAppBar( // Pastikan nama dan parameter cocok persis
    currentScreenTitle: String,
    showBackButton: Boolean,
    onNavigationIconClick: () -> Unit
) {
    TopAppBar(
        title = { Text(currentScreenTitle) },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                    contentDescription = if (showBackButton) "Kembali" else "Menu Utama"
                )
            }
        }
        // Anda bisa menambahkan colors jika ingin kustomisasi lebih lanjut
        // colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        //    containerColor = MaterialTheme.colorScheme.primaryContainer,
        //    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        //    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        // )
    )
}

@Preview(showBackground = true)
@Composable
fun MyAppTopAppBarPreview() {
    LaporanDetiTheme { // Bungkus dengan tema Anda untuk preview yang lebih akurat
        MyAppTopAppBar(
            currentScreenTitle = "Judul Halaman Preview",
            showBackButton = false,
            onNavigationIconClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyAppTopAppBarWithBackPreview() {
    LaporanDetiTheme { // Bungkus dengan tema Anda
        MyAppTopAppBar(
            currentScreenTitle = "Detail Laporan Preview",
            showBackButton = true,
            onNavigationIconClick = {}
        )
    }
}
    