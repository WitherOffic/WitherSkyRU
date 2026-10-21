# Derive the known manual scope from actual test selections; do not count repeated Cleave.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskBefore=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskName in @('TestEnchantmentParagraphs','TestEnchantmentBatch54','TestEnchantmentBatch58')){
    $taskText=[IO.File]::ReadAllText((Join-Path $taskProject "tests/$taskName.java"))
    $taskSelection=[regex]::Match($taskText,'(?s)private static final Set<String> FAMILIES=Set\.of\((.*?)\);').Groups[1].Value
    if(!$taskSelection){throw "Missing actual test selection $taskName"}
    foreach($taskMatch in [regex]::Matches($taskSelection,'"([A-Z0-9_]+)"')){[void]$taskBefore.Add($taskMatch.Groups[1].Value)}
}
$taskText=[IO.File]::ReadAllText((Join-Path $taskProject 'tests/TestEnchantmentExpansion.java'))
$taskSelection=[regex]::Match($taskText,'(?s)private static final Map<String,Set<String>> PHASES=Map\.of\((.*?)\);').Groups[1].Value
if(!$taskSelection){throw 'Missing expansion selection'}
foreach($taskMatch in [regex]::Matches($taskSelection,'"([A-Z0-9_]+)"')){[void]$taskBefore.Add($taskMatch.Groups[1].Value)}
$taskText=[IO.File]::ReadAllText((Join-Path $taskProject 'tests/TestEnchantmentBatch50.java'))
if(!$taskText.Contains('Set.of("CAYENNE","CLEAVE").contains(family)')){throw 'Historical batch50 scope changed; re-audit it'}
[void]$taskBefore.Add('CAYENNE');[void]$taskBefore.Add('CLEAVE')
$taskText=[IO.File]::ReadAllText((Join-Path $taskProject 'tests/TestEnchantmentBatch59.java'))
$taskSelection=[regex]::Match($taskText,'(?s)private static final Set<String> FAMILIES=Set\.of\((.*?)\);').Groups[1].Value
$taskSelected=@([regex]::Matches($taskSelection,'"([A-Z0-9_]+)"')|ForEach-Object {$_.Groups[1].Value})
$taskNew=@($taskSelected|Where-Object {!$taskBefore.Contains($_)});$taskRepeated=@($taskSelected|Where-Object {$taskBefore.Contains($_)})
$taskAfter=[Collections.Generic.HashSet[string]]::new($taskBefore,[StringComparer]::Ordinal)
foreach($taskName in $taskSelected){[void]$taskAfter.Add($taskName)}
$taskFrozen=Get-Content -LiteralPath (Join-Path $taskProject 'translation-source/item-context.json') -Raw|ConvertFrom-Json
$taskBooks=@($taskFrozen|Where-Object {$_.name -ceq 'Enchanted Book' -and $_.id -cmatch '^[A-Z0-9_]+;[1-9][0-9]{0,2}$'})
$taskOldIds=@($taskBooks|Where-Object {$taskBefore.Contains(($_.id -split ';')[0])}|ForEach-Object id|Sort-Object -Unique)
$taskAfterIds=@($taskBooks|Where-Object {$taskAfter.Contains(($_.id -split ';')[0])}|ForEach-Object id|Sort-Object -Unique)
$taskNewIds=@($taskAfterIds|Where-Object {$_ -notin $taskOldIds})
$taskRepeatedIds=@($taskBooks|Where-Object {($_.id -split ';')[0] -in $taskRepeated}|ForEach-Object id|Sort-Object -Unique)
if($taskBooks.Count -ne 766 -or $taskBefore.Count -ne 36 -or $taskAfter.Count -ne 39 -or $taskOldIds.Count -ne 238 -or $taskAfterIds.Count -ne 257 -or $taskNewIds.Count -ne 19 -or $taskRepeatedIds.Count -ne 6){throw 'Scope changed; counters must be re-derived, not forced'}
$taskRoot=Join-Path $taskProject ('build/enchant-scope59-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskReport=[ordered]@{scope='Known manual test selections/frozen corpus only; must also pass those tests. Not all server or new-string count.';before_families=$taskBefore.Count;after_families=$taskAfter.Count;before_card_ids=$taskOldIds;after_card_ids=$taskAfterIds;new_families=$taskNew;repeated_families=$taskRepeated;new_card_ids=$taskNewIds;rechecked_card_ids=$taskRepeatedIds;frozen_card_count=$taskBooks.Count;remaining_without_full_manual_review=766-$taskAfterIds.Count}
[IO.File]::WriteAllText((Join-Path $taskRoot 'scope.json'),($taskReport|ConvertTo-Json -Depth 10)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output "SCOPE VERIFIED:238->257/766 cards;36->39/156 families;19 new/6 Cleave rechecked;$taskRoot"
