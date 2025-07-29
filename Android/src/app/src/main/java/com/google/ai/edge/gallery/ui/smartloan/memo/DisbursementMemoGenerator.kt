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

package com.google.ai.edge.gallery.ui.smartloan.memo

import com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData
import com.google.ai.edge.gallery.ui.smartloan.finance.LoanOffer
import com.google.ai.edge.gallery.ui.smartloan.validation.ApplicationValidationResult
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data structure for complete disbursement memo
 */
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

/**
 * Applicant personal information
 */
data class ApplicantInfo(
    val fullName: String,
    val idNumber: String,
    val dateOfBirth: String,
    val employerName: String,
    val grossSalary: Double,
    val netSalary: Double,
    val phoneNumber: String = "",
    val address: String = ""
)

/**
 * Loan terms and financial details
 */
data class LoanDetails(
    val approvedAmount: Double,
    val interestRate: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val totalRepayment: Double,
    val dsr: Double,
    val processingFee: Double = 0.0,
    val offerValidUntil: Long
)

/**
 * Document and data verification status
 */
data class VerificationInfo(
    val idVerified: Boolean,
    val salaryVerified: Boolean,
    val extractionConfidenceAverage: Float,
    val businessRulesPass: Boolean,
    val verificationTimestamp: Long,
    val documentsReceived: List<String> = listOf("ID_FRONT", "ID_BACK", "PAYSLIP")
)

/**
 * Risk assessment and scoring
 */
data class RiskAssessment(
    val overallRiskScore: Float,
    val confidenceLevel: Float,
    val riskFactors: List<String>,
    val mitigatingFactors: List<String>,
    val recommendedAction: String
)

/**
 * Approval workflow information
 */
data class ApprovalInfo(
    val approvedBy: String = "AI_SYSTEM",
    val approvalTimestamp: Long,
    val biometricConfirmed: Boolean = false,
    val termsAccepted: Boolean = false,
    val approvalNotes: String = "",
    val nextSteps: List<String> = listOf(
        "Verify bank account details",
        "Process disbursement",
        "Send confirmation SMS",
        "Schedule first payment reminder"
    )
)

/**
 * Generates comprehensive disbursement memos for approved loan applications
 */
class DisbursementMemoGenerator {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Generate complete disbursement memo from application data
     */
    fun generateMemo(
        applicationId: String,
        extractedData: ExtractedApplicationData,
        validationResult: ApplicationValidationResult,
        loanOffer: LoanOffer,
        biometricConfirmed: Boolean = false,
        termsAccepted: Boolean = false
    ): DisbursementMemo {
        val timestamp = System.currentTimeMillis()
        val payslipData = extractedData.payslipData.firstOrNull()
        
        return DisbursementMemo(
            applicationId = applicationId,
            timestamp = timestamp,
            applicantInfo = ApplicantInfo(
                fullName = extractedData.idCardData.fullName,
                idNumber = extractedData.idCardData.idNumber,
                dateOfBirth = extractedData.idCardData.dateOfBirth,
                employerName = payslipData?.employerName ?: "Unknown",
                grossSalary = payslipData?.grossSalary ?: 0.0,
                netSalary = payslipData?.netSalary ?: 0.0
            ),
            loanDetails = LoanDetails(
                approvedAmount = loanOffer.recommendedAmount,
                interestRate = loanOffer.interestRate,
                termMonths = loanOffer.loanTerm,
                monthlyPayment = loanOffer.monthlyPayment,
                totalRepayment = loanOffer.totalRepayment,
                dsr = loanOffer.dsr,
                processingFee = calculateProcessingFee(loanOffer.recommendedAmount),
                offerValidUntil = loanOffer.offerValidUntil
            ),
            verification = VerificationInfo(
                idVerified = extractedData.idCardData.isValid,
                salaryVerified = payslipData?.isValid ?: false,
                extractionConfidenceAverage = extractedData.overallConfidence,
                businessRulesPass = validationResult.isEligible,
                verificationTimestamp = timestamp
            ),
            riskAssessment = generateRiskAssessment(validationResult, extractedData),
            approvalInfo = ApprovalInfo(
                approvalTimestamp = timestamp,
                biometricConfirmed = biometricConfirmed,
                termsAccepted = termsAccepted,
                approvalNotes = generateApprovalNotes(validationResult, loanOffer)
            )
        )
    }
    
    /**
     * Generate application ID in format: SL{YYYYMMDDHHMMSS}{RAND}
     */
    fun generateApplicationId(): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "SL$timestamp$random"
    }
    
    /**
     * Calculate processing fee (typically 2% of loan amount, capped at KSh 5000)
     */
    private fun calculateProcessingFee(loanAmount: Double): Double {
        val feeRate = 0.02 // 2%
        val calculatedFee = loanAmount * feeRate
        val maxFee = 5000.0
        return minOf(calculatedFee, maxFee)
    }
    
    /**
     * Generate risk assessment based on validation results
     */
    private fun generateRiskAssessment(
        validationResult: ApplicationValidationResult,
        extractedData: ExtractedApplicationData
    ): RiskAssessment {
        val riskFactors = mutableListOf<String>()
        val mitigatingFactors = mutableListOf<String>()
        
        // Analyze validation results for risk factors
        validationResult.failedRules.forEach { rule ->
            riskFactors.add("Failed validation: $rule")
        }
        
        if (extractedData.overallConfidence < 0.9f) {
            riskFactors.add("Lower extraction confidence: ${String.format("%.1f", extractedData.overallConfidence * 100)}%")
        }
        
        // Identify mitigating factors
        if (validationResult.isEligible) {
            mitigatingFactors.add("Passed all critical business rules")
        }
        
        if (extractedData.overallConfidence >= 0.8f) {
            mitigatingFactors.add("Good document quality and extraction confidence")
        }
        
        val payslipData = extractedData.payslipData.firstOrNull()
        if (payslipData != null && payslipData.grossSalary > 50000) {
            mitigatingFactors.add("Above-average income level")
        }
        
        val overallRiskScore = calculateRiskScore(validationResult, extractedData)
        
        return RiskAssessment(
            overallRiskScore = overallRiskScore,
            confidenceLevel = validationResult.overallConfidence,
            riskFactors = riskFactors,
            mitigatingFactors = mitigatingFactors,
            recommendedAction = when {
                overallRiskScore <= 0.3f -> "APPROVE_STANDARD"
                overallRiskScore <= 0.6f -> "APPROVE_WITH_MONITORING"
                else -> "MANUAL_REVIEW_REQUIRED"
            }
        )
    }
    
    /**
     * Calculate overall risk score (0.0 = lowest risk, 1.0 = highest risk)
     */
    private fun calculateRiskScore(
        validationResult: ApplicationValidationResult,
        extractedData: ExtractedApplicationData
    ): Float {
        var riskScore = 0.0f
        
        // Base risk from failed validations
        riskScore += validationResult.failedRules.size * 0.2f
        
        // Risk from low confidence
        if (extractedData.overallConfidence < 0.8f) {
            riskScore += (0.8f - extractedData.overallConfidence)
        }
        
        // Cap at 1.0
        return minOf(riskScore, 1.0f)
    }
    
    /**
     * Generate approval notes based on validation and offer details
     */
    private fun generateApprovalNotes(
        validationResult: ApplicationValidationResult,
        loanOffer: LoanOffer
    ): String {
        val notes = mutableListOf<String>()
        
        notes.add("Application processed via AI-powered Smart Loan system")
        notes.add("DSR: ${String.format("%.1f", loanOffer.dsr)}% (within policy limits)")
        
        if (validationResult.getWarningMessages().isNotEmpty()) {
            notes.add("Warnings: ${validationResult.getWarningMessages().joinToString(", ")}")
        }
        
        notes.add("Biometric authentication required for final approval")
        
        return notes.joinToString(". ")
    }
    
    /**
     * Format memo for human-readable display
     */
    fun formatMemoForDisplay(memo: DisbursementMemo): String {
        return buildString {
            appendLine("=== LOAN DISBURSEMENT MEMO ===")
            appendLine("Application ID: ${memo.applicationId}")
            appendLine("Generated: ${dateFormatter.format(Date(memo.timestamp))}")
            appendLine("Status: ${memo.status}")
            appendLine()
            
            appendLine("APPLICANT INFORMATION:")
            appendLine("Name: ${memo.applicantInfo.fullName}")
            appendLine("ID Number: ${memo.applicantInfo.idNumber}")
            appendLine("Employer: ${memo.applicantInfo.employerName}")
            appendLine("Gross Salary: KSh ${String.format("%,.2f", memo.applicantInfo.grossSalary)}")
            appendLine()
            
            appendLine("LOAN DETAILS:")
            appendLine("Approved Amount: KSh ${String.format("%,.2f", memo.loanDetails.approvedAmount)}")
            appendLine("Interest Rate: ${String.format("%.1f", memo.loanDetails.interestRate * 100)}%")
            appendLine("Term: ${memo.loanDetails.termMonths} months")
            appendLine("Monthly Payment: KSh ${String.format("%,.2f", memo.loanDetails.monthlyPayment)}")
            appendLine("DSR: ${String.format("%.1f", memo.loanDetails.dsr)}%")
            appendLine()
            
            appendLine("RISK ASSESSMENT:")
            appendLine("Risk Score: ${String.format("%.2f", memo.riskAssessment.overallRiskScore)}")
            appendLine("Recommended Action: ${memo.riskAssessment.recommendedAction}")
            
            if (memo.riskAssessment.riskFactors.isNotEmpty()) {
                appendLine("Risk Factors: ${memo.riskAssessment.riskFactors.joinToString(", ")}")
            }
            
            appendLine()
            appendLine("APPROVAL STATUS:")
            appendLine("Biometric Confirmed: ${if (memo.approvalInfo.biometricConfirmed) "YES" else "PENDING"}")
            appendLine("Terms Accepted: ${if (memo.approvalInfo.termsAccepted) "YES" else "PENDING"}")
        }
    }
}