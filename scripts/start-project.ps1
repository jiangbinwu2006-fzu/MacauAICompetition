$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $projectRoot '.runtime'
$supervisorScript = Join-Path $PSScriptRoot 'backend-supervisor.ps1'

function Test-LocalPort([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        return $result.AsyncWaitHandle.WaitOne(500) -and $client.Connected
    } finally { $client.Dispose() }
}

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
Remove-Item -LiteralPath (Join-Path $runtimeDir 'backend.stop') -Force -ErrorAction SilentlyContinue

if (-not (Test-LocalPort 8290)) {
    $powershell = (Get-Command powershell.exe -ErrorAction Stop).Source
    $supervisor = Start-Process -FilePath $powershell `
        -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $supervisorScript, '-ProjectRoot', $projectRoot `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -LiteralPath (Join-Path $runtimeDir 'backend-supervisor.pid') -Value $supervisor.Id -Encoding ascii
}

if (-not (Test-LocalPort 3001)) {
    $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
    $frontendDir = Join-Path $projectRoot 'ai-tourism-frontend'
    $frontend = Start-Process -FilePath $npm `
        -ArgumentList 'run', 'dev', '--', '--host', '0.0.0.0' `
        -WorkingDirectory $frontendDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.out.log') `
        -RedirectStandardError (Join-Path $runtimeDir 'frontend.err.log') `
        -PassThru
    Set-Content -LiteralPath (Join-Path $runtimeDir 'frontend.pid') -Value $frontend.Id -Encoding ascii
}

$deadline = (Get-Date).AddSeconds(25)
while ((Get-Date) -lt $deadline -and (-not (Test-LocalPort 8290) -or -not (Test-LocalPort 3001))) {
    Start-Sleep -Milliseconds 500
}

if (-not (Test-LocalPort 8290)) { throw 'Backend failed to start on port 8290. Check .runtime/backend.err.log.' }
if (-not (Test-LocalPort 3001)) { throw 'Frontend failed to start on port 3001. Check .runtime/frontend.err.log.' }

Write-Output 'Project is running:'
Write-Output '  Visitor: http://localhost:3001/explore'
Write-Output '  Ops:     http://localhost:3001/ops'
Write-Output '  Backend: http://localhost:8290'
