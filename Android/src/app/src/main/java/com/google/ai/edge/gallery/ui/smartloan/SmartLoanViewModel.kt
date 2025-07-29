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

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.TASK_SMART_LOAN
import com.google.ai.edge.gallery.data.TASKS
import com.google.ai.edge.gallery.data.getModelByName
import com.google.ai.edge.gallery.ui.smartloan.data.SmartLoanApplicationData
import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.ai.edge.gallery.ui.smartloan.storage.SecureImageStorage
import com.google.ai.edge.gallery.ui.smartloan.validation.RuleValidationEngine
import com.google.ai.edge.gallery.ui.smartloan.validation.ApplicationValidationResult
import com.google.ai.edge.gallery.ui.smartloan.finance.LoanOfferGenerator
import com.google.ai.edge.gallery.ui.smartloan.finance.LoanOffer
import com.google.ai.edge.gallery.ui.smartloan.finance.AffordabilityCalculator
import com.google.ai.edge.gallery.ui.smartloan.memo.DisbursementMemo
import com.google.ai.edge.gallery.ui.smartloan.memo.DisbursementMemoGenerator
import com.google.ai.edge.gallery.ui.smartloan.storage.MemoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "SmartLoanViewModel"

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
 * UI state for the Smart Loan application flow
 */
data class SmartLoanUiState(
  val applicationData: SmartLoanApplicationData = SmartLoanApplicationData(),
  val extractedData: ExtractedApplicationData? = null,
  val loanOffer: LoanOffer = LoanOffer(
    maxLoanAmount = 0.0,
    recommendedAmount = 0.0,
    interestRate = 0.0,
    loanTerm = 0,
    monthlyPayment = 0.0,
    totalRepayment = 0.0,
    totalInterest = 0.0,
    processingFee = 0.0,
    dsr = 0.0,
    offerValidUntil = 0L
  ),
  // Business rules validation
  val validationResult: ApplicationValidationResult? = null,
  val validationInProgress: Boolean = false,
  val validationError: String? = null,
  // AI Extraction fields
  val extractionProgress: Float = 0f,
  val extractionStatus: String = "",
  val extractionError: String? = null,
  // Legacy validation fields (for backward compatibility)
  val isValidating: Boolean = false,
  val validationProgress: Float = 0f,
  val validationStatus: String = "Starting validation...",
  val errorMessage: String? = null,
  val isLoading: Boolean = false,
  // Task 05: Offer & Acceptance Flow
  val showTermsDialog: Boolean = false,
  val showDeclineDialog: Boolean = false,
  val showAmountAdjustment: Boolean = false,
  val isAcceptingLoan: Boolean = false,
  val acceptanceError: String? = null,
  val disbursementMemo: DisbursementMemo? = null,
  val isLoanAccepted: Boolean = false,
)

/**
 * ViewModel for managing Smart Loan application flow
 */
@HiltViewModel
class SmartLoanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureImageStorage: SecureImageStorage,
    private val ruleValidationEngine: RuleValidationEngine
) : ViewModel() {
  
  private val _uiState = MutableStateFlow(SmartLoanUiState())
  val uiState: StateFlow<SmartLoanUiState> = _uiState.asStateFlow()
  
  private val affordabilityCalculator = AffordabilityCalculator()
  private var loanOfferGenerator: LoanOfferGenerator? = null
  
  // Task 05: New components
  private val biometricAuthenticator = BiometricAuthenticator(context)
  private val disbursementMemoGenerator = DisbursementMemoGenerator()
  private val memoStorage = MemoStorage(context)
  
  init {
    // Initialize validation engine
    try {
      ruleValidationEngine.initialize()
      val policy = ruleValidationEngine.getPolicyConfiguration()
      loanOfferGenerator = LoanOfferGenerator(affordabilityCalculator, policy)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize validation engine", e)
    }
  }

  /**
   * Capture and securely store front ID image
   */
  fun captureIdFront(bitmap: Bitmap) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val filename = secureImageStorage.generateFilename("id_front")
        val savedPath = secureImageStorage.saveImageSecurely(bitmap, filename)
        
        if (savedPath != null) {
          val currentAppData = _uiState.value.applicationData
          val updatedIdImages = currentAppData.idImages.copy(
            frontId = bitmap,
            frontIdPath = savedPath
          )
          val updatedAppData = currentAppData.copy(idImages = updatedIdImages)
          
          _uiState.value = _uiState.value.copy(
            applicationData = updatedAppData,
            isLoading = false,
            errorMessage = null
          )
          
          Log.d(TAG, "Front ID image captured and saved securely")
        } else {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = "Failed to save front ID image"
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error capturing front ID image", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          errorMessage = "Error capturing front ID: ${e.message}"
        )
      }
    }
  }

  /**
   * Capture and securely store back ID image
   */
  fun captureIdBack(bitmap: Bitmap) {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val filename = secureImageStorage.generateFilename("id_back")
        val savedPath = secureImageStorage.saveImageSecurely(bitmap, filename)
        
        if (savedPath != null) {
          val currentAppData = _uiState.value.applicationData
          val updatedIdImages = currentAppData.idImages.copy(
            backId = bitmap,
            backIdPath = savedPath
          )
          val updatedAppData = currentAppData.copy(idImages = updatedIdImages)
          
          _uiState.value = _uiState.value.copy(
            applicationData = updatedAppData,
            isLoading = false,
            errorMessage = null
          )
          
          Log.d(TAG, "Back ID image captured and saved securely")
        } else {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = "Failed to save back ID image"
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error capturing back ID image", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          errorMessage = "Error capturing back ID: ${e.message}"
        )
      }
    }
  }

  /**
   * Capture and securely store payslip image at specific index
   */
  fun capturePayslipImage(index: Int, bitmap: Bitmap) {
    if (index !in 0..3) {
      Log.w(TAG, "Invalid payslip index: $index")
      return
    }

    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val filename = secureImageStorage.generateFilename("payslip_${index + 1}")
        val savedPath = secureImageStorage.saveImageSecurely(bitmap, filename)
        
        if (savedPath != null) {
          val currentAppData = _uiState.value.applicationData
          val updatedPayslipImages = currentAppData.payslipImages.updatePayslipAt(
            index, bitmap, savedPath
          )
          val updatedAppData = currentAppData.copy(payslipImages = updatedPayslipImages)
          
          _uiState.value = _uiState.value.copy(
            applicationData = updatedAppData,
            isLoading = false,
            errorMessage = null
          )
          
          Log.d(TAG, "Payslip ${index + 1} image captured and saved securely")
        } else {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = "Failed to save payslip image"
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error capturing payslip image at index $index", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          errorMessage = "Error capturing payslip: ${e.message}"
        )
      }
    }
  }

  /**
   * Delete image at specific location
   */
  fun deleteImage(imageType: String, index: Int? = null) {
    viewModelScope.launch {
      try {
        val currentAppData = _uiState.value.applicationData
        
        when (imageType.lowercase()) {
          "id_front" -> {
            currentAppData.idImages.frontIdPath?.let { path ->
              secureImageStorage.deleteImageSecurely(path)
            }
            val updatedIdImages = currentAppData.idImages.copy(
              frontId = null,
              frontIdPath = null
            )
            _uiState.value = _uiState.value.copy(
              applicationData = currentAppData.copy(idImages = updatedIdImages)
            )
          }
          "id_back" -> {
            currentAppData.idImages.backIdPath?.let { path ->
              secureImageStorage.deleteImageSecurely(path)
            }
            val updatedIdImages = currentAppData.idImages.copy(
              backId = null,
              backIdPath = null
            )
            _uiState.value = _uiState.value.copy(
              applicationData = currentAppData.copy(idImages = updatedIdImages)
            )
          }
          "payslip" -> {
            if (index != null && index in 0..3) {
              // Delete the specific payslip image
              if (index < currentAppData.payslipImages.paths.size) {
                val pathToDelete = currentAppData.payslipImages.paths[index]
                secureImageStorage.deleteImageSecurely(pathToDelete)
              }
              
              val updatedPayslipImages = currentAppData.payslipImages.updatePayslipAt(
                index, null, null
              )
              _uiState.value = _uiState.value.copy(
                applicationData = currentAppData.copy(payslipImages = updatedPayslipImages)
              )
            }
          }
        }
        
        Log.d(TAG, "Image deleted: $imageType${index?.let { " at index $it" } ?: ""}")
      } catch (e: Exception) {
        Log.e(TAG, "Error deleting image: $imageType", e)
        _uiState.value = _uiState.value.copy(
          errorMessage = "Error deleting image: ${e.message}"
        )
      }
    }
  }

  /**
   * Load previously saved data from secure storage
   */
  fun loadSavedData() {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        // In a real implementation, you might save/load application metadata
        // For now, we'll just clear the loading state
        _uiState.value = _uiState.value.copy(isLoading = false)
        
        Log.d(TAG, "Saved data loaded successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Error loading saved data", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          errorMessage = "Error loading saved data: ${e.message}"
        )
      }
    }
  }

  /**
   * Clear error message
   */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
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

      // Legacy mock data for backward compatibility
      delay(1000)
      val mockExtractedData = LoanApplicationData(
        applicantName = "John Doe",
        nationalId = "12345678",
        monthlyIncome = 75000.0,
        employer = "Tech Solutions Ltd",
        isValid = true
      )

      val mockLoanOffer = LoanOffer(
        maxLoanAmount = 500000.0,
        recommendedAmount = 0.0, // Assuming recommended amount is 0 for mock
        interestRate = 15.5,
        loanTerm = 12,
        monthlyPayment = 48750.0,
        totalRepayment = 585000.0, // Mock total repayment
        totalInterest = 85000.0, // Mock total interest
        processingFee = 5000.0,
        dsr = 0.0, // Mock DSR
        offerValidUntil = System.currentTimeMillis() + 3600000 // 1 hour validity
      )

      _uiState.value = _uiState.value.copy(
        isValidating = false,
        validationStatus = "Validation complete!",
        loanOffer = mockLoanOffer
      )
    }
  }

  /**
   * Start AI-powered data extraction from captured images
   */
  fun startDataExtraction() {
    viewModelScope.launch(Dispatchers.Default) {
      try {
        val currentData = _uiState.value.applicationData
        
        // Check if we have required images
        if (!currentData.isIdCaptureComplete()) {
          _uiState.value = _uiState.value.copy(
            extractionError = "ID images are required for processing"
          )
          return@launch
        }
        
        if (!currentData.hasMinimumPayslips()) {
          _uiState.value = _uiState.value.copy(
            extractionError = "At least one payslip is required for processing"
          )
          return@launch
        }

        // Reset extraction state
        _uiState.value = _uiState.value.copy(
          extractionProgress = 0f,
          extractionStatus = "Initializing AI model...",
          extractionError = null
        )

        // Find the Gemma model from ANY task (since it works in Ask Image, it must be initialized somewhere)
        Log.d(TAG, "=== SEARCHING FOR GEMMA MODEL ===")
        var gemmaModel: Model? = null
        
        // First try TASK_SMART_LOAN
        Log.d(TAG, "TASK_SMART_LOAN models: ${TASK_SMART_LOAN.models.map { "${it.name}(instance=${it.instance != null})" }}")
        gemmaModel = TASK_SMART_LOAN.models.find { it.name == "Gemma-3n-E4B-it-int4" }
        
        // If not found in Smart Loan, search all tasks (Ask Image, etc.)
        if (gemmaModel == null) {
          Log.d(TAG, "Model not found in TASK_SMART_LOAN, searching all tasks...")
          for (task in TASKS) {
            Log.d(TAG, "Checking task ${task.type.id} with ${task.models.size} models")
            for (model in task.models) {
              Log.d(TAG, "  - ${model.name} (instance=${model.instance != null})")
            }
            gemmaModel = task.models.find { it.name == "Gemma-3n-E4B-it-int4" }
            if (gemmaModel != null) {
              Log.d(TAG, "✅ Found Gemma model in task: ${task.type} (instance=${gemmaModel.instance != null})")
              break
            }
          }
        }
        
        // Also try getModelByName as final fallback
        if (gemmaModel == null) {
          gemmaModel = getModelByName("Gemma-3n-E4B-it-int4")
          if (gemmaModel != null) {
            Log.d(TAG, "✅ Found Gemma model via getModelByName")
          }
        }
        
        if (gemmaModel == null) {
          Log.e(TAG, "❌ No Gemma model found anywhere!")
          _uiState.value = _uiState.value.copy(
            extractionError = "No suitable AI model found. Please download the Gemma-3n-E4B-it-int4 model from Model Manager first."
          )
          return@launch
        }

        // Check if model is initialized (same check as Ask Image)
        if (gemmaModel.instance == null) {
          Log.e(TAG, "❌ Model not initialized! Model found but instance is null.")
          Log.d(TAG, "Model details: name=${gemmaModel.name}, instance=${gemmaModel.instance}")
          _uiState.value = _uiState.value.copy(
            extractionError = "AI model not initialized. Please go to Model Manager, find the Gemma-3n-E4B-it-int4 model, and initialize it first."
          )
          return@launch
        }

        Log.d(TAG, "✅ Using initialized model instance (same as Ask Image approach)")

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.2f,
          extractionStatus = "Processing front ID image..."
        )

        // Extract ID data using Ask Image approach
        val frontIdResult = extractDataFromImage(
          model = gemmaModel,
          bitmap = currentData.idImages.frontId!!,
          prompt = createSimpleIdExtractionPrompt()
        )

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.4f,
          extractionStatus = "Processing back ID image..."
        )

        val backIdResult = extractDataFromImage(
          model = gemmaModel,
          bitmap = currentData.idImages.backId!!,
          prompt = createSimpleIdExtractionPrompt()
        )

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.6f,
          extractionStatus = "Processing payslips..."
        )

        // Get all available payslips
        val payslipBitmaps = listOfNotNull(
          currentData.payslipImages.payslip1,
          currentData.payslipImages.payslip2,
          currentData.payslipImages.payslip3,
          currentData.payslipImages.payslip4
        )

        Log.d(TAG, "Processing ${payslipBitmaps.size} payslips...")
        
        val payslipResults = mutableListOf<String>()
        val allPayslipData = mutableListOf<PayslipData>()
        
        // Process each payslip individually
        payslipBitmaps.forEachIndexed { index, payslipBitmap ->
          _uiState.value = _uiState.value.copy(
            extractionProgress = 0.6f + (index * 0.2f / payslipBitmaps.size),
            extractionStatus = "Processing payslip ${index + 1} of ${payslipBitmaps.size}..."
          )
          
          val payslipResult = extractDataFromImage(
            model = gemmaModel,
            bitmap = payslipBitmap,
            prompt = createSimplePayslipExtractionPrompt()
          )
          
          payslipResults.add(payslipResult)
          Log.d(TAG, "🔍 Payslip ${index + 1} extraction returned: '${payslipResult.take(200)}${if(payslipResult.length > 200) "..." else ""}'")
          
          // Parse individual payslip and add to list
          val payslipData = parsePayslipDataFromText(payslipResult)
          if (payslipData.isValid) {
            allPayslipData.add(payslipData)
            Log.d(TAG, "✅ Payslip ${index + 1} valid: ${payslipData.payPeriod}")
          } else {
            Log.w(TAG, "❌ Payslip ${index + 1} invalid or failed parsing")
          }
          
          // Add delay between payslip processing
          delay(1000)
          
          // Reset session after each payslip (important for model state)
          Log.d(TAG, "🔄 Resetting session after payslip ${index + 1} extraction...")
          LlmChatModelHelper.resetSession(gemmaModel)
        }

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.9f,
          extractionStatus = "Finalizing extraction..."
        )

        Log.d(TAG, "=== EXTRACTION RESULTS (Ask Image Approach) ===")
        Log.d(TAG, "Front ID: $frontIdResult")
        Log.d(TAG, "Back ID: $backIdResult")
        payslipResults.forEachIndexed { index, result ->
          Log.d(TAG, "Payslip ${index + 1}: ${result.take(100)}...")
        }
        Log.d(TAG, "Total valid payslips processed: ${allPayslipData.size}")

        // Parse the results into structured data
        val idData = parseIdDataFromText(frontIdResult + " " + backIdResult)

        // Create extracted data structure with ALL payslips
        val extractedData = ExtractedApplicationData(
          idCardData = idData,
          payslipData = allPayslipData,
          extractionStartTime = System.currentTimeMillis() - 5000,
          extractionEndTime = System.currentTimeMillis()
        ).calculateOverallConfidence()

        _uiState.value = _uiState.value.copy(
          extractionProgress = 1.0f,
          extractionStatus = "Extraction completed successfully!",
          extractedData = extractedData
        )

        Log.d(TAG, "AI extraction completed. Overall confidence: ${extractedData.overallConfidence}")
        Log.d(TAG, "🚨 DEBUG: Line after AI extraction completed - execution continuing...")
        Log.d(TAG, "🚨 DEBUG: extractedData is null: ${extractedData == null}")
        
        // Trigger business rules validation after successful extraction
        // Move to main thread to avoid potential coroutine cancellation issues
        withContext(Dispatchers.Main) {
          try {
            Log.d(TAG, "About to call validateApplicationWithBusinessRules() ...")
            Log.d(TAG, "ruleValidationEngine is null: ${ruleValidationEngine == null}")
            Log.d(TAG, "loanOfferGenerator is null: ${loanOfferGenerator == null}")
            validateApplicationWithBusinessRules()
            Log.d(TAG, "validateApplicationWithBusinessRules() call completed")
          } catch (e: Exception) {
            Log.e(TAG, "Exception calling validateApplicationWithBusinessRules()", e)
          }
        }

      } catch (e: Exception) {
        Log.e(TAG, "Data extraction failed", e)
        _uiState.value = _uiState.value.copy(
          extractionError = "Data extraction failed: ${e.message}"
        )
      }
    }
  }

  /**
   * Extract data from image using EXACTLY the same approach as Ask Image
   * This literally copies LlmChatViewModelBase.generateResponse() logic
   */
  private suspend fun extractDataFromImage(model: Model, bitmap: Bitmap, prompt: String): String {
    Log.d(TAG, "🔄 Using EXACT Ask Image generateResponse approach...")
    Log.d(TAG, "Prompt: $prompt")
    
    // EXACT copy of Ask Image's generateResponse method
    val accelerator = model.getStringConfigValue(key = com.google.ai.edge.gallery.data.ConfigKey.ACCELERATOR, defaultValue = "")
    
    // Wait for instance to be initialized (EXACT copy of Ask Image)
    while (model.instance == null) {
      delay(100)
    }
    delay(500) // EXACT warmup delay from Ask Image
    
    return suspendCancellableCoroutine { continuation ->
      try {
        
        // Run inference with EXACT same parameters as Ask Image
        val instance = model.instance as com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
        var prefillTokens = instance.session.sizeInTokens(prompt)
        prefillTokens += 1 * 257  // EXACT image token calculation from Ask Image
        
        var firstRun = true
        var timeToFirstToken = 0f
        var firstTokenTs = 0L
        var decodeTokens = 0
        var prefillSpeed = 0f
        var decodeSpeed: Float
        val start = System.currentTimeMillis()
        
        var fullResponse = ""
        
        LlmChatModelHelper.runInference(
          model = model,
          input = prompt,
          images = listOf(bitmap),
          resultListener = { partialResult, done ->
            val curTs = System.currentTimeMillis()
            
            // Add debug logging to understand what's happening
            Log.d(TAG, "🔍 Streaming callback - done: $done, partialResult length: ${partialResult.length}")
            Log.d(TAG, "🔍 Partial result preview: '${partialResult.take(100)}...'")
            
            if (firstRun) {
              firstTokenTs = System.currentTimeMillis()
              timeToFirstToken = (firstTokenTs - start) / 1000f
              prefillSpeed = prefillTokens / timeToFirstToken
              firstRun = false
            } else {
              decodeTokens++
            }
            
            // Don't overwrite with empty or whitespace-only results - MediaPipe sends an empty final callback
            if (partialResult.isNotBlank()) {
              fullResponse += partialResult
            }
            Log.d(TAG, "🔍 Current fullResponse length: ${fullResponse.length}")
            
            if (done) {
              val latencyMs = System.currentTimeMillis() - start
              decodeSpeed = decodeTokens / ((curTs - firstTokenTs) / 1000f)
              if (decodeSpeed.isNaN()) {
                decodeSpeed = 0f
              }
              
              Log.d(TAG, "=== AI RESPONSE (EXACT Ask Image Clone) ===")
              Log.d(TAG, "Response: $fullResponse")
              Log.d(TAG, "Time to first token: ${timeToFirstToken}s")
              Log.d(TAG, "Decode speed: ${decodeSpeed} tokens/s")
              Log.d(TAG, "=== END AI RESPONSE ===")
              
              fullResponse = fullResponse.trim()

              // Check if response is empty and log warning
              if (fullResponse.isEmpty()) {
                Log.w(TAG, "⚠️ WARNING: AI response is empty! This indicates an issue with model inference.")
                Log.w(TAG, "⚠️ Model: ${model.name}, Prompt length: ${prompt.length}")
                Log.w(TAG, "⚠️ Decode tokens: $decodeTokens, Time to first token: ${timeToFirstToken}s")
              } else {
                Log.d(TAG, "✅ AI response received successfully (${fullResponse.length} characters)")
              }
              
              continuation.resume(fullResponse)
            }
          },
          cleanUpListener = {
            Log.d(TAG, "Inference cleanup completed (Ask Image style)")
          }
        )
        
      } catch (e: Exception) {
        Log.e(TAG, "Error in extractDataFromImage", e)
        continuation.resume("Error: ${e.message}")
      }
    }
  }

  /**
   * Simple ID extraction prompt (much simpler than function calling)
   */
  private fun createSimpleIdExtractionPrompt(): String {
    return """
    Look at this Kenyan National ID card and extract the following information:
    - Full name
    - ID number (8 digits)
    - Date of birth
    - Any other visible details
    
    Please provide a clear, structured response with the extracted information.
    """.trimIndent()
  }

  /**
   * Simple payslip extraction prompt
   */
  private fun createSimplePayslipExtractionPrompt(): String {
    return """
    Look at this payslip and extract the following information:
    - Employee name
    - Employer/Company name
    - Gross salary amount
    - Net salary amount
    - Pay period/month
    - All deductions (with names and amounts)
    - All allowances (with names and amounts)
    
    Please provide a clear, structured response with the extracted information.
    For deductions and allowances, list each item with its name and amount clearly.
    """.trimIndent()
  }

  /**
   * Parse ID data from text response (simplified)
   */
  private fun parseIdDataFromText(response: String): IdCardData {
    // Simple parsing - look for patterns in the response
    val fullName = extractField(response, listOf("name", "full name", "Name"))
    val idNumber = extractIdNumber(response)
    val dateOfBirth = extractDate(response, listOf("birth", "born", "DOB"))
    
    return IdCardData(
      fullName = fullName,
      idNumber = idNumber,
      dateOfBirth = dateOfBirth,
      extractionConfidence = if (fullName.isNotBlank() || idNumber.isNotBlank()) 0.8f else 0.1f
    ).validate()
  }

  /**
   * Parse payslip data from text response (simplified)
   */
  private fun parsePayslipDataFromText(response: String): PayslipData {
    val employeeName = extractField(response, listOf("employee", "name"))
    val employerName = extractField(response, listOf("employer", "company"))
    val grossSalary = extractSalary(response, listOf("gross", "total"))
    val netSalary = extractSalary(response, listOf("net", "take home"))
    val payPeriod = extractField(response, listOf("pay period", "month", "period"))
    val formattedPayPeriod = convertPayPeriodFormat(payPeriod)
    
    // Extract deductions and allowances
    val deductions = extractDeductionsFromText(response)
    val allowances = extractAllowancesFromText(response)
    
    Log.d(TAG, "🔍 Parsed payslip data:")
    Log.d(TAG, "  Employee: '$employeeName'")
    Log.d(TAG, "  Employer: '$employerName'") 
    Log.d(TAG, "  Gross: $grossSalary")
    Log.d(TAG, "  Net: $netSalary")
    Log.d(TAG, "  Pay Period: '$payPeriod' -> '$formattedPayPeriod'")
    Log.d(TAG, "  Deductions: $deductions")
    Log.d(TAG, "  Allowances: $allowances")
    
    val payslipData = PayslipData(
      employeeName = employeeName,
      employerName = employerName,
      grossSalary = grossSalary,
      netSalary = netSalary,
      payPeriod = formattedPayPeriod,
      deductions = deductions,
      allowances = allowances,
      extractionConfidence = if (employeeName.isNotBlank() || grossSalary > 0) 0.8f else 0.1f
    ).validate()
    
    Log.d(TAG, "🔍 PayslipData isValid: ${payslipData.isValid}")
    
    return payslipData
  }

  /**
   * Helper function to extract field values
   */
  private fun extractField(text: String, keywords: List<String>): String {
    keywords.forEach { keyword ->
      // More flexible regex to handle markdown formatting and various separators
      val regex = "(?i)(?:\\*+\\s*)?(?:employee\\s+)?(?:employer/company\\s+)?${keyword}[\\s/]*(?:name)?[\\s]*:+\\*+\\s*([^\\n\\r\\*]+)".toRegex()
      val match = regex.find(text)
      if (match != null) {
        // Clean up Markdown or bullet characters (e.g. "**", "***", "<", ",")
        val raw = match.groupValues[1].trim()
        val cleaned = raw.replace(Regex("[*<>]+"), "").trim()
        return cleaned
      }
    }
    
    // Fallback: try simpler pattern
    keywords.forEach { keyword ->
      val regex = "(?i)$keyword[:\\s-]*([^\\n\\r]{1,50})".toRegex()
      val match = regex.find(text)
      if (match != null) {
        // Clean up Markdown or bullet characters (e.g. "**", "***", "<", ",")
        val raw = match.groupValues[1].trim()
        val cleaned = raw.replace(Regex("[*<>]+"), "").trim()
        return cleaned
      }
    }
    return ""
  }

  /**
   * Helper function to extract ID number
   */
  private fun extractIdNumber(text: String): String {
    val regex = "\\b\\d{8}\\b".toRegex()
    val match = regex.find(text)
    return match?.value ?: ""
  }

  /**
   * Helper function to extract dates
   */
  private fun extractDate(text: String, keywords: List<String>): String {
    keywords.forEach { keyword ->
      val regex = "(?i)$keyword[:\\s-]*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})".toRegex()
      val match = regex.find(text)
      if (match != null) {
        return match.groupValues[1].trim()
      }
    }
    return ""
  }

  /**
   * Helper function to convert month name format to YYYY-MM format
   */
  private fun convertPayPeriodFormat(payPeriod: String): String {
    if (payPeriod.isBlank()) return ""
    
    // Handle "March-2025" format
    val monthYearRegex = "(?i)(january|february|march|april|may|june|july|august|september|october|november|december)[-\\s]*(\\d{4})".toRegex()
    val match = monthYearRegex.find(payPeriod)
    
    if (match != null) {
      val monthName = match.groupValues[1].lowercase()
      val year = match.groupValues[2]
      
      val monthNumber = when (monthName) {
        "january" -> "01"
        "february" -> "02"
        "march" -> "03"
        "april" -> "04"
        "may" -> "05"
        "june" -> "06"
        "july" -> "07"
        "august" -> "08"
        "september" -> "09"
        "october" -> "10"
        "november" -> "11"
        "december" -> "12"
        else -> return payPeriod // Return original if month not recognized
      }
      
      return "$year-$monthNumber"
    }
    
    // Return original if no match found
    return payPeriod
  }

  /**
   * Helper function to extract salary amounts
   */
  private fun extractSalary(text: String, keywords: List<String>): Double {
    keywords.forEach { keyword ->
      // Handle markdown formatting in salary extraction
      val regex = "(?i)(?:\\*+\\s*)?${keyword}\\s+salary\\s+amount[:\\s]*\\*+\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)".toRegex()
      val match = regex.find(text)
      if (match != null) {
        return match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
      }
    }
    
    // Fallback: try simpler pattern
    keywords.forEach { keyword ->
      val regex = "(?i)$keyword[:\\s-]*[Kk]?[Ss]?[Hh]?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)".toRegex()
      val match = regex.find(text)
      if (match != null) {
        return match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
      }
    }
    return 0.0
  }

  /**
   * Extract deductions from payslip text response
   */
  private fun extractDeductionsFromText(text: String): Map<String, Double> {
    val deductions = mutableMapOf<String, Double>()
    
    // Look for deductions section patterns
    val deductionPatterns = listOf(
      "deductions?[:\\s]*([\\s\\S]*?)(?=allowances?|net|total|$)",
      "deductions?[:\\s]*([\\s\\S]*?)(?=\\n\\n|allowances|net pay)",
      "(?:^|\\n)\\s*-\\s*([^:\\n]+):\\s*([\\d,]+(?:\\.\\d{2})?)"
    )
    
    deductionPatterns.forEach { pattern ->
      val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
      val match = regex.find(text)
      if (match != null) {
        val section = match.groupValues[1]
        
        // Extract individual deduction items
        val itemRegex = "([A-Za-z\\s/\\-]+?)[:]*\\s*(?:KSh\\s*)?([\\d,]+(?:\\.\\d{2})?)".toRegex()
        itemRegex.findAll(section).forEach { itemMatch ->
          val name = itemMatch.groupValues[1].trim()
          val amountStr = itemMatch.groupValues[2].replace(",", "")
          val amount = amountStr.toDoubleOrNull() ?: 0.0
          
          if (name.isNotBlank() && amount > 0) {
            deductions[name] = amount
          }
        }
      }
    }
    
    // Fallback: Look for common deduction keywords anywhere in text
    val commonDeductions = listOf(
      "co-operative loan", "cooperative loan", "sacco loan",
      "premier kenya loan", "kenya loan", "bank loan",
      "kuppet", "union", "mshwari", "support", "uwin",
      "personal loan", "advance", "salary advance"
    )
    
    commonDeductions.forEach { keyword ->
      val regex = "(?i)$keyword[:\\s]*(?:KSh\\s*)?([\\d,]+(?:\\.\\d{2})?)".toRegex()
      val match = regex.find(text)
      if (match != null) {
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount > 0) {
          deductions[keyword.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }] = amount
        }
      }
    }
    
    Log.d(TAG, "Extracted deductions: $deductions")
    return deductions
  }

  /**
   * Extract allowances from payslip text response
   */
  private fun extractAllowancesFromText(text: String): Map<String, Double> {
    val allowances = mutableMapOf<String, Double>()
    
    // Look for allowances section patterns
    val allowancePatterns = listOf(
      "allowances?[:\\s]*([\\s\\S]*?)(?=deductions?|net|total|$)",
      "allowances?[:\\s]*([\\s\\S]*?)(?=\\n\\n|deductions|net pay)",
      "(?:^|\\n)\\s*-\\s*([^:\\n]+):\\s*([\\d,]+(?:\\.\\d{2})?)"
    )
    
    allowancePatterns.forEach { pattern ->
      val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
      val match = regex.find(text)
      if (match != null) {
        val section = match.groupValues[1]
        
        // Extract individual allowance items
        val itemRegex = "([A-Za-z\\s/\\-]+?)[:]*\\s*(?:KSh\\s*)?([\\d,]+(?:\\.\\d{2})?)".toRegex()
        itemRegex.findAll(section).forEach { itemMatch ->
          val name = itemMatch.groupValues[1].trim()
          val amountStr = itemMatch.groupValues[2].replace(",", "")
          val amount = amountStr.toDoubleOrNull() ?: 0.0
          
          if (name.isNotBlank() && amount > 0) {
            allowances[name] = amount
          }
        }
      }
    }
    
    // Fallback: Look for common allowance keywords
    val commonAllowances = listOf(
      "housing allowance", "house allowance", "rental",
      "hardship allowance", "hardship", 
      "commuter allowance", "transport allowance", "commuter",
      "medical allowance", "medical"
    )
    
    commonAllowances.forEach { keyword ->
      val regex = "(?i)$keyword[:\\s]*(?:KSh\\s*)?([\\d,]+(?:\\.\\d{2})?)".toRegex()
      val match = regex.find(text)
      if (match != null) {
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount > 0) {
          allowances[keyword.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }] = amount
        }
      }
    }
    
    Log.d(TAG, "Extracted allowances: $allowances")
    return allowances
  }

  /**
   * Starts data extraction with a specific model (Ask Image approach)
   * This is the preferred method that follows the same pattern as Ask Image
   */
  fun startDataExtractionWithModel(model: Model) {
    viewModelScope.launch(Dispatchers.Default) {
      try {
        val currentData = _uiState.value.applicationData
        
        // Check if we have required images
        if (!currentData.isIdCaptureComplete()) {
          _uiState.value = _uiState.value.copy(
            extractionError = "ID images are required for processing"
          )
          return@launch
        }
        
        if (!currentData.hasMinimumPayslips()) {
          _uiState.value = _uiState.value.copy(
            extractionError = "At least one payslip is required for processing"
          )
          return@launch
        }

        // Reset extraction state
        _uiState.value = _uiState.value.copy(
          extractionProgress = 0f,
          extractionStatus = "Initializing AI model...",
          extractionError = null
        )

        // Wait for instance to be initialized (same as Ask Image)
        while (model.instance == null) {
          Log.d(TAG, "⏳ Waiting for model instance to be ready...")
          delay(100)
        }
        
        // Add warmup delay like Ask Image does (critical for vision models!)
        Log.d(TAG, "🔥 Model instance ready, adding warmup delay...")
        delay(500)

        Log.d(TAG, "✅ Using initialized model: ${model.name} (same as Ask Image approach)")

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.2f,
          extractionStatus = "Processing front ID image..."
        )

        // Extract ID data using Ask Image approach
        val frontIdResult = extractDataFromImage(
          model = model,
          bitmap = currentData.idImages.frontId!!,
          prompt = createSimpleIdExtractionPrompt()
        )
        
        Log.d(TAG, "🔍 Front ID extraction returned: '${frontIdResult.take(200)}${if(frontIdResult.length > 200) "..." else ""}'")
        Log.d(TAG, "🔍 Front ID result length: ${frontIdResult.length}")
        
        // Add delay to ensure response is fully processed before session reset
        delay(1000)
        
        // Reset session to clear state (critical for multiple consecutive calls!)
        Log.d(TAG, "🔄 Resetting session after front ID extraction...")
        LlmChatModelHelper.resetSession(model)

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.4f,
          extractionStatus = "Processing back ID image..."
        )

        val backIdResult = extractDataFromImage(
          model = model,
          bitmap = currentData.idImages.backId!!,
          prompt = createSimpleIdExtractionPrompt()
        )
        
        Log.d(TAG, "🔍 Back ID extraction returned: '${backIdResult.take(200)}${if(backIdResult.length > 200) "..." else ""}'")
        Log.d(TAG, "🔍 Back ID result length: ${backIdResult.length}")
        
        // Add delay to ensure response is fully processed before session reset
        delay(1000)
        
        // Reset session to clear state (critical for multiple consecutive calls!)
        Log.d(TAG, "🔄 Resetting session after back ID extraction...")
        LlmChatModelHelper.resetSession(model)

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.6f,
          extractionStatus = "Processing payslips..."
        )

        // Get all available payslips
        val payslipBitmaps = listOfNotNull(
          currentData.payslipImages.payslip1,
          currentData.payslipImages.payslip2,
          currentData.payslipImages.payslip3,
          currentData.payslipImages.payslip4
        )

        Log.d(TAG, "Processing ${payslipBitmaps.size} payslips...")
        
        val payslipResults = mutableListOf<String>()
        val allPayslipData = mutableListOf<PayslipData>()
        
        // Process each payslip individually
        payslipBitmaps.forEachIndexed { index, payslipBitmap ->
          _uiState.value = _uiState.value.copy(
            extractionProgress = 0.6f + (index * 0.2f / payslipBitmaps.size),
            extractionStatus = "Processing payslip ${index + 1} of ${payslipBitmaps.size}..."
          )
          
          val payslipResult = extractDataFromImage(
            model = model,
            bitmap = payslipBitmap,
            prompt = createSimplePayslipExtractionPrompt()
          )
          
          payslipResults.add(payslipResult)
          Log.d(TAG, "🔍 Payslip ${index + 1} extraction returned: '${payslipResult.take(200)}${if(payslipResult.length > 200) "..." else ""}'")
          
          // Parse individual payslip and add to list
          val payslipData = parsePayslipDataFromText(payslipResult)
          if (payslipData.isValid) {
            allPayslipData.add(payslipData)
            Log.d(TAG, "✅ Payslip ${index + 1} valid: ${payslipData.payPeriod}")
          } else {
            Log.w(TAG, "❌ Payslip ${index + 1} invalid or failed parsing")
          }
          
          // Add delay between payslip processing
          delay(1000)
          
          // Reset session after each payslip (important for model state)
          Log.d(TAG, "🔄 Resetting session after payslip ${index + 1} extraction...")
          LlmChatModelHelper.resetSession(model)
        }

        _uiState.value = _uiState.value.copy(
          extractionProgress = 0.9f,
          extractionStatus = "Finalizing extraction..."
        )

        Log.d(TAG, "=== EXTRACTION RESULTS (Ask Image Approach) ===")
        Log.d(TAG, "Front ID: $frontIdResult")
        Log.d(TAG, "Back ID: $backIdResult")
        payslipResults.forEachIndexed { index, result ->
          Log.d(TAG, "Payslip ${index + 1}: ${result.take(100)}...")
        }
        Log.d(TAG, "Total valid payslips processed: ${allPayslipData.size}")

        // Parse the results into structured data
        val idData = parseIdDataFromText(frontIdResult + " " + backIdResult)

        // Create extracted data structure with ALL payslips
        val extractedData = ExtractedApplicationData(
          idCardData = idData,
          payslipData = allPayslipData,
          extractionStartTime = System.currentTimeMillis() - 5000,
          extractionEndTime = System.currentTimeMillis()
        ).calculateOverallConfidence()

        _uiState.value = _uiState.value.copy(
          extractionProgress = 1.0f,
          extractionStatus = "Extraction completed successfully!",
          extractedData = extractedData
        )

        Log.d(TAG, "AI extraction completed. Overall confidence: ${extractedData.overallConfidence}")
        
        // Trigger business rules validation after successful extraction
        withContext(Dispatchers.Main) {
          try {
            Log.d(TAG, "About to call validateApplicationWithBusinessRules() ...")
            Log.d(TAG, "ruleValidationEngine is null: ${ruleValidationEngine == null}")
            Log.d(TAG, "loanOfferGenerator is null: ${loanOfferGenerator == null}")
            validateApplicationWithBusinessRules()
            Log.d(TAG, "validateApplicationWithBusinessRules() call completed")
          } catch (e: Exception) {
            Log.e(TAG, "Exception calling validateApplicationWithBusinessRules()", e)
          }
        }

      } catch (e: Exception) {
        Log.e(TAG, "Data extraction failed", e)
        _uiState.value = _uiState.value.copy(
          extractionError = "Data extraction failed: ${e.message}"
        )
      }
    }
  }

  /**
   * Retry data extraction (simplified - just restart the entire extraction)
   */
  fun retryExtraction(imageType: String) {
    Log.d(TAG, "Retrying extraction for: $imageType")
    clearExtractionError()
    startDataExtraction()
  }

  /**
   * Validate extracted data and check for issues
   */
  fun validateExtractedData() {
    val extractedData = _uiState.value.extractedData
    if (extractedData != null) {
      val issues = extractedData.getValidationIssues()
      if (issues.isNotEmpty()) {
        _uiState.value = _uiState.value.copy(
          extractionError = "Validation issues: ${issues.joinToString(", ")}"
        )
      }
    }
  }

  /**
   * Validates extracted application data using business rules
   */
  fun validateApplicationWithBusinessRules() {
    viewModelScope.launch {
      val extractedData = _uiState.value.extractedData
      if (extractedData == null) {
        _uiState.value = _uiState.value.copy(
          validationError = "No extracted data available for validation"
        )
        return@launch
      }
      
      try {
        _uiState.value = _uiState.value.copy(
          validationInProgress = true,
          validationError = null,
          validationStatus = "Validating business rules..."
        )
        
        Log.d(TAG, "Starting business rules validation")
        
        // Run validation
        val validationResult = ruleValidationEngine.validateApplication(
          extractedData = extractedData,
          requestedLoanAmount = 0.0 // Use default calculation
        )
        
        Log.d(TAG, "Validation complete. Eligible: ${validationResult.isEligible}")
        Log.d(TAG, ruleValidationEngine.getValidationSummary(validationResult))
        
        _uiState.value = _uiState.value.copy(
          validationResult = validationResult,
          validationInProgress = false,
          validationStatus = if (validationResult.isEligible) "Validation passed" else "Validation failed"
        )
        Log.d(TAG, "UI state updated with validationResult (eligible=${validationResult.isEligible})")
        
        // Generate loan offer if eligible
        if (validationResult.isEligible) {
          generateLoanOffer()
        }
        
      } catch (e: Exception) {
        Log.e(TAG, "Business rules validation failed", e)
        _uiState.value = _uiState.value.copy(
          validationInProgress = false,
          validationError = "Validation failed: ${e.message}"
        )
      }
    }
  }
  
  /**
   * Generates loan offer based on validation results
   */
  private suspend fun generateLoanOffer() {
    val extractedData = _uiState.value.extractedData
    val validationResult = _uiState.value.validationResult
    val generator = loanOfferGenerator
    
    if (extractedData == null || validationResult == null || generator == null) {
      Log.e(TAG, "Cannot generate loan offer - missing data or generator")
      return
    }
    
    try {
      Log.d(TAG, "Generating loan offer...")
      
      val loanOffer = generator.generateOffer(
        extractedData = extractedData,
        validationResult = validationResult
      )
      
      if (loanOffer != null) {
        _uiState.value = _uiState.value.copy(
          loanOffer = loanOffer,
          validationStatus = "Loan offer generated successfully"
        )
        
        Log.d(TAG, "Loan offer generated:")
        Log.d(TAG, "  Max Amount: ${loanOffer.maxLoanAmount}")
        Log.d(TAG, "  Recommended: ${loanOffer.recommendedAmount}")
        Log.d(TAG, "  Interest Rate: ${loanOffer.getFormattedInterestRate()}")
        Log.d(TAG, "  Monthly Payment: ${loanOffer.monthlyPayment}")
      } else {
        Log.w(TAG, "Loan offer generator returned null")
        _uiState.value = _uiState.value.copy(
          validationError = "Unable to generate loan offer"
        )
      }
      
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate loan offer", e)
      _uiState.value = _uiState.value.copy(
        validationError = "Loan offer generation failed: ${e.message}"
      )
    }
  }
  
  /**
   * Recalculates loan offer for a specific amount
   */
  fun recalculateLoanOfferForAmount(newAmount: Double) {
    viewModelScope.launch {
      val currentOffer = _uiState.value.loanOffer
      val extractedData = _uiState.value.extractedData
      val generator = loanOfferGenerator
      
      if (extractedData == null || generator == null) {
        Log.e(TAG, "Cannot recalculate offer - missing data")
        return@launch
      }
      
      try {
        val recalculatedOffer = generator.recalculateOfferForAmount(
          baseOffer = currentOffer,
          newAmount = newAmount,
          payslips = extractedData.payslipData
        )
        
        if (recalculatedOffer != null) {
          _uiState.value = _uiState.value.copy(loanOffer = recalculatedOffer)
          Log.d(TAG, "Loan offer recalculated for amount: $newAmount")
        } else {
          Log.w(TAG, "Failed to recalculate offer for amount: $newAmount")
        }
        
      } catch (e: Exception) {
        Log.e(TAG, "Error recalculating loan offer", e)
      }
    }
  }
  
  /**
   * Gets maximum affordable loan amount
   */
  fun getMaxAffordableLoanAmount(): Double {
    val extractedData = _uiState.value.extractedData
    return if (extractedData != null) {
      ruleValidationEngine.calculateMaxAffordableLoan(extractedData.payslipData)
    } else {
      0.0
    }
  }
  
  /**
   * Clears validation errors
   */
  fun clearValidationError() {
    _uiState.value = _uiState.value.copy(validationError = null)
  }

  /**
   * Reset application state and clear all stored images
   */
  fun resetApplication() {
    viewModelScope.launch {
      try {
        // Clear all stored images
        secureImageStorage.clearAllImages()
        
        // Reset UI state
        _uiState.value = SmartLoanUiState()
        
        Log.d(TAG, "Application reset successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Error resetting application", e)
        _uiState.value = _uiState.value.copy(
          errorMessage = "Error resetting application: ${e.message}"
        )
      }
    }
  }

  /**
   * Check if ID capture is complete
   */
  fun isIdCaptureComplete(): Boolean {
    return _uiState.value.applicationData.isIdCaptureComplete()
  }

  /**
   * Check if payslip capture is complete (all 4 payslips)
   */
  fun isPayslipCaptureComplete(): Boolean {
    return _uiState.value.applicationData.isPayslipCaptureComplete()
  }

  /**
   * Check if minimum payslips are captured (at least 1)
   */
  fun hasMinimumPayslips(): Boolean {
    return _uiState.value.applicationData.hasMinimumPayslips()
  }

  /**
   * Get storage usage information
   */
  fun getStorageInfo(): Pair<Long, Long> {
    return Pair(
      secureImageStorage.getTotalImageSizeBytes(),
      secureImageStorage.getAvailableStorageBytes()
    )
  }

  /**
   * Clear extraction error message
   */
  fun clearExtractionError() {
    _uiState.value = _uiState.value.copy(
      errorMessage = null,
      extractionError = null
    )
  }

  // ========================================
  // TASK 05: OFFER & ACCEPTANCE FLOW
  // ========================================

  /**
   * Show terms and conditions dialog
   */
  fun showTermsDialog() {
    _uiState.value = _uiState.value.copy(showTermsDialog = true)
  }

  /**
   * Hide terms and conditions dialog
   */
  fun hideTermsDialog() {
    _uiState.value = _uiState.value.copy(showTermsDialog = false)
  }

  /**
   * Show decline options dialog
   */
  fun showDeclineDialog() {
    _uiState.value = _uiState.value.copy(showDeclineDialog = true)
  }

  /**
   * Hide decline options dialog
   */
  fun hideDeclineDialog() {
    _uiState.value = _uiState.value.copy(showDeclineDialog = false)
  }

  /**
   * Toggle amount adjustment visibility
   */
  fun toggleAmountAdjustment() {
    _uiState.value = _uiState.value.copy(
      showAmountAdjustment = !_uiState.value.showAmountAdjustment
    )
  }

  /**
   * Handle loan decline with feedback
   */
  fun declineLoan(reason: String, feedback: String) {
    viewModelScope.launch {
      try {
        Log.d(TAG, "Loan declined - Reason: $reason, Feedback: $feedback")
        
        // Hide decline dialog
        _uiState.value = _uiState.value.copy(showDeclineDialog = false)
        
        // TODO: Send decline feedback to analytics/backend
        // For now, just log the feedback
        Log.i(TAG, "Decline feedback recorded: $reason - $feedback")
        
        // Navigate back or show decline confirmation
        resetApplication()
        
      } catch (e: Exception) {
        Log.e(TAG, "Error processing loan decline", e)
        _uiState.value = _uiState.value.copy(
          showDeclineDialog = false,
          acceptanceError = "Failed to process decline: ${e.message}"
        )
      }
    }
  }

  /**
   * Accept loan offer with biometric authentication
   */
  fun acceptLoanOffer() {
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(
          isAcceptingLoan = true,
          acceptanceError = null
        )

        // Check biometric availability
        if (!biometricAuthenticator.isBiometricAvailable()) {
          _uiState.value = _uiState.value.copy(
            isAcceptingLoan = false,
            acceptanceError = "Biometric authentication not available: ${biometricAuthenticator.getBiometricStatusMessage()}"
          )
          return@launch
        }

        // Perform biometric authentication
        suspendCancellableCoroutine<Boolean> { continuation ->
          biometricAuthenticator.authenticate(
            title = "Confirm Loan Acceptance",
            subtitle = "Use your fingerprint to confirm acceptance of the loan offer",
            onSuccess = {
              Log.d(TAG, "Biometric authentication successful")
              continuation.resume(true)
            },
            onError = { error ->
              Log.e(TAG, "Biometric authentication failed: $error")
              _uiState.value = _uiState.value.copy(
                isAcceptingLoan = false,
                acceptanceError = "Authentication failed: $error"
              )
              continuation.resume(false)
            },
            onCancel = {
              Log.d(TAG, "Biometric authentication cancelled")
              _uiState.value = _uiState.value.copy(
                isAcceptingLoan = false,
                acceptanceError = null
              )
              continuation.resume(false)
            }
          )
        }.let { authSuccess ->
          if (authSuccess) {
            // Generate disbursement memo
            generateDisbursementMemo()
          }
        }

      } catch (e: Exception) {
        Log.e(TAG, "Error during loan acceptance", e)
        _uiState.value = _uiState.value.copy(
          isAcceptingLoan = false,
          acceptanceError = "Failed to accept loan: ${e.message}"
        )
      }
    }
  }

  /**
   * Generate disbursement memo after successful acceptance
   */
  private suspend fun generateDisbursementMemo() {
    try {
      val currentState = _uiState.value
      val extractedData = currentState.extractedData
      val validationResult = currentState.validationResult
      val loanOffer = currentState.loanOffer

      if (extractedData == null || validationResult == null) {
        throw IllegalStateException("Missing required data for memo generation")
      }

      Log.d(TAG, "Generating disbursement memo...")

      val memo = disbursementMemoGenerator.generateMemo(
        applicationId = disbursementMemoGenerator.generateApplicationId(),
        extractedData = extractedData,
        validationResult = validationResult,
        loanOffer = loanOffer,
        biometricConfirmed = true,
        termsAccepted = true
      )

      // Save memo securely
      val savedPath = memoStorage.saveDisbursementMemo(memo)
      Log.d(TAG, "Disbursement memo saved: $savedPath")

      // Update UI state
      _uiState.value = _uiState.value.copy(
        isAcceptingLoan = false,
        isLoanAccepted = true,
        disbursementMemo = memo,
        acceptanceError = null
      )

      Log.i(TAG, "Loan acceptance completed successfully")

    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate disbursement memo", e)
      _uiState.value = _uiState.value.copy(
        isAcceptingLoan = false,
        acceptanceError = "Failed to generate loan documentation: ${e.message}"
      )
    }
  }

  /**
   * Export disbursement memo
   */
  fun exportDisbursementMemo() {
    viewModelScope.launch {
      try {
        val memo = _uiState.value.disbursementMemo
        if (memo == null) {
          Log.w(TAG, "No disbursement memo to export")
          return@launch
        }

        val success = memoStorage.exportMemoAsText(memo, disbursementMemoGenerator)
        if (success) {
          Log.i(TAG, "Disbursement memo exported successfully")
          // TODO: Show success toast/snackbar
        } else {
          Log.e(TAG, "Failed to export disbursement memo")
          // TODO: Show error toast/snackbar
        }

      } catch (e: Exception) {
        Log.e(TAG, "Error exporting disbursement memo", e)
      }
    }
  }

  /**
   * View disbursement memo (for future implementation)
   */
  fun viewDisbursementMemo() {
    val memo = _uiState.value.disbursementMemo
    if (memo != null) {
      val formattedMemo = disbursementMemoGenerator.formatMemoForDisplay(memo)
      Log.d(TAG, "Formatted memo for display: ${formattedMemo.length} characters")
      // TODO: Show memo in a dialog or navigate to memo viewer screen
    }
  }

  /**
   * Clear acceptance error
   */
  fun clearAcceptanceError() {
    _uiState.value = _uiState.value.copy(acceptanceError = null)
  }

  /**
   * Reset application for new loan application
   */
  fun startNewApplication() {
    viewModelScope.launch {
      // Clear all application data but keep memo history
      _uiState.value = SmartLoanUiState()
      
      // Clear stored images
      secureImageStorage.clearAllImages()
      
      Log.d(TAG, "Started new loan application")
    }
  }

  override fun onCleared() {
    super.onCleared()
    // Clean up any resources if needed
    Log.d(TAG, "ViewModel cleared and resources cleaned up")
  }
} 