/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.smartloan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.ai.edge.gallery.ui.common.createTempPictureUri

private const val TAG = "SmartLoanCameraHelper"

/**
 * Camera capture result
 */
sealed class CameraResult {
  data class Success(val bitmap: Bitmap) : CameraResult()
  data class Error(val message: String) : CameraResult()
  object Cancelled : CameraResult()
}

/**
 * Image selection source options
 */
enum class ImageSource {
  CAMERA,
  GALLERY,
  FILES
}

/**
 * Image type for Smart Loan capture
 */
enum class SmartLoanImageType(val displayName: String) {
  ID_FRONT("Front ID"),
  ID_BACK("Back ID"),
  PAYSLIP_1("Payslip 1"),
  PAYSLIP_2("Payslip 2"),
  PAYSLIP_3("Payslip 3"),
  PAYSLIP_4("Payslip 4")
}

/**
 * Composable function for Smart Loan image selection (camera, gallery, or files)
 * Adapts the Gallery app's camera functionality for ID and payslip specific needs
 */
@Composable
fun rememberSmartLoanImageSelector(
  imageType: SmartLoanImageType,
  onResult: (CameraResult) -> Unit
): (ImageSource) -> Unit {
  val context = LocalContext.current
  var tempPhotoUri by remember { mutableStateOf(value = Uri.EMPTY) }

  // Camera launcher for taking pictures
  val cameraLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { isImageSaved ->
    if (isImageSaved) {
      handleImageCaptured(
        context = context,
        uri = tempPhotoUri,
        imageType = imageType,
        onResult = onResult
      )
    } else {
      onResult(CameraResult.Cancelled)
    }
  }

  // Gallery launcher for selecting images
  val galleryLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      handleImageCaptured(
        context = context,
        uri = uri,
        imageType = imageType,
        onResult = onResult
      )
    } else {
      onResult(CameraResult.Cancelled)
    }
  }

  // File picker launcher for selecting document files
  val fileLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      handleImageCaptured(
        context = context,
        uri = uri,
        imageType = imageType,
        onResult = onResult
      )
    } else {
      onResult(CameraResult.Cancelled)
    }
  }

  // Permission request launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      tempPhotoUri = context.createTempPictureUri(
        fileName = "smartloan_${imageType.name.lowercase()}_${System.currentTimeMillis()}"
      )
      cameraLauncher.launch(tempPhotoUri)
    } else {
      onResult(CameraResult.Error("Camera permission denied"))
    }
  }

  return { source ->
    when (source) {
      ImageSource.CAMERA -> {
        // Check if camera permission is granted
        when (PackageManager.PERMISSION_GRANTED) {
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
            // Permission already granted, launch camera
            tempPhotoUri = context.createTempPictureUri(
              fileName = "smartloan_${imageType.name.lowercase()}_${System.currentTimeMillis()}"
            )
            cameraLauncher.launch(tempPhotoUri)
          }
          else -> {
            // Request permission
            permissionLauncher.launch(Manifest.permission.CAMERA)
          }
        }
      }
      ImageSource.GALLERY -> {
        galleryLauncher.launch("image/*")
      }
      ImageSource.FILES -> {
        fileLauncher.launch(arrayOf("image/*", "application/pdf"))
      }
    }
  }
}

/**
 * Legacy function for backward compatibility - defaults to camera capture
 */
@Composable
fun rememberSmartLoanCameraLauncher(
  imageType: SmartLoanImageType,
  onResult: (CameraResult) -> Unit
): () -> Unit {
  val imageSelector = rememberSmartLoanImageSelector(imageType, onResult)
  return { imageSelector(ImageSource.CAMERA) }
}

/**
 * Handle the captured/selected image with Smart Loan specific processing
 */
private fun handleImageCaptured(
  context: Context,
  uri: Uri,
  imageType: SmartLoanImageType,
  onResult: (CameraResult) -> Unit
) {
  Log.d(TAG, "Processing image for ${imageType.displayName}: $uri")

  try {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    if (originalBitmap == null) {
      onResult(CameraResult.Error("Failed to decode selected image"))
      return
    }

    // Process the bitmap based on image type
    val processedBitmap = processImageForType(originalBitmap, imageType)
    
    // Validate image quality
    val validationResult = validateImageQuality(processedBitmap, imageType)
    if (validationResult != null) {
      Log.w(TAG, "Image quality issue for ${imageType.displayName}: $validationResult")
      // For now, we'll still accept the image but could show a warning
    }

    onResult(CameraResult.Success(processedBitmap))
    
  } catch (e: Exception) {
    Log.e(TAG, "Error processing selected image", e)
    onResult(CameraResult.Error("Failed to process image: ${e.message}"))
  }
}

/**
 * Process the bitmap based on the image type requirements
 */
private fun processImageForType(bitmap: Bitmap, imageType: SmartLoanImageType): Bitmap {
  var processedBitmap = bitmap

  // Handle orientation based on image type expectations
  val shouldBePortrait = when (imageType) {
    SmartLoanImageType.PAYSLIP_1, SmartLoanImageType.PAYSLIP_2, 
    SmartLoanImageType.PAYSLIP_3, SmartLoanImageType.PAYSLIP_4 -> true
    SmartLoanImageType.ID_FRONT, SmartLoanImageType.ID_BACK -> false // IDs are usually landscape
  }

  // Rotate if orientation doesn't match expectation
  val isCurrentlyPortrait = bitmap.height > bitmap.width
  if (shouldBePortrait && !isCurrentlyPortrait) {
    val matrix = Matrix()
    matrix.postRotate(90f)
    processedBitmap = Bitmap.createBitmap(
      bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
  } else if (!shouldBePortrait && isCurrentlyPortrait) {
    val matrix = Matrix()
    matrix.postRotate(-90f)
    processedBitmap = Bitmap.createBitmap(
      bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
  }

  // Resize if too large to manage memory
  val maxDimension = when (imageType) {
    SmartLoanImageType.ID_FRONT, SmartLoanImageType.ID_BACK -> 2048
    else -> 1920 // Payslips
  }

  if (processedBitmap.width > maxDimension || processedBitmap.height > maxDimension) {
    val ratio = maxDimension.toFloat() / maxOf(processedBitmap.width, processedBitmap.height)
    val newWidth = (processedBitmap.width * ratio).toInt()
    val newHeight = (processedBitmap.height * ratio).toInt()
    
    val resizedBitmap = Bitmap.createScaledBitmap(processedBitmap, newWidth, newHeight, true)
    
    // Recycle the intermediate bitmap if it's different from original
    if (processedBitmap != bitmap) {
      processedBitmap.recycle()
    }
    
    processedBitmap = resizedBitmap
  }

  return processedBitmap
}

/**
 * Basic image quality validation
 * Returns null if image is acceptable, or error message if there are issues
 */
private fun validateImageQuality(bitmap: Bitmap, imageType: SmartLoanImageType): String? {
  // Check minimum resolution
  val minWidth = 800
  val minHeight = 600
  
  if (bitmap.width < minWidth || bitmap.height < minHeight) {
    return "Image resolution too low (${bitmap.width}x${bitmap.height}). Minimum required: ${minWidth}x${minHeight}"
  }

  // Check if image is too dark (basic check)
  // This is a simple approximation - in a real app you'd use more sophisticated algorithms
  val pixels = IntArray(bitmap.width * bitmap.height)
  bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
  
  val brightness = pixels.take(1000).map { pixel ->
    val r = (pixel shr 16) and 0xFF
    val g = (pixel shr 8) and 0xFF
    val b = pixel and 0xFF
    (r + g + b) / 3
  }.average()

  if (brightness < 30) {
    return "Image appears too dark. Please ensure good lighting."
  }

  if (brightness > 240) {
    return "Image appears overexposed. Please avoid bright lighting."
  }

  return null // Image quality is acceptable
}

/**
 * Get recommended capture instructions for each image type
 */
fun getImageCaptureInstructions(imageType: SmartLoanImageType): String {
  return when (imageType) {
    SmartLoanImageType.ID_FRONT -> 
      "Position your ID card flat with all corners visible. Ensure text is clear and readable."
    SmartLoanImageType.ID_BACK -> 
      "Flip your ID card and capture the back side. Make sure all information is visible."
    SmartLoanImageType.PAYSLIP_1 -> 
      "Capture your most recent payslip. Ensure all text and numbers are clearly visible."
    SmartLoanImageType.PAYSLIP_2 -> 
      "Capture your second most recent payslip. All details should be readable."
    SmartLoanImageType.PAYSLIP_3 -> 
      "Capture your third payslip. Maintain good lighting and steady hands."
    SmartLoanImageType.PAYSLIP_4 -> 
      "Capture your fourth payslip to complete the requirement."
  }
} 