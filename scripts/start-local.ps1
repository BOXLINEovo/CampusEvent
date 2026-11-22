$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$pgHome = Join-Path $env:LOCALAPPDATA 'CampusEvent\pgsql'
$pgData = Join-Path $env:LOCALAPPDATA 'CampusEvent\pgdata'
$jar = Join-Path $root 'target\campus-event-0.0.1-SNAPSHOT.jar'
$appOut = Join-Path $root 'target\app-start.log'
$appErr = Join-Path $root 'target\app-start.err.log'

function Test-Port([int]$port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Resolve-Java {
    $candidates = @()
    if ($env:JAVA_HOME) {
        # JAVA_HOME may point to either the JDK home or its bin directory.
        $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe')
        $candidates += (Join-Path $env:JAVA_HOME 'java.exe')
    }
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidates += $javaCommand.Source
    }
    $candidates += Get-ChildItem -Path (Join-Path $env:USERPROFILE 'jsp\jdk*\bin\java.exe') -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
    $candidates += Get-ChildItem -Path (Join-Path $env:ProgramFiles 'Java\jdk*\bin\java.exe') -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }
    throw "Java not found. Set JAVA_HOME to the JDK home (or its bin directory), or add java to PATH."
}

if (-not (Test-Path -LiteralPath (Join-Path $pgHome 'bin\pg_ctl.exe'))) {
    throw "PostgreSQL not found at $pgHome. Prepare the portable PostgreSQL first."
}

if (-not (Test-Port 5432)) {
    & (Join-Path $pgHome 'bin\pg_ctl.exe') -D $pgData -l (Join-Path $pgData 'server.log') -w start
}

if (-not (Test-Port 8080)) {
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Jar not found: $jar. Build it with: mvn -DskipTests package"
    }
    $java = Resolve-Java
    Start-Process -FilePath $java `
        -ArgumentList @('-jar', $jar, '--spring.profiles.active=dev') `
        -WorkingDirectory $root `
        -RedirectStandardOutput $appOut `
        -RedirectStandardError $appErr `
        -WindowStyle Hidden

    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Port 8080) { break }
        Start-Sleep -Seconds 1
    }
}

"Java:       $(Resolve-Java)"
"PostgreSQL: $(if (Test-Port 5432) { 'UP' } else { 'DOWN' })"
"App:        $(if (Test-Port 8080) { 'UP' } else { 'DOWN' })"
"Home:       http://localhost:8080/"
"Health:     http://localhost:8080/health"
