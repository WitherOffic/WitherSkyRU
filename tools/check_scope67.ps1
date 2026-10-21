$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskTarget=Join-Path $taskProject 'translation-source/JAR_SCOPE_0.15.67.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve saved scope proof'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskBase=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.66-dev.jar'
$taskCandidate=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar'
if((Get-FileHash -LiteralPath $taskBase).Hash -ne 'B36538B44ADD46533576505A186583092CF04E508A1F472BFAC07E6BE5B62A59' -or (Get-FileHash -LiteralPath $taskCandidate).Hash -ne '8676237B43BF896DD362FE7F6C89F84B75A2E42901E9793737593E64583779F7'){throw 'Different JAR'}
function Get-TaskEntryHash($entry){$taskStream=$entry.Open();$taskHash=[Security.Cryptography.SHA256]::Create();try{return [BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskHash.Dispose();$taskStream.Dispose()}}
$taskOld=[IO.Compression.ZipFile]::OpenRead($taskBase);$taskNew=[IO.Compression.ZipFile]::OpenRead($taskCandidate);$taskLive=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.64-dev.jar'))
try{
    if((Get-TaskEntryHash $taskLive.GetEntry('ru/wither/skyru/RenderContext.class')) -ne (Get-TaskEntryHash $taskOld.GetEntry('ru/wither/skyru/RenderContext.class'))){throw 'Live64 source context differs from benchmark66 baseline'}
    $taskRows=@();foreach($taskEntry in $taskNew.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskPrior=$taskOld.GetEntry($taskEntry.FullName);$taskBefore=if($taskPrior){Get-TaskEntryHash $taskPrior}else{$null};$taskAfter=Get-TaskEntryHash $taskEntry;$taskState=if(!$taskPrior){'added'}elseif($taskBefore -eq $taskAfter){'identical'}else{'changed'}
        if($taskState -ne 'identical' -and $taskEntry.FullName -notmatch '^(ru/wither/skyru/RenderContext(?:\$[A-Za-z0-9]+)?\.class|fabric\.mod\.json)$'){throw "Unrelated runtime change: $($taskEntry.FullName)"}
        $taskRows+=@{entry=$taskEntry.FullName;state=$taskState;baseline_sha256=$taskBefore;candidate_sha256=$taskAfter}
    }
    foreach($taskEntry in $taskOld.Entries){if(!$taskEntry.FullName.EndsWith('/') -and !$taskNew.GetEntry($taskEntry.FullName)){throw 'Removed runtime entry'}}
}finally{$taskOld.Dispose();$taskNew.Dispose();$taskLive.Dispose()}
[IO.File]::WriteAllText($taskTarget,(@{runtime='0.15.67-dev';baseline='0.15.66-dev';live64_rendercontext_class_byte_identical_to_baseline66=$true;all_translation_data_byte_identical_vs66=$true;scope='All JAR entries compared; only RenderContext implementation/debug classes and version may differ. This does not replace a gameplay A/B.';entries=$taskRows}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
