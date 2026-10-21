param([switch]$TargetedOnly)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.77-dev.jar"
$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
$taskBaseline="$taskProject/build/npc76-checks-2db94ab26b25440e93fec9c6460d1af5"
$taskOldArgs=Get-Content "$taskBaseline/compile-arguments.txt"
$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/npc77-checks-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibs)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt"
[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';'
Write-Output "NPC77 ROOT:$taskRoot"
$taskScope=Get-Content "$taskBaseline/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskTests=@('TestNpc77','TestLoreBoundary77','TestEnchantmentExpansion','TestReviewedLoreIndex')+@($taskScope.tests_passed|Where-Object {$_ -notin @('TestEnchantmentExpansion','TestReviewedLoreIndex')})
if($TargetedOnly){$taskTests=$taskTests[0..14]}
if(@($taskTests|Sort-Object -Unique).Count -ne $taskTests.Count){throw 'Duplicate checks'}
$taskPassed=@()
foreach($taskTest in $taskTests){
 $taskTestArgs=@("$taskRoot/isolated-$taskTest","$taskProject/translation-source")
 if($taskTest -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"}
 if($taskTest -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
 if($taskTest -eq 'TestHudMixins'){
   $taskTransform="$taskRoot/isolated-$taskTest"
   New-Item -ItemType Directory -Path "$taskTransform/mods"|Out-Null
   Copy-Item -LiteralPath $taskJar -Destination "$taskTransform/mods"
   Copy-Item -LiteralPath 'C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/fabric-api-0.155.3+26.1.2.jar' -Destination "$taskTransform/mods"
   Push-Location $taskTransform
   try{& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dmixin.debug.export=true' '-Dmixin.debug.countInjections=true' '-cp' ((@($taskRoot)+$taskLibs)-join ';') TestHudMixins $taskTransform > "$taskRoot/$taskTest-output.txt" 2>&1;$taskExit=$LASTEXITCODE}finally{Pop-Location}
 }else{
   & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskCp $taskTest @taskTestArgs > "$taskRoot/$taskTest-output.txt" 2>&1
   $taskExit=$LASTEXITCODE
 }
 if($taskExit -ne 0){Get-Content "$taskRoot/$taskTest-output.txt" -Tail 20;throw "Failed $taskTest;$taskRoot"}
 $taskPassed+=$taskTest;Write-Output "PASS $taskTest"
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskHash){throw 'Executable changed'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.77-dev';runtime_jar_sha256=$taskHash;tests_passed=$taskPassed;targeted_only=$TargetedOnly.IsPresent;reused_successes=@();installed_pack_modified=$false;live_fps_or_frame_times_measured=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "NPC77 PASSED:$($taskPassed.Count);$taskRoot"
