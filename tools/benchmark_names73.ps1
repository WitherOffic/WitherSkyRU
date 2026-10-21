param([switch]$Prototype)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/names73-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path "$taskRoot/tests"|Out-Null
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '1645060A5C13D085B44AFE3666009F88F31BD55CD8C6ED2E0AF13B76EFD48E5D'){throw 'Wrong72 oracle'}
$taskArgs=Get-Content "$taskProject/build/prepared72-checks-32b3a062404840a8993bba091c06f03e/compile-arguments.txt";$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCp=(@($taskBefore)+$taskLibs)-join ';'
if($Prototype){New-Item -ItemType Directory -Path "$taskRoot/candidate"|Out-Null;& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' $taskCp '-d' "$taskRoot/candidate" "$taskProject/src/main/java/ru/wither/skyru/PlayerNames.java";if($LASTEXITCODE -ne 0){throw 'Candidate compile failed'};$taskAfter=$taskBefore}
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' $taskCp '-d' "$taskRoot/tests" "$taskProject/tests/BenchmarkNames73.java";if($LASTEXITCODE -ne 0){throw 'Benchmark compile failed'}
$taskRecords=@();$taskIndex=0
foreach($taskVariant in @('baseline','candidate','candidate','baseline')){
 $taskJar=if($taskVariant -eq 'candidate'){$taskAfter}else{$taskBefore};$taskCp=(@("$taskRoot/tests")+@($(if($Prototype -and $taskVariant -eq 'candidate'){"$taskRoot/candidate"}))+@($taskJar)+$taskLibs|Where-Object {$_})-join ';'
 $taskOutput="$taskRoot/$taskIndex-$taskVariant.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-cp' $taskCp ru.wither.skyru.BenchmarkNames73 $taskOutput "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'Benchmark failed'}
 $taskRecords+=@{index=$taskIndex;variant=$taskVariant;jar_sha256=(Get-FileHash $taskJar).Hash;prototype_class_override=$Prototype.IsPresent -and $taskVariant -eq 'candidate';results=Get-Content $taskOutput -Raw|ConvertFrom-Json};Write-Output "PASS $taskIndex $taskVariant";$taskIndex++
}
$taskSummary=foreach($taskMode in @('index_negative','index_positive','index_legacy','sequence_stable_roster','sequence_dynamic_roster')){
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRecords[$taskPair[0]].results.$taskMode;$taskNew=$taskRecords[$taskPair[1]].results.$taskMode;@{wall_change_percent=100*($taskNew.ns_per_operation/$taskOld.ns_per_operation-1);before_ns=$taskOld.ns_per_operation;after_ns=$taskNew.ns_per_operation;before_bytes=$taskOld.bytes_per_operation;after_bytes=$taskNew.bytes_per_operation;before_thread_cpu_ns=$taskOld.thread_cpu_ns_per_operation;after_thread_cpu_ns=$taskNew.thread_cpu_ns_per_operation}}
 @{mode=$taskMode;orders=@($taskOrders)}
}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{prototype=$Prototype.IsPresent;scope='82-name roster;four isolated JVMs inABBA order;all rounds retained;not game FPS/frame-time proof';records=$taskRecords;summary=@($taskSummary)}|ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
$taskSummary|ConvertTo-Json -Depth 5;Write-Output "NAMES73 BENCHMARK ROOT:$taskRoot"
