# 🚀 Akili AI - Deployment Guide

## 📱 **Ready to Deploy: Akili AI Smart Loan Platform**

Your **Akili AI** app has been successfully rebranded and is ready for deployment! Here's your complete deployment guide.

---

## 🔧 **Pre-Deployment Setup**

### **1. Android SDK Requirements**
```bash
# Ensure Android SDK is properly installed
# Required SDK path: C:\Users\Administrator\AppData\Local\Android\Sdk

# Verify SDK components:
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0+
- Android SDK Platform-Tools
- Android Emulator (for testing)
```

### **2. Environment Variables**
```powershell
# Set in PowerShell (run as Administrator)
$env:ANDROID_HOME = "C:\Users\Administrator\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools"
```

---

## 🏗️ **Build Instructions**

### **Option 1: Command Line Build**
```bash
cd Android/src

# Clean build
./gradlew clean

# Debug APK (for testing)
./gradlew assembleDebug

# Release APK (for production)
./gradlew assembleRelease
```

### **Option 2: Android Studio Build**
1. Open `Android/src` in Android Studio
2. Wait for Gradle sync to complete
3. Build → Generate Signed Bundle/APK
4. Choose APK
5. Select release configuration
6. Sign with debug key (for testing) or create release key

---

## 📦 **APK Output Locations**

After successful build, APKs will be generated at:

```
# Debug APK
Android/src/app/build/outputs/apk/debug/app-debug.apk

# Release APK  
Android/src/app/build/outputs/apk/release/app-release.apk
```

---

## 🔐 **App Signing (Production)**

For production deployment, you'll need to create a signed APK:

### **Create Keystore**
```bash
keytool -genkey -v -keystore akili-ai-release.keystore -alias akili-ai -keyalg RSA -keysize 2048 -validity 10000
```

### **Update build.gradle.kts**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../akili-ai-release.keystore")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "akili-ai"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

---

## 📋 **Current App Configuration**

### **App Details**
- **Name**: Akili AI
- **Package**: `com.akili.ai.gallery`
- **Version**: 1.0 (Code: 1)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

### **Key Features**
- ✅ Smart Loan processing with AI vision
- ✅ Offline functionality (no internet required after model download)
- ✅ Biometric authentication
- ✅ Document scanning (ID cards, payslips)
- ✅ Real-time loan assessment
- ✅ Professional banking-grade UI

---

## 🧪 **Testing Instructions**

### **Pre-Deployment Testing**
1. **Install Debug APK**:
   ```bash
   adb install app-debug.apk
   ```

2. **Test Core Features**:
   - [ ] App launches successfully
   - [ ] Smart Loan flow works end-to-end
   - [ ] Camera functionality for document capture
   - [ ] AI model loading and processing
   - [ ] Biometric authentication
   - [ ] Offline mode functionality

3. **Device Compatibility**:
   - Test on Samsung S24 Ultra (primary target)
   - Test on devices with 6GB+ RAM
   - Verify performance meets <7 seconds target

---

## 🚀 **Deployment Options**

### **1. Direct APK Distribution**
- Share `app-release.apk` directly with stakeholders
- Perfect for board-room demos and internal testing

### **2. Internal App Distribution**
- Upload to Firebase App Distribution
- Use Microsoft App Center
- Create internal company portal

### **3. Play Store (Future)**
- Complete Play Console setup
- Add store listings and screenshots
- Submit for review

---

## 📊 **Demo Mode Setup**

Your app includes demo mode for presentations:

```kotlin
// Demo mode is enabled by default for board-room demos
buildConfigField("boolean", "DEMO_MODE", "true")
```

### **Demo Flow Features**:
- Pre-loaded sample images
- Simulated AI processing (2-3 seconds)
- Realistic loan offers
- Works in airplane mode

---

## 🎯 **Board-Room Demo Checklist**

- [ ] APK installed on demo device (Samsung S24 Ultra recommended)
- [ ] Device in airplane mode to demonstrate offline capability
- [ ] Demo images pre-loaded
- [ ] Biometric authentication set up
- [ ] Full flow tested multiple times
- [ ] Backup devices ready

---

## 🆘 **Troubleshooting**

### **Build Issues**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf .gradle
rm -rf app/build

# Restart daemon
./gradlew --stop
./gradlew assembleDebug
```

### **SDK Issues**
- Verify Android SDK installation
- Check ANDROID_HOME environment variable
- Update local.properties with correct SDK path

### **Memory Issues**
- Increase Gradle heap size in gradle.properties
- Close other applications during build

---

## 📞 **Support**

For deployment assistance:
1. Check build logs with `./gradlew assembleRelease --stacktrace`
2. Verify all dependencies are properly installed
3. Test on physical device before board presentation

---

**🎉 Your Akili AI Smart Loan platform is ready to revolutionize financial services!**