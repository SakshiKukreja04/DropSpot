# 🚀 Running DropSpot on Android Device via USB

## Prerequisites
✅ Android device with USB debugging enabled
✅ USB cable connected to computer
✅ ADB (Android Debug Bridge) installed
✅ Build successful (completed ✅)

## Steps to Run on Device

### 1. Enable USB Debugging on Phone
- Settings → Developer Options → USB Debugging (ON)
- Allow USB debugging when prompted

### 2. Connect via ADB
```powershell
adb devices
```
You should see your device listed as "device"

### 3. Install APK
```powershell
cd C:\Users\saksh\AndroidStudioProjects\DropSpot
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 4. Run App
```powershell
adb shell am start -n com.example.dropspot/.WelcomeActivity
```

### 5. View Logs (Optional)
```powershell
adb logcat | findstr "DropSpot\|Firebase\|FCM"
```

## Testing Flow

1. **Payment Test**
   - Card: 4111111111111111
   - Expiry: 12/25
   - CVV: 123
   - Address: Any valid address

2. **Check Notifications**
   - Pull down notification panel
   - Verify FCM notifications arrive

3. **Verify Status Updates**
   - Status should change to "Paid"/"Dispatched"/"Delivered"
   - Tracking number should appear

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "no devices" | Check USB debugging is ON |
| "Installation failed" | Clear: `adb shell pm clear com.example.dropspot` |
| App crashes | Check logcat for errors |
| No notifications | Verify FCM token in Firestore |

✅ Ready to test!

