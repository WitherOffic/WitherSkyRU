$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOldArgs=Get-Content "$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c/compile-arguments.txt"
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/hub-benchmark69-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskNew="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskNew)+$taskLibraries)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkHub69.java"
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskModes=@('world_unknown','world_player','world_minion','world_new_roles','npc_unknown','npc_existing','npc_new')
foreach($taskMode in $taskModes){$taskIndex=0;foreach($taskVersion in @('0.15.68-dev','0.15.69-dev','0.15.69-dev','0.15.68-dev')){
 $taskLabel="$taskMode-$taskIndex-$taskVersion";$taskIndex++
 $taskJar=if($taskVersion -eq '0.15.68-dev'){'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar'}else{$taskNew}
 $taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dwitherskyru.benchmark.warmup=20000' '-Dwitherskyru.benchmark.measure=4000' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' $taskCp BenchmarkHub69 "$taskRoot/$taskLabel" $taskVersion "$taskRoot/$taskLabel.json"|Tee-Object -FilePath "$taskRoot/$taskLabel-output.txt"
 if($LASTEXITCODE -ne 0){throw "Benchmark failed $taskLabel"}
}}
[IO.File]::WriteAllText("$taskRoot/SCOPE.json",(@{date='2026-10-15';scope='Each scenario isolated;68,69,69,68 order;28 fresh JVMs;4 timing and allocation rounds each. No live FPS. New positive rows produce additional translated output and are not equal work.';modes=$taskModes;baseline_sha256=(Get-FileHash 'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar').Hash;jar_sha256=(Get-FileHash $taskNew).Hash;all_timing_and_allocation_rounds_retained=$true;installed_pack_modified=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "HUB69 BENCHMARK ROOT: $taskRoot"
