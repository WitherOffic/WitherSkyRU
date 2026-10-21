$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/opt94-benchmark-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc93-checks-9054ad2b27cc4c48bd434da9b36ae938/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.93-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '502D91F03B30262A11499DA0C7DF31195455273DD43F68BA68243C18911C24DF'){throw 'Wrong93 baseline'}
$taskHash=(Get-FileHash $taskAfter).Hash
foreach($taskDir in @('receipt','hud','draw','draw-fixture')){New-Item -ItemType Directory -Path "$taskRoot/$taskDir"|Out-Null}
$taskCp=(@($taskAfter)+$taskLibs)-join ';'
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d "$taskRoot/receipt" "$taskProject/tests/BenchmarkOptimization94.java"
if($LASTEXITCODE -ne 0){throw 'Receipt harness compilation failed'}
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d "$taskRoot/hud" "$taskProject/tests/BenchmarkPrepared72.java"
if($LASTEXITCODE -ne 0){throw 'HUD harness compilation failed'}
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d "$taskRoot/draw-fixture" "$taskProject/tests/fixtures94/net/minecraft/client/gui/components/ChatComponent.java"
if($LASTEXITCODE -ne 0){throw 'Draw caller fixture compilation failed'}
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d "$taskRoot/draw" "$taskProject/tests/BenchmarkProtectedChat94.java"
if($LASTEXITCODE -ne 0){throw 'Draw harness compilation failed'}
$taskAll=@{}
foreach($taskFamily in @('receipt','hud','draw')){
 $taskRuns=@();$taskIndex=0;$taskClass=switch($taskFamily){'receipt'{'BenchmarkOptimization94'}'hud'{'ru.wither.skyru.BenchmarkPrepared72'}'draw'{'BenchmarkProtectedChat94'}}
 foreach($taskVersion in @('0.15.93-dev','0.15.94-dev','0.15.94-dev','0.15.93-dev')){
  $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskFamily/$taskIndex-$taskVersion.json";$taskExtra=@();if($taskFamily -eq 'draw'){$taskExtra=@(($taskVersion -eq '0.15.94-dev').ToString().ToLowerInvariant())}
  & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' "-Dwitherskyru.fixture.path=$taskRoot/draw-fixture" '-cp' ((@("$taskRoot/$taskFamily",$taskJar)+$taskLibs)-join ';') $taskClass $taskFile "$taskRoot/$taskFamily/isolated-$taskIndex" @taskExtra > "$taskRoot/$taskFamily/$taskIndex-output.txt" 2>&1
  if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskFamily/$taskIndex-output.txt" -Tail 10;throw "Failed benchmark $taskFamily/$taskVersion"}
  $taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS $taskFamily $taskIndex $taskVersion";$taskIndex++
 }
 $taskSummary=foreach($taskMode in @($taskRuns[0].results.PSObject.Properties.Name|Where-Object {$_ -ne 'scope'})){
  $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;$taskNanos=if($taskFamily -eq 'receipt'){'ns_per_message'}else{'ns_per_operation'};$taskBytes=if($taskFamily -eq 'receipt'){'bytes_per_message'}else{'bytes_per_operation'};@{before_ns=$taskOld.$taskNanos;after_ns=$taskNew.$taskNanos;wall_change_percent=100*($taskNew.$taskNanos/$taskOld.$taskNanos-1);before_bytes=$taskOld.$taskBytes;after_bytes=$taskNew.$taskBytes}}
  @{mode=$taskMode;orders=@($taskOrders)}
 }
 $taskAll[$taskFamily]=@{records=$taskRuns;summary=@($taskSummary)}
}
if((Get-FileHash $taskAfter).Hash -ne $taskHash){throw 'Candidate changed during measurement'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='12 isolated JVMs;3 families each93,94,94,93;all rounds retained;not game FPS';families=$taskAll;runtime_jar_sha256=$taskHash}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "OPT94 BENCHMARK ROOT:$taskRoot"
