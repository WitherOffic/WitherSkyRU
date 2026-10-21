$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskData="$taskProject/src/main/resources/data/witherskyru"
$taskFresh=Get-Content "$taskProject/translation-source/NPC_SOURCES_0.15.76.json" -Raw|ConvertFrom-Json
$taskEvidence=@{pages=@($taskFresh.pages)}
$taskKnown=[Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
foreach($taskFile in (Get-ChildItem $taskData -Filter '*.json'|Where-Object Name -ne 'npc-expansion76.json')){foreach($taskRow in (Get-Content $taskFile.FullName -Raw|ConvertFrom-Json).records){if($taskRow.source -and !$taskKnown.ContainsKey($taskRow.source)){$taskKnown[$taskRow.source]=$taskRow}}}
$taskRuntimeRetained=Get-Content "$taskProject/translation-source/NPC_RETAINED_RUNTIME_0.15.74.json" -Raw|ConvertFrom-Json
foreach($taskRow in $taskRuntimeRetained.records){$taskKnown[$taskRow.source]=@{translation=$taskRow.existing_target}}
$taskRows=@();$taskRetained=@();$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskLine in [IO.File]::ReadAllLines("$taskProject/translation-source/NPC_0.15.76_REVIEWED.tsv")){
 if(!$taskLine -or $taskLine.StartsWith('#')){continue};$taskFields=$taskLine.Split('|');if($taskFields.Count -notin @(3,4)){throw 'Invalid reviewed line'}
 $taskSpeaker=$taskFields[0];$taskPage=@($taskEvidence.pages|Where-Object name -ceq $taskSpeaker)
 if($taskPage.Count -ne 1 -or $taskPage[0].tag -notin @('NPC','SECURITY')){throw 'Missing closed speaker/tag source'}
 $taskTag=$taskPage[0].tag;$taskPrefix="[$taskTag] ${taskSpeaker}: ";$taskSource=$taskPrefix+$taskFields[1];$taskTarget=$taskPrefix+$taskFields[2]
 if(!$taskSeen.Add($taskSource)){throw "Duplicate reviewed source:$taskSource"}
 $taskPage=@($taskEvidence.pages|Where-Object name -ceq $taskSpeaker);if($taskPage.Count -ne 1 -or !$taskPage[0].section.Contains($taskSource)){throw "Independent published source missing:$taskSource"}
 if($taskSource -match '\[[Pp][Ll][Aa][Yy][Ee][Rr]\]|<[^>]+>|#|\[pet\]|\[Bundle Name\]|\[potion effect\]'){throw 'Placeholder is not an exact server message'}
 if($taskKnown.ContainsKey($taskSource)){$taskRetained+=@{source=$taskSource;existing_target=$taskKnown[$taskSource].translation;reviewed_target=$taskTarget};continue}
 $taskRow=[ordered]@{source_key="npc76.$($taskRows.Count)";speaker=$taskSpeaker;tag=$taskTag;source=$taskSource;translation=$taskTarget;evidence='published_community_wiki_exact_not_live_packet';source_url=$taskPage[0].url}
 if($taskFields.Count -eq 4){$taskAnchors=$taskFields[3]|ConvertFrom-Json -AsHashtable;if($taskFields[3].StartsWith('[')){$taskRow.style_anchors=@($taskAnchors)}else{$taskRow.style_anchors=@($taskAnchors.Keys|Sort-Object|ForEach-Object {@{source=$_;target=$taskAnchors[$_]}})}}
 $taskRows+=,$taskRow
}
$taskResource=[ordered]@{format_version=1;language='ru';scope='Finite source-backed exact NPC additions loaded into the existing story map once at startup. Not every NPC,all branches,dynamic greetings or observed live colour layouts.';records=$taskRows}
[IO.File]::WriteAllText("$taskData/npc-expansion76.json",($taskResource|ConvertTo-Json -Depth 12)+"`n",[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_RETAINED_0.15.76.json",(@{scope='Existing exact source wins;no old target overwritten or claimed as new';records=$taskRetained}|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "NPC76 new=$($taskRows.Count);speakers=$(@($taskRows.speaker|Sort-Object -Unique).Count);retained=$($taskRetained.Count)"
$taskRows|Group-Object {$_.speaker}|Sort-Object Name|Select-Object Name,Count|ConvertTo-Json
