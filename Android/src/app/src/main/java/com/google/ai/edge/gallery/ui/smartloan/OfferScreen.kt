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

package com.google.ai.edge.gallery.ui.smartloan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

/**
 * Enhanced Offer Screen with professional banking design and Task 05 integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferScreen(
    onAcceptOffer: () -> Unit,
    onDeclineOffer: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: SmartLoanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val loanOffer = uiState.loanOffer
    val extractedData = uiState.extractedData
    
    // Show dialogs based on state
    if (uiState.showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { viewModel.hideTermsDialog() },
            onAccept = {
                viewModel.hideTermsDialog()
                viewModel.acceptLoanOffer()
            }
        )
    }
    
    if (uiState.showDeclineDialog) {
        DeclineOptionsDialog(
            onDecline = { reason, feedback ->
                viewModel.declineLoan(reason, feedback)
                onDeclineOffer()
            },
            onDismiss = { viewModel.hideDeclineDialog() }
        )
    }
    
    // Navigate to success screen when loan is accepted
    LaunchedEffect(uiState.isLoanAccepted) {
        if (uiState.isLoanAccepted) {
            onAcceptOffer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Loan Offer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1976D2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color.White
                        )
                    )
                )
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Success banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Approved",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Congratulations!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            "Your loan application has been approved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF000000)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loan offer details
            LoanOfferCard(
                loanOffer = loanOffer,
                extractedData = extractedData
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Amount adjustment section
            if (uiState.showAmountAdjustment) {
                AmountAdjustmentCard(
                    currentAmount = loanOffer.recommendedAmount,
                    minAmount = loanOffer.recommendedAmount * 0.5, // 50% of recommended
                    maxAmount = loanOffer.maxLoanAmount,
                    interestRate = loanOffer.interestRate,
                    termMonths = loanOffer.loanTerm,
                    currentDSR = loanOffer.dsr,
                    onAmountChange = { newAmount ->
                        viewModel.recalculateLoanOfferForAmount(newAmount)
                    },
                    onRecalculate = {
                        // Recalculation handled in onAmountChange
                    }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Amount adjustment toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Adjust loan amount?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF000000)
                    )
                    
                    TextButton(
                        onClick = { viewModel.toggleAmountAdjustment() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF1976D2)
                        )
                    ) {
                        Text(
                            if (uiState.showAmountAdjustment) "Hide Options" else "Show Options",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Terms preview
            TermsPreviewCard(
                onViewFullTerms = { viewModel.showTermsDialog() }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Quick decline option
            QuickDeclineCard(
                onDecline = { viewModel.showDeclineDialog() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Accept button
                Button(
                    onClick = { viewModel.showTermsDialog() },
                    enabled = !uiState.isAcceptingLoan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isAcceptingLoan) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Accept Loan Offer",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Decline button
                OutlinedButton(
                    onClick = { viewModel.showDeclineDialog() },
                    enabled = !uiState.isAcceptingLoan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Decline Offer",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Error message
            if (uiState.acceptanceError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD32F2F)
                            )
                                                         Text(
                                 uiState.acceptanceError ?: "",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = Color(0xFF000000)
                             )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LoanOfferCard(
    loanOffer: com.google.ai.edge.gallery.ui.smartloan.finance.LoanOffer,
    extractedData: com.google.ai.edge.gallery.ui.smartloan.data.ExtractedApplicationData?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Your Loan Offer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Applicant information
            if (extractedData?.idCardData?.fullName?.isNotEmpty() == true) {
                Text(
                    "Applicant Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF000000)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow(
                    label = "Full Name",
                    value = extractedData.idCardData.fullName
                )
                
                if (extractedData.idCardData.idNumber.isNotEmpty()) {
                    InfoRow(
                        label = "ID Number",
                        value = extractedData.idCardData.idNumber
                    )
                }
                
                // Show employer and income from payslip data
                extractedData.payslipData.firstOrNull()?.let { payslip ->
                    if (payslip.employerName.isNotEmpty()) {
                        InfoRow(
                            label = "Employer",
                            value = payslip.employerName
                        )
                    }
                    
                    if (payslip.grossSalary > 0) {
                        InfoRow(
                            label = "Monthly Income",
                            value = "KSh ${NumberFormat.getNumberInstance().format(payslip.grossSalary)}"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Loan details
            Text(
                "Loan Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Highlighted approved amount
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Approved Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "KSh ${NumberFormat.getNumberInstance().format(loanOffer.recommendedAmount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoRow(
                label = "Monthly Payment",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.monthlyPayment)}"
            )
            
            InfoRow(
                label = "Interest Rate",
                value = "${String.format("%.1f", loanOffer.interestRate * 100)}% p.a."
            )
            
            InfoRow(
                label = "Loan Period",
                value = "${loanOffer.loanTerm} months"
            )
            
            InfoRow(
                label = "Total Repayment",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.totalRepayment)}"
            )
            
            InfoRow(
                label = "Processing Fee",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.processingFee)}"
            )
            
            InfoRow(
                label = "Debt Service Ratio",
                value = "${String.format("%.1f", loanOffer.dsr)}%"
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF000000),
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF000000),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
} 