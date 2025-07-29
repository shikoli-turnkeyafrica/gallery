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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import java.text.NumberFormat
import java.util.Locale

/**
 * Data Review Screen - Allows manual review and correction of AI-extracted data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataReviewScreen(
  onContinue: () -> Unit,
  onNavigateUp: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val extractedData = uiState.extractedData
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Review Extracted Data") },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    
    if (extractedData == null) {
      // Show loading or error state
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "No extracted data available",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateUp) {
          Text("Go Back")
        }
      }
      return@Scaffold
    }
    
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      
      // Overall confidence and status
      OverallConfidenceCard(
        confidence = extractedData.overallConfidence,
        isComplete = extractedData.isComplete,
        validationIssues = extractedData.getValidationIssues()
      )
      
      // ID Card Data Review
      IdDataReviewSection(
        idData = extractedData.idCardData,
        onDataUpdated = { updatedIdData ->
          // TODO: Update the extracted data in ViewModel
        }
      )
      
      // Payslip Data Review
      PayslipDataReviewSection(
        payslipData = extractedData.payslipData,
        onDataUpdated = { index, updatedPayslipData ->
          // TODO: Update the payslip data in ViewModel
        }
      )
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        TextButton(
          onClick = {
            // Retry extraction
            viewModel.retryExtraction("all")
          },
          modifier = Modifier.weight(1f)
        ) {
          Text("Retry Extraction")
        }
        
        Button(
          onClick = onContinue,
          enabled = extractedData.isComplete,
          modifier = Modifier.weight(1f)
        ) {
          Text("Continue")
        }
      }
    }
  }
}

@Composable
private fun OverallConfidenceCard(
  confidence: Float,
  isComplete: Boolean,
  validationIssues: List<String>,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = when {
        confidence > 0.8f && isComplete -> MaterialTheme.colorScheme.primaryContainer
        confidence > 0.6f -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
      }
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = if (confidence > 0.8f && isComplete) Icons.Default.CheckCircle else Icons.Default.Warning,
          contentDescription = null,
          tint = when {
            confidence > 0.8f && isComplete -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
          }
        )
        Text(
          text = "Extraction Quality",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Confidence bar
      LinearProgressIndicator(
        progress = { confidence },
        modifier = Modifier.fillMaxWidth()
      )
      
      Spacer(modifier = Modifier.height(4.dp))
      
      Text(
        text = "${(confidence * 100).toInt()}% confidence",
        style = MaterialTheme.typography.bodyMedium
      )
      
      // Validation issues
      if (validationIssues.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Issues to review:",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.error
        )
        validationIssues.forEach { issue ->
          Text(
            text = "• $issue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    }
  }
}

@Composable
private fun IdDataReviewSection(
  idData: IdCardData,
  onDataUpdated: (IdCardData) -> Unit,
  modifier: Modifier = Modifier
) {
  var editableIdData by remember { mutableStateOf(idData) }
  
  OutlinedCard(
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "ID Card Information",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
        ConfidenceBadge(confidence = idData.extractionConfidence)
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Full Name
      OutlinedTextField(
        value = editableIdData.fullName,
        onValueChange = { 
          editableIdData = editableIdData.copy(fullName = it)
          onDataUpdated(editableIdData.validate())
        },
        label = { Text("Full Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // ID Number
      OutlinedTextField(
        value = editableIdData.idNumber,
        onValueChange = { 
          editableIdData = editableIdData.copy(idNumber = it)
          onDataUpdated(editableIdData.validate())
        },
        label = { Text("ID Number") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Date of Birth
      OutlinedTextField(
        value = editableIdData.dateOfBirth,
        onValueChange = { 
          editableIdData = editableIdData.copy(dateOfBirth = it)
          onDataUpdated(editableIdData.validate())
        },
        label = { Text("Date of Birth (YYYY-MM-DD)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Place of Birth (optional)
      OutlinedTextField(
        value = editableIdData.placeOfBirth,
        onValueChange = { 
          editableIdData = editableIdData.copy(placeOfBirth = it)
          onDataUpdated(editableIdData.validate())
        },
        label = { Text("Place of Birth (Optional)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      // Show age if valid date
      editableIdData.getAge()?.let { age ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Age: $age years",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun PayslipDataReviewSection(
  payslipData: List<PayslipData>,
  onDataUpdated: (Int, PayslipData) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Payslip Information",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium
    )
    
    if (payslipData.isEmpty()) {
      OutlinedCard {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "No payslip data extracted",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      payslipData.forEachIndexed { index, payslip ->
        if (payslip.isValid) {
          PayslipReviewCard(
            payslipData = payslip,
            payslipIndex = index,
            onDataUpdated = { updatedPayslip ->
              onDataUpdated(index, updatedPayslip)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun PayslipReviewCard(
  payslipData: PayslipData,
  payslipIndex: Int,
  onDataUpdated: (PayslipData) -> Unit,
  modifier: Modifier = Modifier
) {
  var editablePayslipData by remember { mutableStateOf(payslipData) }
  val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
  
  OutlinedCard(
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Payslip ${payslipIndex + 1}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium
        )
        ConfidenceBadge(confidence = payslipData.extractionConfidence)
      }
      
      Spacer(modifier = Modifier.height(12.dp))
      
      // Employee Name
      OutlinedTextField(
        value = editablePayslipData.employeeName,
        onValueChange = { 
          editablePayslipData = editablePayslipData.copy(employeeName = it)
          onDataUpdated(editablePayslipData.validate())
        },
        label = { Text("Employee Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Employer
      OutlinedTextField(
        value = editablePayslipData.employerName,
        onValueChange = { 
          editablePayslipData = editablePayslipData.copy(employerName = it)
          onDataUpdated(editablePayslipData.validate())
        },
        label = { Text("Employer") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Salary fields
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = editablePayslipData.grossSalary.toString(),
          onValueChange = { 
            val newGross = it.toDoubleOrNull() ?: 0.0
            editablePayslipData = editablePayslipData.copy(grossSalary = newGross)
            onDataUpdated(editablePayslipData.validate())
          },
          label = { Text("Gross Salary") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        
        OutlinedTextField(
          value = editablePayslipData.netSalary.toString(),
          onValueChange = { 
            val newNet = it.toDoubleOrNull() ?: 0.0
            editablePayslipData = editablePayslipData.copy(netSalary = newNet)
            onDataUpdated(editablePayslipData.validate())
          },
          label = { Text("Net Salary") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          singleLine = true
        )
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Pay Period
      OutlinedTextField(
        value = editablePayslipData.payPeriod,
        onValueChange = { 
          editablePayslipData = editablePayslipData.copy(payPeriod = it)
          onDataUpdated(editablePayslipData.validate())
        },
        label = { Text("Pay Period (YYYY-MM)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
      
      // Show formatted period
      if (editablePayslipData.payPeriod.isNotBlank()) {
        Text(
          text = "Period: ${editablePayslipData.getFormattedPayPeriod()}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      // Show salary calculation check
      if (editablePayslipData.grossSalary > 0 && editablePayslipData.netSalary > 0) {
        val isConsistent = editablePayslipData.isNetSalaryConsistent()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (isConsistent) "✓ Salary calculation appears consistent" else "⚠ Net salary may not match deductions",
          style = MaterialTheme.typography.bodySmall,
          color = if (isConsistent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

@Composable
private fun ConfidenceBadge(
  confidence: Float,
  modifier: Modifier = Modifier
) {
  val backgroundColor = when {
    confidence > 0.8f -> MaterialTheme.colorScheme.primaryContainer
    confidence > 0.6f -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.errorContainer
  }
  
  val textColor = when {
    confidence > 0.8f -> MaterialTheme.colorScheme.onPrimaryContainer
    confidence > 0.6f -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onErrorContainer
  }
  
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = backgroundColor)
  ) {
    Text(
      text = "${(confidence * 100).toInt()}%",
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
} 