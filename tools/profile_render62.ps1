param([string]$Version='0.15.61-dev')
# Offline CPU/allocation sampling only. Never attaches to or launches the game.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerify=Get-ChildItem (Join-Path $taskProject 'build') -Directory -Filter 'jar-verification-0.15.61-dev-*' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerify.FullName 'TestChatNotifications-arguments.txt'))
$taskLibraries=@($taskManifest[[Array]::IndexOf($taskManifest,'-cp')+1].Trim('"').Split(';')|Where-Object{$_ -notmatch '/build/(libs/WitherSkyRU|jar-verification)'})
$taskRoot=Join-Path $taskProject ('build/render-profile-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot | Out-Null
$taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar"
$taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d $taskRoot (Join-Path $taskProject 'tests/BenchmarkRenderHotPaths.java')
if($LASTEXITCODE -ne 0){throw 'Profile fixture compilation failed'}
$taskRecording=(Join-Path $taskRoot 'offline.jfr').Replace('\','/')
& "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' "-XX:StartFlightRecording=filename=$taskRecording,settings=profile,dumponexit=true,maxsize=128m,jdk.InitialEnvironmentVariable#enabled=false,jdk.InitialSystemProperty#enabled=false" '-Dwitherskyru.benchmark.warmup=20000' '-Dwitherskyru.benchmark.measure=4000' -cp $taskCp BenchmarkRenderHotPaths (Join-Path $taskRoot 'isolated') $Version (Join-Path $taskRoot 'timings-instrumented.json') | Tee-Object -FilePath (Join-Path $taskRoot 'instrumented-output.txt')
if($LASTEXITCODE -ne 0){throw 'Offline profile fixture failed'}
foreach($taskView in @('hot-methods','allocation-by-site','allocation-by-class','gc-pauses')){
    & "$taskJava/jfr.exe" view --width 180 --cell-height 4 $taskView $taskRecording | Tee-Object -FilePath (Join-Path $taskRoot "$taskView.txt")
    if($LASTEXITCODE -ne 0){throw "Profile view failed $taskView"}
}
Write-Output "OFFLINE PROFILE: $taskRoot; instrumented timings are not normal FPS measurements."
