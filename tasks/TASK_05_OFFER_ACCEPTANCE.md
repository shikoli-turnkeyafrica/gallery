# TASK 05: Offer Screen & Acceptance Flow (Week 5)

## 🎯 **Objective**
Create the final loan offer screen with biometric authentication for acceptance and generate disbursement memo for back-office processing.

## ✅ **Success Criteria**
- Professional loan offer display with all key terms
- Biometric authentication (fingerprint) for acceptance
- Disbursement memo generated as JSON
- Offer decline functionality with feedback
- Amount adjustment capability within limits
- Legal terms and conditions acceptance
- Clean, bank-grade UI/UX

## 📋 **Detailed Steps**

### Step 1: Enhanced OfferScreen UI Design
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/OfferScreen.kt`
- [ ] **UI Components**:
  ```kotlin
  @Composable
  fun OfferScreen(
    loanOffer: LoanOffer,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    onAmountAdjust: (newAmount: Double) -> Unit,
    onViewTerms: () -> Unit
  ) {
    // Loan offer summary card
    // Amount adjustment slider  
    // Terms breakdown table
    // Biometric accept button
    // Decline options
  }
  ```

### Step 2: Loan Offer Summary Card
- [ ] **Design Elements**:
  - **Loan Amount**: Large, prominent display (KES 150,000)
  - **Monthly Payment**: Clear installment amount
  - **Interest Rate**: Annual percentage rate
  - **Loan Term**: Duration in months
  - **Total Repayment**: Full amount to be paid
  - **DSR Indicator**: Visual gauge showing debt ratio
  - **Offer Validity**: Countdown timer

### Step 3: Amount Adjustment Interface
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/AmountAdjustmentCard.kt`
- [ ] **Features**:
  ```kotlin
  @Composable
  fun AmountAdjustmentCard(
    currentAmount: Double,
    minAmount: Double,
    maxAmount: Double,
    onAmountChange: (Double) -> Unit
  ) {
    // Interactive slider for amount selection
    // Real-time payment calculation updates
    // DSR warning if exceeding limits
    // "Recalculate" button for new terms
  }
  ```

### Step 4: Terms and Conditions Component
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/TermsAndConditions.kt`
- [ ] **Content**:
  ```kotlin
  @Composable
  fun TermsAndConditionsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
  ) {
    // Scrollable terms text
    // Key highlights section
    // Checkbox for "I agree" 
    // "Accept Terms" button
  }
  ```
- [ ] **Terms Content**: Add realistic loan terms in `assets/loan_terms.txt`

### Step 5: Biometric Authentication Implementation
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/BiometricAuthenticator.kt`
- [ ] **Dependencies**: Add to `build.gradle.kts`:
  ```kotlin
  implementation "androidx.biometric:biometric:1.1.0"
  ```
- [ ] **Implementation**:
  ```kotlin
  class BiometricAuthenticator(private val context: Context) {
    
    fun authenticate(
      title: String = "Confirm Loan Acceptance",
      subtitle: String = "Use fingerprint to confirm your loan application",
      onSuccess: () -> Unit,
      onError: (String) -> Unit,
      onCancel: () -> Unit
    ) {
      val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
          }
          
          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onError(errString.toString())
          }
          
          override fun onAuthenticationFailed() {
            onError("Authentication failed. Please try again.")
          }
        }
      )
      
      val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Cancel")
        .build()
      
      biometricPrompt.authenticate(promptInfo)
    }
  }
  ```

### Step 6: Disbursement Memo Generator
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/memo/DisbursementMemoGenerator.kt`
- [ ] **Memo Structure**:
  ```kotlin
  data class DisbursementMemo(
    val applicationId: String,
    val timestamp: Long,
    val applicantInfo: ApplicantInfo,
    val loanDetails: LoanDetails,
    val verification: VerificationInfo,
    val riskAssessment: RiskAssessment,
    val approvalInfo: ApprovalInfo,
    val status: String = "APPROVED_PENDING_DISBURSEMENT"
  )
  
  data class ApplicantInfo(
    val fullName: String,
    val idNumber: String,
    val dateOfBirth: String,
    val employerName: String,
    val grossSalary: Double,
    val netSalary: Double
  )
  
  data class LoanDetails(
    val approvedAmount: Double,
    val interestRate: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val totalRepayment: Double,
    val dsr: Double
  )
  
  data class VerificationInfo(
    val idVerified: Boolean,
    val salaryVerified: Boolean,
    val extractionConfidenceAverage: Float,
    val businessRulesPass: Boolean,
    val verificationTimestamp: Long
  )
  ```

### Step 7: Memo Storage and Export
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/storage/MemoStorage.kt`
- [ ] **Storage Functions**:
  ```kotlin
  class MemoStorage(private val context: Context) {
    
    fun saveDisbursementMemo(memo: DisbursementMemo): String {
      val memoDir = File(context.filesDir, "memos")
      if (!memoDir.exists()) memoDir.mkdirs()
      
      val filename = "disbursement_${memo.applicationId}_${System.currentTimeMillis()}.json"
      val file = File(memoDir, filename)
      
      val gson = GsonBuilder().setPrettyPrinting().create()
      file.writeText(gson.toJson(memo))
      
      return file.absolutePath
    }
    
    fun getAllMemos(): List<DisbursementMemo> {
      // Load all memo files from storage
    }
    
    fun exportMemoToDocuments(memo: DisbursementMemo): Boolean {
      // Copy memo to Documents folder for easy access
    }
  }
  ```

### Step 8: Offer Decline Handling
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/DeclineOptionsDialog.kt`
- [ ] **Decline Reasons**:
  ```kotlin
  val DECLINE_REASONS = listOf(
    "Amount too low",
    "Interest rate too high", 
    "Monthly payment too high",
    "Loan term too short",
    "Changed my mind",
    "Found better offer elsewhere",
    "Other"
  )
  
  @Composable
  fun DeclineOptionsDialog(
    onDecline: (reason: String, feedback: String) -> Unit,
    onDismiss: () -> Unit
  ) {
    // Radio buttons for decline reasons
    // Optional feedback text field
    // "Submit Decline" button
  }
  ```

### Step 9: Success and Confirmation Screens
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/AcceptanceSuccessScreen.kt`
- [ ] **Success Flow**:
  ```kotlin
  @Composable
  fun AcceptanceSuccessScreen(
    applicationId: String,
    memoPath: String,
    onFinish: () -> Unit,
    onViewMemo: () -> Unit
  ) {
    // Success animation/checkmark
    // Application ID display
    // "What happens next" information
    // "View Disbursement Memo" button
    // "Start New Application" button
  }
  ```

### Step 10: Integration with SmartLoanViewModel
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
- [ ] **Add State**:
  ```kotlin
  data class SmartLoanUiState(
    // ... existing fields
    val termsAccepted: Boolean = false,
    val biometricAuthInProgress: Boolean = false,
    val applicationAccepted: Boolean? = null,
    val disbursementMemo: DisbursementMemo? = null,
    val memoFilePath: String? = null,
    val declineReason: String? = null
  )
  ```
- [ ] **Add Functions**:
  ```kotlin
  fun acceptTermsAndConditions()
  fun adjustLoanAmount(newAmount: Double)
  suspend fun acceptLoanOffer()
  suspend fun declineLoanOffer(reason: String, feedback: String)
  fun generateApplicationId(): String
  fun resetApplication()
  ```

## 🔧 **Technical Implementation Details**

### Biometric Capability Check
```kotlin
fun isBiometricAvailable(): Boolean {
    return when (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        else -> false
    }
}
```

### Amount Adjustment Logic
```kotlin
fun adjustLoanAmount(newAmount: Double): LoanOffer? {
    if (newAmount < minAmount || newAmount > maxAmount) return null
    
    val newMonthlyPayment = calculator.calculateMonthlyPayment(
        newAmount, currentOffer.interestRate, currentOffer.termMonths
    )
    
    val newDSR = calculator.calculateDSR(
        averageSalary, existingDeductions, newMonthlyPayment
    )
    
    if (newDSR > policy.maxDSR) return null
    
    return currentOffer.copy(
        recommendedAmount = newAmount,
        monthlyPayment = newMonthlyPayment,
        totalRepayment = newAmount * (1 + currentOffer.interestRate),
        dsr = newDSR
    )
}
```

### Application ID Generation
```kotlin
fun generateApplicationId(): String {
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
    val random = (1000..9999).random()
    return "SL$timestamp$random"
}
```

## 🧪 **Testing Checklist**
- [ ] Loan offer displays correctly with all terms
- [ ] Amount adjustment works within policy limits
- [ ] DSR updates dynamically with amount changes
- [ ] Biometric authentication prompts correctly
- [ ] Terms and conditions display properly
- [ ] Acceptance generates valid disbursement memo
- [ ] Decline flow captures feedback correctly
- [ ] Success screen shows all required info
- [ ] Memo files are accessible in Documents folder
- [ ] Application reset clears all data

## 📱 **UI/UX Requirements**

### Visual Design
- [ ] **Professional Banking Look**: Clean, trustworthy design
- [ ] **Clear Information Hierarchy**: Most important info prominent
- [ ] **Accessible Colors**: Good contrast, colorblind-friendly
- [ ] **Responsive Layout**: Works well on S24 Ultra screen

### User Experience
- [ ] **Clear CTAs**: Obvious next steps for user
- [ ] **Progress Indication**: User knows where they are in flow
- [ ] **Error Handling**: Graceful handling of biometric failures
- [ ] **Confirmation Steps**: Multiple confirmations before final accept

### Accessibility
- [ ] **Screen Reader Support**: Proper content descriptions
- [ ] **Large Touch Targets**: Easy to tap buttons
- [ ] **Keyboard Navigation**: Full functionality without touch

## 🎯 **Definition of Done**  
- ✅ Professional loan offer screen with all terms clearly displayed
- ✅ Amount adjustment works within policy constraints
- ✅ Biometric authentication successfully protects acceptance
- ✅ Disbursement memo generated with all required fields
- ✅ Decline flow captures user feedback
- ✅ Success confirmation provides clear next steps
- ✅ All UI components follow banking design standards
- ✅ Memo files accessible for back-office processing
- ✅ Full flow tested on Samsung S24 Ultra
- ✅ Error scenarios handled gracefully

## 📅 **Estimated Time**
**4-5 days** for experienced Android developer
**5-6 days** for developer new to biometric authentication

## 🔗 **Next Task**
After completion: Move to `TASK_06_MVP_POLISH.md` 