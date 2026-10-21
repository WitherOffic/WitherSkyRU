# Select only explicit menu text; never export private chat or edit the installed pack.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskDestination=Join-Path $taskProject 'translation-source/SACK_MENUS_OBSERVED_0.15.61.json'
if(Test-Path -LiteralPath $taskDestination){throw 'Historical capture already exists'}
$taskInput='C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/config/witherskyru/unknown.tsv'
$taskWanted=@('To Sack of Sacks','Get From Sacks','Enchanted Agronomy Sack','Agronomy Sacks','Small Sack of Sacks','Sack of Sacks III','Slots','A sack which contains other sacks.','Sackception!','Also accessible via /sacks','Unlock additional slots in your Sack of','Obtain sack capacity upgrades as well','as exclusive bits shop sacks.','From Pocket Sack-in-a-Sack','for sack total (','Empty sack!','Stored:','Total items:','Click to pickup!','Click to put items in!')
$taskMatches=@{}
$taskStream=[IO.File]::Open($taskInput,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
$taskReader=[IO.StreamReader]::new($taskStream,[Text.Encoding]::UTF8)
try{$taskLine=0;while(($taskValue=$taskReader.ReadLine()) -ne $null){$taskLine++;if($taskWanted -ccontains $taskValue -and !$taskMatches.ContainsKey($taskValue)){$taskMatches[$taskValue]=$taskLine}}}finally{$taskReader.Dispose()}
$taskRecords=@(foreach($taskText in $taskWanted){if(!$taskMatches.ContainsKey($taskText)){throw "Unobserved exact label: $taskText"};[ordered]@{source=$taskText;origin='installed config/witherskyru/unknown.tsv (read-only)';first_line=$taskMatches[$taskText]}})
$taskResult=[ordered]@{date='2026-10-14';scope='20 selected exact sack-menu fragments; unknown log does not prove adjacency, component actions or full live UI coverage';records=$taskRecords}
[IO.File]::WriteAllText($taskDestination,($taskResult|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "Captured $($taskRecords.Count) explicit menu strings; private/player chat not exported."
