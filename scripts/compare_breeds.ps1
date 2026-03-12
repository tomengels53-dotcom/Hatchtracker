# Extract breed IDs from Kotlin file (lines with: id = "some_id",)
$ktFile = "core\data\src\main\java\com\example\hatchtracker\data\repository\BreedStandardRepository.kt"
$tsFile = "scripts\seedBreedStandards.ts"

$ktLines = Get-Content $ktFile
$ktIds = @()
foreach ($line in $ktLines) {
    if ($line -match '^\s+id = "([^"]+)"') {
        $ktIds += $Matches[1]
    }
}
$ktIds = $ktIds | Sort-Object | Select-Object -Unique

# Extract breed IDs from TypeScript file (lines with: id: "some_id",)
$tsLines = Get-Content $tsFile
$tsIds = @()
foreach ($line in $tsLines) {
    if ($line -match '^\s+id:\s*"([^"]+)"') {
        $tsIds += $Matches[1]
    }
}
$tsIds = $tsIds | Sort-Object | Select-Object -Unique

Write-Host "=== IN TS BUT NOT IN KT ==="
foreach ($id in $tsIds) {
    if ($ktIds -notcontains $id) {
        Write-Host "  MISSING FROM KT: $id"
    }
}

Write-Host ""
Write-Host "=== IN KT BUT NOT IN TS ==="
foreach ($id in $ktIds) {
    if ($tsIds -notcontains $id) {
        Write-Host "  MISSING FROM TS: $id"
    }
}

Write-Host ""
Write-Host "=== SUMMARY ==="
Write-Host "KT breed count: $($ktIds.Count)"
Write-Host "TS breed count: $($tsIds.Count)"
