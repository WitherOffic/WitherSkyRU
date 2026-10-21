param([Parameter(Mandatory=$true)][string[]]$CommonRoots,[Parameter(Mandatory=$true)][string]$ContextRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar'
if((Get-FileHash -LiteralPath $taskJar).Hash -ne '8676237B43BF896DD362FE7F6C89F84B75A2E42901E9793737593E64583779F7'){throw 'Different runtime'}
$taskScope=Get-Content -LiteralPath (Join-Path $taskProject 'build/performance67-416d3fe89d4848579abd66c716a5a057/CHECK_SCOPE.json') -Raw|ConvertFrom-Json
if($taskScope.tests_passed.Count -ne 118 -or $taskScope.targeted_only -or $CommonRoots.Count -ne 2){throw 'Incomplete final checks or benchmark scope'}
$taskRuns=@();$taskIndex=0
foreach($taskName in $CommonRoots){$taskRoot=Join-Path $taskProject $taskName;$taskBefore=Get-Content -LiteralPath "$taskRoot/0.15.66-dev.json" -Raw|ConvertFrom-Json;$taskAfter=Get-Content -LiteralPath "$taskRoot/0.15.67-dev.json" -Raw|ConvertFrom-Json;$taskRuns+=@{kind='54 common scenarios';order=if($taskIndex++ -eq 0){'66 then67'}else{'67 then66'};root=$taskName;before=$taskBefore;after=$taskAfter}}
$taskContextPath=Join-Path $taskProject $ContextRoot
$taskContextManifest=Get-Content -LiteralPath "$taskContextPath/RUNTIME_HASHES.json" -Raw|ConvertFrom-Json
if($taskContextManifest.phase -ne 'final' -or $taskContextManifest.hashes.'0.15.67-dev' -ne (Get-FileHash -LiteralPath $taskJar).Hash){throw 'Not final measured context runtime'}
$taskContexts=@();foreach($taskName in @('0-0.15.66-dev','1-0.15.67-dev','2-0.15.67-dev','3-0.15.66-dev')){$taskContexts+=@{label=$taskName;result=(Get-Content -LiteralPath "$taskContextPath/$taskName.json" -Raw|ConvertFrom-Json)}}
$taskPreliminaryRoot='build/context-benchmark67-c514cd082df04c75be4a06a0f9ed741f';$taskPreliminary=@();foreach($taskName in @('0-0.15.66-dev','1-0.15.67-dev','2-0.15.67-dev','3-0.15.66-dev')){$taskPreliminary+=@{label=$taskName;result=(Get-Content -LiteralPath (Join-Path $taskProject "$taskPreliminaryRoot/$taskName.json") -Raw|ConvertFrom-Json)}}
$taskOutput=@{runtime='0.15.67-dev';date='2026-10-15';runtime_jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;baseline_jar_sha256='B36538B44ADD46533576505A186583092CF04E508A1F472BFAC07E6BE5B62A59';live_profile_is_installed64_not_new67=$true;candidate_game_fps_measured=$false;zero_fps_impact_proven=$false;final_benchmarks_after_final118_checks=$true;all_cpu_rounds_retained=$true;common_allocation_medians_only=$true;context_allocation_rounds_retained=$true;context_root=$ContextRoot;final_context_runs=$taskContexts;common_runs=$taskRuns;unreleased_preliminary_context=@{root=$taskPreliminaryRoot;candidate_sha256='AA68ECF6754373EB0CEF78A01508F2896AC4467E3D195B7427231C96B8456B65';targeted_checks=3;full_checks_interrupted_not_passed=$true;runs=$taskPreliminary}}
$taskTarget=Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.67_RAW.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve measurements'}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
