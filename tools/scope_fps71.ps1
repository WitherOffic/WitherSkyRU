$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.70-dev.jar";$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.71-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '32AFF41415A452DCF90AE7D02956F7BC33F0F96725206C13CDDEFB0F66CF3DA5'){throw 'Wrong delivered70 baseline'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entries([string]$taskFile){$taskEntries=@{};$taskZip=[IO.Compression.ZipFile]::OpenRead($taskFile);try{foreach($taskEntry in $taskZip.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskStream=$taskEntry.Open();try{$taskEntries[$taskEntry.FullName]=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($taskStream))}finally{$taskStream.Dispose()}}}finally{$taskZip.Dispose()};return $taskEntries}
$taskOld=Read-Entries $taskBefore;$taskNew=Read-Entries $taskAfter
if(@(Compare-Object @($taskOld.Keys) @($taskNew.Keys)).Count -ne 0){throw 'Unexpected added/removed runtime entries'}
$taskAllowed=@('fabric.mod.json','ru/wither/skyru/DisplayNoop.class','ru/wither/skyru/SkyRuClient.class','ru/wither/skyru/RenderContext.class','ru/wither/skyru/RenderContext$Scope.class','ru/wither/skyru/TechnicalText.class','ru/wither/skyru/TechnicalText$Scope.class','ru/wither/skyru/OverhaulTextBudget.class','ru/wither/skyru/mixin/GuiTextMixin.class','ru/wither/skyru/mixin/HudCollectorMixin.class')
$taskChanged=@();foreach($taskEntry in $taskNew.Keys){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){if($taskEntry -notin $taskAllowed){throw "Unexpected runtime change:$taskEntry"};$taskChanged+=@{entry=$taskEntry;before=$taskOld[$taskEntry];after=$taskNew[$taskEntry]}}}
$taskResources=@($taskNew.Keys|Where-Object {$_ -like 'data/*' -or $_ -like 'assets/*'})
foreach($taskEntry in $taskResources){if($taskOld[$taskEntry] -ne $taskNew[$taskEntry]){throw 'Translation/resource changed'}}
$taskReport=@{before='0.15.70-dev';before_sha256=(Get-FileHash $taskBefore).Hash;after='0.15.71-dev';after_sha256=(Get-FileHash $taskAfter).Hash;runtime_entry_count=$taskNew.Count;unchanged_translation_and_asset_resources=$taskResources.Count;all_translation_resources_byte_identical=$true;added_entries=0;removed_entries=0;changed=$taskChanged;not_proof_of_live_fps_improvement=$true}
[IO.File]::WriteAllText("$taskProject/translation-source/JAR_SCOPE_0.15.71.json",($taskReport|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
$taskReport|ConvertTo-Json -Depth 5
