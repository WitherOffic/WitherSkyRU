param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskChecks=Join-Path $taskProject 'build/performance67-416d3fe89d4848579abd66c716a5a057'
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar'
$taskQA=Get-Content -LiteralPath (Join-Path $taskProject 'QA_REPORT.json') -Raw|ConvertFrom-Json
$taskScope=Get-Content -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Raw|ConvertFrom-Json
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskDigest -ne '8676237B43BF896DD362FE7F6C89F84B75A2E42901E9793737593E64583779F7' -or $taskDigest -ne $taskScope.runtime_jar_sha256 -or $taskDigest -ne $taskQA.latest_verified_batch.jar_sha256){throw 'Different unverified JAR'}
if($taskScope.tests_passed.Count -ne 118 -or $taskScope.targeted_only){throw 'Incomplete regression scope'}
foreach($taskTest in $taskScope.tests_passed){$taskFile=Join-Path $taskChecks "$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item -LiteralPath $taskFile).Length -eq 0){throw "Missing check: $taskTest"}}
if((Get-Content -LiteralPath (Join-Path $taskChecks 'TestRenderContext67-output.txt') -Raw) -notmatch '30911 assertions'){throw 'Missing independent oracle'}
$taskDest=(Resolve-Path -LiteralPath $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar','WitherSkyRU_0.15.67-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.67-dev_performance_checks.zip','PERFORMANCE_0.15.67_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve existing artifact: $taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery67-'+[Guid]::NewGuid().ToString('N'));$taskEvidence=Join-Path $taskRoot 'performance-checks'
New-Item -ItemType Directory -Path (Join-Path $taskEvidence 'checks')|Out-Null
Copy-Item -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Destination (Join-Path $taskEvidence 'checks')
foreach($taskTest in $taskScope.tests_passed){Copy-Item -LiteralPath (Join-Path $taskChecks "$taskTest-output.txt") -Destination (Join-Path $taskEvidence 'checks')}
foreach($taskName in @('PERFORMANCE_0.15.67_RU.md','PERFORMANCE_0.15.67_RAW.json','PERFORMANCE_0.15.67_RECHECK.json','JAR_SCOPE_0.15.67.json','FPS_LIVE_PROFILE_2026_10_15_SUMMARY.json')){Copy-Item -LiteralPath (Join-Path $taskProject "translation-source/$taskName") -Destination $taskEvidence}
$taskFiles=@(Get-ChildItem -LiteralPath $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
$taskManifest=@{version='0.15.67-dev';jar_sha256=$taskDigest;live_profile_runtime='0.15.64-dev';new67_game_fps_measured=$false;raw_jfr_included=$false;all_translations_unchanged_vs66=$true;installed_pack_modified=$false;reproduction='Project source includes new context oracle and benchmark scripts. Delivered66 is the pinned behavior/performance baseline. Restore historical fixture/JAR roots listed by check_performance67.ps1 for all118 regressions. Live profile is an observational baseline64 capture, not a67-vs66 gameplay A/B; raw JFR stays only in local build for privacy.';files=$taskFiles}
[IO.File]::WriteAllText((Join-Path $taskEvidence 'MANIFEST.json'),($taskManifest|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem -LiteralPath $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& (Join-Path $PSScriptRoot 'package_dev.ps1') -OutputDirectory $taskDest
Copy-Item -LiteralPath (Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.67_RU.md') -Destination (Join-Path $taskDest $taskNames[3])
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){
    $taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0
    try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskLocal=Join-Path $taskArchive.root $taskEntry.FullName;$taskStream=$taskEntry.Open();try{$taskHash=[Security.Cryptography.SHA256]::Create();try{$taskActual=[BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskHash.Dispose()}}finally{$taskStream.Dispose()};if($taskActual -ne (Get-FileHash -LiteralPath $taskLocal).Hash){throw "Archive mismatch: $($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()}
    Write-Output "VERIFIED ARCHIVE $($taskArchive.file): $taskCount files"
}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskDigest){throw 'Copied JAR differs'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item -LiteralPath $taskFile).Length+' bytes SHA256 '+(Get-FileHash -LiteralPath $taskFile).Hash)}
Write-Output "DELIVERY EVIDENCE ROOT: $taskRoot"
