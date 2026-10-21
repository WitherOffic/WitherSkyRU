param([Parameter(Mandatory=$true)][string]$Mods)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava=Join-Path $env:JAVA_HOME 'bin/java.exe'
$taskRoot=Join-Path $taskProject ('build/vendor262-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot | Out-Null
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim()
$taskDeps=@($taskCp.Split(';') | Where-Object {$_ -notmatch '[\\/]build[\\/](classes[\\/]java[\\/]main|resources[\\/]main)$'})
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar'
function Invoke-VendorCheck([string]$Name,[string]$Class,[string[]]$Arguments) {
 $taskWork=Join-Path $taskRoot $Name
 New-Item -ItemType Directory -Path "$taskWork/mods" -Force | Out-Null
 Copy-Item -LiteralPath $taskJar -Destination "$taskWork/mods"
 Copy-Item -LiteralPath "$Mods/fabric-api-0.161.0+26.2.jar" -Destination "$taskWork/mods"
 Push-Location $taskWork
 try {
  & $taskJava '-Xmx1g' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dmixin.debug.export=true' '-cp' ($script:taskDeps -join ';') $Class $taskWork @Arguments > "$taskWork/output.txt" 2>&1
  if($LASTEXITCODE -ne 0){Get-Content "$taskWork/output.txt" -Tail 40;throw "Vendor check failed: $Name"}
  Get-Content "$taskWork/output.txt" | Select-String '^PASS'
 } finally { Pop-Location }
}
function New-BytecodeFixture([string]$Source,[string]$Destination) {
 # Original class bytes only. Test metadata omits unrelated mod entrypoints and
 # foreign mixins, exactly as the existing vendor predicate/proxy tests do.
 $taskSourceZip=[IO.Compression.ZipFile]::OpenRead($Source)
 $taskFixture=[IO.Compression.ZipFile]::Open($Destination,[IO.Compression.ZipArchiveMode]::Create)
 try {
  $taskReader=[IO.StreamReader]::new($taskSourceZip.GetEntry('fabric.mod.json').Open())
  try{$taskMeta=$taskReader.ReadToEnd()|ConvertFrom-Json}finally{$taskReader.Dispose()}
  foreach($taskEntry in $taskSourceZip.Entries | Where-Object {$_.FullName.EndsWith('.class')}){
   $taskIn=$taskEntry.Open();$taskOut=$taskFixture.CreateEntry($taskEntry.FullName).Open()
   try{$taskIn.CopyTo($taskOut)}finally{$taskIn.Dispose();$taskOut.Dispose()}
  }
  $taskWriter=[IO.StreamWriter]::new($taskFixture.CreateEntry('fabric.mod.json').Open(),[Text.UTF8Encoding]::new($false))
  try{$taskWriter.Write((@{schemaVersion=1;id=$taskMeta.id;version=$taskMeta.version;name='TEST ONLY original vendor class bytes';environment='client'}|ConvertTo-Json -Compress))}finally{$taskWriter.Dispose()}
 } finally {$taskFixture.Dispose();$taskSourceZip.Dispose()}
}
New-Item -ItemType Directory -Path "$taskRoot/combined/mods","$taskRoot/skyblocker/mods","$taskRoot/overhaul/mods" | Out-Null
foreach($taskName in @('reliable-recipe-viewer-8.11.1+26.2-fabric.jar','skyrecipes-0.5.13+26.2.jar')){
 Copy-Item -LiteralPath "$Mods/$taskName" -Destination "$taskRoot/combined/mods"
}
Invoke-VendorCheck 'combined' 'TestHudMixins' @('rrv','skyrecipes')
New-BytecodeFixture "$Mods/skyblocker-6.11.0+26.2.jar" "$taskRoot/skyblocker/mods/skyblocker-fixture.jar"
Invoke-VendorCheck 'skyblocker' 'TestSkyblockerSearchBridge' @()
# Kotlin implementation dependencies, copied only into this isolated test root.
$taskKotlin=[IO.Compression.ZipFile]::OpenRead("$Mods/fabric-language-kotlin-1.14.2+kotlin.2.4.21.jar")
try{
 foreach($taskEntry in $taskKotlin.Entries | Where-Object {$_.FullName -match '^META-INF/jars/.+\.jar$'}){
  $taskDest=Join-Path $taskRoot ([IO.Path]::GetFileName($taskEntry.FullName))
  [IO.Compression.ZipFileExtensions]::ExtractToFile($taskEntry,$taskDest)
  $taskDeps+=$taskDest
 }
}finally{$taskKotlin.Dispose()}
New-BytecodeFixture "$Mods/scoreboardoverhaul-1.5.1-mc26.2.jar" "$taskRoot/overhaul/mods/overhaul-fixture.jar"
Invoke-VendorCheck 'overhaul' 'TestOverhaul151BudgetBridge' @()
@{jar_sha256=(Get-FileHash $taskJar).Hash;combined='unmodified full RRV/SkyRecipes jars; transformation and actual native filter calls, no entrypoints';skyblocker='original class bytes, metadata-only fixture';overhaul='original class bytes, metadata-only fixture and proxy draws';live_rendering=$false} | ConvertTo-Json | Set-Content "$taskRoot/RESULT.json" -Encoding utf8
Write-Output "VENDOR262_PASS=$taskRoot"
