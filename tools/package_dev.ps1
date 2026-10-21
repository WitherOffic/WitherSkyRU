param([Parameter(Mandatory=$true)][string]$OutputDirectory, [switch]$SourceOnly)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$metadata = Get-Content -LiteralPath (Join-Path $project 'src\main\resources\fabric.mod.json') -Raw | ConvertFrom-Json
$jarName = "WitherSkyRU-26.1.2-fabric-$($metadata.version).jar"
$jar = Join-Path $project "build\libs\$jarName"
if (!$SourceOnly -and !(Test-Path -LiteralPath $jar)) { throw 'Compile the development JAR before packaging, or use -SourceOnly to save sources without compilation.' }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$destinationJar = Join-Path $OutputDirectory $jarName
if (!$SourceOnly -and (Test-Path -LiteralPath $destinationJar)) { throw "Output already exists: $destinationJar" }
$archive = Join-Path $OutputDirectory "WitherSkyRU_$($metadata.version)_MC26.1.2_project.zip"
if (Test-Path -LiteralPath $archive) { throw "Output already exists: $archive" }
if (!$SourceOnly) { Copy-Item -LiteralPath $jar -Destination $destinationJar }
$included = @('src','tools','tests','licenses','translation-source','build-local.ps1','build.gradle','settings.gradle','gradle.properties','LICENSE','THIRD_PARTY_NOTICES.md','README_RU.md','CHANGELOG_RU.md','QA_REPORT.json')
$paths = @($included | ForEach-Object { Join-Path $project $_ })
Compress-Archive -LiteralPath $paths -DestinationPath $archive -CompressionLevel Optimal
if ($SourceOnly) { Get-Item -LiteralPath $archive | Select-Object FullName,Length }
else { Get-Item -LiteralPath $destinationJar,$archive | Select-Object FullName,Length }
