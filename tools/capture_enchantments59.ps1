# Read-only pinned capture; never replaces frozen corpus or prior evidence.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/ENCHANTMENT_PRIMARY_0.15.59.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskRevision=(Invoke-RestMethod -Uri 'https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/git/ref/heads/master').object.sha
if($taskRevision -notmatch '^[a-f0-9]{40}$'){throw 'Invalid revision'}
$taskRecords=@(foreach($taskFamily in @('THUNDERLORD','CLEAVE','IMPALING','VENOMOUS')){
    $taskMax=switch($taskFamily){'CLEAVE'{6};'IMPALING'{5};default{7}}
    foreach($taskLevel in 1..$taskMax){
        $taskId="$taskFamily;$taskLevel"
        $taskUrl="https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/$taskRevision/items/$([Uri]::EscapeDataString($taskId)).json"
        $taskRaw=(Invoke-WebRequest -Uri $taskUrl).Content
        $taskItem=$taskRaw|ConvertFrom-Json
        if($taskItem.internalname -ne $taskId -or $taskItem.lore.Count -lt 5 -or ($taskItem.lore[0] -replace '(?i)§[0-9a-fk-or]','') -ne 'Combinable in Anvil'){throw "Unknown book shape $taskId"}
        $taskEffects=@(for($taskAt=3;$taskAt -lt $taskItem.lore.Count -and $taskItem.lore[$taskAt] -ne '';$taskAt++){$taskItem.lore[$taskAt]})
        [ordered]@{id=$taskId;source=$taskUrl;source_sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskRaw)));title_plain=($taskItem.lore[2] -replace '(?i)§[0-9a-fk-or]','');effect_legacy=$taskEffects;effect_plain=@($taskEffects|ForEach-Object {$_ -replace '(?i)§[0-9a-fk-or]',''});full_lore_legacy=@($taskItem.lore)}
    }
})
[IO.File]::WriteAllText($taskDestination,([ordered]@{date='2026-10-14';revision=$taskRevision;scope='25 pinned books/four families;repository text,not live Components or complete server';records=$taskRecords}|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED:$($taskRecords.Count) primary books at $taskRevision"
