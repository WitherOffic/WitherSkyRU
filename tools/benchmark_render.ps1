param([string]$Version='0.15.44-dev',[switch]$Source)
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerify=Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Directory -Filter 'jar-verification-0.15.40-dev-*' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerify.FullName 'TestChatNotifications-arguments.txt'))
$taskLibraries=@($taskManifest[[Array]::IndexOf($taskManifest,'-cp')+1].Trim('"').Split(';') | Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|jar-verification)'})
$taskRoot=Join-Path $taskProject ('build/render-perf-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskBaseline=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.40-dev.jar'
$taskNew=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$Version.jar"
if($Source){$taskNew=(Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Directory -Filter 'source-readability-*'|Sort-Object LastWriteTime -Descending|Select-Object -First 1).FullName}
$taskCompileCp=(@($taskBaseline)+$taskLibraries)-join ';'
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' $taskCompileCp '-d' $taskRoot (Join-Path $taskProject 'tests/BenchmarkRenderPerformance.java')
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
foreach($taskLabel in @('baseline40',$Version)){
    $taskRuntime=@($taskRoot);if($taskLabel -eq 'baseline40'){$taskRuntime+=$taskBaseline}else{$taskRuntime+=$taskNew;if($Source){$taskRuntime+=Join-Path $taskProject 'src/main/resources'}}
    $taskCp=($taskRuntime+$taskLibraries)-join ';'
    & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkRenderPerformance (Join-Path $taskRoot $taskLabel) $taskLabel (Join-Path $taskRoot "$taskLabel.json")|Tee-Object -FilePath (Join-Path $taskRoot "$taskLabel-output.txt")
    if($LASTEXITCODE -ne 0){throw "Benchmark failed: $taskLabel"}
}
Write-Output "PERFORMANCE MEASURED: $taskRoot"
