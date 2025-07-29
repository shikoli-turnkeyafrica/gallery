# TASK 02: Image Capture & Storage (Week 2) ✅ **COMPLETED WITH ENHANCEMENTS**

## 🎯 **Objective**
Implement **multiple image input methods** (Camera, Gallery, Files) to capture ID and payslip images, with secure local storage and persistence across app restarts.

## 🚀 **Enhanced Features Added**
- **Multiple Input Sources**: Camera capture, Gallery selection, and File picker
- **Material 3 UI**: Professional bottom sheet selection dialog
- **File Format Support**: Images (JPG, PNG) and PDF documents
- **Enhanced UX**: "Add Image" workflow with clear options
- **Real-time Processing**: Automatic orientation detection and compression

## ✅ **Success Criteria**
- Front and back ID images can be captured/selected and stored ✅
- 4 payslip images can be captured/selected and stored ✅
- **Multiple input methods**: Camera, Gallery, and File selection ✅
- Images persist across app restarts ✅
- Thumbnails display correctly with delete/retake options ✅
- All images stored securely using EncryptedFile ✅
- No memory leaks from bitmap handling ✅
- **Enhanced UX**: User-friendly source selection dialog ✅

## 📋 **Detailed Steps**

### Step 1: Enhanced Image Input Infrastructure ✅
- [x] **Study**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/common/chat/MessageInputImage.kt`
- [x] **Extract**: Camera helper functions from Gallery "Ask Image" feature
- [x] **Create**: `SmartLoanCameraHelper.kt` in smartloan package
- [x] **Adapt**: Camera functionality for ID/payslip specific needs
- [x] **Enhanced**: Added gallery and file selection capabilities
- [x] **Multiple Sources**: Camera, Gallery (`GetContent`), Files (`OpenDocument`)

### Step 2: Create Image Data Models ✅
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/data/CapturedImages.kt`
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

### Step 3: Implement Secure Storage Manager ✅
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/storage/SecureImageStorage.kt`
- [x] **Dependencies**: Add androidx.security:security-crypto to build.gradle
- [ ] **Functions**:
  - `saveImageSecurely(bitmap: Bitmap, filename: String): String`
  - `loadImageSecurely(path: String): Bitmap?`
  - `deleteImageSecurely(path: String): Boolean`
  - `clearAllImages(): Boolean`

### Step 3.5: Image Source Selection Dialog ✅
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/ImageSourceSelectionDialog.kt`
- [x] **Features**:
  - Material 3 bottom sheet design with drag handle
  - Three selection options: "Take Photo", "Choose from Gallery", "Browse Files"
  - Proper icons and descriptions for each option
  - Accessible design with clear visual hierarchy
  - Support for PDF files in addition to images

### Step 4: Enhanced IdCaptureScreen Implementation ✅
- [x] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/IdCaptureScreen.kt`
- [x] **Features**:
  - **Multiple input options**: Camera, Gallery, File selection via dialog
  - "Add Image" buttons that open source selection dialog
  - Thumbnail display of captured/selected images
  - "Retake" and "Delete" options for each image
  - "Continue" button (enabled only when both images captured)
  - Real-time image processing and validation
- [x] **Integration**: Use SecureImageStorage for persistence

### Step 5: Enhanced PayslipCaptureScreen Implementation ✅
- [x] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/PayslipCaptureScreen.kt`
- [x] **Features**:
  - Grid layout showing 4 payslip slots
  - **Multiple input options**: Camera, Gallery, File selection for each payslip
  - Clear labeling (Payslip 1, 2, 3, 4)
  - Progress indicator (X of 4 captured) with progress bar
  - Thumbnail previews with retake/delete options
  - "Continue" button (enabled when minimum 1 payslip captured)
  - Real-time validation and error handling

### Step 6: Update SmartLoanViewModel for Data Management ✅
- [x] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
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

### Step 7: Add Image Quality Validation ✅
- [x] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/validation/ImageQualityChecker.kt`
- [x] **Validations**:
  - Minimum resolution check (800x600)
  - Blur detection (Laplacian variance method)
  - Brightness validation (not too dark/bright)
  - File size management (<5MB per image)
  - Document orientation detection
- [x] **Integration**: Real-time validation with user feedback

### Step 8: Memory Management & Optimization ✅
- [x] **Bitmap Recycling**: Proper cleanup of unused bitmaps
- [x] **Image Compression**: Compress images while maintaining quality
- [x] **Memory Monitoring**: Add memory usage logging and storage checks
- [x] **Background Processing**: Move image operations off main thread
- [x] **Secure Storage**: EncryptedFile with proper key management

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
- [x] **Multiple Input Methods**: Camera, Gallery, and File selection work for all images ✅
- [x] Capture/select front and back ID images successfully ✅
- [x] Capture/select all 4 payslip images successfully ✅
- [x] Images persist after closing and reopening app ✅
- [x] Thumbnails display correctly ✅
- [x] Delete/retake functionality works ✅
- [x] **Image Source Dialog**: Bottom sheet selection works correctly ✅
- [x] **File Support**: PDF and various image formats supported ✅
- [x] No memory leaks during repeated capture/delete cycles ✅
- [x] Works properly on Samsung S24 Ultra ✅
- [x] Low storage scenarios handled gracefully ✅
- [x] App rotation doesn't lose captured images ✅
- [x] **Permission Handling**: Camera permissions requested properly ✅
- [x] **Error Handling**: User-friendly error messages for failed operations ✅

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
- ✅ All 6 images (2 ID + 4 payslips) can be captured/selected and stored
- ✅ **Multiple input methods**: Camera, Gallery, and File selection supported
- ✅ Images persist across app restarts using EncryptedFile
- ✅ Clean UI with thumbnails, delete, and retake options
- ✅ **Enhanced UX**: Material 3 bottom sheet for source selection
- ✅ **File Format Support**: Images and PDFs can be selected
- ✅ No memory leaks or performance issues
- ✅ Proper error handling for edge cases
- ✅ Camera permissions handled correctly  
- ✅ Image quality validation provides helpful feedback
- ✅ **Real-time Processing**: Image orientation and compression handling
- ✅ **Secure Storage**: Banking-grade encrypted file storage

## 📅 **Estimated Time**
**4-5 days** for experienced Android developer
**5-6 days** for junior developer new to camera/storage APIs

## 🔗 **Next Task**
After completion: Move to `TASK_03_VISION_EXTRACTION.md` 