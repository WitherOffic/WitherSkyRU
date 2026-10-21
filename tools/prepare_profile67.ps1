$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject 'build/live-fps-audit-15afb46e55aa4ef4b6266ab9da2380ba'
$taskTarget=Join-Path $taskProject 'translation-source/FPS_LIVE_PROFILE_2026_10_15_SUMMARY.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve saved profile summary'}
$taskReport=Get-Content -LiteralPath "$taskRoot/summary-including-hooks.json" -Raw|ConvertFrom-Json
if($taskReport.scope -notmatch 'injected witherskyru'){throw 'Missing injected-hook accounting'}
$taskOutput=[ordered]@{date='2026-10-15';observed_loaded_version_from_startup_log='0.15.64-dev';recording_duration_seconds=30;source_root='build/live-fps-audit-15afb46e55aa4ef4b6266ab9da2380ba';recording_sha256=(Get-FileHash -LiteralPath "$taskRoot/gameplay.jfr").Hash;raw_recording_not_delivered=$true;scope=$taskReport.scope;sample_events=$taskReport.sample_events;render_thread_samples=$taskReport.render_thread_samples;render_samples_with_mod_stack=$taskReport.render_samples_with_mod_stack;render_mod_inclusive_sample_percent=$taskReport.render_mod_inclusive_sample_percent;render_mod_inclusive=$taskReport.render_mod_inclusive;render_top_self=$taskReport.render_top_self;gc_pause_count=$taskReport.gc_pause_count;gc_pause_total_ms=$taskReport.gc_pause_total_ms;gc_pause_max_ms=$taskReport.gc_pause_max_ms;allocation_rates_not_reported=$true;measured_game_fps=$false;without_mod_control_recording=$false;not_exclusive_mod_cpu_cost=$true;not_proof_of_entire_reported_fps_loss=$true;installed_pack_modified=$false}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
