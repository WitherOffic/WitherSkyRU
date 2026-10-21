# Read-only NPC-only extraction from user logs; never emits private player chat.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskLogs='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs'
$taskDestination=Join-Path $taskProject 'translation-source/NPC_GAPS_OBSERVED_0.15.60.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists; preserve it'}
$taskKnown=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskResource in @('npc-dialogues','garden-dialogues','quest-dialogues','story-dialogues','winter-dialogues','dungeon-dialogues')){
    $taskData=Get-Content -LiteralPath (Join-Path $taskProject "src/main/resources/data/witherskyru/$taskResource.json") -Raw|ConvertFrom-Json
    foreach($taskRecord in $taskData.records){[void]$taskKnown.Add($taskRecord.source)}
}
$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskRows=[Collections.Generic.List[object]]::new();$taskFiles=0
foreach($taskFile in (Get-ChildItem -LiteralPath $taskLogs -File|Where-Object {$_.Name -match '\.log(?:\.gz)?$'}|Sort-Object Name)){
    $taskStream=[IO.File]::Open($taskFile.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite);$taskInput=$taskStream
    if($taskFile.Name.EndsWith('.gz')){$taskInput=[IO.Compression.GZipStream]::new($taskStream,[IO.Compression.CompressionMode]::Decompress)}
    $taskReader=[IO.StreamReader]::new($taskInput,[Text.Encoding]::UTF8);$taskLine=0
    try{while(($taskText=$taskReader.ReadLine()) -ne $null){
        $taskLine++;if($taskText -notmatch '\[CHAT\] (.+)$'){continue};$taskSample=$Matches[1]
        $taskPlain=$taskSample -replace '(?i)§[0-9a-fk-or]','' -replace '^\[\d{2}:\d{2}:\d{2}\] ',''
        if($taskPlain -notmatch '^\[NPC\] ([^:]{1,60}): (.+)$'){continue};$taskSpeaker=$Matches[1];$taskBody=$Matches[2]
        # Dynamic personal greetings/opaque components are not treated as literal scripts.
        if($taskBody -match 'Wither_101|Howdy, |\[.* head\]|\uFFFC' -or $taskBody -notmatch '[A-Za-z]' -or $taskBody -match '[А-Яа-яЁё]'){continue}
        $taskSource=$taskPlain -replace ' \((?:x|×)[0-9]{1,6}\)$',''
        if($taskKnown.Contains($taskSource) -or !$taskSeen.Add($taskSource)){continue}
        $taskRows.Add([ordered]@{speaker=$taskSpeaker;source=$taskSource;sample=$taskSample;origin=$taskFile.Name;line=$taskLine})
    };$taskFiles++}finally{$taskReader.Dispose();$taskStream.Dispose()}
}
$taskRecords=@($taskRows|Sort-Object {$_['speaker']},{$_['source']})
$taskReport=[ordered]@{date='2026-10-14';status='LOCAL_DISPLAY_LOG_CHANNEL_UNVERIFIED';scope='Distinct NPC-labelled English display lines not in exact resources; dynamic handlers may already support them. No player chat exported. Not complete scripts/server/channel capture.';files_read=$taskFiles;known_exact_union_count=$taskKnown.Count;records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskReport|ConvertTo-Json -Depth 10)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED:$($taskRecords.Count) NPC-only exact-resource candidates;logs=$taskFiles;known union=$($taskKnown.Count)"
$taskRecords|Group-Object {$_['speaker']}|Sort-Object Count -Descending|Select-Object -First 20|ForEach-Object {Write-Output "$($_.Name):$($_.Count)"}
