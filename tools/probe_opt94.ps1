param([Parameter(Mandatory=$true)][string]$Probe)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=(Resolve-Path $Probe).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskProject/build/npc93-checks-9054ad2b27cc4c48bd434da9b36ae938/compile-arguments.txt"
$taskCp=$taskArgs[6].Trim('"')
$taskCandidate="$taskRoot/tests;$taskRoot/candidate;$taskCp"
foreach($taskTest in @('TestRenderContext67','TestNpc93')){
 & "$taskJava/java.exe" '-Xmx768m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCandidate $taskTest "$taskRoot/isolated-$taskTest" "$taskProject/translation-source" > "$taskRoot/$taskTest-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskTest-output.txt" -Tail 10;throw 'Candidate correctness failed'}
 Get-Content "$taskRoot/$taskTest-output.txt" -Tail 1
}
$taskRuns=@();$taskIndex=0
foreach($taskKind in @('before','candidate','candidate','before')){
 $taskRuntime="$taskRoot/tests;$taskCp";if($taskKind -eq 'candidate'){$taskRuntime=$taskCandidate}
 $taskFile="$taskRoot/context-$taskIndex-$taskKind.json"
 & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskRuntime BenchmarkContext67 $taskFile > "$taskRoot/context-$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/context-$taskIndex-output.txt" -Tail 10;throw 'Probe benchmark failed'}
 $taskRuns+=@{kind=$taskKind;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS context $taskIndex $taskKind";$taskIndex++
}
[IO.File]::WriteAllText("$taskRoot/CONTEXT_RAW.json",(@{scope='Read-only candidate classpath experiment;not installed or game FPS';records=$taskRuns}|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
foreach($taskMode in @('fresh_shallow','fresh_deep32','warm_metadata64','churn_metadata300')){
 foreach($taskPair in @(@(0,1),@(3,2))){$taskOld=$taskRuns[$taskPair[0]].results.$taskMode;$taskNew=$taskRuns[$taskPair[1]].results.$taskMode;Write-Output "$taskMode : $([math]::Round(100*($taskNew.ms_per10/$taskOld.ms_per10-1),1))% wall; bytes/10 $($taskOld.bytes_per10) -> $($taskNew.bytes_per10)"}
}
