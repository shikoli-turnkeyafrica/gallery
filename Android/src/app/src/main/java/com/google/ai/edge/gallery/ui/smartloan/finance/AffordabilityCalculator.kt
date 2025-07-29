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
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import kotlin.math.pow

private const val TAG = "AffordabilityCalculator"

/**
 * Calculator for loan affordability, DSR, and payment calculations
 */
class AffordabilityCalculator {
    
    /**
     * Calculates Debt Service Ratio (DSR) as a percentage
     * DSR = (Total Monthly Debt Payments / Gross Monthly Income) * 100
     */
    fun calculateDSR(
        grossSalary: Double,
        existingDeductions: Map<String, Double>,
        proposedLoanPayment: Double
    ): Double {
        if (grossSalary <= 0) return Double.MAX_VALUE
        
        // Calculate total existing debt payments
        val totalExistingDebt = existingDeductions.values.sum()
        
        // Total debt = existing + proposed loan payment
        val totalDebtPayments = totalExistingDebt + proposedLoanPayment
        
        val dsr = (totalDebtPayments / grossSalary) * 100
        
        Log.d(TAG, "DSR Calculation:")
        Log.d(TAG, "  Gross Salary: $grossSalary")
        Log.d(TAG, "  Existing Debt: $totalExistingDebt")
        Log.d(TAG, "  Proposed Payment: $proposedLoanPayment")
        Log.d(TAG, "  Total Debt: $totalDebtPayments")
        Log.d(TAG, "  DSR: ${String.format("%.2f", dsr)}%")
        
        return dsr
    }
    
    /**
     * Calculates maximum loan amount while keeping DSR within limits
     */
    fun calculateMaxLoanAmount(
        grossSalary: Double,
        existingDeductions: Map<String, Double> = emptyMap(),
        maxDSR: Double = 50.0, // 50% maximum DSR
        interestRate: Double = 0.15, // 15% annual
        termMonths: Int = 12
    ): Double {
        if (grossSalary <= 0) return 0.0
        
        // Calculate maximum allowable monthly debt payment
        val maxMonthlyDebt = (grossSalary * maxDSR) / 100.0
        
        // Subtract existing debt payments
        val existingDebt = existingDeductions.values.sum()
        val availableForNewLoan = maxMonthlyDebt - existingDebt
        
        if (availableForNewLoan <= 0) return 0.0
        
        // Calculate maximum loan amount based on available monthly payment
        val maxLoanAmount = calculateLoanPrincipal(
            monthlyPayment = availableForNewLoan,
            interestRate = interestRate,
            termMonths = termMonths
        )
        
        Log.d(TAG, "Max Loan Calculation:")
        Log.d(TAG, "  Gross Salary: $grossSalary")
        Log.d(TAG, "  Max DSR: ${maxDSR}%")
        Log.d(TAG, "  Max Monthly Debt: $maxMonthlyDebt")
        Log.d(TAG, "  Existing Debt: $existingDebt")
        Log.d(TAG, "  Available for New Loan: $availableForNewLoan")
        Log.d(TAG, "  Max Loan Amount: $maxLoanAmount")
        
        return maxLoanAmount
    }
    
    /**
     * Calculates monthly payment for a given loan amount
     * Uses PMT formula: P * [r(1+r)^n] / [(1+r)^n - 1]
     */
    fun calculateMonthlyPayment(
        loanAmount: Double,
        interestRate: Double,
        termMonths: Int
    ): Double {
        if (loanAmount <= 0 || termMonths <= 0) return 0.0
        if (interestRate <= 0) return loanAmount / termMonths // Interest-free loan
        
        val monthlyRate = interestRate / 12.0
        val factor = (1 + monthlyRate).pow(termMonths)
        val monthlyPayment = loanAmount * (monthlyRate * factor) / (factor - 1)
        
        Log.d(TAG, "Monthly Payment Calculation:")
        Log.d(TAG, "  Loan Amount: $loanAmount")
        Log.d(TAG, "  Annual Rate: ${(interestRate * 100)}%")
        Log.d(TAG, "  Term: $termMonths months")
        Log.d(TAG, "  Monthly Payment: $monthlyPayment")
        
        return monthlyPayment
    }
    
    /**
     * Calculates loan principal from monthly payment (reverse PMT calculation)
     */
    fun calculateLoanPrincipal(
        monthlyPayment: Double,
        interestRate: Double,
        termMonths: Int
    ): Double {
        if (monthlyPayment <= 0 || termMonths <= 0) return 0.0
        if (interestRate <= 0) return monthlyPayment * termMonths // Interest-free loan
        
        val monthlyRate = interestRate / 12.0
        val factor = (1 + monthlyRate).pow(termMonths)
        val principal = monthlyPayment * (factor - 1) / (monthlyRate * factor)
        
        return principal
    }
    
    /**
     * Calculates total interest over the loan term
     */
    fun calculateTotalInterest(
        loanAmount: Double,
        interestRate: Double,
        termMonths: Int
    ): Double {
        val monthlyPayment = calculateMonthlyPayment(loanAmount, interestRate, termMonths)
        val totalPayments = monthlyPayment * termMonths
        return totalPayments - loanAmount
    }
    
    /**
     * Calculates effective interest rate if paid early
     */
    fun calculateEarlyPaymentSavings(
        loanAmount: Double,
        interestRate: Double,
        originalTermMonths: Int,
        actualPaymentMonths: Int
    ): Double {
        val originalInterest = calculateTotalInterest(loanAmount, interestRate, originalTermMonths)
        val actualInterest = calculateTotalInterest(loanAmount, interestRate, actualPaymentMonths)
        return originalInterest - actualInterest
    }
    
    /**
     * Extracts deductions from payslip data for DSR calculation
     */
    fun extractDeductionsFromPayslips(payslips: List<PayslipData>): Map<String, Double> {
        if (payslips.isEmpty()) return emptyMap()
        
        // Average deductions across all payslips
        val allDeductions = mutableMapOf<String, MutableList<Double>>()
        
        payslips.forEach { payslip ->
            payslip.deductions.forEach { (key, value) ->
                allDeductions.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        
        // Calculate average for each deduction type
        return allDeductions.mapValues { (_, values) ->
            values.average()
        }
    }
    
    /**
     * Validates if proposed loan meets affordability criteria
     */
    fun validateAffordability(
        grossSalary: Double,
        existingDeductions: Map<String, Double>,
        proposedLoanAmount: Double,
        interestRate: Double,
        termMonths: Int,
        maxDSR: Double = 50.0
    ): AffordabilityResult {
        val monthlyPayment = calculateMonthlyPayment(proposedLoanAmount, interestRate, termMonths)
        val dsr = calculateDSR(grossSalary, existingDeductions, monthlyPayment)
        val maxAffordable = calculateMaxLoanAmount(grossSalary, existingDeductions, maxDSR, interestRate, termMonths)
        
        return AffordabilityResult(
            isAffordable = dsr <= maxDSR,
            dsr = dsr,
            monthlyPayment = monthlyPayment,
            maxAffordableAmount = maxAffordable,
            excessAmount = if (proposedLoanAmount > maxAffordable) proposedLoanAmount - maxAffordable else 0.0
        )
    }
}

/**
 * Result of affordability validation
 */
data class AffordabilityResult(
    val isAffordable: Boolean,
    val dsr: Double,
    val monthlyPayment: Double,
    val maxAffordableAmount: Double,
    val excessAmount: Double = 0.0
) {
    fun getDSRPercentage(): String = String.format("%.1f%%", dsr)
} 