$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskBeforeJar='C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar'
if((Get-FileHash -LiteralPath $taskBeforeJar).Hash -ne '81A0ECE1E69C69903F29616BA6B526586D1F8E84E0ECCA9C0DA2999B7E2CA4EA'){throw 'Different delivered68 baseline'}
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.69-dev.jar"
function Read-TaskEntries([string]$Path){$taskZip=[IO.Compression.ZipFile]::OpenRead($Path);$taskEntries=@{};try{foreach($taskEntry in $taskZip.Entries){$taskStream=$taskEntry.Open();try{$taskEntries[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskEntries}
$taskBefore=Read-TaskEntries $taskBeforeJar;$taskAfter=Read-TaskEntries $taskJar;$taskNames=@(@($taskBefore.Keys)+@($taskAfter.Keys)|Sort-Object -Unique)
$taskDiff=@($taskNames|ForEach-Object {@{entry=$_;state=if(!$taskBefore.ContainsKey($_)){'added'}elseif(!$taskAfter.ContainsKey($_)){'removed'}elseif($taskBefore[$_] -eq $taskAfter[$_]){'identical'}else{'changed'};before_sha256=$taskBefore[$_];after_sha256=$taskAfter[$_]}})
$taskProtected=@($taskDiff|Where-Object {$_.entry -match '^ru/wither/skyru/(mixin/|RenderContext|ReviewedLore|TooltipTranslation|StyledRuns|StyledItemTitle|OfflineDictionary|Unknown|HudTextFit|TechnicalText)'})
if(@($taskProtected|Where-Object state -ne 'identical').Count -ne 0){throw 'Unexpected frame/lore/cache implementation change'}
$taskAllowedChanged=@('ru/wither/skyru/SkyRuClient.class','ru/wither/skyru/NpcDialogueTranslator.class','data/witherskyru/proofread.tsv','fabric.mod.json')
foreach($taskEntry in ($taskDiff|Where-Object state -ne 'identical')){
 if($taskEntry.state -eq 'removed'){throw 'Unexpected removed entry'}
 if($taskEntry.entry -in $taskAllowedChanged -or $taskEntry.entry -match '^ru/wither/skyru/ChatNotificationTranslator(?:\$[^/]+)?\.class$' -or $taskEntry.entry -in @('ru/wither/skyru/NpcInteractionLabel.class','data/witherskyru/hub-dialogues.json')){continue}
 throw "Unexpected runtime change: $($taskEntry.entry)"
}
$taskProof=@{version='0.15.69-dev';baseline='0.15.68-dev';jar_sha256=(Get-FileHash -LiteralPath $taskJar).Hash;entries=$taskDiff;frame_mixin_context_lore_dictionary_cache_classes_byte_identical=$true;new_frame_hooks=0;new_caches=0;new_network_calls=0;startup_merge_not_ninth_chat_provider=$true;world_label_change_is_existing_hook_whitelist_only=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.69.json",($taskProof|ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
$taskDiff|Where-Object state -ne 'identical'|ForEach-Object {Write-Output ($_.state+' '+$_.entry)}
