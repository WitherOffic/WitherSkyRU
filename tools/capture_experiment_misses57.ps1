# Bounded exact visible strings only, not a copy of private chat/all unknown history.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDestination=Join-Path $taskProject 'translation-source/EXPERIMENT_MISSES_0.15.57.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture exists'}
$taskSource='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/config/witherskyru/unknown.tsv'
$taskExact=@('Add-on Experiment','Clicks:','Enchant items to earn Enchanting EXP!','Enchanting Exp','Experiment over','Experimental XP:','Experimentation Table RNG','Experimentation Table RNG Meter','Experiments, Bazaar','Main Experiment','Renew Experiments','Stored Experimental XP:','Superpairs (Beginner)','Superpairs Rewards','the Experimentation Table.','To Experimentation Table','Unknown experiment!','Add-on locked!','Unlock this minigame with a higher','Enchanting level!','Required Enchanting:','Grid Size:','Possible pairs:','Starting cost:','Enchanting level too low!','Rare chance to find VII books!')
$taskCounts=[Collections.Generic.SortedDictionary[string,int]]::new([StringComparer]::Ordinal)
foreach($taskLine in Get-Content -LiteralPath $taskSource){
    if($taskLine -notin $taskExact -and $taskLine -notmatch '^(?:\([0-9]/[1-9]\) Experimentation Table RNG|[0-9][0-9,.]*[kMB]?(?:-[0-9][0-9,.]*[kMB]?)? Enchanting Exp(?: \((?:Stakes|Pairs)\))?|[0-9]{1,6} XP Levels|[0-9]{1,3}-[0-9]{1,3} (?:Experience Bottles|Grand Experience Bottles|Titanic Experience Bottles)|[IVX]{1,8}-[IVX]{1,8} Enchanted Books)$'){continue}
    if(!$taskCounts.ContainsKey($taskLine)){$taskCounts[$taskLine]=0};$taskCounts[$taskLine]++
}
$taskRecords=@(foreach($taskPair in $taskCounts.GetEnumerator()){[ordered]@{source=$taskPair.Key;occurrences=$taskPair.Value}})
$taskResult=[ordered]@{date='2026-10-14';status='LOCAL_MISS_STRINGS_ORIGIN_CHANNEL_LAYOUT_STYLE_UNVERIFIED';scope='Exact experimental labels/costs/reward strings; row adjacency is NOT a complete tooltip capture; no whole private history';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 20)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "CAPTURED:$($taskRecords.Count) distinct bounded experimental strings"
