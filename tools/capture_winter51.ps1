# Read-only, bounded evidence capture. Never changes the installed game or logs.
param([string]$LogPath='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs/latest.log')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskLog=(Resolve-Path -LiteralPath $LogPath).Path
$taskDestination=Join-Path $taskProject 'translation-source/WINTER_OBSERVED_0.15.51.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists; do not overwrite'}
$taskLine=0
$taskRecords=@(foreach($taskText in (Get-Content -LiteralPath $taskLog -Encoding utf8)){
    $taskLine++
    if($taskText -match '\[CHAT\] (\[02:(?:38|39):[0-9]{2}\] \[NPC\] (?:St\. Jerry|Frosty): .+)$'){
        [ordered]@{line=$taskLine;source_legacy=$Matches[1]}
    }
})
if($taskRecords.Count -ne 15){throw "Expected the frozen 15 observed messages, found $($taskRecords.Count); inspect changed log before capturing"}
$taskCapturedText=($taskRecords.source_legacy -join "`n")
$taskHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskCapturedText)))
$taskResult=[ordered]@{date='2026-10-14';source='installed latest.log, bounded 02:38-02:39 NPC window';captured_text_sha256=$taskHash;scope='Observed text/legacy colors only; chat channels, complete component trees and actions require synthetic verification; not all winter branches';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED: $($taskRecords.Count) observed winter messages; installed files unchanged"
