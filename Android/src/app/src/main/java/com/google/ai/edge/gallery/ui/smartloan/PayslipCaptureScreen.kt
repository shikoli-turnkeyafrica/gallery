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

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Payslip Capture Screen - For capturing up to 4 payslip images
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipCaptureScreen(
  onContinue: () -> Unit,
  onNavigateUp: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Capture Payslips") },
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
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      
      // Instructions
      Text(
        text = "Capture photos of your recent payslips (at least 1, up to 4)",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      
      // Progress indicator
      Text(
        text = "${uiState.payslipImages.images.size} of 4 payslips captured",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      
      // Payslip Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        // Existing payslip images
        itemsIndexed(uiState.payslipImages.images) { index, imageUri ->
          PayslipImageCard(
            imageUri = imageUri,
            onRemove = { viewModel.removePayslipImage(index) }
          )
        }
        
        // Add button if can add more
        if (uiState.payslipImages.canAddMore) {
          item {
            AddPayslipCard(
              onAddPayslip = {
                // In real implementation, this would open camera
                // For now, we'll use a mock URI
                val mockUri = Uri.parse("content://mock/payslip_${uiState.payslipImages.images.size + 1}")
                viewModel.addPayslipImage(mockUri)
              }
            )
          }
        }
        
        // Fill remaining slots with empty cards
        val emptySlots = 4 - uiState.payslipImages.images.size - (if (uiState.payslipImages.canAddMore) 1 else 0)
        repeat(emptySlots) {
          item {
            EmptyPayslipCard()
          }
        }
      }
      
      // Continue Button
      Button(
        onClick = onContinue,
        enabled = viewModel.isPayslipCaptureComplete(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Continue to Validation")
      }
    }
  }
}

@Composable
private fun PayslipImageCard(
  imageUri: Uri,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.aspectRatio(1f),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    )
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = "Captured Payslip",
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Payslip",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      
      // Remove button
      IconButton(
        onClick = onRemove,
        modifier = Modifier.align(Alignment.TopEnd)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Remove",
          tint = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

@Composable
private fun AddPayslipCard(
  onAddPayslip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    onClick = onAddPayslip,
    modifier = modifier.aspectRatio(1f)
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Payslip",
          modifier = Modifier.size(32.dp),
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Add Payslip",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun EmptyPayslipCard(
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.aspectRatio(1f),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Empty",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
      )
    }
  }
} 