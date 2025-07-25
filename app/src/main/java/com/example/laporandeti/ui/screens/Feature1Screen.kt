package com.example.laporandeti.ui.screens

import android.net.Uri // Diperlukan untuk Uri.encode
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController


import com.example.laporandeti.AppScreenRoutes
import com.example.laporandeti.LaporanViewModel

// Jika LaporanViewModel ada di package lain, impor juga
// import com.example.laporandeti.viewmodel.LaporanViewModel


// Link Google Form Anda - PASTIKAN INI ADA DAN BENAR
const val FORM_URL_PIKET = "https://docs.google.com/forms/d/e/1FAIpQLScMdzW2pLGpVWf936J0xKy5FkobWH-g39vqSS9kehTrhoy4Cw/viewform?usp=header"
const val FORM_URL_KEGIATAN = "URL_FORM_KEGIATAN_ANDA" // GANTI DENGAN URL SEBENARNYA




@Composable
fun Feature1Screen(
    navController: NavController,
    laporanViewModel: LaporanViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Pilih Jenis Laporan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ReportTypeButton( // <--- Pemanggilan ReportTypeButton
            reportName = "Laporan Piket",
            onClick = {
                Log.d("Feature1Screen", "Tombol Laporan Piket diklik! URL: $FORM_URL_PIKET")
                laporanViewModel.setLastSelectedReportTypeUrl(FORM_URL_PIKET)
                val reportTitle = "Laporan Piket"
                navController.navigate("feature1_webview_base/${Uri.encode("Laporan Piket")}")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ReportTypeButton( // <--- Pemanggilan ReportTypeButton
            reportName = "Laporan Kegiatan",
            onClick = {
                if (FORM_URL_KEGIATAN == "URL_FORM_KEGIATAN_ANDA") {
                    Log.e("Feature1Screen", "URL Form Kegiatan belum diatur!")
                    return@ReportTypeButton
                }
                Log.d("Feature1Screen", "Tombol Laporan Kegiatan diklik! URL: $FORM_URL_KEGIATAN")
                laporanViewModel.setLastSelectedReportTypeUrl(FORM_URL_KEGIATAN)
                val reportTitle = "Laporan Kegiatan"
                navController.navigate("${AppScreenRoutes.Feature1WebView}/${Uri.encode(reportTitle)}")
            }
        )
    }
}


@Composable
fun ReportTypeButton(
    reportName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(text = reportName, fontSize = 16.sp)
    }
}
// !! --------------------------------------------- !!
@Preview(showBackground = true, name = "Feature 1 Screen Preview")
@Composable
fun Feature1ScreenPreview() {
    val navController = rememberNavController()
    Feature1Screen(navController = navController)
}

@Preview(showBackground = true, name = "Report Type Button Preview")
@Composable
fun ReportTypeButtonPreview() {
    ReportTypeButton(reportName = "Contoh Laporan", onClick = {})
}