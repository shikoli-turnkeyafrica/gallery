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

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.google.ai.edge.gallery.data.TASK_SMART_LOAN
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.data.ModelDownloadStatusType

/**
 * Validation Progress Screen - Shows AI processing progress and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidationProgressScreen(
  onValidationComplete: () -> Unit,
  viewModel: SmartLoanViewModel,
  modelManagerViewModel: ModelManagerViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val context = LocalContext.current
  
  // Get the first available model from TASK_SMART_LOAN (like Ask Image does)
  val selectedModel = if (TASK_SMART_LOAN.models.isNotEmpty()) {
    TASK_SMART_LOAN.models[0]
  } else {
    null
  }
  
  // Initialize model when download status is SUCCEEDED (same as Ask Image)
  LaunchedEffect(selectedModel) {
    if (selectedModel != null) {
      val downloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
      if (downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED) {
        android.util.Log.d("ValidationProgressScreen", "Initializing model '${selectedModel.name}' (Ask Image approach)")
        modelManagerViewModel.initializeModel(context, task = TASK_SMART_LOAN, model = selectedModel)
      }
    }
  }
  
  // Start AI extraction when model is initialized (same as Ask Image)
  LaunchedEffect(selectedModel?.instance) {
    if (selectedModel != null && selectedModel.instance != null) {
      android.util.Log.d("ValidationProgressScreen", "Model initialized, starting extraction with: ${selectedModel.name}")
      viewModel.startDataExtractionWithModel(selectedModel)
    }
  }
  
  // Navigate to offer screen when extraction is complete
  LaunchedEffect(uiState.extractionProgress, uiState.extractedData) {
    if (uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == true) {
      // Add a brief delay to show 100% completion before navigation
      delay(2000) // 2 second delay to show success state
      onValidationComplete()
    }
  }
  
  // Show post-completion feedback if extraction is done but data is incomplete
  LaunchedEffect(uiState.extractionProgress, uiState.extractedData) {
    if (uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == false) {
      // Data extracted but needs manual review - wait a bit then navigate anyway
      delay(3000) // 3 second delay to show completion message
      onValidationComplete() // Navigate to review screen anyway
    }
  }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Processing Application") }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      
      // AI Processing Animation
      val infiniteTransition = rememberInfiniteTransition(label = "ai_processing")
      val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
          animation = tween(2000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
      )
      
      Card(
        modifier = Modifier
          .size(120.dp)
          .padding(bottom = 32.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        )
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Processing",
            modifier = Modifier
              .size(64.dp)
              .rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
      
      // Title
      Text(
        text = "AI Processing Your Documents",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
      )
      
      // Status Message
      val statusMessage = when {
        uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == true -> "✅ Documents processed successfully! Preparing your loan offer..."
        uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == false -> "⚠️ Extraction complete! Some data may need manual review..."
        uiState.extractionStatus.isNotBlank() -> uiState.extractionStatus
        else -> "Initializing AI model..."
      }
      
      Text(
        text = statusMessage,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = when {
          uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == true -> MaterialTheme.colorScheme.primary
          uiState.extractionProgress >= 1f && uiState.extractedData?.isComplete == false -> MaterialTheme.colorScheme.tertiary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(bottom = 8.dp)
      )
      
      // Error Message (if any)
      uiState.extractionError?.let { error ->
        Text(
          text = error,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }
      
      // Animated Progress Bar
      val animatedProgress by animateFloatAsState(
        targetValue = uiState.extractionProgress,
        animationSpec = tween(
          durationMillis = 800, // Smooth 800ms transition
          easing = EaseOutCubic
        ),
        label = "progress_animation"
      )
      
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
      )
      
      // Progress Percentage
      Text(
        text = "${(animatedProgress * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
      )
      
      // Confidence Score (if available)
      uiState.extractedData?.let { data ->
        if (data.overallConfidence > 0f) {
          Text(
            text = "Extraction Confidence: ${(data.overallConfidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = if (data.overallConfidence > 0.8f) 
              MaterialTheme.colorScheme.primary 
            else 
              MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }
      
      // AI Extraction Steps
      Column(
        modifier = Modifier.padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ProcessingStep(
          title = "Initializing AI Vision Model",
          isCompleted = uiState.extractionProgress > 0.1f,
          isActive = uiState.extractionProgress in 0f..0.1f,
          details = if (uiState.extractionProgress <= 0.1f) "Loading Gemma 3n model..." else null
        )
        ProcessingStep(
          title = "Processing ID Documents",
          isCompleted = uiState.extractionProgress > 0.3f,
          isActive = uiState.extractionProgress in 0.1f..0.3f,
          details = uiState.extractedData?.idCardData?.let { id ->
            if (id.isValid) "✓ Extracted: ${id.fullName}" else null
          }
        )
        ProcessingStep(
          title = "Extracting Payslip Data",
          isCompleted = uiState.extractionProgress > 0.9f,
          isActive = uiState.extractionProgress in 0.3f..0.9f,
          details = uiState.extractedData?.let { data ->
            val validPayslips = data.payslipData.count { it.isValid }
            if (validPayslips > 0) "✓ Processed $validPayslips payslip(s)" else null
          }
        )
        ProcessingStep(
          title = "Validating Extracted Data",
          isCompleted = uiState.extractionProgress >= 1f,
          isActive = uiState.extractionProgress in 0.9f..1f,
          details = uiState.extractedData?.let { data ->
            if (data.isComplete) "✓ Ready for loan assessment" else "Checking data consistency..."
          }
        )
      }
    }
  }
}

@Composable
private fun ProcessingStep(
  title: String,
  isCompleted: Boolean,
  isActive: Boolean,
  details: String? = null,
  modifier: Modifier = Modifier,
) {
  val textColor = when {
    isCompleted -> MaterialTheme.colorScheme.primary
    isActive -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  
  val icon = when {
    isCompleted -> "✅"
    isActive -> "🔄"
    else -> "⏳"
  }
  
  Column(
    modifier = modifier
  ) {
    Text(
      text = "$icon $title",
      style = MaterialTheme.typography.bodyMedium,
      color = textColor
    )
    
    // Show details if available
    details?.let { detailText ->
      Text(
        text = detailText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
      )
    }
  }
} 