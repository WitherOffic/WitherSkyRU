$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc102-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/opt94-checks-90d2b9dba1da42fba339980016087ed8/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.101-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.102-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne 'E0EE8AA7086809A24B001F57F71902C778349215670C983BAA1382AF82606A09'){throw 'Wrong101 baseline'}
$taskHash=(Get-FileHash $taskAfter).Hash
foreach($taskDir in @('receipt','hud','mixed')){New-Item -ItemType Directory -Path "$taskRoot/$taskDir"|Out-Null}
$taskCp=(@($taskAfter)+$taskLibs)-join ';'
foreach($taskFamily in @('receipt','hud','mixed')){
 $taskSource=switch($taskFamily){'receipt'{'BenchmarkNpc102.java'}'hud'{'BenchmarkPrepared72.java'}'mixed'{'BenchmarkDynamicHud96.java'}}
 & "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d "$taskRoot/$taskFamily" "$taskProject/tests/$taskSource"
 if($LASTEXITCODE -ne 0){throw 'Harness compilation failed'}
}
$taskAll=@{}
foreach($taskFamily in @('receipt','hud','mixed')){
 $taskRuns=@();$taskIndex=0;$taskClass=switch($taskFamily){'receipt'{'BenchmarkNpc102'}'hud'{'ru.wither.skyru.BenchmarkPrepared72'}'mixed'{'ru.wither.skyru.BenchmarkDynamicHud96'}}
 foreach($taskVersion in @('0.15.101-dev','0.15.102-dev','0.15.102-dev','0.15.101-dev')){
  $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskFamily/$taskIndex-$taskVersion.json";$taskExtra=@()
  & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' ((@("$taskRoot/$taskFamily",$taskJar)+$taskLibs)-join ';') $taskClass $taskFile "$taskRoot/$taskFamily/isolated-$taskIndex" @taskExtra > "$taskRoot/$taskFamily/$taskIndex-output.txt" 2>&1
  if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskFamily/$taskIndex-output.txt" -Tail 10;throw "Failed benchmark $taskFamily/$taskVersion"}
  $taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS $taskFamily $taskIndex $taskVersion";$taskIndex++
 }
 $taskSummary=foreach($taskMode in @($taskRuns[0].results.PSObject.Properties.Name|Where-Object {$_ -ne 'scope'})){
  $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;$taskNanos=if($taskFamily -eq 'receipt'){'ns_per_message'}else{'ns_per_operation'};$taskBytes=if($taskFamily -eq 'receipt'){'bytes_per_message'}else{'bytes_per_operation'};@{before_ns=$taskOld.$taskNanos;after_ns=$taskNew.$taskNanos;wall_change_percent=100*($taskNew.$taskNanos/$taskOld.$taskNanos-1);before_bytes=$taskOld.$taskBytes;after_bytes=$taskNew.$taskBytes}}
  @{mode=$taskMode;equal_useful_output=($taskMode -notlike "new_*");orders=@($taskOrders)}
 }
 $taskAll[$taskFamily]=@{records=$taskRuns;summary=@($taskSummary)}
}
if((Get-FileHash $taskAfter).Hash -ne $taskHash){throw 'Candidate changed during measurement'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='12 isolated JVMs;3 families each101,102,102,101;all rounds retained;not game FPS';families=$taskAll;runtime_jar_sha256=$taskHash}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "NPC102 BENCHMARK ROOT:$taskRoot"
