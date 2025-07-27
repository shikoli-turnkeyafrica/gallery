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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/** Smart Loan navigation routes */
object SmartLoanRoutes {
  const val START_APPLICATION = "smart_loan_start"
  const val ID_CAPTURE = "smart_loan_id_capture"
  const val PAYSLIP_CAPTURE = "smart_loan_payslip_capture"
  const val VALIDATION_PROGRESS = "smart_loan_validation"
  const val OFFER = "smart_loan_offer"
}

/**
 * Smart Loan navigation graph
 */
@Composable
fun SmartLoanNavHost(
  navController: NavHostController,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: SmartLoanViewModel = hiltViewModel()

  NavHost(
    navController = navController,
    startDestination = SmartLoanRoutes.START_APPLICATION,
    modifier = modifier,
  ) {
    
    composable(route = SmartLoanRoutes.START_APPLICATION) {
      StartApplicationScreen(
        onStartApplication = {
          navController.navigate(SmartLoanRoutes.ID_CAPTURE)
        },
        onNavigateUp = onNavigateUp,
      )
    }
    
    composable(route = SmartLoanRoutes.ID_CAPTURE) {
      IdCaptureScreen(
        onContinue = {
          navController.navigate(SmartLoanRoutes.PAYSLIP_CAPTURE)
        },
        onNavigateUp = { navController.navigateUp() },
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.PAYSLIP_CAPTURE) {
      PayslipCaptureScreen(
        onContinue = {
          navController.navigate(SmartLoanRoutes.VALIDATION_PROGRESS)
        },
        onNavigateUp = { navController.navigateUp() },
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.VALIDATION_PROGRESS) {
      ValidationProgressScreen(
        onValidationComplete = {
          navController.navigate(SmartLoanRoutes.OFFER) {
            // Clear back stack to prevent going back to validation
            popUpTo(SmartLoanRoutes.VALIDATION_PROGRESS) { inclusive = true }
          }
        },
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.OFFER) {
      OfferScreen(
        onAcceptOffer = {
          // In real app, this would handle offer acceptance
          onNavigateUp()
        },
        onDeclineOffer = {
          // In real app, this would handle offer decline  
          onNavigateUp()
        },
        onNavigateUp = onNavigateUp,
        viewModel = viewModel,
      )
    }
  }
} 