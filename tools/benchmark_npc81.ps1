$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc81-benchmark-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.80-dev.jar"
$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.81-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '5595F223EEF7F16499C6541CB81F3B69F1203D25C010B4C7439436968BEC1228'){throw 'Wrong80 baseline'}
$taskAfterHash=(Get-FileHash $taskAfter).Hash
$taskArgs=Get-Content "$taskProject/build/npc80-checks-7e205c83ab2e4e4199efbd67bd732426/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskAfter)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkNpc81.java"
if($LASTEXITCODE -ne 0){throw 'Compile benchmark failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.80-dev','0.15.81-dev','0.15.81-dev','0.15.80-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkNpc81 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'NPC benchmark failed'}
 $taskRuns+=@{index=$taskIndex;version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json}
 Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
if((Get-FileHash $taskAfter).Hash -ne $taskAfterHash){throw 'Candidate changed during benchmark'}
$taskSummary=foreach($taskMode in @('unknown','existing_dynamic','existing_exact','existing_style','existing_tal','existing_wizard','existing_tyashoi','new_elle')){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_message;after_ns=$taskNew.ns_per_message;wall_change_percent=100*($taskNew.ns_per_message/$taskOld.ns_per_message-1);before_bytes=$taskOld.bytes_per_message;after_bytes=$taskNew.bytes_per_message;before_cpu_ns=$taskOld.cpu_ns_per_message;after_cpu_ns=$taskNew.cpu_ns_per_message}}
 @{mode=$taskMode;equal_useful_output=($taskMode -ne 'new_elle');orders=@($taskOrders)}
}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Four isolated JVMs in80,81,81,80 order;all rounds retained;chat receipt only,not game FPS or frame timing';records=$taskRuns;summary=@($taskSummary)}|ConvertTo-Json -Depth 16),[Text.UTF8Encoding]::new($false))
$taskSummary|ConvertTo-Json -Depth 6
Write-Output "NPC81 BENCHMARK ROOT:$taskRoot"
