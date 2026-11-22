$ErrorActionPreference = 'Stop'

$pgHome = Join-Path $env:LOCALAPPDATA 'CampusEvent\pgsql'
$psql = Join-Path $pgHome 'bin\psql.exe'
$createdb = Join-Path $pgHome 'bin\createdb.exe'
$env:PGPASSWORD = 'campusevent'

if (-not (Test-Path -LiteralPath $psql)) {
    throw "psql not found at $psql"
}

$exists = & $psql -h 127.0.0.1 -p 5432 -U campusevent -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='campusevent_test'"
if ($exists.Trim() -ne '1') {
    & $createdb -h 127.0.0.1 -p 5432 -U campusevent campusevent_test
    "created database campusevent_test"
} else {
    "database campusevent_test already exists"
}
