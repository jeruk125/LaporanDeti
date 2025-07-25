package com.example.laporandeti.ui.gallery // Sesuaikan package jika perlu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.laporandeti.data.model.MediaStoreImage
import com.example.laporandeti.data.paging.AppImagesPagingSource
import com.example.laporandeti.util.APP_IMAGE_SUBFOLDER // Import konstanta subfolder yang benar (APP_IMAGE_SUBFOLDER)
import kotlinx.coroutines.flow.Flow

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val appImages: Flow<PagingData<MediaStoreImage>> = Pager(
        // Konfigurasi Paging: ukuran halaman, dll.
        config = PagingConfig(
            pageSize = 30, // Berapa banyak item yang dimuat per halaman
            enablePlaceholders = false // Nonaktifkan placeholder jika tidak diperlukan
        ),
        // Factory untuk membuat PagingSource
        pagingSourceFactory = {
            AppImagesPagingSource(
                context = application.applicationContext, // Pastikan mengirim Context
                // Gunakan nama parameter yang baru: targetAppSubFolder
                // Dan pastikan konstanta yang diimpor adalah yang benar (APP_IMAGE_SUBFOLDER)
                targetAppSubFolder = APP_IMAGE_SUBFOLDER
            )
        }
    ).flow
        .cachedIn(viewModelScope) // Cache hasil di ViewModelScope agar tetap ada saat konfigurasi berubah
}
