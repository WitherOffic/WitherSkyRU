# Mechanical observation extraction. Reads historical logs without launching the game.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskRoot='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs'
$taskOutput=Join-Path $taskProject 'translation-source/QUEST_0.15.35_OBSERVED_SAMPLES.json'
if(Test-Path -LiteralPath $taskOutput){throw 'Frozen quest snapshot already exists'}
$taskKnown=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskResource in @('npc-dialogues.json','garden-dialogues.json','dungeon-dialogues.json')){
    (Get-Content -Raw -LiteralPath (Join-Path $taskProject "src/main/resources/data/witherskyru/$taskResource")|ConvertFrom-Json).records|ForEach-Object{[void]$taskKnown.Add($_.source)}
}
$taskGroups=@{
    'Sam'=1
    'Captain Baha'=2; 'Researcher Gilbert'=2; 'Researcher Ribery'=2; 'Researcher Lily'=2; 'Marigold'=2; 'Queen Mismyla'=2; 'Arthur'=2
    'Beth'=3; 'Moby'=3
    'Emissary Wilson'=4; 'Gwendolyn'=4; 'Iron Forger'=4; 'Jotraeline Greatforge'=4; 'Lapis Miner'=4
    'Carpenter'=5; 'Jamie'=5; 'Jerry'=5; 'Farmhand'=5; 'Rusty'=5; 'Weaponsmith'=5
}
$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskRows=[Collections.Generic.List[object]]::new()
$taskFiles=0
foreach($taskFile in (Get-ChildItem -LiteralPath $taskRoot -File|Where-Object{$_.Name -match '\.log(?:\.gz)?$'}|Sort-Object Name)){
    $taskStream=[IO.File]::Open($taskFile.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
    $taskInput=$taskStream
    if($taskFile.Name.EndsWith('.gz')){$taskInput=[IO.Compression.GZipStream]::new($taskStream,[IO.Compression.CompressionMode]::Decompress)}
    $taskReader=[IO.StreamReader]::new($taskInput,[Text.Encoding]::UTF8)
    try{while(($taskLine=$taskReader.ReadLine()) -ne $null){
        if($taskLine -notmatch '\[CHAT\] (.+)$'){continue}
        $taskSample=$Matches[1]
        $taskSource=$taskSample -replace '(?i)§[0-9a-fk-or]','' -replace '^\[\d{2}:\d{2}:\d{2}\] ',''
        if($taskSource -notmatch '^\[NPC\] ([^:]{1,60}): '){continue}
        $taskSpeaker=$Matches[1]
        if(!$taskGroups.ContainsKey($taskSpeaker)){continue}
        # No player-specific greetings or opaque head labels in the frozen corpus.
        if($taskSource -match 'Wither_101|Howdy, |\[.* head\]|\uFFFC'){continue}
        $taskBase=$taskSource -replace ' \((?:x|×)[0-9]{1,6}\)$',''
        if($taskKnown.Contains($taskBase) -or !$taskSeen.Add($taskBase)){continue}
        $taskRows.Add([ordered]@{pass=$taskGroups[$taskSpeaker];speaker=$taskSpeaker;source=$taskBase;sample=$taskSample;origin=$taskFile.Name})
    };$taskFiles++}finally{$taskReader.Dispose();$taskStream.Dispose()}
}
$taskSnapshot=[ordered]@{status='LOCAL_DISPLAY_LOG_ONLY_CHANNEL_UNVERIFIED';files_read=$taskFiles;scope='Five selected observed NPC/quest sections, not all quests or all server NPCs';samples=@($taskRows|Sort-Object pass,speaker,source)}
[IO.File]::WriteAllText($taskOutput,($taskSnapshot|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "Frozen selected quest sources: $($taskRows.Count), logs: $taskFiles"
