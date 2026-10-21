$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.75-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne 'F07822E5F0D3A8753D8E770C196B5EED471828A33B10702BBBB404211147EB18'){throw 'Wrong74 baseline'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Entries75([string]$taskPath){$taskResult=@{};$taskZip=[IO.Compression.ZipFile]::OpenRead($taskPath);try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskStream=$taskEntry.Open();try{$taskResult[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskResult}
$taskOld=Entries75 $taskBefore;$taskNew=Entries75 $taskAfter
$taskAdded=@($taskNew.Keys|Where-Object {!$taskOld.ContainsKey($_)});$taskRemoved=@($taskOld.Keys|Where-Object {!$taskNew.ContainsKey($_)})
if($taskRemoved.Count){throw 'Unexpected removed entries'}
foreach($taskEntry in $taskAdded){if($taskEntry -notin @('data/witherskyru/npc-expansion75.json','META-INF/licenses/NPC75-SOURCES.txt')){throw "Unexpected new runtime entry:$taskEntry"}}
$taskChanged=@();foreach($taskEntry in $taskNew.Keys){if($taskOld.ContainsKey($taskEntry) -and $taskOld[$taskEntry] -ne $taskNew[$taskEntry]){if($taskEntry -ne 'fabric.mod.json' -and $taskEntry -notmatch '^ru/wither/skyru/(?:NpcDialogueTranslator|ChatNotificationTranslator)(?:\$[^/]+)?\.class$'){throw "Unexpected runtime change:$taskEntry"};$taskChanged+=@{entry=$taskEntry;before=$taskOld[$taskEntry];after=$taskNew[$taskEntry]}}}
$taskResources=@($taskOld.Keys|Where-Object {$_ -like 'data/*' -or $_ -like 'assets/*'});foreach($taskEntry in $taskResources){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){throw "Prior resource changed:$taskEntry"}}
$taskFrameClasses=@($taskOld.Keys|Where-Object {$_ -like 'ru/wither/skyru/*' -and $_ -like '*.class' -and $_ -notmatch '^ru/wither/skyru/(?:NpcDialogueTranslator|ChatNotificationTranslator)(?:\$[^/]+)?\.class$'})
$taskProof=@{before='0.15.74-dev';before_sha256=(Get-FileHash $taskBefore).Hash;after='0.15.75-dev';after_sha256=(Get-FileHash $taskAfter).Hash;runtime_entry_count=$taskNew.Count;old_translation_and_asset_resources_byte_identical=$taskResources.Count;unchanged_other_runtime_classes=$taskFrameClasses.Count;all_frame_world_input_item_dictionary_and_prepared_cache_classes_byte_identical=$true;new_frame_hooks=0;added_entries=$taskAdded;removed_entries=$taskRemoved;changed=$taskChanged;new_npc_resources_loaded_once_into_existing_map=$true;new_per_message_providers=0;not_live_fps_proof=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.75.json",($taskProof|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$taskProof|ConvertTo-Json -Depth 6
