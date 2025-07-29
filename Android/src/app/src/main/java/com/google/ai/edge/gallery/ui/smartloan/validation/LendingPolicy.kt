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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Complete lending policy configuration loaded from JSON
 */
data class PolicyConfiguration(
    @SerializedName("lendingPolicy")
    val lendingPolicy: LendingPolicy,
    
    @SerializedName("validationRules")
    val validationRules: ValidationRuleConfig,
    
    @SerializedName("loanTerms")
    val loanTerms: LoanTermsConfig,
    
    @SerializedName("errorMessages")
    val errorMessages: Map<String, String>
)

/**
 * Core lending policy parameters
 */
data class LendingPolicy(
    @SerializedName("maxDSR")
    val maxDSR: Double,
    
    @SerializedName("minAge")
    val minAge: Int,
    
    @SerializedName("maxAge") 
    val maxAge: Int,
    
    @SerializedName("minSalary")
    val minSalary: Double,
    
    @SerializedName("maxLoanAmount")
    val maxLoanAmount: Double,
    
    @SerializedName("defaultInterestRate")
    val defaultInterestRate: Double,
    
    @SerializedName("defaultTermMonths")
    val defaultTermMonths: Int,
    
    @SerializedName("payslipRecencyMonths")
    val payslipRecencyMonths: Int,
    
    @SerializedName("minExtractionConfidence")
    val minExtractionConfidence: Double,
    
    @SerializedName("conservativeOfferRatio")
    val conservativeOfferRatio: Double
)

/**
 * Configuration for all validation rules
 */
data class ValidationRuleConfig(
    @SerializedName("payslipRecency")
    val payslipRecency: RuleConfig,
    
    @SerializedName("payslipCount") 
    val payslipCount: RuleConfig,
    
    @SerializedName("nameConsistency")
    val nameConsistency: RuleConfig,
    
    @SerializedName("retirementAge")
    val retirementAge: RuleConfig,
    
    @SerializedName("dataQuality")
    val dataQuality: RuleConfig,
    
    @SerializedName("affordability")
    val affordability: RuleConfig
)

/**
 * Individual rule configuration
 */
data class RuleConfig(
    @SerializedName("enabled")
    val enabled: Boolean,
    
    @SerializedName("errorMessage")
    val errorMessage: String,
    
    @SerializedName("priority")
    val priority: Int,
    
    // Rule-specific parameters
    @SerializedName("maxAgeMonths")
    val maxAgeMonths: Int? = null,
    
    @SerializedName("fuzzyMatchThreshold")
    val fuzzyMatchThreshold: Double? = null,
    
    @SerializedName("maxAge")
    val maxAge: Int? = null,
    
    @SerializedName("minConfidence")
    val minConfidence: Double? = null,
    
    @SerializedName("maxDSR")
    val maxDSR: Double? = null,
    
    @SerializedName("minSalary")
    val minSalary: Double? = null,
    
    @SerializedName("minPayslips")
    val minPayslips: Int? = null
)

/**
 * Loan terms and interest rate configuration
 */
data class LoanTermsConfig(
    @SerializedName("availableTerms")
    val availableTerms: List<Int>,
    
    @SerializedName("defaultTerm") 
    val defaultTerm: Int,
    
    @SerializedName("interestRates")
    val interestRates: Map<String, Double>
) {
    /**
     * Gets interest rate for a specific term
     */
    fun getInterestRate(termMonths: Int): Double {
        return interestRates[termMonths.toString()] ?: 0.15 // Default 15%
    }
}

/**
 * Utility to load policy from assets
 */
object PolicyLoader {
    private var cachedPolicy: PolicyConfiguration? = null
    
    /**
     * Loads policy configuration from assets/policy_rules.json
     */
    fun loadPolicy(context: Context): PolicyConfiguration {
        if (cachedPolicy == null) {
            val json = context.assets.open("policy_rules.json").bufferedReader().use { it.readText() }
            cachedPolicy = Gson().fromJson(json, PolicyConfiguration::class.java)
        }
        return cachedPolicy!!
    }
    
    /**
     * Clears cached policy (useful for testing)
     */
    fun clearCache() {
        cachedPolicy = null
    }
} 