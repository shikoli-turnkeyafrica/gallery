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

package com.google.ai.edge.gallery.ui.smartloan.extraction

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import com.google.ai.edge.gallery.ui.smartloan.data.LoanApplicationFormData
import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.framework.image.BitmapImageBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VisionExtractionEngine"

/**
 * AI-powered vision extraction engine for Smart Loan document processing.
 * Uses MediaPipe LLM Inference with Gemma 3n vision model and simulated function calling.
 * Based on AI Edge APIs healthcare demo patterns.
 */
@Singleton
class VisionExtractionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var llmInference: LlmInference? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Initializes the AI model for vision extraction with simulated function calling
     */
    suspend fun initialize(modelPath: String? = null): Boolean = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Initializing Vision Extraction Engine with simulated function calling...")
            
            // Use provided model path or fallback to hardcoded filename
            val finalModelPath = modelPath ?: "gemma-3n-E4B-it-int4.task"
            Log.d(TAG, "Using model path: $finalModelPath")
            
                         // Initialize MediaPipe LLM Inference with Gemma 3n vision model
             // Use CPU backend like Ask Image (which works perfectly)
             val options = LlmInference.LlmInferenceOptions.builder()
                 .setModelPath(finalModelPath)
                 .setMaxTokens(4096)
                 .setPreferredBackend(LlmInference.Backend.CPU)  // Changed from GPU to CPU
                 .setMaxNumImages(1) // Support for image input
                 .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            
            Log.d(TAG, "Vision Extraction Engine initialized successfully with model: $finalModelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vision Extraction Engine", e)
            when {
                e.message?.contains("model") == true || e.message?.contains("file") == true -> {
                    Log.e(TAG, "Model file not found - ensure Gemma-3n-E4B-it-int4 model is downloaded")
                }
                e.message?.contains("memory") == true -> {
                    Log.e(TAG, "Insufficient memory to load model")
                }
                else -> {
                    Log.e(TAG, "Unknown model initialization error: ${e.message}")
                }
            }
            false
        }
    }
    
    /**
     * Extracts structured data from front and back ID images
     */
    suspend fun extractIdData(
        frontIdBitmap: Bitmap,
        backIdBitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): IdCardData = withContext(Dispatchers.Default) {
        try {
            onProgress("Processing front ID image...")
            val frontData = extractIdFromSingleImage(frontIdBitmap, "front")
            
            onProgress("Processing back ID image...")
            val backData = extractIdFromSingleImage(backIdBitmap, "back")
            
            onProgress("Combining ID data...")
            
            // Combine data from both sides, prioritizing the side with higher confidence
            val combinedData = combineIdData(frontData, backData)
            
            Log.d(TAG, "ID extraction completed with confidence: ${combinedData.extractionConfidence}")
            combinedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract ID data", e)
            IdCardData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Extracts structured data from a single payslip image
     */
    suspend fun extractPayslipData(
        payslipBitmap: Bitmap,
        payslipIndex: Int = 0,
        onProgress: (String) -> Unit = {}
    ): PayslipData = withContext(Dispatchers.Default) {
        try {
            onProgress("Processing payslip ${payslipIndex + 1}...")
            
            // Use the optimized prompt from DataExtractionFunctions
            val prompt = DataExtractionFunctions.createPayslipExtractionPrompt()
            
            val response = processImageWithAI(payslipBitmap, prompt, "extractPayslipData")
            
            val extractedData = parsePayslipResponse(response)
            
            Log.d(TAG, "Payslip ${payslipIndex + 1} extraction completed with confidence: ${extractedData.extractionConfidence}")
            extractedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract payslip data", e)
            PayslipData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Extracts data from multiple payslip images
     */
    suspend fun extractMultiplePayslips(
        payslipBitmaps: List<Bitmap>,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): List<PayslipData> = withContext(Dispatchers.Default) {
        val results = mutableListOf<PayslipData>()
        
        payslipBitmaps.forEachIndexed { index, bitmap ->
            val progress = (index.toFloat() / payslipBitmaps.size)
            onProgress("Processing payslip ${index + 1} of ${payslipBitmaps.size}...", progress)
            
            val payslipData = extractPayslipData(bitmap, index) { status ->
                onProgress(status, progress)
            }
            results.add(payslipData)
        }
        
        results
    }
    
    /**
     * Extracts structured data from loan application form (front and back pages)
     */
    suspend fun extractLoanApplicationData(
        frontFormBitmap: Bitmap,
        backFormBitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): LoanApplicationFormData = withContext(Dispatchers.Default) {
        try {
            onProgress("Processing loan application form...")
            
            // Process front page (contains most application data)
            val frontData = extractLoanApplicationFromSingleImage(frontFormBitmap, "front")
            
            onProgress("Processing terms and conditions page...")
            
            // Process back page (contains terms, signatures, additional info)
            val backData = extractLoanApplicationFromSingleImage(backFormBitmap, "back")
            
            onProgress("Combining loan application data...")
            
            // Combine data from both pages, prioritizing front page data
            val combinedData = combineLoanApplicationData(frontData, backData)
            
            Log.d(TAG, "Loan application extraction completed with confidence: ${combinedData.extractionConfidence}")
            combinedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract loan application data", e)
            LoanApplicationFormData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Processes complete Smart Loan application (ID + payslips + loan application form)
     */
    suspend fun extractCompleteApplication(
        frontId: Bitmap,
        backId: Bitmap,
        payslips: List<Bitmap>,
        frontLoanApplication: Bitmap? = null,
        backLoanApplication: Bitmap? = null,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): ExtractedApplicationData = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            onProgress("Starting document processing...", 0.0f)
            
            // Extract ID data (30% of progress)
            onProgress("Processing ID documents...", 0.1f)
            val idData = extractIdData(frontId, backId) { status ->
                onProgress(status, 0.1f)
            }
            onProgress("ID processing complete", 0.3f)
            
            // Extract payslip data (60% of progress)
            val payslipData = extractMultiplePayslips(payslips) { status, payslipProgress ->
                val overallProgress = 0.3f + (payslipProgress * 0.6f)
                onProgress(status, overallProgress)
            }
            onProgress("Payslip processing complete", 0.8f)
            
            // Extract loan application form data if provided (10% of progress)
            val loanApplicationData = if (frontLoanApplication != null && backLoanApplication != null) {
                onProgress("Processing loan application form...", 0.85f)
                extractLoanApplicationData(frontLoanApplication, backLoanApplication) { status ->
                    onProgress(status, 0.85f)
                }
            } else {
                LoanApplicationFormData()
            }
            onProgress("Loan application processing complete", 0.9f)
            
            // Validate and combine data
            onProgress("Validating extracted data...", 0.95f)
            val extractedData = ExtractedApplicationData(
                idCardData = idData,
                payslipData = payslipData,
                loanApplicationFormData = loanApplicationData,
                extractionStartTime = startTime,
                extractionEndTime = System.currentTimeMillis()
            ).calculateOverallConfidence()
            
            onProgress("Document processing complete!", 1.0f)
            
            Log.d(TAG, "Complete application extraction finished. Overall confidence: ${extractedData.overallConfidence}")
            extractedData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract complete application", e)
            ExtractedApplicationData()
        }
    }
    
    /**
     * Extracts ID data from a single image (front or back)
     */
         private suspend fun extractIdFromSingleImage(bitmap: Bitmap, side: String): IdCardData {
         // Use the optimized prompt from DataExtractionFunctions
         val prompt = DataExtractionFunctions.createIdExtractionPrompt()
         
         val response = processImageWithAI(bitmap, prompt, "extractIdCardData")
         return parseIdResponse(response)
     }
    
    /**
     * Combines ID data from front and back images
     */
    private fun combineIdData(frontData: IdCardData, backData: IdCardData): IdCardData {
        // Use the data with higher confidence, but merge fields where possible
        val primaryData = if (frontData.extractionConfidence >= backData.extractionConfidence) {
            frontData
        } else {
            backData
        }
        
        val secondaryData = if (frontData.extractionConfidence >= backData.extractionConfidence) {
            backData
        } else {
            frontData
        }
        
        return primaryData.copy(
            // Fill in missing fields from secondary data
            fullName = if (primaryData.fullName.isNotBlank()) primaryData.fullName else secondaryData.fullName,
            idNumber = if (primaryData.idNumber.isNotBlank()) primaryData.idNumber else secondaryData.idNumber,
            dateOfBirth = if (primaryData.dateOfBirth.isNotBlank()) primaryData.dateOfBirth else secondaryData.dateOfBirth,
            expiryDate = if (primaryData.expiryDate.isNotBlank()) primaryData.expiryDate else secondaryData.expiryDate,
            placeOfBirth = if (primaryData.placeOfBirth.isNotBlank()) primaryData.placeOfBirth else secondaryData.placeOfBirth,
            // Use average confidence if both have data
            extractionConfidence = if (secondaryData.extractionConfidence > 0.1f) {
                (primaryData.extractionConfidence + secondaryData.extractionConfidence) / 2.0f
            } else {
                primaryData.extractionConfidence
            }
        ).validate()
    }
    
         /**
      * Processes an image with the AI model using the same approach as Ask Image
      */
     private suspend fun processImageWithAI(bitmap: Bitmap, prompt: String, functionName: String): String {
         val inference = llmInference ?: throw IllegalStateException("Vision engine not initialized")
         
         return suspendCancellableCoroutine { continuation ->
             try {
                 Log.d(TAG, "Processing image with AI model using Ask Image approach...")
                 
                 // Use the same simple approach as Ask Image - no complex session creation
                 val session = LlmInferenceSession.createFromOptions(
                     inference,
                     LlmInferenceSession.LlmInferenceSessionOptions.builder().build()
                 )
                 
                 // Add prompt first (exactly like Ask Image)
                 if (prompt.trim().isNotEmpty()) {
                     session.addQueryChunk(prompt)
                 }
                 
                 // Add image (exactly like Ask Image - no try/catch)
                 session.addImage(BitmapImageBuilder(bitmap).build())
                 Log.d(TAG, "Image added to session successfully")
                 
                 // Generate response with simple callback (like Ask Image)
                 val fullResponse = StringBuilder()
                 session.generateResponseAsync { partialResult, done ->
                     fullResponse.append(partialResult)
                     if (done) {
                         val completeResponse = fullResponse.toString()
                         Log.d(TAG, "=== AI MODEL RESPONSE DEBUG ===")
                         Log.d(TAG, "Function: $functionName")
                         Log.d(TAG, "Response length: ${completeResponse.length} characters")
                         Log.d(TAG, "Full AI Response: $completeResponse")
                         Log.d(TAG, "=== END AI RESPONSE ===")
                         
                         session.close()
                         continuation.resume(completeResponse)
                     }
                 }
                 
                 continuation.invokeOnCancellation {
                     session.close()
                 }
                 
             } catch (e: Exception) {
                 Log.e(TAG, "Error processing image with AI", e)
                 continuation.resume("")
             }
         }
     }
    
    /**
     * Parses AI response for ID card extraction with function calling
     */
    private fun parseIdResponse(response: String): IdCardData {
        return try {
            Log.d(TAG, "=== PARSING ID RESPONSE DEBUG ===")
            Log.d(TAG, "Raw response to parse: $response")
            
            // Parse function call response from AI Edge APIs
            val functionCall = extractStructuredJson(response)
            Log.d(TAG, "Extracted JSON structure: $functionCall")
            
            if (functionCall != null) {
                // Extract function call arguments
                val functionName = functionCall["name"]?.toString()?.replace("\"", "")
                val arguments = functionCall["arguments"] as? JsonObject
                
                Log.d(TAG, "Function name: $functionName")
                Log.d(TAG, "Arguments: $arguments")
                
                if (functionName == "extractIdCardData" && arguments != null) {
                    Log.d(TAG, "Calling parseIdCardFunction with arguments")
                    val result = DataExtractionFunctions.parseIdCardFunction(arguments)
                    Log.d(TAG, "parseIdCardFunction result: $result")
                    return result
                } else {
                    Log.w(TAG, "Function name mismatch or null arguments. Expected: extractIdCardData, Got: $functionName")
                }
            } else {
                Log.w(TAG, "No structured JSON found, trying fallback parsing")
            }
            
            // Fallback: try to parse as direct JSON structure
            Log.d(TAG, "Using fallback JSON parsing")
            val fallbackResult = parseIdDataFromJson(response)
            Log.d(TAG, "Fallback result: $fallbackResult")
            Log.d(TAG, "=== END ID RESPONSE PARSING ===")
            fallbackResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ID response", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            IdCardData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Parses AI response for payslip extraction with function calling
     */
    private fun parsePayslipResponse(response: String): PayslipData {
        return try {
            Log.d(TAG, "=== PARSING PAYSLIP RESPONSE DEBUG ===")
            Log.d(TAG, "Raw response to parse: $response")
            
            // Parse function call response from AI Edge APIs
            val functionCall = extractStructuredJson(response)
            Log.d(TAG, "Extracted JSON structure: $functionCall")
            
            if (functionCall != null) {
                // Extract function call arguments
                val functionName = functionCall["name"]?.toString()?.replace("\"", "")
                val arguments = functionCall["arguments"] as? JsonObject
                
                Log.d(TAG, "Function name: $functionName")
                Log.d(TAG, "Arguments: $arguments")
                
                if (functionName == "extractPayslipData" && arguments != null) {
                    Log.d(TAG, "Calling parsePayslipFunction with arguments")
                    val result = DataExtractionFunctions.parsePayslipFunction(arguments)
                    Log.d(TAG, "parsePayslipFunction result: $result")
                    return result
                } else {
                    Log.w(TAG, "Function name mismatch or null arguments. Expected: extractPayslipData, Got: $functionName")
                }
            } else {
                Log.w(TAG, "No structured JSON found, trying fallback parsing")
            }
            
            // Fallback: try to parse as direct JSON structure
            Log.d(TAG, "Using fallback JSON parsing")
            val fallbackResult = parsePayslipDataFromJson(response)
            Log.d(TAG, "Fallback result: $fallbackResult")
            Log.d(TAG, "=== END PAYSLIP RESPONSE PARSING ===")
            fallbackResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse payslip response", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            PayslipData(extractionConfidence = 0.0f)
        }
    }
    
      /**
   * Extracts structured JSON from AI response (fallback without function calling SDK)
   */
  private fun extractStructuredJson(response: String): JsonObject? {
    return try {
      // Look for JSON-like pattern in response
      val jsonStart = response.indexOf("{")
      val jsonEnd = response.lastIndexOf("}") + 1
      
      if (jsonStart != -1 && jsonEnd > jsonStart) {
        val jsonString = response.substring(jsonStart, jsonEnd)
        json.parseToJsonElement(jsonString) as JsonObject
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not extract JSON from response", e)
      null
    }
  }
    
    /**
     * Fallback parser for ID data from JSON response
     */
    private fun parseIdDataFromJson(response: String): IdCardData {
        return try {
            Log.d(TAG, "Attempting fallback JSON parsing for ID data")
            
            // Extract any JSON-like structure from the response
            val jsonObject = extractStructuredJson(response)
            if (jsonObject != null) {
                // Try to map common field names to our data structure
                val fullName = jsonObject["full_name"]?.toString()?.replace("\"", "")
                    ?: jsonObject["name"]?.toString()?.replace("\"", "")
                    ?: jsonObject["fullName"]?.toString()?.replace("\"", "") ?: ""
                
                val idNumber = jsonObject["id_number"]?.toString()?.replace("\"", "")
                    ?: jsonObject["idNumber"]?.toString()?.replace("\"", "")
                    ?: jsonObject["identification_number"]?.toString()?.replace("\"", "") ?: ""
                
                val dateOfBirth = jsonObject["date_of_birth"]?.toString()?.replace("\"", "")
                    ?: jsonObject["dateOfBirth"]?.toString()?.replace("\"", "")
                    ?: jsonObject["birth_date"]?.toString()?.replace("\"", "") ?: ""
                
                val expiryDate = jsonObject["expiry_date"]?.toString()?.replace("\"", "")
                    ?: jsonObject["expiryDate"]?.toString()?.replace("\"", "")
                    ?: jsonObject["expiration_date"]?.toString()?.replace("\"", "") ?: ""
                
                val placeOfBirth = jsonObject["place_of_birth"]?.toString()?.replace("\"", "")
                    ?: jsonObject["placeOfBirth"]?.toString()?.replace("\"", "")
                    ?: jsonObject["birth_place"]?.toString()?.replace("\"", "") ?: ""
                
                val confidence = try {
                    jsonObject["confidence"]?.toString()?.replace("\"", "")?.toFloatOrNull() ?: 0.3f
                } catch (e: Exception) { 0.3f }
                
                Log.d(TAG, "Parsed ID data from JSON: name=$fullName, id=$idNumber")
                
                IdCardData(
                    fullName = fullName,
                    idNumber = idNumber,
                    dateOfBirth = dateOfBirth,
                    expiryDate = expiryDate,
                    placeOfBirth = placeOfBirth,
                    extractionConfidence = confidence
                ).validate()
            } else {
                Log.w(TAG, "No JSON structure found in response")
                IdCardData(extractionConfidence = 0.1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback ID JSON parsing", e)
            IdCardData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Fallback parser for payslip data from JSON response
     */
    private fun parsePayslipDataFromJson(response: String): PayslipData {
        return try {
            Log.d(TAG, "Attempting fallback JSON parsing for payslip data")
            
            // Extract any JSON-like structure from the response
            val jsonObject = extractStructuredJson(response)
            if (jsonObject != null) {
                // Try to map common field names to our data structure
                val employeeName = jsonObject["employee_name"]?.toString()?.replace("\"", "")
                    ?: jsonObject["employeeName"]?.toString()?.replace("\"", "")
                    ?: jsonObject["name"]?.toString()?.replace("\"", "") ?: ""
                
                val employerName = jsonObject["employer_name"]?.toString()?.replace("\"", "")
                    ?: jsonObject["employerName"]?.toString()?.replace("\"", "")
                    ?: jsonObject["company"]?.toString()?.replace("\"", "") ?: ""
                
                val payPeriod = jsonObject["pay_period"]?.toString()?.replace("\"", "")
                    ?: jsonObject["payPeriod"]?.toString()?.replace("\"", "")
                    ?: jsonObject["period"]?.toString()?.replace("\"", "") ?: ""
                
                // Parse gross salary
                val grossSalary = try {
                    jsonObject["gross_salary"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: jsonObject["grossSalary"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: jsonObject["gross_pay"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: 0.0
                } catch (e: Exception) { 0.0 }
                
                // Parse net salary
                val netSalary = try {
                    jsonObject["net_salary"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: jsonObject["netSalary"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: jsonObject["net_pay"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: 0.0
                } catch (e: Exception) { 0.0 }
                
                val confidence = try {
                    jsonObject["confidence"]?.toString()?.replace("\"", "")?.toFloatOrNull() ?: 0.3f
                } catch (e: Exception) { 0.3f }
                
                Log.d(TAG, "Parsed payslip data from JSON: employee=$employeeName, employer=$employerName, gross=$grossSalary")
                
                PayslipData(
                    employeeName = employeeName,
                    employerName = employerName,
                    payPeriod = payPeriod,
                    grossSalary = grossSalary,
                    netSalary = netSalary,
                    deductions = emptyMap(), // Could be enhanced to parse deductions
                    extractionConfidence = confidence
                ).validate()
            } else {
                Log.w(TAG, "No JSON structure found in response")
                PayslipData(extractionConfidence = 0.1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback payslip JSON parsing", e)
            PayslipData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Extracts loan application data from a single image (front or back page)
     */
    private suspend fun extractLoanApplicationFromSingleImage(bitmap: Bitmap, side: String): LoanApplicationFormData {
        // Use the optimized prompt from DataExtractionFunctions
        val prompt = DataExtractionFunctions.createLoanApplicationExtractionPrompt()
        
        val response = processImageWithAI(bitmap, prompt, "extractLoanApplicationData")
        return parseLoanApplicationResponse(response)
    }
    
    /**
     * Combines loan application data from front and back pages
     */
    private fun combineLoanApplicationData(frontData: LoanApplicationFormData, backData: LoanApplicationFormData): LoanApplicationFormData {
        // Prioritize front page data (contains most application information)
        val primaryData = if (frontData.extractionConfidence >= backData.extractionConfidence) {
            frontData
        } else {
            backData
        }
        
        val secondaryData = if (frontData.extractionConfidence >= backData.extractionConfidence) {
            backData
        } else {
            frontData
        }
        
        return primaryData.copy(
            // Fill in missing fields from secondary data if primary is empty
            title = if (primaryData.title.isNotBlank()) primaryData.title else secondaryData.title,
            firstName = if (primaryData.firstName.isNotBlank()) primaryData.firstName else secondaryData.firstName,
            middleName = if (primaryData.middleName.isNotBlank()) primaryData.middleName else secondaryData.middleName,
            lastName = if (primaryData.lastName.isNotBlank()) primaryData.lastName else secondaryData.lastName,
            idPassportNumber = if (primaryData.idPassportNumber.isNotBlank()) primaryData.idPassportNumber else secondaryData.idPassportNumber,
            dateOfBirth = if (primaryData.dateOfBirth.isNotBlank()) primaryData.dateOfBirth else secondaryData.dateOfBirth,
            telephoneMobile = if (primaryData.telephoneMobile.isNotBlank()) primaryData.telephoneMobile else secondaryData.telephoneMobile,
            emailAddress = if (primaryData.emailAddress.isNotBlank()) primaryData.emailAddress else secondaryData.emailAddress,
            requestedLoanAmount = if (primaryData.requestedLoanAmount > 0.0) primaryData.requestedLoanAmount else secondaryData.requestedLoanAmount,
            requestedInstallmentAmount = if (primaryData.requestedInstallmentAmount > 0.0) primaryData.requestedInstallmentAmount else secondaryData.requestedInstallmentAmount,
            requestedLoanPeriodMonths = if (primaryData.requestedLoanPeriodMonths > 0) primaryData.requestedLoanPeriodMonths else secondaryData.requestedLoanPeriodMonths,
            disbursementMode = if (primaryData.disbursementMode.isNotBlank()) primaryData.disbursementMode else secondaryData.disbursementMode,
            clientMPesaNumber = if (primaryData.clientMPesaNumber.isNotBlank()) primaryData.clientMPesaNumber else secondaryData.clientMPesaNumber,
            applicantSignatureDate = if (primaryData.applicantSignatureDate.isNotBlank()) primaryData.applicantSignatureDate else secondaryData.applicantSignatureDate,
            // Use average confidence if both have data
            extractionConfidence = if (secondaryData.extractionConfidence > 0.1f) {
                (primaryData.extractionConfidence + secondaryData.extractionConfidence) / 2.0f
            } else {
                primaryData.extractionConfidence
            }
        ).validate()
    }
    
    /**
     * Parses AI response for loan application form extraction
     */
    private fun parseLoanApplicationResponse(response: String): LoanApplicationFormData {
        return try {
            Log.d(TAG, "=== PARSING LOAN APPLICATION RESPONSE DEBUG ===")
            Log.d(TAG, "Raw response to parse: $response")
            
            // Parse function call response
            val functionCall = extractStructuredJson(response)
            Log.d(TAG, "Extracted JSON structure: $functionCall")
            
            if (functionCall != null) {
                val functionName = functionCall["name"]?.toString()?.replace("\"", "")
                val arguments = functionCall["arguments"] as? JsonObject
                
                Log.d(TAG, "Function name: $functionName")
                Log.d(TAG, "Arguments: $arguments")
                
                if (functionName == "extractLoanApplicationData" && arguments != null) {
                    Log.d(TAG, "Calling parseLoanApplicationFunction with arguments")
                    val result = DataExtractionFunctions.parseLoanApplicationFunction(arguments)
                    Log.d(TAG, "parseLoanApplicationFunction result: $result")
                    return result
                } else {
                    Log.w(TAG, "Function name mismatch or null arguments. Expected: extractLoanApplicationData, Got: $functionName")
                }
            } else {
                Log.w(TAG, "No structured JSON found, trying fallback parsing")
            }
            
            // Fallback: try to parse as direct JSON structure
            Log.d(TAG, "Using fallback JSON parsing")
            val fallbackResult = parseLoanApplicationDataFromJson(response)
            Log.d(TAG, "Fallback result: $fallbackResult")
            Log.d(TAG, "=== END LOAN APPLICATION RESPONSE PARSING ===")
            fallbackResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse loan application response", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            LoanApplicationFormData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Fallback parser for loan application data from JSON response
     */
    private fun parseLoanApplicationDataFromJson(response: String): LoanApplicationFormData {
        return try {
            Log.d(TAG, "Attempting fallback JSON parsing for loan application data")
            
            val jsonObject = extractStructuredJson(response)
            if (jsonObject != null) {
                val title = jsonObject["title"]?.toString()?.replace("\"", "") ?: ""
                val firstName = jsonObject["firstName"]?.toString()?.replace("\"", "")
                    ?: jsonObject["first_name"]?.toString()?.replace("\"", "") ?: ""
                val middleName = jsonObject["middleName"]?.toString()?.replace("\"", "")
                    ?: jsonObject["middle_name"]?.toString()?.replace("\"", "") ?: ""
                val lastName = jsonObject["lastName"]?.toString()?.replace("\"", "")
                    ?: jsonObject["last_name"]?.toString()?.replace("\"", "") ?: ""
                val idPassportNumber = jsonObject["idPassportNumber"]?.toString()?.replace("\"", "")
                    ?: jsonObject["id_passport_number"]?.toString()?.replace("\"", "") ?: ""
                val telephoneMobile = jsonObject["telephoneMobile"]?.toString()?.replace("\"", "")
                    ?: jsonObject["telephone_mobile"]?.toString()?.replace("\"", "") ?: ""
                val requestedLoanAmount = try {
                    jsonObject["requestedLoanAmount"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: jsonObject["requested_loan_amount"]?.toString()?.replace("\"", "")?.replace(",", "")?.toDoubleOrNull()
                        ?: 0.0
                } catch (e: Exception) { 0.0 }
                
                val confidence = try {
                    jsonObject["confidence"]?.toString()?.replace("\"", "")?.toFloatOrNull() ?: 0.3f
                } catch (e: Exception) { 0.3f }
                
                Log.d(TAG, "Parsed loan application data from JSON: name=$firstName $lastName, loan=$requestedLoanAmount")
                
                LoanApplicationFormData(
                    title = title,
                    firstName = firstName,
                    middleName = middleName,
                    lastName = lastName,
                    idPassportNumber = idPassportNumber,
                    telephoneMobile = telephoneMobile,
                    requestedLoanAmount = requestedLoanAmount,
                    extractionConfidence = confidence
                ).validate()
            } else {
                Log.w(TAG, "No JSON structure found in response")
                LoanApplicationFormData(extractionConfidence = 0.1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fallback loan application JSON parsing", e)
            LoanApplicationFormData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Releases resources when done
     */
    fun cleanup() {
        try {
            llmInference?.close()
            llmInference = null
            Log.d(TAG, "Vision Extraction Engine cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Checks if the engine is initialized
     */
    fun isInitialized(): Boolean {
        return llmInference != null
    }
    
    /**
     * Gets memory usage information
     */
    fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val total = runtime.totalMemory()
        return "Memory: ${used / 1024 / 1024}MB / ${total / 1024 / 1024}MB"
    }
} 