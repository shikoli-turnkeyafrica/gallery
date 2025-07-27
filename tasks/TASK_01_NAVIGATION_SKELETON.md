# TASK 01: Smart Loan Navigation Skeleton (Week 1)

## 🎯 **Objective**
Create the basic navigation flow for Smart Loan app as an additional section within the existing Gallery app.

## ✅ **Success Criteria**
- Smart Loan entry point visible on Gallery home screen
- 5 Smart Loan screens navigate correctly 
- Back navigation works properly
- No crashes during navigation flow
- Clean UI without Gallery interference

## 📋 **Detailed Steps**

### Step 1: Add Smart Loan Navigation Entry Point
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/HomeScreen.kt`
- [ ] **Action**: Add "Smart Loan Demo" tile/button to existing Gallery home screen
- [ ] **UI**: Use similar styling to existing Gallery tiles
- [ ] **Navigation**: Set up route to Smart Loan flow

### Step 2: Create Smart Loan Navigation Graph
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/navigation/SmartLoanNavGraph.kt`
- [ ] **Action**: Create separate NavHost for Smart Loan screens
- [ ] **Routes**: Define navigation routes for all 5 screens
- [ ] **Arguments**: Set up screen-to-screen data passing

### Step 3: Create Smart Loan Screen Composables
- [ ] **Directory**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/`
- [ ] **Files to create**:
  - `StartApplicationScreen.kt` - Welcome/intro screen
  - `IdCaptureScreen.kt` - ID photo capture screen
  - `PayslipCaptureScreen.kt` - Payslip photos screen  
  - `ValidationProgressScreen.kt` - AI processing screen
  - `OfferScreen.kt` - Final loan offer screen

### Step 4: Create Basic UI Layouts
- [ ] **StartApplicationScreen**: Welcome message, "Start Application" button
- [ ] **IdCaptureScreen**: Camera preview, "Capture Front ID", "Capture Back ID" buttons
- [ ] **PayslipCaptureScreen**: Grid for 4 payslip images, camera integration
- [ ] **ValidationProgressScreen**: Progress indicator, status messages
- [ ] **OfferScreen**: Loan details card, Accept/Decline buttons

### Step 5: Wire Up Navigation Logic
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/navigation/GalleryNavGraph.kt`
- [ ] **Action**: Add Smart Loan navigation integration
- [ ] **Test**: Verify all screen transitions work
- [ ] **Back navigation**: Ensure proper back stack handling

### Step 6: Create Smart Loan ViewModel (Basic)
- [ ] **File**: `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/smartloan/SmartLoanViewModel.kt`
- [ ] **Purpose**: Manage navigation state and temporary data
- [ ] **State**: Track current screen, captured images, user progress
- [ ] **Actions**: Handle screen transitions, data validation

## 🔧 **Technical Notes**

### Navigation Structure
```kotlin
SmartLoanNavGraph {
  composable("start") { StartApplicationScreen(...) }
  composable("id_capture") { IdCaptureScreen(...) }  
  composable("payslip_capture") { PayslipCaptureScreen(...) }
  composable("validation") { ValidationProgressScreen(...) }
  composable("offer") { OfferScreen(...) }
}
```

### Data Flow
```
StartScreen → IdCapture → PayslipCapture → Validation → Offer
     ↓            ↓            ↓            ↓         ↓
   (none)    (ID images)  (4 payslips)  (AI data)  (loan offer)
```

## 🧪 **Testing Checklist**
- [ ] Launch Gallery app normally (existing functionality works)
- [ ] Navigate to Smart Loan section from home
- [ ] Complete full navigation flow (Start → Offer)
- [ ] Test back navigation from each screen
- [ ] Verify no crashes or memory leaks
- [ ] Test on Samsung S24 Ultra specifically

## 📦 **Dependencies Needed**
- No new dependencies for basic navigation
- Reuse existing Gallery Compose navigation setup
- Camera integration uses existing Gallery camera helper

## 🎯 **Definition of Done**
- ✅ Smart Loan tile appears on Gallery home screen
- ✅ All 5 Smart Loan screens are accessible via navigation
- ✅ Navigation flows work in both directions
- ✅ No impact on existing Gallery functionality
- ✅ Clean, consistent UI styling
- ✅ Builds and runs without errors on S24 Ultra

## 📅 **Estimated Time**
**2-3 days** for experienced Android developer
**3-4 days** for junior developer learning Compose Navigation

## 🔗 **Next Task**
After completion: Move to `TASK_02_CAPTURE_STORAGE.md` 