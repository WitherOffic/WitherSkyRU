param([string]$PrismRoot = (Join-Path ([Environment]::GetFolderPath('ApplicationData')) 'PrismLauncher'))
$ErrorActionPreference = 'Stop'
# Convenience only. Gradle resolves versioned dependencies, not installed mods.
if (!$env:JAVA_HOME) {
    $taskJava = Join-Path $PrismRoot 'java/java-runtime-epsilon'
    if (Test-Path -LiteralPath (Join-Path $taskJava 'bin/java.exe')) { $env:JAVA_HOME = $taskJava }
}
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat clean build --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'build/libs') -Filter '*.jar' | Select-Object -ExpandProperty FullName
} finally { Pop-Location }
