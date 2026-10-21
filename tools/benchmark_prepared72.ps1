param([Parameter(Mandatory=$true)][string]$CheckRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskCheck=(Resolve-Path -LiteralPath $CheckRoot).Path;$taskOldArgs=Get-Content "$taskCheck/compile-arguments.txt";$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/prepared72-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCurrent="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskCurrent)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkPrepared72.java";if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskIndex=0;$taskRecords=@();$taskHashes=@{}
foreach($taskVersion in @('0.15.71-dev','0.15.72-dev','0.15.72-dev','0.15.71-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskHashes[$taskVersion]=(Get-FileHash -LiteralPath $taskJar).Hash;$taskFile="$taskRoot/$taskIndex-$taskVersion.json";$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';'
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp ru.wither.skyru.BenchmarkPrepared72 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'Benchmark failed'}
 $taskRecords+=@{index=$taskIndex;version=$taskVersion;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
if($taskHashes['0.15.71-dev'] -ne '0BAA651B0D612D95AC0D3F486278094C0B16497CD6618914791C8BA21316CA46'){throw 'Wrong baseline'}
$taskSummary=@();foreach($taskMode in @('stable_hud','stable_outside_fresh_context','dynamic_counter','fresh_colors_actions','fragmented16','over_limit_uncached','stable_hud_consume','dynamic_counter_consume','fresh_colors_actions_consume','fragmented16_consume')){
 $taskOrders=@();foreach($taskPair in @(@(0,1),@(3,2))){$taskBefore=$taskRecords[$taskPair[0]].results.$taskMode;$taskAfter=$taskRecords[$taskPair[1]].results.$taskMode;$taskThreadChange=$null;if($taskBefore.thread_cpu_ns_per_operation -gt 0){$taskThreadChange=100*($taskAfter.thread_cpu_ns_per_operation/$taskBefore.thread_cpu_ns_per_operation-1)};$taskOrders+=@{before_ns=$taskBefore.ns_per_operation;after_ns=$taskAfter.ns_per_operation;cpu_change_percent=100*($taskAfter.ns_per_operation/$taskBefore.ns_per_operation-1);legacy_cpu_change_percent_is_wall_time=$true;thread_cpu_change_percent=$taskThreadChange;before_thread_cpu_ns=$taskBefore.thread_cpu_ns_per_operation;after_thread_cpu_ns=$taskAfter.thread_cpu_ns_per_operation;before_bytes=$taskBefore.bytes_per_operation;after_bytes=$taskAfter.bytes_per_operation}}
 $taskSummary+=@{mode=$taskMode;orders=$taskOrders}
}
$taskRaw=@{scope='Four isolated JVMs in71,72,72,71 order; all rounds retained. Pure glyph plans with current styles. Microbenchmark, not game FPS/0.1%low/GPU. No live A/B.';runtime_hashes=$taskHashes;records=$taskRecords;summary=$taskSummary}
[IO.File]::WriteAllText("$taskRoot/RAW.json",($taskRaw|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false));$taskSummary|ConvertTo-Json -Depth 5;Write-Output "PREPARED72 BENCHMARK ROOT:$taskRoot"
