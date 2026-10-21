# Bounded public primary capture. No game or installed-file writes.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/SACK_PRIMARY_0.15.55.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskRevision=(Invoke-RestMethod -Uri 'https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/git/ref/heads/master').object.sha
if($taskRevision -notmatch '^[a-f0-9]{40}$'){throw 'Invalid revision'}
$taskTree=(Invoke-RestMethod -Uri "https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/git/trees/${taskRevision}?recursive=1").tree
$taskPaths=@($taskTree | Where-Object { $_.type -eq 'blob' -and $_.path -match '^items/[^/]*SACK[^/]*\.json$' })
$taskRecords=@(foreach($taskEntry in $taskPaths){
    $taskUrl="https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/$taskRevision/$($taskEntry.path)"
    $taskRaw=(Invoke-WebRequest -Uri $taskUrl).Content
    $taskItem=$taskRaw|ConvertFrom-Json
    if(!$taskItem.internalname -or !$taskItem.lore){throw 'Malformed sack capture'}
    [ordered]@{id=$taskItem.internalname;source=$taskUrl;source_sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskRaw)));title_legacy=$taskItem.displayname;lore_legacy=@($taskItem.lore)}
})
$taskResult=[ordered]@{date='2026-10-14';revision=$taskRevision;scope='All repository item paths containing SACK; includes unrelated Sack of Sacks upgrades; not all live UI';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED: $($taskRecords.Count) current primary SACK-path cards at $taskRevision"
