param([Parameter(Mandatory=$true)][string]$CheckRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskCheck=(Resolve-Path -LiteralPath $CheckRoot).Path
$taskOldArgs=Get-Content "$taskCheck/compile-arguments.txt"
$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/fps71-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCurrent="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.71-dev.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskCurrent)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkFrameNoops71.java"
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskIndex=0;$taskRecords=@();$taskHashes=@{}
foreach($taskVersion in @('0.15.70-dev','0.15.71-dev','0.15.71-dev','0.15.70-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskHashes[$taskVersion]=(Get-FileHash -LiteralPath $taskJar).Hash
 $taskFile="$taskRoot/$taskIndex-$taskVersion.json";$taskCp=(@($taskRoot,$taskJar)+$taskLibs)-join ';'
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkFrameNoops71 $taskFile "$taskRoot/isolated-$taskIndex" > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 15;throw 'Benchmark failed'}
 $taskRecords+=@{index=$taskIndex;version=$taskVersion;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "BENCHMARK PASS $taskIndex $taskVersion";$taskIndex++
}
if($taskHashes['0.15.70-dev'] -ne '32AFF41415A452DCF90AE7D02956F7BC33F0F96725206C13CDDEFB0F66CF3DA5'){throw 'Wrong baseline'}
$taskSummary=@();foreach($taskMode in @('numeric_sequence','mixed_sequence','prose_sequence','technical_scope','vendor_scope')){
 $taskOrders=@();foreach($taskPair in @(@(0,1),@(3,2))){$taskBefore=$taskRecords[$taskPair[0]].results.$taskMode;$taskAfter=$taskRecords[$taskPair[1]].results.$taskMode;$taskOrders+=@{before_ns=$taskBefore.ns_per_operation;after_ns=$taskAfter.ns_per_operation;cpu_change_percent=100*($taskAfter.ns_per_operation/$taskBefore.ns_per_operation-1);before_bytes=$taskBefore.bytes_per_operation;after_bytes=$taskAfter.bytes_per_operation}}
 $taskSummary+=@{mode=$taskMode;orders=$taskOrders}
}
$taskRaw=@{scope='Four isolated JVMs in70,71,71,70 order; all 4 rounds retained per mode. Microbenchmark, not game FPS,0.1%low or GPU. No game A/B validation of new executable.';runtime_hashes=$taskHashes;records=$taskRecords;summary=$taskSummary}
[IO.File]::WriteAllText("$taskRoot/RAW.json",($taskRaw|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false))
$taskSummary|ConvertTo-Json -Depth 5
Write-Output "FPS71 BENCHMARK ROOT:$taskRoot"
