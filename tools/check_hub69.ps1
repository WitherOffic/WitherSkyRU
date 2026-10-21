param([switch]$TargetedOnly,[switch]$ScanOnly,[string]$ResumeRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar"
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
$taskOldArgs=Get-Content "$taskProject/build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt"
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskRoot=Join-Path $taskProject ('build/hub69-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibraries)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt";[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs";if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskRuntime=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
Write-Output "HUB69 CHECK ROOT: $taskRoot"
if(!$TargetedOnly){& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskRuntime AuditItemCatalogue64 "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7" "$taskRoot/audit"|Tee-Object -FilePath "$taskRoot/audit-output.txt";if($LASTEXITCODE -ne 0){throw 'Catalogue scan failed'}}
if($ScanOnly){return}
$taskPrevious=Get-Content "$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskPriority=@('TestHubNpcs69','TestStoryDialogues','TestNpcLookup61','TestMinionLabels','TestSafetyQuiz66','TestPerformanceCaches','TestRenderContext67','TestPlayerTextProtection','TestDungeons68')
$taskTests=$taskPriority+@($taskPrevious.tests_passed|Where-Object {$_ -notin $taskPriority})
if($TargetedOnly){$taskTests=$taskPriority}
$taskReused=@()
if($ResumeRoot){
 $taskPrior=Get-Content "$ResumeRoot/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
 if($taskPrior.runtime_jar_sha256 -ne $taskDigest -or $taskPrior.runtime -ne '0.15.69-dev'){throw 'Cannot reuse results from a different executable'}
 foreach($taskClass in $taskPrior.tests_passed){
  if($taskClass -notin $taskTests){throw 'Unrecognized prior check'}
  $taskOutput="$ResumeRoot/$taskClass-output.txt"
  if(!(Test-Path -LiteralPath $taskOutput) -or (Get-Item -LiteralPath $taskOutput).Length -eq 0){throw 'Incomplete prior successful result'}
  Copy-Item -LiteralPath $taskOutput -Destination "$taskRoot/$taskClass-output.txt";$taskReused+=$taskClass
 }
}
foreach($taskClass in $taskTests){$taskTestArgs=@("$taskRoot/isolated-$taskClass","$taskProject/translation-source");if($taskClass -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"};if($taskClass -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
 if($taskClass -in $taskReused){continue}
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskRuntime $taskClass @taskTestArgs|Tee-Object -FilePath "$taskRoot/$taskClass-output.txt"
 if($LASTEXITCODE -ne 0){throw "Failed $taskClass; $taskRoot"}
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskDigest){throw 'JAR changed during tests'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.69-dev';runtime_jar_sha256=$taskDigest;tests_passed=$taskTests;targeted_only=$TargetedOnly.IsPresent;full_catalogue_scanned=(!$TargetedOnly);reused_same_executable_successes=$taskReused;reused_from=$ResumeRoot;installed_pack_modified=$false;game_started=$false;all_hypixel_npcs_translated=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "HUB69 PASSED: $($taskTests.Count); $taskRoot"
