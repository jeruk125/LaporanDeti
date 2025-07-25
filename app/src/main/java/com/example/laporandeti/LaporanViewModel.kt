package com.example.laporandeti

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Untuk SharedPreferences (opsional, bisa diganti dengan DataStore)
private const val PREFS_NAME = "laporan_prefs"
private const val KEY_LAST_OPENED_URL = "last_opened_url"
private const val KEY_LAST_SELECTED_REPORT_TYPE_URL = "last_selected_report_type_url"

class LaporanViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    // Menyimpan URL yang sedang aktif di WebView
    private val _currentWebViewUrl = MutableLiveData<String?>(null)
    val currentWebViewUrl: LiveData<String?> = _currentWebViewUrl

    // Menyimpan URL dari jenis laporan yang terakhir dipilih pengguna
    // Ini akan digunakan untuk "kembali ke link google form bila sebelumnya sudah memilih"
    private val _lastSelectedReportTypeUrl = MutableLiveData<String?>(null)
    val lastSelectedReportTypeUrl: LiveData<String?> = _lastSelectedReportTypeUrl

    init {
        // Muat URL yang terakhir dipilih saat ViewModel dibuat
        _lastSelectedReportTypeUrl.value =
            sharedPreferences.getString(KEY_LAST_SELECTED_REPORT_TYPE_URL, null)
        Log.d(
            "LaporanViewModel",
            "Init: Loaded lastSelectedReportTypeUrl: ${_lastSelectedReportTypeUrl.value}"
        )
    }

    fun setCurrentlyOpenWebViewUrl(url: String?) {
        _currentWebViewUrl.value = url
        // Simpan juga sebagai URL yang terakhir benar-benar dibuka jika valid
        if (!url.isNullOrEmpty()) {
            saveToPreferences(KEY_LAST_OPENED_URL, url)
            Log.d("LaporanViewModel", "setCurrentlyOpenWebViewUrl: $url")
        }
    }

    fun setLastSelectedReportTypeUrl(url: String?) {
        _lastSelectedReportTypeUrl.value = url
        if (url != null) {
            saveToPreferences(KEY_LAST_SELECTED_REPORT_TYPE_URL, url)
            Log.d("LaporanViewModel", "setLastSelectedReportTypeUrl: $url")

        } else {
            // Jika dikosongkan (kembali ke daftar laporan), hapus juga dari prefs
            sharedPreferences.edit().remove(KEY_LAST_SELECTED_REPORT_TYPE_URL).apply()
            Log.d("LaporanViewModel", "Cleared lastSelectedReportTypeUrl")
        }
    }


    private fun saveToPreferences(key: String, value: String?) {
        viewModelScope.launch { // Operasi SharedPreferences sebaiknya tidak di main thread jika banyak
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    // Untuk memuat URL yang terakhir dibuka (misalnya saat aplikasi start)
    // fun getLastOpenedUrl(): String? {
    //     return sharedPreferences.getString(KEY_LAST_OPENED_URL, null)
    // }

    // Untuk membersihkan state webview, misal saat kembali ke daftar laporan
    fun clearWebViewState() {
        _currentWebViewUrl.value = null
        // Tidak menghapus _lastSelectedReportTypeUrl karena itu menandakan pilihan terakhir
        Log.d("LaporanViewModel", "clearWebViewState called")
    }
}