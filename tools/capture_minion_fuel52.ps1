# Bounded public repository capture, no game/installed-file writes.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/MINION_FUEL_0.15.52.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskHead=(Invoke-RestMethod -Uri 'https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/git/ref/heads/master').object.sha
if($taskHead -notmatch '^[a-f0-9]{40}$'){throw 'Invalid primary revision'}
$taskRecords=@(foreach($taskId in @('HAMSTER_WHEEL','FOUL_FLESH','CHARCOAL','COAL','COAL_BLOCK','ENCHANTED_CHARCOAL','ENCHANTED_COAL','ENCHANTED_BREAD')){
    $taskUrl="https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/$taskHead/items/$taskId.json"
    $taskRaw=(Invoke-WebRequest -Uri $taskUrl).Content
    $taskItem=$taskRaw|ConvertFrom-Json
    if($taskItem.internalname -ne $taskId -or !$taskItem.lore){throw "Unexpected primary item: $taskId"}
    [ordered]@{id=$taskId;source=$taskUrl;source_sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskRaw)));title_legacy=$taskItem.displayname;lore_legacy=@($taskItem.lore);crafttext=$taskItem.crafttext}
})
$taskResult=[ordered]@{date='2026-10-14';revision=$taskHead;scope='Eight current repository fuel item cards, not a live component capture/all minion fuels';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED: $($taskRecords.Count) current primary fuel cards at $taskHead"
