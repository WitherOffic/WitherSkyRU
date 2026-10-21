param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskQA=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($taskQA.latest_verified_batch.version -ne '0.15.69-dev' -or $taskQA.latest_verified_batch.packaged_groups_passed -ne 120){throw 'Incomplete verified milestone'}
$taskChecks=Join-Path $taskProject $taskQA.latest_verified_batch.packaged_gate_root
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar";$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskHash -ne $taskQA.latest_verified_batch.jar_sha256 -or $taskHash -ne $taskScope.runtime_jar_sha256 -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 120 -or (Get-Content "$taskChecks/TestHubNpcs69-output.txt" -Raw) -notmatch '80809 assertions'){throw 'Different executable from final checks'}
$taskDest=(Resolve-Path -LiteralPath $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar','WitherSkyRU_0.15.69-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.69-dev_npc_checks.zip','HUB_NPC_0.15.69_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve delivered artifact: $taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery69-'+[Guid]::NewGuid().ToString('N'));$taskEvidence="$taskRoot/npc-checks";New-Item -ItemType Directory -Path "$taskEvidence/checks"|Out-Null
Copy-Item -LiteralPath "$taskChecks/CHECK_SCOPE.json" -Destination "$taskEvidence/checks"
foreach($taskTest in $taskScope.tests_passed){$taskFile="$taskChecks/$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item -LiteralPath $taskFile).Length -eq 0){throw "Missing successful output $taskTest"};Copy-Item -LiteralPath $taskFile -Destination "$taskEvidence/checks"}
foreach($taskName in @('HUB_NPC_0.15.69_RU.md','HUB_NPC_0.15.69_REVIEWED.tsv','HUB_NPC_OBSERVED_0.15.69.json','HUB_NPC_AUDIT_0.15.69.json','PERFORMANCE_0.15.69_RAW.json','PERFORMANCE_0.15.69_RECHECK.json','JAR_SCOPE_0.15.69.json')){Copy-Item -LiteralPath "$taskProject/translation-source/$taskName" -Destination $taskEvidence}
foreach($taskName in @('REPLAY-0.15.68-dev.json','REPLAY-0.15.69-dev.json')){Copy-Item -LiteralPath "$taskChecks/$taskName" -Destination $taskEvidence}
Copy-Item -LiteralPath "$taskChecks/audit/summary.json" -Destination "$taskEvidence/CATALOGUE_SUMMARY.json"
$taskFiles=@(Get-ChildItem -LiteralPath $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
[IO.File]::WriteAllText("$taskEvidence/MANIFEST.json",(@{version='0.15.69-dev';jar_sha256=$taskHash;scope='120 successful groups on final JAR;83 exact additions/two numeric forms;finite79 public NPC samples;8937 identical card records vs68;28 benchmark plus12 separate repeat processes;not all NPCs or game FPS';installed_modpack_modified=$false;full_private_logs_or_raw_jfr_included=$false;files=$taskFiles}|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem -LiteralPath $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& "$taskProject/tools/package_dev.ps1" -OutputDirectory $taskDest
Copy-Item -LiteralPath "$taskProject/translation-source/HUB_NPC_0.15.69_RU.md" -Destination (Join-Path $taskDest $taskNames[3])
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){
 $taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0
 try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};if($taskEntry.FullName -match '(?i)\.(?:jfr|log)$|(?:^|/)unknown\.tsv$'){throw 'Unexpected private diagnostics in archive'};$taskStream=$taskEntry.Open();try{$taskEntryHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()};if($taskEntryHash -ne (Get-FileHash -LiteralPath (Join-Path $taskArchive.root $taskEntry.FullName)).Hash){throw "Archive mismatch $($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()}
 Write-Output "VERIFIED ARCHIVE $($taskArchive.file): $taskCount files"
}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskHash){throw 'Delivered JAR hash mismatch'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item -LiteralPath $taskFile).Length+' bytes SHA256 '+(Get-FileHash -LiteralPath $taskFile).Hash)}
Write-Output "DELIVERY69 ROOT: $taskRoot"
