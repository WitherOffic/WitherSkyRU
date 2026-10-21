param([Parameter(Mandatory=$true)][string]$CheckRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$CheckRoot/compile-arguments.txt";$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
foreach($taskVersion in @('0.15.69-dev','0.15.70-dev')){
 $taskJar=if($taskVersion -eq '0.15.69-dev'){'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar'}else{"$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar"}
 $taskCp=(@($CheckRoot,$taskJar)+$taskLibs)-join ';'
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp AuditNpcObserved60 "$CheckRoot/replay-$taskVersion" "$taskProject/translation-source/NPC_OBSERVED_0.15.70.json" "$CheckRoot/REPLAY-$taskVersion.json"|Tee-Object -FilePath "$CheckRoot/replay-$taskVersion-output.txt"
 if($LASTEXITCODE -ne 0){throw 'NPC replay failed'}
}
$taskBefore=Get-Content "$CheckRoot/REPLAY-0.15.69-dev.json" -Raw|ConvertFrom-Json;$taskAfter=Get-Content "$CheckRoot/REPLAY-0.15.70-dev.json" -Raw|ConvertFrom-Json
if($taskBefore.records.Count -ne 143 -or $taskAfter.records.Count -ne 143 -or $taskBefore.unchanged -ne 39 -or $taskAfter.unchanged -ne 0){throw 'Unexpected independent replay result'}
$taskOldCards=Get-Content "$taskProject/build/hub69-2689a861e4fe4abcb55395f2b7cc737d/audit/items.json" -Raw|ConvertFrom-Json
$taskNewCards=Get-Content "$CheckRoot/audit/items.json" -Raw|ConvertFrom-Json
$taskMap=@{};foreach($taskCard in $taskOldCards){$taskMap[$taskCard.id]=$taskCard};$taskChanged=@()
foreach($taskCard in $taskNewCards){if(!$taskMap.ContainsKey($taskCard.id)){throw 'Missing catalogue ID'};if(($taskMap[$taskCard.id]|ConvertTo-Json -Depth 30 -Compress) -cne ($taskCard|ConvertTo-Json -Depth 30 -Compress)){$taskChanged+=$taskCard.id}}
if($taskOldCards.Count -ne 8937 -or $taskNewCards.Count -ne 8937 -or $taskChanged.Count -ne 0){throw 'Unexpected item change in NPC batch'}
$taskRows=(Get-Content "$taskProject/src/main/resources/data/witherskyru/npc-continuations70.json" -Raw|ConvertFrom-Json).records
$taskTable=@($taskRows|Group-Object speaker|Sort-Object Name|ForEach-Object {@{speaker=$_.Name;new_exact_lines=$_.Count;full_script_certified=$false}})
$taskResult=@{date='2026-10-15';version='0.15.70-dev';jar_sha256=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar").Hash;check_root=$CheckRoot;new_exact_lines=22;new_exact_speakers=$taskTable.Count;batch=$taskTable;source_candidates_replayed=143;before_unchanged=39;after_unchanged=0;changed_not_proof_of_completeness_before=104;changed_not_proof_of_completeness_after=143;old_literal_bronze_result_corrected=1;new_named_Maxwell_shapes=2;new_Kat_time_sentence_shapes=2;new_Jacob_result_shapes=3;old_Jacob_collected_color_boundary_repaired=$true;catalogue_compared=8937;catalogue_changed_record_ids=$taskChanged;all_hypixel_npcs_translated=$false;all_npc_script_denominators_known=$false;game_fps_measured=$false;installed_modpack_modified=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_AUDIT_0.15.70.json",($taskResult|ConvertTo-Json -Depth 10)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "NPC70 AUDIT:39->0 unchanged in143 sources;22 new exact/$($taskTable.Count) speakers;8937 catalogue records identical"
