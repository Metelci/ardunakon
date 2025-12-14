#!/bin/bash
# ProGuard Mapping File Verification Script
# Verifies that critical classes are present in the ProGuard mapping file
# Usage: ./scripts/verify-proguard.sh

set -e

MAPPING_FILE="app/build/outputs/mapping/release/mapping.txt"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "ProGuard Rule Verification"
echo "========================================="
echo ""

# Check if mapping file exists
if [ ! -f "$PROJECT_ROOT/$MAPPING_FILE" ]; then
    echo -e "${RED}❌ Mapping file not found: $MAPPING_FILE${NC}"
    echo ""
    echo "Run the following command first:"
    echo "  ./gradlew assembleRelease"
    echo ""
    exit 1
fi

echo -e "${YELLOW}📄 Checking: $MAPPING_FILE${NC}"
echo ""

# Critical classes that MUST be present in release builds
# If any of these are missing, ProGuard rules are broken
CRITICAL_CLASSES=(
    "AppBluetoothManager"
    "BluetoothScanner"
    "BleConnectionManager"
    "ClassicConnectionManager"
    "ProtocolManager"
    "CryptoEngine"
    "ProfileManager"
    "WifiManager"
    "TelemetryManager"
    "ConnectionStateManager"
)

# Track failures
FAILED_CHECKS=0
PASSED_CHECKS=0

# Check each critical class
for class_name in "${CRITICAL_CLASSES[@]}"; do
    if grep -q "$class_name" "$PROJECT_ROOT/$MAPPING_FILE"; then
        echo -e "${GREEN}✓${NC} $class_name"
        ((PASSED_CHECKS++))
    else
        echo -e "${RED}✗${NC} $class_name ${RED}MISSING${NC}"
        ((FAILED_CHECKS++))
    fi
done

echo ""
echo "========================================="
echo "Results: $PASSED_CHECKS passed, $FAILED_CHECKS failed"
echo "========================================="
echo ""

# Exit with error if any checks failed
if [ $FAILED_CHECKS -gt 0 ]; then
    echo -e "${RED}❌ ProGuard verification FAILED!${NC}"
    echo ""
    echo "One or more critical classes are missing from the release build."
    echo "This likely means ProGuard rules in app/proguard-rules.pro are incorrect."
    echo ""
    echo "Action required:"
    echo "  1. Review app/proguard-rules.pro"
    echo "  2. Add missing -keep rules for the classes listed above"
    echo "  3. Rebuild and re-run this verification"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ All critical classes present in mapping file!${NC}"
echo ""
echo "ProGuard rules are working correctly."
echo ""

# Optional: Show mapping file statistics
echo "Mapping file statistics:"
TOTAL_CLASSES=$(grep -c "^com.metelci.ardunakon" "$PROJECT_ROOT/$MAPPING_FILE" || echo "0")
echo "  Total classes mapped: $TOTAL_CLASSES"
echo ""

exit 0
