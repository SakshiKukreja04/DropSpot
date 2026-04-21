#!/bin/bash
# DropSpot Quick Testing Script
# Usage: bash test_dropspot.sh

echo "========================================"
echo "DropSpot App - Bug Fix Verification"
echo "========================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Build
echo -e "${YELLOW}[1/4] Building APK...${NC}"
cd "C:\Users\saksh\AndroidStudioProjects\DropSpot"
./gradlew assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}[2/4] Installing APK...${NC}"
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Installation successful${NC}"
else
    echo -e "${RED}✗ Installation failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}[3/4] Starting app...${NC}"
adb shell am start -n com.example.dropspot/.MainActivity

sleep 3
echo -e "${GREEN}✓ App started${NC}"

echo ""
echo -e "${YELLOW}[4/4] Monitoring logs...${NC}"
echo -e "${YELLOW}(Press Ctrl+C to stop)${NC}"
echo ""

adb logcat | grep -E "PaymentActivity|AnnouncementsFragment|EventsAdapter|MyRequestsFragment|ERROR|WARN"

echo ""
echo -e "${GREEN}========================================"
echo "Testing Complete"
echo "========================================${NC}"

