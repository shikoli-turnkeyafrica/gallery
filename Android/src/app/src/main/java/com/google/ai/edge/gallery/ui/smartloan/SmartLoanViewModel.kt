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

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing captured ID images
 */
data class IdImages(
  val frontImage: Uri? = null,
  val backImage: Uri? = null,
)

/**
 * Data class representing payslip images (up to 4)
 */
data class PayslipImages(
  val images: List<Uri> = emptyList(),
) {
  val isComplete: Boolean get() = images.isNotEmpty()
  val canAddMore: Boolean get() = images.size < 4
}

/**
 * Data class representing extracted loan application data
 */
data class LoanApplicationData(
  val applicantName: String = "",
  val nationalId: String = "",
  val monthlyIncome: Double = 0.0,
  val employer: String = "",
  val isValid: Boolean = false,
)

/**
 * Data class representing loan offer details
 */
data class LoanOfferData(
  val maxLoanAmount: Double = 0.0,
  val interestRate: Double = 0.0,
  val loanTerm: Int = 0, // in months
  val monthlyPayment: Double = 0.0,
  val processingFee: Double = 0.0,
)

/**
 * UI state for the Smart Loan application flow
 */
data class SmartLoanUiState(
  val idImages: IdImages = IdImages(),
  val payslipImages: PayslipImages = PayslipImages(),
  val applicationData: LoanApplicationData = LoanApplicationData(),
  val loanOffer: LoanOfferData = LoanOfferData(),
  val isValidating: Boolean = false,
  val validationProgress: Float = 0f,
  val validationStatus: String = "Starting validation...",
  val errorMessage: String? = null,
)

/**
 * ViewModel for managing Smart Loan application flow
 */
@HiltViewModel
class SmartLoanViewModel @Inject constructor() : ViewModel() {
  
  private val _uiState = MutableStateFlow(SmartLoanUiState())
  val uiState: StateFlow<SmartLoanUiState> = _uiState.asStateFlow()

  /**
   * Capture front ID image
   */
  fun captureIdFront(imageUri: Uri) {
    val currentImages = _uiState.value.idImages
    _uiState.value = _uiState.value.copy(
      idImages = currentImages.copy(frontImage = imageUri)
    )
  }

  /**
   * Capture back ID image
   */
  fun captureIdBack(imageUri: Uri) {
    val currentImages = _uiState.value.idImages
    _uiState.value = _uiState.value.copy(
      idImages = currentImages.copy(backImage = imageUri)
    )
  }

  /**
   * Add payslip image
   */
  fun addPayslipImage(imageUri: Uri) {
    val currentImages = _uiState.value.payslipImages
    if (currentImages.canAddMore) {
      val updatedImages = currentImages.images + imageUri
      _uiState.value = _uiState.value.copy(
        payslipImages = PayslipImages(updatedImages)
      )
    }
  }

  /**
   * Remove payslip image
   */
  fun removePayslipImage(index: Int) {
    val currentImages = _uiState.value.payslipImages
    val updatedImages = currentImages.images.toMutableList().apply {
      if (index in 0 until size) removeAt(index)
    }
    _uiState.value = _uiState.value.copy(
      payslipImages = PayslipImages(updatedImages)
    )
  }

  /**
   * Start validation process (simulated for now)
   */
  fun startValidation() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isValidating = true,
        validationProgress = 0f,
        validationStatus = "Processing ID documents..."
      )

      // Simulate ID processing
      delay(1500)
      _uiState.value = _uiState.value.copy(
        validationProgress = 0.25f,
        validationStatus = "Extracting personal information..."
      )

      delay(1500)
      _uiState.value = _uiState.value.copy(
        validationProgress = 0.5f,
        validationStatus = "Processing payslip documents..."
      )

      delay(1500)
      _uiState.value = _uiState.value.copy(
        validationProgress = 0.75f,
        validationStatus = "Calculating loan eligibility..."
      )

      delay(1500)
      _uiState.value = _uiState.value.copy(
        validationProgress = 1f,
        validationStatus = "Generating loan offer..."
      )

      // Simulate extracted data and loan calculation
      delay(1000)
      val mockApplicationData = LoanApplicationData(
        applicantName = "John Doe",
        nationalId = "12345678",
        monthlyIncome = 75000.0,
        employer = "Tech Solutions Ltd",
        isValid = true
      )

      val mockLoanOffer = LoanOfferData(
        maxLoanAmount = 500000.0,
        interestRate = 15.5,
        loanTerm = 12,
        monthlyPayment = 48750.0,
        processingFee = 5000.0
      )

      _uiState.value = _uiState.value.copy(
        isValidating = false,
        applicationData = mockApplicationData,
        loanOffer = mockLoanOffer,
        validationStatus = "Validation complete!"
      )
    }
  }

  /**
   * Reset application state
   */
  fun resetApplication() {
    _uiState.value = SmartLoanUiState()
  }

  /**
   * Check if ID capture is complete
   */
  fun isIdCaptureComplete(): Boolean {
    val images = _uiState.value.idImages
    return images.frontImage != null && images.backImage != null
  }

  /**
   * Check if payslip capture is complete
   */
  fun isPayslipCaptureComplete(): Boolean {
    return _uiState.value.payslipImages.isComplete
  }
} 