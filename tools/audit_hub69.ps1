param([Parameter(Mandatory=$true)][string]$CheckRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$CheckRoot/compile-arguments.txt"
$taskLibraries=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCapture="$taskProject/translation-source/HUB_NPC_OBSERVED_0.15.69.json"
foreach($taskVersion in @('0.15.68-dev','0.15.69-dev')){
 $taskJar=if($taskVersion -eq '0.15.68-dev'){'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar'}else{"$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar"}
 $taskCp=(@($CheckRoot,$taskJar)+$taskLibraries)-join ';'
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp AuditNpcObserved60 "$CheckRoot/replay-$taskVersion" $taskCapture "$CheckRoot/REPLAY-$taskVersion.json"|Tee-Object -FilePath "$CheckRoot/replay-$taskVersion-output.txt"
 if($LASTEXITCODE -ne 0){throw 'Public NPC replay failed'}
}
$taskBefore=Get-Content "$CheckRoot/REPLAY-0.15.68-dev.json" -Raw|ConvertFrom-Json
$taskAfter=Get-Content "$CheckRoot/REPLAY-0.15.69-dev.json" -Raw|ConvertFrom-Json
$taskNew=Get-Content "$taskProject/src/main/resources/data/witherskyru/hub-dialogues.json" -Raw|ConvertFrom-Json
$taskTable=@($taskNew.records|Group-Object speaker|Sort-Object Name|ForEach-Object {@{speaker=$_.Name;new_exact_lines=$_.Count;observed_source_bodies=@($_.Group|Where-Object evidence -eq 'observed_public_npc_line').Count;published_only_variants=@($_.Group|Where-Object evidence -ne 'observed_public_npc_line').Count;complete_npc_script_certified=$false}})
$taskInventory=@{}
foreach($taskResource in Get-ChildItem "$taskProject/src/main/resources/data/witherskyru" -Filter '*dialogues.json'){
 foreach($taskRow in (Get-Content -LiteralPath $taskResource.FullName -Raw|ConvertFrom-Json).records){if(!$taskInventory.ContainsKey($taskRow.speaker)){$taskInventory[$taskRow.speaker]=@()};$taskInventory[$taskRow.speaker]+=@{source=$taskRow.source;resource=$taskResource.Name}}
}
$taskCensus=@($taskInventory.GetEnumerator()|Sort-Object Name|ForEach-Object {@{speaker=$_.Name;exact_dialogue_records=$_.Value.Count;resources=@($_.Value.resource|Sort-Object -Unique);full_npc_script_denominator_known=$false}})
$taskCatalogueBefore=Get-Content "$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c/audit/items.json" -Raw|ConvertFrom-Json
$taskCatalogueAfter=Get-Content "$CheckRoot/audit/items.json" -Raw|ConvertFrom-Json
$taskBeforeMap=@{};foreach($taskCard in $taskCatalogueBefore){$taskBeforeMap[$taskCard.id]=$taskCard}
$taskChanged=@();foreach($taskCard in $taskCatalogueAfter){$taskOld=$taskBeforeMap[$taskCard.id];if(!$taskOld){throw 'Catalogue ID mismatch'};if(($taskOld|ConvertTo-Json -Depth 30 -Compress) -cne ($taskCard|ConvertTo-Json -Depth 30 -Compress)){$taskChanged+=,$taskCard.id}}
if($taskCatalogueAfter.Count -ne 8937 -or $taskCatalogueBefore.Count -ne 8937){throw 'Incomplete catalogue differential'}
$taskResult=@{date='2026-10-15';scope='Exact finite public NPC fixture and pinned catalogue differential;not a server-wide NPC/script census. Changed text is not proof of full translation. Actual reviewed targets checked separately by TestHubNpcs69.';version='0.15.69-dev';check_root=$CheckRoot;jar_sha256=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar").Hash;batch=$taskTable;known_exact_speakers=$taskCensus;before=@{unchanged=$taskBefore.unchanged;changed_not_complete=$taskBefore.changed_not_proof_of_complete_translation};after=@{unchanged=$taskAfter.unchanged;changed_not_complete=$taskAfter.changed_not_proof_of_complete_translation};remaining_observed_unchanged=@($taskAfter.records|Where-Object unchanged_plain);catalogue_cards_compared=8937;catalogue_changed_record_ids=$taskChanged;all_hypixel_npcs_translated=$false;all_npc_script_denominators_known=$false;private_full_logs_exported=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/HUB_NPC_AUDIT_0.15.69.json",($taskResult|ConvertTo-Json -Depth 15)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "HUB69 AUDIT: unchanged observed $($taskBefore.unchanged) -> $($taskAfter.unchanged); catalogue changed records $($taskChanged.Count); exact speaker identifiers $($taskCensus.Count) (not complete scripts)"
