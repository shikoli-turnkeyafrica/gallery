# TASK 04: Rule Validation & Affordability Calculation (Week 4)

## 🎯 **Objective**
Implement business rules engine and affordability calculator to validate extracted data and determine loan eligibility and offer terms.

## ✅ **Success Criteria**
- 5 core business rules implemented and tested
- Debt Service Ratio (DSR) calculation accurate
- Policy rules configurable via JSON
- Clear validation error messages
- Loan amount calculation based on affordability
- All rules have comprehensive unit tests

## 📋 **Detailed Steps**

### Step 1: Define Business Rules Framework
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/validation/BusinessRule.kt`
- [ ] **Rule Interface**:
  ```kotlin
  interface BusinessRule<T> {
    val name: String
    val description: String
    val priority: Int // 1 = highest priority
    
    suspend fun validate(data: T): ValidationResult
  }
  
  data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
    val confidence: Float = 1.0f
  )
  ```

### Step 2: Implement Core Business Rules
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/validation/CoreBusinessRules.kt`
- [ ] **Rule 1 - Payslip Recency**:
  ```kotlin
  class PayslipRecencyRule : BusinessRule<List<PayslipData>> {
    override suspend fun validate(data: List<PayslipData>): ValidationResult {
      // Check all payslips are within last 3 months
      // Return error if any payslip is too old
    }
  }
  ```
- [ ] **Rule 2 - Name Consistency**:
  ```kotlin
  class NameConsistencyRule : BusinessRule<Pair<IdCardData, List<PayslipData>>> {
    override suspend fun validate(data: Pair<IdCardData, List<PayslipData>>): ValidationResult {
      // Verify ID name matches payslip names (with fuzzy matching)
      // Handle common variations (middle names, titles, etc.)
    }
  }
  ```
- [ ] **Rule 3 - Retirement Age Check**:
  ```kotlin
  class RetirementAgeRule : BusinessRule<IdCardData> {
    override suspend fun validate(data: IdCardData): ValidationResult {
      // Calculate age from date of birth
      // Reject if age + loan term > 60 years
    }
  }
  ```
- [ ] **Rule 4 - Data Quality Check**:
  ```kotlin
  class DataQualityRule : BusinessRule<ExtractionData> {
    override suspend fun validate(data: ExtractionData): ValidationResult {
      // Check extraction confidence scores
      // Minimum 0.85 confidence for approval
    }
  }
  ```

### Step 3: Implement DSR (Debt Service Ratio) Calculator
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/finance/AffordabilityCalculator.kt`
- [ ] **Core Functions**:
  ```kotlin
  class AffordabilityCalculator {
    
    fun calculateDSR(
      grossSalary: Double,
      existingDeductions: Map<String, Double>,
      proposedLoanPayment: Double
    ): Double {
      // DSR = (Total Monthly Debt Payments / Gross Monthly Income) * 100
      // Include PAYE, NSSF, existing loans, proposed loan
    }
    
    fun calculateMaxLoanAmount(
      grossSalary: Double,
      existingDeductions: Map<String, Double>,
      interestRate: Double = 0.15, // 15% annual
      termMonths: Int = 12
    ): Double {
      // Calculate maximum loan keeping DSR <= 50%
    }
    
    fun calculateMonthlyPayment(
      loanAmount: Double,
      interestRate: Double,
      termMonths: Int
    ): Double {
      // PMT formula: P * [r(1+r)^n] / [(1+r)^n - 1]
    }
  }
  ```

### Step 4: Create Policy Configuration System
- [ ] **File**: `Android/src/app/src/main/assets/policy_rules.json`
- [ ] **Configuration Structure**:
  ```json
  {
    "lendingPolicy": {
      "maxDSR": 0.50,
      "minAge": 18,
      "maxAge": 60,
      "minSalary": 15000,
      "maxLoanAmount": 500000,
      "defaultInterestRate": 0.15,
      "defaultTermMonths": 12,
      "payslipRecencyMonths": 3,
      "minExtractionConfidence": 0.85
    },
    "validationRules": {
      "payslipRecency": {
        "enabled": true,
        "maxAgeMonths": 3,
        "errorMessage": "Payslips must be from the last 3 months"
      },
      "nameConsistency": {
        "enabled": true,
        "fuzzyMatchThreshold": 0.8,
        "errorMessage": "ID name must match payslip names"
      }
    }
  }
  ```

### Step 5: Implement Rule Validation Engine
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/validation/RuleValidationEngine.kt`
- [ ] **Engine Functions**:
  ```kotlin
  class RuleValidationEngine(private val context: Context) {
    
    private val rules: List<BusinessRule<*>> = loadRulesFromConfig()
    
    suspend fun validateApplication(
      idData: IdCardData,
      payslipData: List<PayslipData>
    ): ApplicationValidationResult {
      
      val results = mutableListOf<ValidationResult>()
      
      // Run all rules in priority order
      rules.sortedBy { it.priority }.forEach { rule ->
        val result = when (rule) {
          is PayslipRecencyRule -> rule.validate(payslipData)
          is NameConsistencyRule -> rule.validate(idData to payslipData)
          is RetirementAgeRule -> rule.validate(idData)
          // ... other rules
        }
        results.add(result)
      }
      
      return ApplicationValidationResult(
        isEligible = results.all { it.isValid },
        validationResults = results,
        failedRules = results.filter { !it.isValid }
      )
    }
  }
  ```

### Step 6: Create Loan Offer Generation
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/finance/LoanOfferGenerator.kt`
- [ ] **Offer Logic**:
  ```kotlin
  data class LoanOffer(
    val maxAmount: Double,
    val recommendedAmount: Double,
    val interestRate: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val totalRepayment: Double,
    val dsr: Double,
    val offerValidUntil: Long,
    val conditions: List<String> = emptyList()
  )
  
  class LoanOfferGenerator(
    private val calculator: AffordabilityCalculator,
    private val policy: LendingPolicy
  ) {
    
    fun generateOffer(
      idData: IdCardData,
      payslipData: List<PayslipData>,
      validationResult: ApplicationValidationResult
    ): LoanOffer? {
      
      if (!validationResult.isEligible) return null
      
      val averageSalary = payslipData.map { it.grossSalary }.average()
      val maxAmount = calculator.calculateMaxLoanAmount(averageSalary)
      
      // Conservative offer at 80% of max
      val recommendedAmount = maxAmount * 0.8
      
      return LoanOffer(
        maxAmount = maxAmount,
        recommendedAmount = recommendedAmount,
        interestRate = policy.defaultInterestRate,
        termMonths = policy.defaultTermMonths,
        monthlyPayment = calculator.calculateMonthlyPayment(
          recommendedAmount, policy.defaultInterestRate, policy.defaultTermMonths
        ),
        totalRepayment = recommendedAmount * (1 + policy.defaultInterestRate),
        dsr = calculator.calculateDSR(averageSalary, getDeductions(payslipData), 
          calculator.calculateMonthlyPayment(recommendedAmount, policy.defaultInterestRate, policy.defaultTermMonths)),
        offerValidUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
      )
    }
  }
  ```

### Step 7: Integration with SmartLoanViewModel
- [ ] **File**: Update `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
- [ ] **Add State**:
  ```kotlin
  data class SmartLoanUiState(
    // ... existing fields
    val validationResult: ApplicationValidationResult? = null,
    val loanOffer: LoanOffer? = null,
    val validationInProgress: Boolean = false,
    val validationError: String? = null
  )
  ```
- [ ] **Add Functions**:
  - `validateApplication()`
  - `generateLoanOffer()`
  - `recalculateOffer(newAmount: Double)`

### Step 8: Create Comprehensive Unit Tests
- [ ] **File**: `Android/src/app/src/test/java/com/google/ai/edge/gallery/smartloan/BusinessRulesTest.kt`
- [ ] **Test Scenarios**:
  ```kotlin
  class BusinessRulesTest {
    
    @Test
    fun `payslip recency rule rejects old payslips`() {
      // Test with payslips older than 3 months
    }
    
    @Test
    fun `name consistency rule handles variations`() {
      // Test with "John Doe" vs "John M. Doe" vs "JOHN DOE"
    }
    
    @Test
    fun `DSR calculation is accurate`() {
      // Test with known salary and deduction values
    }
    
    @Test
    fun `retirement age rule rejects applicants too close to 60`() {
      // Test age boundary conditions
    }
    
    @Test
    fun `loan offer generation respects policy limits`() {
      // Test maximum amounts and DSR limits
    }
  }
  ```

## 🔧 **Technical Implementation Details**

### Fuzzy Name Matching
```kotlin
private fun calculateNameSimilarity(name1: String, name2: String): Double {
    val distance = JaroWinklerDistance()
    return distance.apply(
        name1.uppercase().trim(),
        name2.uppercase().trim()
    )
}
```

### Date Calculations
```kotlin
private fun calculateAge(dateOfBirth: String): Int {
    val dob = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return Period.between(dob, LocalDate.now()).years
}
```

### Policy Loading
```kotlin
private fun loadPolicyFromAssets(): LendingPolicy {
    val json = context.assets.open("policy_rules.json").bufferedReader().use { it.readText() }
    return Gson().fromJson(json, LendingPolicy::class.java)
}
```

## 🧪 **Testing Strategy**

### Test Data Sets
- [ ] **Valid Applications**: Should pass all rules and generate offers
- [ ] **Edge Cases**: Boundary conditions for age, DSR, salary
- [ ] **Invalid Applications**: Should fail specific rules with clear messages
- [ ] **Name Variations**: Test fuzzy matching tolerance

### Performance Tests
- [ ] **Rule Execution Time**: <1 second for full validation
- [ ] **Memory Usage**: Minimal heap usage during calculations
- [ ] **Concurrent Access**: Thread-safe rule execution

## 🎯 **Business Logic Validation**

| Rule | Pass Criteria | Fail Criteria | Expected Behavior |
|------|---------------|---------------|-------------------|
| **Payslip Recency** | All within 3 months | Any >3 months old | Reject with specific date |
| **Name Consistency** | >80% similarity | <80% similarity | Reject with name mismatch |
| **Age Limit** | 18-60 years | <18 or >60 | Reject with age limit |
| **DSR Limit** | ≤50% | >50% | Reduce loan amount or reject |
| **Data Quality** | >85% confidence | <85% confidence | Request image retake |

## 🎯 **Definition of Done**
- ✅ All 5 business rules implemented and tested
- ✅ DSR calculation matches financial industry standards
- ✅ Policy rules configurable via JSON
- ✅ Clear, actionable error messages for failed rules
- ✅ Loan offers generated within policy constraints
- ✅ Unit test coverage >90%
- ✅ Integration tests with sample data pass
- ✅ Performance meets targets (<1s validation time)

## 📅 **Estimated Time**
**4-5 days** for experienced fintech developer
**6-7 days** for developer new to financial calculations

## 🔗 **Next Task**
After completion: Move to `TASK_05_OFFER_ACCEPTANCE.md` 