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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * ID Capture Screen - For capturing front and back ID images with real camera functionality
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
  val snackbarHostState = remember { SnackbarHostState() }
  var showConfirmDialog by remember { mutableStateOf<String?>(null) }

  // Show error messages
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearError()
    }
  }

  // State for camera error messages
  var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

  // Show camera error messages
  LaunchedEffect(cameraErrorMessage) {
    cameraErrorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      cameraErrorMessage = null
    }
  }

  // Image selection states
  var showFrontIdSourceDialog by remember { mutableStateOf(false) }
  var showBackIdSourceDialog by remember { mutableStateOf(false) }

  // Image selectors for front and back ID
  val frontIdSelector = rememberSmartLoanImageSelector(
    imageType = SmartLoanImageType.ID_FRONT
  ) { result ->
    when (result) {
      is CameraResult.Success -> viewModel.captureIdFront(result.bitmap)
      is CameraResult.Error -> cameraErrorMessage = result.message
      CameraResult.Cancelled -> { /* User cancelled */ }
    }
  }

  val backIdSelector = rememberSmartLoanImageSelector(
    imageType = SmartLoanImageType.ID_BACK
  ) { result ->
    when (result) {
      is CameraResult.Success -> viewModel.captureIdBack(result.bitmap)
      is CameraResult.Error -> cameraErrorMessage = result.message
      CameraResult.Cancelled -> { /* User cancelled */ }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Capture ID Documents") },
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
        text = "Please capture clear photos of both sides of your ID document",
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
      
      // ID Capture Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
                 IdCaptureCard(
           title = "Front ID",
           bitmap = uiState.applicationData.idImages.frontId,
           instructions = getImageCaptureInstructions(SmartLoanImageType.ID_FRONT),
           onCapture = { showFrontIdSourceDialog = true },
           onDelete = { showConfirmDialog = "id_front" },
           modifier = Modifier.weight(1f)
         )
         
         IdCaptureCard(
           title = "Back ID", 
           bitmap = uiState.applicationData.idImages.backId,
           instructions = getImageCaptureInstructions(SmartLoanImageType.ID_BACK),
           onCapture = { showBackIdSourceDialog = true },
           onDelete = { showConfirmDialog = "id_back" },
           modifier = Modifier.weight(1f)
         )
      }
      
      // Progress indicator
      val capturedCount = listOfNotNull(
        uiState.applicationData.idImages.frontId,
        uiState.applicationData.idImages.backId
      ).size
      
      Text(
        text = "Progress: $capturedCount / 2 ID images captured",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Continue Button
      Button(
        onClick = onContinue,
        enabled = viewModel.isIdCaptureComplete() && !uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Continue to Payslips")
      }
    }
  }

  // Confirmation dialog for image deletion
  showConfirmDialog?.let { imageType ->
    AlertDialog(
      onDismissRequest = { showConfirmDialog = null },
      title = { Text("Delete Image") },
      text = { Text("Are you sure you want to delete this image? You'll need to retake it.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteImage(imageType)
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

  // Image source selection dialogs
  if (showFrontIdSourceDialog) {
    ImageSourceSelectionDialog(
      imageType = SmartLoanImageType.ID_FRONT,
      onSourceSelected = frontIdSelector,
      onDismiss = { showFrontIdSourceDialog = false }
    )
  }

  if (showBackIdSourceDialog) {
    ImageSourceSelectionDialog(
      imageType = SmartLoanImageType.ID_BACK,
      onSourceSelected = backIdSelector,
      onDismiss = { showBackIdSourceDialog = false }
    )
  }
}

@Composable
private fun IdCaptureCard(
  title: String,
  bitmap: Bitmap?,
  instructions: String,
  onCapture: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = if (bitmap != null) 
        MaterialTheme.colorScheme.primaryContainer 
      else 
        MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )
      
      if (bitmap != null) {
        // Show captured image thumbnail
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$title captured",
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(
                2.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(8.dp)
              )
              .clickable { onCapture() } // Allow retake by clicking image
          )
          
          // Action buttons overlay
          Row(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // Retake button
            IconButton(
              onClick = onCapture,
              modifier = Modifier
                .size(32.dp)
                .background(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                  RoundedCornerShape(16.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retake",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
            
            // Delete button
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .size(32.dp)
                .background(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                  RoundedCornerShape(16.dp)
                )
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
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
              .padding(4.dp)
              .size(24.dp)
              .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
              )
              .padding(2.dp)
          )
        }
      } else {
        // Show capture button and instructions
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Camera placeholder
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .border(
                2.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
              )
              .clickable { onCapture() },
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
                             Text(
                 text = "Tap to add image",
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
               )
            }
          }

                     // Add image button
           OutlinedButton(
             onClick = onCapture,
             modifier = Modifier.fillMaxWidth()
           ) {
             Icon(
               imageVector = Icons.Default.CameraAlt,
               contentDescription = "Add Image",
               modifier = Modifier.size(16.dp)
             )
             Spacer(modifier = Modifier.width(4.dp))
             Text("Add Image")
           }
        }
      }
      
      // Instructions text
      Text(
        text = instructions,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
} 