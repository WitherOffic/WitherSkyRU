param([string]$Version='0.15.47-dev',[string]$BaselineVersion='0.15.46-dev')
# Offline API benchmark only, never launches a client or touches installed mods.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerify=Get-ChildItem (Join-Path $taskProject 'build') -Directory -Filter "jar-verification-$Version-*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$taskArgs=[IO.File]::ReadAllLines((Join-Path $taskVerify.FullName 'TestChatNotifications-arguments.txt'))
$taskLibs=@($taskArgs[[Array]::IndexOf($taskArgs,'-cp')+1].Trim('"').Split(';') | Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|jar-verification)'})
$taskOut=Join-Path $taskProject ('build/npc-receipt-perf-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskOut | Out-Null
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@(Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar")+$taskLibs)-join ';') -d $taskOut (Join-Path $taskProject 'tests/BenchmarkNpcRequests.java')
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
foreach($taskVersion in @($BaselineVersion,$Version)){
    $taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
    & "$taskJava/java.exe" -Xms256m -Xmx256m '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp ((@($taskOut,$taskJar)+$taskLibs)-join ';') BenchmarkNpcRequests (Join-Path $taskOut $taskVersion) $taskVersion (Join-Path $taskOut "$taskVersion.json") | Tee-Object -FilePath (Join-Path $taskOut "$taskVersion-output.txt")
    if($LASTEXITCODE -ne 0){throw 'Benchmark failed'}
}
Write-Output "NPC RECEIPT MEASURED: $taskOut"
