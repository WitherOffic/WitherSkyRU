$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskData="$taskProject/src/main/resources/data/witherskyru"
$taskReview="$taskProject/translation-source/HUB_NPC_0.15.69_REVIEWED.tsv"
$taskPublished=$false;$taskRows=@();$taskKeys=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskExisting=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskResource in (Get-ChildItem -LiteralPath $taskData -Filter '*dialogues.json' | Where-Object Name -ne 'hub-dialogues.json')){foreach($taskRecord in ((Get-Content -LiteralPath $taskResource.FullName -Raw|ConvertFrom-Json).records)){[void]$taskExisting.Add($taskRecord.source)}}
foreach($taskLine in [IO.File]::ReadAllLines($taskReview)){
 if($taskLine.StartsWith('# Published')){$taskPublished=$true};if(!$taskLine -or $taskLine.StartsWith('#')){continue}
 $taskFields=$taskLine.Split('|');if($taskFields.Count -notin @(3,4)){throw 'Invalid reviewed row'}
 $taskSpeaker=$taskFields[0];$taskPrefix="[NPC] ${taskSpeaker}: ";$taskSource=$taskPrefix+$taskFields[1]
 if(!$taskKeys.Add($taskSource) -or $taskExisting.Contains($taskSource)){throw "Overlapping dialogue: $taskSource"}
 $taskRecord=[ordered]@{source_key=('hub69.'+$taskRows.Count);speaker=$taskSpeaker;source=$taskSource;translation=($taskPrefix+$taskFields[2]);evidence=if($taskPublished){'published_exact_variant_not_live_captured'}else{'observed_public_npc_line'}}
 if($taskFields.Count -eq 4){$taskRecord.style_anchors=@($taskFields[3]|ConvertFrom-Json -AsHashtable)}
 if($taskPublished){$taskRecord.source_url=switch($taskSpeaker){'Maxwell'{'https://hypixel-skyblock.fandom.com/wiki/Maxwell'}'Jacobus'{'https://wiki.hypixel.net/Jacobus'}'Maths Enjoyer'{'https://hypixel-skyblock.fandom.com/wiki/Maths_Enjoyer'}}}
 $taskRows+=,$taskRecord
}
$taskObservedSpeakers=@($taskRows|Where-Object evidence -eq 'observed_public_npc_line'|ForEach-Object speaker|Sort-Object -Unique)
$taskLogDir='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs'
$taskCapture=@();$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskLog in (Get-ChildItem -LiteralPath $taskLogDir -File|Where-Object {$_.Name -match '^(latest\.log|2026-10-1[345].*\.log\.gz)$'})){
 $taskFile=[IO.File]::OpenRead($taskLog.FullName);$taskReader=if($taskLog.Extension -eq '.gz'){[IO.StreamReader]::new([IO.Compression.GZipStream]::new($taskFile,[IO.Compression.CompressionMode]::Decompress))}else{[IO.StreamReader]::new($taskFile)}
 $taskLineNumber=0;$taskHash=$null
 try{while(($taskLine=$taskReader.ReadLine()) -ne $null){
  $taskLineNumber++
  if($taskLine -notmatch '\[CHAT\] (?:\[[0-9:]+\] )?(\[NPC\] ([A-Za-z .\x27-]{1,40}): .*)$'){continue}
  $taskSample=$Matches[1];$taskSpeaker=$Matches[2]
  if($taskSpeaker -notin $taskObservedSpeakers -or $taskSample -match 'Wither_101|\bhead\]'){continue}
  $taskPlain=[regex]::Replace($taskSample,'(?i)§[0-9a-fk-or]','')
  if(!$taskSeen.Add($taskSample)){continue}
  if(!$taskHash){$taskHash=(Get-FileHash -LiteralPath $taskLog.FullName).Hash}
  $taskCapture+=@{speaker=$taskSpeaker;source=$taskPlain;sample=$taskSample;archive=$taskLog.Name;archive_sha256=$taskHash;line=$taskLineNumber}
 }}finally{$taskReader.Dispose();$taskFile.Dispose()}
}
foreach($taskRecord in ($taskRows|Where-Object evidence -eq 'observed_public_npc_line')){
 if($taskRecord.source -cnotin $taskCapture.source){throw "No independent observed source: $($taskRecord.source)"}
}
$taskChoices=@(
 @{source='Select an option: [Ok, then what?]';target='Выберите ответ: [Хорошо, а дальше?]'},
 @{source='Select an option: [Accessory Power?]';target='Выберите ответ: [Сила аксессуаров?]'},
 @{source="Select an option: [That's amazing!]";target='Выберите ответ: [Потрясающе!]'}
)
$taskChoiceCapture=@()
# Only the three reviewed public option layouts; no arbitrary choices/form input are exported.
foreach($taskLog in (Get-ChildItem -LiteralPath $taskLogDir -File|Where-Object {$_.Name -match '^2026-10-15.*\.log\.gz$|^latest\.log$'})){
 $taskFile=[IO.File]::OpenRead($taskLog.FullName);$taskReader=if($taskLog.Extension -eq '.gz'){[IO.StreamReader]::new([IO.Compression.GZipStream]::new($taskFile,[IO.Compression.CompressionMode]::Decompress))}else{[IO.StreamReader]::new($taskFile)}
 try{while(($taskLine=$taskReader.ReadLine()) -ne $null){if($taskLine -notmatch '\[CHAT\] (?:\[[0-9:]+\] )?(.*Select an option:.*)$'){continue};$taskSample=$Matches[1];$taskPlain=[regex]::Replace($taskSample,'(?i)§[0-9a-fk-or]','').Trim();if($taskPlain -cin $taskChoices.source -and $taskSample -cnotin $taskChoiceCapture.sample){$taskChoiceCapture+=@{sample=$taskSample;source=$taskPlain;archive=$taskLog.Name}}}}finally{$taskReader.Dispose();$taskFile.Dispose()}
}
$taskResource=[ordered]@{format_version=1;language='ru';scope='Finite exact NPC additions; all existing providers unchanged. Loaded into the existing story lookup at startup, not a ninth per-message provider. Published legacy variants are not claims about current mechanics.';records=$taskRows}
[IO.File]::WriteAllText("$taskData/hub-dialogues.json",($taskResource|ConvertTo-Json -Depth 12)+"`n",[Text.UTF8Encoding]::new($false))
$taskEvidence=@{date='2026-10-15';scope='Whitelisted public NPC display lines only. No full log, player chat, personal name greeting or form answer. Legacy text preserves colours but does not prove original channel/click/hover metadata. Independent source samples are not synthesized from targets.';review_sha256=(Get-FileHash -LiteralPath $taskReview).Hash;records=$taskCapture;choices=$taskChoices;observed_choice_samples=$taskChoiceCapture;published_sources=@('https://hypixel-skyblock.fandom.com/wiki/Maxwell','https://wiki.hypixel.net/Jacobus','https://hypixel-skyblock.fandom.com/wiki/Maths_Enjoyer');current_accessory_power_overrides_old_magical_power_claims=$true;all_hypixel_npcs_translated=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/HUB_NPC_OBSERVED_0.15.69.json",($taskEvidence|ConvertTo-Json -Depth 12)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "HUB69: $($taskRows.Count) authored lines; $(@($taskRows|Where-Object evidence -eq 'observed_public_npc_line').Count) observed sources; $($taskCapture.Count) scoped observed variants; $($taskChoiceCapture.Count) observed choice samples"
