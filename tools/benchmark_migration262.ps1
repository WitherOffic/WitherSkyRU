param([Parameter(Mandatory=$true)][string]$BaselineJar)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskCandidate="$taskProject/build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar"
$taskRoot=Join-Path $taskProject ('build/performance262-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot | Out-Null
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim().Split(';')
$taskDeps=@($taskCp | Where-Object {$_ -notmatch '[\\/]build[\\/](classes[\\/]java[\\/]main|resources[\\/]main)$'})
$taskModes=@('stable_scoped','titles_pipeline','tooltip_components','tooltip_sequences','numeric62_pipeline','disabled_pipeline','debug_pipeline','roster_lookup')
$taskRuns=@();$taskIndex=0
# Isolated sequential JVMs, A-B-B-A, identical harness and Minecraft 26.2 libraries.
# This isolates the mod changes; it is not a 26.1.2 vs 26.2 game/FPS comparison.
foreach($taskLabel in @('baseline115','candidate262','candidate262','baseline115')){
 $taskJar=if($taskLabel -eq 'baseline115'){$BaselineJar}else{$taskCandidate}
 foreach($taskMode in $taskModes){
  $taskOutput="$taskRoot/$taskIndex-$taskMode.json"
  & (Join-Path $env:JAVA_HOME 'bin/java.exe') '-Xms256m' '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dwitherskyru.benchmark.warmup=20000' '-Dwitherskyru.benchmark.measure=10000' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' ((@($taskJar)+$taskDeps)-join ';') BenchmarkRenderHotPaths "$taskRoot/config-$taskIndex-$taskMode" $taskLabel $taskOutput > "$taskRoot/$taskIndex-$taskMode.txt" 2>&1
  if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-$taskMode.txt" -Tail 15;throw 'Benchmark failed'}
  $taskRuns+=@{label=$taskLabel;order=$taskIndex;mode=$taskMode;jar_sha256=(Get-FileHash $taskJar).Hash;result=(Get-Content $taskOutput -Raw|ConvertFrom-Json).$taskMode}
  Write-Output "PERFORMANCE262 $taskIndex $taskLabel $taskMode"
 }
 $taskIndex++
}
$taskSummary=foreach($taskMode in $taskModes){
 $taskEntries=@($taskRuns|Where-Object {$_.mode -eq $taskMode})
 $taskOrders=foreach($taskPair in @(@(0,1),@(3,2))){
  $taskA=$taskEntries[$taskPair[0]].result;$taskB=$taskEntries[$taskPair[1]].result
  @{change_percent=100*($taskB.ms_per10/$taskA.ms_per10-1);baseline_ms_per10=$taskA.ms_per10;candidate_ms_per10=$taskB.ms_per10;baseline_bytes_per10=$taskA.allocated_bytes_per10;candidate_bytes_per10=$taskB.allocated_bytes_per10}
 }
 @{mode=$taskMode;orders=@($taskOrders)}
}
@{scope='8 unchanged synthetic text API workloads;32 sequential JVMs, each mode A-B-B-A;20000 warmup/4x10000 measured10-row batches. Not native rendering, game FPS, 0.1% lows or zero cost.';runs=$taskRuns;summary=@($taskSummary)} | ConvertTo-Json -Depth 10 | Set-Content "$taskRoot/RESULT.json" -Encoding utf8
Write-Output "PERFORMANCE262_ROOT=$taskRoot"
