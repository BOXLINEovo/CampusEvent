$ErrorActionPreference = 'Stop'

$pgHome = Join-Path $env:LOCALAPPDATA 'CampusEvent\pgsql'
$pgData = Join-Path $env:LOCALAPPDATA 'CampusEvent\pgdata'

function Test-Port([int]$port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

$app = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($app) {
    Stop-Process -Id $app.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

if (Test-Port 5432) {
    & (Join-Path $pgHome 'bin\pg_ctl.exe') -D $pgData -m fast -w stop
}

"PostgreSQL: $(if (Test-Port 5432) { 'UP' } else { 'DOWN' })"
"App:        $(if (Test-Port 8080) { 'UP' } else { 'DOWN' })"
