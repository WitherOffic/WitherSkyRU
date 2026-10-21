# Bounded read-only capture of the user's Slayer screenshot window.
param([string]$LogPath='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/logs/latest.log')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/SLAYER_OBSERVED_0.15.53.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskLine=0
$taskRecords=@(foreach($taskText in (Get-Content -LiteralPath $LogPath -Encoding utf8)){
    $taskLine++
    if($taskText -match '\[CHAT\] (\[03:(?:38:58|39:(?:0[1-8]|10))\] (?<body>.+))$'){
        $taskMessage=$Matches[1];$taskBody=$Matches.body.Trim()
        if($taskBody -match '^(?:SLAYER |NICE! SLAYER|Your Slayer |» Talk to Maddox|Zombie Slayer |✆ RING|\[NPC\] Maddox: ✆ Someone answers!|\[SkyHanni\] (?:You have gained|Slayer quest took)|\[Skyblocker\] Slayer убит за|§5§l» §7Slay)'){
            [ordered]@{line=$taskLine;source_legacy=$taskMessage}
        }
    }
})
if($taskRecords.Count -ne 16){throw "Expected16 bounded screenshot records; found $($taskRecords.Count). Inspect changed source before capture"}
$taskHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(($taskRecords.source_legacy -join "`n"))))
$taskResult=[ordered]@{date='2026-10-14';screenshot='codex-clipboard-c8cacd23-4464-4d33-8e40-6d6cbe933a0a.png';source='installed latest.log, bounded03:38:58-03:39:10 selected gameplay messages only';captured_text_sha256=$taskHash;scope='Observed text, padding and available legacy codes only; full component styles/actions/channels reconstructed in tests; not all Slayer dialogue';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED: $($taskRecords.Count) Slayer screenshot records; installed files unchanged"
