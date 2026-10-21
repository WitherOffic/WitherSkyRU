param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskChecks=Join-Path $taskProject 'build/safety66-9748a3a1585c46e492b20cbcd70a9cd4'
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.66-dev.jar'
$taskQA=Get-Content -LiteralPath (Join-Path $taskProject 'QA_REPORT.json') -Raw|ConvertFrom-Json
$taskScope=Get-Content -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Raw|ConvertFrom-Json
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskDigest -ne 'B36538B44ADD46533576505A186583092CF04E508A1F472BFAC07E6BE5B62A59' -or $taskDigest -ne $taskScope.runtime_jar_sha256 -or $taskDigest -ne $taskQA.latest_verified_batch.jar_sha256){throw 'Different unverified JAR'}
if($taskScope.tests_passed.Count -ne 117 -or $taskScope.targeted_only){throw 'Incomplete regression scope'}
foreach($taskTest in $taskScope.tests_passed){$taskFile=Join-Path $taskChecks "$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item -LiteralPath $taskFile).Length -eq 0){throw "Missing check: $taskTest"}}
if((Get-Content -LiteralPath (Join-Path $taskChecks 'TestSafetyQuiz66-output.txt') -Raw) -notmatch '183309 assertions'){throw 'Missing independent check'}
$taskDest=(Resolve-Path -LiteralPath $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.66-dev.jar','WitherSkyRU_0.15.66-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.66-dev_safety_checks.zip','SAFETY_0.15.66_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve existing artifact: $taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery66-'+[Guid]::NewGuid().ToString('N'))
$taskEvidence=Join-Path $taskRoot 'safety-checks'
New-Item -ItemType Directory -Path (Join-Path $taskEvidence 'checks')|Out-Null
Copy-Item -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Destination (Join-Path $taskEvidence 'checks')
foreach($taskTest in $taskScope.tests_passed){Copy-Item -LiteralPath (Join-Path $taskChecks "$taskTest-output.txt") -Destination (Join-Path $taskEvidence 'checks')}
foreach($taskName in @('SAFETY_0.15.66_RU.md','SAFETY_QUIZ_OBSERVED_0.15.66.json','JAR_SCOPE_0.15.66.json','PERFORMANCE_0.15.66_RAW.json')){Copy-Item -LiteralPath (Join-Path $taskProject "translation-source/$taskName") -Destination $taskEvidence}
$taskFiles=@(Get-ChildItem -LiteralPath $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
$taskManifest=@{version='0.15.66-dev';jar_sha256=$taskDigest;observed_quiz_questions=3;observed_quiz_answers=9;all_quests_translated=$false;game_fps_measured=$false;game_started=$false;reproduction='Source archive has new independent test and scripts. Restore historical fixtures/JARs at their retained build paths for the whole117-group regression; CHECK_SCOPE explicitly distinguishes archived64/65 catalogue diffs from current66 behavior. Evidence contains only18 selected public quest messages, not original game logs or personal form inputs.';files=$taskFiles}
[IO.File]::WriteAllText((Join-Path $taskEvidence 'MANIFEST.json'),($taskManifest|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem -LiteralPath $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& (Join-Path $PSScriptRoot 'package_dev.ps1') -OutputDirectory $taskDest
Copy-Item -LiteralPath (Join-Path $taskProject 'translation-source/SAFETY_0.15.66_RU.md') -Destination (Join-Path $taskDest $taskNames[3])
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){
    $taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0
    try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskLocal=Join-Path $taskArchive.root $taskEntry.FullName;$taskStream=$taskEntry.Open();try{$taskHash=[Security.Cryptography.SHA256]::Create();try{$taskActual=[BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskHash.Dispose()}}finally{$taskStream.Dispose()};if($taskActual -ne (Get-FileHash -LiteralPath $taskLocal).Hash){throw "Archive mismatch: $($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()}
    Write-Output "VERIFIED ARCHIVE $($taskArchive.file): $taskCount files"
}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskDigest){throw 'Copied JAR differs'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item -LiteralPath $taskFile).Length+' bytes SHA256 '+(Get-FileHash -LiteralPath $taskFile).Hash)}
Write-Output "DELIVERY EVIDENCE ROOT: $taskRoot"
