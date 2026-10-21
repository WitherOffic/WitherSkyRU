param([switch]$ScanOnly)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar=Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.65-dev.jar'
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
$taskPinned=Join-Path $taskProject 'build/item-audit64-a4de0b74b10243fba6713d30f70978f7'
$taskOldArgs=Get-Content -LiteralPath (Join-Path $taskProject 'build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt')
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskRoot=Join-Path $taskProject ('build/turbo65-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibraries)-join ';'),'-d',$taskRoot)+@(Get-ChildItem -LiteralPath (Join-Path $taskProject 'tests') -Filter '*.java'|ForEach-Object FullName)
$taskArgs=Join-Path $taskRoot 'compile-arguments.txt'
[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs"
if($LASTEXITCODE -ne 0){throw 'Packaged test compilation failed'}
$taskRuntime=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'audit')|Out-Null
& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskRuntime AuditItemCatalogue64 $taskPinned "$taskRoot/audit" | Tee-Object -FilePath "$taskRoot/audit-output.txt"
if($LASTEXITCODE -ne 0){throw 'Catalogue scan failed'}
Write-Output "TURBO CHECK ROOT: $taskRoot"
if($ScanOnly){return}
$taskFrozen=@('TestTooltipPlans63','TestRenderFastPaths62')
$taskGate=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'check_continuous_jar.ps1') -Raw
$taskTests=@('TestUniformLore65','TestTurbo65','TestPerformanceCaches','TestScoreboardConsistency')+@([regex]::Matches($taskGate,"'(Test[A-Za-z0-9]+)'")|ForEach-Object {$_.Groups[1].Value}|Select-Object -Unique|Where-Object {$_ -notin $taskFrozen})+@('TestCatalogueCorrections64')
foreach($taskClass in $taskTests){$taskTestArgs=@("$taskRoot/isolated-$taskClass","$taskProject/translation-source");if($taskClass -eq 'TestTurbo65'){$taskTestArgs+="$taskRoot/audit"};if($taskClass -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
    & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskRuntime $taskClass @taskTestArgs | Tee-Object -FilePath "$taskRoot/$taskClass-output.txt"
    if($LASTEXITCODE -ne 0){throw "Failed packaged check: $taskClass; root $taskRoot"}
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskDigest){throw 'JAR changed while tests ran'}
[IO.File]::WriteAllText((Join-Path $taskRoot 'CHECK_SCOPE.json'),(@{runtime='0.15.65-dev';runtime_jar_sha256=$taskDigest;tests_passed=$taskTests;frozen_optimization_oracles_inapplicable=$taskFrozen;historical64_differential_uses_archived64_audit=$true;current_full_catalogue_differential='TestTurbo65 against delivered64 outputs';all_live_states_verified=$false;game_started=$false;installed_pack_modified=$false}|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
Write-Output "ALL PACKAGED CHECKS PASSED: $($taskTests.Count); $taskRoot"
