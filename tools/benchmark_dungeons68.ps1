param([switch]$Reverse)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOldArgs=Get-Content "$taskProject/build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt"
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskRoot=Join-Path $taskProject ('build/dungeons-benchmark68-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskNew="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskNew)+$taskLibraries)-join ';') '-d' $taskRoot "$taskProject/tests/BenchmarkDungeons68.java"
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskVersions=if($Reverse){@('0.15.68-dev','0.15.67-dev')}else{@('0.15.67-dev','0.15.68-dev')};$taskHashes=@{}
foreach($taskVersion in $taskVersions){$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskHashes[$taskVersion]=(Get-FileHash -LiteralPath $taskJar).Hash;$taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'; & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkDungeons68 "$taskRoot/config-$taskVersion" "$taskProject/translation-source" "$taskRoot/$taskVersion.json" | Tee-Object -FilePath "$taskRoot/$taskVersion-output.txt";if($LASTEXITCODE -ne 0){throw 'Benchmark failed'}}
[IO.File]::WriteAllText("$taskRoot/RUNTIME_HASHES.json",($taskHashes|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
Write-Output "DUNGEONS68 BENCHMARK ROOT: $taskRoot"
