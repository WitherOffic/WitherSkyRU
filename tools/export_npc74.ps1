param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskBatch=(Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json).latest_verified_batch
if($taskBatch.version -ne '0.15.74-dev' -or $taskBatch.packaged_groups_passed -ne 126 -or $taskBatch.new_exact_npc_rows -ne 239){throw 'Unverified milestone'}
$taskChecks=Join-Path $taskProject $taskBatch.packaged_gate_root;$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
if($taskHash -ne $taskBatch.jar_sha256 -or $taskHash -ne $taskScope.runtime_jar_sha256 -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 126){throw 'Different executable from final gate'}
$taskDest=(Resolve-Path $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar','WitherSkyRU_0.15.74-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.74-dev_npc_checks.zip','NPC_0.15.74_RU.md','NPC_REGISTRY_0.15.74_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve delivered artifact:$taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery74-'+[Guid]::NewGuid().ToString('N'));$taskEvidence="$taskRoot/npc-checks";New-Item -ItemType Directory -Path "$taskEvidence/checks"|Out-Null
Copy-Item -LiteralPath "$taskChecks/CHECK_SCOPE.json" -Destination "$taskEvidence/checks"
foreach($taskTest in $taskScope.tests_passed){$taskFile="$taskChecks/$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item $taskFile).Length -eq 0){throw "Missing successful check:$taskTest"};Copy-Item -LiteralPath $taskFile -Destination "$taskEvidence/checks"}
foreach($taskFile in @('NPC_0.15.74_RU.md','NPC_REGISTRY_0.15.74_RU.md','NPC_AUDIT_0.15.74.json','NPC_INDEX_0.15.74.json','NPC_SOURCES_0.15.74.json','NPC_0.15.74_REVIEWED.tsv','NPC_RETAINED_0.15.74.json','NPC_RETAINED_RUNTIME_0.15.74.json','NPC74_RETAINED_RUNTIME_PROOF.json','NPC_TERMS_0.15.74.json','PERFORMANCE_0.15.74_RAW.json','JAR_SCOPE_0.15.74.json')){Copy-Item -LiteralPath "$taskProject/translation-source/$taskFile" -Destination $taskEvidence}
$taskFiles=@(Get-ChildItem $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash $_.FullName).Hash}})
[IO.File]::WriteAllText("$taskEvidence/MANIFEST.json",(@{version='0.15.74-dev';jar_sha256=$taskHash;scope='126 successful groups,239 new published exact lines/31 NPCs,independent sources/reviewed targets,572-name bounded registry,unchanged frame/data proofs,4 final ABBA receipt benchmark processes;not all NPCs or live FPS proof';game_mods_or_settings_changed=$false;private_jfr_or_full_game_logs_included=$false;files=$taskFiles}|ConvertTo-Json -Depth 9),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& "$taskProject/tools/package_dev.ps1" -OutputDirectory $taskDest
foreach($taskReport in @($taskNames[3],$taskNames[4])){Copy-Item -LiteralPath "$taskProject/translation-source/$taskReport" -Destination (Join-Path $taskDest $taskReport)}
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){
 $taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0
 try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};if($taskEntry.FullName -match '(?i)\.(?:jfr|log)$|(?:^|/)unknown\.tsv$'){throw 'Private diagnostic artifact found'};$taskStream=$taskEntry.Open();try{$taskEntryHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()};if($taskEntryHash -ne (Get-FileHash -LiteralPath (Join-Path $taskArchive.root $taskEntry.FullName)).Hash){throw "Archive mismatch:$($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()}
 Write-Output "VERIFIED ARCHIVE $($taskArchive.file):$taskCount files"
}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskHash){throw 'Delivered JAR mismatch'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item $taskFile).Length+' bytes SHA256 '+(Get-FileHash $taskFile).Hash)}
Write-Output "DELIVERY74 ROOT:$taskRoot"
