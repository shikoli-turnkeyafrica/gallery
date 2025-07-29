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

package com.google.ai.edge.gallery.ui.smartloan.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Data class representing extracted information from Kenyan National ID card
 */
@Serializable
data class IdCardData(
    val fullName: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "", // YYYY-MM-DD format
    val expiryDate: String = "", // YYYY-MM-DD format
    val placeOfBirth: String = "",
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false,
    val extractionTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Validates the extracted ID card data
     */
    fun validate(): IdCardData {
        val isNameValid = fullName.isNotBlank() && fullName.length >= 2
        val isIdNumberValid = idNumber.matches(Regex("\\d{8}")) // 8-digit Kenyan ID format
        val isDateValid = isValidDate(dateOfBirth)
        val hasMinimumData = isNameValid && isIdNumberValid
        
        return copy(
            isValid = hasMinimumData && extractionConfidence > 0.7f
        )
    }
    
    /**
     * Gets the age from date of birth
     */
    fun getAge(): Int? {
        return try {
            val birthDate = LocalDate.parse(dateOfBirth, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            today.year - birthDate.year - if (today.dayOfYear < birthDate.dayOfYear) 1 else 0
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    /**
     * Checks if ID is expired
     */
    fun isExpired(): Boolean {
        return try {
            if (expiryDate.isBlank()) return false
            val expiry = LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE)
            expiry.isBefore(LocalDate.now())
        } catch (e: DateTimeParseException) {
            false
        }
    }
    
    private fun isValidDate(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}

/**
 * Data class representing extracted information from payslip
 */
@Serializable
data class PayslipData(
    val employeeName: String = "",
    val employerName: String = "",
    val grossSalary: Double = 0.0,
    val netSalary: Double = 0.0,
    val payPeriod: String = "", // "2024-01" format (YYYY-MM)
    val deductions: Map<String, Double> = emptyMap(),
    val allowances: Map<String, Double> = emptyMap(),
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false,
    val extractionTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Validates the extracted payslip data
     */
    fun validate(): PayslipData {
        val isNameValid = employeeName.isNotBlank()
        val isEmployerValid = employerName.isNotBlank()
        val isSalaryValid = grossSalary > 0 && netSalary > 0 && netSalary <= grossSalary
        val isPeriodValid = isValidPayPeriod(payPeriod)
        val hasMinimumData = isNameValid && isEmployerValid && isSalaryValid
        
        return copy(
            isValid = hasMinimumData && extractionConfidence > 0.7f && isPeriodValid
        )
    }
    
    /**
     * Gets total deductions amount
     */
    fun getTotalDeductions(): Double {
        return deductions.values.sum()
    }
    
    /**
     * Gets total allowances amount
     */
    fun getTotalAllowances(): Double {
        return allowances.values.sum()
    }
    
    /**
     * Calculates the expected net salary based on gross salary, deductions, and allowances
     */
    fun getCalculatedNetSalary(): Double {
        return grossSalary + getTotalAllowances() - getTotalDeductions()
    }
    
    /**
     * Checks if the extracted net salary matches the calculated one (within tolerance)
     */
    fun isNetSalaryConsistent(tolerance: Double = 1000.0): Boolean {
        val calculated = getCalculatedNetSalary()
        return kotlin.math.abs(netSalary - calculated) <= tolerance
    }
    
    /**
     * Gets the formatted pay period for display
     */
    fun getFormattedPayPeriod(): String {
        return try {
            val parts = payPeriod.split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val month = parts[1].toInt()
                val monthNames = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                "${monthNames[month - 1]} $year"
            } else {
                payPeriod
            }
        } catch (e: Exception) {
            payPeriod
        }
    }
    
    private fun isValidPayPeriod(period: String): Boolean {
        return try {
            val parts = period.split("-")
            if (parts.size != 2) return false
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            year in 2020..LocalDate.now().year + 1 && month in 1..12
        } catch (e: NumberFormatException) {
            false
        }
    }
}

/**
 * Data class representing extracted information from loan application form
 */
@Serializable
data class LoanApplicationFormData(
    // Applicant Details Section
    val title: String = "", // Ms., Mr., Mrs., Dr., etc.
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val idPassportNumber: String = "",
    val isMarried: Boolean? = null,
    val numberOfDependents: Int = 0,
    val dateOfBirth: String = "", // DD-MMM-YYYY format
    val personalPOBox: String = "",
    val postalCode: String = "",
    val townCity: String = "",
    val residentialAddress: String = "",
    val upcountryAddress: String = "",
    val nearestSchoolChurch: String = "",
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val branch: String = "",
    val telephoneMobile: String = "",
    val emailAddress: String = "",
    
    // References/Colleague Details
    val references: List<ReferenceDetail> = emptyList(),
    
    // Loan Request Section
    val requestedLoanAmount: Double = 0.0,
    val requestedInstallmentAmount: Double = 0.0,
    val requestedLoanPeriodMonths: Int = 0,
    val disbursementMode: String = "", // M-PESA, TT, RTGS
    val clientMPesaNumber: String = "",
    
    // Official Use Section
    val approvedLoanAmount: Double = 0.0,
    val approvedInstallment: Double = 0.0,
    val approvedMonths: Int = 0,
    val loanOfficerName: String = "",
    val loanOfficerTelRef: String = "",
    val teamLeaderName: String = "",
    val processingDate: String = "",
    
    // Terms and Conditions Data (from back page)
    val interestRate: Double = 0.0,
    val hasAcceptedTerms: Boolean = false,
    val applicantSignatureDate: String = "",
    val witnessName: String = "",
    val witnessSignatureDate: String = "",
    
    // Metadata
    val extractionConfidence: Float = 0.0f,
    val isValid: Boolean = false,
    val extractionTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Reference detail data class for colleague/reference information
     */
    @Serializable
    data class ReferenceDetail(
        val firstName: String = "",
        val middleName: String = "",
        val lastName: String = "",
        val mobileNumber: String = ""
    )
    
    /**
     * Validates the extracted loan application form data
     */
    fun validate(): LoanApplicationFormData {
        val hasRequiredApplicantInfo = firstName.isNotBlank() && 
                                      lastName.isNotBlank() && 
                                      idPassportNumber.isNotBlank()
        val hasValidLoanRequest = requestedLoanAmount > 0.0 && 
                                 requestedLoanPeriodMonths > 0
        val hasContactInfo = telephoneMobile.isNotBlank() || emailAddress.isNotBlank()
        val hasMinimumData = hasRequiredApplicantInfo && hasValidLoanRequest && hasContactInfo
        
        return copy(
            isValid = hasMinimumData && extractionConfidence > 0.7f
        )
    }
    
    /**
     * Gets the full name of the applicant
     */
    fun getFullName(): String {
        return listOfNotNull(
            title.takeIf { it.isNotBlank() },
            firstName.takeIf { it.isNotBlank() },
            middleName.takeIf { it.isNotBlank() },
            lastName.takeIf { it.isNotBlank() }
        ).joinToString(" ")
    }
    
    /**
     * Gets the complete address
     */
    fun getCompleteAddress(): String {
        return listOfNotNull(
            residentialAddress.takeIf { it.isNotBlank() },
            townCity.takeIf { it.isNotBlank() },
            postalCode.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }
    
    /**
     * Validates loan request amounts
     */
    fun isLoanRequestValid(): Boolean {
        return requestedLoanAmount > 0.0 && 
               requestedInstallmentAmount > 0.0 && 
               requestedLoanPeriodMonths > 0 &&
               requestedInstallmentAmount * requestedLoanPeriodMonths >= requestedLoanAmount * 0.8 // Basic check
    }
    
    /**
     * Checks if the form has been officially approved
     */
    fun isOfficiallyApproved(): Boolean {
        return approvedLoanAmount > 0.0 && 
               loanOfficerName.isNotBlank() && 
               processingDate.isNotBlank()
    }
}

/**
 * Aggregated extracted data for Smart Loan application
 */
@Serializable
data class ExtractedApplicationData(
    val idCardData: IdCardData = IdCardData(),
    val payslipData: List<PayslipData> = emptyList(),
    val loanApplicationFormData: LoanApplicationFormData = LoanApplicationFormData(),
    val extractionStartTime: Long = System.currentTimeMillis(),
    val extractionEndTime: Long = 0L,
    val overallConfidence: Float = 0.0f,
    val isComplete: Boolean = false
) {
    /**
     * Calculates overall confidence based on ID and payslip data
     */
    fun calculateOverallConfidence(): ExtractedApplicationData {
        val idConfidence = if (idCardData.isValid) idCardData.extractionConfidence else 0.0f
        val payslipConfidences = payslipData.filter { it.isValid }.map { it.extractionConfidence }
        
        val totalConfidence = if (payslipConfidences.isNotEmpty()) {
            (idConfidence + payslipConfidences.average().toFloat()) / 2.0f
        } else {
            idConfidence
        }
        
        return copy(
            overallConfidence = totalConfidence,
            isComplete = idCardData.isValid && payslipData.any { it.isValid }
        )
    }
    
    /**
     * Gets the average monthly income from valid payslips
     */
    fun getAverageMonthlyIncome(): Double {
        val validPayslips = payslipData.filter { it.isValid && it.netSalary > 0 }
        return if (validPayslips.isNotEmpty()) {
            validPayslips.map { it.netSalary }.average()
        } else {
            0.0
        }
    }
    
    /**
     * Gets the most recent payslip data
     */
    fun getMostRecentPayslip(): PayslipData? {
        return payslipData
            .filter { it.isValid }
            .maxByOrNull { it.payPeriod }
    }
    
    /**
     * Checks if the applicant name matches between ID and payslips
     */
    fun isNameConsistent(): Boolean {
        if (!idCardData.isValid || payslipData.isEmpty()) return true
        
        val idName = idCardData.fullName.lowercase().trim()
        return payslipData.any { payslip ->
            if (!payslip.isValid) return@any false
            val payslipName = payslip.employeeName.lowercase().trim()
            
            // Check if names have significant overlap
            val idWords = idName.split(" ").filter { it.length > 2 }
            val payslipWords = payslipName.split(" ").filter { it.length > 2 }
            
            val matchingWords = idWords.intersect(payslipWords.toSet()).size
            matchingWords >= kotlin.math.min(2, kotlin.math.min(idWords.size, payslipWords.size))
        }
    }
    
    /**
     * Gets validation issues for manual review
     */
    fun getValidationIssues(): List<String> {
        val issues = mutableListOf<String>()
        
        if (!idCardData.isValid) {
            issues.add("ID card data incomplete or low confidence")
        }
        
        if (idCardData.isExpired()) {
            issues.add("ID card appears to be expired")
        }
        
        if (payslipData.isEmpty() || payslipData.none { it.isValid }) {
            issues.add("No valid payslip data extracted")
        }
        
        if (!isNameConsistent()) {
            issues.add("Name mismatch between ID and payslip")
        }
        
        payslipData.forEachIndexed { index, payslip ->
            if (payslip.isValid && !payslip.isNetSalaryConsistent()) {
                issues.add("Payslip ${index + 1}: Net salary calculation doesn't match")
            }
        }
        
        if (overallConfidence < 0.8f) {
            issues.add("Overall extraction confidence is low (${(overallConfidence * 100).toInt()}%)")
        }
        
        return issues
    }
} 