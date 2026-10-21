param([Parameter(Mandatory=$true)][string[]]$CommonRoots,[Parameter(Mandatory=$true)][string[]]$FullRoots)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar'
if((Get-FileHash -LiteralPath $taskJar).Hash -ne '721732CC20BC69EE2337F1A557DBDEFE6B183602D8932D6EDA0AB43244183113'){throw 'Benchmarked JAR changed'}
if($CommonRoots.Count -ne 2 -or $FullRoots.Count -ne 2){throw 'Both orders of both workloads are required'}
$taskScope=Get-Content -LiteralPath (Join-Path $taskProject 'build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/CHECK_SCOPE.json') -Raw|ConvertFrom-Json
if($taskScope.tests_passed.Count -ne 116){throw 'Final checks incomplete'}
$taskRuns=@();$taskIndex=0
foreach($taskName in $CommonRoots+$FullRoots){
    $taskRoot=Join-Path $taskProject $taskName
    $taskBefore=Get-Content -LiteralPath "$taskRoot/0.15.64-dev.json" -Raw|ConvertFrom-Json
    $taskAfter=Get-Content -LiteralPath "$taskRoot/0.15.65-dev.json" -Raw|ConvertFrom-Json
    $taskRuns+=@{kind=if($taskIndex -lt 2){'54 common scenarios'}else{'10 complete changed tooltips; unequal useful output work'};order=if($taskIndex%2 -eq 0){'64 then65'}else{'65 then64'};root=$taskName;warmup_batches=10000;measured_batches_per_round=4000;rounds=4;before=$taskBefore;after=$taskAfter}
    if($taskIndex -ge 2){$taskHashes=Get-Content -LiteralPath "$taskRoot/RUNTIME_HASHES.json" -Raw|ConvertFrom-Json;if($taskHashes.'0.15.65-dev' -ne (Get-FileHash -LiteralPath $taskJar).Hash){throw 'Different measured runtime'}}
    $taskIndex++
}
$taskPreliminary=@();$taskIndex=0
foreach($taskName in @('build/render-hotpaths-8d7df300910645588f00bae45bfb581a','build/render-hotpaths-0b6425c62a89437b8221fd2c14f2a9ea','build/turbo-benchmark65-bc69311d88c349248780fdef5bdb4dfc','build/turbo-benchmark65-ceaaaf3258bc4be5925621c745c93b11')){
    $taskRoot=Join-Path $taskProject $taskName
    $taskPreliminary+=@{root=$taskName;kind=if($taskIndex -lt 2){'54 common scenarios'}else{'10 changed full tooltips'};order=if($taskIndex%2 -eq 0){'64 then65'}else{'65 then64'};unreleased_candidate_sha256='65A1A3743C168F48EBE74721EB14C8F048A97EEEB303430A8789005BB3AE87BC';before=(Get-Content -LiteralPath "$taskRoot/0.15.64-dev.json" -Raw|ConvertFrom-Json);after=(Get-Content -LiteralPath "$taskRoot/0.15.65-dev.json" -Raw|ConvertFrom-Json)}
    $taskIndex++
}
$taskOutput=@{runtime='0.15.65-dev';date='2026-10-15';runtime_jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;baseline_jar_sha256='4AD3FD841E8430B91760DC12151F92088595D21BA0DE21CC4EFC9323CC39CC6C';same_output_work_for_changed_tooltips=$false;zero_fps_impact_proven=$false;game_fps_measured=$false;all_cpu_rounds_retained=$true;allocation_medians_only=$true;tests_completed_before_measurements=$true;runs=$taskRuns;preliminary_before_uniform_fastpath_not_released=$taskPreliminary}
$taskTarget=Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.65_RAW.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve existing compiled measurements'}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
