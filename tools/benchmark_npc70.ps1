$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskProject/build/hub69-2689a861e4fe4abcb55395f2b7cc737d/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar"
$taskRoot=Join-Path $taskProject ('build/npc-benchmark70-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskJar)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkNpc70.java";if($LASTEXITCODE -ne 0){throw 'Benchmark compile failed'}
$taskModes=@('unknown','existing','named','duration','contest');$taskResults=@();$taskRaw=@()
foreach($taskMode in $taskModes){$taskIndex=0;$taskRuns=@();foreach($taskVersion in @('0.15.69-dev','0.15.70-dev','0.15.70-dev','0.15.69-dev')){
 $taskLabel="$taskMode-$taskIndex-$taskVersion";$taskIndex++
 $taskRuntimeJar=if($taskVersion -eq '0.15.69-dev'){'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar'}else{$taskJar};$taskCp=(@($taskRoot,$taskRuntimeJar)+$taskLibs)-join ';'
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' $taskCp BenchmarkNpc70 "$taskRoot/$taskLabel" $taskVersion "$taskRoot/$taskLabel.json"|Tee-Object -FilePath "$taskRoot/$taskLabel-output.txt";if($LASTEXITCODE -ne 0){throw "Failed $taskLabel"};$taskRuns+=,(Get-Content "$taskRoot/$taskLabel.json" -Raw|ConvertFrom-Json)
 }
 $taskRaw+=@{mode=$taskMode;runs=$taskRuns};$taskResults+=@{mode=$taskMode;equal_useful_output=($taskMode -in @('unknown','existing'));cpu_percent=@((100*($taskRuns[1].$taskMode.ms_per10/$taskRuns[0].$taskMode.ms_per10-1)),(100*($taskRuns[2].$taskMode.ms_per10/$taskRuns[3].$taskMode.ms_per10-1)));before_microseconds_per_input=@(($taskRuns[0].$taskMode.ms_per10*100),($taskRuns[3].$taskMode.ms_per10*100));after_microseconds_per_input=@(($taskRuns[1].$taskMode.ms_per10*100),($taskRuns[2].$taskMode.ms_per10*100))}
}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.70_RAW.json",(@{date='2026-10-15';root=$taskRoot;jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;scope='20 fresh JVMs,69/70/70/69 per case;20000 warmup/4x4000 measured batches of10;all timing/allocation rounds retained;receipt cost not FPS;new positives do more useful work';results=$taskResults;raw=$taskRaw;actual_game_fps_measured=$false;zero_fps_impact_proven=$false}|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "NPC70 BENCHMARK ROOT: $taskRoot"
