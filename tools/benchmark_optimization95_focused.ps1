$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/opt95-focused-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/opt94-checks-90d2b9dba1da42fba339980016087ed8/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.95-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne 'A784B75D65EFBCB8246C72CEE0AE7B60525EBDA9AA07B664127BC39A547BAC00'){throw 'Wrong94 baseline'}
$taskHash=(Get-FileHash $taskAfter).Hash
$taskAll=@{}
foreach($taskFamily in @('hud','receipt')){
 New-Item -ItemType Directory -Path "$taskRoot/$taskFamily"|Out-Null
 $taskSource=if($taskFamily -eq 'hud'){'BenchmarkHudFocus95'}else{'BenchmarkReceiptFocus95'}
 & "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskAfter)+$taskLibs)-join ';') -d "$taskRoot/$taskFamily" "$taskProject/tests/$taskSource.java"
 if($LASTEXITCODE -ne 0){throw 'Focused harness compilation failed'}
 $taskClass=if($taskFamily -eq 'hud'){"ru.wither.skyru.$taskSource"}else{$taskSource}
 $taskRuns=@();$taskIndex=0
 foreach($taskVersion in @('0.15.94-dev','0.15.95-dev','0.15.95-dev','0.15.94-dev','0.15.94-dev','0.15.95-dev')){
  $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskFamily/$taskIndex-$taskVersion.json";$taskExtra=if($taskFamily -eq 'receipt'){@('focused')}else{@()}
  & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' ((@("$taskRoot/$taskFamily",$taskJar)+$taskLibs)-join ';') $taskClass $taskFile "$taskRoot/$taskFamily/isolated-$taskIndex" @taskExtra > "$taskRoot/$taskFamily/$taskIndex-output.txt" 2>&1
  if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskFamily/$taskIndex-output.txt" -Tail 10;throw "Failed focused benchmark $taskFamily/$taskVersion"}
  $taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS focused $taskFamily $taskIndex $taskVersion";$taskIndex++
 }
 $taskSummary=foreach($taskMode in @($taskRuns[0].results.PSObject.Properties.Name|Where-Object {$_ -ne 'scope'})){
  $taskOrders=foreach($taskPair in @(@(0,1),@(3,2),@(4,5))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;$taskNanos=if($taskFamily -eq 'receipt'){'ns_per_message'}else{'ns_per_operation'};$taskBytes=if($taskFamily -eq 'receipt'){'bytes_per_message'}else{'bytes_per_operation'};@{before_ns=$taskOld.$taskNanos;after_ns=$taskNew.$taskNanos;wall_change_percent=100*($taskNew.$taskNanos/$taskOld.$taskNanos-1);before_bytes=$taskOld.$taskBytes;after_bytes=$taskNew.$taskBytes}}
  @{mode=$taskMode;orders=@($taskOrders)}
 }
 $taskAll[$taskFamily]=@{records=$taskRuns;summary=@($taskSummary)}
}
if((Get-FileHash $taskAfter).Hash -ne $taskHash){throw 'Candidate changed during measurement'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='12 focused isolated JVMs;2 families each94,95,95,94,94,95;all rounds retained;not game FPS';families=$taskAll;runtime_jar_sha256=$taskHash}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "OPT95 FOCUSED ROOT:$taskRoot"
