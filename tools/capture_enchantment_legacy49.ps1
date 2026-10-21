# Mechanical capture of public repository originals; no user chat or item instances.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskRepo='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/config/notenoughupdates/repo/items'
$taskCorpus=Get-Content -LiteralPath (Join-Path $taskProject 'translation-source/item-context.json') -Raw|ConvertFrom-Json
$taskFamilies=@('BUG_BLENDER','CHAMPION','SMITE','BANE_OF_ARTHROPODS','LETHALITY')
$taskRecords=@()
foreach($taskItem in $taskCorpus|Where-Object {$_.name -eq 'Enchanted Book' -and $_.id.Split(';')[0] -in $taskFamilies}){
    $taskFile=Join-Path $taskRepo ($taskItem.id+'.json')
    $taskOriginal=Get-Content -LiteralPath $taskFile -Raw|ConvertFrom-Json
    if($taskOriginal.internalname -ne $taskItem.id){throw ('Original ID mismatch '+$taskItem.id)}
    $taskRows=@();for($taskN=3;$taskN -lt $taskOriginal.lore.Count -and $taskOriginal.lore[$taskN] -ne '';$taskN++){$taskRows+=$taskOriginal.lore[$taskN]}
    $taskPlain=$taskRows|ForEach-Object {[regex]::Replace($_,'(?i)§[0-9a-fk-or]','')}
    $taskExpected=@();for($taskN=3;$taskN -lt $taskItem.lore.Count -and $taskItem.lore[$taskN] -ne '';$taskN++){$taskExpected+=$taskItem.lore[$taskN]}
    $taskChanged=($taskPlain -join "`n") -ne ($taskExpected -join "`n")
    if($taskChanged){
        $taskActualSentence=($taskPlain -join ' ').Replace('','❈')
        $taskFrozenSentence=$taskExpected -join ' '
        if(!$taskItem.id.StartsWith('LETHALITY;') -or $taskActualSentence -ne $taskFrozenSentence){throw ('Unreviewed frozen corpus drift '+$taskItem.id)}
    }
    $taskRecords+=@{id=$taskItem.id;legacy_title=$taskOriginal.lore[2];legacy_effect=$taskRows;reviewed_wrap_or_icon_change=$taskChanged;source_file=$taskFile;source_sha256=(Get-FileHash -LiteralPath $taskFile).Hash}
}
if($taskRecords.Count -ne 26){throw 'Expected26 known book cards'}
$taskOutput=Join-Path $taskProject 'translation-source/ENCHANTMENT_LEGACY_0.15.49.json'
$taskData=@{scope='26 original repository cards only, not game captures or all server';records=$taskRecords}|ConvertTo-Json -Depth 5
[IO.File]::WriteAllText($taskOutput,$taskData+"`n",[Text.UTF8Encoding]::new($false))
Write-Output ('CAPTURED26 ORIGINAL BOOKS: '+$taskOutput)
