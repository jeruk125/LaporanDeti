package com.example.laporandeti.data.paging

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
// import androidx.core.content.ContextCompat // Tidak lagi diperlukan secara eksplisit di sini jika getExternalMediaDirs dipanggil langsung dari context
import androidx.paging.PagingSource
import androidx.paging.PagingState
// Hapus import androidx.preference.isNotEmpty
import com.example.laporandeti.data.model.MediaStoreImage
import com.example.laporandeti.util.APP_IMAGE_SUBFOLDER // Import konstanta subfolder
import com.example.laporandeti.util.TAG_PAGING_SOURCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppImagesPagingSource(
    private val context: Context,
    private val targetAppSubFolder: String = APP_IMAGE_SUBFOLDER
) : PagingSource<Int, MediaStoreImage>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaStoreImage> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize.coerceAtMost(MAX_PAGE_SIZE)

        return try {
            val images = withContext(Dispatchers.IO) {
                fetchImages(pageNumber, pageSize)
            }

            val nextKey = if (images.size < pageSize || images.isEmpty()) null else pageNumber + 1
            val prevKey = if (pageNumber == 0) null else pageNumber - 1

            Log.d(TAG_PAGING_SOURCE, "Load successful. Page: $pageNumber, Loaded: ${images.size}, SubFolder: $targetAppSubFolder")
            LoadResult.Page(data = images, prevKey = prevKey, nextKey = nextKey)
        } catch (e: Exception) {
            Log.e(TAG_PAGING_SOURCE, "Error loading images for page $pageNumber, subFolder $targetAppSubFolder: ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    private suspend fun fetchImages(page: Int, limit: Int): List<MediaStoreImage> {
        val imageList = mutableListOf<MediaStoreImage>()
        val offset = page * limit

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        ).toTypedArray()

        val selectionArgsList = mutableListOf<String>()
        val selectionClauses = mutableListOf<String>()

        val packageName = context.packageName

        // Menggunakan isNotEmpty() dari String standar Kotlin
        if (targetAppSubFolder.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val expectedRelativePathPrefix = "Android/media/$packageName/$targetAppSubFolder/"
                selectionClauses.add("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                selectionArgsList.add("$expectedRelativePathPrefix%")
                Log.d(TAG_PAGING_SOURCE, "MediaStore Q+ Query. RELATIVE_PATH LIKE: ${selectionArgsList.last()}")
            } else {
                // Memanggil getExternalMediaDirs() pada instance context
                val mediaDirs: Array<File> = context.externalMediaDirs
                if (mediaDirs.isNotEmpty()) {
                    // mediaDirs[0] bisa null, jadi kita perlu check nullability
                    val appSpecificRootMediaDir: File? = mediaDirs[0]
                    if (appSpecificRootMediaDir != null) {
                        val targetDir = File(appSpecificRootMediaDir, targetAppSubFolder)
                        selectionClauses.add("${MediaStore.Images.Media.DATA} LIKE ?")
                        // Memanggil absolutePath pada instance File targetDir
                        selectionArgsList.add("${targetDir.absolutePath}${File.separator}%")
                        Log.d(TAG_PAGING_SOURCE, "MediaStore <Q Query. DATA LIKE: ${selectionArgsList.last()}")
                    } else {
                        Log.e(TAG_PAGING_SOURCE, "Direktori media khusus aplikasi utama null untuk API < Q.")
                        return emptyList()
                    }
                } else {
                    Log.e(TAG_PAGING_SOURCE, "Tidak dapat menentukan path media khusus aplikasi untuk API < Q. Tidak ada gambar yang akan dimuat.")
                    return emptyList()
                }
            }
        } else {
            Log.w(TAG_PAGING_SOURCE, "targetAppSubFolder kosong, tidak ada filter folder yang diterapkan.")
        }

        val finalSelection = if (selectionClauses.isNotEmpty()) selectionClauses.joinToString(separator = " AND ") else null
        val finalSelectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val queryCursor: android.database.Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, finalSelection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, finalSelectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                queryArgs,
                null
            )
        } else {
            val legacySortOrder = "$sortOrder LIMIT $limit OFFSET $offset"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                finalSelection,
                finalSelectionArgs,
                legacySortOrder
            )
        }

        queryCursor?.use { cursor ->
            populateImageListFromCursor(cursor, imageList)
        }
        return imageList
    }

    private fun populateImageListFromCursor(cursor: android.database.Cursor, imageList: MutableList<MediaStoreImage>) {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val pathColumnName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        val pathColumn = cursor.getColumnIndexOrThrow(pathColumnName)

        Log.d(TAG_PAGING_SOURCE, "Cursor count: ${cursor.count}. Iterating:")
        var itemsIterated = 0
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(displayNameColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val pathValue = cursor.getString(pathColumn) // Ini akan menjadi relative path (Q+) atau absolute path (<Q)
            val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            imageList.add(MediaStoreImage(id, displayName, contentUri, dateAdded, pathValue))
            itemsIterated++
        }
        Log.d(TAG_PAGING_SOURCE, "Iterated $itemsIterated items.")
    }

    override fun getRefreshKey(state: PagingState<Int, MediaStoreImage>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }
}
