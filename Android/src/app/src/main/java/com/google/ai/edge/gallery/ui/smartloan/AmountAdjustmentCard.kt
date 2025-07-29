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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * Interactive card for adjusting loan amount with real-time calculations
 */
@Composable
fun AmountAdjustmentCard(
    currentAmount: Double,
    minAmount: Double,
    maxAmount: Double,
    interestRate: Double,
    termMonths: Int,
    currentDSR: Double,
    maxDSR: Double = 50.0,
    onAmountChange: (Double) -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(currentAmount) { 
        mutableFloatStateOf(currentAmount.toFloat()) 
    }
    var hasChanged by remember { mutableStateOf(false) }
    
    // Calculate derived values
    val monthlyPayment = calculateMonthlyPayment(sliderValue.toDouble(), interestRate, termMonths)
    val totalRepayment = monthlyPayment * termMonths
    val newDSR = (currentDSR / currentAmount) * sliderValue.toDouble() // Approximate DSR calculation
    val isDSRWarning = newDSR > maxDSR * 0.8 // Warning at 80% of max DSR
    val isDSRDanger = newDSR > maxDSR
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Adjust Loan Amount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                
                if (hasChanged) {
                    Button(
                        onClick = {
                            onAmountChange(sliderValue.toDouble())
                            onRecalculate()
                            hasChanged = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Recalculate",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current amount display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Selected Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "KSh ${NumberFormat.getNumberInstance().format(sliderValue.toDouble())}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Amount range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "KSh ${NumberFormat.getNumberInstance().format(minAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Text(
                    "KSh ${NumberFormat.getNumberInstance().format(maxAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            // Amount slider
            Slider(
                value = sliderValue,
                onValueChange = { 
                    sliderValue = it
                    hasChanged = true
                },
                valueRange = minAmount.toFloat()..maxAmount.toFloat(),
                steps = ((maxAmount - minAmount) / 5000).roundToInt() - 1, // 5000 step increments
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1976D2),
                    activeTrackColor = Color(0xFF1976D2),
                    inactiveTrackColor = Color(0xFFE0E0E0)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Payment calculations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDSRDanger) Color(0xFFFFEBEE) else Color(0xFFF8F9FA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Payment Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF000000)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PaymentRow(
                        label = "Monthly Payment",
                        value = "KSh ${NumberFormat.getNumberInstance().format(monthlyPayment)}",
                        isHighlight = true
                    )
                    
                    PaymentRow(
                        label = "Interest Rate",
                        value = "${String.format("%.1f", interestRate * 100)}% p.a."
                    )
                    
                    PaymentRow(
                        label = "Loan Term",
                        value = "$termMonths months"
                    )
                    
                    PaymentRow(
                        label = "Total Repayment",
                        value = "KSh ${NumberFormat.getNumberInstance().format(totalRepayment)}"
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    
                    // DSR indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Debt Service Ratio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF000000)
                            )
                            
                            if (isDSRWarning) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "DSR Warning",
                                    tint = if (isDSRDanger) Color(0xFFFF5722) else Color(0xFFFF9800),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Text(
                            "${String.format("%.1f", newDSR)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isDSRDanger -> Color(0xFFFF5722)
                                isDSRWarning -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }
                }
            }
            
            // DSR warning message
            if (isDSRWarning) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDSRDanger) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isDSRDanger) Color(0xFFFF5722) else Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                if (isDSRDanger) "DSR Limit Exceeded" else "High DSR Warning",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDSRDanger) Color(0xFFD32F2F) else Color(0xFFE65100)
                            )
                            
                            Text(
                                if (isDSRDanger) {
                                    "This amount exceeds the maximum DSR of ${maxDSR}%. Please select a lower amount."
                                } else {
                                    "Your DSR is approaching the maximum limit of ${maxDSR}%. Consider a lower amount for better approval chances."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF000000),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF000000)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) Color(0xFF1976D2) else Color(0xFF000000)
        )
    }
}

/**
 * Calculate monthly payment using PMT formula
 */
private fun calculateMonthlyPayment(
    loanAmount: Double,
    annualInterestRate: Double,
    termMonths: Int
): Double {
    if (annualInterestRate == 0.0) {
        return loanAmount / termMonths
    }
    
    val monthlyRate = annualInterestRate / 12
    val numerator = loanAmount * monthlyRate * Math.pow(1 + monthlyRate, termMonths.toDouble())
    val denominator = Math.pow(1 + monthlyRate, termMonths.toDouble()) - 1
    
    return numerator / denominator
}