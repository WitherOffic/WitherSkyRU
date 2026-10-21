$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc104-hud-verify-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc104-checks-0ac8d09677ec435c8a4e23c33f06ed07/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.104-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskJar)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/BenchmarkHud104Verify.java"
if($LASTEXITCODE -ne 0){throw 'HUD verify compilation failed'}
$taskRuns=@();$taskIndex=0
foreach($taskVersion in @('0.15.103-dev','0.15.104-dev','0.15.104-dev','0.15.103-dev')){
 $taskRuntime="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' ((@($taskRoot,$taskRuntime)+$taskLibs)-join ';') ru.wither.skyru.BenchmarkHud104Verify $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 10;throw 'HUD verify failed'}
 $taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskRuntime).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS long HUD $taskIndex $taskVersion";$taskIndex++
}
$taskSummary=foreach($taskMode in @($taskRuns[0].results.PSObject.Properties.Name|Where-Object {$_ -ne 'scope'})){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;@{before_ns=$taskOld.ns_per_operation;after_ns=$taskNew.ns_per_operation;wall_change_percent=100*($taskNew.ns_per_operation/$taskOld.ns_per_operation-1);before_bytes=$taskOld.bytes_per_operation;after_bytes=$taskNew.bytes_per_operation}}
 @{mode=$taskMode;equal_useful_output=$true;orders=@($taskOrders)}
}
if((Get-FileHash $taskJar).Hash -ne $taskHash){throw 'Candidate changed'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Two equal-work HUD modes,4isolated JVMs103/104/104/103;2million warmup,four2million-operation rounds;not game FPS';runtime_jar_sha256=$taskHash;records=$taskRuns;summary=@($taskSummary)}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "NPC104 LONG HUD ROOT:$taskRoot"
