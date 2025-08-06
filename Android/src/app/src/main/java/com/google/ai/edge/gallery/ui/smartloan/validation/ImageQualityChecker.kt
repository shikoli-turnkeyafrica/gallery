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

package com.google.ai.edge.gallery.ui.smartloan.validation

import android.graphics.Bitmap
import kotlin.math.sqrt

/**
 * Image quality validation result
 */
data class ImageQualityResult(
  val isAcceptable: Boolean,
  val issues: List<String> = emptyList(),
  val suggestions: List<String> = emptyList(),
  val score: Float = 0f // 0.0 to 1.0, where 1.0 is perfect quality
)

/**
 * Image quality validation configuration
 */
data class QualityConfig(
  val minResolution: Pair<Int, Int> = Pair(1920, 1080),
  val maxFileSizeMB: Int = 5,
  val minBrightness: Int = 30,
  val maxBrightness: Int = 240,
  val minBlurThreshold: Double = 100.0 // Laplacian variance threshold
)

/**
 * Comprehensive image quality checker for Smart Loan documents
 */
object ImageQualityChecker {

  /**
   * Validate image quality with comprehensive checks
   */
  fun validateImage(
    bitmap: Bitmap,
    imageType: String,
    config: QualityConfig = QualityConfig()
  ): ImageQualityResult {
    val issues = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    var score = 1.0f

    // 1. Resolution check
    val resolutionResult = checkResolution(bitmap, config)
    if (!resolutionResult.isValid) {
      issues.add("Low resolution: ${bitmap.width}x${bitmap.height}")
      suggestions.add("Ensure camera is in focus and move closer to the document")
      score -= 0.3f
    }

    // 2. Brightness validation
    val brightnessResult = checkBrightness(bitmap, config)
    if (!brightnessResult.isValid) {
      issues.add(brightnessResult.message)
      suggestions.add(brightnessResult.suggestion)
      score -= 0.2f
    }

    // 3. Blur detection (basic Laplacian variance)
    val blurResult = checkBlurriness(bitmap, config)
    if (!blurResult.isValid) {
      issues.add("Image appears blurry")
      suggestions.add("Hold the camera steady and ensure the document is in focus")
      score -= 0.25f
    }

    // 4. Document type specific validation
    val documentResult = checkDocumentSpecific(bitmap, imageType)
    if (!documentResult.isValid) {
      issues.addAll(documentResult.issues)
      suggestions.addAll(documentResult.suggestions)
      score -= 0.15f
    }

    // 5. File size estimation
    val sizeResult = estimateFileSize(bitmap, config)
    if (!sizeResult.isValid) {
      issues.add("Image file size too large")
      suggestions.add("Image quality is very high, consider reducing camera resolution if issues persist")
      score -= 0.1f
    }

    // Ensure score doesn't go below 0
    score = maxOf(0f, score)

    return ImageQualityResult(
      isAcceptable = issues.isEmpty() || score >= 0.6f, // Accept if no major issues or score >= 60%
      issues = issues,
      suggestions = suggestions,
      score = score
    )
  }

  /**
   * Check if image resolution meets minimum requirements
   */
  private fun checkResolution(bitmap: Bitmap, config: QualityConfig): ValidationResult {
    val (minWidth, minHeight) = config.minResolution
    val isValid = bitmap.width >= minWidth && bitmap.height >= minHeight
    
    return ValidationResult(
      isValid = isValid,
      message = if (!isValid) "Resolution ${bitmap.width}x${bitmap.height} below minimum ${minWidth}x${minHeight}" else "",
      suggestion = "Move closer to the document and ensure good lighting"
    )
  }

  /**
   * Check image brightness levels
   */
  private fun checkBrightness(bitmap: Bitmap, config: QualityConfig): ValidationResult {
    // Sample pixels for brightness calculation (use every 10th pixel for performance)
    val sampleSize = minOf(10000, bitmap.width * bitmap.height / 100)
    val pixels = IntArray(sampleSize)
    
    // Sample pixels evenly across the image
    var index = 0
    val skipFactor = maxOf(1, (bitmap.width * bitmap.height) / sampleSize)
    
    for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 100)) {
      for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 100)) {
        if (index < sampleSize) {
          pixels[index] = bitmap.getPixel(x, y)
          index++
        }
      }
    }

    val brightness = pixels.take(index).map { pixel ->
      val r = (pixel shr 16) and 0xFF
      val g = (pixel shr 8) and 0xFF
      val b = pixel and 0xFF
      // Use weighted average for better brightness calculation
      (r * 0.299 + g * 0.587 + b * 0.114).toInt()
    }.average().toInt()

    return when {
      brightness < config.minBrightness -> ValidationResult(
        isValid = false,
        message = "Image too dark (brightness: $brightness)",
        suggestion = "Ensure good lighting or turn on flash"
      )
      brightness > config.maxBrightness -> ValidationResult(
        isValid = false,
        message = "Image overexposed (brightness: $brightness)",
        suggestion = "Avoid direct lighting or flash, use natural light"
      )
      else -> ValidationResult(isValid = true)
    }
  }

  /**
   * Check image sharpness using Laplacian variance
   */
  private fun checkBlurriness(bitmap: Bitmap, config: QualityConfig): ValidationResult {
    // Convert to grayscale and calculate Laplacian variance
    val width = bitmap.width
    val height = bitmap.height
    
    // Sample a smaller area for performance (center 50% of image)
    val startX = width / 4
    val endX = width * 3 / 4
    val startY = height / 4
    val endY = height * 3 / 4
    
    var laplacianVariance = 0.0
    var count = 0
    
    for (y in startY + 1 until endY - 1) {
      for (x in startX + 1 until endX - 1) {
        val center = getGrayscale(bitmap.getPixel(x, y))
        val up = getGrayscale(bitmap.getPixel(x, y - 1))
        val down = getGrayscale(bitmap.getPixel(x, y + 1))
        val left = getGrayscale(bitmap.getPixel(x - 1, y))
        val right = getGrayscale(bitmap.getPixel(x + 1, y))
        
        // Laplacian kernel: center * 4 - (up + down + left + right)
        val laplacian = center * 4 - (up + down + left + right)
        laplacianVariance += laplacian * laplacian
        count++
      }
    }
    
    laplacianVariance = if (count > 0) laplacianVariance / count else 0.0
    
    return ValidationResult(
      isValid = laplacianVariance >= config.minBlurThreshold,
      message = if (laplacianVariance < config.minBlurThreshold) "Image blur detected (variance: ${laplacianVariance.toInt()})" else "",
      suggestion = "Hold camera steady and ensure document is in focus"
    )
  }

  /**
   * Document-type specific validations
   */
  private fun checkDocumentSpecific(bitmap: Bitmap, imageType: String): DocumentValidationResult {
    val issues = mutableListOf<String>()
    val suggestions = mutableListOf<String>()
    
    when (imageType.lowercase()) {
      "id_front", "id_back" -> {
        // Check aspect ratio for ID cards (typically 3.375 x 2.125 inches, ratio ~1.588)
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (aspectRatio < 1.3f || aspectRatio > 1.9f) {
          issues.add("Unusual aspect ratio for ID document")
          suggestions.add("Ensure the entire ID card is visible and properly framed")
        }
        
        // Check for sufficient contrast (documents should have clear text)
        if (!hasGoodContrast(bitmap)) {
          issues.add("Low contrast detected")
          suggestions.add("Ensure good lighting and avoid shadows on the document")
        }
      }
      
      "payslip" -> {
        // Payslips are typically portrait orientation
        if (bitmap.width > bitmap.height) {
          issues.add("Payslip appears to be in landscape orientation")
          suggestions.add("Rotate phone to portrait mode for better capture")
        }
        
        // Check for sufficient detail (payslips have lots of text)
        if (bitmap.width < 1200 || bitmap.height < 1600) {
          issues.add("Resolution may be too low for payslip text to be readable")
          suggestions.add("Move closer to the document or use higher camera resolution")
        }
      }
      
      "loan_application_front", "loan_application_back" -> {
        // Loan application forms are typically portrait orientation
        if (bitmap.width > bitmap.height) {
          issues.add("Loan application form appears to be in landscape orientation")
          suggestions.add("Rotate phone to portrait mode for better capture")
        }
        
        // Check for sufficient resolution for form field text
        if (bitmap.width < 1400 || bitmap.height < 1800) {
          issues.add("Resolution may be too low for form fields to be readable")
          suggestions.add("Move closer to the document and ensure good lighting for clear text capture")
        }
        
        // Check for sufficient contrast (forms should have clear text and field boundaries)
        if (!hasGoodContrast(bitmap)) {
          issues.add("Low contrast detected - form fields may not be clearly visible")
          suggestions.add("Ensure good lighting and avoid shadows on the form")
        }
      }
    }
    
    return DocumentValidationResult(
      isValid = issues.isEmpty(),
      issues = issues,
      suggestions = suggestions
    )
  }

  /**
   * Estimate file size and validate against limits
   */
  private fun estimateFileSize(bitmap: Bitmap, config: QualityConfig): ValidationResult {
    // Rough estimation: assume JPEG compression will result in ~1/8 of raw size
    val rawSizeBytes = bitmap.width * bitmap.height * 4 // 4 bytes per pixel (ARGB)
    val estimatedJpegSize = rawSizeBytes / 8 // Rough JPEG compression estimate
    val estimatedSizeMB = estimatedJpegSize / (1024 * 1024)
    
    return ValidationResult(
      isValid = estimatedSizeMB <= config.maxFileSizeMB,
      message = if (estimatedSizeMB > config.maxFileSizeMB) "Estimated file size (${estimatedSizeMB}MB) exceeds limit" else "",
      suggestion = "Consider reducing camera resolution if file size is too large"
    )
  }

  /**
   * Convert color pixel to grayscale value
   */
  private fun getGrayscale(pixel: Int): Int {
    val r = (pixel shr 16) and 0xFF
    val g = (pixel shr 8) and 0xFF
    val b = pixel and 0xFF
    return (r * 0.299 + g * 0.587 + b * 0.114).toInt()
  }

  /**
   * Check if image has good contrast (simple standard deviation check)
   */
  private fun hasGoodContrast(bitmap: Bitmap): Boolean {
    val sampleSize = minOf(1000, bitmap.width * bitmap.height / 1000)
    val grayscaleValues = mutableListOf<Int>()
    
    // Sample pixels evenly
    val stepX = maxOf(1, bitmap.width / 32)
    val stepY = maxOf(1, bitmap.height / 32)
    
    for (y in 0 until bitmap.height step stepY) {
      for (x in 0 until bitmap.width step stepX) {
        if (grayscaleValues.size < sampleSize) {
          grayscaleValues.add(getGrayscale(bitmap.getPixel(x, y)))
        }
      }
    }
    
    if (grayscaleValues.isEmpty()) return true
    
    val mean = grayscaleValues.average()
    val variance = grayscaleValues.map { (it - mean) * (it - mean) }.average()
    val standardDeviation = sqrt(variance)
    
    // Good contrast should have standard deviation > 20
    return standardDeviation > 20.0
  }
}

/**
 * Basic validation result
 */
private data class ValidationResult(
  val isValid: Boolean,
  val message: String = "",
  val suggestion: String = ""
)

/**
 * Document-specific validation result
 */
private data class DocumentValidationResult(
  val isValid: Boolean,
  val issues: List<String> = emptyList(),
  val suggestions: List<String> = emptyList()
) 