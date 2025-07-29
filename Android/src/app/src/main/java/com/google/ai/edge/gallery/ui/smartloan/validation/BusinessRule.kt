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

/**
 * Base interface for all business rules in the Smart Loan system
 */
interface BusinessRule<T> {
    /** Unique identifier for the rule */
    val name: String
    
    /** Human-readable description of what the rule validates */
    val description: String
    
    /** Priority for rule execution (1 = highest priority) */
    val priority: Int
    
    /** Whether this rule is enabled */
    val enabled: Boolean get() = true
    
    /**
     * Validates the provided data against this business rule
     * @param data The data to validate
     * @return BusinessValidationResult indicating success or failure
     */
    suspend fun validate(data: T): BusinessValidationResult
}

/**
 * Result of a business rule validation
 */
data class BusinessValidationResult(
    /** Whether the validation passed */
    val isValid: Boolean,
    
    /** Error message if validation failed */
    val errorMessage: String? = null,
    
    /** Warning message for non-critical issues */
    val warningMessage: String? = null,
    
    /** Confidence level of the validation (0.0 to 1.0) */
    val confidence: Float = 1.0f,
    
    /** Additional metadata about the validation */
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(confidence: Float = 1.0f, metadata: Map<String, Any> = emptyMap()) = 
            BusinessValidationResult(isValid = true, confidence = confidence, metadata = metadata)
            
        fun failure(errorMessage: String, confidence: Float = 1.0f, metadata: Map<String, Any> = emptyMap()) = 
            BusinessValidationResult(isValid = false, errorMessage = errorMessage, confidence = confidence, metadata = metadata)
            
        fun warning(warningMessage: String, confidence: Float = 1.0f, metadata: Map<String, Any> = emptyMap()) = 
            BusinessValidationResult(isValid = true, warningMessage = warningMessage, confidence = confidence, metadata = metadata)
    }
}

/**
 * Result of validating an entire loan application
 */
data class ApplicationValidationResult(
    /** Whether the application is eligible for loan approval */
    val isEligible: Boolean,
    
    /** Results from all individual rule validations */
    val validationResults: List<BusinessValidationResult>,
    
    /** Rules that failed validation */
    val failedRules: List<String> = emptyList(),
    
    /** Rules that generated warnings */
    val warningRules: List<String> = emptyList(),
    
    /** Overall confidence score (average of all rule confidences) */
    val overallConfidence: Float = 0.0f,
    
    /** Additional application-level metadata */
    val applicationMetadata: Map<String, Any> = emptyMap()
) {
    
    /**
     * Gets all error messages from failed validations
     */
    fun getErrorMessages(): List<String> {
        return validationResults.mapNotNull { it.errorMessage }
    }
    
    /**
     * Gets all warning messages from validations
     */
    fun getWarningMessages(): List<String> {
        return validationResults.mapNotNull { it.warningMessage }
    }
    
    /**
     * Checks if there are any warnings (but no failures)
     */
    fun hasWarnings(): Boolean {
        return warningRules.isNotEmpty() && isEligible
    }
} 