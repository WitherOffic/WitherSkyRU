$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskCatalogue="$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7/catalogue.json"
if((Get-FileHash -LiteralPath $taskCatalogue).Hash -ne '38FBA44F6D2843C3E31B4A682660553CA51C35D40B663DB1F4B4D725B7AD69A6'){throw 'Different catalogue'}
$taskIds=@('DUNGEON_STONE','DUNGEON_CHEST_KEY','DUNGEON_DECOY','DUNGEON_TRAP','DUNGEONBREAKER','SPIRIT_LEAP','INFINITE_SPIRIT_LEAP','REVIVE_STONE','HAUNT_ABILITY','TANK_DUNGEON_ABILITY_1','TANK_DUNGEON_ABILITY_2','MACHINE_GUN_BOW','BOSS_SPIRIT_BOW','CRYPT_BOW','DEATH_BOW','SUPER_UNDEAD_BOW','LESSER_ORB_OF_HEALING','ITEM_SPIRIT_BOW','STARRED_ITEM_SPIRIT_BOW')
$taskCatalogueData=Get-Content -LiteralPath $taskCatalogue -Raw|ConvertFrom-Json
$taskRecords=@($taskCatalogueData.records|Where-Object {$_.id -in $taskIds})
if($taskRecords.Count -ne $taskIds.Count){throw 'Missing selected cards'}
$taskAllDungeons=@($taskCatalogueData.records|Where-Object {($_.lore_legacy -join "`n") -match '(?:^|\n)§[^\n]*DUNGEON (?:ITEM|SWORD|BOW|HELMET|CHESTPLATE|LEGGINGS|BOOTS|PICKAXE|WAND|GLOVES|NECKLACE|BELT|CLOAK)(?:$|\n)'})
$taskOutput=@{date='2026-10-15';source_commit='a225a02217776299ab3fb5cf5d9c0b155c4fc445';catalogue_sha256=(Get-FileHash -LiteralPath $taskCatalogue).Hash;scope='19 pinned utility/weapon cards; all dungeon footer cards enumerated separately, not all manually reviewed';dungeon_footer_card_ids=@($taskAllDungeons|ForEach-Object id);records=$taskRecords}
$taskPath="$taskProject/translation-source/DUNGEONS_PRIMARY_0.15.68.json"
if(Test-Path -LiteralPath $taskPath){throw 'Preserve original fixture'}
[IO.File]::WriteAllText($taskPath,($taskOutput|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "Selected $($taskRecords.Count) full cards; enumerated $($taskAllDungeons.Count) dungeon footer cards"
