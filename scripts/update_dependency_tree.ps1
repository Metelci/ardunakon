$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $repoRoot "gradlew.bat"
$outFile = Join-Path $repoRoot "dependency_tree.txt"

if (-not (Test-Path $gradlew)) {
  throw "gradlew.bat not found at $gradlew"
}

Write-Host "Generating dependency tree..."
& $gradlew :app:dependencies | Out-File -FilePath $outFile -Encoding utf8

Write-Host "Wrote $outFile"
