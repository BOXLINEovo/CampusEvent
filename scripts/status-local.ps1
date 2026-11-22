function Test-Port([int]$port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

"PostgreSQL (5432): $(if (Test-Port 5432) { 'UP' } else { 'DOWN' })"
"App (8080):        $(if (Test-Port 8080) { 'UP' } else { 'DOWN' })"
