# Mechanical read-only capture of the next non-empty CHAT line after NEW OBJECTIVE.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskOutput=Join-Path $taskProject 'translation-source/QUEST_0.15.35_OBJECTIVES.json'
if(Test-Path -LiteralPath $taskOutput){throw 'Frozen objective snapshot already exists'}
$taskRoot='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs'
$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskRows=[Collections.Generic.List[object]]::new()
foreach($taskFile in (Get-ChildItem -LiteralPath $taskRoot -File|Where-Object{$_.Name -match '\.log(?:\.gz)?$'}|Sort-Object Name)){
    $taskStream=[IO.File]::Open($taskFile.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
    $taskInput=$taskStream
    if($taskFile.Name.EndsWith('.gz')){$taskInput=[IO.Compression.GZipStream]::new($taskStream,[IO.Compression.CompressionMode]::Decompress)}
    $taskReader=[IO.StreamReader]::new($taskInput,[Text.Encoding]::UTF8)
    $taskPending=$false
    try{while(($taskLine=$taskReader.ReadLine()) -ne $null){
        if($taskLine -notmatch '\[CHAT\] (.+)$'){continue}
        $taskSample=$Matches[1]
        $taskSource=($taskSample -replace '(?i)§[0-9a-fk-or]','' -replace '^\[\d{2}:\d{2}:\d{2}\] ','').Trim()
        if($taskSource -eq 'NEW OBJECTIVE'){$taskPending=$true;continue}
        if($taskPending -and $taskSource){
            if($taskSeen.Add($taskSource)){$taskRows.Add([ordered]@{source=$taskSource;sample=$taskSample;origin=$taskFile.Name})}
            $taskPending=$false
        }
    }}finally{$taskReader.Dispose();$taskStream.Dispose()}
}
$taskSnapshot=[ordered]@{status='LOCAL_DISPLAY_LOG_ONLY_CHANNEL_UNVERIFIED';scope='35 observed objectives, not all server quests';samples=$taskRows}
[IO.File]::WriteAllText($taskOutput,($taskSnapshot|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "Frozen objective samples: $($taskRows.Count)"
