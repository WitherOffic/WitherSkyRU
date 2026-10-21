$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskTarget=Join-Path $taskProject 'translation-source/JAR_SCOPE_0.15.66.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve saved scope proof'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskOldPath=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar'
$taskNewPath=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.66-dev.jar'
if((Get-FileHash -LiteralPath $taskOldPath).Hash -ne '721732CC20BC69EE2337F1A557DBDEFE6B183602D8932D6EDA0AB43244183113' -or (Get-FileHash -LiteralPath $taskNewPath).Hash -ne 'B36538B44ADD46533576505A186583092CF04E508A1F472BFAC07E6BE5B62A59'){throw 'Different JAR'}
$taskOld=[IO.Compression.ZipFile]::OpenRead($taskOldPath);$taskNew=[IO.Compression.ZipFile]::OpenRead($taskNewPath)
try{
    $taskRows=@();foreach($taskEntry in $taskNew.Entries){
        if($taskEntry.FullName.EndsWith('/')){continue}
        $taskPrior=$taskOld.GetEntry($taskEntry.FullName);$taskHash=[Security.Cryptography.SHA256]::Create()
        $taskStream=$taskEntry.Open();try{$taskNextHash=[BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskStream.Dispose()}
        $taskPriorHash=$null;if($taskPrior){$taskStream=$taskPrior.Open();try{$taskPriorHash=[BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskStream.Dispose()}}
        $taskHash.Dispose();$taskState=if(!$taskPrior){'added'}elseif($taskPriorHash -eq $taskNextHash){'identical'}else{'changed'}
        if($taskState -ne 'identical' -and $taskEntry.FullName -notmatch '^(ru/wither/skyru/ChatNotificationTranslator(?:\$[A-Za-z0-9]+)?\.class|data/witherskyru/story-dialogues\.json|fabric\.mod\.json)$'){throw "Unrelated changed entry: $($taskEntry.FullName)"}
        $taskRows+=@{entry=$taskEntry.FullName;state=$taskState;baseline_sha256=$taskPriorHash;candidate_sha256=$taskNextHash}
    }
    foreach($taskEntry in $taskOld.Entries){if(!$taskEntry.FullName.EndsWith('/') -and !$taskNew.GetEntry($taskEntry.FullName)){throw 'Removed prior entry'}}
}finally{$taskOld.Dispose();$taskNew.Dispose()}
if(@($taskRows|Where-Object state -eq 'changed').Count -ne 6 -or @($taskRows|Where-Object state -eq 'added').Count -ne 1){throw 'Unexpected scope'}
[IO.File]::WriteAllText($taskTarget,(@{runtime='0.15.66-dev';baseline='0.15.65-dev';scope='All non-directory JAR entries hashed; no removed entries; non-chat classes and item dictionaries unchanged. Not new live8937-item proof.';entries=$taskRows}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
