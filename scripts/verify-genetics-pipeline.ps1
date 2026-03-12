# verify-genetics-pipeline.ps1
# This script performs a repository-wide check for violations of the Breeding/Genetics architecture.

$ErrorActionPreference = "Stop"

Write-Host "--- Breeding Architecture Enforcement Script ---" -ForegroundColor Cyan

$violations = 0

# 1. Check for direct usage of internal engines outside of the service and facade
Write-Host "[1/3] Checking for internal engine leaks..."
$internalEngines = @("GeneticProbabilityEngine", "PhenotypeResolver")
foreach ($engine in $internalEngines) {
    $matches = Get-ChildItem -Path . -Filter "*.kt" -Recurse | 
               Select-String -Pattern $engine | 
               Where-Object { 
                   $_.Path -notmatch "BreedingPredictionService.kt" -and 
                   $_.Path -notmatch "GeneticsFacade.kt" -and
                   $_.Path -notmatch "GeneticTraitCatalog.kt" -and
                   $_.Path -notmatch "GeneticsArchitectureTest.kt" -and
                   $_.Path -notmatch "Test.kt" -and
                   $_.Path -notmatch "$engine.kt" -and
                   $_.Path -notmatch "_backup_app_folder" -and
                   $_.Path -notmatch "_monolith_snapshot"
               }
    
    if ($matches) {
        Write-Host "VIOLATION: Direct usage of $engine found in:" -ForegroundColor Red
        $matches | ForEach-Object { Write-Host "  $($_.Path):$($_.LineNumber)" }
        $violations++
    }
}

# 2. Check for legacy predictOffspringTraits
Write-Host "[2/3] Checking for legacy function references..."
$legacyPatterns = @("predictOffspringTraits", "com.example.hatchtracker.domain.logic")
foreach ($pattern in $legacyPatterns) {
    $matches = Get-ChildItem -Path . -Filter "*.kt" -Recurse | 
               Select-String -Pattern $pattern |
               Where-Object { 
                   $_.Path -notmatch "GeneticsArchitectureTest.kt" -and
                   $_.Path -notmatch "_backup_app_folder" -and
                   $_.Path -notmatch "_monolith_snapshot"
               }

    if ($matches) {
        Write-Host "VIOLATION: Legacy reference '$pattern' found in:" -ForegroundColor Red
        $matches | ForEach-Object { Write-Host "  $($_.Path):$($_.LineNumber)" }
        $violations++
    }
}

# 3. Check for hardcoded species strings in ViewModels (Heuristic)
Write-Host "[3/3] Checking for hardcoded species strings in ViewModels..."
$speciesSet = @("Chicken", "Duck", "Quail", "Turkey", "Goose", "Peafowl", "Pheasant")
foreach ($species in $speciesSet) {
    $matches = Get-ChildItem -Path "feature/breeding" -Filter "*ViewModel.kt" -Recurse | 
               Select-String -Pattern "listOf\(`"$species`"|`"$species`"" |
               Where-Object { $_.Line -match "availableSpecies|listOf" }

    if ($matches) {
        Write-Host "WARNING: Potential hardcoded species '$species' found in ViewModel:" -ForegroundColor Yellow
        $matches | ForEach-Object { Write-Host "  $($_.Path):$($_.LineNumber)" }
        # Treat as violation for stricter enforcement if requested
    }
}

if ($violations -eq 0) {
    Write-Host "`nSUCCESS: Breeding architecture compliance verified." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nFAILURE: Found $violations architectural violations." -ForegroundColor Red
    exit 1
}
