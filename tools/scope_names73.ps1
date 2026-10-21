$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '1645060A5C13D085B44AFE3666009F88F31BD55CD8C6ED2E0AF13B76EFD48E5D'){throw 'Wrong delivered72 baseline'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entries73([string]$taskFile){$taskEntries=@{};$taskZip=[IO.Compression.ZipFile]::OpenRead($taskFile);try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskStream=$taskEntry.Open();try{$taskEntries[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskEntries}
$taskOld=Read-Entries73 $taskBefore;$taskNew=Read-Entries73 $taskAfter
$taskAdded=@($taskNew.Keys|Where-Object {!$taskOld.ContainsKey($_)});$taskRemoved=@($taskOld.Keys|Where-Object {!$taskNew.ContainsKey($_)})
if($taskAdded.Count -ne 0 -or $taskRemoved.Count -ne 0){throw 'Unexpected added/removed runtime entries'}
$taskAllowed=@('fabric.mod.json','ru/wither/skyru/PlayerNames.class','ru/wither/skyru/PlayerNames$NameIndex.class')
$taskChanged=@();foreach($taskEntry in $taskNew.Keys){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){if($taskEntry -notin $taskAllowed){throw "Unexpected change:$taskEntry"};$taskChanged+=@{entry=$taskEntry;before=$taskOld[$taskEntry];after=$taskNew[$taskEntry]}}}
$taskResources=@($taskNew.Keys|Where-Object {$_ -like 'data/*' -or $_ -like 'assets/*'});foreach($taskEntry in $taskResources){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){throw 'Resource changed'}}
$taskReport=@{before='0.15.72-dev';before_sha256=(Get-FileHash $taskBefore).Hash;after='0.15.73-dev';after_sha256=(Get-FileHash $taskAfter).Hash;runtime_entry_count=$taskNew.Count;unchanged_translation_and_asset_resources=$taskResources.Count;all_translation_resources_byte_identical=$true;new_frame_hooks=0;added_entries=$taskAdded;removed_entries=$taskRemoved;changed=$taskChanged;not_live_fps_proof=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.73.json",($taskReport|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
$taskReport|ConvertTo-Json -Depth 5
