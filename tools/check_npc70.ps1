param([switch]$TargetedOnly,[string]$ResumeRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar";$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
$taskOldArgs=Get-Content "$taskProject/build/hub69-2689a861e4fe4abcb55395f2b7cc737d/compile-arguments.txt"
$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/npc70-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibs)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt";[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs";if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';';Write-Output "NPC70 ROOT: $taskRoot"
if(!$TargetedOnly){& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp AuditItemCatalogue64 "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7" "$taskRoot/audit"|Tee-Object -FilePath "$taskRoot/audit-output.txt";if($LASTEXITCODE -ne 0){throw 'Catalogue audit failed'}}
$taskOldScope=Get-Content "$taskProject/build/hub69-2689a861e4fe4abcb55395f2b7cc737d/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskPriority=@('TestNpc70','TestGreenhouseTutorial','TestNpcDynamicInstructions','TestNpcLookup61','TestHubNpcs69','TestStoryDialogues','TestPerformanceCaches','TestPlayerTextProtection','TestNpcCounters')
$taskTests=$taskPriority+@($taskOldScope.tests_passed|Where-Object {$_ -notin $taskPriority});if($TargetedOnly){$taskTests=$taskPriority}
$taskReused=@();if($ResumeRoot){$taskPrevious=Get-Content "$ResumeRoot/CHECK_SCOPE.json" -Raw|ConvertFrom-Json;if($taskPrevious.runtime_jar_sha256 -ne $taskHash -or $taskPrevious.runtime -ne '0.15.70-dev'){throw 'Different executable reuse'};foreach($taskTest in $taskPrevious.tests_passed){if($taskTest -notin $taskTests){throw 'Unknown check'};Copy-Item -LiteralPath "$ResumeRoot/$taskTest-output.txt" -Destination "$taskRoot/$taskTest-output.txt";$taskReused+=$taskTest}}
foreach($taskTest in $taskTests){if($taskTest -in $taskReused){continue};$taskTestArgs=@("$taskRoot/isolated-$taskTest","$taskProject/translation-source");if($taskTest -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"};if($taskTest -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskCp $taskTest @taskTestArgs|Tee-Object -FilePath "$taskRoot/$taskTest-output.txt";if($LASTEXITCODE -ne 0){throw "Failed $taskTest;$taskRoot"}
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskHash){throw 'Executable changed'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.70-dev';runtime_jar_sha256=$taskHash;tests_passed=$taskTests;targeted_only=$TargetedOnly.IsPresent;reused_same_executable_successes=$taskReused;reused_from=$ResumeRoot;installed_pack_modified=$false;game_started=$false;all_hypixel_npcs_translated=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "NPC70 PASSED:$($taskTests.Count);$taskRoot"
