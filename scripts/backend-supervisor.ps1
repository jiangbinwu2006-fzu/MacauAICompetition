param(
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
$processPath = $env:Path
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
if (-not $env:Path) { [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process') }
$runtimeDir = Join-Path $ProjectRoot '.runtime'
$backendDir = Join-Path $ProjectRoot 'ai-tourism-backend'
$preferredJar = Join-Path $backendDir 'target\ai-tourism-2.0.0.jar'
$jarPath = if (Test-Path -LiteralPath $preferredJar) {
    $preferredJar
} else {
    Get-ChildItem -LiteralPath (Join-Path $backendDir 'target') -Filter 'ai-tourism-*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '\.original$' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
$stopFlag = Join-Path $runtimeDir 'backend.stop'
$pidFile = Join-Path $runtimeDir 'backend.pid'
$supervisorLog = Join-Path $runtimeDir 'backend-supervisor.log'
$stdoutLog = Join-Path $runtimeDir 'backend.out.log'
$stderrLog = Join-Path $runtimeDir 'backend.err.log'

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
if (-not $jarPath -or -not (Test-Path -LiteralPath $jarPath)) {
    throw "Backend jar not found: $jarPath"
}

while (-not (Test-Path -LiteralPath $stopFlag)) {
    $java = (Get-Command java -ErrorAction Stop).Source
    $process = Start-Process -FilePath $java `
        -ArgumentList '-jar', $jarPath `
        -WorkingDirectory $backendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru
    Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ascii
    Add-Content -LiteralPath $supervisorLog -Value "$(Get-Date -Format o) backend started pid=$($process.Id)"
    $process.WaitForExit()
    Add-Content -LiteralPath $supervisorLog -Value "$(Get-Date -Format o) backend exited code=$($process.ExitCode)"
    if (-not (Test-Path -LiteralPath $stopFlag)) { Start-Sleep -Seconds 3 }
}

Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
