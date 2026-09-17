param(
    [string]$Path = 'docs\09-postman\fixna-localboost-api-v1.postman_collection.json'
)
$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    Write-Host "MISSING: $Path"
    exit 2
}

$raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
$bytes = (Get-Item -LiteralPath $Path).Length
Write-Host ("BYTES=" + $bytes)

try {
    $doc = $raw | ConvertFrom-Json
    Write-Host 'JSON_VALID'
} catch {
    Write-Host ("JSON_INVALID: " + $_.Exception.Message)
    exit 3
}

Write-Host ("schema=" + $doc.info.schema)
Write-Host ("name=" + $doc.info.name)

$markers = [regex]::Matches($raw, '__CHUNK_MARKER_\d+__')
if ($markers.Count -gt 0) {
    Write-Host ("LEFTOVER_MARKERS=" + $markers.Count + " -> " + (($markers | ForEach-Object { $_.Value }) -join ', '))
} else {
    Write-Host 'LEFTOVER_MARKERS=0'
}

$script:total = 0
function Show-Items {
    param($Items, [string]$Indent)
    foreach ($item in $Items) {
        if ($item.PSObject.Properties['item']) {
            Write-Host ($Indent + '[FOLDER] ' + $item.name)
            Show-Items -Items $item.item -Indent ($Indent + '  ')
        } else {
            $script:total++
            $m = $item.request.method
            $u = $item.request.url
            if ($u -isnot [string]) { $u = $u.raw }
            Write-Host ($Indent + $m + ' ' + $u + '   # ' + $item.name)
        }
    }
}

if ($doc.PSObject.Properties['item']) {
    Show-Items -Items $doc.item -Indent ''
}
Write-Host ("TOTAL_REQUESTS=" + $script:total)
