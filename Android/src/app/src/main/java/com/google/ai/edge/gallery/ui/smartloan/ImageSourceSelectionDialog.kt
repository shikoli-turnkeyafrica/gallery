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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Image source selection dialog that presents options for camera, gallery, or file selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceSelectionDialog(
  imageType: SmartLoanImageType,
  onSourceSelected: (ImageSource) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Title
      Text(
        text = "Add ${imageType.displayName}",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      
      Text(
        text = "Choose how you'd like to add your image",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      // Options
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Camera Option
        ImageSourceOption(
          icon = { 
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = "Camera",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          title = "Take Photo",
          subtitle = "Use camera to capture a new image",
          onClick = { 
            onSourceSelected(ImageSource.CAMERA)
            onDismiss()
          }
        )

        // Gallery Option
        ImageSourceOption(
          icon = { 
            Icon(
              imageVector = Icons.Default.Photo,
              contentDescription = "Gallery",
              tint = MaterialTheme.colorScheme.secondary
            )
          },
          title = "Choose from Gallery",
          subtitle = "Select an existing photo from your gallery",
          onClick = { 
            onSourceSelected(ImageSource.GALLERY)
            onDismiss()
          }
        )

        // Files Option
        ImageSourceOption(
          icon = { 
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = "Files",
              tint = MaterialTheme.colorScheme.tertiary
            )
          },
          title = "Browse Files",
          subtitle = "Pick an image or document file",
          onClick = { 
            onSourceSelected(ImageSource.FILES)
            onDismiss()
          }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Cancel button
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Cancel")
      }
    }
  }
}

@Composable
private fun ImageSourceOption(
  icon: @Composable () -> Unit,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedCard(
    onClick = onClick,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Icon
      icon()
      
      Spacer(modifier = Modifier.width(16.dp))
      
      // Text content
      Column(
        modifier = Modifier.weight(1f)
      ) {
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
    }
  }
} 