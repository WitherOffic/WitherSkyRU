$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskData="$taskProject/src/main/resources/data/witherskyru"
$taskSource="$taskProject/translation-source/NPC_0.15.70_REVIEWED.tsv"
$taskCaptured=Get-Content "$taskProject/translation-source/NPC_GAPS_OBSERVED_0.15.60.json" -Raw|ConvertFrom-Json
$taskKnown=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskResource in (Get-ChildItem -LiteralPath $taskData -Filter '*dialogues.json')){foreach($taskRow in (Get-Content -LiteralPath $taskResource.FullName -Raw|ConvertFrom-Json).records){[void]$taskKnown.Add($taskRow.source)}}
$taskRows=@();$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskLine in [IO.File]::ReadAllLines($taskSource)){
 if(!$taskLine -or $taskLine.StartsWith('#')){continue};$taskFields=$taskLine.Split('|');if($taskFields.Count -notin @(3,4)){throw 'Bad reviewed source'}
 $taskPrefix="[NPC] $($taskFields[0]): ";$taskKey=$taskPrefix+$taskFields[1]
 if($taskKey -cnotin $taskCaptured.records.source -or !$taskSeen.Add($taskKey) -or $taskKnown.Contains($taskKey)){throw "Unconfirmed/overlapping source $taskKey"}
 $taskRow=[ordered]@{source_key="npc70.$($taskRows.Count)";speaker=$taskFields[0];source=$taskKey;translation=($taskPrefix+$taskFields[2]);evidence='observed_public_npc_line'}
 if($taskFields.Count -eq 4){$taskRow.style_anchors=@($taskFields[3]|ConvertFrom-Json -AsHashtable)}
 $taskRows+=,$taskRow
}
if($taskRows.Count -ne 22){throw 'Unexpected exact batch size'}
[IO.File]::WriteAllText("$taskData/npc-continuations70.json",(@{format_version=1;language='ru';scope='22 exact public NPC lines independently retained from historical user display logs;not all scripts,current existence,channels or interactions. Loaded into existing story map at startup.';records=$taskRows}|ConvertTo-Json -Depth 12)+"`n",[Text.UTF8Encoding]::new($false))
$taskEvidence=@{date='2026-10-15';scope='143 previously whitelisted public NPC English source candidates from2026-10-09..14. No new private/full log export. Legacy channel/click metadata not proven. All source text preserved independently of reviewed targets.';source_fixture='NPC_GAPS_OBSERVED_0.15.60.json';source_fixture_sha256=(Get-FileHash "$taskProject/translation-source/NPC_GAPS_OBSERVED_0.15.60.json").Hash;records=$taskCaptured.records;maxwell_published=@(@{source='[NPC] Maxwell: [Player], that''s an excellent power!';target='[NPC] Maxwell: [Player], отличная сила!'},@{source='[NPC] Maxwell: A wise choice, [Player]!';target='[NPC] Maxwell: Мудрый выбор, [Player]!'});maxwell_source_url='https://hypixel-skyblock.fandom.com/wiki/Maxwell';maxwell_named_replies_observed_locally=$false;all_hypixel_npcs_translated=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_OBSERVED_0.15.70.json",($taskEvidence|ConvertTo-Json -Depth 12)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "NPC70:22 observed exact sources;$($taskCaptured.records.Count) independent replay candidates;2 published Maxwell named shapes"
