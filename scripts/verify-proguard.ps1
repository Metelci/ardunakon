# ProGuard Mapping File Verification Script
# Verifies that critical classes are present in the ProGuard mapping file
# Usage: .\scripts\verify-proguard.ps1

$ErrorActionPreference = "Stop"

$MappingFile = "app\build\outputs\mapping\release\mapping.txt"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "ProGuard Rule Verification" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if mapping file exists
$MappingPath = Join-Path $ProjectRoot $MappingFile
if (-not (Test-Path $MappingPath)) {
    Write-Host "❌ Mapping file not found: $MappingFile" -ForegroundColor Red
    Write-Host ""
    Write-Host "Run the following command first:"
    Write-Host "  .\gradlew assembleRelease"
    Write-Host ""
    exit 1
}

Write-Host "📄 Checking: $MappingFile" -ForegroundColor Yellow
Write-Host ""

# Critical classes that MUST be present in release builds
$CriticalClasses = @(
    "AppBluetoothManager",
    "BluetoothScanner",
    "BleConnectionManager",
    "ClassicConnectionManager",
    "ProtocolManager",
    "CryptoEngine",
    "ProfileManager",
    "WifiManager",
    "TelemetryManager",
    "ConnectionStateManager"
)

# Read mapping file content
$MappingContent = Get-Content $MappingPath -Raw

# Track results
$FailedChecks = 0
$PassedChecks = 0

# Check each critical class
foreach ($ClassName in $CriticalClasses) {
    if ($MappingContent -match $ClassName) {
        Write-Host "✓ $ClassName" -ForegroundColor Green
        $PassedChecks++
    } else {
        Write-Host "✗ $ClassName MISSING" -ForegroundColor Red
        $FailedChecks++
    }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Results: $PassedChecks passed, $FailedChecks failed" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Exit with error if any checks failed
if ($FailedChecks -gt 0) {
    Write-Host "❌ ProGuard verification FAILED!" -ForegroundColor Red
    Write-Host ""
    Write-Host "One or more critical classes are missing from the release build."
    Write-Host "This likely means ProGuard rules in app/proguard-rules.pro are incorrect."
    Write-Host ""
    Write-Host "Action required:"
    Write-Host "  1. Review app/proguard-rules.pro"
    Write-Host "  2. Add missing -keep rules for the classes listed above"
    Write-Host "  3. Rebuild and re-run this verification"
    Write-Host ""
    exit 1
}

Write-Host "✅ All critical classes present in mapping file!" -ForegroundColor Green
Write-Host ""
Write-Host "ProGuard rules are working correctly."
Write-Host ""

# Optional: Show mapping file statistics
$TotalClasses = ($MappingContent | Select-String "^com.metelci.ardunakon" -AllMatches).Matches.Count
Write-Host "Mapping file statistics:"
Write-Host "  Total classes mapped: $TotalClasses"
Write-Host ""

exit 0
