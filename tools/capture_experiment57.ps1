# Pinned read-only public capture; source code is diagnostic data, never executed.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/EXPERIMENT_PRIMARY_0.15.57.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskRecords=[Collections.Generic.List[object]]::new()
foreach($taskRepo in @('NotEnoughUpdates/NotEnoughUpdates-REPO','hannibal002/SkyHanni')){
    $taskInfo=Invoke-RestMethod -Uri "https://api.github.com/repos/$taskRepo"
    $taskHead=(Invoke-RestMethod -Uri "https://api.github.com/repos/$taskRepo/git/ref/heads/$($taskInfo.default_branch)").object.sha
    if($taskHead -notmatch '^[a-f0-9]{40}$'){throw 'Invalid revision'}
    $taskPaths=if($taskRepo -like 'NotEnough*'){@('items/EXPERIMENTATION_TABLE.json','items/METAPHYSICAL_SERUM.json')}else{@('src/main/java/at/hannibal2/skyhanni/api/ExperimentationTableApi.kt','src/main/java/at/hannibal2/skyhanni/api/ExperimentationSuperpairApi.kt','src/main/java/at/hannibal2/skyhanni/features/chat/CompactExperimentRewards.kt','src/main/java/at/hannibal2/skyhanni/features/inventory/experimentationtable/ExperimentationXPOverlay.kt','src/main/java/at/hannibal2/skyhanni/features/inventory/experimentationtable/ExperimentsAddonsHelper.kt')}
    foreach($taskPath in $taskPaths){
        $taskUrl="https://raw.githubusercontent.com/$taskRepo/$taskHead/$taskPath"
        $taskRaw=(Invoke-WebRequest -Uri $taskUrl).Content
        $taskRecord=[ordered]@{repository=$taskRepo;revision=$taskHead;path=$taskPath;source=$taskUrl;source_sha256=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskRaw)))}
        if($taskPath.EndsWith('.json')){$taskItem=$taskRaw|ConvertFrom-Json; $taskRecord.id=$taskItem.internalname;$taskRecord.title_legacy=$taskItem.displayname;$taskRecord.lore_legacy=@($taskItem.lore)}else{$taskRecord.source_code=$taskRaw}
        $taskRecords.Add($taskRecord)
    }
}
[IO.File]::WriteAllText($taskDestination,([ordered]@{date='2026-10-14';scope='Two current item cards and five first-party mod implementation files; item lore vs matcher evidence are distinct, not full live GUI';records=$taskRecords}|ConvertTo-Json -Depth 30)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED:$($taskRecords.Count) pinned records"
