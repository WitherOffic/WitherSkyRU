param([string]$HistoricalProject, [int]$Workers=4)
$ErrorActionPreference='Stop'
if($Workers -lt 1 -or $Workers -gt 4){throw 'Use 1..4 isolated JVMs'}
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java'}
$taskRoot=Join-Path $taskProject ('build/check262-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot | Out-Null
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim()
# Test the packaged mod, not loose source/class output.
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar'
$taskDeps=@($taskCp.Split(';') | Where-Object {$_ -notmatch '[\\/]build[\\/](classes[\\/]java[\\/]main|resources[\\/]main)$'})
$taskCp=(@($taskJar)+$taskDeps)-join ';'
$taskArchive=if($HistoricalProject){(Resolve-Path $HistoricalProject).Path}else{$taskProject}
$taskSources=Join-Path $taskArchive 'translation-source-regression115'
$taskOldScope=Get-Content "$taskProject/tests/fixtures/BASELINE_TEST_SCOPE.json" -Raw | ConvertFrom-Json
$taskTests=@('TestMigration262','TestNpcLiterals115','TestNpcNumbers115','TestEggs114','TestNpc113','TestExpansion112','TestExpansion111','TestGame110','TestSkins109','TestBoss108','TestSystem107')+@($taskOldScope.tests_passed)
if($taskTests.Count -ne 190 -or @($taskTests|Sort-Object -Unique).Count -ne 190){throw 'Unexpected test scope'}
$taskQueue=[Collections.Generic.Queue[string]]::new()
foreach($taskTest in $taskTests){$taskQueue.Enqueue($taskTest)}
$taskRunning=[Collections.Generic.List[object]]::new()
$taskPassed=[Collections.Generic.List[string]]::new()
$taskFailed=[Collections.Generic.List[string]]::new()
Write-Output "CHECK262_ROOT=$taskRoot"
try {
 while($taskQueue.Count -or $taskRunning.Count){
  while($taskQueue.Count -and $taskRunning.Count -lt $Workers){
   $taskTest=$taskQueue.Dequeue(); $taskWork=$taskProject
   $taskArgs=@('-Xmx768m','-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-cp',$taskCp,$taskTest,"$taskRoot/isolated-$taskTest",$taskSources)
   if($taskTest -eq 'TestTurbo65' -or $taskTest -eq 'TestCatalogueCorrections64'){
    $taskAudit=if($taskTest -eq 'TestTurbo65'){'turbo65-cf2b5fc9846142e2acb1a61b61a903fc'}else{'catalogue-fixes64-419a81e71817498daf02854f93d77619'}
    $taskCopy="$taskRoot/audit-$taskTest"; New-Item -ItemType Directory -Path $taskCopy | Out-Null
    Copy-Item -LiteralPath "$taskArchive/build/$taskAudit/audit/items.json" -Destination $taskCopy
    $taskArgs+=$taskCopy
   }
   if($taskTest -eq 'TestHudMixins'){
    $taskWork="$taskRoot/isolated-$taskTest"; New-Item -ItemType Directory -Path "$taskWork/mods" | Out-Null
    Copy-Item -LiteralPath $taskJar -Destination "$taskWork/mods"
    foreach($taskDependency in $taskDeps | Where-Object {$_ -match 'fabric-(resource-loader-v1|api-base)-[^/\\]+\.jar$'}){
     Copy-Item -LiteralPath $taskDependency -Destination "$taskWork/mods"
    }
    $taskKnotCp=($taskDeps -join ';')
    $taskArgs=@('-Xmx768m','-Djava.awt.headless=true','-Dmixin.debug.export=true','-Dmixin.debug.countInjections=true','-cp',$taskKnotCp,$taskTest,$taskWork)
   }
   $taskQuoted=($taskArgs | ForEach-Object {'"'+$_.Replace('\','/')+'"'})-join ' '
   $taskProcess=Start-Process -FilePath $taskJava -ArgumentList $taskQuoted -WorkingDirectory $taskWork -WindowStyle Hidden -RedirectStandardOutput "$taskRoot/$taskTest.out.txt" -RedirectStandardError "$taskRoot/$taskTest.err.txt" -PassThru
   $taskRunning.Add(@{name=$taskTest;process=$taskProcess})
  }
  foreach($taskJob in @($taskRunning.ToArray())){
   $taskJob.process.Refresh(); if(-not $taskJob.process.HasExited){continue}
   $taskJob.process.WaitForExit()
   if($taskJob.process.ExitCode -eq 0){$taskPassed.Add($taskJob.name);Write-Output "PASS $($taskJob.name)"}
   else{$taskFailed.Add($taskJob.name);Write-Output "FAIL $($taskJob.name)";Get-Content "$taskRoot/$($taskJob.name).err.txt" -Tail 9}
   $taskJob.process.Dispose();[void]$taskRunning.Remove($taskJob)
  }
  if($taskRunning.Count){Start-Sleep -Milliseconds 100}
 }
} finally {
 foreach($taskJob in $taskRunning.ToArray()){if(-not $taskJob.process.HasExited){$taskJob.process.Kill()};$taskJob.process.Dispose()}
}
@{jar_sha256=(Get-FileHash $taskJar).Hash;passed=@($taskPassed.ToArray());failed=@($taskFailed.ToArray());live_game_test=$false;workers=$Workers} | ConvertTo-Json -Depth 4 | Set-Content "$taskRoot/RESULT.json" -Encoding utf8
Write-Output "CHECK262: $($taskPassed.Count) passed; $($taskFailed.Count) failed; $taskRoot"
if($taskFailed.Count){throw 'Some checks failed; see RESULT.json and retained logs'}
