# Read-only source capture; never changes installed cache or launches the game.
param([string]$CacheRoot='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/catharsis-repo-cache')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/BAG_GUI_LABELS_0.15.56.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Preserve historical capture'}
$taskRecords=@(foreach($taskRelative in @('guis/generic/generic.json','guis/your_bags/your_bags.json','guis/bazaar/bazaar.json')){
    $taskSource=Join-Path $CacheRoot $taskRelative
    if(!(Test-Path -LiteralPath $taskSource)){throw "Missing source $taskRelative"}
    [ordered]@{source=$taskRelative;sha256=(Get-FileHash -LiteralPath $taskSource).Hash;definition=(Get-Content -LiteralPath $taskSource -Raw|ConvertFrom-Json)}
})
[IO.File]::WriteAllText($taskDestination,([ordered]@{date='2026-10-14';scope='Three actual cache definitions, exact visible title/name/lore conditions only; not all tooltips, dynamic titles or live server';records=$taskRecords}|ConvertTo-Json -Depth 60)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output 'CAPTURED:3 actual bag/generic/bazaar cache definitions'
