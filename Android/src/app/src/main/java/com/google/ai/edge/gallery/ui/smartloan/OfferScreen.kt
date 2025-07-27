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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

/**
 * Offer Screen - Shows final loan offer with acceptance options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferScreen(
  onAcceptOffer: () -> Unit,
  onDeclineOffer: () -> Unit,
  onNavigateUp: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Loan Offer") },
        navigationIcon = {
          androidx.compose.material3.IconButton(onClick = onNavigateUp) {
            androidx.compose.material3.Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      
      // Success Header
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Congratulations!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
          )
          Text(
            text = "Your loan application has been approved",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
      
      // Applicant Information
      Card(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "Applicant Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
          )
          
          InfoRow("Name", uiState.applicationData.applicantName)
          InfoRow("ID Number", uiState.applicationData.nationalId)
          InfoRow("Employer", uiState.applicationData.employer)
          InfoRow("Monthly Income", numberFormat.format(uiState.applicationData.monthlyIncome))
        }
      }
      
      // Loan Offer Details
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "Loan Offer Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
          )
          
          // Main loan amount (highlighted)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Loan Amount",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = numberFormat.format(uiState.loanOffer.maxLoanAmount),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          
          InfoRow("Interest Rate", "${uiState.loanOffer.interestRate}% per annum")
          InfoRow("Loan Term", "${uiState.loanOffer.loanTerm} months")
          InfoRow("Monthly Payment", numberFormat.format(uiState.loanOffer.monthlyPayment))
          InfoRow("Processing Fee", numberFormat.format(uiState.loanOffer.processingFee))
        }
      }
      
      // Terms and Conditions
      Card(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "Important Terms",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          
          Text(
            text = "• This offer is valid for 48 hours\n" +
                  "• Early repayment allowed without penalties\n" +
                  "• Late payment fees may apply\n" +
                  "• Loan is subject to final verification",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDeclineOffer,
          modifier = Modifier.weight(1f)
        ) {
          Text("Decline")
        }
        
        Button(
          onClick = onAcceptOffer,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text("Accept Offer")
        }
      }
    }
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium
    )
  }
} 