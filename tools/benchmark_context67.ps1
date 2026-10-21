param([ValidateSet('preliminary','final')][string]$Phase='preliminary')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/context-benchmark67-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskOldArgs=Get-Content -LiteralPath (Join-Path $taskProject 'build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt')
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar'
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskJar)+$taskLibraries)-join ';') '-d' $taskRoot (Join-Path $taskProject 'tests/BenchmarkContext67.java')
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskHashes=@{};$taskIndex=0
foreach($taskVersion in @('0.15.66-dev','0.15.67-dev','0.15.67-dev','0.15.66-dev')){
    $taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskHashes[$taskVersion]=(Get-FileHash -LiteralPath $taskJar).Hash;$taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
    & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkContext67 "$taskRoot/$taskIndex-$taskVersion.json" | Tee-Object -FilePath "$taskRoot/$taskIndex-output.txt"
    if($LASTEXITCODE -ne 0){throw 'Context benchmark failed'};$taskIndex++
}
[IO.File]::WriteAllText((Join-Path $taskRoot 'RUNTIME_HASHES.json'),(@{phase=$Phase;hashes=$taskHashes;order=@('66','67','67','66')}|ConvertTo-Json -Depth 3),[Text.UTF8Encoding]::new($false))
Write-Output "CONTEXT67 BENCHMARK ROOT: $taskRoot"
