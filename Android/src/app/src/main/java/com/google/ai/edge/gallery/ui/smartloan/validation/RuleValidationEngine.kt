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

package com.google.ai.edge.gallery.ui.smartloan.validation

import android.content.Context
import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import com.google.ai.edge.gallery.ui.smartloan.finance.AffordabilityCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "RuleValidationEngine"

/**
 * Engine that orchestrates validation of loan applications using business rules
 */
class RuleValidationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val calculator = AffordabilityCalculator()
    private lateinit var policy: PolicyConfiguration
    private lateinit var rules: List<BusinessRule<*>>
    
    /**
     * Initializes the validation engine with policy configuration
     */
    fun initialize() {
        try {
            policy = PolicyLoader.loadPolicy(context)
            rules = createBusinessRules()
            Log.d(TAG, "Rule validation engine initialized with ${rules.size} rules")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize rule validation engine", e)
            throw e
        }
    }
    
    /**
     * Validates a complete loan application
     */
    suspend fun validateApplication(
        extractedData: ExtractedApplicationData,
        requestedLoanAmount: Double = 0.0
    ): ApplicationValidationResult {
        
        if (!::policy.isInitialized) {
            initialize()
        }
        
        Log.d(TAG, "Starting application validation with ${rules.size} rules")
        
        val validationResults = mutableListOf<BusinessValidationResult>()
        val failedRules = mutableListOf<String>()
        val warningRules = mutableListOf<String>()
        
        // Execute rules in priority order
        val sortedRules = rules.sortedBy { rule ->
            when (rule) {
                is PayslipRecencyRule -> rule.priority
                is PayslipCountRule -> rule.priority
                is NameConsistencyRule -> rule.priority
                is RetirementAgeRule -> rule.priority
                is DataQualityRule -> rule.priority
                is AffordabilityRule -> rule.priority
                else -> Int.MAX_VALUE
            }
        }
        
        for (rule in sortedRules) {
            try {
                val result = when (rule) {
                    is PayslipRecencyRule -> {
                        Log.d(TAG, "Executing PayslipRecencyRule")
                        rule.validate(extractedData.payslipData)
                    }
                    is PayslipCountRule -> {
                        Log.d(TAG, "Executing PayslipCountRule")
                        rule.validate(extractedData.payslipData)
                    }
                    is NameConsistencyRule -> {
                        Log.d(TAG, "Executing NameConsistencyRule") 
                        rule.validate(extractedData.idCardData to extractedData.payslipData)
                    }
                    is RetirementAgeRule -> {
                        Log.d(TAG, "Executing RetirementAgeRule")
                        rule.validate(extractedData.idCardData)
                    }
                    is DataQualityRule -> {
                        Log.d(TAG, "Executing DataQualityRule")
                        rule.validate(extractedData)
                    }
                    is AffordabilityRule -> {
                        Log.d(TAG, "Executing AffordabilityRule")
                        val effectiveAmount = if (requestedLoanAmount > 0) {
                            requestedLoanAmount
                        } else {
                            // Calculate default amount based on affordability
                            calculateDefaultLoanAmount(extractedData.payslipData)
                        }
                        rule.validate(extractedData.payslipData to effectiveAmount)
                    }
                    else -> {
                        Log.w(TAG, "Unknown rule type: ${rule::class.simpleName}")
                        BusinessValidationResult.success()
                    }
                }
                
                validationResults.add(result)
                
                // Track failed and warning rules
                if (!result.isValid) {
                    failedRules.add(rule.name)
                    Log.w(TAG, "Rule ${rule.name} failed: ${result.errorMessage}")
                } else if (result.warningMessage != null) {
                    warningRules.add(rule.name)
                    Log.w(TAG, "Rule ${rule.name} warning: ${result.warningMessage}")
                } else {
                    Log.d(TAG, "Rule ${rule.name} passed")
                }
                
                // Early termination for critical failures (optional)
                if (!result.isValid && rule.priority <= 2) {
                    Log.d(TAG, "Critical rule ${rule.name} failed, considering early termination")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing rule ${rule.name}", e)
                val errorResult = BusinessValidationResult.failure("Rule execution error: ${e.message}")
                validationResults.add(errorResult)
                failedRules.add(rule.name)
            }
        }
        
        // Calculate overall results
        val isEligible = failedRules.isEmpty()
        val overallConfidence = if (validationResults.isNotEmpty()) {
            validationResults.map { it.confidence }.average().toFloat()
        } else {
            0.0f
        }
        
        Log.d(TAG, "Validation complete. Eligible: $isEligible, Failed rules: ${failedRules.size}, Confidence: $overallConfidence")
        
        return ApplicationValidationResult(
            isEligible = isEligible,
            validationResults = validationResults,
            failedRules = failedRules,
            warningRules = warningRules,
            overallConfidence = overallConfidence,
            applicationMetadata = mapOf(
                "total_rules_executed" to validationResults.size,
                "execution_timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Validates only specific aspects (useful for quick checks)
     */
    suspend fun validateDataQuality(extractedData: ExtractedApplicationData): BusinessValidationResult {
        if (!::policy.isInitialized) initialize()
        
        val dataQualityRule = rules.find { it is DataQualityRule } as? DataQualityRule
        return dataQualityRule?.validate(extractedData) ?: BusinessValidationResult.failure("Data quality rule not found")
    }
    
    /**
     * Validates only affordability (useful for loan amount calculations)
     */
    suspend fun validateAffordability(payslips: List<PayslipData>, requestedAmount: Double): BusinessValidationResult {
        if (!::policy.isInitialized) initialize()
        
        val affordabilityRule = rules.find { it is AffordabilityRule } as? AffordabilityRule
        return affordabilityRule?.validate(payslips to requestedAmount) ?: BusinessValidationResult.failure("Affordability rule not found")
    }
    
    /**
     * Gets current policy configuration
     */
    fun getPolicyConfiguration(): PolicyConfiguration {
        if (!::policy.isInitialized) initialize()
        return policy
    }
    
    /**
     * Calculates maximum affordable loan amount
     */
    fun calculateMaxAffordableLoan(payslips: List<PayslipData>): Double {
        if (payslips.isEmpty()) return 0.0
        
        val averageSalary = payslips.map { it.grossSalary }.average()
        val existingDeductions = calculator.extractDeductionsFromPayslips(payslips)
        
        return calculator.calculateMaxLoanAmount(
            grossSalary = averageSalary,
            existingDeductions = existingDeductions,
            maxDSR = policy.lendingPolicy.maxDSR * 100, // Convert to percentage
            interestRate = policy.lendingPolicy.defaultInterestRate,
            termMonths = policy.lendingPolicy.defaultTermMonths
        )
    }
    
    /**
     * Creates instances of all business rules with current policy
     */
    private fun createBusinessRules(): List<BusinessRule<*>> {
        val rules = mutableListOf<BusinessRule<*>>()
        
        // Create each rule with its configuration
        if (policy.validationRules.payslipRecency.enabled) {
            rules.add(PayslipRecencyRule(policy.validationRules.payslipRecency))
        }
        
        if (policy.validationRules.payslipCount.enabled) {
            rules.add(PayslipCountRule(policy.validationRules.payslipCount))
        }
        
        if (policy.validationRules.nameConsistency.enabled) {
            rules.add(NameConsistencyRule(policy.validationRules.nameConsistency))
        }
        
        if (policy.validationRules.retirementAge.enabled) {
            rules.add(RetirementAgeRule(policy.validationRules.retirementAge, policy.lendingPolicy))
        }
        
        if (policy.validationRules.dataQuality.enabled) {
            rules.add(DataQualityRule(policy.validationRules.dataQuality))
        }
        
        if (policy.validationRules.affordability.enabled) {
            rules.add(AffordabilityRule(policy.validationRules.affordability, calculator, policy.lendingPolicy))
        }
        
        Log.d(TAG, "Created ${rules.size} business rules")
        return rules
    }
    
    /**
     * Calculates a conservative default loan amount based on salary
     */
    private fun calculateDefaultLoanAmount(payslips: List<PayslipData>): Double {
        if (payslips.isEmpty()) return 0.0
        
        val maxAffordable = calculateMaxAffordableLoan(payslips)
        
        // Apply conservative ratio from policy
        return maxAffordable * policy.lendingPolicy.conservativeOfferRatio
    }
    
    /**
     * Provides detailed validation summary for debugging
     */
    fun getValidationSummary(result: ApplicationValidationResult): String {
        val summary = StringBuilder()
        summary.appendLine("=== LOAN APPLICATION VALIDATION SUMMARY ===")
        summary.appendLine("Overall Eligible: ${result.isEligible}")
        summary.appendLine("Overall Confidence: ${String.format("%.1f%%", result.overallConfidence * 100)}")
        summary.appendLine("Total Rules: ${result.validationResults.size}")
        summary.appendLine("Failed Rules: ${result.failedRules.size}")
        summary.appendLine("Warning Rules: ${result.warningRules.size}")
        summary.appendLine()
        
        if (result.failedRules.isNotEmpty()) {
            summary.appendLine("FAILED RULES:")
            result.getErrorMessages().forEach { error ->
                summary.appendLine("  ❌ $error")
            }
            summary.appendLine()
        }
        
        if (result.warningRules.isNotEmpty()) {
            summary.appendLine("WARNINGS:")
            result.getWarningMessages().forEach { warning ->
                summary.appendLine("  ⚠️ $warning")
            }
            summary.appendLine()
        }
        
        summary.appendLine("=== END VALIDATION SUMMARY ===")
        return summary.toString()
    }
} 