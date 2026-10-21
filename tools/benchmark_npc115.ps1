$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc115-verify-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.115-dev.jar";$taskHash=(Get-FileHash $taskAfter).Hash
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskAfter)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/BenchmarkNpc115.java"
if($LASTEXITCODE -ne 0){throw 'Harness compilation failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.114-dev','0.15.115-dev','0.15.115-dev','0.15.114-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkNpc115 $taskFile "$taskRoot/isolated-$taskIndex" focused > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 10;throw 'NPC115 benchmark failed'}
 $taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS NPC115 $taskIndex $taskVersion";$taskIndex++
}
$taskSummary=foreach($taskMode in @($taskRuns[0].results.PSObject.Properties.Name|Where-Object {$_ -ne 'scope'})){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_message;after_ns=$taskNew.ns_per_message;wall_change_percent=100*($taskNew.ns_per_message/$taskOld.ns_per_message-1);before_bytes=$taskOld.bytes_per_message;after_bytes=$taskNew.bytes_per_message}}
 @{mode=$taskMode;equal_useful_output=$taskRuns[0].results.$taskMode.equal_useful_output;orders=@($taskOrders)}
}
if((Get-FileHash $taskAfter).Hash -ne $taskHash){throw 'Candidate changed'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Four isolated JVMs114,115,115,114;seventeen equal-work modes,longer warmup/repeated rounds;not game FPS';records=$taskRuns;summary=@($taskSummary);runtime_jar_sha256=$taskHash}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "NPC115 FOCUSED BENCHMARK ROOT:$taskRoot"
