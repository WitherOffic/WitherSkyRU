$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc74-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar";if((Get-FileHash $taskBefore).Hash -ne '5805CB5C5973434E666ACF6129F12DB9B005F20EF7BEAB852541AAA6995E8FDF'){throw 'Wrong73 baseline'}
$taskArgs=Get-Content "$taskProject/build/names73-checks-1cc05db78e3b453da3efb2d5c9b40038/compile-arguments.txt";$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskAfter)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkNpc74.java";if($LASTEXITCODE -ne 0){throw 'Compile benchmark failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.73-dev','0.15.74-dev','0.15.74-dev','0.15.73-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkNpc74 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'NPC benchmark failed'}
 $taskRuns+=@{index=$taskIndex;version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
$taskSummary=foreach($taskMode in @('unknown','existing','new_intro','new_phone','choice')){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_message;after_ns=$taskNew.ns_per_message;wall_change_percent=100*($taskNew.ns_per_message/$taskOld.ns_per_message-1);before_bytes=$taskOld.bytes_per_message;after_bytes=$taskNew.bytes_per_message;before_thread_cpu_ns=$taskOld.thread_cpu_ns_per_message;after_thread_cpu_ns=$taskNew.thread_cpu_ns_per_message}}
 @{mode=$taskMode;equal_useful_output=($taskMode -in @('unknown','existing'));orders=@($taskOrders)}
}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Four isolated JVMs in73,74,74,73 order;all rounds retained;receipt API,not game FPS or frame timing';records=$taskRuns;summary=@($taskSummary)}|ConvertTo-Json -Depth 16),[Text.UTF8Encoding]::new($false));$taskSummary|ConvertTo-Json -Depth 6;Write-Output "NPC74 BENCHMARK ROOT:$taskRoot"
