param([Parameter(Mandatory=$true)][string]$Version)
# Bytecode-only vendor fixture. Does not run Minecraft.main, Skyblocker.init,
# network, a launcher or a window, and never writes to the installed modpack.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskPrism='C:/Users/Wither/AppData/Roaming/PrismLauncher'
$taskJava=Join-Path $taskPrism 'java/java-runtime-epsilon/bin'
$taskVendor=Join-Path $taskPrism 'instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/skyblocker-6.10.4+26.1.2 (2).jar'
$taskVerification=Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Filter "jar-verification-$Version-*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if(!$taskVerification){throw 'Run baseline JAR verifier first'}
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerification.FullName 'TestChatNotifications-arguments.txt'))
$taskCp=$taskManifest[[Array]::IndexOf($taskManifest,'-cp')+1].Trim('"')
$taskRoot=Join-Path $taskProject ('build/skyblocker-bridge-'+$Version+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'mods') | Out-Null
$taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar"
Copy-Item -LiteralPath $taskJar -Destination (Join-Path $taskRoot 'mods')
Copy-Item -LiteralPath (Join-Path $taskPrism 'instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/fabric-api-0.155.3+26.1.2.jar') -Destination (Join-Path $taskRoot 'mods')
$taskSource=[IO.Compression.ZipFile]::OpenRead($taskVendor)
$taskFixture=[IO.Compression.ZipFile]::Open((Join-Path $taskRoot 'mods/skyblocker-bytecode-fixture.jar'),[IO.Compression.ZipArchiveMode]::Create)
try {
    # Unmodified class bytes; only fixture metadata omits unrelated entrypoints,
    # foreign mixins and dependencies which are not part of this predicate test.
    foreach($taskEntry in $taskSource.Entries | Where-Object {$_.FullName -match '\.class$'}){
        $taskInput=$taskEntry.Open();$taskOutput=$taskFixture.CreateEntry($taskEntry.FullName).Open()
        try{$taskInput.CopyTo($taskOutput)}finally{$taskInput.Dispose();$taskOutput.Dispose()}
    }
    $taskMetadata=$taskFixture.CreateEntry('fabric.mod.json').Open();$taskWriter=[IO.StreamWriter]::new($taskMetadata,[Text.UTF8Encoding]::new($false))
    try{$taskWriter.Write('{"schemaVersion":1,"id":"skyblocker","version":"6.10.4+26.1.2","name":"TEST ONLY unmodified Skyblocker classes","environment":"client"}')}finally{$taskWriter.Dispose()}
}finally{$taskSource.Dispose();$taskFixture.Dispose()}
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-classpath' $taskCp '-d' $taskRoot (Join-Path $taskProject 'tests/TestSkyblockerSearchBridge.java')
if($LASTEXITCODE -ne 0){throw 'Bridge test compilation failed'}
Push-Location $taskRoot
try {
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dmixin.debug.export=true' '-classpath' ($taskRoot+';'+$taskCp) TestSkyblockerSearchBridge $taskRoot | Tee-Object -FilePath (Join-Path $taskRoot 'output.txt')
    if($LASTEXITCODE -ne 0){throw 'Actual Skyblocker predicate test failed'}
}finally{Pop-Location}
Write-Output "SKYBLOCKER BYTECODE VERIFIED: $taskRoot"
