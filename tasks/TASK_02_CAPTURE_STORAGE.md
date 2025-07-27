# TASK 02: Image Capture & Storage (Week 2)

## 🎯 **Objective**
Implement camera functionality to capture ID and payslip images, with secure local storage and persistence across app restarts.

## ✅ **Success Criteria**
- Front and back ID images can be captured and stored
- 4 payslip images can be captured and stored
- Images persist across app restarts
- Thumbnails display correctly with delete/retake options
- All images stored securely using EncryptedFile
- No memory leaks from bitmap handling

## 📋 **Detailed Steps**

### Step 1: Reuse Gallery Camera Infrastructure
- [ ] **Study**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageInputImage.kt`
- [ ] **Extract**: Camera helper functions from Gallery "Ask Image" feature
- [ ] **Create**: `SmartLoanCameraHelper.kt` in smartloan package
- [ ] **Adapt**: Camera functionality for ID/payslip specific needs

### Step 2: Create Image Data Models
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/data/CapturedImages.kt`
- [ ] **Models**:
  ```kotlin
  data class IdImages(
    val frontId: Bitmap? = null,
    val backId: Bitmap? = null,
    val frontIdPath: String? = null,
    val backIdPath: String? = null
  )
  
  data class PayslipImages(
    val payslip1: Bitmap? = null,
    val payslip2: Bitmap? = null, 
    val payslip3: Bitmap? = null,
    val payslip4: Bitmap? = null,
    val paths: List<String> = emptyList()
  )
  
  data class SmartLoanApplicationData(
    val idImages: IdImages = IdImages(),
    val payslipImages: PayslipImages = PayslipImages(),
    val timestamp: Long = System.currentTimeMillis()
  )
  ```

### Step 3: Implement Secure Storage Manager
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/storage/SecureImageStorage.kt`
- [ ] **Dependencies**: Add androidx.security:security-crypto to build.gradle
- [ ] **Functions**:
  - `saveImageSecurely(bitmap: Bitmap, filename: String): String`
  - `loadImageSecurely(path: String): Bitmap?`
  - `deleteImageSecurely(path: String): Boolean`
  - `clearAllImages(): Boolean`

### Step 4: Enhanced IdCaptureScreen Implementation
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/IdCaptureScreen.kt`
- [ ] **Features**:
  - Camera preview with overlay guides for ID positioning
  - "Capture Front ID" and "Capture Back ID" buttons
  - Thumbnail display of captured images
  - "Retake" and "Delete" options for each image
  - "Continue" button (enabled only when both images captured)
- [ ] **Integration**: Use SecureImageStorage for persistence

### Step 5: Enhanced PayslipCaptureScreen Implementation  
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/PayslipCaptureScreen.kt`
- [ ] **Features**:
  - Grid layout showing 4 payslip slots
  - Camera integration for each payslip
  - Clear labeling (Payslip 1, 2, 3, 4)
  - Progress indicator (2 of 4 captured)
  - Thumbnail previews with retake options
  - "Continue" button (enabled when all 4 captured)

### Step 6: Update SmartLoanViewModel for Data Management
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
- [ ] **State Management**:
  ```kotlin
  data class SmartLoanUiState(
    val applicationData: SmartLoanApplicationData = SmartLoanApplicationData(),
    val isLoading: Boolean = false,
    val currentScreen: String = "start",
    val errorMessage: String? = null
  )
  ```
- [ ] **Functions**:
  - `captureIdImage(isfront: Boolean, bitmap: Bitmap)`
  - `capturePayslipImage(index: Int, bitmap: Bitmap)`
  - `deleteImage(imageType: String, index: Int? = null)`
  - `loadSavedData()` - for app restart persistence

### Step 7: Add Image Quality Validation
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/validation/ImageQualityChecker.kt`
- [ ] **Validations**:
  - Minimum resolution check (1920x1080)
  - Blur detection (basic Laplacian variance)
  - Brightness validation (not too dark/bright)
  - File size reasonable (<5MB per image)
- [ ] **Integration**: Show warning messages for poor quality images

### Step 8: Memory Management & Optimization
- [ ] **Bitmap Recycling**: Proper cleanup of unused bitmaps
- [ ] **Image Compression**: Compress images while maintaining quality
- [ ] **Memory Monitoring**: Add memory usage logging
- [ ] **Background Processing**: Move image operations off main thread

## 🔧 **Technical Implementation Details**

### EncryptedFile Setup
```kotlin
val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
val fileToWrite = File(context.filesDir, "smartloan_$filename")
val encryptedFile = EncryptedFile.Builder(
    fileToWrite,
    context,
    masterKeyAlias,
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
).build()
```

### Camera Permission Handling
```kotlin
val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // Launch camera
    } else {
        // Show permission denied message
    }
}
```

## 🧪 **Testing Checklist**
- [ ] Capture front and back ID images successfully
- [ ] Capture all 4 payslip images successfully  
- [ ] Images persist after closing and reopening app
- [ ] Thumbnails display correctly
- [ ] Delete/retake functionality works
- [ ] No memory leaks during repeated capture/delete cycles
- [ ] Works properly on Samsung S24 Ultra
- [ ] Low storage scenarios handled gracefully
- [ ] App rotation doesn't lose captured images

## 📦 **Dependencies to Add**
```kotlin
// In Android/src/app/build.gradle.kts
implementation "androidx.security:security-crypto:1.1.0-alpha06"
implementation "androidx.camera:camera-camera2:1.3.0"
implementation "androidx.camera:camera-lifecycle:1.3.0"
implementation "androidx.camera:camera-view:1.3.0"
```

## ⚠️ **Potential Challenges**
- **Large bitmap memory usage**: Implement proper compression
- **Camera permissions**: Handle denied permissions gracefully  
- **Storage space**: Check available space before saving
- **Image orientation**: Handle device rotation correctly
- **Performance**: Ensure smooth UI during image operations

## 🎯 **Definition of Done**
- ✅ All 6 images (2 ID + 4 payslips) can be captured and stored
- ✅ Images persist across app restarts using EncryptedFile
- ✅ Clean UI with thumbnails, delete, and retake options
- ✅ No memory leaks or performance issues
- ✅ Proper error handling for edge cases
- ✅ Camera permissions handled correctly
- ✅ Image quality validation provides helpful feedback

## 📅 **Estimated Time**
**4-5 days** for experienced Android developer
**5-6 days** for junior developer new to camera/storage APIs

## 🔗 **Next Task**
After completion: Move to `TASK_03_VISION_EXTRACTION.md` 