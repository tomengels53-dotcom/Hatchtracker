# Phase 1: Fix ID mismatches in seedBreedStandards.ts
$tsFile = "scripts\seedBreedStandards.ts"

$content = Get-Content $tsFile -Raw

# Fix known ID mismatches (TS → canonical KT IDs)
$content = $content -replace '"id: "narragansett"', 'id: "narragansett_turkey"'
$content = $content -replace 'id: "narragansett",', 'id: "narragansett_turkey",'
$content = $content -replace 'id: "new_hampshire",', 'id: "new_hampshire_red",'
$content = $content -replace 'id: "swedish_blue",', 'id: "swedish_blue_duck",'
$content = $content -replace 'id: "dendermondse_eend",', 'id: "dendermonde_duck",'

Set-Content $tsFile $content -NoNewline

Write-Host "Phase 1 done: ID mismatches fixed."
