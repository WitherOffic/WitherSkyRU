# Mechanical read-only capture from the actual installed Catharsis cache.
# No game/launcher/entrypoint/network/installed-file writes.
param([string]$CacheRoot='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/catharsis-repo-cache')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskCache=(Resolve-Path -LiteralPath $CacheRoot).Path
$taskPaths=@('guis/experimentation_table/experimentation_table.json','guis/experimentation_table/chronomatron.json','guis/experimentation_table/ultrasequencer.json','guis/experimentation_table/superpairs.json','guis/skills/combat/slayer/slayer.json')
$taskRecords=foreach($taskRelative in $taskPaths){
    $taskFile=Join-Path $taskCache $taskRelative
    if(!(Test-Path -LiteralPath $taskFile -PathType Leaf)){throw "Missing source file: $taskFile"}
    [ordered]@{source=$taskRelative;sha256=(Get-FileHash -LiteralPath $taskFile -Algorithm SHA256).Hash;definition=(Get-Content -LiteralPath $taskFile -Raw|ConvertFrom-Json)}
}
$taskDestination=Join-Path $taskProject 'translation-source/GUI_LABELS_0.15.51.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Capture exists; do not overwrite historical evidence'}
$taskResult=[ordered]@{date='2026-10-14';scope='Five actual cache definitions: title/name/lore match conditions only, not complete tooltips/all server windows';records=@($taskRecords)}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 60)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "PROJECT: $taskProject"
Write-Output "CACHE: $taskCache"
Write-Output "CAPTURED: $(@($taskRecords).Count) GUI definitions; source files unchanged"
