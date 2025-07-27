# Understanding the taskTypes System in AI Edge Gallery

*A comprehensive guide for junior mobile developers*

## 📋 Overview

The AI Edge Gallery app uses a **taskTypes system** to categorize and display AI models across different sections of the app. This document explains how this system works, how to modify it, and common pitfalls to avoid.

## 🏗️ System Architecture

### 1. The Task Hierarchy

The app organizes functionality around **Tasks** (app sections) that contain **Models** (AI models):

```
App Home Screen
├── AI Chat (Task)          → Contains models with "llm_chat" taskType
├── Prompt Lab (Task)       → Contains models with "llm_prompt_lab" taskType  
├── Ask Image (Task)        → Contains models with "llm_ask_image" taskType
└── Audio Scribe (Task)     → Contains models with "llm_ask_audio" taskType (hidden)
```

### 2. Core Components

#### TaskType Enum (`Tasks.kt`)
Defines all available task types:

```kotlin
enum class TaskType(val label: String, val id: String) {
  LLM_CHAT(label = "AI Chat", id = "llm_chat"),
  LLM_PROMPT_LAB(label = "Prompt Lab", id = "llm_prompt_lab"),
  LLM_ASK_IMAGE(label = "Ask Image", id = "llm_ask_image"),
  LLM_ASK_AUDIO(label = "Audio Scribe", id = "llm_ask_audio"),
  // Test tasks (not used in production)
  TEST_TASK_1(label = "Test task 1", id = "test_task_1"),
  TEST_TASK_2(label = "Test task 2", id = "test_task_2"),
}
```

**Key Points:**
- `label` = What users see in the UI
- `id` = What you put in `model_allowlist.json` taskTypes array

#### Task Objects (`Tasks.kt`)
Each task type has a corresponding Task object:

```kotlin
val TASK_LLM_CHAT = Task(
  type = TaskType.LLM_CHAT,
  icon = Icons.Outlined.Forum,
  models = mutableListOf(),                    // ← Models get added here!
  description = "Chat with on-device large language models",
  docUrl = "https://ai.google.dev/...",
  sourceCodeUrl = "https://github.com/...",
  textInputPlaceHolderRes = R.string.placeholder_text
)

val TASK_LLM_PROMPT_LAB = Task(
  type = TaskType.LLM_PROMPT_LAB,
  icon = Icons.Outlined.Widgets,
  models = mutableListOf(),                    // ← Models get added here!
  description = "Single turn use cases with on-device large language models"
)

val TASK_LLM_ASK_IMAGE = Task(
  type = TaskType.LLM_ASK_IMAGE,
  icon = Icons.Outlined.Mms,
  models = mutableListOf(),                    // ← Models get added here!
  description = "Ask questions about images with on-device large language models"
)
```

#### Global Task List
All tasks are collected in a single list:

```kotlin
val TASKS: List<Task> = listOf(
  TASK_LLM_ASK_IMAGE, 
  TASK_LLM_ASK_AUDIO, 
  TASK_LLM_PROMPT_LAB, 
  TASK_LLM_CHAT
)
```

## ⚙️ How Model Assignment Works

### 1. Model Definition (in `model_allowlist.json`)

```json
{
  "name": "Hammer2.1-1.5B q8",
  "modelId": "litert-community/Hammer2.1-1.5b",
  "modelFile": "hammer2.1_1.5b_q8_ekv4096.task",
  "taskTypes": ["llm_chat", "llm_prompt_lab"]  // ← This determines where it appears!
}
```

### 2. Assignment Logic (in `ModelManagerViewModel.kt`)

When the app loads, it processes each model:

```kotlin
// In loadModelAllowlist() function:
for (allowedModel in modelAllowlist.models) {
  if (allowedModel.disabled == true) {
    continue  // Skip disabled models
  }

  val model = allowedModel.toModel()
  
  // Check each possible taskType and add to appropriate task
  if (allowedModel.taskTypes.contains(TASK_LLM_CHAT.type.id)) {
    TASK_LLM_CHAT.models.add(model)        // Appears in "AI Chat"
  }
  if (allowedModel.taskTypes.contains(TASK_LLM_PROMPT_LAB.type.id)) {
    TASK_LLM_PROMPT_LAB.models.add(model)  // Appears in "Prompt Lab"
  }
  if (allowedModel.taskTypes.contains(TASK_LLM_ASK_IMAGE.type.id)) {
    TASK_LLM_ASK_IMAGE.models.add(model)   // Appears in "Ask Image"  
  }
  if (allowedModel.taskTypes.contains(TASK_LLM_ASK_AUDIO.type.id)) {
    TASK_LLM_ASK_AUDIO.models.add(model)   // Appears in "Audio Scribe"
  }
}
```

**Important:** This is a **multi-membership system**. One model can appear in multiple sections!

### 3. UI Display (in `HomeScreen.kt`)

The home screen counts models for each task and displays cards:

```kotlin
private fun TaskList(tasks: List<Task>, navigateToTaskScreen: (Task) -> Unit) {
  for (task in tasks) {
    // Skip audio task (not shown in current UI)
    if (task.type != TaskType.LLM_ASK_AUDIO) {
      TaskCard(
        task = task,
        modelCount = task.models.size,  // ← Count from taskTypes assignment
        onClick = { navigateToTaskScreen(task) }
      )
    }
  }
}
```

## 📱 UI Flow Example

Let's trace a model through the entire system:

### Model Definition:
```json
{
  "name": "Vision Model X",
  "taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image"],
  "llmSupportImage": true
}
```

### Processing:
1. ✅ Contains `"llm_chat"` → Added to `TASK_LLM_CHAT.models`
2. ✅ Contains `"llm_prompt_lab"` → Added to `TASK_LLM_PROMPT_LAB.models`  
3. ✅ Contains `"llm_ask_image"` → Added to `TASK_LLM_ASK_IMAGE.models`
4. ❌ Doesn't contain `"llm_ask_audio"` → Not added to audio task

### UI Result:
```
Home Screen:
├── AI Chat (5 Models)        ← +1 from our model
├── Prompt Lab (6 Models)     ← +1 from our model
├── Ask Image (3 Models)      ← +1 from our model
└── Audio Scribe (hidden)
```

### Navigation:
- Tap "AI Chat" → See Vision Model X in the model list
- Tap "Prompt Lab" → See Vision Model X in the model list  
- Tap "Ask Image" → See Vision Model X in the model list

## 🔧 Practical Examples

### Example 1: Text-Only Chat Model
```json
{
  "name": "Chat Model",
  "taskTypes": ["llm_chat", "llm_prompt_lab"]
}
```
**Result:** Appears in AI Chat and Prompt Lab only.

### Example 2: Vision Model  
```json
{
  "name": "Vision Model",
  "llmSupportImage": true,
  "taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image"]
}
```
**Result:** Appears in AI Chat, Prompt Lab, and Ask Image.

### Example 3: Specialized Model
```json
{
  "name": "Image-Only Model", 
  "llmSupportImage": true,
  "taskTypes": ["llm_ask_image"]
}
```
**Result:** Appears only in Ask Image.

### Example 4: Audio Model (Future)
```json
{
  "name": "Audio Model",
  "llmSupportAudio": true, 
  "taskTypes": ["llm_chat", "llm_ask_audio"]
}
```
**Result:** Appears in AI Chat. Audio section currently hidden in UI.

## ⚠️ Common Pitfalls for Junior Developers

### 1. Invalid taskType Strings
```json
// ❌ WRONG - These don't exist!
"taskTypes": ["function_call", "vision", "custom_task"]

// ✅ CORRECT - Only use these 4:
"taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image", "llm_ask_audio"]
```

### 2. Case Sensitivity
```json
// ❌ WRONG - Case matters!
"taskTypes": ["LLM_CHAT", "llm_Chat", "Llm_prompt_lab"]

// ✅ CORRECT - Exact match required:
"taskTypes": ["llm_chat", "llm_prompt_lab"]
```

### 3. Forgetting Multi-Membership
```json
// A model with this taskTypes array:
"taskTypes": ["llm_chat", "llm_prompt_lab", "llm_ask_image"]

// Will appear in ALL THREE sections, not just one!
```

### 4. Audio Task Confusion
```json
// This model WON'T be visible in UI (audio section is hidden):
"taskTypes": ["llm_ask_audio"]

// Include other taskTypes to make it visible:
"taskTypes": ["llm_chat", "llm_ask_audio"]
```

## 🛠️ How to Add New Task Types (Advanced)

If you need to add a new section to the app:

### 1. Add to TaskType Enum
```kotlin
enum class TaskType(val label: String, val id: String) {
  // ... existing types ...
  NEW_TASK(label = "New Feature", id = "new_feature"),
}
```

### 2. Create Task Object
```kotlin
val TASK_NEW_FEATURE = Task(
  type = TaskType.NEW_TASK,
  icon = Icons.Outlined.YourIcon,
  models = mutableListOf(),
  description = "Description of new feature"
)
```

### 3. Add to Global List
```kotlin
val TASKS: List<Task> = listOf(
  // ... existing tasks ...
  TASK_NEW_FEATURE
)
```

### 4. Update Assignment Logic
```kotlin
// In ModelManagerViewModel.loadModelAllowlist():
if (allowedModel.taskTypes.contains(TASK_NEW_FEATURE.type.id)) {
  TASK_NEW_FEATURE.models.add(model)
}
```

### 5. Add Navigation Handling
```kotlin
// In GalleryNavGraph navigateToTaskScreen():
when (taskType) {
  // ... existing cases ...
  TaskType.NEW_TASK -> navController.navigate("${NewTaskDestination.route}/${modelName}")
}
```

### 6. Update UI (if needed)
```kotlin
// HomeScreen may need updates to show new task type
```

## 🐛 Debugging taskTypes Issues

### Problem: Model not appearing in expected section

1. **Check JSON syntax:**
   ```bash
   node -e "JSON.parse(require('fs').readFileSync('model_allowlist.json')); console.log('Valid JSON')"
   ```

2. **Verify taskType strings:**
   ```bash
   grep -n "taskTypes" model_allowlist.json
   ```

3. **Check logcat for loading:**
   ```bash
   adb logcat | grep "AGModelManagerViewModel"
   ```

4. **Clear app data:**
   ```bash
   adb shell pm clear com.google.aiedge.gallery
   ```

### Problem: Model appears in wrong section

- **Check taskTypes array** - model appears wherever its taskTypes specify
- **Verify TaskType.id values** - must match exactly

### Problem: Model count doesn't update

- **Clear app data** after JSON changes
- **Rebuild app** to include new assets
- **Check for cached allowlist data**

## 📚 Key Files Reference

| File | Purpose |
|------|---------|
| `Tasks.kt` | Defines TaskType enum and Task objects |
| `ModelManagerViewModel.kt` | Assigns models to tasks based on taskTypes |
| `HomeScreen.kt` | Displays task cards with model counts |
| `GalleryNavGraph.kt` | Handles navigation between task screens |
| `model_allowlist.json` | Contains model definitions with taskTypes |

## 🎯 Summary for Junior Developers

1. **taskTypes is a multi-membership system** - one model can appear in multiple app sections
2. **Only 4 valid taskType IDs exist** - anything else is silently ignored
3. **Assignment happens at app startup** - models are added to task.models lists
4. **UI displays based on task.models.size** - counts come from successful assignments
5. **Navigation uses TaskType enum** - links UI sections to screen destinations
6. **Audio section is hidden** - models with only "llm_ask_audio" won't be visible

**Remember:** When you see a model count of "5 Models" in the UI, that means 5 models have that section's taskType ID in their taskTypes array!

---

*This system provides flexible model categorization while maintaining a clean separation between model definitions (JSON) and UI organization (Kotlin code).* 