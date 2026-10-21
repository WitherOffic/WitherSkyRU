$ErrorActionPreference='Stop'
$taskDelivery='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_9'
$taskManifest=Get-Content -LiteralPath "$taskDelivery/HASHES.json" -Raw | ConvertFrom-Json
$taskCount=0
foreach($taskEntry in $taskManifest.PSObject.Properties){
 if((Get-FileHash -LiteralPath (Join-Path $taskDelivery $taskEntry.Name)).Hash -ne $taskEntry.Value){throw "Delivery mismatch: $($taskEntry.Name)"};$taskCount++
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskArchive='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_0.15.112.zip'
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskArchive)
try{
 foreach($taskEntry in $taskManifest.PSObject.Properties){
  $taskMember=$taskZip.GetEntry($taskEntry.Name);if($null -eq $taskMember){throw "Missing ZIP entry: $($taskEntry.Name)"}
  $taskStream=$taskMember.Open();$taskHasher=[Security.Cryptography.SHA256]::Create()
  try{$taskHash=[BitConverter]::ToString($taskHasher.ComputeHash($taskStream)).Replace('-','');if($taskHash -ne $taskEntry.Value){throw "ZIP mismatch: $($taskEntry.Name)"}}finally{$taskStream.Dispose();$taskHasher.Dispose()}
 }
 $taskMember=$taskZip.GetEntry('HASHES.json');if($null -eq $taskMember){throw 'ZIP manifest missing'}
 $taskStream=$taskMember.Open();$taskHasher=[Security.Cryptography.SHA256]::Create()
 try{$taskHash=[BitConverter]::ToString($taskHasher.ComputeHash($taskStream)).Replace('-','');if($taskHash -ne (Get-FileHash -LiteralPath "$taskDelivery/HASHES.json").Hash){throw 'ZIP manifest mismatch'}}finally{$taskStream.Dispose();$taskHasher.Dispose()}
 $taskZipCount=$taskZip.Entries.Count;if($taskZipCount -ne ($taskCount+1)){throw 'ZIP entry count mismatch'}
}finally{$taskZip.Dispose()}
$taskPrevious='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_8'
$taskOldManifest=Get-Content -LiteralPath "$taskPrevious/HASHES.json" -Raw | ConvertFrom-Json
$taskOldCount=0
foreach($taskEntry in $taskOldManifest.PSObject.Properties){
 if((Get-FileHash -LiteralPath (Join-Path $taskPrevious $taskEntry.Name)).Hash -ne $taskEntry.Value){throw "Previous package modified: $($taskEntry.Name)"};$taskOldCount++
}
if((Get-FileHash -LiteralPath 'C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_0.15.111.zip').Hash -ne 'C856979A7AEEAC1DB18DBCFD46F941CB4E391E0A381002E89BCF61793E2EF997'){throw 'Previous ZIP modified'}
$taskJarHash=(Get-FileHash -LiteralPath 'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar').Hash
if($taskJarHash -ne '866E0C2277F876229F4EFA98C388CE942F8E548181EDDBDC0FEB96A91AA0095B'){throw 'JAR mismatch'}
[pscustomobject]@{verified_files=$taskCount;verified_zip_members=$taskZipCount;previous_files_unchanged=$taskOldCount;jar_sha256=$taskJarHash;zip_sha256=(Get-FileHash -LiteralPath $taskArchive).Hash} | ConvertTo-Json
