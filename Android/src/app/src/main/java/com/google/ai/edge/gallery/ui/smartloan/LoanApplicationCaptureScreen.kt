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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState

/**
 * Screen for capturing loan application form documents (front and back pages)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationCaptureScreen(
  onContinue: () -> Unit,
  onNavigateUp: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var showConfirmDialog by remember { mutableStateOf<String?>(null) }
  var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
  var showImageSourceDialog by remember { mutableStateOf<SmartLoanImageType?>(null) }

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

  // Image selectors for front and back
  val frontFormSelector = rememberSmartLoanImageSelector(
    SmartLoanImageType.LOAN_APPLICATION_FRONT
  ) { result ->
    when (result) {
      is CameraResult.Success -> viewModel.captureLoanApplicationImage(true, result.bitmap)
      is CameraResult.Error -> cameraErrorMessage = result.message
      CameraResult.Cancelled -> { /* User cancelled */ }
    }
  }

  val backFormSelector = rememberSmartLoanImageSelector(
    SmartLoanImageType.LOAN_APPLICATION_BACK
  ) { result ->
    when (result) {
      is CameraResult.Success -> viewModel.captureLoanApplicationImage(false, result.bitmap)
      is CameraResult.Error -> cameraErrorMessage = result.message
      CameraResult.Cancelled -> { /* User cancelled */ }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Loan Application Form") },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      // Continue button
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
      ) {
        Button(
          onClick = onContinue,
          enabled = uiState.applicationData.isLoanApplicationCaptureComplete(),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Continue to Processing")
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Header section
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Loan Application Form",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Capture both pages of your completed loan application form",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
          )
        }
      }

      // Progress indicator
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val capturedCount = uiState.applicationData.loanApplicationImages.getCapturedCount()
        Text(
          text = "Progress: $capturedCount/2 pages captured",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
          progress = { capturedCount / 2f },
          modifier = Modifier.width(100.dp),
        )
      }

      // Front page capture section
      LoanApplicationImageCaptureCard(
        title = "Front Page",
        subtitle = "Applicant details, loan request, and signatures",
        imageType = SmartLoanImageType.LOAN_APPLICATION_FRONT,
        capturedImage = uiState.applicationData.loanApplicationImages.frontForm,
        onCaptureClick = { 
          showImageSourceDialog = SmartLoanImageType.LOAN_APPLICATION_FRONT
        },
        onDeleteClick = {
          showConfirmDialog = "front"
        }
      )

      // Back page capture section
      LoanApplicationImageCaptureCard(
        title = "Back Page",
        subtitle = "Terms and conditions, interest rates, and legal clauses",
        imageType = SmartLoanImageType.LOAN_APPLICATION_BACK,
        capturedImage = uiState.applicationData.loanApplicationImages.backForm,
        onCaptureClick = { 
          showImageSourceDialog = SmartLoanImageType.LOAN_APPLICATION_BACK
        },
        onDeleteClick = {
          showConfirmDialog = "back"
        }
      )

      // Requirements card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "Requirements for Good Capture:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(8.dp))
          
          val requirements = listOf(
            "• Ensure all text is clearly readable",
            "• Form should be completely filled out",
            "• All signatures should be visible",
            "• Good lighting with no shadows",
            "• Hold phone steady to avoid blur",
            "• Capture the entire page within frame"
          )
          
          requirements.forEach { requirement ->
            Text(
              text = requirement,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
        }
      }
    }
  }

  // Image source selection dialog
  showImageSourceDialog?.let { imageType ->
    ImageSourceSelectionDialog(
      imageType = imageType,
      onSourceSelected = { source ->
        when (imageType) {
          SmartLoanImageType.LOAN_APPLICATION_FRONT -> frontFormSelector(source)
          SmartLoanImageType.LOAN_APPLICATION_BACK -> backFormSelector(source)
          else -> { /* Handle other types if needed */ }
        }
        showImageSourceDialog = null
      },
      onDismiss = { showImageSourceDialog = null }
    )
  }

  // Delete confirmation dialog
  showConfirmDialog?.let { page ->
    AlertDialog(
      onDismissRequest = { showConfirmDialog = null },
      title = { Text("Delete Image") },
      text = { Text("Are you sure you want to delete the $page page image? You'll need to capture it again.") },
      confirmButton = {
        TextButton(
          onClick = {
            when (page) {
              "front" -> viewModel.deleteLoanApplicationImage(true)
              "back" -> viewModel.deleteLoanApplicationImage(false)
            }
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
} 

/**
 * Composable card for capturing loan application form images
 */
@Composable
private fun LoanApplicationImageCaptureCard(
  title: String,
  subtitle: String,
  imageType: SmartLoanImageType,
  capturedImage: android.graphics.Bitmap?,
  onCaptureClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        
        // Status indicator
        if (capturedImage != null) {
          Icon(
            Icons.Default.Check,
            contentDescription = "Captured",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Image preview or capture button
      if (capturedImage != null) {
        // Show captured image with action buttons
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          // In a real implementation, you'd show the actual image here
          // For now, showing a placeholder with success indication
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.Description,
              contentDescription = null,
              modifier = Modifier.size(32.dp),
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "✓ Captured",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onCaptureClick,
            modifier = Modifier.weight(1f)
          ) {
            Text("Retake")
          }
          OutlinedButton(
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f)
          ) {
            Text("Delete")
          }
        }
      } else {
        // Show capture button
        Button(
          onClick = onCaptureClick,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            Icons.Default.Description,
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add ${title}")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Instructions
        Text(
          text = getImageCaptureInstructions(imageType),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
} 