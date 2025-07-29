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

package com.google.ai.edge.gallery.ui.smartloan.finance

import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import com.google.ai.edge.gallery.ui.smartloan.validation.ApplicationValidationResult
import com.google.ai.edge.gallery.ui.smartloan.validation.LendingPolicy
import com.google.ai.edge.gallery.ui.smartloan.validation.LoanTermsConfig
import com.google.ai.edge.gallery.ui.smartloan.validation.PolicyConfiguration

private const val TAG = "LoanOfferGenerator"

/**
 * Data class representing a loan offer
 */
data class LoanOffer(
    val maxLoanAmount: Double,
    val recommendedAmount: Double,
    val interestRate: Double,
    val loanTerm: Int, // in months
    val monthlyPayment: Double,
    val totalRepayment: Double,
    val totalInterest: Double,
    val processingFee: Double,
    val dsr: Double, // as percentage
    val offerValidUntil: Long, // timestamp
    val conditions: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
) {
    
    /**
     * Gets formatted DSR percentage
     */
    fun getFormattedDSR(): String = String.format("%.1f%%", dsr)
    
    /**
     * Gets formatted interest rate
     */
    fun getFormattedInterestRate(): String = String.format("%.1f%%", interestRate * 100)
    
    /**
     * Checks if offer is still valid
     */
    fun isValid(): Boolean = System.currentTimeMillis() < offerValidUntil
    
    /**
     * Gets time remaining for offer validity (in hours)
     */
    fun getValidityHours(): Long {
        val remainingMs = offerValidUntil - System.currentTimeMillis()
        return maxOf(0, remainingMs / (1000 * 60 * 60))
    }
}

/**
 * Generates loan offers based on validation results and policy
 */
class LoanOfferGenerator(
    private val calculator: AffordabilityCalculator,
    private val policy: PolicyConfiguration
) {
    
    /**
     * Generates a loan offer based on application data and validation results
     */
    fun generateOffer(
        extractedData: ExtractedApplicationData,
        validationResult: ApplicationValidationResult,
        requestedAmount: Double = 0.0,
        preferredTerm: Int = 0
    ): LoanOffer? {
        
        if (!validationResult.isEligible) {
            Log.d(TAG, "Cannot generate offer - application is not eligible")
            return null
        }
        
        val payslips = extractedData.payslipData
        if (payslips.isEmpty()) {
            Log.e(TAG, "Cannot generate offer - no payslip data available")
            return null
        }
        
        Log.d(TAG, "Generating loan offer for eligible application")
        Log.d(TAG, "  Requested amount: $requestedAmount")
        Log.d(TAG, "  Preferred term: $preferredTerm months")
        
        // Calculate maximum affordable amount
        val averageSalary = payslips.map { it.grossSalary }.average()
        val existingDeductions = calculator.extractDeductionsFromPayslips(payslips)
        
        val maxAffordableAmount = calculator.calculateMaxLoanAmount(
            grossSalary = averageSalary,
            existingDeductions = existingDeductions,
            maxDSR = policy.lendingPolicy.maxDSR * 100,
            interestRate = policy.lendingPolicy.defaultInterestRate,
            termMonths = policy.lendingPolicy.defaultTermMonths
        )
        
        // Apply policy maximum
        val policyMaxAmount = policy.lendingPolicy.maxLoanAmount
        val actualMaxAmount = minOf(maxAffordableAmount, policyMaxAmount)
        
        // Determine recommended amount
        val recommendedAmount = when {
            requestedAmount > 0 && requestedAmount <= actualMaxAmount -> requestedAmount
            requestedAmount > actualMaxAmount -> actualMaxAmount
            else -> actualMaxAmount * policy.lendingPolicy.conservativeOfferRatio
        }
        
        // Determine loan term
        val effectiveTerm = determineLoanTerm(preferredTerm, recommendedAmount, averageSalary)
        val effectiveInterestRate = policy.loanTerms.getInterestRate(effectiveTerm)
        
        // Calculate loan details
        val monthlyPayment = calculator.calculateMonthlyPayment(
            loanAmount = recommendedAmount,
            interestRate = effectiveInterestRate,
            termMonths = effectiveTerm
        )
        
        val totalRepayment = monthlyPayment * effectiveTerm
        val totalInterest = totalRepayment - recommendedAmount
        val processingFee = calculateProcessingFee(recommendedAmount)
        
        val dsr = calculator.calculateDSR(
            grossSalary = averageSalary,
            existingDeductions = existingDeductions,
            proposedLoanPayment = monthlyPayment
        )
        
        // Generate conditions and warnings
        val conditions = generateOfferConditions(extractedData, validationResult)
        val warnings = generateOfferWarnings(validationResult, dsr)
        
        // Offer validity (48 hours from now)
        val validUntil = System.currentTimeMillis() + (48 * 60 * 60 * 1000)
        
        val offer = LoanOffer(
            maxLoanAmount = actualMaxAmount,
            recommendedAmount = recommendedAmount,
            interestRate = effectiveInterestRate,
            loanTerm = effectiveTerm,
            monthlyPayment = monthlyPayment,
            totalRepayment = totalRepayment,
            totalInterest = totalInterest,
            processingFee = processingFee,
            dsr = dsr,
            offerValidUntil = validUntil,
            conditions = conditions,
            warnings = warnings,
            metadata = mapOf(
                "average_salary" to averageSalary,
                "max_affordable" to maxAffordableAmount,
                "policy_max" to policyMaxAmount,
                "validation_confidence" to validationResult.overallConfidence,
                "generation_timestamp" to System.currentTimeMillis()
            )
        )
        
        Log.d(TAG, "Generated loan offer:")
        Log.d(TAG, "  Max Amount: ${offer.maxLoanAmount}")
        Log.d(TAG, "  Recommended: ${offer.recommendedAmount}")
        Log.d(TAG, "  Interest Rate: ${offer.getFormattedInterestRate()}")
        Log.d(TAG, "  Term: ${offer.loanTerm} months")
        Log.d(TAG, "  Monthly Payment: ${offer.monthlyPayment}")
        Log.d(TAG, "  DSR: ${offer.getFormattedDSR()}")
        
        return offer
    }
    
    /**
     * Generates multiple offer options with different terms
     */
    fun generateOfferOptions(
        extractedData: ExtractedApplicationData,
        validationResult: ApplicationValidationResult,
        requestedAmount: Double = 0.0
    ): List<LoanOffer> {
        
        if (!validationResult.isEligible) return emptyList()
        
        val offers = mutableListOf<LoanOffer>()
        
        // Generate offers for each available term
        policy.loanTerms.availableTerms.forEach { term ->
            try {
                val offer = generateOffer(
                    extractedData = extractedData,
                    validationResult = validationResult,
                    requestedAmount = requestedAmount,
                    preferredTerm = term
                )
                if (offer != null) {
                    offers.add(offer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating offer for $term months", e)
            }
        }
        
        Log.d(TAG, "Generated ${offers.size} loan offer options")
        return offers.sortedBy { it.loanTerm }
    }
    
    /**
     * Recalculates offer for a specific loan amount
     */
    fun recalculateOfferForAmount(
        baseOffer: LoanOffer,
        newAmount: Double,
        payslips: List<PayslipData>
    ): LoanOffer? {
        
        if (newAmount <= 0 || newAmount > baseOffer.maxLoanAmount) {
            Log.w(TAG, "Invalid amount for recalculation: $newAmount (max: ${baseOffer.maxLoanAmount})")
            return null
        }
        
        val averageSalary = payslips.map { it.grossSalary }.average()
        val existingDeductions = calculator.extractDeductionsFromPayslips(payslips)
        
        val monthlyPayment = calculator.calculateMonthlyPayment(
            loanAmount = newAmount,
            interestRate = baseOffer.interestRate,
            termMonths = baseOffer.loanTerm
        )
        
        val totalRepayment = monthlyPayment * baseOffer.loanTerm
        val totalInterest = totalRepayment - newAmount
        val processingFee = calculateProcessingFee(newAmount)
        
        val dsr = calculator.calculateDSR(
            grossSalary = averageSalary,
            existingDeductions = existingDeductions,
            proposedLoanPayment = monthlyPayment
        )
        
        return baseOffer.copy(
            recommendedAmount = newAmount,
            monthlyPayment = monthlyPayment,
            totalRepayment = totalRepayment,
            totalInterest = totalInterest,
            processingFee = processingFee,
            dsr = dsr,
            metadata = baseOffer.metadata + mapOf(
                "recalculated_at" to System.currentTimeMillis(),
                "original_amount" to baseOffer.recommendedAmount
            )
        )
    }
    
    /**
     * Determines optimal loan term based on amount and salary
     */
    private fun determineLoanTerm(preferredTerm: Int, loanAmount: Double, salary: Double): Int {
        // Use preferred term if valid
        if (preferredTerm > 0 && policy.loanTerms.availableTerms.contains(preferredTerm)) {
            return preferredTerm
        }
        
        // Otherwise use policy default
        return policy.loanTerms.defaultTerm
    }
    
    /**
     * Calculates processing fee based on loan amount
     */
    private fun calculateProcessingFee(loanAmount: Double): Double {
        // Simple processing fee: 2% of loan amount, minimum 1000, maximum 5000
        val feeRate = 0.02
        val calculatedFee = loanAmount * feeRate
        return when {
            calculatedFee < 1000 -> 1000.0
            calculatedFee > 5000 -> 5000.0
            else -> calculatedFee
        }
    }
    
    /**
     * Generates offer conditions based on validation results
     */
    private fun generateOfferConditions(
        extractedData: ExtractedApplicationData,
        validationResult: ApplicationValidationResult
    ): List<String> {
        val conditions = mutableListOf<String>()
        
        // Standard conditions
        conditions.add("This offer is valid for 48 hours from generation time")
        conditions.add("Final approval subject to document verification")
        conditions.add("Loan disbursement will be made to your registered bank account")
        conditions.add("Early repayment is allowed without penalties")
        
        // Conditional conditions based on validation
        if (validationResult.overallConfidence < 0.95f) {
            conditions.add("Additional document verification may be required due to extraction confidence")
        }
        
        if (validationResult.hasWarnings()) {
            conditions.add("Offer subject to resolution of data quality warnings")
        }
        
        return conditions
    }
    
    /**
     * Generates warnings based on validation results and DSR
     */
    private fun generateOfferWarnings(
        validationResult: ApplicationValidationResult,
        dsr: Double
    ): List<String> {
        val warnings = mutableListOf<String>()
        
        // High DSR warning
        if (dsr > 40.0) {
            warnings.add("Your debt service ratio is ${String.format("%.1f%%", dsr)}, which is relatively high")
        }
        
        // Low confidence warning
        if (validationResult.overallConfidence < 0.9f) {
            warnings.add("Document extraction confidence is below optimal levels")
        }
        
        // Add validation warnings
        warnings.addAll(validationResult.getWarningMessages())
        
        return warnings
    }
} 