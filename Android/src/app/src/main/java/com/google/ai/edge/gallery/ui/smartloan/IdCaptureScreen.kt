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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * ID Capture Screen - For capturing front and back ID images
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCaptureScreen(
  onContinue: () -> Unit,
  onNavigateUp: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Capture ID Documents") },
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
        text = "Please capture clear photos of both sides of your ID document",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      
      // Camera Preview Placeholder (would be actual camera in real implementation)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Camera",
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Camera Preview",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp)
            )
          }
        }
      }
      
      // ID Capture Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        IdCaptureCard(
          title = "Front ID",
          isCompleted = uiState.idImages.frontImage != null,
          onCapture = {
            // In real implementation, this would open camera and capture
            // For now, we'll use a mock URI
            val mockUri = Uri.parse("content://mock/front_id")
            viewModel.captureIdFront(mockUri)
          },
          modifier = Modifier.weight(1f)
        )
        
        IdCaptureCard(
          title = "Back ID",
          isCompleted = uiState.idImages.backImage != null,
          onCapture = {
            // In real implementation, this would open camera and capture
            // For now, we'll use a mock URI
            val mockUri = Uri.parse("content://mock/back_id")
            viewModel.captureIdBack(mockUri)
          },
          modifier = Modifier.weight(1f)
        )
      }
      
      Spacer(modifier = Modifier.weight(1f))
      
      // Continue Button
      Button(
        onClick = onContinue,
        enabled = viewModel.isIdCaptureComplete(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Continue to Payslips")
      }
    }
  }
}

@Composable
private fun IdCaptureCard(
  title: String,
  isCompleted: Boolean,
  onCapture: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = if (isCompleted) 
        MaterialTheme.colorScheme.primaryContainer 
      else 
        MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )
      
      if (isCompleted) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Completed",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      } else {
        OutlinedButton(
          onClick = onCapture,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Capture",
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Capture",
            modifier = Modifier.padding(start = 4.dp)
          )
        }
      }
    }
  }
} 