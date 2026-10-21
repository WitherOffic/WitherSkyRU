$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskArchive='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs/2026-10-13-3.log.gz'
$taskTarget=Join-Path $taskProject 'translation-source/SAFETY_QUIZ_OBSERVED_0.15.66.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve captured evidence'}
$taskMessages=@();$taskLineNumber=0
$taskFile=[IO.File]::OpenRead($taskArchive)
$taskGz=[IO.Compression.GZipStream]::new($taskFile,[IO.Compression.CompressionMode]::Decompress)
$taskReader=[IO.StreamReader]::new($taskGz)
try{while(($taskLine=$taskReader.ReadLine()) -ne $null){
    $taskLineNumber++
    if($taskLine -notmatch '\[CHAT\] (\[(04:56:23|04:57:00|04:57:30|04:57:31)\] .*)$' -and $taskLine -notmatch '\[CHAT\] (\[(04:52:15|04:52:16|04:52:22)\] (?:RING\.\.\.|\[NPC\] Charlotte:|\[NPC\] Brigette: .*Thanks, Salad!).*)$'){continue}
    $taskMessage=$Matches[1]
    if($taskMessage.Trim() -match '^\[[0-9:]+\]$'){continue}
    $taskMessages+=@{line=$taskLineNumber;source_legacy=$taskMessage;normalized_newlines=$taskMessage.Replace('\n',"`n")}
}}finally{$taskReader.Dispose();$taskFile.Dispose()}
if($taskMessages.Count -ne 18){throw "Unexpected scoped capture size: $($taskMessages.Count)"}
$taskOutput=@{scope='Observed3 question headings/7 question rows/3 complete answer bundles/2 NPC lines/3 ring notices. Log does not prove channel/clicks/styles lost by logging. No personal form input retained.';captured='2026-10-15';archive='2026-10-13-3.log.gz';archive_sha256=(Get-FileHash -LiteralPath $taskArchive).Hash;user_screenshots=@('codex-clipboard-c52a92b9-2b6e-49ca-84e8-6f63285caf71.png','codex-clipboard-612fff4b-d589-4d9d-8b2f-b30e0882071d.png');messages=$taskMessages}
[IO.File]::WriteAllText($taskTarget,($taskOutput|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
