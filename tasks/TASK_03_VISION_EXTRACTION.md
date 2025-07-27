# TASK 03: AI Vision → JSON Data Extraction (Week 3)

## 🎯 **Objective**
Implement AI-powered data extraction from ID and payslip images using Gemma 3n vision model and Function Calling SDK to convert images into structured JSON data.

## ✅ **Success Criteria**
- ID images extract: name, ID number, date of birth, expiry date
- Payslip images extract: name, salary, employer, month/year, deductions
- All data structured in consistent JSON format
- Extraction accuracy >85% for clear images
- Processing time <10 seconds per image on S24 Ultra
- Proper error handling for unclear/invalid images

## 📋 **Detailed Steps**

### Step 1: Add AI Edge Function Calling SDK
- [ ] **File**: `Android/src/app/build.gradle.kts`
- [ ] **Dependencies**:
  ```kotlin
  implementation "com.google.ai.edge:localagents-functioncalling:0.1.0-alpha03"
  ```
- [ ] **Verify**: Existing MediaPipe Tasks GenAI dependency is compatible

### Step 2: Download Required AI Models
- [ ] **Update**: `model_allowlist.json` in assets
- [ ] **Add Models**:
  ```json
  {
    "id": "Gemma3n-E4B-Vision-q8",
    "modelId": "google/gemma-3n-E4B-it-litert-preview", 
    "modelFile": "gemma-3n-E4B-it-int4.task",
    "sizeInBytes": 4405655031,
    "estimatedPeakMemoryInBytes": 6979321856,
    "llmSupportImage": true,
    "taskTypes": ["llm_ask_image", "vision_extraction"]
  }
  ```
- [ ] **Test**: Verify model downloads and loads correctly

### Step 3: Create Data Extraction Models
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/data/ExtractedData.kt`
- [ ] **Data Classes**:
  ```kotlin
  data class IdCardData(
    val fullName: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "", // YYYY-MM-DD format
    val expiryDate: String = "", // YYYY-MM-DD format
    val placeOfBirth: String = "",
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false
  )
  
  data class PayslipData(
    val employeeName: String = "",
    val employerName: String = "",
    val grossSalary: Double = 0.0,
    val netSalary: Double = 0.0,
    val payPeriod: String = "", // "2024-01" format
    val deductions: Map<String, Double> = emptyMap(),
    val allowances: Map<String, Double> = emptyMap(),
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false
  )
  ```

### Step 4: Create Function Calling Interface
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/extraction/DataExtractionFunctions.kt`
- [ ] **Function Definitions**:
  ```kotlin
  @FunctionDeclaration
  fun extractIdCardData(
    fullName: String,
    idNumber: String, 
    dateOfBirth: String,
    expiryDate: String,
    placeOfBirth: String,
    confidence: Float
  ): IdCardData
  
  @FunctionDeclaration  
  fun extractPayslipData(
    employeeName: String,
    employerName: String,
    grossSalary: Double,
    netSalary: Double,
    payPeriod: String,
    deductions: Map<String, Double>,
    allowances: Map<String, Double>,
    confidence: Float
  ): PayslipData
  ```

### Step 5: Implement Vision Extraction Engine
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/extraction/VisionExtractionEngine.kt`
- [ ] **Core Functions**:
  ```kotlin
  class VisionExtractionEngine(private val context: Context) {
    
    suspend fun extractIdData(
      frontIdBitmap: Bitmap,
      backIdBitmap: Bitmap
    ): IdCardData
    
    suspend fun extractPayslipData(
      payslipBitmap: Bitmap
    ): PayslipData
    
    private suspend fun processImageWithAI(
      bitmap: Bitmap,
      prompt: String,
      functions: List<FunctionDeclaration>
    ): String
  }
  ```

### Step 6: Create AI Prompts for Data Extraction
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/extraction/ExtractionPrompts.kt`
- [ ] **ID Card Prompt**:
  ```kotlin
  val ID_EXTRACTION_PROMPT = """
  Analyze this Kenyan National ID card image and extract the following information.
  
  Look for:
  - Full name (as printed on ID)
  - ID number (8 digits)
  - Date of birth (DD/MM/YYYY format)
  - Expiry date if visible
  - Place of birth if visible
  
  Call the extractIdCardData function with the extracted information.
  If any field is unclear or not visible, use empty string.
  Set confidence between 0.0-1.0 based on image clarity.
  """.trimIndent()
  ```
- [ ] **Payslip Prompt**:
  ```kotlin
  val PAYSLIP_EXTRACTION_PROMPT = """
  Analyze this payslip image and extract salary information.
  
  Look for:
  - Employee name
  - Employer/company name  
  - Gross salary amount
  - Net salary amount
  - Pay period (month/year)
  - Deductions (PAYE, NSSF, NHIF, etc.)
  - Allowances (housing, transport, etc.)
  
  Call the extractPayslipData function with the extracted information.
  Convert all amounts to numbers (remove commas, currency symbols).
  Set confidence based on text clarity and completeness.
  """.trimIndent()
  ```

### Step 7: Update ValidationProgressScreen
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/ValidationProgressScreen.kt`
- [ ] **Progress Tracking**:
  - Show "Processing ID card..." (30%)
  - Show "Processing payslip 1/4..." (40-80%)
  - Show "Validating extracted data..." (90%)
  - Show "Generating loan assessment..." (100%)
- [ ] **Error Handling**: Display retry options for failed extractions

### Step 8: Integrate with SmartLoanViewModel
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
- [ ] **Add State**:
  ```kotlin
  data class SmartLoanUiState(
    // ... existing fields
    val extractedIdData: IdCardData? = null,
    val extractedPayslips: List<PayslipData> = emptyList(),
    val extractionProgress: Float = 0.0f,
    val extractionStatus: String = "",
    val extractionError: String? = null
  )
  ```
- [ ] **Add Functions**:
  - `startDataExtraction()`
  - `retryExtraction(imageType: String)`
  - `validateExtractedData()`

### Step 9: Add Data Validation & Correction UI
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/DataReviewScreen.kt`
- [ ] **Features**:
  - Display extracted data in editable form
  - Highlight low-confidence extractions
  - Allow manual correction of errors
  - Show confidence scores per field
  - "Confirm & Continue" button

## 🔧 **Technical Implementation Details**

### Function Calling Setup
```kotlin
val functionCallingAgent = FunctionCallingAgent.Builder()
    .setContext(context)
    .setModelPath("gemma-3n-E4B-it-int4.task")
    .setFunctions(listOf(::extractIdCardData, ::extractPayslipData))
    .build()

val result = functionCallingAgent.generateResponse(
    prompt = ID_EXTRACTION_PROMPT,
    image = frontIdBitmap
)
```

### Memory Management for Vision Processing
```kotlin
// Process images sequentially to manage memory
private suspend fun processImagesSequentially(images: List<Bitmap>) {
    images.forEach { bitmap ->
        val result = processImageWithAI(bitmap, prompt, functions)
        // Process result immediately
        // Clear bitmap from memory if no longer needed
    }
}
```

## 🧪 **Testing Strategy**

### Unit Tests
- [ ] **File**: `Android/src/app/src/test/java/com/google/ai/edge/gallery/smartloan/VisionExtractionEngineTest.kt`
- [ ] **Test Cases**:
  - Valid ID card extraction
  - Payslip data extraction
  - Error handling for unclear images
  - Function calling JSON parsing

### Integration Tests  
- [ ] **Sample Images**: Create test dataset in `assets/samples/`
  - Clear Kenyan ID cards (front/back)
  - Various payslip formats
  - Blurry/unclear images for error testing
- [ ] **Expected Results**: JSON files with expected extraction results

### Performance Tests
- [ ] **Memory Usage**: Monitor during batch processing
- [ ] **Processing Time**: Ensure <10s per image on S24 Ultra
- [ ] **Accuracy**: Measure extraction accuracy on test dataset

## 🎯 **Accuracy Targets**

| Data Field | Target Accuracy | Measurement Method |
|------------|----------------|-------------------|
| ID Name | >95% | Exact string match |
| ID Number | >99% | Exact number match |
| Gross Salary | >90% | ±5% tolerance |
| Net Salary | >90% | ±5% tolerance |
| Employer Name | >85% | Fuzzy string match |
| Pay Period | >95% | Date format validation |

## ⚠️ **Potential Challenges**
- **Poor image quality**: Implement retry mechanisms
- **Various ID/payslip formats**: Test with diverse samples
- **OCR errors**: Add validation rules for common mistakes
- **Memory consumption**: Optimize bitmap handling
- **Processing time**: Balance accuracy vs speed

## 🎯 **Definition of Done**
- ✅ AI models successfully extract data from clear images
- ✅ Function calling returns properly structured JSON
- ✅ Extraction accuracy meets targets (>85%)
- ✅ Progress indication during processing
- ✅ Error handling for failed extractions
- ✅ Manual correction UI for low-confidence extractions
- ✅ Memory efficient processing
- ✅ Comprehensive test coverage

## 📅 **Estimated Time**
**5-6 days** for experienced Android + AI developer
**7-8 days** for developer new to vision AI and function calling

## 🔗 **Next Task**
After completion: Move to `TASK_04_RULE_VALIDATION.md` 