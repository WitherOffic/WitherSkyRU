$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc75-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.75-dev.jar";if((Get-FileHash $taskBefore).Hash -ne 'F07822E5F0D3A8753D8E770C196B5EED471828A33B10702BBBB404211147EB18'){throw 'Wrong74 baseline'}
$taskArgs=Get-Content "$taskProject/build/names73-checks-1cc05db78e3b453da3efb2d5c9b40038/compile-arguments.txt";$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskAfter)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkNpc75.java";if($LASTEXITCODE -ne 0){throw 'Compile benchmark failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.74-dev','0.15.75-dev','0.15.75-dev','0.15.74-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkNpc75 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'NPC benchmark failed'}
 $taskRuns+=@{index=$taskIndex;version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
$taskSummary=foreach($taskMode in @('unknown','existing','existing74','new_intro','new_phone','choice')){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_message;after_ns=$taskNew.ns_per_message;wall_change_percent=100*($taskNew.ns_per_message/$taskOld.ns_per_message-1);before_bytes=$taskOld.bytes_per_message;after_bytes=$taskNew.bytes_per_message;before_thread_cpu_ns=$taskOld.thread_cpu_ns_per_message;after_thread_cpu_ns=$taskNew.thread_cpu_ns_per_message}}
 @{mode=$taskMode;equal_useful_output=($taskMode -in @('unknown','existing','existing74'));orders=@($taskOrders)}
}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Four isolated JVMs in74,75,75,74 order;all rounds retained;receipt API,not game FPS or frame timing';records=$taskRuns;summary=@($taskSummary)}|ConvertTo-Json -Depth 16),[Text.UTF8Encoding]::new($false));$taskSummary|ConvertTo-Json -Depth 6;Write-Output "NPC75 BENCHMARK ROOT:$taskRoot"
