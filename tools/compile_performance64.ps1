$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.64-dev.jar'
if((Get-FileHash -LiteralPath $taskJar).Hash -ne '4AD3FD841E8430B91760DC12151F92088595D21BA0DE21CC4EFC9323CC39CC6C'){throw 'Benchmarked JAR has changed'}
$taskCommon=@('render-hotpaths-1ab6ee762b3f4cc6a85a0e5c22a11454','render-hotpaths-57074de4911f4877aad2d50fd8e3223d')
$taskFull=@('catalogue-benchmark64-48359e3a30704bd7bd8a7ef68060c398','catalogue-benchmark64-de61e3fa471044eea080155c72645a1c')
$taskRuns=@();$taskIndex=0
foreach($taskName in $taskCommon+$taskFull){$taskRoot=Join-Path $taskProject "build/$taskName";$taskBefore=Get-Content -LiteralPath "$taskRoot/0.15.62-dev.json" -Raw|ConvertFrom-Json;$taskAfter=Get-Content -LiteralPath "$taskRoot/0.15.64-dev.json" -Raw|ConvertFrom-Json;$taskRuns+=@{kind=if($taskIndex -lt 2){'54 common scenarios'}else{'10 complete changed tooltips; unequal useful output work'};order=if($taskIndex%2 -eq 0){'62 then64'}else{'64 then62'};root="build/$taskName";warmup_batches=10000;measured_batches_per_round=4000;rounds=4;before=$taskBefore;after=$taskAfter};$taskIndex++}
$taskOutput=@{runtime='0.15.64-dev';runtime_jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;baseline_jar_sha256='07D7632DD8C5772E1E54BCEF0A8E9B882BA87F30E1AB7569686224CDEFBAC4C3';same_output_work_for_changed_tooltips=$false;zero_fps_impact_proven=$false;game_fps_measured=$false;all_rounds_retained=$true;tests_completed_before_measurements=$true;runs=$taskRuns}
$taskTarget=Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.64_RAW.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve existing compiled measurements'}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
