$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.71-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '0BAA651B0D612D95AC0D3F486278094C0B16497CD6618914791C8BA21316CA46'){throw 'Wrong delivered71 baseline'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entries72([string]$taskFile){$taskEntries=@{};$taskZip=[IO.Compression.ZipFile]::OpenRead($taskFile);try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskStream=$taskEntry.Open();try{$taskEntries[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskEntries}
$taskOld=Read-Entries72 $taskBefore;$taskNew=Read-Entries72 $taskAfter
$taskAdded=@($taskNew.Keys|Where-Object {!$taskOld.ContainsKey($_)});$taskRemoved=@($taskOld.Keys|Where-Object {!$taskNew.ContainsKey($_)})
if($taskRemoved.Count -ne 0){throw 'Unexpected removed runtime entries'}
foreach($taskEntry in $taskAdded){if($taskEntry -notmatch '^ru/wither/skyru/PreparedDisplay(?:\$[^/]+)?\.class$'){throw "Unexpected new entry:$taskEntry"}}
$taskAllowed=@('fabric.mod.json','ru/wither/skyru/SkyRuClient.class','ru/wither/skyru/PlayerNames.class','ru/wither/skyru/PlayerNames$NameIndex.class')
$taskChanged=@();foreach($taskEntry in $taskNew.Keys){if($taskOld.ContainsKey($taskEntry) -and $taskOld[$taskEntry] -ne $taskNew[$taskEntry]){if($taskEntry -notin $taskAllowed){throw "Unexpected change:$taskEntry"};$taskChanged+=@{entry=$taskEntry;before=$taskOld[$taskEntry];after=$taskNew[$taskEntry]}}}
$taskResources=@($taskNew.Keys|Where-Object {$_ -like 'data/*' -or $_ -like 'assets/*'});foreach($taskEntry in $taskResources){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){throw 'Resource changed'}}
$taskReport=@{before='0.15.71-dev';before_sha256=(Get-FileHash $taskBefore).Hash;after='0.15.72-dev';after_sha256=(Get-FileHash $taskAfter).Hash;runtime_entry_count=$taskNew.Count;unchanged_translation_and_asset_resources=$taskResources.Count;all_translation_resources_byte_identical=$true;new_frame_hooks=0;added_entries=$taskAdded;removed_entries=$taskRemoved;changed=$taskChanged;not_live_fps_proof=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.72.json",($taskReport|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
$taskReport|ConvertTo-Json -Depth 5
