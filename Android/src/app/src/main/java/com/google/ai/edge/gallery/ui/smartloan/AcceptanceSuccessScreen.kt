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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.smartloan.finance.LoanOffer
import com.google.ai.edge.gallery.ui.smartloan.memo.DisbursementMemo
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Success screen shown after loan acceptance with biometric confirmation
 */
@Composable
fun AcceptanceSuccessScreen(
    loanOffer: LoanOffer?,
    disbursementMemo: DisbursementMemo?,
    onExportMemo: () -> Unit,
    onViewMemo: () -> Unit,
    onNewApplication: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color.White
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Animated success icon
        AnimatedVisibility(
            visible = animationPlayed,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Success message
        AnimatedVisibility(
            visible = animationPlayed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 800, delayMillis = 300)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Loan Approved!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Congratulations! Your loan application has been successfully approved and processed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF000000),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Loan details summary
        if (loanOffer != null) {
            AnimatedVisibility(
                visible = animationPlayed,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 800, delayMillis = 600)
                )
            ) {
                LoanSummaryCard(loanOffer = loanOffer)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Disbursement timeline
        AnimatedVisibility(
            visible = animationPlayed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 800, delayMillis = 900)
            )
        ) {
            DisbursementTimelineCard()
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Next steps
        AnimatedVisibility(
            visible = animationPlayed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 800, delayMillis = 1200)
            )
        ) {
            NextStepsCard()
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Memo actions
        if (disbursementMemo != null) {
            AnimatedVisibility(
                visible = animationPlayed,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 800, delayMillis = 1500)
                )
            ) {
                MemoActionsCard(
                    onExportMemo = onExportMemo,
                    onViewMemo = onViewMemo
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Action buttons
        AnimatedVisibility(
            visible = animationPlayed,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 800, delayMillis = 1800)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNewApplication,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "New Application",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Back to Home",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun LoanSummaryCard(
    loanOffer: LoanOffer,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Loan Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SummaryRow(
                label = "Approved Amount",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.recommendedAmount)}",
                isHighlight = true
            )
            
            SummaryRow(
                label = "Monthly Payment",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.monthlyPayment)}"
            )
            
            SummaryRow(
                label = "Interest Rate",
                value = "${String.format("%.1f", loanOffer.interestRate * 100)}% p.a."
            )
            
            SummaryRow(
                label = "Loan Term",
                value = "${loanOffer.loanTerm} months"
            )
            
            SummaryRow(
                label = "Total Repayment",
                value = "KSh ${NumberFormat.getNumberInstance().format(loanOffer.totalRepayment)}"
            )
        }
    }
}

@Composable
private fun DisbursementTimelineCard(
    modifier: Modifier = Modifier
) {
    val currentDate = Calendar.getInstance()
    val disbursementDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    val firstPaymentDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 31) }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Disbursement Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            TimelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Application Approved",
                subtitle = "Today - ${dateFormat.format(currentDate.time)}",
                isCompleted = true,
                isLast = false
            )
            
            TimelineItem(
                icon = Icons.Default.AccountBalance,
                title = "Funds Disbursement",
                subtitle = "Tomorrow - ${dateFormat.format(disbursementDate.time)}",
                isCompleted = false,
                isLast = false
            )
            
            TimelineItem(
                icon = Icons.Default.Payment,
                title = "First Payment Due",
                subtitle = dateFormat.format(firstPaymentDate.time),
                isCompleted = false,
                isLast = true
            )
        }
    }
}

@Composable
private fun NextStepsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ListAlt,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "What's Next?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            NextStepItem(
                number = "1",
                title = "Disbursement Confirmation",
                description = "You'll receive SMS confirmation once funds are transferred to your account."
            )
            
            NextStepItem(
                number = "2",
                title = "Payment Reminders",
                description = "We'll send monthly reminders 3 days before each payment is due."
            )
            
            NextStepItem(
                number = "3",
                title = "Account Management",
                description = "Use our mobile app to track payments, view statements, and manage your loan."
            )
        }
    }
}

@Composable
private fun MemoActionsCard(
    onExportMemo: () -> Unit,
    onViewMemo: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Loan Documentation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Your disbursement memo has been generated and saved securely.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewMemo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9C27B0)
                    )
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Memo")
                }
                
                Button(
                    onClick = onExportMemo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
            color = if (isHighlight) Color(0xFF4CAF50) else Color(0xFF000000)
        )
    }
}

@Composable
private fun TimelineItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isCompleted) Color.White else Color(0xFF666666),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
            
            if (!isLast) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun NextStepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF1976D2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                lineHeight = 18.sp
            )
        }
    }
}