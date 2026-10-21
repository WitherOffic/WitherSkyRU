param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskQA=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($taskQA.latest_verified_batch.version -ne '0.15.68-dev' -or $taskQA.latest_verified_batch.packaged_groups_passed -ne 119){throw 'Different verified version'}
$taskChecks=Join-Path $taskProject $taskQA.latest_verified_batch.packaged_gate_root
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar";$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskDigest -ne '81A0ECE1E69C69903F29616BA6B526586D1F8E84E0ECCA9C0DA2999B7E2CA4EA' -or $taskDigest -ne $taskScope.runtime_jar_sha256 -or $taskDigest -ne $taskQA.latest_verified_batch.jar_sha256 -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 119){throw 'Unverified JAR'}
if((Get-Content "$taskChecks/TestDungeons68-output.txt" -Raw) -notmatch '26036 assertions'){throw 'Incomplete independent tests'}
$taskDest=(Resolve-Path -LiteralPath $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar','WitherSkyRU_0.15.68-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.68-dev_dungeons_checks.zip','DUNGEONS_0.15.68_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve $taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery68-'+[Guid]::NewGuid().ToString('N'));$taskEvidence="$taskRoot/dungeon-checks";New-Item -ItemType Directory -Path "$taskEvidence/checks"|Out-Null
Copy-Item -LiteralPath "$taskChecks/CHECK_SCOPE.json" -Destination "$taskEvidence/checks"
foreach($taskTest in $taskScope.tests_passed){$taskFile="$taskChecks/$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item -LiteralPath $taskFile).Length -eq 0){throw "Missing $taskTest"};Copy-Item -LiteralPath $taskFile -Destination "$taskEvidence/checks"}
foreach($taskFile in @('DUNGEONS_0.15.68_RU.md','DUNGEONS_PRIMARY_0.15.68.json','DUNGEON_ORB_OBSERVED_0.15.68.json','DUNGEON_ARMOR_0.15.68.json','DUNGEONS_AUDIT_0.15.68.json','PERFORMANCE_0.15.68_RAW.json','PERFORMANCE_0.15.68_RECHECK.json','JAR_SCOPE_0.15.68.json')){Copy-Item -LiteralPath "$taskProject/translation-source/$taskFile" -Destination $taskEvidence}
Copy-Item -LiteralPath "$taskChecks/audit/summary.json" -Destination "$taskEvidence/CATALOGUE_SUMMARY.json"
$taskFiles=@(Get-ChildItem -LiteralPath $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
[IO.File]::WriteAllText("$taskEvidence/MANIFEST.json",(@{version='0.15.68-dev';jar_sha256=$taskDigest;installed_pack_modified=$false;all_dungeons_translated=$false;raw_jfr_or_full_private_logs_included=$false;reproduction='Project scripts require pinned fixture/JAR roots listed by check_dungeons68.ps1; all119 test outputs retained.19 full cards and8 armor paragraphs independently checked. Full catalogue differential compares delivered67 to68; source/card hashes retained. No live FPS claim.';files=$taskFiles}|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem -LiteralPath $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& "$taskProject/tools/package_dev.ps1" -OutputDirectory $taskDest
Copy-Item -LiteralPath "$taskProject/translation-source/DUNGEONS_0.15.68_RU.md" -Destination (Join-Path $taskDest $taskNames[3])
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){$taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0;try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskStream=$taskEntry.Open();$taskHasher=[Security.Cryptography.SHA256]::Create();try{$taskHash=[BitConverter]::ToString($taskHasher.ComputeHash($taskStream)).Replace('-','')}finally{$taskHasher.Dispose();$taskStream.Dispose()};if($taskHash -ne (Get-FileHash -LiteralPath (Join-Path $taskArchive.root $taskEntry.FullName)).Hash){throw "Archive mismatch $($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()};Write-Output "VERIFIED ARCHIVE $($taskArchive.file): $taskCount files"}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskDigest){throw 'Copied JAR differs'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item -LiteralPath $taskFile).Length+' bytes SHA256 '+(Get-FileHash -LiteralPath $taskFile).Hash)}
Write-Output "DELIVERY68 ROOT: $taskRoot"
