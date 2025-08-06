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
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

/** Smart Loan navigation routes */
object SmartLoanRoutes {
  const val START_APPLICATION = "smart_loan_start"
  const val ID_CAPTURE = "smart_loan_id_capture"
  const val PAYSLIP_CAPTURE = "smart_loan_payslip_capture"
  const val LOAN_APPLICATION_CAPTURE = "smart_loan_application_capture"
  const val VALIDATION_PROGRESS = "smart_loan_validation"
  const val VALIDATION_REPORT = "smart_loan_validation_report"
  const val OFFER = "smart_loan_offer"
  const val ACCEPTANCE_SUCCESS = "smart_loan_acceptance_success"
}

/**
 * Smart Loan navigation graph
 */
@Composable
fun SmartLoanNavHost(
  navController: NavHostController,
  modelManagerViewModel: ModelManagerViewModel,
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
          navController.navigate(SmartLoanRoutes.LOAN_APPLICATION_CAPTURE)
        },
        onNavigateUp = { navController.navigateUp() },
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.LOAN_APPLICATION_CAPTURE) {
      LoanApplicationCaptureScreen(
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
          navController.navigate(SmartLoanRoutes.VALIDATION_REPORT) {
            // Clear back stack to prevent going back to validation
            popUpTo(SmartLoanRoutes.VALIDATION_PROGRESS) { inclusive = true }
          }
        },
        viewModel = viewModel,
        modelManagerViewModel = modelManagerViewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.VALIDATION_REPORT) {
      ValidationReportScreen(
        onContinue = {
          navController.navigate(SmartLoanRoutes.OFFER)
        },
        onNavigateUp = { navController.navigateUp() },
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.OFFER) {
      OfferScreen(
        onAcceptOffer = {
          navController.navigate(SmartLoanRoutes.ACCEPTANCE_SUCCESS) {
            popUpTo(SmartLoanRoutes.OFFER) { inclusive = true }
          }
        },
        onDeclineOffer = {
          // Navigate back to start for new application
          navController.navigate(SmartLoanRoutes.START_APPLICATION) {
            popUpTo(SmartLoanRoutes.START_APPLICATION) { inclusive = true }
          }
        },
        onNavigateUp = onNavigateUp,
        viewModel = viewModel,
      )
    }
    
    composable(route = SmartLoanRoutes.ACCEPTANCE_SUCCESS) {
      AcceptanceSuccessScreen(
        loanOffer = viewModel.uiState.value.loanOffer,
        disbursementMemo = viewModel.uiState.value.disbursementMemo,
        onExportMemo = { viewModel.exportDisbursementMemo() },
        onViewMemo = { viewModel.viewDisbursementMemo() },
        onNewApplication = {
          viewModel.startNewApplication()
          navController.navigate(SmartLoanRoutes.START_APPLICATION) {
            popUpTo(SmartLoanRoutes.START_APPLICATION) { inclusive = true }
          }
        },
        onBackToHome = onNavigateUp
      )
    }
  }
} 