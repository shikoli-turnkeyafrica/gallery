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

import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import com.google.ai.edge.gallery.ui.smartloan.finance.AffordabilityCalculator
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val TAG = "CoreBusinessRules"

/**
 * Rule 1: Validates that all payslips are recent (within policy timeframe)
 */
class PayslipRecencyRule(
    private val config: RuleConfig
) : BusinessRule<List<PayslipData>> {
    
    override val name = "PayslipRecency"
    override val description = "Validates payslips are within the required timeframe"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: List<PayslipData>): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val maxAgeMonths = config.maxAgeMonths ?: 3
        val cutoffDate = LocalDate.now().minusMonths(maxAgeMonths.toLong())
        
        Log.d(TAG, "Validating payslip recency against cutoff: $cutoffDate")
        
        val oldPayslips = mutableListOf<String>()
        
        data.forEach { payslip ->
            if (payslip.payPeriod.isNotBlank()) {
                try {
                    // Parse YYYY-MM format
                    val payslipDate = LocalDate.parse("${payslip.payPeriod}-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    
                    if (payslipDate.isBefore(cutoffDate)) {
                        oldPayslips.add(payslip.payPeriod)
                        Log.w(TAG, "Payslip ${payslip.payPeriod} is older than $maxAgeMonths months")
                    }
                } catch (e: DateTimeParseException) {
                    Log.w(TAG, "Could not parse payslip date: ${payslip.payPeriod}")
                    oldPayslips.add(payslip.payPeriod) // Treat unparseable dates as invalid
                }
            }
        }
        
        return if (oldPayslips.isEmpty()) {
            BusinessValidationResult.success(
                confidence = 1.0f,
                metadata = mapOf("validated_payslips" to data.size)
            )
        } else {
            BusinessValidationResult.failure(
                errorMessage = "${config.errorMessage}. Found payslips from: ${oldPayslips.joinToString(", ")}",
                metadata = mapOf("old_payslips" to oldPayslips)
            )
        }
    }
}

/**
 * Rule 1.5: Validates that sufficient payslips are provided for accurate income assessment
 */
class PayslipCountRule(
    private val config: RuleConfig
) : BusinessRule<List<PayslipData>> {
    
    override val name = "PayslipCount"
    override val description = "Validates sufficient payslips for reliable income assessment"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: List<PayslipData>): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val minRequired = config.minPayslips ?: 3 // Default to 3 months for reliable assessment
        val validPayslips = data.filter { it.isValid }
        
        Log.d(TAG, "Validating payslip count: ${validPayslips.size} valid payslips, minimum required: $minRequired")
        
        return if (validPayslips.size >= minRequired) {
            BusinessValidationResult.success(
                confidence = 1.0f,
                metadata = mapOf(
                    "payslip_count" to validPayslips.size,
                    "required_count" to minRequired,
                    "payslip_periods" to validPayslips.map { it.payPeriod }
                )
            )
        } else {
            BusinessValidationResult.failure(
                errorMessage = "${config.errorMessage}. Provided: ${validPayslips.size} payslips, Required: $minRequired for accurate income assessment",
                metadata = mapOf(
                    "payslip_count" to validPayslips.size,
                    "required_count" to minRequired,
                    "missing_count" to (minRequired - validPayslips.size)
                )
            )
        }
    }
}

/**
 * Rule 2: Validates name consistency between ID and payslips
 */
class NameConsistencyRule(
    private val config: RuleConfig
) : BusinessRule<Pair<IdCardData, List<PayslipData>>> {
    
    override val name = "NameConsistency"
    override val description = "Validates ID name matches payslip names"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: Pair<IdCardData, List<PayslipData>>): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val (idData, payslips) = data
        val threshold = config.fuzzyMatchThreshold ?: 0.8
        
        val idName = idData.fullName.trim()
        if (idName.isBlank()) {
            return BusinessValidationResult.failure("ID name is missing or empty")
        }
        
        Log.d(TAG, "Validating name consistency for ID name: '$idName'")
        
        val validPayslips = payslips.filter { it.employeeName.isNotBlank() }
        if (validPayslips.isEmpty()) {
            return BusinessValidationResult.failure("No valid employee names found in payslips")
        }
        
        val mismatches = mutableListOf<String>()
        val similarities = mutableListOf<Double>()
        
        validPayslips.forEach { payslip ->
            val similarity = calculateNameSimilarity(idName, payslip.employeeName)
            similarities.add(similarity)
            
            Log.d(TAG, "Name similarity: '$idName' vs '${payslip.employeeName}' = $similarity")
            
            if (similarity < threshold) {
                mismatches.add(payslip.employeeName)
            }
        }
        
        val averageSimilarity = similarities.average()
        
        return if (mismatches.isEmpty()) {
            BusinessValidationResult.success(
                confidence = averageSimilarity.toFloat(),
                metadata = mapOf(
                    "average_similarity" to averageSimilarity,
                    "validated_names" to validPayslips.size
                )
            )
        } else {
            BusinessValidationResult.failure(
                errorMessage = "${config.errorMessage}. ID: '$idName', Payslips: ${mismatches.joinToString(", ")}",
                confidence = averageSimilarity.toFloat(),
                metadata = mapOf(
                    "mismatched_names" to mismatches,
                    "similarity_scores" to similarities
                )
            )
        }
    }
    
    /**
     * Calculates name similarity using Jaro-Winkler-like algorithm
     */
    private fun calculateNameSimilarity(name1: String, name2: String): Double {
        val n1 = normalizeNameForComparison(name1)
        val n2 = normalizeNameForComparison(name2)
        
        if (n1 == n2) return 1.0
        
        // Simple similarity based on common words
        val words1 = n1.split(" ").filter { it.length > 1 }
        val words2 = n2.split(" ").filter { it.length > 1 }
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val commonWords = words1.intersect(words2.toSet()).size
        val totalWords = (words1.size + words2.size).toDouble()
        
        return (2.0 * commonWords) / totalWords
    }
    
    /**
     * Normalizes name for comparison (uppercase, remove extra spaces, etc.)
     */
    private fun normalizeNameForComparison(name: String): String {
        return name.uppercase()
            .replace(Regex("[^A-Z\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Rule 3: Validates applicant age and retirement constraints
 */
class RetirementAgeRule(
    private val config: RuleConfig,
    private val lendingPolicy: LendingPolicy
) : BusinessRule<IdCardData> {
    
    override val name = "RetirementAge"
    override val description = "Validates applicant age is within lending limits"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: IdCardData): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val dateOfBirth = data.dateOfBirth
        if (dateOfBirth.isBlank()) {
            return BusinessValidationResult.failure("Date of birth is missing from ID")
        }
        
        try {
            val age = calculateAge(dateOfBirth)
            val maxAge = config.maxAge ?: lendingPolicy.maxAge
            val maxAgeAtLoanEnd = age + (lendingPolicy.defaultTermMonths / 12)
            
            Log.d(TAG, "Age validation: current=$age, maxAllowed=$maxAge, ageAtLoanEnd=$maxAgeAtLoanEnd")
            
            return when {
                age < lendingPolicy.minAge -> {
                    BusinessValidationResult.failure(
                        "Applicant age ($age) is below minimum age (${lendingPolicy.minAge})",
                        metadata = mapOf("current_age" to age, "min_age" to lendingPolicy.minAge)
                    )
                }
                maxAgeAtLoanEnd > maxAge -> {
                    BusinessValidationResult.failure(
                        "${config.errorMessage}. Current age: $age, Age at loan completion: $maxAgeAtLoanEnd",
                        metadata = mapOf(
                            "current_age" to age,
                            "age_at_loan_end" to maxAgeAtLoanEnd,
                            "max_allowed_age" to maxAge
                        )
                    )
                }
                else -> {
                    BusinessValidationResult.success(
                        confidence = 1.0f,
                        metadata = mapOf("age" to age, "age_at_loan_end" to maxAgeAtLoanEnd)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date of birth: $dateOfBirth", e)
            return BusinessValidationResult.failure("Invalid date of birth format: $dateOfBirth")
        }
    }
    
    /**
     * Calculates age from date of birth string
     */
    private fun calculateAge(dateOfBirth: String): Int {
        // Try multiple date formats
        val formats = listOf(
            "dd.MM.yyyy",
            "dd/MM/yyyy", 
            "yyyy-MM-dd",
            "dd-MM-yyyy"
        )
        
        for (format in formats) {
            try {
                val dob = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern(format))
                return Period.between(dob, LocalDate.now()).years
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        
        throw IllegalArgumentException("Unable to parse date: $dateOfBirth")
    }
}

/**
 * Rule 4: Validates data extraction quality
 */
class DataQualityRule(
    private val config: RuleConfig
) : BusinessRule<ExtractedApplicationData> {
    
    override val name = "DataQuality"
    override val description = "Validates extraction confidence meets minimum requirements"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: ExtractedApplicationData): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val minConfidence = config.minConfidence ?: 0.85
        val overallConfidence = data.overallConfidence
        
        Log.d(TAG, "Data quality validation: confidence=$overallConfidence, minimum=$minConfidence")
        
        val lowConfidenceFields = mutableListOf<String>()
        
        // Check ID data confidence
        if (data.idCardData.extractionConfidence < minConfidence) {
            lowConfidenceFields.add("ID Card (${String.format("%.1f%%", data.idCardData.extractionConfidence * 100)})")
        }
        
        // Check payslip confidences
        data.payslipData.forEachIndexed { index, payslip ->
            if (payslip.extractionConfidence < minConfidence) {
                lowConfidenceFields.add("Payslip ${index + 1} (${String.format("%.1f%%", payslip.extractionConfidence * 100)})")
            }
        }
        
        return if (overallConfidence >= minConfidence) {
            BusinessValidationResult.success(
                confidence = overallConfidence,
                metadata = mapOf("overall_confidence" to overallConfidence)
            )
        } else {
            BusinessValidationResult.failure(
                "${config.errorMessage}. Overall confidence: ${String.format("%.1f%%", overallConfidence * 100)}, Required: ${String.format("%.1f%%", minConfidence * 100)}",
                confidence = overallConfidence,
                metadata = mapOf(
                    "low_confidence_fields" to lowConfidenceFields,
                    "overall_confidence" to overallConfidence,
                    "required_confidence" to minConfidence
                )
            )
        }
    }
}

/**
 * Rule 5: Validates affordability and DSR constraints
 */
class AffordabilityRule(
    private val config: RuleConfig,
    private val calculator: AffordabilityCalculator,
    private val lendingPolicy: LendingPolicy
) : BusinessRule<Pair<List<PayslipData>, Double>> {
    
    override val name = "Affordability"
    override val description = "Validates income sufficiency and DSR limits"
    override val priority = config.priority
    override val enabled = config.enabled
    
    override suspend fun validate(data: Pair<List<PayslipData>, Double>): BusinessValidationResult {
        if (!enabled) return BusinessValidationResult.success()
        
        val (payslips, requestedAmount) = data
        val minSalary = config.minSalary ?: lendingPolicy.minSalary
        val maxDSR = config.maxDSR ?: lendingPolicy.maxDSR
        
        if (payslips.isEmpty()) {
            return BusinessValidationResult.failure("No payslip data available for affordability assessment")
        }
        
        val averageGrossSalary = payslips.map { it.grossSalary }.average()
        
        Log.d(TAG, "Affordability validation:")
        Log.d(TAG, "  Average salary: $averageGrossSalary")
        Log.d(TAG, "  Minimum salary: $minSalary")
        Log.d(TAG, "  Requested amount: $requestedAmount")
        
        // Check minimum salary requirement
        if (averageGrossSalary < minSalary) {
            return BusinessValidationResult.failure(
                "Average salary (${String.format("%.0f", averageGrossSalary)}) is below minimum requirement (${String.format("%.0f", minSalary)})",
                metadata = mapOf(
                    "average_salary" to averageGrossSalary,
                    "min_salary" to minSalary
                )
            )
        }
        
        // Calculate affordability
        val existingDeductions = calculator.extractDeductionsFromPayslips(payslips)
        val affordabilityResult = calculator.validateAffordability(
            grossSalary = averageGrossSalary,
            existingDeductions = existingDeductions,
            proposedLoanAmount = requestedAmount,
            interestRate = lendingPolicy.defaultInterestRate,
            termMonths = lendingPolicy.defaultTermMonths,
            maxDSR = maxDSR * 100 // Convert to percentage
        )
        
        return if (affordabilityResult.isAffordable) {
            BusinessValidationResult.success(
                confidence = 1.0f,
                metadata = mapOf(
                    "dsr" to affordabilityResult.dsr,
                    "monthly_payment" to affordabilityResult.monthlyPayment,
                    "max_affordable" to affordabilityResult.maxAffordableAmount
                )
            )
        } else {
            BusinessValidationResult.failure(
                "${config.errorMessage}. DSR: ${affordabilityResult.getDSRPercentage()}, Max allowed: ${String.format("%.1f%%", maxDSR * 100)}",
                metadata = mapOf(
                    "dsr" to affordabilityResult.dsr,
                    "max_dsr" to maxDSR * 100,
                    "monthly_payment" to affordabilityResult.monthlyPayment,
                    "max_affordable" to affordabilityResult.maxAffordableAmount,
                    "excess_amount" to affordabilityResult.excessAmount
                )
            )
        }
    }
} 