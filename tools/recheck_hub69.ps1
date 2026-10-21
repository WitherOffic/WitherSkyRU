param([Parameter(Mandatory=$true)][string]$BenchmarkRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOldArgs=Get-Content "$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c/compile-arguments.txt"
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/hub-recheck69-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskModes=@('world_unknown','world_player','world_minion');$taskRaw=@();$taskResults=@()
foreach($taskMode in $taskModes){$taskIndex=0;$taskRuns=@();foreach($taskVersion in @('0.15.68-dev','0.15.69-dev','0.15.69-dev','0.15.68-dev')){
 $taskLabel="$taskMode-$taskIndex-$taskVersion";$taskIndex++
 $taskJar=if($taskVersion -eq '0.15.68-dev'){'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar'}else{"$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar"}
 $taskCp=(@($BenchmarkRoot,$taskJar)+$taskLibraries)-join ';'
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dwitherskyru.benchmark.warmup=100000' '-Dwitherskyru.benchmark.measure=20000' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' $taskCp BenchmarkHub69 "$taskRoot/$taskLabel" $taskVersion "$taskRoot/$taskLabel.json"|Tee-Object -FilePath "$taskRoot/$taskLabel-output.txt"
 if($LASTEXITCODE -ne 0){throw 'World microbenchmark recheck failed'}
 $taskRuns+=,(Get-Content "$taskRoot/$taskLabel.json" -Raw|ConvertFrom-Json)
 }
 $taskRaw+=@{mode=$taskMode;runs=$taskRuns}
 $taskResults+=@{mode=$taskMode;cpu_percent=@((100*($taskRuns[1].$taskMode.ms_per10/$taskRuns[0].$taskMode.ms_per10-1)),(100*($taskRuns[2].$taskMode.ms_per10/$taskRuns[3].$taskMode.ms_per10-1)));before_microseconds_per_input=@(($taskRuns[0].$taskMode.ms_per10*100),($taskRuns[3].$taskMode.ms_per10*100));after_microseconds_per_input=@(($taskRuns[1].$taskMode.ms_per10*100),($taskRuns[2].$taskMode.ms_per10*100))}
}
$taskData=@{date='2026-10-15';scope='Repeat all three equal-output world scenarios separately,with 100000 warmup/4x20000 measured batches;68,69,69,68 each. Does not overwrite initial28 process results. All timing and allocation rounds retained;not FPS.';root=$taskRoot;jar_sha256=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar").Hash;results=$taskResults;raw=$taskRaw}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.69_RECHECK.json",($taskData|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "HUB69 RECHECK ROOT: $taskRoot"
