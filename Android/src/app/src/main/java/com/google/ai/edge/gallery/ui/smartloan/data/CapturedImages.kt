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

package com.google.ai.edge.gallery.ui.smartloan.data

import android.graphics.Bitmap

/**
 * Data class for storing ID document images
 */
data class IdImages(
  val frontId: Bitmap? = null,
  val backId: Bitmap? = null,
  val frontIdPath: String? = null,
  val backIdPath: String? = null
)

/**
 * Data class for storing payslip images (up to 4)
 */
data class PayslipImages(
  val payslip1: Bitmap? = null,
  val payslip2: Bitmap? = null,
  val payslip3: Bitmap? = null,
  val payslip4: Bitmap? = null,
  val paths: List<String> = emptyList()
) {
  /**
   * Get payslip bitmap by index (0-3)
   */
  fun getPayslipByIndex(index: Int): Bitmap? {
    return when (index) {
      0 -> payslip1
      1 -> payslip2
      2 -> payslip3
      3 -> payslip4
      else -> null
    }
  }

  /**
   * Get the number of captured payslips
   */
  fun getCapturedCount(): Int {
    return listOfNotNull(payslip1, payslip2, payslip3, payslip4).size
  }

  /**
   * Create a copy with a payslip updated at the specified index
   */
  fun updatePayslipAt(index: Int, bitmap: Bitmap?, path: String?): PayslipImages {
    val newPaths = paths.toMutableList()
    
    // Update or add path
    if (index < newPaths.size) {
      if (path != null) {
        newPaths[index] = path
      } else {
        newPaths.removeAt(index)
      }
    } else if (path != null) {
      // Extend the list if needed
      while (newPaths.size <= index) {
        newPaths.add("")
      }
      newPaths[index] = path
    }

    return when (index) {
      0 -> copy(payslip1 = bitmap, paths = newPaths)
      1 -> copy(payslip2 = bitmap, paths = newPaths)
      2 -> copy(payslip3 = bitmap, paths = newPaths)
      3 -> copy(payslip4 = bitmap, paths = newPaths)
      else -> this
    }
  }
}

/**
 * Data class for storing loan application form images (front and back)
 */
data class LoanApplicationImages(
  val frontForm: Bitmap? = null,
  val backForm: Bitmap? = null,
  val frontFormPath: String? = null,
  val backFormPath: String? = null
) {
  /**
   * Check if loan application capture is complete (both front and back captured)
   */
  fun isComplete(): Boolean {
    return frontForm != null && backForm != null
  }

  /**
   * Get the number of captured form pages
   */
  fun getCapturedCount(): Int {
    return listOfNotNull(frontForm, backForm).size
  }
}

/**
 * Main data class for Smart Loan application containing all captured images
 */
data class SmartLoanApplicationData(
  val idImages: IdImages = IdImages(),
  val payslipImages: PayslipImages = PayslipImages(),
  val loanApplicationImages: LoanApplicationImages = LoanApplicationImages(),
  val timestamp: Long = System.currentTimeMillis()
) {
  /**
   * Check if ID capture is complete (both front and back captured)
   */
  fun isIdCaptureComplete(): Boolean {
    return idImages.frontId != null && idImages.backId != null
  }

  /**
   * Check if payslip capture is complete (all 4 payslips captured)
   */
  fun isPayslipCaptureComplete(): Boolean {
    return payslipImages.getCapturedCount() >= 4
  }

  /**
   * Check if minimum payslip requirement is met (at least 1 payslip)
   */
  fun hasMinimumPayslips(): Boolean {
    return payslipImages.getCapturedCount() >= 1
  }

  /**
   * Check if loan application form capture is complete (both front and back captured)
   */
  fun isLoanApplicationCaptureComplete(): Boolean {
    return loanApplicationImages.isComplete()
  }

  /**
   * Check if all required documents are captured
   */
  fun isAllDocumentsCaptured(): Boolean {
    return isIdCaptureComplete() && hasMinimumPayslips() && isLoanApplicationCaptureComplete()
  }
} 