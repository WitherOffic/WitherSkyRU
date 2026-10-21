$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskCat=Get-Content "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7/catalogue.json" -Raw|ConvertFrom-Json
$taskCases=@(
    @{id='witherborn';item='POWER_WITHER_CHESTPLATE';head='Spawns a wither minion every ';rows=4;target=@('Призывает иссушителя каждые 30','с; одновременно — не более 1 иссушителя.','Ваши иссушители летят к','ближайшим врагам и взрываются.')},
    @{id='skeleton_arrows5';item='SKELETON_MASTER_CHESTPLATE';head='Increase the damage you deal with';rows=2;target=@('Увеличивает урон от стрел','на 5%.')},
    @{id='skeleton_arrows25';item='SKELETON_MASTER_CHESTPLATE';head='Increase the damage you deal with';occurrence=1;rows=2;target=@('Увеличивает урон от стрел','на 25%.')},
    @{id='skeleton_extra25';item='SKELETON_SOLDIER_CHESTPLATE';head='Increase the damage you deal with';occurrence=1;rows=2;target=@('Увеличивает урон от стрел','дополнительно на 25%.')},
    @{id='maxor_arrows';item='SPEED_WITHER_CHESTPLATE';head='Reduces the damage you take from';rows=3;target=@('Уменьшает урон от иссушителей','на 10% и увеличивает','урон от стрел на 5%.')},
    @{id='adaptive_training';item='ADAPTIVE_CHESTPLATE';head='Every ';rows=2;target=@('За каждые 5 уровней Катакомб эта часть брони','получает +2% к характеристикам.')},
    @{id='adaptive_class_notice';item='ADAPTIVE_CHESTPLATE';head='Grants additional bonuses based on your';rows=2;target=@('Даёт дополнительные бонусы в зависимости','от выбранного класса подземелий!')},
    @{id='adaptive_tank';item='ADAPTIVE_CHESTPLATE';head='Tank: +';rows=4;target=@('Танк: +30 защиты и уменьшает урон','на 5% за каждую надетую часть комплекта,','если тот же монстр попадает по вам в течение 10','с.')}
)
$taskRecords=@()
foreach($taskCase in $taskCases){$taskItem=$taskCat.records|Where-Object id -eq $taskCase.item;$taskAll=@($taskItem.lore_legacy);$taskHeads=@(for($taskAt=0;$taskAt -lt $taskAll.Count;$taskAt++){if(($taskAll[$taskAt] -replace '§[0-9a-fk-or]','').StartsWith($taskCase.head)){$taskAt}});$taskStart=$taskHeads[[int]$taskCase.occurrence];if($null -eq $taskStart){throw "Missing paragraph $($taskCase.id)"};$taskRows=@($taskAll[$taskStart..($taskStart+$taskCase.rows-1)]);$taskPlain=@($taskRows|ForEach-Object {$_ -replace '§[0-9a-fk-or]',''});$taskCover=@($taskCat.records|Where-Object {($_.lore_legacy -replace '§[0-9a-fk-or]','' -join "`n").Contains(($taskPlain -join "`n"))}|ForEach-Object id);$taskRecords+=@{id=$taskCase.id;source_item=$taskCase.item;source_url=$taskItem.source_url;source_rows=$taskRows;target_rows=$taskCase.target;exact_snapshot_card_ids=$taskCover}}
$taskPath="$taskProject/translation-source/DUNGEON_ARMOR_0.15.68.json";if(Test-Path -LiteralPath $taskPath){throw 'Preserve primary fixture'}
[IO.File]::WriteAllText($taskPath,(@{date='2026-10-15';source_commit='a225a02217776299ab3fb5cf5d9c0b155c4fc445';scope='Eight full primary paragraphs; exact cards listed, not whole armor cards certified';records=$taskRecords}|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "Armor paragraphs: $($taskRecords.Count)"
