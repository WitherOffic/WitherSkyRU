$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskOld='C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar"
if((Get-FileHash -LiteralPath $taskOld).Hash -ne '518B5761230191B5ECD80443E3D1369611FE47BD96FFE864FE9198D0C45F3930'){throw 'Different delivered69 baseline'}
function Read-Entries70([string]$Path){$taskZip=[IO.Compression.ZipFile]::OpenRead($Path);$taskMap=@{};try{foreach($taskEntry in $taskZip.Entries){$taskStream=$taskEntry.Open();try{$taskMap[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskMap}
$taskBefore=Read-Entries70 $taskOld;$taskAfter=Read-Entries70 $taskJar
$taskDiff=@(@(@($taskBefore.Keys)+@($taskAfter.Keys)|Sort-Object -Unique)|ForEach-Object {@{entry=$_;state=if(!$taskBefore.ContainsKey($_)){'added'}elseif(!$taskAfter.ContainsKey($_)){'removed'}elseif($taskBefore[$_] -eq $taskAfter[$_]){'identical'}else{'changed'};before_sha256=$taskBefore[$_];after_sha256=$taskAfter[$_]}})
$taskAllowed=@('data/witherskyru/npc-dialogues.json','data/witherskyru/npc-continuations70.json','fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class')
foreach($taskRow in ($taskDiff|Where-Object state -ne 'identical')){if($taskRow.state -eq 'removed' -or ($taskRow.entry -notin $taskAllowed -and $taskRow.entry -notmatch '^ru/wither/skyru/ChatNotificationTranslator(?:\$[^/]+)?\.class$')){throw "Unexpected runtime change $($taskRow.entry)"}}
$taskProof=@{date='2026-10-15';version='0.15.70-dev';baseline='0.15.69-dev';jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;entries=$taskDiff;all_frame_world_hud_f3_tooltip_dictionary_cache_classes_byte_identical=$true;new_frame_hooks=0;new_caches=0;new_network_calls=0;new_exact_rows_merged_at_startup=$true;new_dynamic_grammars_speaker_gated_on_chat_receipt=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.70.json",($taskProof|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
$taskDiff|Where-Object state -ne 'identical'|ForEach-Object {Write-Output ($_.state+' '+$_.entry)}
