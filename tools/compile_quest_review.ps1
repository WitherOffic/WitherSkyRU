# Mechanical conversion of authored TSV; translations/anchors are edited with apply_patch.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskSnapshot=Get-Content -Raw -LiteralPath (Join-Path $taskProject 'translation-source/QUEST_0.15.35_OBSERVED_SAMPLES.json')|ConvertFrom-Json
$taskSourceMap=@{}
foreach($taskSample in $taskSnapshot.samples){$taskSourceMap[$taskSample.source]=$taskSample}
$taskRows=[Collections.Generic.List[object]]::new()
$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskLine in [IO.File]::ReadAllLines((Join-Path $taskProject 'translation-source/QUEST_0.15.35_REVIEWED.tsv'))){
    if(!$taskLine.Trim() -or $taskLine.StartsWith('#')){continue}
    $taskCells=$taskLine.Split("`t")
    if($taskCells.Count -ne 3 -or !$taskSourceMap.ContainsKey($taskCells[0]) -or !$taskSeen.Add($taskCells[0])){throw 'Invalid, duplicate or unobserved authored quest row'}
    $taskObserved=$taskSourceMap[$taskCells[0]]
    $taskPrefix="[NPC] $($taskObserved.speaker): "
    $taskAnchors=@(($taskCells[2]|ConvertFrom-Json).PSObject.Properties|ForEach-Object{[ordered]@{source=$_.Name;target=[string]$_.Value}})
    $taskRow=[ordered]@{source_key="quest.pass$($taskObserved.pass).$($taskRows.Count+1)";pass=$taskObserved.pass;speaker=$taskObserved.speaker;source=$taskCells[0];translation=$taskPrefix+$taskCells[1]}
    if($taskAnchors.Count){$taskRow.style_anchors=$taskAnchors}
    $taskRows.Add($taskRow)
}
$taskResult=[ordered]@{format_version=1;language='ru';status='OBSERVED_QUEST_DIALOGUES_OFFLINE_NOT_LIVE_TESTED';scope='Selected observed quest and neighbouring NPC sentences from 25 local logs. No full-server quest coverage claim.';records=$taskRows}
$taskOutput=Join-Path $taskProject 'src/main/resources/data/witherskyru/quest-dialogues.json'
[IO.File]::WriteAllText($taskOutput,($taskResult|ConvertTo-Json -Depth 9)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "Compiled reviewed quest records: $($taskRows.Count)"
