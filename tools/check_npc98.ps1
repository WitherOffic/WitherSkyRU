param([int]$Workers=4)
$ErrorActionPreference='Stop'
if($Workers -lt 1 -or $Workers -gt 4){throw 'Use1..4 isolated JVMs'}
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.98-dev.jar"
$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
$taskBaseline="$taskProject/build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55"
$taskOldArgs=Get-Content "$taskBaseline/compile-arguments.txt"
$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/npc98-checks-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibs)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt"
[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';'
$taskOldScope=Get-Content "$taskBaseline/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskTests=@('TestNpc98')+@($taskOldScope.tests_passed)
if($taskTests.Count -ne 165 -or @($taskTests|Sort-Object -Unique).Count -ne 165){throw 'Wrong fresh group list'}
$taskWaiting=[Collections.Generic.Queue[string]]::new()
foreach($taskTest in $taskTests){$taskWaiting.Enqueue($taskTest)}
$taskRunning=[Collections.Generic.List[object]]::new()
$taskPassed=[Collections.Generic.List[string]]::new()
Write-Output "NPC98 PARALLEL ROOT:$taskRoot"
try{
 while($taskWaiting.Count -gt 0 -or $taskRunning.Count -gt 0){
  while($taskWaiting.Count -gt 0 -and $taskRunning.Count -lt $Workers){
   $taskTest=$taskWaiting.Dequeue();$taskWorking=$taskProject;$taskTestArgs=@("$taskRoot/isolated-$taskTest","$taskProject/translation-source")
   if($taskTest -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"}
   if($taskTest -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
   $taskJavaArgs=@('-Xmx768m','-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-cp',$taskCp,$taskTest)+$taskTestArgs
   if($taskTest -eq 'TestHudMixins'){
    $taskWorking="$taskRoot/isolated-$taskTest";New-Item -ItemType Directory -Path "$taskWorking/mods"|Out-Null
    Copy-Item -LiteralPath $taskJar -Destination "$taskWorking/mods"
    Copy-Item -LiteralPath 'C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/fabric-api-0.155.3+26.1.2.jar' -Destination "$taskWorking/mods"
    $taskJavaArgs=@('-Xmx768m','-Djava.awt.headless=true','-Dmixin.debug.export=true','-Dmixin.debug.countInjections=true','-cp',((@($taskRoot)+$taskLibs)-join ';'),$taskTest,$taskWorking)
   }
   $taskQuoted=($taskJavaArgs|ForEach-Object {'"'+$_.Replace('\','/')+'"'})-join ' '
   $taskOut="$taskRoot/$taskTest-output.txt";$taskErr="$taskRoot/$taskTest-stderr.txt"
   $taskProcess=Start-Process -FilePath "$taskJava/java.exe" -ArgumentList $taskQuoted -WorkingDirectory $taskWorking -WindowStyle Hidden -RedirectStandardOutput $taskOut -RedirectStandardError $taskErr -PassThru
   $taskRunning.Add(@{name=$taskTest;process=$taskProcess;output=$taskOut;stderr=$taskErr})
  }
  foreach($taskJob in @($taskRunning.ToArray())){
   $taskJob.process.Refresh()
   if(-not $taskJob.process.HasExited){continue}
   $taskJob.process.WaitForExit();$taskExit=$taskJob.process.ExitCode
   $taskErrorText=[IO.File]::ReadAllText($taskJob.stderr)
   if($taskErrorText){[IO.File]::AppendAllText($taskJob.output,"`n"+$taskErrorText,[Text.UTF8Encoding]::new($false))}
   if($taskExit -ne 0){Get-Content $taskJob.output -Tail 25;throw "Failed $($taskJob.name);$taskRoot"}
   $taskPassed.Add($taskJob.name);Write-Output "PASS $($taskJob.name) ($($taskPassed.Count)/165)"
   $taskJob.process.Dispose();[void]$taskRunning.Remove($taskJob)
  }
  if($taskRunning.Count -gt 0){Start-Sleep -Milliseconds 100}
 }
}catch{
 foreach($taskJob in $taskRunning.ToArray()){if(-not $taskJob.process.HasExited){$taskJob.process.Kill()};$taskJob.process.Dispose()}
 throw
}
& 'C:/Users/Wither/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 -m unittest discover -s "$taskProject/tests" -p 'test_npc_corpus77.py' -v > "$taskRoot/TestNpcCorpusParser98-output.txt" 2>&1
if($LASTEXITCODE -ne 0){throw 'Parser tests failed'}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskHash){throw 'Executable changed'}
if($taskPassed.Count -ne 165){throw 'Not all fresh checks passed'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.98-dev';runtime_jar_sha256=$taskHash;tests_passed=@($taskPassed.ToArray());targeted_only=$false;reused_successes=@();isolated_parallel_workers=$Workers;installed_pack_modified=$false;live_fps_or_frame_times_measured=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "NPC98 PASSED:$($taskPassed.Count);$taskRoot"
