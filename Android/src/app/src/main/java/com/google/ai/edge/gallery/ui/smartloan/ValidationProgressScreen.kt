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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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

/**
 * Validation Progress Screen - Shows AI processing progress and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidationProgressScreen(
  onValidationComplete: () -> Unit,
  viewModel: SmartLoanViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  
  // Start validation when screen is first displayed
  LaunchedEffect(Unit) {
    viewModel.startValidation()
  }
  
  // Navigate to offer screen when validation is complete
  LaunchedEffect(uiState.isValidating) {
    if (!uiState.isValidating && uiState.validationProgress >= 1f) {
      onValidationComplete()
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
      Text(
        text = uiState.validationStatus,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 24.dp)
      )
      
      // Progress Bar
      LinearProgressIndicator(
        progress = { uiState.validationProgress },
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
      )
      
      // Progress Percentage
      Text(
        text = "${(uiState.validationProgress * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium
      )
      
      // Progress Steps
      Column(
        modifier = Modifier.padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ProcessingStep(
          title = "ID Document Analysis",
          isCompleted = uiState.validationProgress > 0.25f,
          isActive = uiState.validationProgress in 0f..0.25f
        )
        ProcessingStep(
          title = "Personal Information Extraction",
          isCompleted = uiState.validationProgress > 0.5f,
          isActive = uiState.validationProgress in 0.25f..0.5f
        )
        ProcessingStep(
          title = "Payslip Processing",
          isCompleted = uiState.validationProgress > 0.75f,
          isActive = uiState.validationProgress in 0.5f..0.75f
        )
        ProcessingStep(
          title = "Loan Eligibility Calculation",
          isCompleted = uiState.validationProgress >= 1f,
          isActive = uiState.validationProgress in 0.75f..1f
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
  
  Text(
    text = "$icon $title",
    style = MaterialTheme.typography.bodyMedium,
    color = textColor,
    modifier = modifier
  )
} 