param([Parameter(Mandatory=$true)][string[]]$CommonRoots,[Parameter(Mandatory=$true)][string[]]$QuizRoots)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.66-dev.jar'
if((Get-FileHash -LiteralPath $taskJar).Hash -ne 'B36538B44ADD46533576505A186583092CF04E508A1F472BFAC07E6BE5B62A59'){throw 'Different runtime'}
if($CommonRoots.Count -ne 2 -or $QuizRoots.Count -ne 2){throw 'Both orders required'}
$taskScope=Get-Content -LiteralPath (Join-Path $taskProject 'build/safety66-9748a3a1585c46e492b20cbcd70a9cd4/CHECK_SCOPE.json') -Raw|ConvertFrom-Json
if($taskScope.tests_passed.Count -ne 117 -or $taskScope.targeted_only){throw 'Incomplete regression'}
$taskRuns=@();$taskIndex=0
foreach($taskName in $CommonRoots+$QuizRoots){
    $taskRoot=Join-Path $taskProject $taskName
    $taskBefore=Get-Content -LiteralPath "$taskRoot/0.15.65-dev.json" -Raw|ConvertFrom-Json
    $taskAfter=Get-Content -LiteralPath "$taskRoot/0.15.66-dev.json" -Raw|ConvertFrom-Json
    $taskRuns+=@{kind=if($taskIndex -lt 2){'54 common scenarios'}else{'13 observed quiz messages and13 negative messages; positive useful work differs'};order=if($taskIndex%2 -eq 0){'65 then66'}else{'66 then65'};root=$taskName;warmup_batches=10000;measured_batches_per_round=4000;rounds=4;before=$taskBefore;after=$taskAfter}
    if($taskIndex -ge 2){$taskHashes=Get-Content -LiteralPath "$taskRoot/RUNTIME_HASHES.json" -Raw|ConvertFrom-Json;if($taskHashes.'0.15.66-dev' -ne (Get-FileHash -LiteralPath $taskJar).Hash){throw 'Measured different JAR'}}
    $taskIndex++
}
$taskTarget=Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.66_RAW.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve measurements'}
$taskOutput=@{runtime='0.15.66-dev';date='2026-10-15';runtime_jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;baseline_jar_sha256='721732CC20BC69EE2337F1A557DBDEFE6B183602D8932D6EDA0AB43244183113';game_fps_measured=$false;zero_fps_impact_proven=$false;tests_completed_before_measurements=$true;positive_quiz_equal_useful_work=$false;all_cpu_rounds_retained=$true;common_allocation_medians_only=$true;quiz_allocation_rounds_retained=$true;uncontrolled_computer_background_load=$true;runs=$taskRuns}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
