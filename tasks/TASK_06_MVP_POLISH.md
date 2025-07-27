# TASK 06: MVP Polish & Demo Build (Week 6)

## 🎯 **Objective**
Finalize the Smart Loan MVP for board-room demo, remove internet permissions, optimize performance, and create production-ready APK with demo documentation.

## ✅ **Success Criteria**
- Complete offline functionality (no internet permission)
- Full flow completes in <7 seconds on S24 Ultra  
- Memory usage optimized for 6GB+ devices
- Production APK signed and ready for distribution
- Comprehensive demo guide with test scenarios
- Error handling covers all edge cases
- Professional UI polish matches banking standards

## 📋 **Detailed Steps**

### Step 1: Remove Internet Dependencies
- [ ] **File**: `Android/src/app/src/main/AndroidManifest.xml`
- [ ] **Remove Permissions**:
  ```xml
  <!-- Remove or comment out -->
  <!-- <uses-permission android:name="android.permission.INTERNET" /> -->
  <!-- <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /> -->
  ```
- [ ] **Verify**: App functions completely offline
- [ ] **Test**: Enable airplane mode and run full flow

### Step 2: Gallery Model Management Integration
- [ ] **File**: Update `Android/src/app/src/main/assets/model_allowlist.json`
- [ ] **Add Smart Loan Models**:
  ```json
  {
    "models": [
      {
        "name": "Gemma3n-E4B-SmartLoan",
        "modelId": "google/gemma-3n-E4B-it-litert-preview",
        "modelFile": "gemma-3n-E4B-it-int4.task",
        "description": "Vision model for ID and payslip data extraction",
        "sizeInBytes": 4405655031,
        "estimatedPeakMemoryInBytes": 6979321856,
        "llmSupportImage": true,
        "taskTypes": ["smartloan_extraction"]
      }
    ]
  }
  ```
- [ ] **Integration**: Use Gallery's model downloader for Smart Loan models
- [ ] **UI**: Show model download progress in Smart Loan flow

### Step 3: Memory Management Optimization
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/memory/MemoryOptimizer.kt`
- [ ] **Memory Guard**:
  ```kotlin
  class MemoryOptimizer(private val context: Context) {
    
    fun checkAvailableMemory(): Long {
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memInfo = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(memInfo)
      return memInfo.availMem
    }
    
    fun isMemorySufficientForVisionProcessing(): Boolean {
      val requiredMemory = 4L * 1024 * 1024 * 1024 // 4GB
      return checkAvailableMemory() > requiredMemory
    }
    
    fun showLowMemoryWarning(onProceed: () -> Unit, onCancel: () -> Unit) {
      // Show dialog about memory constraints
      // Suggest closing other apps
    }
  }
  ```

### Step 4: Performance Optimization
- [ ] **Image Processing**:
  - Compress images before AI processing
  - Process images sequentially to avoid memory spikes
  - Recycle bitmaps immediately after use
- [ ] **AI Model Loading**:
  - Pre-load models during app start
  - Keep models in memory during active session
  - Unload models when Smart Loan flow exits
- [ ] **UI Performance**:
  - Lazy loading for heavy components
  - Background processing for non-UI tasks
  - Smooth animations during processing

### Step 5: Comprehensive Error Handling
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/ErrorHandler.kt`
- [ ] **Error Scenarios**:
  ```kotlin
  sealed class SmartLoanError(val message: String, val actionable: Boolean = true) {
    object InsufficientMemory : SmartLoanError("Device memory too low", false)
    object ModelLoadFailed : SmartLoanError("AI model failed to load", true)
    object CameraPermissionDenied : SmartLoanError("Camera permission required", true)
    object ImageTooBlurry : SmartLoanError("Image quality too low", true)
    object ExtractionFailed : SmartLoanError("Could not read document", true)
    object BiometricUnavailable : SmartLoanError("Fingerprint not available", false)
    object ValidationFailed : SmartLoanError("Application requirements not met", false)
    class UnexpectedError(msg: String) : SmartLoanError(msg, true)
  }
  
  class ErrorHandler {
    fun handleError(error: SmartLoanError): ErrorAction {
      return when (error) {
        is SmartLoanError.ImageTooBlurry -> ErrorAction.ShowRetakeDialog
        is SmartLoanError.ModelLoadFailed -> ErrorAction.ShowRetryDialog
        is SmartLoanError.InsufficientMemory -> ErrorAction.ShowMemoryWarning
        // ... handle all error types
      }
    }
  }
  ```

### Step 6: UI Polish and Banking Standards
- [ ] **Design System**:
  - Consistent color scheme (professional blue/green)
  - Standard banking typography (readable, authoritative)
  - Professional icons and imagery
  - Consistent spacing and alignment
- [ ] **Animations**:
  - Smooth transitions between screens
  - Loading animations during processing
  - Success/error feedback animations
  - Progress indicators for long operations
- [ ] **Accessibility**:
  - Content descriptions for all UI elements
  - Proper contrast ratios (WCAG AA)
  - Large touch targets (48dp minimum)
  - Screen reader compatibility

### Step 7: Demo Data and Test Scenarios
- [ ] **File**: Create `Android/src/app/src/main/assets/demo/`
- [ ] **Sample Images**:
  - `demo_id_front.jpg` - Clear Kenyan ID front
  - `demo_id_back.jpg` - Clear Kenyan ID back  
  - `demo_payslip_1.jpg` - Recent payslip
  - `demo_payslip_2.jpg` - Recent payslip
  - `demo_payslip_3.jpg` - Recent payslip
  - `demo_payslip_4.jpg` - Recent payslip
- [ ] **Test Data**:
  ```kotlin
  object DemoData {
    val DEMO_ID_DATA = IdCardData(
      fullName = "John Mwangi Kariuki",
      idNumber = "12345678",
      dateOfBirth = "1985-03-15",
      expiryDate = "2030-03-15",
      extractionConfidence = 0.95f,
      isValid = true
    )
    
    val DEMO_PAYSLIPS = listOf(
      PayslipData(
        employeeName = "John Mwangi Kariuki", 
        employerName = "Safaricom Ltd",
        grossSalary = 85000.0,
        netSalary = 62000.0,
        payPeriod = "2024-06",
        extractionConfidence = 0.92f,
        isValid = true
      )
      // ... 3 more demo payslips
    )
  }
  ```

### Step 8: Production APK Build Configuration
- [ ] **File**: `Android/src/app/build.gradle.kts`
- [ ] **Release Configuration**:
  ```kotlin
  android {
    buildTypes {
      release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
        
        // Remove debug features
        buildConfigField("boolean", "DEBUG_MODE", "false")
        buildConfigField("boolean", "DEMO_MODE", "true")
      }
    }
    
    signingConfigs {
      create("release") {
        storeFile = file("../keystore/smartloan.keystore")
        storePassword = "your_store_password"
        keyAlias = "smartloan"
        keyPassword = "your_key_password"
      }
    }
  }
  ```

### Step 9: Demo Mode Implementation
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/DemoModeManager.kt`
- [ ] **Demo Features**:
  ```kotlin
  class DemoModeManager {
    fun isDemoMode(): Boolean = BuildConfig.DEMO_MODE
    
    fun loadDemoImages(): DemoImages {
      // Load pre-captured demo images from assets
      // Skip camera capture in demo mode
    }
    
    fun simulateAIProcessing(onProgress: (Float) -> Unit, onComplete: () -> Unit) {
      // Simulate AI processing with realistic timing
      // 2-3 seconds per image with progress updates
    }
    
    fun generateDemoOffer(): LoanOffer {
      // Return realistic loan offer based on demo data
    }
  }
  ```

### Step 10: Create Demo Documentation
- [ ] **File**: `SMART_LOAN_DEMO_GUIDE.md`
- [ ] **Content Structure**:
  ```markdown
  # Smart Loan Demo Guide
  
  ## Quick Start (30 seconds)
  1. Install SmartLoan_v1.0-demo.apk
  2. Switch phone to Airplane Mode
  3. Open app → Smart Loan Demo
  4. Follow on-screen prompts
  5. Complete full flow in <2 minutes
  
  ## Demo Scenarios
  ### Scenario 1: Successful Application
  - Use demo images (pre-loaded)
  - Shows approval with KES 120,000 offer
  - Demonstrates biometric acceptance
  
  ### Scenario 2: Failed Validation
  - Modify demo data to trigger rule failures
  - Shows clear rejection reasons
  
  ## Key Talking Points for Executives
  - "Everything runs offline"
  - "AI extracts data in seconds"
  - "Follows banking compliance rules"
  - "Biometric security built-in"
  ```

### Step 11: Performance Benchmarking
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/benchmark/PerformanceBenchmark.kt`
- [ ] **Metrics to Track**:
  ```kotlin
  data class PerformanceBenchmark(
    val imageProcessingTime: Long,
    val aiExtractionTime: Long,
    val ruleValidationTime: Long,
    val totalFlowTime: Long,
    val peakMemoryUsage: Long,
    val deviceModel: String,
    val timestamp: Long
  )
  
  fun benchmarkFullFlow(): PerformanceBenchmark {
    // Measure each step of the loan application process
    // Log results for optimization analysis
  }
  ```

### Step 12: Final Quality Assurance
- [ ] **Full Flow Testing**:
  - Test complete application flow 10 times
  - Test with different image qualities
  - Test edge cases and error scenarios
  - Test memory usage during extended sessions
- [ ] **Device Compatibility**:
  - Test on Samsung S24 Ultra (primary target)
  - Test on mid-range devices (6GB RAM)
  - Verify performance meets targets
- [ ] **Airplane Mode Verification**:
  - Complete 5 full applications in airplane mode
  - Verify no network calls or crashes
  - Test all error scenarios offline

## 🔧 **Technical Implementation Details**

### APK Optimization
```bash
# Build optimized release APK
./gradlew assembleRelease

# Verify APK size and contents
bundletool dump manifests --bundle=app-release.aab
```

### Memory Profiling
```kotlin
fun logMemoryUsage(tag: String) {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    Log.d("MemoryProfiler", "$tag: ${usedMemory / 1024 / 1024} MB")
}
```

### Performance Testing
```kotlin
fun measureExecutionTime(operation: String, block: () -> Unit): Long {
    val startTime = System.currentTimeMillis()
    block()
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime
    Log.d("Performance", "$operation: ${duration}ms")
    return duration
}
```

## 🧪 **Demo Testing Scenarios**

### Executive Demo Flow (2 minutes)
1. **Opening** (15s): Launch app, navigate to Smart Loan
2. **Image Capture** (30s): Capture or load demo images
3. **AI Processing** (45s): Show extraction and validation
4. **Offer Display** (30s): Present loan offer with terms
5. **Acceptance** (20s): Biometric auth and success screen

### Stress Testing
- [ ] **Memory Stress**: Run 20 consecutive applications
- [ ] **Error Recovery**: Test all error scenarios
- [ ] **Battery Impact**: Measure power consumption
- [ ] **Performance Degradation**: Long-running session testing

## 🎯 **Definition of Done**
- ✅ App runs completely offline (airplane mode works)
- ✅ Full flow completes in <7 seconds on S24 Ultra
- ✅ Memory usage stays under 4GB during processing
- ✅ Professional UI meets banking industry standards
- ✅ All error scenarios handled gracefully
- ✅ Production APK signed and optimized
- ✅ Demo guide provides clear instructions
- ✅ Performance benchmarks meet targets
- ✅ 100% success rate in airplane mode testing
- ✅ Ready for board-room presentation

## 📅 **Estimated Time**
**3-4 days** for experienced Android developer
**4-5 days** for developer new to production optimization

## 🎯 **Final Deliverables**
- ✅ `SmartLoan_v1.0-demo.apk` (signed, optimized)
- ✅ `SMART_LOAN_DEMO_GUIDE.md` (executive presentation guide)
- ✅ Performance benchmark report
- ✅ Demo video (2-minute walkthrough)
- ✅ Technical documentation for future developers

## 🔗 **Next Task**
After completion: Move to `TASK_07_BUFFER_WEEK.md` (bug fixes and UAT) 