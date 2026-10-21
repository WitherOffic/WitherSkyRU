param([string]$Version='0.15.44-dev',[string]$BaselineVersion='0.15.43-dev',[switch]$Reverse,[ValidateRange(1000,100000)][int]$WarmupBatches=1000,[ValidateRange(500,100000)][int]$MeasuredBatches=500)
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerify=Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Directory -Filter 'jar-verification-0.15.42-dev-*' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerify.FullName 'TestChatNotifications-arguments.txt'))
$taskLibraries=@($taskManifest[[Array]::IndexOf($taskManifest,'-cp')+1].Trim('"').Split(';') | Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|jar-verification)'})
$taskRoot=Join-Path $taskProject ('build/render-hotpaths-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskNew=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskNew)+$taskLibraries)-join ';') '-d' $taskRoot (Join-Path $taskProject 'tests/BenchmarkRenderHotPaths.java')
if($LASTEXITCODE -ne 0){throw 'Hot-path benchmark compilation failed'}
$taskOrder=if($Reverse){@($Version,$BaselineVersion)}else{@($BaselineVersion,$Version)}
foreach($taskLabel in $taskOrder){
    $taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$taskLabel.jar"
    $taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
    & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' "-Dwitherskyru.benchmark.warmup=$WarmupBatches" "-Dwitherskyru.benchmark.measure=$MeasuredBatches" '-cp' $taskCp BenchmarkRenderHotPaths (Join-Path $taskRoot $taskLabel) $taskLabel (Join-Path $taskRoot "$taskLabel.json") | Tee-Object -FilePath (Join-Path $taskRoot "$taskLabel-output.txt")
    if($LASTEXITCODE -ne 0){throw "Hot-path benchmark failed: $taskLabel"}
}
Write-Output "HOT PATHS MEASURED: $taskRoot"
