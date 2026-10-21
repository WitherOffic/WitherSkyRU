# Read public canonical repository files only. Never touches player items or installed mods.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskRepo='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/config/notenoughupdates/repo/items'
$taskRecords=@();$taskIds=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskFile in Get-ChildItem -LiteralPath $taskRepo -File -Filter '*;*.json'|Sort-Object Name){
    if($taskFile.Name -notmatch '^[A-Z0-9_]+;[1-9][0-9]{0,2}\.json$'){continue}
    $taskItem=Get-Content -LiteralPath $taskFile.FullName -Raw|ConvertFrom-Json
    if($taskItem.itemid -ne 'minecraft:enchanted_book' -or [regex]::Replace($taskItem.displayname,'(?i)§[0-9a-fk-or]','').Trim() -ne 'Enchanted Book'){continue}
    if(!$taskIds.Add($taskItem.internalname)){throw ('Duplicate book ID '+$taskItem.internalname)}
    if($taskItem.internalname -ne [IO.Path]::GetFileNameWithoutExtension($taskFile.Name)){throw 'Book ID/file mismatch'}
    $taskEffect=@();for($taskN=3;$taskN -lt $taskItem.lore.Count -and $taskItem.lore[$taskN] -ne '';$taskN++){$taskEffect+=$taskItem.lore[$taskN]}
    if($taskEffect.Count -eq 0){throw ('No complete effect paragraph '+$taskItem.internalname)}
    $taskRecords+=@{id=$taskItem.internalname;title_legacy=$taskItem.lore[2];title_plain=[regex]::Replace($taskItem.lore[2],'(?i)§[0-9a-fk-or]','');effect_legacy=$taskEffect;effect_plain=@($taskEffect|ForEach-Object {[regex]::Replace($_,'(?i)§[0-9a-fk-or]','')});full_lore_legacy=$taskItem.lore;source_sha256=(Get-FileHash -LiteralPath $taskFile.FullName).Hash}
}
$taskFamilies=@($taskRecords|ForEach-Object {$_.id.Split(';')[0]}|Sort-Object -Unique)
$taskOld=Get-Content -LiteralPath (Join-Path $taskProject 'translation-source/item-context.json') -Raw|ConvertFrom-Json
$taskFrozen=@{};foreach($taskItem in $taskOld|Where-Object {$_.name -eq 'Enchanted Book' -and $_.id -match '^[A-Z0-9_]+;[1-9][0-9]{0,2}$'}){
    $taskEffect=@();for($taskN=3;$taskN -lt $taskItem.lore.Count -and $taskItem.lore[$taskN] -ne '';$taskN++){$taskEffect+=$taskItem.lore[$taskN]};$taskFrozen[$taskItem.id]=$taskEffect -join "`n"
}
$taskChanged=0;$taskAdded=0
foreach($taskRecord in $taskRecords){$taskKnown=$taskFrozen.ContainsKey($taskRecord.id);if(!$taskKnown){$taskAdded++}elseif(($taskRecord.effect_plain -join "`n") -ne $taskFrozen[$taskRecord.id]){$taskChanged++}}
$taskOutput=Join-Path $taskProject 'translation-source/ENCHANTMENT_CURRENT_REPO_2026-10-13.json'
$taskData=@{scope='Current local public NEU repository snapshot, not all live server or player items; frozen corpus retained separately';date='2026-10-13';book_level_cards=$taskRecords.Count;families=$taskFamilies.Count;new_ids_vs_frozen=$taskAdded;changed_effects_vs_frozen=$taskChanged;records=$taskRecords}|ConvertTo-Json -Depth 6
[IO.File]::WriteAllText($taskOutput,$taskData+"`n",[Text.UTF8Encoding]::new($false))
Write-Output ('CURRENT REPO: cards='+$taskRecords.Count+' families='+$taskFamilies.Count+' newIDs='+$taskAdded+' changedEffects='+$taskChanged+'; not all live server')
