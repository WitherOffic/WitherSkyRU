param([string]$Commit='a225a02217776299ab3fb5cf5d9c0b155c4fc445',[string]$CaptureDate=(Get-Date -Format 'yyyy-MM-dd'))
# Public reference data only; never reads player inventories or modifies installed mods.
$ErrorActionPreference='Stop'
if($Commit -notmatch '^[0-9a-f]{40}$'){throw 'Expected pinned public commit'}
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject ('build/item-audit64-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskHeaders=@{'User-Agent'='WitherSkyRU-public-translation-audit'}
$taskMeta=Invoke-RestMethod "https://api.github.com/repos/NotEnoughUpdates/NotEnoughUpdates-REPO/commits/$Commit" -Headers $taskHeaders -TimeoutSec 45
if($taskMeta.sha -ne $Commit){throw 'Commit mismatch'}
$taskApiUrl='https://api.hypixel.net/v2/resources/skyblock/items'
$taskApiPath=Join-Path $taskRoot 'official-items.json'
Invoke-WebRequest $taskApiUrl -OutFile $taskApiPath -TimeoutSec 45
$taskApi=Get-Content -Raw -LiteralPath $taskApiPath|ConvertFrom-Json
if(!$taskApi.success -or $taskApi.items.Count -lt 1000){throw 'Invalid public item resource'}
$taskArchive=Join-Path $taskRoot 'public-repo.zip'
Invoke-WebRequest "https://codeload.github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/zip/$Commit" -OutFile $taskArchive -TimeoutSec 120
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskArchive)
$taskRecords=[Collections.Generic.List[object]]::new()
$taskIds=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$taskHash=[Security.Cryptography.SHA256]::Create()
try{
    foreach($taskEntry in $taskZip.Entries|Sort-Object FullName){
        if($taskEntry.FullName -notmatch '^[^/]+/items/([^/]+\.json)$'){continue}
        $taskName=$Matches[1]
        $taskStream=$taskEntry.Open();try{$taskDigest=[Convert]::ToHexString($taskHash.ComputeHash($taskStream))}finally{$taskStream.Dispose()}
        $taskReader=[IO.StreamReader]::new($taskEntry.Open(),[Text.Encoding]::UTF8)
        try{$taskItem=$taskReader.ReadToEnd()|ConvertFrom-Json}finally{$taskReader.Dispose()}
        if(!$taskItem.displayname){throw "Missing public display name: $taskName"}
        $taskId=if($taskItem.internalname){[string]$taskItem.internalname}else{[IO.Path]::GetFileNameWithoutExtension($taskName)}
        if(!$taskIds.Add($taskId)){throw "Duplicate public item ID $taskId"}
        $taskRecords.Add([ordered]@{id=$taskId;name_legacy=$taskItem.displayname;name_plain=[regex]::Replace($taskItem.displayname,'(?i)§[0-9a-fk-or]','');lore_legacy=@($taskItem.lore);itemid=$taskItem.itemid;source_sha256=$taskDigest;source_url="https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/$Commit/items/$([Uri]::EscapeDataString($taskName))"})
    }
}finally{$taskZip.Dispose();$taskHash.Dispose()}
if($taskRecords.Count -lt 8000){throw 'Unexpectedly small public catalogue'}
$taskData=[ordered]@{scope='Pinned public NEU item cards, including variants; not all live player states or a human proofread';capture_date=$CaptureDate;commit=$Commit;commit_date=$taskMeta.commit.committer.date;records=$taskRecords.ToArray()}
[IO.File]::WriteAllText((Join-Path $taskRoot 'catalogue.json'),($taskData|ConvertTo-Json -Depth 20),[Text.UTF8Encoding]::new($false))
$taskBooks=@($taskRecords|Where-Object {$_.itemid -eq 'minecraft:enchanted_book' -and $_.id -match '^[A-Z0-9_]+;[1-9][0-9]{0,2}$' -and $_.name_plain -eq 'Enchanted Book'})
$taskManifest=[ordered]@{capture_date=$CaptureDate;commit=$Commit;commit_date=$taskMeta.commit.committer.date;repository_url=$taskMeta.html_url;repository_archive_sha256=(Get-FileHash -LiteralPath $taskArchive).Hash;official_items_url=$taskApiUrl;official_items_sha256=(Get-FileHash -LiteralPath $taskApiPath).Hash;official_last_updated=$taskApi.lastUpdated;official_item_count=$taskApi.items.Count;public_card_count=$taskRecords.Count;book_level_cards=$taskBooks.Count;enchantment_families=@($taskBooks|ForEach-Object {($_.id -split ';')[0]}|Sort-Object -Unique).Count;installed_modpack_modified=$false;all_live_server_variants_known=$false;full_manual_proofread=$false}
[IO.File]::WriteAllText((Join-Path $taskRoot 'manifest.json'),($taskManifest|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
$taskManifest|ConvertTo-Json -Depth 5
Write-Output "PUBLIC SNAPSHOT: $taskRoot"
