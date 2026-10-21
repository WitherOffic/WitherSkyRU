$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc80-benchmark-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.79-dev.jar"
$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.80-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '5CB882D29B6D4C2BC52991C0C7A1EC5385939A8AA0A8D670CCA3C7C5A83BFCF0'){throw 'Wrong79 baseline'}
$taskAfterHash=(Get-FileHash $taskAfter).Hash
$taskArgs=Get-Content "$taskProject/build/npc79-checks-0f398f45a86b4858b1a8919cfdcac529/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskAfter)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkNpc80.java"
if($LASTEXITCODE -ne 0){throw 'Compile benchmark failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.79-dev','0.15.80-dev','0.15.80-dev','0.15.79-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkNpc80 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'NPC benchmark failed'}
 $taskRuns+=@{index=$taskIndex;version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json}
 Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
if((Get-FileHash $taskAfter).Hash -ne $taskAfterHash){throw 'Candidate changed during benchmark'}
$taskSummary=foreach($taskMode in @('unknown','existing_dynamic','existing_exact','existing_style','existing_tal','existing_wizard','new_tyashoi')){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_message;after_ns=$taskNew.ns_per_message;wall_change_percent=100*($taskNew.ns_per_message/$taskOld.ns_per_message-1);before_bytes=$taskOld.bytes_per_message;after_bytes=$taskNew.bytes_per_message;before_cpu_ns=$taskOld.cpu_ns_per_message;after_cpu_ns=$taskNew.cpu_ns_per_message}}
 @{mode=$taskMode;equal_useful_output=($taskMode -ne 'new_tyashoi');orders=@($taskOrders)}
}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Four isolated JVMs in79,80,80,79 order;all rounds retained;chat receipt only,not game FPS or frame timing';records=$taskRuns;summary=@($taskSummary)}|ConvertTo-Json -Depth 16),[Text.UTF8Encoding]::new($false))
$taskSummary|ConvertTo-Json -Depth 6
Write-Output "NPC80 BENCHMARK ROOT:$taskRoot"
