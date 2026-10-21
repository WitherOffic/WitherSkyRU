param([switch]$Reverse)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/catalogue-benchmark64-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskOldArgs=Get-Content -LiteralPath (Join-Path $taskProject 'build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt')
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.64-dev.jar'
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskJar)+$taskLibraries)-join ';') '-d' $taskRoot (Join-Path $taskProject 'tests/BenchmarkCatalogue64.java')
if($LASTEXITCODE -ne 0){throw 'Benchmark compilation failed'}
$taskVersions=if($Reverse){@('0.15.64-dev','0.15.62-dev')}else{@('0.15.62-dev','0.15.64-dev')}
foreach($taskVersion in $taskVersions){$taskJar=Join-Path $taskProject "build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskCp=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'; & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskCp BenchmarkCatalogue64 "$taskRoot/config-$taskVersion" "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7/catalogue.json" "$taskRoot/$taskVersion.json" | Tee-Object -FilePath "$taskRoot/$taskVersion-output.txt";if($LASTEXITCODE -ne 0){throw 'Benchmark failed'}}
Write-Output "CATALOGUE BENCHMARK ROOT: $taskRoot"
