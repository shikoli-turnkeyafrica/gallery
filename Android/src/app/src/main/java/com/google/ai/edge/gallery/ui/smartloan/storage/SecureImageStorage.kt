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

package com.google.ai.edge.gallery.ui.smartloan.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SecureImageStorage"
private const val SMART_LOAN_DIR = "smartloan_images"
private const val MAX_IMAGE_SIZE_MB = 5
private const val MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024L * 1024L

/**
 * Secure storage manager for Smart Loan images using EncryptedFile
 */
@Singleton
class SecureImageStorage @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

  init {
    // Create the secure directory if it doesn't exist
    val smartLoanDir = File(context.filesDir, SMART_LOAN_DIR)
    if (!smartLoanDir.exists()) {
      smartLoanDir.mkdirs()
    }
  }

  /**
   * Save an image securely using EncryptedFile
   * @param bitmap The bitmap to save
   * @param filename The filename (without extension)
   * @return The full path of the saved file, or null if failed
   */
  fun saveImageSecurely(bitmap: Bitmap, filename: String): String? {
    return try {
      // Compress bitmap to byte array
      val outputStream = ByteArrayOutputStream()
      val compressionQuality = getCompressionQuality(bitmap)
      bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
      val imageBytes = outputStream.toByteArray()
      outputStream.close()

      // Check file size
      if (imageBytes.size > MAX_IMAGE_SIZE_BYTES) {
        Log.w(TAG, "Image size (${imageBytes.size / 1024 / 1024}MB) exceeds limit of ${MAX_IMAGE_SIZE_MB}MB")
        return null
      }

      // Create encrypted file
      val fileToWrite = File(File(context.filesDir, SMART_LOAN_DIR), "$filename.enc")
      val encryptedFile = EncryptedFile.Builder(
        fileToWrite,
        context,
        masterKeyAlias,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
      ).build()

      // Write encrypted data
      encryptedFile.openFileOutput().use { encryptedOutputStream ->
        encryptedOutputStream.write(imageBytes)
        encryptedOutputStream.flush()
      }

      Log.d(TAG, "Image saved securely: ${fileToWrite.absolutePath}")
      fileToWrite.absolutePath
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save image securely: $filename", e)
      null
    }
  }

  /**
   * Load an image securely from EncryptedFile
   * @param path The full path of the encrypted file
   * @return The decoded bitmap, or null if failed
   */
  fun loadImageSecurely(path: String): Bitmap? {
    return try {
      val encryptedFile = EncryptedFile.Builder(
        File(path),
        context,
        masterKeyAlias,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
      ).build()

      // Read encrypted data
      val imageBytes = encryptedFile.openFileInput().use { encryptedInputStream ->
        encryptedInputStream.readBytes()
      }

      // Decode bitmap
      val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
      Log.d(TAG, "Image loaded securely: $path")
      bitmap
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load image securely: $path", e)
      null
    }
  }

  /**
   * Delete an encrypted image file
   * @param path The full path of the encrypted file
   * @return true if deleted successfully, false otherwise
   */
  fun deleteImageSecurely(path: String): Boolean {
    return try {
      val file = File(path)
      val deleted = file.delete()
      Log.d(TAG, "Image deleted: $path, success: $deleted")
      deleted
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete image: $path", e)
      false
    }
  }

  /**
   * Clear all Smart Loan images
   * @return true if all images were cleared successfully, false otherwise
   */
  fun clearAllImages(): Boolean {
    return try {
      val smartLoanDir = File(context.filesDir, SMART_LOAN_DIR)
      if (smartLoanDir.exists()) {
        val files = smartLoanDir.listFiles()
        files?.forEach { file -> 
          file.delete()
        }
        Log.d(TAG, "All Smart Loan images cleared")
        true
      } else {
        Log.d(TAG, "Smart Loan directory doesn't exist")
        true
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear all images", e)
      false
    }
  }

  /**
   * Generate a unique filename for different image types
   */
  fun generateFilename(imageType: String, timestamp: Long = System.currentTimeMillis()): String {
    return "smartloan_${imageType}_$timestamp"
  }

  /**
   * Get available storage space for Smart Loan images
   */
  fun getAvailableStorageBytes(): Long {
    return try {
      val smartLoanDir = File(context.filesDir, SMART_LOAN_DIR)
      smartLoanDir.usableSpace
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get available storage", e)
      0L
    }
  }

  /**
   * Get total size of Smart Loan images
   */
  fun getTotalImageSizeBytes(): Long {
    return try {
      val smartLoanDir = File(context.filesDir, SMART_LOAN_DIR)
      if (smartLoanDir.exists()) {
        smartLoanDir.listFiles()?.fold(0L) { acc, file -> acc + file.length() } ?: 0L
      } else {
        0L
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to calculate total image size", e)
      0L
    }
  }

  /**
   * Check if there's enough storage space for a new image
   */
  fun hasEnoughStorage(estimatedSizeBytes: Long = MAX_IMAGE_SIZE_BYTES): Boolean {
    val availableSpace = getAvailableStorageBytes()
    val hasSpace = availableSpace > estimatedSizeBytes
    if (!hasSpace) {
      Log.w(TAG, "Insufficient storage. Available: ${availableSpace / 1024 / 1024}MB, Required: ${estimatedSizeBytes / 1024 / 1024}MB")
    }
    return hasSpace
  }

  /**
   * Calculate optimal compression quality based on bitmap size
   */
  private fun getCompressionQuality(bitmap: Bitmap): Int {
    val pixels = bitmap.width * bitmap.height
    return when {
      pixels > 4_000_000 -> 70  // Very high resolution
      pixels > 2_000_000 -> 80  // High resolution  
      pixels > 1_000_000 -> 85  // Medium resolution
      else -> 90               // Lower resolution
    }
  }
} 