param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskPinned=Join-Path $taskProject 'build/item-audit64-a4de0b74b10243fba6713d30f70978f7'
$taskChecks=Join-Path $taskProject 'build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc'
$taskBaseline=Join-Path $taskProject 'build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit'
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar'
$taskQA=Get-Content -LiteralPath (Join-Path $taskProject 'QA_REPORT.json') -Raw|ConvertFrom-Json
$taskScope=Get-Content -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Raw|ConvertFrom-Json
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskDigest -ne '721732CC20BC69EE2337F1A557DBDEFE6B183602D8932D6EDA0AB43244183113' -or $taskDigest -ne $taskScope.runtime_jar_sha256 -or $taskDigest -ne $taskQA.latest_verified_batch.jar_sha256){throw 'Different unverified JAR'}
if($taskScope.tests_passed.Count -ne 116){throw 'Incomplete regression scope'}
foreach($taskTest in $taskScope.tests_passed){$taskFile=Join-Path $taskChecks "$taskTest-output.txt";if(!(Test-Path -LiteralPath $taskFile) -or (Get-Item -LiteralPath $taskFile).Length -eq 0){throw "Missing check: $taskTest"}}
foreach($taskGuard in @(@('TestTurbo65',242937),@('TestUniformLore65',311738))){if((Get-Content -LiteralPath (Join-Path $taskChecks ($taskGuard[0]+'-output.txt')) -Raw) -notmatch ($taskGuard[1].ToString()+' assertions')){throw 'Missing independent guard'}}
if((Get-FileHash -LiteralPath (Join-Path $taskPinned 'catalogue.json')).Hash -ne '38FBA44F6D2843C3E31B4A682660553CA51C35D40B663DB1F4B4D725B7AD69A6'){throw 'Different public source'}
$taskDest=(Resolve-Path -LiteralPath $OutputDirectory).Path
$taskNames=@('WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar','WitherSkyRU_0.15.65-dev_MC26.1.2_project.zip','WitherSkyRU_0.15.65-dev_item_checks.zip','TURBO_0.15.65_RU.md')
foreach($taskName in $taskNames){if(Test-Path -LiteralPath (Join-Path $taskDest $taskName)){throw "Preserve existing artifact: $taskName"}}
$taskRoot=Join-Path $taskProject ('build/delivery65-'+[Guid]::NewGuid().ToString('N'))
$taskEvidence=Join-Path $taskRoot 'item-checks'
foreach($taskName in @('source','baseline64','candidate65','checks','prototype')){New-Item -ItemType Directory -Path (Join-Path $taskEvidence $taskName)|Out-Null}
foreach($taskName in @('catalogue.json','official-items.json','manifest.json')){Copy-Item -LiteralPath (Join-Path $taskPinned $taskName) -Destination (Join-Path $taskEvidence 'source')}
Copy-Item -LiteralPath (Join-Path $taskProject 'translation-source/TURBO_PRIMARY_0.15.65.json') -Destination (Join-Path $taskEvidence 'source')
foreach($taskName in @('items.json','summary.json')){Copy-Item -LiteralPath (Join-Path $taskBaseline $taskName) -Destination (Join-Path $taskEvidence 'baseline64')}
foreach($taskName in @('items.json','summary.json','reviewed-changed-ids65.json','official-names.json')){Copy-Item -LiteralPath (Join-Path $taskChecks "audit/$taskName") -Destination (Join-Path $taskEvidence 'candidate65')}
Copy-Item -LiteralPath (Join-Path $taskChecks 'CHECK_SCOPE.json') -Destination (Join-Path $taskEvidence 'checks')
foreach($taskTest in $taskScope.tests_passed){Copy-Item -LiteralPath (Join-Path $taskChecks "$taskTest-output.txt") -Destination (Join-Path $taskEvidence 'checks')}
Copy-Item -LiteralPath (Join-Path $taskProject 'build/prototypes/WitherSkyRU-0.15.65-pre-uniform-fastpath-65A1A374.jar') -Destination (Join-Path $taskEvidence 'prototype')
Copy-Item -LiteralPath (Join-Path $taskPinned 'export-50fec8785bbe4c6d81f4a6b015514ad1/SOURCE_LICENSE.txt') -Destination $taskEvidence
foreach($taskName in @('TURBO_0.15.65_RU.md','PERFORMANCE_0.15.65_RAW.json','PERFORMANCE_0.15.65_NEGATIVE_RAW.json')){Copy-Item -LiteralPath (Join-Path $taskProject "translation-source/$taskName") -Destination $taskEvidence}
$taskFiles=@(Get-ChildItem -LiteralPath $taskEvidence -Recurse -File|ForEach-Object {@{path=$_.FullName.Substring($taskEvidence.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
$taskManifest=@{version='0.15.65-dev';jar_sha256=$taskDigest;source_commit='a225a02217776299ab3fb5cf5d9c0b155c4fc445';public_cards=8937;reviewed_output_changes=93;all_items_fixed=$false;full_manual_proofread=$false;game_fps_measured=$false;reproduction='Pinned inputs and per-ID output included. Restore source catalogue and baseline64 to the exact build roots in check_turbo65.ps1; restore prototype oracle to build/prototypes. The prototype is UNRELEASED and must not be installed. Delivered64 remains the separate baseline JAR. Tests retain historical fixture roots documented in CHECK_SCOPE; not a standalone game installation.';files=$taskFiles}
[IO.File]::WriteAllText((Join-Path $taskEvidence 'MANIFEST.json'),($taskManifest|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Compress-Archive -LiteralPath @(Get-ChildItem -LiteralPath $taskEvidence|ForEach-Object FullName) -DestinationPath (Join-Path $taskDest $taskNames[2]) -CompressionLevel Optimal
& (Join-Path $PSScriptRoot 'package_dev.ps1') -OutputDirectory $taskDest
Copy-Item -LiteralPath (Join-Path $taskProject 'translation-source/TURBO_0.15.65_RU.md') -Destination (Join-Path $taskDest $taskNames[3])
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach($taskArchive in @(@{file=$taskNames[1];root=$taskProject},@{file=$taskNames[2];root=$taskEvidence})){
    $taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskDest $taskArchive.file));$taskCount=0
    try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskLocal=Join-Path $taskArchive.root $taskEntry.FullName;$taskStream=$taskEntry.Open();try{$taskHash=[Security.Cryptography.SHA256]::Create();try{$taskActual=[BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskHash.Dispose()}}finally{$taskStream.Dispose()};if($taskActual -ne (Get-FileHash -LiteralPath $taskLocal).Hash){throw "Archive mismatch: $($taskEntry.FullName)"};$taskCount++}}finally{$taskZip.Dispose()}
    Write-Output "VERIFIED ARCHIVE $($taskArchive.file): $taskCount files"
}
if((Get-FileHash -LiteralPath (Join-Path $taskDest $taskNames[0])).Hash -ne $taskDigest){throw 'Copied JAR differs'}
foreach($taskName in $taskNames){$taskFile=Join-Path $taskDest $taskName;Write-Output ($taskName+' '+(Get-Item -LiteralPath $taskFile).Length+' bytes SHA256 '+(Get-FileHash -LiteralPath $taskFile).Hash)}
Write-Output "DELIVERY EVIDENCE ROOT: $taskRoot"
