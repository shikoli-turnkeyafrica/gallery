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

package com.google.ai.edge.gallery.ui.smartloan.extraction

import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.data.IdCardData
import com.google.ai.edge.gallery.ui.smartloan.data.PayslipData
import com.google.ai.edge.gallery.ui.smartloan.data.LoanApplicationFormData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.JsonElement

private const val TAG = "DataExtractionFunctions"

/**
 * Function calling interface for AI-powered data extraction from Smart Loan documents.
 * These functions provide structured output for the AI model to populate with extracted data.
 */
object DataExtractionFunctions {
    
    /**
     * Function declaration for extracting ID card data.
     * The AI model will call this function with extracted information from Kenyan National ID.
     */
    const val EXTRACT_ID_CARD_FUNCTION = """
    {
        "name": "extractIdCardData",
        "description": "Extract structured information from a Kenyan National ID card image",
        "parameters": {
            "type": "object",
            "properties": {
                "fullName": {
                    "type": "string",
                    "description": "The full name as printed on the ID card"
                },
                "idNumber": {
                    "type": "string", 
                    "description": "The 8-digit national ID number"
                },
                "dateOfBirth": {
                    "type": "string",
                    "description": "Date of birth in YYYY-MM-DD format"
                },
                "expiryDate": {
                    "type": "string",
                    "description": "ID expiry date in YYYY-MM-DD format, empty if not visible"
                },
                "placeOfBirth": {
                    "type": "string",
                    "description": "Place of birth if visible on the ID, empty if not found"
                },
                "confidence": {
                    "type": "number",
                    "description": "Confidence score between 0.0 and 1.0 based on image clarity and text readability",
                    "minimum": 0.0,
                    "maximum": 1.0
                }
            },
            "required": ["fullName", "idNumber", "dateOfBirth", "confidence"]
        }
    }
    """
    
    /**
     * Function declaration for extracting payslip data.
     * The AI model will call this function with extracted salary and employment information.
     */
    const val EXTRACT_PAYSLIP_FUNCTION = """
    {
        "name": "extractPayslipData",
        "description": "Extract structured salary and employment information from a payslip image",
        "parameters": {
            "type": "object",
            "properties": {
                "employeeName": {
                    "type": "string",
                    "description": "Employee name as shown on the payslip"
                },
                "employerName": {
                    "type": "string",
                    "description": "Company or employer name"
                },
                "grossSalary": {
                    "type": "number",
                    "description": "Gross salary amount (before deductions)",
                    "minimum": 0
                },
                "netSalary": {
                    "type": "number", 
                    "description": "Net salary amount (after deductions)",
                    "minimum": 0
                },
                "payPeriod": {
                    "type": "string",
                    "description": "Pay period in YYYY-MM format (e.g., '2024-01' for January 2024)"
                },
                "deductions": {
                    "type": "object",
                    "description": "Map of deduction types to amounts",
                    "additionalProperties": {
                        "type": "number"
                    }
                },
                "allowances": {
                    "type": "object", 
                    "description": "Map of allowance types to amounts",
                    "additionalProperties": {
                        "type": "number"
                    }
                },
                "confidence": {
                    "type": "number",
                    "description": "Confidence score between 0.0 and 1.0 based on image clarity and data completeness",
                    "minimum": 0.0,
                    "maximum": 1.0
                }
            },
            "required": ["employeeName", "employerName", "grossSalary", "netSalary", "payPeriod", "confidence"]
        }
    }
    """
    
    /**
     * Function declaration for extracting loan application form data.
     * The AI model will call this function with extracted information from loan application forms.
     */
    const val EXTRACT_LOAN_APPLICATION_FUNCTION = """
    {
        "name": "extractLoanApplicationData",
        "description": "Extract structured information from a loan application form",
        "parameters": {
            "type": "object",
            "properties": {
                "title": {
                    "type": "string",
                    "description": "Title (Mr., Ms., Mrs., Dr., etc.)"
                },
                "firstName": {
                    "type": "string",
                    "description": "First name of the applicant"
                },
                "middleName": {
                    "type": "string", 
                    "description": "Middle name of the applicant"
                },
                "lastName": {
                    "type": "string",
                    "description": "Last name of the applicant"
                },
                "idPassportNumber": {
                    "type": "string",
                    "description": "ID or passport number"
                },
                "dateOfBirth": {
                    "type": "string",
                    "description": "Date of birth in DD-MMM-YYYY format"
                },
                "telephoneMobile": {
                    "type": "string",
                    "description": "Phone number"
                },
                "emailAddress": {
                    "type": "string",
                    "description": "Email address"
                },
                "residentialAddress": {
                    "type": "string",
                    "description": "Residential address"
                },
                "townCity": {
                    "type": "string",
                    "description": "Town or city"
                },
                "bankName": {
                    "type": "string",
                    "description": "Bank name"
                },
                "accountName": {
                    "type": "string",
                    "description": "Bank account name"
                },
                "accountNumber": {
                    "type": "string",
                    "description": "Bank account number"
                },
                "requestedLoanAmount": {
                    "type": "number",
                    "description": "Requested loan amount",
                    "minimum": 0
                },
                "requestedInstallmentAmount": {
                    "type": "number",
                    "description": "Requested monthly installment amount",
                    "minimum": 0
                },
                "requestedLoanPeriodMonths": {
                    "type": "number",
                    "description": "Requested loan period in months",
                    "minimum": 1
                },
                "disbursementMode": {
                    "type": "string",
                    "description": "Disbursement mode (M-PESA, TT, RTGS)"
                },
                "clientMPesaNumber": {
                    "type": "string",
                    "description": "Client M-PESA number if applicable"
                },
                "applicantSignatureDate": {
                    "type": "string",
                    "description": "Date of applicant signature"
                },
                "confidence": {
                    "type": "number",
                    "description": "Confidence score between 0.0 and 1.0 based on form completeness and clarity",
                    "minimum": 0.0,
                    "maximum": 1.0
                }
            },
            "required": ["firstName", "lastName", "requestedLoanAmount", "confidence"]
        }
    }
    """
    
    /**
     * Parses the AI model's function call arguments for ID card extraction
     */
    fun parseIdCardFunction(args: JsonObject): IdCardData {
        return try {
            Log.d(TAG, "=== PARSING ID CARD FUNCTION ARGS ===")
            Log.d(TAG, "Raw args: $args")
            
            val fullName = (args["fullName"] as? JsonPrimitive)?.content ?: ""
            val idNumber = (args["idNumber"] as? JsonPrimitive)?.content ?: "" 
            val dateOfBirth = (args["dateOfBirth"] as? JsonPrimitive)?.content ?: ""
            val expiryDate = (args["expiryDate"] as? JsonPrimitive)?.content ?: ""
            val placeOfBirth = (args["placeOfBirth"] as? JsonPrimitive)?.content ?: ""
            val confidence = (args["confidence"] as? JsonPrimitive)?.float ?: 0.0f
            
            Log.d(TAG, "Parsed values:")
            Log.d(TAG, "  fullName: '$fullName'")
            Log.d(TAG, "  idNumber: '$idNumber'")
            Log.d(TAG, "  dateOfBirth: '$dateOfBirth'")
            Log.d(TAG, "  expiryDate: '$expiryDate'")
            Log.d(TAG, "  placeOfBirth: '$placeOfBirth'")
            Log.d(TAG, "  confidence: $confidence")
            
            val result = IdCardData(
                fullName = fullName,
                idNumber = idNumber,
                dateOfBirth = dateOfBirth,
                expiryDate = expiryDate,
                placeOfBirth = placeOfBirth,
                extractionConfidence = confidence,
                isValid = false // Will be validated separately
            ).validate()
            
            Log.d(TAG, "Final ID result after validation: $result")
            Log.d(TAG, "=== END ID FUNCTION PARSING ===")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception in parseIdCardFunction: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            IdCardData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Parses the AI model's function call arguments for payslip extraction
     */
    fun parsePayslipFunction(args: JsonObject): PayslipData {
        return try {
            Log.d(TAG, "=== PARSING PAYSLIP FUNCTION ARGS ===")
            Log.d(TAG, "Raw args: $args")
            
            val employeeName = (args["employeeName"] as? JsonPrimitive)?.content ?: ""
            val employerName = (args["employerName"] as? JsonPrimitive)?.content ?: ""
            val grossSalary = (args["grossSalary"] as? JsonPrimitive)?.double ?: 0.0
            val netSalary = (args["netSalary"] as? JsonPrimitive)?.double ?: 0.0
            val payPeriod = (args["payPeriod"] as? JsonPrimitive)?.content ?: ""
            val deductions = parseMoneyMap(args["deductions"])
            val allowances = parseMoneyMap(args["allowances"])
            val confidence = (args["confidence"] as? JsonPrimitive)?.float ?: 0.0f
            
            Log.d(TAG, "Parsed values:")
            Log.d(TAG, "  employeeName: '$employeeName'")
            Log.d(TAG, "  employerName: '$employerName'")
            Log.d(TAG, "  grossSalary: $grossSalary")
            Log.d(TAG, "  netSalary: $netSalary")
            Log.d(TAG, "  payPeriod: '$payPeriod'")
            Log.d(TAG, "  deductions: $deductions")
            Log.d(TAG, "  allowances: $allowances")
            Log.d(TAG, "  confidence: $confidence")
            
            val result = PayslipData(
                employeeName = employeeName,
                employerName = employerName,
                grossSalary = grossSalary,
                netSalary = netSalary,
                payPeriod = payPeriod,
                deductions = deductions,
                allowances = allowances,
                extractionConfidence = confidence,
                isValid = false // Will be validated separately
            ).validate()
            
            Log.d(TAG, "Final payslip result after validation: $result")
            Log.d(TAG, "=== END PAYSLIP FUNCTION PARSING ===")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception in parsePayslipFunction: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            PayslipData(extractionConfidence = 0.0f)
        }
    }
    
    /**
     * Helper function to parse money-related JSON objects into Map<String, Double>
     */
    private fun parseMoneyMap(jsonElement: JsonElement?): Map<String, Double> {
        return try {
            val jsonObject = jsonElement as? JsonObject ?: return emptyMap()
            jsonObject.mapValues { (_, value) ->
                (value as? JsonPrimitive)?.double ?: 0.0
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Creates a formatted function call prompt for ID extraction
     */
    fun createIdExtractionPrompt(): String {
        return """
        You are an AI that extracts data from Kenyan National ID cards. 
        
        Look at this ID card image and extract the information.
        
        Respond with EXACTLY this JSON format (no other text):
        {
            "name": "extractIdCardData",
            "arguments": {
                "fullName": "the full name on the ID",
                "idNumber": "the 8-digit ID number", 
                "dateOfBirth": "birth date in YYYY-MM-DD format",
                "expiryDate": "expiry date in YYYY-MM-DD format or empty",
                "placeOfBirth": "place of birth or empty",
                "confidence": 0.8
            }
        }
        
        Use empty strings for fields you cannot read clearly.
        Set confidence between 0.1 and 1.0 based on image quality.
        """.trimIndent()
    }
    
        /**
     * Creates a formatted function call prompt for loan application form extraction
     */
    fun createLoanApplicationExtractionPrompt(): String {
        return """
        You are an AI that extracts data from loan application forms.
        
        Look at this loan application form image and extract the information from all filled fields.
        
        Respond with EXACTLY this JSON format (no other text):
        {
            "name": "extractLoanApplicationData",
            "arguments": {
                "title": "Ms.",
                "firstName": "first name",
                "middleName": "middle name", 
                "lastName": "last name",
                "idPassportNumber": "ID/passport number",
                "dateOfBirth": "07-Dec-1971",
                "telephoneMobile": "phone number",
                "emailAddress": "email@example.com",
                "residentialAddress": "residential address",
                "townCity": "town/city",
                "bankName": "bank name",
                "accountName": "account name",
                "accountNumber": "account number",
                "requestedLoanAmount": 50000.0,
                "requestedInstallmentAmount": 1250.0,
                "requestedLoanPeriodMonths": 48,
                "disbursementMode": "M-PESA",
                "clientMPesaNumber": "M-PESA number",
                "applicantSignatureDate": "date",
                "confidence": 0.8
            }
        }
        
        Extract ALL visible information from the form.
        Use empty strings for fields you cannot read clearly.
        Convert amounts to numbers (remove commas/currency symbols).
        Set confidence between 0.1 and 1.0 based on form completeness and image quality.
        """.trimIndent()
    }
    
    /**
* Creates a formatted function call prompt for payslip extraction
     */
    fun createPayslipExtractionPrompt(): String {
        return """
        You are an AI that extracts data from payslips. 
        
        Look at this payslip image and extract the salary information.
        
        Respond with EXACTLY this JSON format (no other text):
        {
            "name": "extractPayslipData",
            "arguments": {
                "employeeName": "employee name",
                "employerName": "company name",
                "grossSalary": 50000.0,
                "netSalary": 42000.0,
                "payPeriod": "2024-01",
                "deductions": {
                    "PAYE": 8000.0,
                    "NSSF": 2000.0
                },
                "allowances": {
                    "Housing": 15000.0
                },
                "confidence": 0.8
            }
        }
        
        Convert amounts to numbers (remove commas/currency symbols).
        Use empty strings for text you cannot read clearly.
        Set confidence between 0.1 and 1.0 based on image quality.
        """.trimIndent()
    }
    
    /**
     * Parses loan application form function call response into data object
     */
    fun parseLoanApplicationFunction(arguments: JsonObject): LoanApplicationFormData {
        return try {
            Log.d(TAG, "=== PARSING LOAN APPLICATION FUNCTION ===")
            Log.d(TAG, "Arguments received: $arguments")
            
            val title = (arguments["title"] as? JsonPrimitive)?.content ?: ""
            val firstName = (arguments["firstName"] as? JsonPrimitive)?.content ?: ""
            val middleName = (arguments["middleName"] as? JsonPrimitive)?.content ?: ""
            val lastName = (arguments["lastName"] as? JsonPrimitive)?.content ?: ""
            val idPassportNumber = (arguments["idPassportNumber"] as? JsonPrimitive)?.content ?: ""
            val dateOfBirth = (arguments["dateOfBirth"] as? JsonPrimitive)?.content ?: ""
            val telephoneMobile = (arguments["telephoneMobile"] as? JsonPrimitive)?.content ?: ""
            val emailAddress = (arguments["emailAddress"] as? JsonPrimitive)?.content ?: ""
            val residentialAddress = (arguments["residentialAddress"] as? JsonPrimitive)?.content ?: ""
            val townCity = (arguments["townCity"] as? JsonPrimitive)?.content ?: ""
            val bankName = (arguments["bankName"] as? JsonPrimitive)?.content ?: ""
            val accountName = (arguments["accountName"] as? JsonPrimitive)?.content ?: ""
            val accountNumber = (arguments["accountNumber"] as? JsonPrimitive)?.content ?: ""
            
            val requestedLoanAmount = try {
                (arguments["requestedLoanAmount"] as? JsonPrimitive)?.double ?: 0.0
            } catch (e: Exception) { 0.0 }
            
            val requestedInstallmentAmount = try {
                (arguments["requestedInstallmentAmount"] as? JsonPrimitive)?.double ?: 0.0
            } catch (e: Exception) { 0.0 }
            
            val requestedLoanPeriodMonths = try {
                (arguments["requestedLoanPeriodMonths"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            } catch (e: Exception) { 0 }
            
            val disbursementMode = (arguments["disbursementMode"] as? JsonPrimitive)?.content ?: ""
            val clientMPesaNumber = (arguments["clientMPesaNumber"] as? JsonPrimitive)?.content ?: ""
            val applicantSignatureDate = (arguments["applicantSignatureDate"] as? JsonPrimitive)?.content ?: ""
            
            val confidence = try {
                (arguments["confidence"] as? JsonPrimitive)?.float ?: 0.3f
            } catch (e: Exception) { 0.3f }
            
            val result = LoanApplicationFormData(
                title = title,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                idPassportNumber = idPassportNumber,
                dateOfBirth = dateOfBirth,
                telephoneMobile = telephoneMobile,
                emailAddress = emailAddress,
                residentialAddress = residentialAddress,
                townCity = townCity,
                bankName = bankName,
                accountName = accountName,
                accountNumber = accountNumber,
                requestedLoanAmount = requestedLoanAmount,
                requestedInstallmentAmount = requestedInstallmentAmount,
                requestedLoanPeriodMonths = requestedLoanPeriodMonths,
                disbursementMode = disbursementMode,
                clientMPesaNumber = clientMPesaNumber,
                applicantSignatureDate = applicantSignatureDate,
                extractionConfidence = confidence
            ).validate()
            
            Log.d(TAG, "Parsed loan application data: $result")
            Log.d(TAG, "=== END LOAN APPLICATION PARSING ===")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing loan application function", e)
            LoanApplicationFormData(extractionConfidence = 0.0f)
        }
    }

    /**
     * Gets all available functions for the AI model
     */
    fun getAllFunctions(): List<String> {
        return listOf(
            EXTRACT_ID_CARD_FUNCTION,
            EXTRACT_PAYSLIP_FUNCTION,
            EXTRACT_LOAN_APPLICATION_FUNCTION
        )
    }
    
    /**
     * Validates that a function call response contains the expected function name
     */
    fun isValidFunctionCall(functionCall: JsonObject, expectedFunctionName: String): Boolean {
        return try {
            val functionName = (functionCall["name"] as? JsonPrimitive)?.content
            functionName == expectedFunctionName
        } catch (e: Exception) {
            false
        }
    }
} 