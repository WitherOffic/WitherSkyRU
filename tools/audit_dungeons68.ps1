param([Parameter(Mandatory=$true)][string]$FinalCheckRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskCheck=Join-Path $taskProject $FinalCheckRoot
$taskScope=Get-Content "$taskCheck/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskScope.tests_passed.Count -ne 119 -or $taskScope.targeted_only){throw 'Need final119 before comparison'}
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskCheck/compile-arguments.txt";$taskLibraries=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskOldJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar"
if((Get-FileHash -LiteralPath $taskOldJar).Hash -ne '8676237B43BF896DD362FE7F6C89F84B75A2E42901E9793737593E64583779F7'){throw 'Wrong delivered oracle67'}
$taskOldAudit=Join-Path $taskProject ('build/dungeons68-oracle67-'+[Guid]::NewGuid().ToString('N'))
$taskCp=(@($taskCheck,$taskOldJar)+$taskLibraries)-join ';'
& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp AuditItemCatalogue64 "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7" $taskOldAudit | Tee-Object -FilePath "$taskCheck/oracle67-audit-output.txt"
if($LASTEXITCODE -ne 0){throw 'Oracle scan failed'}
$taskBefore=Get-Content "$taskOldAudit/items.json" -Raw|ConvertFrom-Json;$taskAfter=Get-Content "$taskCheck/audit/items.json" -Raw|ConvertFrom-Json
$taskOld=@{};foreach($taskItem in $taskBefore){$taskOld[$taskItem.id]=$taskItem}
$taskFixture=Get-Content "$taskProject/translation-source/DUNGEONS_PRIMARY_0.15.68.json" -Raw|ConvertFrom-Json
$taskChanged=@();$taskRegressions=@()
foreach($taskItem in $taskAfter){$taskPrev=$taskOld[$taskItem.id];if(!$taskPrev){throw 'Different source cards'}
    $taskTextChanged=(@($taskItem.translated_title)+@($taskItem.component_lore)+@($taskItem.sequence_lore) -join "`n") -cne (@($taskPrev.translated_title)+@($taskPrev.component_lore)+@($taskPrev.sequence_lore) -join "`n")
    if($taskTextChanged){$taskChanged+=@{id=$taskItem.id;source_url=$taskItem.source_url;before_title=$taskPrev.translated_title;after_title=$taskItem.translated_title;before_lore=$taskPrev.component_lore;after_lore=$taskItem.component_lore}}
    foreach($taskFlag in @('numeric_change_candidate','numeric_style_change_candidate','new_effective_style_candidate','lost_effective_style_candidate','component_sequence_text_difference_candidate')){if($taskItem.$taskFlag -and !$taskPrev.$taskFlag){$taskRegressions+=@{id=$taskItem.id;flag=$taskFlag}}}
}
if($taskRegressions.Count -gt 0){throw ('New integrity flags: '+($taskRegressions|ConvertTo-Json -Compress))}
$taskRows=@();$taskDungeon=@($taskAfter|Where-Object {$_.id -in $taskFixture.dungeon_footer_card_ids})
$taskRowsBefore=0;$taskRowsAfter=0;$taskEnglishRowsBefore=0;$taskEnglishRowsAfter=0
foreach($taskItem in $taskDungeon){$taskPrev=$taskOld[$taskItem.id];for($taskAt=0;$taskAt -lt $taskItem.source_lore.Count;$taskAt++){if([string]::IsNullOrWhiteSpace($taskItem.source_lore[$taskAt])){continue};$taskRowsBefore++;if($taskItem.component_lore[$taskAt] -cne $taskItem.source_lore[$taskAt]){$taskRowsAfter++}}
    $taskEnglishRowsBefore+=$taskPrev.english_prose_candidate_rows.Count;$taskEnglishRowsAfter+=$taskItem.english_prose_candidate_rows.Count
    foreach($taskAt in $taskItem.english_prose_candidate_rows){$taskRows+=@{id=$taskItem.id;source_row=$taskItem.source_lore[$taskAt];output_row=$taskItem.component_lore[$taskAt];row=$taskAt;source_url=$taskItem.source_url}}
}
$taskSummary=@{date='2026-10-15';before='0.15.67-dev';after='0.15.68-dev';oracle_root=$taskOldAudit;check_root=$FinalCheckRoot;catalogue_cards_compared=$taskAfter.Count;changed_card_outputs=$taskChanged.Count;unchanged_cards_not_quality_certified=($taskAfter.Count-$taskChanged.Count);new_integrity_flags=$taskRegressions;dungeon_footer_cards=$taskDungeon.Count;dungeon_nonblank_rows=$taskRowsBefore;dungeon_rows_changed_vs_english_not_quality_count=$taskRowsAfter;dungeon_rows_unchanged_not_all_errors=($taskRowsBefore-$taskRowsAfter);dungeon_english_candidate_cards_before=@($taskBefore|Where-Object {$_.id -in $taskFixture.dungeon_footer_card_ids -and $_.english_prose_candidate_rows.Count -gt 0}).Count;dungeon_english_candidate_cards_after=@($taskDungeon|Where-Object {$_.english_prose_candidate_rows.Count -gt 0}).Count;dungeon_english_candidate_rows_before=$taskEnglishRowsBefore;dungeon_english_candidate_rows_after=$taskEnglishRowsAfter;all_dungeons_translated=$false;all_five_live_orbs_verified=$false;game_fps_measured=$false;full_selected_cards=@($taskFixture.records.id);changed_cards=$taskChanged;remaining_english_candidates=$taskRows}
$taskOut="$taskProject/translation-source/DUNGEONS_AUDIT_0.15.68.json";if(Test-Path -LiteralPath $taskOut){throw 'Preserve differential'}
[IO.File]::WriteAllText($taskOut,($taskSummary|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
Write-Output "DUNGEONS68 comparison: $($taskAfter.Count) cards,$($taskChanged.Count) changed; no new numeric/style/channel flags; remaining dungeon prose candidates $taskEnglishRowsAfter"
