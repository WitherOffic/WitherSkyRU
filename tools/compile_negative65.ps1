$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject 'build/negative65-7d0bed7bc13d43529bc16a87f7d87ce3'
$taskHashes=Get-Content -LiteralPath "$taskRoot/RUNTIME_HASHES.json" -Raw|ConvertFrom-Json
if($taskHashes.'0.15.65-dev' -ne '721732CC20BC69EE2337F1A557DBDEFE6B183602D8932D6EDA0AB43244183113'){throw 'Different runtime'}
$taskRuns=@();foreach($taskName in @('0-0.15.64-dev','1-0.15.65-dev','2-0.15.65-dev','3-0.15.64-dev')){$taskRuns+=@{label=$taskName;result=(Get-Content -LiteralPath "$taskRoot/$taskName.json" -Raw|ConvertFrom-Json)}}
$taskTarget=Join-Path $taskProject 'translation-source/PERFORMANCE_0.15.65_NEGATIVE_RAW.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve existing measurements'}
[IO.File]::WriteAllText($taskTarget,(@{scope='Focused recheck of one noisy common scenario, not replacement of all54 results or game FPS';root='build/negative65-7d0bed7bc13d43529bc16a87f7d87ce3';runtime_hashes=$taskHashes;warmup_batches=100000;measured_batches_per_round=100000;rounds=4;runs=$taskRuns}|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
