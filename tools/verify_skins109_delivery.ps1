$ErrorActionPreference='Stop'
$taskDelivery='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_6'
$taskManifest=Get-Content -LiteralPath "$taskDelivery/HASHES.json" -Raw | ConvertFrom-Json
$taskCount=0
foreach($taskEntry in $taskManifest.PSObject.Properties){
 if((Get-FileHash -LiteralPath (Join-Path $taskDelivery $taskEntry.Name)).Hash -ne $taskEntry.Value){throw "Delivery mismatch: $($taskEntry.Name)"};$taskCount++
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskArchive='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_0.15.109.zip'
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskArchive)
try{
 foreach($taskEntry in $taskManifest.PSObject.Properties){
  $taskMember=$taskZip.GetEntry($taskEntry.Name);if($null -eq $taskMember){throw "Missing ZIP entry: $($taskEntry.Name)"}
  $taskStream=$taskMember.Open();$taskHasher=[Security.Cryptography.SHA256]::Create()
  try{$taskHash=[BitConverter]::ToString($taskHasher.ComputeHash($taskStream)).Replace('-','');if($taskHash -ne $taskEntry.Value){throw "ZIP mismatch: $($taskEntry.Name)"}}finally{$taskStream.Dispose();$taskHasher.Dispose()}
 }
 $taskManifestMember=$taskZip.GetEntry('HASHES.json');if($null -eq $taskManifestMember){throw 'ZIP manifest missing'}
 $taskManifestStream=$taskManifestMember.Open();$taskManifestHasher=[Security.Cryptography.SHA256]::Create()
 try{$taskManifestHash=[BitConverter]::ToString($taskManifestHasher.ComputeHash($taskManifestStream)).Replace('-','');if($taskManifestHash -ne (Get-FileHash -LiteralPath "$taskDelivery/HASHES.json").Hash){throw 'ZIP manifest mismatch'}}finally{$taskManifestStream.Dispose();$taskManifestHasher.Dispose()}
 $taskZipCount=$taskZip.Entries.Count;if($taskZipCount -ne ($taskCount+1)){throw 'ZIP entry count mismatch'}
}finally{$taskZip.Dispose()}
$taskPrevious='C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_5'
$taskOldManifest=Get-Content -LiteralPath "$taskPrevious/HASHES.json" -Raw | ConvertFrom-Json
$taskOldCount=0
foreach($taskEntry in $taskOldManifest.PSObject.Properties){
 if((Get-FileHash -LiteralPath (Join-Path $taskPrevious $taskEntry.Name)).Hash -ne $taskEntry.Value){throw "Previous package modified: $($taskEntry.Name)"};$taskOldCount++
}
if((Get-FileHash -LiteralPath 'C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_0.15.108.zip').Hash -ne '5690D5E8F58A6FA00F4953E59240CB32B05FD21A0328BF8E3462B9E454709E8E'){throw 'Previous ZIP modified'}
$taskJarHash=(Get-FileHash -LiteralPath 'C:/Users/Wither/Desktop/WitherSkyRU/WitherSkyRU-26.1.2-fabric-0.15.109-dev.jar').Hash
if($taskJarHash -ne '5263691ECFAF4CD57D60CCD69DE8C9DD05AE5D2167875DC17BC1AD77FF41CFE3'){throw 'JAR mismatch'}
[pscustomobject]@{verified_files=$taskCount;verified_zip_members=$taskZipCount;previous_files_unchanged=$taskOldCount;jar_sha256=$taskJarHash;zip_sha256=(Get-FileHash -LiteralPath $taskArchive).Hash} | ConvertTo-Json
