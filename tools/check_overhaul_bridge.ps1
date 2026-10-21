param([Parameter(Mandatory=$true)][string]$Version,[string]$VendorJar)
# Unmodified vendor classes in an isolated metadata-only fixture; never run game entrypoints.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskPrism='C:/Users/Wither/AppData/Roaming/PrismLauncher'
$taskJava=Join-Path $taskPrism 'java/java-runtime-epsilon/bin'
$taskMods=Join-Path $taskPrism 'instances/Skypixel (for Hypixel Skyblock)/minecraft/mods'
$taskVerification=Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Filter "jar-verification-$Version-*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if(!$taskVerification){throw 'Run baseline JAR verifier first'}
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerification.FullName 'TestChatNotifications-arguments.txt'))
$taskCp=$taskManifest[[Array]::IndexOf($taskManifest,'-cp')+1].Trim('"')
$taskRoot=Join-Path $taskProject ('build/overhaul-bridge-'+$Version+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'mods') | Out-Null
Copy-Item -LiteralPath (Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar") -Destination (Join-Path $taskRoot 'mods')
Copy-Item -LiteralPath (Join-Path $taskMods 'fabric-api-0.155.3+26.1.2.jar') -Destination (Join-Path $taskRoot 'mods')
# Installed Kotlin's nested library jars are dependencies, copied only to the test directory.
$taskKotlinFiles=@(Get-ChildItem -LiteralPath $taskMods -File -Filter 'fabric-language-kotlin-*.jar')
if($taskKotlinFiles.Count -ne 1){throw 'Expected exactly one installed Fabric Kotlin dependency; do not guess between versions'}
$taskKotlin=[IO.Compression.ZipFile]::OpenRead($taskKotlinFiles[0].FullName)
try {
    $taskMetadata=$taskKotlin.GetEntry('fabric.mod.json')
    if(!$taskMetadata){throw 'Kotlin dependency has no Fabric metadata'}
    $taskReader=[IO.StreamReader]::new($taskMetadata.Open())
    try{$taskKotlinId=($taskReader.ReadToEnd()|ConvertFrom-Json).id}finally{$taskReader.Dispose()}
    if($taskKotlinId -ne 'fabric-language-kotlin'){throw 'Unexpected dependency identity'}
    Write-Output ('TEST DEPENDENCY: '+$taskKotlinFiles[0].Name)
    foreach($taskEntry in $taskKotlin.Entries | Where-Object {$_.FullName -match '^META-INF/jars/.+\.jar$'}){
        $taskDestination=Join-Path $taskRoot ([IO.Path]::GetFileName($taskEntry.FullName))
        [IO.Compression.ZipFileExtensions]::ExtractToFile($taskEntry,$taskDestination)
        $taskCp+=';'+$taskDestination
    }
}finally{$taskKotlin.Dispose()}
if(!$VendorJar){$taskVendors=@(Get-ChildItem -LiteralPath $taskMods -File -Filter 'scoreboardoverhaul-*.jar');if($taskVendors.Count -ne 1){throw 'Expected one installed Overhaul, or name an exact VendorJar'};$VendorJar=$taskVendors[0].FullName}
$taskSource=[IO.Compression.ZipFile]::OpenRead($VendorJar)
$taskReader=[IO.StreamReader]::new($taskSource.GetEntry('fabric.mod.json').Open())
try{$taskVendorMetadata=$taskReader.ReadToEnd()|ConvertFrom-Json}finally{$taskReader.Dispose()}
if($taskVendorMetadata.id -ne 'scoreboard-overhaul' -or $taskVendorMetadata.version -notin @('1.4.3+mc26.1','1.5.1+mc26.1')){
    $taskSource.Dispose();throw 'Only separately inspected Overhaul 1.4.3/1.5.1 bytecode is allowed'
}
$taskClass=if($taskVendorMetadata.version -eq '1.5.1+mc26.1'){'TestOverhaul151BudgetBridge'}else{'TestOverhaulBudgetBridge'}
Write-Output ('TEST VENDOR: '+$VendorJar+'; version='+$taskVendorMetadata.version)
$taskFixture=[IO.Compression.ZipFile]::Open((Join-Path $taskRoot 'mods/overhaul-bytecode-fixture.jar'),[IO.Compression.ZipArchiveMode]::Create)
try {
    foreach($taskEntry in $taskSource.Entries | Where-Object {$_.FullName -match '\.class$'}){
        $taskInput=$taskEntry.Open();$taskOutput=$taskFixture.CreateEntry($taskEntry.FullName).Open()
        try{$taskInput.CopyTo($taskOutput)}finally{$taskInput.Dispose();$taskOutput.Dispose()}
    }
    $taskWriter=[IO.StreamWriter]::new($taskFixture.CreateEntry('fabric.mod.json').Open(),[Text.UTF8Encoding]::new($false))
    try{$taskWriter.Write((@{schemaVersion=1;id='scoreboard-overhaul';version=$taskVendorMetadata.version;name='TEST ONLY unmodified Overhaul classes';environment='client'}|ConvertTo-Json -Compress))}finally{$taskWriter.Dispose()}
}finally{$taskSource.Dispose();$taskFixture.Dispose()}
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-classpath' $taskCp '-d' $taskRoot (Join-Path $taskProject ('tests/'+$taskClass+'.java'))
if($LASTEXITCODE -ne 0){throw 'Bridge compilation failed'}
Push-Location $taskRoot
try {
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dmixin.debug.export=true' '-classpath' ($taskRoot+';'+$taskCp) $taskClass $taskRoot | Tee-Object -FilePath (Join-Path $taskRoot 'output.txt')
    if($LASTEXITCODE -ne 0){throw 'Actual Overhaul bytecode test failed'}
}finally{Pop-Location}
Write-Output "OVERHAUL BYTECODE VERIFIED: $taskRoot"
