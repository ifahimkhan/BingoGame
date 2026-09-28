# Android Development Commands Reference — Bingo Number Caller (KMP)

This document contains all commands used during Android, Compose Multiplatform, and Ktor server development in this project, categorized by utility.

---

## 1. Gradle Build & Assembly Commands

Run with `.\gradlew.bat` (Windows PowerShell / CMD) or `./gradlew` (macOS / Linux / Bash):

```powershell
# Compile & assemble APKs
.\gradlew.bat assembleDebug                          # Build debug APK for all modules
.\gradlew.bat :androidApp:assembleDebug              # Build debug APK for androidApp module
.\gradlew.bat :androidApp:assembleRelease            # Build release APK
.\gradlew.bat :androidApp:bundleDebug                # Build debug Android App Bundle (AAB)
.\gradlew.bat :androidApp:bundleRelease              # Build release Android App Bundle (AAB)

# Install directly to attached device / emulator
.\gradlew.bat :androidApp:installDebug               # Build and install debug APK to device
.\gradlew.bat :androidApp:installRelease             # Build and install release APK to device
.\gradlew.bat :androidApp:uninstallAll               # Uninstall all builds from device

# Build all modules
.\gradlew.bat build                                  # Build and test all modules (:shared, :server, :composeApp, :androidApp)
.\gradlew.bat :server:run                            # Run the standalone Ktor game server locally
.\gradlew.bat clean                                  # Clean build cache and build directories
.\gradlew.bat --stop                                 # Stop running Gradle daemons
.\gradlew.bat build --refresh-dependencies           # Force re-download dependencies
```

---

## 2. Testing & Verification

```powershell
# Unit tests
.\gradlew.bat test                                   # Run all unit tests across all modules
.\gradlew.bat :shared:test                           # Run unit tests in shared module (game logic, ticket generation)
.\gradlew.bat :server:test                           # Run unit tests in server module
.\gradlew.bat :composeApp:test                       # Run unit tests in composeApp module
.\gradlew.bat :androidApp:testDebugUnitTest          # Run unit tests for androidApp

# Android Instrumentation / Device tests
.\gradlew.bat connectedAndroidTest                   # Run instrumentation tests on connected Android device/emulator
.\gradlew.bat connectedCheck                         # Run all device checks

# Code Quality & Linting
.\gradlew.bat lint                                   # Run Android Lint analysis
.\gradlew.bat :androidApp:lintDebug                  # Run lint on debug variant
.\gradlew.bat :androidApp:lintFix                    # Automatically apply safe lint fixes
```

---

## 3. Dependency & Task Diagnostics

```powershell
.\gradlew.bat tasks                                  # List runnable tasks
.\gradlew.bat tasks --all                            # List all available tasks
.\gradlew.bat projects                               # List all Gradle subprojects
.\gradlew.bat :androidApp:dependencies               # View dependency tree
.\gradlew.bat dependencyInsight --dependency ktor    # Inspect specific dependency resolution
```

---

## 4. ADB (Android Debug Bridge) Commands

Default SDK Path on Windows: `C:\Users\i_fah\AppData\Local\Android\Sdk\platform-tools\adb.exe`

```powershell
# Device management
adb devices                                          # List all connected devices & emulators
adb devices -l                                       # List devices with detailed info (model, product)
adb connect <ip>:<port>                              # Connect to device over Wi-Fi
adb pair <ip>:<port> <code>                          # Pair wireless debugging (Android 11+)
adb disconnect                                       # Disconnect wireless devices
adb kill-server                                      # Stop ADB daemon
adb start-server                                     # Start ADB daemon

# App installation & package management
adb install -r <path-to-apk>                         # Install / reinstall APK preserving data
adb install -t <path-to-apk>                         # Allow test APK installation
adb uninstall com.fahim.bingonumbercaller            # Uninstall app
adb shell pm clear com.fahim.bingonumbercaller       # Clear app cache and user data (clean state)
adb shell pm list packages | findstr bingo           # Check if package is installed
adb shell pm path com.fahim.bingonumbercaller        # Show APK installation path on device

# Launching & Stopping
adb shell am start -n com.fahim.bingonumbercaller/.MainActivity          # Launch app
adb shell am force-stop com.fahim.bingonumbercaller                      # Force stop app
adb shell am restart                                                     # Restart Android framework (soft reboot)

# Networking & Reverse Port Forwarding (Essential for Bingo Game multiplayer!)
adb reverse tcp:8080 tcp:8080                        # Forward device port 8080 to host machine port 8080
adb forward tcp:8080 tcp:8080                        # Forward host port 8080 to device port 8080
adb reverse --list                                   # List active reverse port forwarding rules
adb reverse --remove-all                             # Clear all port forwards

# Logging & Debugging
adb logcat -c                                        # Clear log buffer
adb logcat                                           # Stream all logs
adb logcat -s "BingoLive"                            # Filter by tag "BingoLive"
adb logcat "*:E"                                     # Show error-level logs only
adb logcat | Select-String "bingo"                   # Filter logs in PowerShell
adb bugreport ./bugreport.zip                        # Capture full bug report

# Screenshots, Recordings & Files
adb exec-out screencap -p > screenshot.png           # Capture screenshot to computer
adb shell screenrecord /sdcard/demo.mp4              # Record device screen (Ctrl+C to stop)
adb pull /sdcard/demo.mp4 ./demo.mp4                 # Download recorded video
adb push ./localfile.txt /sdcard/                    # Copy file from PC to device
adb pull /sdcard/remotefile.txt ./                   # Copy file from device to PC
```

---

## 5. Android Emulator Commands

Default Emulator Path: `C:\Users\i_fah\AppData\Local\Android\Sdk\emulator\emulator.exe`

```powershell
emulator -list-avds                                  # List installed AVDs (e.g., Small_Phone)
emulator -avd Small_Phone                            # Launch AVD
emulator -avd Small_Phone -no-snapshot-load          # Cold boot AVD without loading snapshot
emulator -avd Small_Phone -wipe-data                 # Wipe user data on start
emulator -avd Small_Phone -netdelay none -netspeed full # Launch with fast network emulation
```

---

## 6. Android SDK & Signing Tools

```powershell
# Keystore & Certificate fingerprints (SHA-1 / SHA-256)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
keytool -genkey -v -keystore release.keystore -alias bingokey -keyalg RSA -keysize 2048 -validity 10000

# APK Signing & Alignment
zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks release.keystore app-release-aligned.apk
apksigner verify app-release-aligned.apk
```

---

## 7. Network & Host IP Discovery (For Ktor Embedded Server)

```powershell
ipconfig                                             # Find local IPv4 address (Wi-Fi adapter) to connect players
Get-NetTCPConnection -LocalPort 8080                 # Check if Ktor server is listening on port 8080
Test-NetConnection -ComputerName 127.0.0.1 -Port 8080 # Test port connectivity
```
