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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Start Application Screen - Welcome screen for Smart Loan application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartApplicationScreen(
  onStartApplication: () -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Smart Loan Demo") },
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
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      
      // App Icon
      Icon(
        imageVector = Icons.Outlined.AccountBalance,
        contentDescription = "Smart Loan",
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      
      // Title
      Text(
        text = "Smart Loan Application",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp)
      )
      
      // Description
      Text(
        text = "Apply for a loan using AI-powered document processing. Simply capture your ID and payslip photos, and get an instant loan offer.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
      )
      
      // Features list
      Column(
        modifier = Modifier.padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FeatureItem("📱 Offline AI Processing")
        FeatureItem("🆔 Instant ID Verification")
        FeatureItem("💰 Quick Loan Assessment")
        FeatureItem("🔒 Secure & Private")
      }
      
      // Start Button
      Button(
        onClick = onStartApplication,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Start Application")
      }
    }
  }
}

@Composable
private fun FeatureItem(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.padding(vertical = 2.dp)
  )
} 