$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskPages=(Get-Content "$taskProject/translation-source/NPC_SOURCES_0.15.76.json" -Raw|ConvertFrom-Json).pages
$taskPolicy=Get-Content "$taskProject/translation-source/NPC_EXCLUSIONS_0.15.76.json" -Raw|ConvertFrom-Json
$taskKnown=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($taskFile in (Get-ChildItem "$taskProject/src/main/resources/data/witherskyru" -Filter '*.json'|Where-Object Name -ne 'npc-expansion76.json')){foreach($taskRow in (Get-Content $taskFile.FullName -Raw|ConvertFrom-Json).records){if($taskRow.source){[void]$taskKnown.Add($taskRow.source)}}}
$taskResult=@()
foreach($taskPage in $taskPages){
 $taskSection=$taskPage.section;$taskStop=$taskPolicy.historical_cutoffs.($taskPage.name)
 if($taskStop){if(!$taskSection.Contains($taskStop)){throw "Missing historical marker:$($taskPage.name)"};$taskSection=$taskSection.Substring(0,$taskSection.IndexOf($taskStop))}
 $taskCandidates=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal);$taskVariable=@();$taskAmbiguous=@()
 foreach($taskLine in ($taskSection -split [Environment]::NewLine -split "\n")){
  foreach($taskMatch in [regex]::Matches($taskLine,'\[(NPC|SECURITY)\] ([^:]{1,60}): (.*?)(?=\[(?:NPC|SECURITY)\]|$)')){
   $taskValue=$taskMatch.Value.TrimEnd();$taskValue=($taskValue -split 'Select an option:',2)[0].TrimEnd()
   $taskValue=($taskValue -split '\[(?:GIVE ITEMS?|PAY DEBT)\]',2)[0].TrimEnd()
   $taskValue=($taskValue -split ('✆ '+[regex]::Escape($taskPage.name)+' has been added to your Abiphone'),2)[0].TrimEnd()
   if($taskValue -match $taskPolicy.variable_patterns){$taskVariable+=$taskValue;continue}
   if($taskValue -in $taskPolicy.exact_exclusions.source){$taskAmbiguous+=$taskValue;continue}
   [void]$taskCandidates.Add($taskValue)
  }
 }
 $taskResult+=@{speaker=$taskPage.name;tag=$taskPage.tag;source_url=$taskPage.url;published_literal_candidates=$taskCandidates.Count;registered_before=@($taskCandidates|Where-Object {$taskKnown.Contains($_)});without_exact_registration=@($taskCandidates|Where-Object {!$taskKnown.Contains($_)}|Sort-Object);published_variable_definitions=$taskVariable;ambiguous_excluded=$taskAmbiguous;historical_section_excluded=[bool]$taskStop;all_live_branches_verified=$false}
}
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_CANDIDATES_0.15.76.json",(@{scope='17 bounded publication snapshots;exact resource presence is not dispatch/live proof';pages=$taskResult}|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
foreach($taskPage in $taskResult){Write-Output ("---"+$taskPage.speaker+" missing="+$taskPage.without_exact_registration.Count+" old="+$taskPage.registered_before.Count);$taskPage.without_exact_registration}

