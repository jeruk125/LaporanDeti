package com.example.laporandeti.data.model

import android.net.Uri

data class MediaStoreImage(
    val id: Long,
    val displayName: String,
    val contentUri: Uri,
    val dateAdded: Long,
    val relativePath: String? // Bisa null jika DATA yang digunakan untuk API < Q
)