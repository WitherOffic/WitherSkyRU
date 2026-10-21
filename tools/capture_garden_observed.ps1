# Mechanical, read-only log extraction; never launches the game or writes installed files.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskLog='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs/latest.log'
$taskOutput=Join-Path $taskProject 'translation-source/GARDEN_0.15.34_OBSERVED_SAMPLES.json'
if(Test-Path -LiteralPath $taskOutput){throw 'Frozen observation snapshot already exists'}
$taskSamples=[Collections.Generic.List[object]]::new()
$taskSeen=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskStream=[IO.FileStream]::new($taskLog,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
$taskReader=[IO.StreamReader]::new($taskStream,[Text.Encoding]::UTF8)
try{$taskLines=$taskReader.ReadToEnd().Split("`n")}finally{$taskReader.Dispose();$taskStream.Dispose()}
foreach($taskLine in $taskLines){
    if($taskLine -notmatch '\[CHAT\] (.+)$'){continue}
    $taskSample=$Matches[1].TrimEnd("`r")
    $taskSource=$taskSample -replace '(?i)§[0-9a-fk-or]','' -replace '^\[\d{2}:\d{2}:\d{2}\] ',''
    if($taskSource -notmatch '^\[NPC\] (Sam|Carpenter|Pesthunter Pamela|Pesthunter Phillip|Jacob|Liam|Duke|Lazy Miner|Jack): '){continue}
    # Identity/opaque greeting is tested separately, not saved with the player's name.
    if($taskSource -match '^\[NPC\] Pesthunter Phillip: Howdy, '){continue}
    if($taskSeen.Add($taskSource)){$taskSamples.Add([ordered]@{source=$taskSource;sample=$taskSample})}
}
$taskRecord=[ordered]@{status='LOCAL_DISPLAY_LOG_ONLY_CHANNEL_UNVERIFIED';source_log=$taskLog;samples=$taskSamples}
[IO.File]::WriteAllText($taskOutput,($taskRecord|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "Frozen Garden legacy samples: $($taskSamples.Count)"
