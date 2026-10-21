$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskTarget="$taskProject/translation-source/JAR_SCOPE_0.15.68.json";if(Test-Path -LiteralPath $taskTarget){throw 'Preserve scope proof'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskBase="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar";$taskCandidate="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar"
if((Get-FileHash -LiteralPath $taskBase).Hash -ne '8676237B43BF896DD362FE7F6C89F84B75A2E42901E9793737593E64583779F7' -or (Get-FileHash -LiteralPath $taskCandidate).Hash -ne '81A0ECE1E69C69903F29616BA6B526586D1F8E84E0ECCA9C0DA2999B7E2CA4EA'){throw 'Different runtime'}
function Get-TaskHash($entry){$taskStream=$entry.Open();$taskHash=[Security.Cryptography.SHA256]::Create();try{return [BitConverter]::ToString($taskHash.ComputeHash($taskStream)).Replace('-','')}finally{$taskHash.Dispose();$taskStream.Dispose()}}
$taskOld=[IO.Compression.ZipFile]::OpenRead($taskBase);$taskNew=[IO.Compression.ZipFile]::OpenRead($taskCandidate)
try{$taskRows=@();foreach($taskEntry in $taskNew.Entries){if($taskEntry.FullName.EndsWith('/')){continue};$taskPrior=$taskOld.GetEntry($taskEntry.FullName);$taskBefore=if($taskPrior){Get-TaskHash $taskPrior}else{$null};$taskAfter=Get-TaskHash $taskEntry;$taskState=if(!$taskPrior){'added'}elseif($taskBefore -eq $taskAfter){'identical'}else{'changed'}
        if($taskState -ne 'identical' -and $taskEntry.FullName -notmatch '^(ru/wither/skyru/ReviewedLoreStyles(?:\$[A-Za-z0-9]+)?\.class|fabric\.mod\.json|data/witherskyru/proofread\.tsv)$'){throw "Unrelated change $($taskEntry.FullName)"}
        $taskRows+=@{entry=$taskEntry.FullName;state=$taskState;before_sha256=$taskBefore;after_sha256=$taskAfter}}
    foreach($taskEntry in $taskOld.Entries){if(!$taskEntry.FullName.EndsWith('/') -and !$taskNew.GetEntry($taskEntry.FullName)){throw 'Removed entry'}}
}finally{$taskOld.Dispose();$taskNew.Dispose()}
[IO.File]::WriteAllText($taskTarget,(@{version='0.15.68-dev';baseline='0.15.67-dev';jar_sha256=(Get-FileHash -LiteralPath $taskCandidate).Hash;render_context67_byte_identical=$true;only_reviewed_paragraph_class_dictionary_and_version_changed=$true;entries=$taskRows}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
