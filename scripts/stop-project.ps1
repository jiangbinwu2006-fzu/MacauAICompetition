$ErrorActionPreference = 'SilentlyContinue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $projectRoot '.runtime'

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
New-Item -ItemType File -Path (Join-Path $runtimeDir 'backend.stop') -Force | Out-Null

foreach ($name in @('backend.pid', 'backend-supervisor.pid', 'frontend.pid')) {
    $path = Join-Path $runtimeDir $name
    if (Test-Path -LiteralPath $path) {
        $processId = Get-Content -LiteralPath $path -ErrorAction SilentlyContinue
        if ($processId) { Stop-Process -Id ([int]$processId) -Force -ErrorAction SilentlyContinue }
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'Project-managed processes stopped.'
