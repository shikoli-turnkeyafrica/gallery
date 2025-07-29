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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Predefined decline reasons
 */
val DECLINE_REASONS = listOf(
    "Amount too low",
    "Interest rate too high", 
    "Monthly payment too high",
    "Loan term too short",
    "Changed my mind",
    "Found better offer elsewhere",
    "Need more time to decide",
    "Other"
)

/**
 * Dialog for collecting loan decline feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclineOptionsDialog(
    onDecline: (reason: String, feedback: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReason by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }
    var showFeedbackField by remember { mutableStateOf(false) }
    
    // Show feedback field when "Other" is selected or any reason is selected
    LaunchedEffect(selectedReason) {
        showFeedbackField = selectedReason.isNotEmpty()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = {
                        Text(
                            "Decline Loan Offer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFFF5722)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFFF8F5)
                    )
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Information message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "We're sorry to see you go!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your feedback helps us improve our loan offers. Please let us know why you're declining this offer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF000000),
                                lineHeight = 20.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Decline reasons
                    Text(
                        "Please select a reason:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF000000)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DECLINE_REASONS.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = null, // Handled by selectable modifier
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1976D2)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF000000)
                            )
                        }
                    }
                    
                    // Feedback text field
                    if (showFeedbackField) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            if (selectedReason == "Other") "Please specify:" else "Additional feedback (optional):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF000000)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = {
                                Text(
                                    if (selectedReason == "Other") {
                                        "Please tell us your reason..."
                                    } else {
                                        "Any additional comments or suggestions..."
                                    },
                                    color = Color(0xFF666666)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            maxLines = 4
                        )
                    }
                }
                
                // Footer with action buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF666666)
                                )
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    val finalReason = selectedReason.ifEmpty { "No reason specified" }
                                    val finalFeedback = if (selectedReason == "Other" && feedbackText.isNotBlank()) {
                                        feedbackText
                                    } else if (feedbackText.isNotBlank()) {
                                        "$finalReason - $feedbackText"
                                    } else {
                                        finalReason
                                    }
                                    onDecline(finalReason, finalFeedback)
                                },
                                enabled = selectedReason.isNotEmpty() && 
                                         (selectedReason != "Other" || feedbackText.isNotBlank()),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5722),
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                )
                            ) {
                                Text(
                                    "Submit Decline",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Reassurance message
                        Text(
                            "Don't worry - you can always apply again later with different terms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple decline confirmation card for quick decline option
 */
@Composable
fun QuickDeclineCard(
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Not interested in this offer?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "We understand loan decisions are important. You can decline this offer and we'll help you find better options.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onDecline,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF5722)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Decline Offer",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}