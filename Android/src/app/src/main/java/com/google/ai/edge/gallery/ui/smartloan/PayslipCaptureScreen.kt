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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Payslip Capture Screen - For capturing up to 4 payslip images with real camera functionality
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
  val snackbarHostState = remember { SnackbarHostState() }
  var showConfirmDialog by remember { mutableStateOf<Int?>(null) }
  var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
  var showImageSourceDialog by remember { mutableStateOf<Pair<Int, SmartLoanImageType>?>(null) }

  // Show error messages
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearError()
    }
  }

  // Show camera error messages
  LaunchedEffect(cameraErrorMessage) {
    cameraErrorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      cameraErrorMessage = null
    }
  }

  // Image types for each payslip slot
  val imageTypes = remember {
    listOf(
      SmartLoanImageType.PAYSLIP_1,
      SmartLoanImageType.PAYSLIP_2,
      SmartLoanImageType.PAYSLIP_3,
      SmartLoanImageType.PAYSLIP_4
    )
  }

  // Pre-create image selectors for each payslip
  val imageSelectors = remember {
    imageTypes.mapIndexed { index, imageType ->
      index to imageType
    }
  }.associate { (index, imageType) ->
    imageType to rememberSmartLoanImageSelector(imageType) { result ->
      when (result) {
        is CameraResult.Success -> viewModel.capturePayslipImage(index, result.bitmap)
        is CameraResult.Error -> cameraErrorMessage = result.message
        CameraResult.Cancelled -> { /* User cancelled */ }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Capture Payslips") },
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
    snackbarHost = { SnackbarHost(snackbarHostState) },
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
      
      // Instructions
      Text(
        text = "Capture photos of your recent payslips (at least 1, up to 4)",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      // Loading indicator
      if (uiState.isLoading) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Processing image...",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
      
      // Progress indicator
      val capturedCount = uiState.applicationData.payslipImages.getCapturedCount()
      Text(
        text = "$capturedCount of 4 payslips captured",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      // Progress bar
      LinearProgressIndicator(
        progress = { capturedCount / 4f },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
      )
      
      // Payslip Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp) // Fixed height for grid
      ) {
        items(4) { index ->
          val bitmap = uiState.applicationData.payslipImages.getPayslipByIndex(index)
          val imageType = imageTypes[index]
          
          PayslipImageCard(
            index = index,
            bitmap = bitmap,
            instructions = getImageCaptureInstructions(imageType),
            onCapture = { showImageSourceDialog = Pair(index, imageType) },
            onDelete = { showConfirmDialog = index }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      
      // Continue Button
      Button(
        onClick = onContinue,
        enabled = viewModel.hasMinimumPayslips() && !uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          if (viewModel.isPayslipCaptureComplete()) {
            "Continue to Validation (All payslips captured)"
          } else {
            "Continue to Validation (${capturedCount}/4 captured)"
          }
        )
      }
    }
  }

  // Confirmation dialog for image deletion
  showConfirmDialog?.let { index ->
    AlertDialog(
      onDismissRequest = { showConfirmDialog = null },
      title = { Text("Delete Payslip") },
      text = { Text("Are you sure you want to delete payslip ${index + 1}? You'll need to retake it.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteImage("payslip", index)
            showConfirmDialog = null
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Image source selection dialog
  showImageSourceDialog?.let { (index, imageType) ->
    ImageSourceSelectionDialog(
      imageType = imageType,
      onSourceSelected = imageSelectors[imageType] ?: { },
      onDismiss = { showImageSourceDialog = null }
    )
  }
}

@Composable
private fun PayslipImageCard(
  index: Int,
  bitmap: Bitmap?,
  instructions: String,
  onCapture: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.aspectRatio(1f),
    colors = CardDefaults.cardColors(
      containerColor = if (bitmap != null) 
        MaterialTheme.colorScheme.primaryContainer 
      else 
        MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Title
      Text(
        text = "Payslip ${index + 1}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )
      
      if (bitmap != null) {
        // Show captured image thumbnail
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Payslip ${index + 1} captured",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(4.dp))
              .border(
                1.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(4.dp)
              )
              .clickable { onCapture() } // Allow retake by clicking image
          )
          
          // Action buttons overlay
          Row(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            // Retake button
            IconButton(
              onClick = onCapture,
              modifier = Modifier
                .size(24.dp)
                .background(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                  RoundedCornerShape(12.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retake",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
              )
            }
            
            // Delete button
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .size(24.dp)
                .background(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                  RoundedCornerShape(12.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp)
              )
            }
          }
          
          // Success indicator
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(2.dp)
              .size(16.dp)
              .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
              )
              .padding(1.dp)
          )
        }
      } else {
        // Show capture button and placeholder
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              RoundedCornerShape(4.dp)
            )
            .clickable { onCapture() },
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Camera",
              modifier = Modifier.size(24.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
                           Text(
                 text = "Tap to add image",
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                 textAlign = TextAlign.Center
               )
          }
        }
      }
      
      // Instructions text (condensed for grid)
      Text(
        text = if (bitmap != null) "Captured" else "Required",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = if (bitmap != null) 
          MaterialTheme.colorScheme.primary 
        else 
          MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
} 