param([switch]$TargetedOnly,[string]$ResumeRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar";$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
$taskOldRoot="$taskProject/build/prepared72-checks-32b3a062404840a8993bba091c06f03e"
$taskOldArgs=Get-Content "$taskOldRoot/compile-arguments.txt";$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/names73-checks-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibs)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt";[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs";if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';';Write-Output "NAMES73 ROOT:$taskRoot"
$taskPriority=@('ru.wither.skyru.TestNames73','ru.wither.skyru.TestPreparedDisplay72','TestFrameNoops71','TestPerformanceCaches','TestHudMixins','TestPlayerTextProtection','TestHudLayout','TestRenderContext67','TestNpc70')
$taskOldScope=Get-Content "$taskOldRoot/CHECK_SCOPE.json" -Raw|ConvertFrom-Json;$taskTests=$taskPriority+@($taskOldScope.tests_passed|Where-Object {$_ -notin $taskPriority});if($TargetedOnly){$taskTests=$taskPriority}
$taskReused=@();if($ResumeRoot){$taskPrevious=Get-Content "$ResumeRoot/CHECK_SCOPE.json" -Raw|ConvertFrom-Json;if($taskPrevious.runtime_jar_sha256 -ne $taskHash -or $taskPrevious.runtime -ne '0.15.73-dev'){throw 'Different executable reuse'};foreach($taskTest in $taskPrevious.tests_passed){if($taskTest -notin $taskTests){throw 'Unknown check'};Copy-Item -LiteralPath "$ResumeRoot/$taskTest-output.txt" -Destination "$taskRoot/$taskTest-output.txt";$taskReused+=$taskTest}}
foreach($taskTest in $taskTests){if($taskTest -in $taskReused){continue};$taskTestArgs=@("$taskRoot/isolated-$taskTest","$taskProject/translation-source");if($taskTest -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"};if($taskTest -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
 if($taskTest -eq 'TestHudMixins'){
   $taskTransform="$taskRoot/isolated-$taskTest";New-Item -ItemType Directory -Path "$taskTransform/mods" -Force|Out-Null;Copy-Item -LiteralPath $taskJar -Destination "$taskTransform/mods"
   Copy-Item -LiteralPath 'C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/fabric-api-0.155.3+26.1.2.jar' -Destination "$taskTransform/mods"
   Push-Location $taskTransform
   try{& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dmixin.debug.export=true' '-Dmixin.debug.countInjections=true' '-cp' ((@($taskRoot)+$taskLibs)-join ';') TestHudMixins $taskTransform > "$taskRoot/$taskTest-output.txt" 2>&1;$taskTransformExit=$LASTEXITCODE}finally{Pop-Location}
   if($taskTransformExit -ne 0){Get-Content "$taskRoot/$taskTest-output.txt" -Tail 20;throw "Failed mixin transformation;$taskRoot"};Write-Output "PASS $taskTest";continue
 }
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskCp $taskTest @taskTestArgs > "$taskRoot/$taskTest-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskTest-output.txt" -Tail 20;throw "Failed $taskTest;$taskRoot"};Write-Output "PASS $taskTest"
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskHash){throw 'Executable changed'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.73-dev';runtime_jar_sha256=$taskHash;tests_passed=$taskTests;targeted_only=$TargetedOnly.IsPresent;reused_same_executable_successes=$taskReused;reused_from=$ResumeRoot;installed_pack_modified=$false;new_version_live_fps_or_frame_times_measured=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "NAMES73 PASSED:$($taskTests.Count);$taskRoot"
