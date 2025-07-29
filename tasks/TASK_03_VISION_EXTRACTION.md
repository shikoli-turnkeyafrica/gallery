# TASK 03: AI Vision → JSON Data Extraction (Week 3)

## 📊 **CURRENT STATUS** 
**🟡 85% Complete - Working with Issues**

✅ **WORKING:**
- AI model loading and initialization
- Image capture and secure storage  
- Progress UI with smooth animations
- End-to-end extraction pipeline
- Data models and structure
- Auto-navigation flow

⚠️ **ISSUES:**
- AI extraction returning 0.0 confidence (see logs: `Overall confidence: 0.0`)
- May need prompt optimization or JSON parsing fixes
- Function calling simulation may need debugging

🔄 **NEXT STEPS:**
- Debug AI response parsing
- Optimize extraction prompts
- Test with higher quality images

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
- [x] **MODIFIED**: Used MediaPipe GenAI with simulated function calling approach
- [x] **File**: `Android/src/app/build.gradle.kts` - Already has MediaPipe dependencies
- [x] **Reason**: Official Function Calling SDK had dependency resolution issues
- [x] **Solution**: Implemented structured JSON prompts with MediaPipe LLM Inference API

### Step 2: Download Required AI Models
- [x] **File**: `model_allowlist.json` updated in assets
- [x] **Model Added**: Gemma-3n-E4B-it-int4 with taskTypes `["smart_loan"]`
- [x] **Verified**: Model downloads and loads correctly
- [x] **Status**: Model path resolution and initialization working ✅

### Step 3: Create Data Extraction Models
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/data/ExtractedData.kt` ✅
- [x] **Data Classes Created**:
  ```kotlin
  data class IdCardData(
    val fullName: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "",
    val expiryDate: String = "",
    val placeOfBirth: String = "",
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false
  )
  
  data class PayslipData(
    val employeeName: String = "",
    val employerName: String = "",
    val grossSalary: Double = 0.0,
    val netSalary: Double = 0.0,
    val payPeriod: String = "",
    val deductions: Map<String, Double> = emptyMap(),
    val allowances: Map<String, Double> = emptyMap(),
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false
  )
  
  data class ExtractedApplicationData(...)
  ```

### Step 4: Create Function Calling Interface  
- [x] **MODIFIED**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/extraction/DataExtractionFunctions.kt` ✅
- [x] **Approach**: Simulated function calling with structured JSON parsing
- [x] **Functions Created**:
  - `EXTRACT_ID_CARD_FUNCTION` - JSON schema definition
  - `EXTRACT_PAYSLIP_FUNCTION` - JSON schema definition  
  - `parseIdCardFunction()` - Parse AI JSON response
  - `parsePayslipFunction()` - Parse AI JSON response
  - `createIdExtractionPrompt()` - Generate structured prompts
  - `createPayslipExtractionPrompt()` - Generate structured prompts

### Step 5: Implement Vision Extraction Engine
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/extraction/VisionExtractionEngine.kt` ✅
- [x] **Core Functions Implemented**:
  ```kotlin
  class VisionExtractionEngine @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun initialize(modelPath: String? = null): Boolean
    suspend fun extractIdData(frontId: Bitmap, backId: Bitmap, onProgress: (String) -> Unit): IdCardData
    suspend fun extractPayslipData(payslipBitmap: Bitmap, payslipIndex: Int, onProgress: (String) -> Unit): PayslipData
    suspend fun extractCompleteApplication(frontId: Bitmap, backId: Bitmap, payslips: List<Bitmap>, onProgress: (String, Float) -> Unit): ExtractedApplicationData
    private suspend fun processImageWithAI(bitmap: Bitmap, prompt: String, functionName: String): String
  }
  ```

### Step 6: Create AI Prompts for Data Extraction
- [x] **Integrated**: Prompts are embedded in `DataExtractionFunctions.kt` ✅
- [x] **ID Card Prompt**: Comprehensive Kenyan ID analysis with structured JSON output
- [x] **Payslip Prompt**: Detailed salary extraction with function calling format
- [x] **Format**: Prompts instruct AI to return specific JSON structure for parsing

### Step 7: Update ValidationProgressScreen
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/ValidationProgressScreen.kt` ✅
- [x] **Progress Tracking Implemented**:
  - "Initializing AI Vision Model" (10%)
  - "Processing ID Documents" (30%)
  - "Extracting Payslip Data" (90%)
  - "Validating Extracted Data" (95%)
  - "Data extraction complete!" (100%)
- [x] **Features Added**:
  - Smooth progress animations
  - Error handling and display
  - Post-completion feedback
  - Auto-navigation after completion

### Step 8: Integrate with SmartLoanViewModel
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt` ✅
- [x] **State Added**:
  ```kotlin
  data class SmartLoanUiState(
    val extractedData: ExtractedApplicationData? = null,
    val extractionProgress: Float = 0.0f,
    val extractionStatus: String = "",
    val extractionError: String? = null,
    // ... other fields
  )
  ```
- [x] **Functions Implemented**:
  - `startDataExtraction()` ✅
  - `clearExtractionError()` ✅
  - `resetApplication()` ✅
  - Model initialization and path resolution ✅

### Step 9: Add Data Validation & Correction UI
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/DataReviewScreen.kt` ✅
- [x] **Features Implemented**:
  - Display extracted data in editable form
  - Highlight low-confidence extractions
  - Show confidence scores per field
  - "Retry Extraction" and "Continue" buttons
  - Manual correction capability

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

## ⚠️ **Current Issues & Troubleshooting**

### 🔍 **Issue: AI Extraction Returning 0.0 Confidence**
**Symptoms:** 
- Logs show: `AI extraction completed. Overall confidence: 0.0`
- All extracted fields likely empty or invalid

**Possible Causes:**
1. **AI Response Parsing**: The JSON response from AI might not match expected format
2. **Prompt Issues**: AI not following the structured JSON format instructions  
3. **Image Quality**: Images might be too blurry/overexposed (logs show image quality warnings)
4. **Model Context**: Vision model might need different prompting approach

**Debug Steps:**
1. **Check AI Raw Response**: Log the actual response from `processImageWithAI()`
2. **Validate JSON Structure**: Ensure `extractStructuredJson()` is parsing correctly
3. **Test with Clear Images**: Use high-quality sample images
4. **Prompt Optimization**: Simplify prompts to ensure AI compliance

### 🛠️ **Quick Debug Commands**
```bash
# Monitor real-time logs
adb logcat | findstr "SmartLoan\|VisionExtraction"

# Check actual AI responses
adb logcat | findstr "AI model response"
```

## ⚠️ **Potential Challenges**
- **Poor image quality**: Implement retry mechanisms ✅ (Basic quality warnings added)
- **Various ID/payslip formats**: Test with diverse samples
- **OCR errors**: Add validation rules for common mistakes  
- **Memory consumption**: Optimize bitmap handling ✅ (Sequential processing implemented)
- **Processing time**: Balance accuracy vs speed ✅ (< 10s achieved)

## 🎯 **Definition of Done**
- [x] ✅ AI models successfully load and initialize
- [x] ✅ Progress indication during processing  
- [x] ✅ Error handling for failed extractions
- [x] ✅ Manual correction UI for low-confidence extractions
- [x] ✅ Memory efficient processing
- [x] ✅ End-to-end integration working
- [ ] ⚠️ **ISSUE**: Extraction returning 0.0 confidence - prompts need optimization
- [ ] ⚠️ **ISSUE**: JSON parsing may need debugging for actual AI responses
- [ ] 🔄 **IN PROGRESS**: Function calling simulation needs refinement

## 📅 **Estimated Time**
**5-6 days** for experienced Android + AI developer
**7-8 days** for developer new to vision AI and function calling

## 🔗 **Next Task**
After completion: Move to `TASK_04_RULE_VALIDATION.md` 