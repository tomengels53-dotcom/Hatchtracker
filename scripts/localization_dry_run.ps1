param(
    [string]$BaseFile = "core/ui/src/main/res/values/strings.xml",
    [string]$ResRoot = "core/ui/src/main/res",
    [string]$OutputRoot = "build/localization_dry_run/core-ui",
    [switch]$IncludeDutch = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-PlaceholderTokens {
    param([string]$Value)
    if ($null -eq $Value) { return @() }
    $matches = [regex]::Matches($Value, "%(?:\d+\$)?[dsf]")
    return @($matches | ForEach-Object { $_.Value } | Sort-Object -Unique)
}

function Is-MojibakeSuspect {
    param([string]$Value)
    if ([string]::IsNullOrEmpty($Value)) { return $false }
    return $Value -match "\u00C3|\u00C2|\u00E2|\uFFFD"
}

function Escape-XmlValue {
    param([string]$Value)
    if ($null -eq $Value) { return "" }
    return $Value.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
}

if (!(Test-Path $BaseFile)) {
    throw "Base file not found: $BaseFile"
}

$baseXml = [xml](Get-Content $BaseFile -Raw)
$baseStrings = @{}
$baseOrder = @()
foreach ($node in $baseXml.resources.string) {
    $name = [string]$node.name
    $value = [string]$node.InnerText
    $baseStrings[$name] = $value
    $baseOrder += $name
}

$localeDirs = Get-ChildItem -Directory $ResRoot | Where-Object { $_.Name -like "values-*" }
if (-not $IncludeDutch) {
    $localeDirs = $localeDirs | Where-Object { $_.Name -ne "values-nl" }
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
$summary = @()

foreach ($dir in $localeDirs) {
    $localeName = $dir.Name
    $sourceFile = Join-Path $dir.FullName "strings.xml"
    if (!(Test-Path $sourceFile)) { continue }

    $localeRaw = Get-Content $sourceFile -Raw
    $localeMap = @{}
    $stringMatches = [regex]::Matches(
        $localeRaw,
        "<string\s+name=`"([^`"]+)`">(.*?)</string>",
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
    foreach ($m in $stringMatches) {
        $k = [string]$m.Groups[1].Value
        $vRaw = [string]$m.Groups[2].Value
        $v = [System.Net.WebUtility]::HtmlDecode($vRaw.Trim())
        $localeMap[$k] = $v
    }

    $baseKeys = @($baseStrings.Keys)
    $localeKeys = @($localeMap.Keys)
    $missing = @($baseKeys | Where-Object { -not $localeMap.ContainsKey($_) } | Sort-Object)
    $extra = @($localeKeys | Where-Object { -not $baseStrings.ContainsKey($_) } | Sort-Object)

    $placeholderMismatch = New-Object System.Collections.Generic.List[string]
    $mojibakeKeys = New-Object System.Collections.Generic.List[string]

    $candidateLines = New-Object System.Collections.Generic.List[string]
    $candidateLines.Add("<resources>")

    $fallbackMissing = 0
    $fallbackPlaceholder = 0
    $fallbackMojibake = 0

    foreach ($key in $baseOrder) {
        $baseValue = $baseStrings[$key]
        $finalValue = $baseValue

        if ($localeMap.ContainsKey($key)) {
            $locValue = [string]$localeMap[$key]
            $baseTokenSig = (@(Get-PlaceholderTokens $baseValue) -join "|")
            $locTokenSig = (@(Get-PlaceholderTokens $locValue) -join "|")

            if (Is-MojibakeSuspect $locValue) {
                $mojibakeKeys.Add($key) | Out-Null
                $fallbackMojibake++
            } elseif ($baseTokenSig -ne $locTokenSig) {
                $placeholderMismatch.Add($key) | Out-Null
                $fallbackPlaceholder++
            } else {
                $finalValue = $locValue
            }
        } else {
            $fallbackMissing++
        }

        $candidateLines.Add("  <string name=`"$key`">$(Escape-XmlValue $finalValue)</string>")
    }

    $candidateLines.Add("</resources>")

    $outDir = Join-Path $OutputRoot $localeName
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $outFile = Join-Path $outDir "strings.xml"
    [System.IO.File]::WriteAllLines($outFile, $candidateLines, (New-Object System.Text.UTF8Encoding($false)))

    $summary += [pscustomobject][ordered]@{
        locale = $localeName
        baseKeyCount = $baseOrder.Count
        localeKeyCount = $localeKeys.Count
        missingKeyCount = $missing.Count
        extraKeyCount = $extra.Count
        placeholderMismatchCount = $placeholderMismatch.Count
        mojibakeSuspectCount = $mojibakeKeys.Count
        fallbackFromMissing = $fallbackMissing
        fallbackFromPlaceholder = $fallbackPlaceholder
        fallbackFromMojibake = $fallbackMojibake
        candidateFile = $outFile
        missingKeys = $missing
        extraKeys = $extra
        placeholderMismatchKeys = $placeholderMismatch
        mojibakeSuspectKeys = $mojibakeKeys
    }
}

$jsonPath = Join-Path $OutputRoot "dry_run_report.json"
$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding utf8

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Localization Dry Run Report")
$md.Add("")
$md.Add("Base key count: $($baseOrder.Count)")
$md.Add("")
$md.Add("| Locale | Missing | Extra | Placeholder Mismatch | Mojibake Suspect | Candidate |")
$md.Add("|---|---:|---:|---:|---:|---|")
foreach ($row in $summary) {
    $md.Add("| $($row.locale) | $($row.missingKeyCount) | $($row.extraKeyCount) | $($row.placeholderMismatchCount) | $($row.mojibakeSuspectCount) | $($row.candidateFile) |")
}

$mdPath = Join-Path $OutputRoot "dry_run_report.md"
[System.IO.File]::WriteAllLines($mdPath, $md, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Dry run complete."
Write-Host "Report JSON: $jsonPath"
Write-Host "Report MD:   $mdPath"
