# 🚀 DEPLOY & TEST ON USB DEVICE - STEP BY STEP

## Pre-Deployment Checklist

- [ ] Phone powered on
- [ ] USB cable available
- [ ] Computer with ADB available
- [ ] Phone battery > 20%
- [ ] Android version 7.0+

---

## STEP 1: Enable USB Debugging on Phone

### On Your Phone:
1. Go to **Settings**
2. Scroll down to **About Phone**
3. Find **Build Number** (usually near bottom)
4. Tap **Build Number** 7 times rapidly
   - You'll see message: "You're now a developer!"
5. Go back to Settings
6. Find **Developer Options** (now visible)
7. Toggle **USB Debugging** ON
8. You'll see confirmation: "Allow debugging?"
9. Tap **Allow**

✅ **USB Debugging is now ENABLED**

---

## STEP 2: Connect Phone to PC via USB

1. Take USB cable
2. Connect your phone to your PC/laptop
3. Phone screen will show: **"Allow USB debugging?"**
4. Check the box "Always allow from this computer"
5. Tap **ALLOW**

✅ **Phone is now CONNECTED**

---

## STEP 3: Verify Connection with ADB

### In PowerShell:

```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath devices
```

### Expected Output:
```
List of devices attached
ABC123DEF456                device
```

✅ **If you see "device" (not "offline"), you're connected!**

---

## STEP 4: Install the App

### Copy this command and paste into PowerShell:

```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apkPath = "C:\Users\saksh\AndroidStudioProjects\DropSpot\app\build\outputs\apk\debug\app-debug.apk"
& $adbPath install -r $apkPath
```

### What You'll See:
```
Performing Streamed Install
Success
```

✅ **App installed successfully!**

---

## STEP 5: Launch the App

### Option A: From Your Phone
1. Unlock your phone
2. Swipe left to find apps (if needed)
3. Look for **DropSpot** icon
4. Tap to open

### Option B: From Command Line
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath shell am start -n com.example.dropspot/.WelcomeActivity
```

✅ **App should open on your phone!**

---

## STEP 6: Test Payment Flow

### Navigate to Payment Screen:
1. Open the app
2. Create a post or view an existing one
3. Look for "Request", "Buy", or "Payment" button
4. Tap to navigate to Payment Activity

### You should see:
- Item title
- Amount in ₹
- Your user name
- "Pay Now" button

✅ **Payment screen loaded!**

---

## STEP 7: Test Razorpay Payment

### Click "Pay Now" button:
1. **Razorpay checkout dialog opens**
   - You'll see a form with card field
   - Email field (pre-filled)
   - Amount field

### Enter Test Card Details:
```
Card Number: 4111 1111 1111 1111
Expiry: 12/25 (any future date)
CVV: 123 (any 3 digits)
```

### Click "Pay":
- Payment processes
- You'll see success/error message
- Dialog should close

✅ **Payment flow completed!**

---

## STEP 8: Verify Payment Success

### On Your Phone:
- Should see: **"Payment Successful"** dialog
- Shows Payment ID
- Shows confirmation

### On Computer (View Logs):
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath logcat | findstr -i "payment"
```

### Check Firebase (if you have access):
- Log in to Firebase Console
- Check Firestore for payment record
- Verify payment details saved

✅ **Payment integration working!**

---

## Common Issues & Quick Fixes

### Issue: "Device not found" / "offline"

**Fix:**
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath kill-server
& $adbPath start-server
& $adbPath devices
```

Then reconnect phone and grant permission.

---

### Issue: "INSTALL_FAILED_INVALID_APK"

**Fix:**
1. Disconnect phone
2. Run: `adb uninstall com.example.dropspot`
3. Reconnect phone
4. Run install command again with `-r` flag

---

### Issue: App crashes on launch

**Check logs:**
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath logcat | findstr "ERROR\|Exception\|FATAL"
```

Common causes:
- Firebase not initialized
- Backend API not running
- Missing permissions

---

### Issue: Razorpay dialog doesn't open

**Check:**
1. Internet is working on phone
2. API Key is correct in AndroidManifest.xml
3. Logcat shows no errors
4. Verify test mode is enabled

---

### Issue: Payment appears to hang

**Wait 10 seconds, then:**
1. Check Logcat for errors
2. Try with different WiFi/4G
3. Check backend logs for issues
4. May need to restart payment flow

---

## Verification Checklist

After installing and testing:

- [ ] App icon appears on phone
- [ ] App launches without crashing
- [ ] Can navigate to payment screen
- [ ] Payment screen displays correctly
- [ ] "Pay Now" button is clickable
- [ ] Razorpay dialog opens after clicking
- [ ] Can enter test card details
- [ ] Payment processes successfully
- [ ] Success message appears
- [ ] No crashes in logcat
- [ ] Logcat shows payment logs

✅ **All items checked = Successful Integration!**

---

## View Detailed Logs (Advanced)

### Real-time logs:
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath logcat
```

### Filter by PaymentActivity:
```powershell
& $adbPath logcat | findstr "PaymentActivity"
```

### Filter by Razorpay:
```powershell
& $adbPath logcat | findstr "Razorpay"
```

### Save logs to file:
```powershell
& $adbPath logcat > payment_test_logs.txt
```

---

## Useful Commands During Testing

### Check app is running:
```powershell
$adbPath = "C:\Users\saksh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adbPath shell pidof com.example.dropspot
```

### Force stop app:
```powershell
& $adbPath shell am force-stop com.example.dropspot
```

### Clear app cache:
```powershell
& $adbPath shell pm clear com.example.dropspot
```

### Uninstall app:
```powershell
& $adbPath uninstall com.example.dropspot
```

### View app info:
```powershell
& $adbPath shell pm dump com.example.dropspot | findstr "version\|versionCode"
```

---

## After Testing

### If Everything Works ✅
1. Congratulations! Razorpay is integrated
2. Test with more transactions
3. Test error scenarios
4. Prepare for production release

### If Issues Occur ❌
1. Check logcat output
2. Verify internet connection
3. Check backend logs
4. Review Firebase configuration
5. See troubleshooting section

---

## Test Scenarios to Try

### Scenario 1: Successful Payment
- Pay with test card
- Expected: Success message
- Data: Saved to Firebase

### Scenario 2: Cancel Payment
- Click back/cancel in Razorpay
- Expected: "Payment cancelled" message
- Data: Not saved

### Scenario 3: Invalid Card
- Enter wrong expiry/CVV
- Expected: Error message from Razorpay
- Data: Not saved

### Scenario 4: Multiple Payments
- Try 2-3 payments in sequence
- Expected: Each payment tracked
- Data: All saved to Firebase

---

## Quick Reference

| Task | Command |
|------|---------|
| Check devices | `adb devices` |
| Install app | `adb install -r app-debug.apk` |
| Launch app | `adb shell am start -n com.example.dropspot/.WelcomeActivity` |
| View logs | `adb logcat` |
| Stop app | `adb shell am force-stop com.example.dropspot` |
| Uninstall | `adb uninstall com.example.dropspot` |
| Clear data | `adb shell pm clear com.example.dropspot` |

---

## Success Indicators ✅

Payment integration is working when:
1. App installs successfully
2. App launches without crash
3. Navigation works smoothly
4. Payment screen loads
5. Amount displays correctly
6. Razorpay dialog opens
7. Test payment completes
8. Success message appears
9. Data saved to Firebase
10. No errors in logcat

---

## File Locations (For Reference)

```
APK File:
C:\Users\saksh\AndroidStudioProjects\DropSpot\app\build\outputs\apk\debug\app-debug.apk

Source Code:
C:\Users\saksh\AndroidStudioProjects\DropSpot\app\src\main\java\com\example\dropspot\

Manifest:
C:\Users\saksh\AndroidStudioProjects\DropSpot\app\src\main\AndroidManifest.xml
```

---

## You're Ready! 🚀

**Everything is built and ready to test.**

Just connect your device and follow the steps above.

**Good luck with your deployment!** 🎉


