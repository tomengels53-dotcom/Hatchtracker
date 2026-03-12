# Audit which breeds are missing baselineGenotype in seedBreedStandards.ts
$tsFile = "scripts\seedBreedStandards.ts"
$lines = Get-Content $tsFile

$currentId = ""
$foundBaseline = $false
$missing = @()
$hasData = @()
$inBreed = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i].Trim()
    
    if ($line -match '^\s*id:\s*"([^"]+)"') {
        if ($currentId -ne "" -and -not $foundBaseline) {
            $missing += $currentId
        }
        elseif ($currentId -ne "" -and $foundBaseline) {
            $hasData += $currentId
        }
        $currentId = $Matches[1]
        $foundBaseline = $false
    }
    
    if ($line -match 'baselineGenotype') {
        $foundBaseline = $true
    }
}

# Handle last breed
if ($currentId -ne "") {
    if (-not $foundBaseline) { $missing += $currentId } else { $hasData += $currentId }
}

Write-Host "=== MISSING baselineGenotype ($($missing.Count) breeds) ==="
$missing | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "=== HAS baselineGenotype ($($hasData.Count) breeds) ==="
Write-Host "Total breeds: $($missing.Count + $hasData.Count)"
