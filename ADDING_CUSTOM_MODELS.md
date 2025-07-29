# Adding Custom Models to Akili AI

This guide explains how to add custom AI models to the Akili AI Android app.

## Overview

The AI Edge Gallery app loads models from a `model_allowlist.json` file. By default, it loads from the internet, but we can configure it to use a local file with custom models.

## Prerequisites

- Android development environment set up
- Java 21 (for compatibility with MediaPipe dependencies)
- Android SDK configured
- Connected Android device or emulator

## Step-by-Step Guide

### 1. Find Real Models on Hugging Face

**⚠️ Critical:** Only use **real model IDs** that exist on Hugging Face! 

To find compatible models:

1. **Visit Hugging Face**: Go to https://huggingface.co/models
2. **Search for LiteRT models**: Use filters like:
   - Search: `litert` or `.task`
   - Filter by `task`: text-generation
3. **Look for `.task` files**: Check the Files tab for models with `.task` extensions
4. **Popular sources**:
   - `litert-community/*` - Community optimized models
   - `google/*-litert-*` - Official Google models

**Examples of real models:**
- `litert-community/Hammer2.1-1.5b` ✅ (Real)
- `litert-community/Gemma3-1B-IT` ✅ (Real)  
- `google/gemma-3n-E2B-it-litert-preview` ✅ (Real)
- `litert-community/Gemma3n-E4B-q8` ❌ (Fictional - doesn't exist)

### 2. Understand the Model Structure

Each model in the allowlist requires these fields:

```json
{
  "name": "Model Display Name",
  "modelId": "huggingface-org/model-name",  // MUST be a real Hugging Face model!
  "modelFile": "model-file.task",           // MUST exist in the model repo!
  "description": "Model description with [links](url) support",
  "sizeInBytes": 1234567890,
  "estimatedPeakMemoryInBytes": 2345678901,
  "version": "20250101",
  "llmSupportImage": true,  // Optional: for vision models
  "llmSupportAudio": false, // Optional: for audio models
  "defaultConfig": {
    "topK": 40,
    "topP": 0.95,
    "temperature": 1.0,
    "maxTokens": 4096,
    "accelerators": "cpu,gpu"
  },
  "taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image"]
}
```

### 3. Valid Task Types

The app only recognizes these task types:

- `"llm_chat"` - Appears in AI Chat section
- `"llm_prompt_lab"` - Appears in Prompt Lab section  
- `"llm_ask_image"` - Appears in Ask Image section
- `"llm_ask_audio"` - Appears in Audio Scribe section

**⚠️ Important:** Do NOT use custom task types like `"function_call"` or `"vision"` - they will be ignored.

### 4. Model Categories

**Text-only models:**
```json
"taskTypes": ["llm_chat", "llm_prompt_lab"]
```

**Vision models (can process images):**
```json
"llmSupportImage": true,
"taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image"]
```

**Audio models:**
```json
"llmSupportAudio": true,
"taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_audio"]
```

### 5. Verify Model Exists Before Adding

Before adding a model, verify it exists:

1. **Check the model page**: `https://huggingface.co/{modelId}`
2. **Verify the .task file**: Look in the Files tab for your `modelFile`
3. **Test the download URL**: 
   ```
   https://huggingface.co/{modelId}/resolve/main/{modelFile}?download=true
   ```

**Example verification:**
- Model ID: `litert-community/Hammer2.1-1.5b`
- File: `hammer2.1_1.5b_q8_ekv4096.task`
- URL: https://huggingface.co/litert-community/Hammer2.1-1.5b/resolve/main/hammer2.1_1.5b_q8_ekv4096.task?download=true

### 6. Add Your Custom Models

Edit the `model_allowlist.json` file to include **only real models**:

```json
{
  "models": [
    // ... existing models ...
    {
      "name": "Hammer2.1-1.5B q8",
      "modelId": "litert-community/Hammer2.1-1.5b",    // ✅ Real model
      "modelFile": "hammer2.1_1.5b_q8_ekv4096.task",  // ✅ Real file
      "description": "Hammer2.1-1.5B model with 8-bit quantization",
      "sizeInBytes": 1750000000,
      "estimatedPeakMemoryInBytes": 3221225472,
      "version": "20250101",
      "defaultConfig": {
        "topK": 40,
        "topP": 0.95,
        "temperature": 1.0,
        "maxTokens": 4096,
        "accelerators": "cpu,gpu"
      },
      "taskTypes": ["llm_chat", "llm_prompt_lab"]
    }
  ]
}
```

### 7. Configure the App to Use Local Models

#### Option A: Disable Internet Loading (Recommended)

1. **Edit the ViewModel:**
   ```kotlin
   // In ModelManagerViewModel.kt, in loadModelAllowlist() function:
   
   // Comment out internet loading:
   // Log.d(TAG, "Loading model allowlist from internet...")
   // val data = getJsonResponse<ModelAllowlist>(url = MODEL_ALLOWLIST_URL)
   // var modelAllowlist: ModelAllowlist? = data?.jsonObj

   // Force loading from local disk:
   Log.d(TAG, "Loading model allowlist from local disk...")
   val modelAllowlist = readModelAllowlistFromDisk()
   ```

2. **Update the disk reading function to use assets:**
   ```kotlin
   private fun readModelAllowlistFromDisk(): ModelAllowlist? {
     try {
       Log.d(TAG, "Reading model allowlist from disk...")
       val file = File(externalFilesDir, MODEL_ALLOWLIST_FILENAME)
       
       var content: String? = null
       
       // First try external files, then fallback to assets
       if (file.exists()) {
         content = file.readText()
         Log.d(TAG, "Model allowlist loaded from external files")
       } else {
         Log.d(TAG, "External file not found, trying to read from assets...")
         try {
           val inputStream = context.assets.open(MODEL_ALLOWLIST_FILENAME)
           content = inputStream.bufferedReader().use { it.readText() }
           Log.d(TAG, "Model allowlist loaded from assets")
         } catch (e: Exception) {
           Log.e(TAG, "Failed to read from assets", e)
         }
       }
       
       if (content != null) {
         Log.d(TAG, "Model allowlist content: $content")
         val gson = Gson()
         val type = object : TypeToken<ModelAllowlist>() {}.type
         return gson.fromJson<ModelAllowlist>(content, type)
       }
     } catch (e: Exception) {
       Log.e(TAG, "failed to read model allowlist from disk", e)
       return null
     }
     return null
   }
   ```

### 8. Add Models to App Assets

1. **Create assets directory:**
   ```bash
   mkdir -p Android/src/app/src/main/assets
   ```

2. **Copy your custom allowlist:**
   ```bash
   cp model_allowlist.json Android/src/app/src/main/assets/
   ```

3. **Verify the file exists:**
   ```bash
   ls -la Android/src/app/src/main/assets/model_allowlist.json
   ```

### 9. Build and Deploy

1. **Set environment variables:**
   ```bash
   export JAVA_HOME="/path/to/jdk-21"
   export ANDROID_HOME="/path/to/Android/Sdk"
   ```

2. **Build and install:**
   ```bash
   cd Android/src
   ./gradlew installDebug
   ```

3. **Clear app data (important!):**
   ```bash
   adb shell pm clear com.google.aiedge.gallery
   ```

### 10. Verify Installation

Open the app and check the model counts:

- **Ask Image**: Should show count including vision models
- **Prompt Lab**: Should show count including all your models  
- **AI Chat**: Should show count including all your models

## Troubleshooting

### Models Not Appearing

1. **Check JSON syntax:**
   ```bash
   node -e "JSON.parse(require('fs').readFileSync('model_allowlist.json', 'utf8')); console.log('JSON is valid')"
   ```

2. **Verify assets file:**
   ```bash
   ls -la Android/src/app/src/main/assets/model_allowlist.json
   ```

3. **Clear app data:**
   ```bash
   adb shell pm clear com.google.aiedge.gallery
   ```

4. **Check logcat for errors:**
   ```bash
   adb logcat | grep "AGModelManagerViewModel"
   ```

### Common Issues

- **Unknown task types:** Only use the 4 valid task types listed above
- **Missing assets file:** Ensure `model_allowlist.json` is in `app/src/main/assets/`
- **Cached data:** Always clear app data after changes
- **Java version:** Use Java 21 for MediaPipe compatibility

### Model URL Format

Models are downloaded from Hugging Face using this URL pattern:
```
https://huggingface.co/{modelId}/resolve/main/{modelFile}?download=true
```

For example:
- `modelId`: "litert-community/Hammer2.1-1.5b"  
- `modelFile`: "hammer2.1_1.5b_q8_ekv4096.task"
- Result: `https://huggingface.co/litert-community/Hammer2.1-1.5b/resolve/main/hammer2.1_1.5b_q8_ekv4096.task?download=true`

## Authentication

For gated models, ensure you have:
1. Hugging Face OAuth app configured
2. Correct `clientId` and `redirectUri` in `AuthConfig.kt`
3. Valid access token for protected models

## Example: Adding a New Model

```json
{
  "name": "MyCustom-7B-Chat",
  "modelId": "myorg/mycustom-7b-chat", 
  "modelFile": "mycustom-7b-chat-q4.task",
  "description": "A custom 7B parameter chat model optimized for mobile deployment",
  "sizeInBytes": 4200000000,
  "estimatedPeakMemoryInBytes": 6300000000,
  "version": "20250127",
  "llmSupportImage": false,
  "defaultConfig": {
    "topK": 50,
    "topP": 0.9, 
    "temperature": 0.8,
    "maxTokens": 2048,
    "accelerators": "gpu,cpu"
  },
  "taskTypes": ["llm_chat", "llm_prompt_lab"]
}
```

This will add "MyCustom-7B-Chat" to both the AI Chat and Prompt Lab sections.

---

## Summary

1. ✅ Create/edit `model_allowlist.json` with your models
2. ✅ Use only valid task types
3. ✅ Modify app to load from assets instead of internet  
4. ✅ Copy JSON to `app/src/main/assets/`
5. ✅ Build and deploy app
6. ✅ Clear app data
7. ✅ Verify models appear in UI

**Happy model adding!** 🚀 