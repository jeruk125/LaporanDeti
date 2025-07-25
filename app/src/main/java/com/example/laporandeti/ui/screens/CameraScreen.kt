package com.example.laporandeti.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.laporandeti.util.APP_IMAGE_SUBFOLDER
import com.example.laporandeti.util.getAppOutputDirectory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

const val TAG_CAMERA_SCREEN = "CameraScreenWithLocation"
const val FILENAME_FORMAT_CAMERA = "yyyy-MM-dd-HH-mm-ss-SSS"

const val TARGET_ASPECT_RATIO = AspectRatio.RATIO_4_3

const val PREFS_NAME = "CameraPrefs"
const val KEY_TIMESTAMP_LOCATION_ENABLED = "isTimestampLocationEnabled"

fun surfaceRotationToString(rotation: Int): String {
    return when (rotation) {
        Surface.ROTATION_0 -> "ROTATION_0 (Portrait Display)"
        Surface.ROTATION_90 -> "ROTATION_90 (Landscape Display - UI CCW)"
        Surface.ROTATION_180 -> "ROTATION_180 (Upside Down Display)"
        Surface.ROTATION_270 -> "ROTATION_270 (270deg CW / -90deg)"
        else -> "UNKNOWN_ROTATION ($rotation)"
    }
}

fun exifOrientationToString(orientation: Int): String {
    return when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> "NORMAL (0deg)"
        ExifInterface.ORIENTATION_ROTATE_90 -> "ROTATE_90 (90deg CW)"
        ExifInterface.ORIENTATION_ROTATE_180 -> "ROTATE_180 (180deg)"
        ExifInterface.ORIENTATION_ROTATE_270 -> "ROTATE_270 (270deg CW / -90deg)"
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "FLIP_HORIZONTAL"
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "FLIP_VERTICAL"
        ExifInterface.ORIENTATION_TRANSPOSE -> "TRANSPOSE (Rotate 90deg CW + Flip Horizontal)"
        ExifInterface.ORIENTATION_TRANSVERSE -> "TRANSVERSE (Rotate 270deg CW + Flip Horizontal)"
        ExifInterface.ORIENTATION_UNDEFINED -> "UNDEFINED"
        else -> "UNKNOWN_EXIF ($orientation)"
    }
}

@Composable
fun CameraScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val sharedPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var isTimestampLocationEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean(KEY_TIMESTAMP_LOCATION_ENABLED, false))
    }

    val displayManager = remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    val currentDisplay = remember(displayManager) { displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY) }
    var currentDisplayRotationState by remember { mutableStateOf(currentDisplay?.rotation ?: Surface.ROTATION_0) }

    val locationState = remember { mutableStateOf<Location?>(null) }
    val addressTextState = remember { mutableStateOf<String>("Mencari lokasi...") } // State untuk alamat
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // CoroutineScope untuk tugas-tugas yang membutuhkan Dispatchers.IO (seperti Geocoding)
    val coroutineScope = rememberCoroutineScope()

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { newLocation ->
                    locationState.value = newLocation
                    Log.d(TAG_CAMERA_SCREEN, "Location updated via callback: Lat=${newLocation.latitude}, Lon=${newLocation.longitude}")

                    // Panggil reverse geocoding saat lokasi diperbarui
                    coroutineScope.launch {
                        addressTextState.value = getAddressFromLocation(context, newLocation)
                    }
                }
            }
        }
    }

    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(TAG_CAMERA_SCREEN, "Izin lokasi diberikan.")
            if (isTimestampLocationEnabled) {
                startLocationUpdates(context, fusedLocationClient, locationCallback, locationRequest)
            }
        } else {
            Log.w(TAG_CAMERA_SCREEN, "Izin lokasi ditolak.")
            if (isTimestampLocationEnabled) {
                Toast.makeText(context, "Izin lokasi diperlukan untuk menampilkan lokasi.", Toast.LENGTH_LONG).show()
                locationState.value = null
                addressTextState.value = "Izin lokasi tidak diberikan" // Update teks status
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            cameraPermissionGranted = true
        }
    }

    LaunchedEffect(isTimestampLocationEnabled) {
        if (isTimestampLocationEnabled) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasLocationPermission) {
                startLocationUpdates(context, fusedLocationClient, locationCallback, locationRequest)
            } else {
                Log.w(TAG_CAMERA_SCREEN, "Fitur timestamp/lokasi aktif, namun izin lokasi belum diberikan. Meminta izin.")
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            stopLocationUpdates(fusedLocationClient, locationCallback)
            locationState.value = null
            addressTextState.value = "" // Kosongkan alamat saat fitur dinonaktifkan
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            stopLocationUpdates(fusedLocationClient, locationCallback)
            Log.d(TAG_CAMERA_SCREEN, "Location updates stopped on dispose.")
        }
    }

    DisposableEffect(displayManager, currentDisplay) {
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == currentDisplay?.displayId) {
                    val newRotation = currentDisplay.rotation
                    if (newRotation != currentDisplayRotationState) {
                        Log.d(TAG_CAMERA_SCREEN, "Display rotation changed from ${surfaceRotationToString(currentDisplayRotationState)} to ${surfaceRotationToString(newRotation)}")
                        currentDisplayRotationState = newRotation
                        imageCapture?.targetRotation = newRotation
                        Log.d(TAG_CAMERA_SCREEN, "Live ImageCapture targetRotation updated by listener to: ${surfaceRotationToString(newRotation)} (for EXIF)")
                    }
                }
            }
        }
        displayManager.registerDisplayListener(displayListener, null)
        val initialRotation = currentDisplay?.rotation ?: Surface.ROTATION_0
        currentDisplayRotationState = initialRotation
        imageCapture?.targetRotation = initialRotation
        Log.d(TAG_CAMERA_SCREEN, "Initial display rotation: ${surfaceRotationToString(currentDisplayRotationState)}")
        onDispose {
            displayManager.unregisterDisplayListener(displayListener)
        }
    }

    if (cameraPermissionGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                cameraProviderFuture = cameraProviderFuture,
                lensFacing = lensFacing,
                initialDisplayRotationForExif = currentDisplayRotationState,
                onImageCaptureReady = { newImageCaptureInstance ->
                    Log.i(TAG_CAMERA_SCREEN, "CAMERASCREEN: onImageCaptureReady. Instance received.")
                    newImageCaptureInstance.targetRotation = currentDisplayRotationState
                    imageCapture = newImageCaptureInstance
                },
                isTimestampLocationEnabled = isTimestampLocationEnabled,
                currentLocation = locationState.value,
                currentAddressText = addressTextState.value // Lewatkan alamat ke preview
            )

            CameraControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
                onTakePhoto = {
                    imageCapture?.let { currentImgCapture ->
                        if (currentImgCapture.targetRotation != currentDisplayRotationState) {
                            Log.w(TAG_CAMERA_SCREEN, "CAMERASCREEN: TakePhoto - Mismatch! Updating ImageCapture targetRotation for EXIF to ${surfaceRotationToString(currentDisplayRotationState)}")
                            currentImgCapture.targetRotation = currentDisplayRotationState
                        }
                        val finalRotationForExif = currentImgCapture.targetRotation
                        Log.i(TAG_CAMERA_SCREEN, "CAMERASCREEN: Taking photo. Output Aspect Ratio will be 4:3. EXIF Rotation: ${surfaceRotationToString(finalRotationForExif)}")

                        takePhoto(
                            imageCapture = currentImgCapture,
                            context = context,
                            executor = Executors.newSingleThreadExecutor(),
                            isTimestampLocationEnabled = isTimestampLocationEnabled,
                            currentLocation = locationState.value,
                            currentAddressText = addressTextState.value, // Lewatkan alamat ke takePhoto
                            onImageSaved = { uri, photoFile, width, height, exifOrientation ->
                                val successMsg = "Foto ${width}x${height} (EXIF: ${exifOrientationToString(exifOrientation)}) disimpan!"
                                Log.i(TAG_CAMERA_SCREEN, "CAMERASCREEN: $successMsg URI: $uri")
                                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                                MediaScannerConnection.scanFile(context, arrayOf(photoFile.absolutePath), arrayOf("image/jpeg")) { path, scannedUri ->
                                    Log.d(TAG_CAMERA_SCREEN, "CAMERASCREEN: MediaScan Done. Path: $path, URI: $scannedUri")
                                }
                            },
                            onError = { exception ->
                                val errorMsg = "Gagal mengambil foto: ${exception.message}"
                                Log.e(TAG_CAMERA_SCREEN, "CAMERASCREEN: $errorMsg", exception)
                                Toast.makeText(context, "Gagal: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    } ?: Log.e(TAG_CAMERA_SCREEN, "CAMERASCREEN: TakePhoto - ImageCapture is NULL.")
                },
                onSwitchCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                },
                isTimestampLocationEnabled = isTimestampLocationEnabled,
                onToggleTimestampLocation = {
                    isTimestampLocationEnabled = !isTimestampLocationEnabled
                    sharedPrefs.edit().putBoolean(KEY_TIMESTAMP_LOCATION_ENABLED, isTimestampLocationEnabled).apply()

                    if (isTimestampLocationEnabled) {
                        Toast.makeText(context, "Timestamp & Lokasi Aktif", Toast.LENGTH_SHORT).show()
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            startLocationUpdates(context, fusedLocationClient, locationCallback, locationRequest)
                        }
                    } else {
                        Toast.makeText(context, "Timestamp & Lokasi Nonaktif", Toast.LENGTH_SHORT).show()
                        stopLocationUpdates(fusedLocationClient, locationCallback)
                        locationState.value = null
                        addressTextState.value = "" // Kosongkan saat dinonaktifkan
                    }
                }
            )
        }
    } else {
        PermissionDeniedView { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lensFacing: Int,
    initialDisplayRotationForExif: Int,
    onImageCaptureReady: (ImageCapture) -> Unit,
    isTimestampLocationEnabled: Boolean,
    currentLocation: Location?,
    currentAddressText: String // Parameter baru untuk alamat
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing, cameraProviderFuture) {
        Log.i(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Triggered. Lens: ${if (lensFacing == CameraSelector.LENS_FACING_FRONT) "Front" else "Back"}")
        try {
            val cameraProvider = cameraProviderFuture.await()
            Log.d(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Unbinding all previous use cases...")
            cameraProvider.unbindAll()

            val preview = Preview.Builder()
                .setTargetAspectRatio(TARGET_ASPECT_RATIO)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            Log.d(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Preview configured with AspectRatio.RATIO_4_3.")

            val newImageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(TARGET_ASPECT_RATIO)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(initialDisplayRotationForExif)
                .build()
            Log.i(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: New ImageCapture created. TargetAspectRatio: 4:3, Initial TargetRotation for EXIF: ${surfaceRotationToString(initialDisplayRotationForExif)}")

            onImageCaptureReady(newImageCapture)

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            Log.d(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Binding new use cases to lifecycle...")
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                newImageCapture
            )
            Log.i(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Camera use cases BOUND. ImageCapture target aspect: 4:3, initial target rot: ${surfaceRotationToString(newImageCapture.targetRotation)}")

        } catch (exc: IllegalArgumentException) {
            Log.e(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Error binding camera - Aspect Ratio 4:3 might not be fully supported. ${exc.localizedMessage}", exc)
            Toast.makeText(context, "Gagal memulai kamera: Aspek rasio 4:3 tidak didukung? ${exc.localizedMessage}", Toast.LENGTH_LONG).show()
        } catch (exc: Exception) {
            Log.e(TAG_CAMERA_SCREEN, "PREVIEW_VIEW LAUNCHED_EFFECT: Failed to bind camera use cases. ${exc.localizedMessage}", exc)
            Toast.makeText(context, "Gagal memulai kamera: ${exc.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (isTimestampLocationEnabled) {
            val currentTime = remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                while (true) {
                    currentTime.value = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    delay(1000)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.End // Tetap di ujung kanan
            ) {
                Text(
                    text = currentTime.value,
                    color = Color.White
                )
                // Tampilkan alamat yang sudah di-geocoding terbalik
                val hasFineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasFineLocationPermission) {
                    Text(
                        text = currentAddressText, // Gunakan state alamat yang sudah diproses
                        color = Color.White,
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = "Izin lokasi tidak diberikan",
                        color = Color.Red,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraControls(
    modifier: Modifier = Modifier,
    onTakePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    isTimestampLocationEnabled: Boolean,
    onToggleTimestampLocation: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSwitchCamera,
            modifier = Modifier
                .size(60.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Filled.Cameraswitch, "Ganti Kamera", tint = Color.White, modifier = Modifier.size(36.dp))
        }

        IconButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .padding(8.dp)
        ) {
            Icon(Icons.Filled.PhotoCamera, "Ambil Foto", tint = Color.White, modifier = Modifier.size(48.dp))
        }

        IconButton(
            onClick = onToggleTimestampLocation,
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (isTimestampLocationEnabled) Color.Green.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(Icons.Filled.Schedule, "Toggle Timestamp/Location", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.7f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Izin kamera diperlukan untuk menggunakan fitur ini.", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Berikan Izin")
        }
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCoroutine { continuation ->
        val executor = continuation.context.get(kotlin.coroutines.ContinuationInterceptor)?.let { it as? kotlinx.coroutines.CoroutineDispatcher }?.asExecutor()
            ?: Executors.newSingleThreadExecutor()

        addListener({
            try {
                continuation.resume(get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, executor)
    }

@SuppressLint("MissingPermission")
private fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    executor: Executor,
    isTimestampLocationEnabled: Boolean,
    currentLocation: Location?,
    currentAddressText: String, // Parameter baru untuk alamat
    onImageSaved: (uri: Uri, photoFile: File, width: Int, height: Int, exifOrientation: Int) -> Unit,
    onError: (exception: ImageCaptureException) -> Unit
) {
    val simpleDateFormat = SimpleDateFormat(FILENAME_FORMAT_CAMERA, Locale.US)
    val photoFileName = "LaporanDeti_${simpleDateFormat.format(System.currentTimeMillis())}.jpg"

    val outputDirectory = getAppOutputDirectory(context, APP_IMAGE_SUBFOLDER)

    if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
        val ioException = ImageCaptureException(ImageCapture.ERROR_FILE_IO, "Gagal membuat direktori: ${outputDirectory.absolutePath}", null)
        Log.e(TAG_CAMERA_SCREEN, "takePhoto: ${ioException.message}", ioException)
        ContextCompat.getMainExecutor(context).execute { onError(ioException) }
        return
    }

    val photoFile = File(outputDirectory, photoFileName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    Log.d(TAG_CAMERA_SCREEN, "takePhoto: Saving to: ${photoFile.absolutePath}")
    Log.d(TAG_CAMERA_SCREEN, "takePhoto: ImageCapture final targetRotation for EXIF is ${surfaceRotationToString(imageCapture.targetRotation)}")
    Log.d(TAG_CAMERA_SCREEN, "takePhoto: ImageCapture configured aspect ratio: 4:3")


    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                Log.i(TAG_CAMERA_SCREEN, "takePhoto: Image SAVED successfully by CameraX. URI: $savedUri")

                val mainExecutor = ContextCompat.getMainExecutor(context)
                val ioExecutor = Executors.newSingleThreadExecutor()

                ioExecutor.execute {
                    var finalWidth = 0
                    var finalHeight = 0
                    var finalExifOrientationValue = ExifInterface.ORIENTATION_UNDEFINED

                    try {
                        val exif = ExifInterface(photoFile.absolutePath)
                        val exifOrientationFromFile = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        Log.d(TAG_CAMERA_SCREEN, "Post-processing: Initial EXIF Orientation from file: $exifOrientationFromFile (${exifOrientationToString(exifOrientationFromFile)})")

                        val rotationDegrees = when (exifOrientationFromFile) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }

                        var originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        if (originalBitmap == null) {
                            Log.e(TAG_CAMERA_SCREEN, "Post-processing: Failed to decode bitmap for rotation.")
                            mainExecutor.execute {
                                onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Gagal memproses gambar setelah disimpan.", null))
                            }
                            return@execute
                        }

                        var processedBitmap = originalBitmap

                        if (rotationDegrees != 0) {
                            Log.d(TAG_CAMERA_SCREEN, "Post-processing: Rotating image pixels by $rotationDegrees degrees permanently.")
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            processedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                            originalBitmap.recycle()
                        } else {
                            Log.d(TAG_CAMERA_SCREEN, "Post-processing: No rotation needed for image pixels (EXIF_ORIENTATION_NORMAL).")
                        }

                        if (isTimestampLocationEnabled) {
                            Log.d(TAG_CAMERA_SCREEN, "Post-processing: Drawing timestamp and/or location on image.")
                            val timestampText = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                            var combinedText = timestampText

                            val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasLocationPermission) {
                                // Gunakan alamat yang sudah ada dan diformat vertikal
                                if (currentLocation != null && currentAddressText.isNotBlank() && currentAddressText != "Mencari lokasi..." && currentAddressText != "Izin lokasi tidak diberikan") {
                                    combinedText += "\n${currentAddressText}"
                                } else if (currentLocation != null) {
                                    // Fallback ke koordinat jika alamat gagal ditemukan tapi lokasi ada
                                    combinedText += "\nLat: ${String.format("%.6f", currentLocation.latitude)}\nLon: ${String.format("%.6f", currentLocation.longitude)}"
                                } else {
                                    combinedText += "\nLokasi tidak tersedia"
                                }
                            } else {
                                combinedText += "\nIzin lokasi tidak diberikan"
                            }

                            processedBitmap = drawTextOnBitmap(processedBitmap, combinedText)
                        }

                        FileOutputStream(photoFile).use { out ->
                            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        processedBitmap.recycle()

                        val updatedExif = ExifInterface(photoFile.absolutePath)
                        updatedExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())

                        if (isTimestampLocationEnabled && currentLocation != null) {
                            val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasLocationPermission) {
                                updatedExif.setLatLong(currentLocation.latitude, currentLocation.longitude)
                                updatedExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (currentLocation.latitude > 0) "N" else "S")
                                updatedExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (currentLocation.longitude > 0) "E" else "W")
                                Log.d(TAG_CAMERA_SCREEN, "Lokasi disimpan ke EXIF: Lat=${currentLocation.latitude}, Lon=${currentLocation.longitude}")
                            } else {
                                Log.w(TAG_CAMERA_SCREEN, "Tidak dapat menyimpan lokasi ke EXIF: Izin lokasi tidak diberikan.")
                            }
                        }
                        updatedExif.saveAttributes()
                        Log.d(TAG_CAMERA_SCREEN, "Post-processing: Image pixels rotated permanently, EXIF orientation set to NORMAL.")

                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(photoFile.absolutePath, options)
                        finalWidth = options.outWidth
                        finalHeight = options.outHeight

                        val finalExif = ExifInterface(photoFile.absolutePath)
                        finalExifOrientationValue = finalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                        Log.i(TAG_CAMERA_SCREEN, "Post-processing: Final image dimensions: ${finalWidth}x${finalHeight}. Final EXIF Orientation: $finalExifOrientationValue (${exifOrientationToString(finalExifOrientationValue)})")

                    } catch (e: Exception) {
                        Log.e(TAG_CAMERA_SCREEN, "Post-processing: Error during image rotation/EXIF update/timestamp/location: ${e.message}", e)
                        mainExecutor.execute {
                            onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Gagal memproses gambar.", e))
                        }
                        return@execute
                    }

                    mainExecutor.execute {
                        onImageSaved(savedUri, photoFile, finalWidth, finalHeight, finalExifOrientationValue)
                    }
                }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG_CAMERA_SCREEN, "takePhoto: ImageCaptureException (callback): ${exc.imageCaptureError} - ${exc.message}", exc)
                ContextCompat.getMainExecutor(context).execute { onError(exc) }
            }
        }
    )
}

private fun drawTextOnBitmap(bitmap: Bitmap, text: String): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = GraphicsColor.WHITE
        textSize = (mutableBitmap.height * 0.03f).coerceAtLeast(24f) // Sesuaikan ukuran teks jika perlu
        setShadowLayer(5f, 0f, 0f, GraphicsColor.BLACK)
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT // Teks akan rata kanan
    }

    val xPadding = (mutableBitmap.width * 0.02f).coerceAtLeast(20f)
    val yPadding = (mutableBitmap.height * 0.02f).coerceAtLeast(20f)

    // Memisahkan teks berdasarkan baris baru (\n)
    val lines = text.split("\n")
    var y = mutableBitmap.height - yPadding // Mulai dari bawah ke atas

    // Menggambar setiap baris teks dari bawah ke atas
    for (i in lines.indices.reversed()) {
        val line = lines[i]
        val x = mutableBitmap.width - xPadding // Posisi X untuk rata kanan
        canvas.drawText(line, x, y, paint)
        y -= (paint.fontMetrics.descent - paint.fontMetrics.ascent) // Geser Y ke atas untuk baris berikutnya
    }

    return mutableBitmap
}

private fun ExifInterface.setLatLong(latitude: Double, longitude: Double) {
    setAttribute(ExifInterface.TAG_GPS_LATITUDE, formatLocation(Math.abs(latitude)))
    setAttribute(ExifInterface.TAG_GPS_LONGITUDE, formatLocation(Math.abs(longitude)))
    setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude > 0) "N" else "S")
    setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude > 0) "E" else "W")
}

private fun formatLocation(value: Double): String {
    val degrees = value.toInt()
    val minutes = ((value - degrees) * 60).toInt()
    val seconds = (((value - degrees) * 60 - minutes) * 60000).toInt()
    return "$degrees/1,$minutes/1,$seconds/1000"
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(context: Context, fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback, locationRequest: LocationRequest) {
    val hasFineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (hasFineLocationPermission) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(TAG_CAMERA_SCREEN, "Starting active location updates.")
    } else {
        Log.w(TAG_CAMERA_SCREEN, "Cannot start location updates: FINE_LOCATION permission not granted.")
    }
}

private fun stopLocationUpdates(fusedLocationClient: FusedLocationProviderClient, locationCallback: LocationCallback) {
    fusedLocationClient.removeLocationUpdates(locationCallback)
    Log.d(TAG_CAMERA_SCREEN, "Stopping active location updates.")
}

private suspend fun getAddressFromLocation(context: Context, location: Location): String {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        var resultAddress: String

        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressParts = mutableListOf<String>()

                if (address.subLocality != null && address.subLocality.isNotBlank()) {
                    addressParts.add(address.subLocality)
                }
                if (address.thoroughfare != null && address.thoroughfare.isNotBlank()) {
                    addressParts.add(address.thoroughfare)
                }
                if (address.locality != null && address.locality.isNotBlank()) {
                    addressParts.add("Kecamatan " + address.locality)
                }
                if (address.subAdminArea != null && address.subAdminArea.isNotBlank()) {
                    addressParts.add(address.subAdminArea)
                } else if (address.locality != null && address.locality.isNotBlank() && address.locality.contains("Kota")) {
                    addressParts.add(address.locality)
                }

                if (address.adminArea != null && address.adminArea.isNotBlank()) {
                    addressParts.add(address.adminArea)
                }

                resultAddress = addressParts.joinToString(separator = "\n").ifEmpty { "Alamat tidak lengkap." }

                Log.d(TAG_CAMERA_SCREEN, "Reverse Geocoding Success: \n$resultAddress")
            } else {
                Log.w(TAG_CAMERA_SCREEN, "No address found for Lat: ${location.latitude}, Lon: ${location.longitude}")
                resultAddress = "Alamat tidak ditemukan (data kosong)."
            }
        } catch (e: IOException) {
            Log.e(TAG_CAMERA_SCREEN, "Geocoder service not available or network error: ${e.message}", e)
            resultAddress = "Layanan alamat tidak tersedia (offline/error)."
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_CAMERA_SCREEN, "Invalid Lat/Lon for Geocoder: ${e.message}", e)
            resultAddress = "Koordinat tidak valid."
        } catch (e: Exception) {
            Log.e(TAG_CAMERA_SCREEN, "Unknown error during geocoding: ${e.message}", e)
            resultAddress = "Error saat mencari alamat."
        }
        resultAddress
    }
}