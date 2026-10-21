param([switch]$Reverse)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/turbo-benchmark65-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskOldArgs=Get-Content -LiteralPath (Join-Path $taskProject 'build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt')
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar'
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskJar)+$taskLibraries)-join ';') '-d' $taskRoot (Join-Path $taskProject 'tests/BenchmarkTurbo65.java')
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskVersions=if($Reverse){@('0.15.65-dev','0.15.64-dev')}else{@('0.15.64-dev','0.15.65-dev')}
$taskHashes=@{}
foreach($taskVersion in $taskVersions){$taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskHashes[$taskVersion]=(Get-FileHash -LiteralPath $taskJar).Hash;$taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'; & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkTurbo65 "$taskRoot/config-$taskVersion" "$taskProject/translation-source/TURBO_PRIMARY_0.15.65.json" "$taskRoot/$taskVersion.json" | Tee-Object -FilePath "$taskRoot/$taskVersion-output.txt";if($LASTEXITCODE -ne 0){throw 'Benchmark failed'}}
[IO.File]::WriteAllText((Join-Path $taskRoot 'RUNTIME_HASHES.json'),($taskHashes|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
Write-Output "TURBO BENCHMARK ROOT: $taskRoot"
