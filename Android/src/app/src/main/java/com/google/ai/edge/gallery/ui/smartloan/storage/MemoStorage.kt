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

package com.google.ai.edge.gallery.ui.smartloan.storage

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.ai.edge.gallery.ui.smartloan.memo.DisbursementMemo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException

/**
 * Handles storage and export of disbursement memos
 */
class MemoStorage(private val context: Context) {
    
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val TAG = "MemoStorage"
    
    companion object {
        private const val MEMO_DIR = "memos"
        private const val MEMO_FILE_PREFIX = "disbursement_"
        private const val MEMO_FILE_EXTENSION = ".json"
    }
    
    /**
     * Save disbursement memo to internal storage
     */
    fun saveDisbursementMemo(memo: DisbursementMemo): String? {
        return try {
            val memoDir = File(context.filesDir, MEMO_DIR)
            if (!memoDir.exists()) {
                memoDir.mkdirs()
            }
            
            val filename = "${MEMO_FILE_PREFIX}${memo.applicationId}_${System.currentTimeMillis()}$MEMO_FILE_EXTENSION"
            val file = File(memoDir, filename)
            
            val jsonContent = gson.toJson(memo)
            file.writeText(jsonContent)
            
            Log.d(TAG, "Memo saved successfully: ${file.absolutePath}")
            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save memo", e)
            null
        }
    }
    
    /**
     * Load all disbursement memos from storage
     */
    fun getAllMemos(): List<DisbursementMemo> {
        return try {
            val memoDir = File(context.filesDir, MEMO_DIR)
            if (!memoDir.exists()) {
                return emptyList()
            }
            
            val memos = mutableListOf<DisbursementMemo>()
            memoDir.listFiles { file ->
                file.name.startsWith(MEMO_FILE_PREFIX) && file.name.endsWith(MEMO_FILE_EXTENSION)
            }?.forEach { file ->
                try {
                    val jsonContent = file.readText()
                    val memo = gson.fromJson(jsonContent, DisbursementMemo::class.java)
                    memos.add(memo)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse memo file: ${file.name}", e)
                }
            }
            
            // Sort by timestamp (newest first)
            memos.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load memos", e)
            emptyList()
        }
    }
    
    /**
     * Get memo by application ID
     */
    fun getMemoByApplicationId(applicationId: String): DisbursementMemo? {
        return getAllMemos().find { it.applicationId == applicationId }
    }
    
    /**
     * Export memo to Downloads folder for easy access
     */
    fun exportMemoToDownloads(memo: DisbursementMemo): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val filename = "${MEMO_FILE_PREFIX}${memo.applicationId}$MEMO_FILE_EXTENSION"
            val file = File(downloadsDir, filename)
            
            val jsonContent = gson.toJson(memo)
            file.writeText(jsonContent)
            
            Log.d(TAG, "Memo exported to Downloads: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export memo to Downloads", e)
            false
        }
    }
    
    /**
     * Export memo as human-readable text file
     */
    fun exportMemoAsText(memo: DisbursementMemo, generator: com.google.ai.edge.gallery.ui.smartloan.memo.DisbursementMemoGenerator): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val filename = "${MEMO_FILE_PREFIX}${memo.applicationId}.txt"
            val file = File(downloadsDir, filename)
            
            val textContent = generator.formatMemoForDisplay(memo)
            file.writeText(textContent)
            
            Log.d(TAG, "Memo exported as text: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export memo as text", e)
            false
        }
    }
    
    /**
     * Delete memo by application ID
     */
    fun deleteMemo(applicationId: String): Boolean {
        return try {
            val memoDir = File(context.filesDir, MEMO_DIR)
            val files = memoDir.listFiles { file ->
                file.name.contains(applicationId) && file.name.endsWith(MEMO_FILE_EXTENSION)
            }
            
            var deleted = false
            files?.forEach { file ->
                if (file.delete()) {
                    deleted = true
                    Log.d(TAG, "Deleted memo file: ${file.name}")
                }
            }
            
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete memo", e)
            false
        }
    }
    
    /**
     * Clear all memos from storage
     */
    fun clearAllMemos(): Boolean {
        return try {
            val memoDir = File(context.filesDir, MEMO_DIR)
            if (!memoDir.exists()) {
                return true
            }
            
            val files = memoDir.listFiles { file ->
                file.name.startsWith(MEMO_FILE_PREFIX) && file.name.endsWith(MEMO_FILE_EXTENSION)
            }
            
            var allDeleted = true
            files?.forEach { file ->
                if (!file.delete()) {
                    allDeleted = false
                    Log.w(TAG, "Failed to delete memo file: ${file.name}")
                }
            }
            
            Log.d(TAG, "Cleared ${files?.size ?: 0} memo files")
            allDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear memos", e)
            false
        }
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): MemoStorageStats {
        return try {
            val memoDir = File(context.filesDir, MEMO_DIR)
            if (!memoDir.exists()) {
                return MemoStorageStats(0, 0L)
            }
            
            val files = memoDir.listFiles { file ->
                file.name.startsWith(MEMO_FILE_PREFIX) && file.name.endsWith(MEMO_FILE_EXTENSION)
            }
            
            val count = files?.size ?: 0
            val totalSize = files?.sumOf { it.length() } ?: 0L
            
            MemoStorageStats(count, totalSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get storage stats", e)
            MemoStorageStats(0, 0L)
        }
    }
    
    /**
     * Check if external storage is available for export
     */
    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}

/**
 * Storage statistics data class
 */
data class MemoStorageStats(
    val memoCount: Int,
    val totalSizeBytes: Long
) {
    fun getTotalSizeMB(): Double = totalSizeBytes / (1024.0 * 1024.0)
}