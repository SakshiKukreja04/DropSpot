# DropSpot App - USB Debugging Installation & Testing Guide

## Quick Start: Deploy to Physical Device via USB

### Step 1: Prepare Your Android Device
1. **Enable Developer Mode**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - You'll see "You're now a developer!"

2. **Enable USB Debugging**
   - Go to Settings → Developer Options
   - Toggle "USB Debugging" ON
   - Confirm the security prompt

3. **Connect via USB**
   - Use a USB cable to connect your phone to your PC
   - Allow the device to be recognized
   - Tap "Allow" on the phone when prompted for debugging access

### Step 2: Verify Connection

```powershell
# Open PowerShell and run:
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath devices
```

**Expected Output:**
```
List of devices attached
[device-id]        device
```

If you see `device` status, you're connected! ✅

### Step 3: Install the App

**Method 1: Using Command Line (Fastest)**
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apkPath = "C:\Users\saksh\AndroidStudioProjects\DropSpot\app\build\outputs\apk\debug\app-debug.apk"
& $adbPath install -r $apkPath
```

**Method 2: Using Batch File**
Create `install_app.bat`:
```batch
@echo off
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
pause
```

**Method 3: Using Android Studio**
1. Open Android Studio
2. Go to Run → Run 'app'
3. Select your connected device
4. Click OK

### Step 4: Launch the App

**Option A: From Device**
- Unlock your phone
- Find "DropSpot" in your apps
- Tap to launch

**Option B: From Command Line**
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath shell am start -n com.example.dropspot/.WelcomeActivity
```

### Step 5: Test Razorpay Integration

1. **Navigate to Payment Activity**
   - From the app, create/view a post
   - Tap "Request" or similar action
   - Navigate to payment screen

2. **Test Payment Flow**
   - You should see payment details
   - Enter test amount (minimum ₹1)
   - Click "Pay Now"

3. **Razorpay Checkout Should Open**
   - You'll see Razorpay test payment form
   - Use test card: 4111 1111 1111 1111
   - Expiry: Any future date (e.g., 12/25)
   - CVV: Any 3 digits (e.g., 123)
   - Click Pay

4. **Verify Success**
   - Payment should complete
   - You'll see success message
   - App should navigate back
   - Check Firebase for payment record

---

## Troubleshooting

### Device Not Showing in `adb devices`

**Windows Issues:**
```powershell
# 1. Kill ADB server
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath kill-server

# 2. Start fresh
& $adbPath start-server

# 3. Check again
& $adbPath devices
```

**Phone Issues:**
- Disconnect USB cable
- Disable USB debugging
- Re-enable USB debugging
- Reconnect USB cable
- Grant permission when prompted

### Installation Fails

**Error: "INSTALL_FAILED_INVALID_APK"**
```powershell
# Reinstall with force flag
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apkPath = "C:\Users\saksh\AndroidStudioProjects\DropSpot\app\build\outputs\apk\debug\app-debug.apk"
& $adbPath install -r $apkPath
```

**Error: "INSTALL_FAILED_USER_RESTRICTED"**
- You may have already installed it
- Use `-r` flag to replace: `adb install -r app-debug.apk`

**Error: "INSTALL_FAILED_INSUFFICIENT_STORAGE"**
- Free up space on your device
- Delete unused apps or files
- Try clearing cache: `adb shell pm clear com.example.dropspot`

### App Crashes on Launch

**Check Logs:**
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath logcat
```

**Common Causes:**
- Firebase not initialized
- Missing permissions
- Network connectivity issues
- Backend server not running

### Payment Screen Not Opening

1. Verify you're logged in
2. Check internet connection
3. Ensure backend API is accessible
4. Check Android Logcat for errors

---

## Useful ADB Commands

### App Management
```powershell
$adb = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Uninstall app
& $adb uninstall com.example.dropspot

# Clear app data
& $adb shell pm clear com.example.dropspot

# View installed packages
& $adb shell pm list packages | findstr dropspot

# View app info
& $adb shell pm dump com.example.dropspot
```

### Debugging
```powershell
# View real-time logs
& $adb logcat

# Filter specific app logs
& $adb logcat | findstr dropspot

# Save logs to file
& $adb logcat > logs.txt
```

### Performance
```powershell
# Check device storage
& $adb shell df

# View memory usage
& $adb shell cat /proc/meminfo

# Battery info
& $adb shell dumpsys battery
```

---

## Test Scenarios

### Scenario 1: Successful Payment
1. Launch app
2. Create or view a post
3. Click payment button
4. Enter valid amount
5. Use test card: 4111111111111111
6. Verify success notification
7. Check Firebase payment record

### Scenario 2: Payment Cancellation
1. Launch payment screen
2. Click "Pay Now"
3. Razorpay checkout opens
4. Press back button
5. App should handle cancellation gracefully
6. Show "Payment cancelled" message

### Scenario 3: Failed Payment
1. Launch payment screen
2. Use invalid test card
3. Attempt payment
4. Should show error message
5. Verify error handling
6. Allow retry

### Scenario 4: Simulate Success (For Testing)
- Click "Simulate Success" button (if available)
- Verify payment saved without actual payment
- Useful for UI/backend testing

---

## Test Razorpay Credentials

**Test Key:** `rzp_test_JJx2tPt9AuRPvv`
**Environment:** Test Mode (No real charges)

### Valid Test Cards

**Success Cards:**
```
Card Number: 4111 1111 1111 1111
Expiry: Any future date (MM/YY)
CVV: Any 3 digits
```

**Other Test Cards:**
- Visa: 4012888888881881
- Mastercard: 5555 5555 5555 4444
- Amex: 3782 822463 10005

---

## Performance Metrics

| Metric | Value |
|--------|-------|
| APK Size | 12.35 MB |
| Min API | 24 (Android 7.0) |
| Target API | 36 (Android 14) |
| Build Time | ~33 seconds |
| Installation Time | ~5-10 seconds |

---

## Post-Installation Verification

After successful installation, verify:

- [ ] App icon appears on home screen
- [ ] App launches without crash
- [ ] Welcome/Login screen displays
- [ ] Can navigate through app
- [ ] Network requests work
- [ ] Firebase initialized
- [ ] Notification permission granted
- [ ] Payment button accessible
- [ ] Razorpay checkout appears
- [ ] Payment completes successfully

---

## Tips for Better Testing

1. **Use Strong WiFi Connection**
   - Razorpay requires stable internet
   - Test with 4G/5G if WiFi is slow

2. **Keep App Logs Open**
   ```powershell
   $adb = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat | findstr "Razorpay|Payment|PaymentActivity"
   ```

3. **Monitor Backend Logs**
   - Check your backend server logs
   - Verify payment webhook is received
   - Confirm Firebase database updates

4. **Test All Payment Flows**
   - Success path
   - Cancellation
   - Error handling
   - Network timeout

5. **Check Device Console**
   - Monitor battery usage
   - Check data consumption
   - Verify no memory leaks

---

## Success Indicators

✅ **Payment Flow Working When:**
1. App installs and launches
2. Payment Activity renders correctly
3. Amount displays properly
4. Razorpay dialog opens on button click
5. Test card payment completes
6. Success dialog shown
7. Payment saved to Firebase
8. Owner receives notification
9. No crashes or errors in Logcat

---

## Need Help?

**Check These First:**
1. Is device visible in `adb devices`? 
2. Is USB debugging enabled?
3. Is internet working on device?
4. Is backend API running?
5. Check logcat for specific errors

**Common Fixes:**
- Restart ADB: `adb kill-server && adb start-server`
- Reinstall app: `adb uninstall` then `adb install`
- Clear app cache: `adb shell pm clear com.example.dropspot`
- Reboot device: `adb reboot`

---

## Files & Locations

- **APK Location:** 
  ```
  C:\Users\saksh\AndroidStudioProjects\DropSpot\app\build\outputs\apk\debug\app-debug.apk
  ```

- **Source Code:**
  ```
  C:\Users\saksh\AndroidStudioProjects\DropSpot\app\src\main\java\com\example\dropspot\
  ```

- **Manifest:**
  ```
  C:\Users\saksh\AndroidStudioProjects\DropSpot\app\src\main\AndroidManifest.xml
  ```

- **Razorpay Configuration:**
  ```
  build.gradle.kts: razorpay:checkout:1.5.8
  AndroidManifest.xml: com.razorpay.ApiKey meta-data
  ```

---

**Status:** ✅ Ready for Testing
**Last Updated:** April 17, 2026

