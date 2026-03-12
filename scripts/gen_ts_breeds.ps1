# Phase 3: Generate TS entries for all KT breeds missing from TS
$ktFile = "core\data\src\main\java\com\example\hatchtracker\data\repository\BreedStandardRepository.kt"
$tsFile = "scripts\seedBreedStandards.ts"

# First, get all TS IDs
$tsContent = Get-Content $tsFile
$tsIds = @()
foreach ($line in $tsContent) {
    if ($line -match '^\s+id:\s*"([^"]+)"') {
        $tsIds += $Matches[1]
    }
}

# Parse KT breeds: collect block per BreedStandard(...)
$ktContent = Get-Content $ktFile -Raw
$lines = Get-Content $ktFile

# Extract each breed block from KT
$breeds = @()
$currentBreed = $null
$inBreed = $false
$depth = 0

foreach ($line in $lines) {
    $trimmed = $line.Trim()
    
    if ($trimmed -match '^BreedStandard\(') {
        $currentBreed = @{
            id = ""; name = ""; origin = ""; species = ""; eggColor = ""
            acceptedColors = @(); weightRoosterKg = 0.0; weightHenKg = 0.0
            official = "true"; recognizedBy = @(); combType = "single"
            fixedTraits = @(); inferredTraits = @(); knownGenes = @()
            category = ""; isTrueBantam = $null
        }
        $inBreed = $true
        $depth = 1
        continue
    }
    
    if ($inBreed) {
        $depth += ($trimmed.ToCharArray() | Where-Object { $_ -eq '(' } | Measure-Object).Count
        $depth -= ($trimmed.ToCharArray() | Where-Object { $_ -eq ')' } | Measure-Object).Count
        
        if ($trimmed -match 'id\s*=\s*"([^"]+)"') { $currentBreed.id = $Matches[1] }
        if ($trimmed -match 'name\s*=\s*"([^"]+)"') { $currentBreed.name = $Matches[1] }
        if ($trimmed -match 'origin\s*=\s*"([^"]+)"') { $currentBreed.origin = $Matches[1] }
        if ($trimmed -match 'species\s*=\s*"([^"]+)"') { $currentBreed.species = $Matches[1] }
        if ($trimmed -match 'eggColor\s*=\s*"([^"]+)"') { $currentBreed.eggColor = $Matches[1] }
        if ($trimmed -match 'weightRoosterKg\s*=\s*([0-9.]+)') { $currentBreed.weightRoosterKg = [double]$Matches[1] }
        if ($trimmed -match 'weightHenKg\s*=\s*([0-9.]+)') { $currentBreed.weightHenKg = [double]$Matches[1] }
        if ($trimmed -match 'combType\s*=\s*"([^"]+)"') { $currentBreed.combType = $Matches[1] }
        if ($trimmed -match 'official\s*=\s*(true|false)') { $currentBreed.official = $Matches[1] }
        if ($trimmed -match 'category\s*=\s*"([^"]+)"') { $currentBreed.category = $Matches[1] }
        if ($trimmed -match 'isTrueBantam\s*=\s*(true|false)') { $currentBreed.isTrueBantam = $Matches[1] }
        
        # Extract listOf items from recognizedBy, acceptedColors, fixedTraits, inferredTraits, knownGenes
        if ($trimmed -match 'recognizedBy\s*=\s*listOf\((.+)\)') {
            $currentBreed.recognizedBy = $Matches[1] -split '","' | ForEach-Object { $_ -replace '"', '' } | Where-Object { $_ -ne '' }
        }
        if ($trimmed -match 'acceptedColors\s*=\s*listOf\((.+)\)') {
            $currentBreed.acceptedColors = $Matches[1] -split '","' | ForEach-Object { $_ -replace '"', '' } | Where-Object { $_ -ne '' }
        }
        if ($trimmed -match 'fixedTraits\s*=\s*listOf\((.+)\)') {
            $currentBreed.fixedTraits = $Matches[1] -split '","' | ForEach-Object { $_ -replace '"', '' } | Where-Object { $_ -ne '' }
        }
        if ($trimmed -match 'inferredTraits\s*=\s*listOf\((.+)\)') {
            $currentBreed.inferredTraits = $Matches[1] -split '","' | ForEach-Object { $_ -replace '"', '' } | Where-Object { $_ -ne '' }
        }
        if ($trimmed -match 'knownGenes\s*=\s*listOf\((.+)\)') {
            $currentBreed.knownGenes = $Matches[1] -split '","' | ForEach-Object { $_ -replace '"', '' } | Where-Object { $_ -ne '' }
        }
        
        if ($depth -le 0 -and $currentBreed.id -ne "") {
            $breeds += [PSCustomObject]$currentBreed
            $inBreed = $false
            $currentBreed = $null
            $depth = 0
        }
    }
}

Write-Host "Total KT breeds parsed: $($breeds.Count)"

# Filter to only breeds missing from TS
$missingBreeds = $breeds | Where-Object { $tsIds -notcontains $_.id }
Write-Host "Breeds missing from TS: $($missingBreeds.Count)"

# Generate TS entries
$tsEntries = @()
foreach ($b in $missingBreeds) {
    $acceptedColorsStr = ($b.acceptedColors | ForEach-Object { "`"$_`"" }) -join ", "
    if (-not $acceptedColorsStr) { $acceptedColorsStr = '"Wild Type"' }
    
    $recognizedByStr = ($b.recognizedBy | ForEach-Object { "`"$_`"" }) -join ", "
    if (-not $recognizedByStr) { $recognizedByStr = "" }
    
    $fixedTraitsStr = ($b.fixedTraits | ForEach-Object { "`"$_`"" }) -join ", "
    if (-not $fixedTraitsStr) { $fixedTraitsStr = "" }
    
    $inferredTraitsStr = ($b.inferredTraits | ForEach-Object { "`"$_`"" }) -join ", "
    if (-not $inferredTraitsStr) { $inferredTraitsStr = "" }
    
    $knownGenesStr = ($b.knownGenes | ForEach-Object { "`"$_`"" }) -join ", "
    
    $officialStr = if ($b.official -eq "true") { "true" } else { "false" }
    
    $speciesMap = @{
        "Chicken" = "Chicken"; "Duck" = "Duck"; "Goose" = "Goose"
        "Turkey" = "Turkey"; "Peafowl" = "Peafowl"; "Pheasant" = "Pheasant"; "Quail" = "Quail"
    }
    $species = if ($speciesMap.ContainsKey($b.species)) { $b.species } else { "Chicken" }
    
    $categoryLine = ""
    if ($b.category -ne "") { $categoryLine = "`n        category: `"$($b.category)`"," }
    
    $isTrueBantamLine = ""
    if ($b.isTrueBantam -ne $null -and $b.isTrueBantam -ne "") { $isTrueBantamLine = "`n        isTrueBantam: $($b.isTrueBantam)," }
    
    $entry = @"
    {
        id: "$($b.id)",
        name: "$($b.name)",
        origin: "$($b.origin)",
        species: "$species",
        eggColor: "$($b.eggColor)",
        acceptedColors: [$acceptedColorsStr],
        weightRoosterKg: $($b.weightRoosterKg),
        weightHenKg: $($b.weightHenKg),
        official: $officialStr,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        recognizedBy: [$recognizedByStr],$categoryLine$isTrueBantamLine
        geneticProfile: {
            knownGenes: [$knownGenesStr],
            fixedTraits: [$fixedTraitsStr],
            inferredTraits: [$inferredTraitsStr],
            unknownTraits: [],
            confidenceLevel: "medium"
        }
    },
"@
    $tsEntries += $entry
}

# Find the last closing ]; in the TS file and insert before it
$tsText = Get-Content $tsFile -Raw

# Find position of ]; (closing the breedStandardsData array) and insert before it
$insertMarker = "];`r`n`r`nexport"
$insertMarkerAlt = "];`n`nexport"

$newEntries = "`r`n    // --- BREEDS ADDED FROM BreedStandardRepository.kt (KT→TS sync) ---`r`n" + ($tsEntries -join "`r`n")

if ($tsText -match [regex]::Escape($insertMarker)) {
    $tsText = $tsText -replace [regex]::Escape($insertMarker), ($newEntries + "`r`n" + $insertMarker)
    Set-Content $tsFile $tsText -NoNewline
    Write-Host "Inserted $($missingBreeds.Count) breed entries before export in TS file."
}
elseif ($tsText -match [regex]::Escape($insertMarkerAlt)) {
    $tsText = $tsText -replace [regex]::Escape($insertMarkerAlt), ($newEntries + "`n" + $insertMarkerAlt)
    Set-Content $tsFile $tsText -NoNewline
    Write-Host "Inserted $($missingBreeds.Count) breed entries (alt marker) before export in TS file."
}
else {
    # Fallback: look for last ]; in file
    $lastBracket = $tsText.LastIndexOf("];")
    if ($lastBracket -ge 0) {
        $tsText = $tsText.Substring(0, $lastBracket) + $newEntries + "`r`n" + $tsText.Substring($lastBracket)
        Set-Content $tsFile $tsText -NoNewline
        Write-Host "Inserted $($missingBreeds.Count) breed entries at last ]; in TS file."
    }
    else {
        Write-Host "ERROR: Could not find insertion point in TS file!"
    }
}
