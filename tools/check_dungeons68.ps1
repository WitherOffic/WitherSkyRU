param([switch]$TargetedOnly,[switch]$ScanOnly)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar"
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
$taskOldArgs=Get-Content "$taskProject/build/continuous-jar-0.15.62-dev-d5c6bf1987e9493d90e27f6ff830bd45/compile-arguments.txt"
$taskLibraries=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/(libs/WitherSkyRU|continuous-jar)'})
$taskRoot=Join-Path $taskProject ('build/dungeons68-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskCompile=@('--release','25','-encoding','UTF-8','-proc:none','-classpath',((@($taskJar)+$taskLibraries)-join ';'),'-d',$taskRoot)+@(Get-ChildItem "$taskProject/tests" -File -Filter '*.java'|ForEach-Object FullName)
$taskArgs="$taskRoot/compile-arguments.txt";[IO.File]::WriteAllLines($taskArgs,@($taskCompile|ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgs";if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$taskRuntime=(@($taskRoot,$taskJar)+$taskLibraries)-join ';'
if(!$TargetedOnly){& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' $taskRuntime AuditItemCatalogue64 "$taskProject/build/item-audit64-a4de0b74b10243fba6713d30f70978f7" "$taskRoot/audit" | Tee-Object -FilePath "$taskRoot/audit-output.txt";if($LASTEXITCODE -ne 0){throw 'Catalogue scan failed'}}
Write-Output "DUNGEONS68 CHECK ROOT: $taskRoot";if($ScanOnly){return}
$taskGate=Get-Content "$taskProject/tools/check_continuous_jar.ps1" -Raw
$taskFrozen=@('TestTooltipPlans63','TestRenderFastPaths62')
$taskTests=@('TestDungeons68','TestRenderContext67','TestSafetyQuiz66','TestUniformLore65','TestTurbo65','TestPerformanceCaches','TestScoreboardConsistency')+@([regex]::Matches($taskGate,"'(Test[A-Za-z0-9]+)'")|ForEach-Object {$_.Groups[1].Value}|Select-Object -Unique|Where-Object {$_ -notin $taskFrozen})+@('TestCatalogueCorrections64')
$taskPriority=@('TestDungeons68','TestSkillRequirements','TestNeighbourProse','TestPowerStones','TestCollectionRequirements','TestMiningArrowProse','TestMiningStyles','TestNumericLore','TestTooltipReadability','TestDungeonReadability','TestDungeonUiLabels','TestDungeonClassStats','TestDungeonAbilityProse','TestDungeonUtilityProse','TestDungeonBossProse','TestDungeonChatResources','TestDungeonProgress','TestDungeonCombat','TestQuestInterfaces')
$taskTests=$taskPriority+@($taskTests|Where-Object {$_ -notin $taskPriority})
if($TargetedOnly){$taskTests=@('TestDungeons68','TestRenderContext67','TestPerformanceCaches','TestScoreboardConsistency')}
foreach($taskClass in $taskTests){$taskTestArgs=@("$taskRoot/isolated-$taskClass","$taskProject/translation-source");if($taskClass -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"};if($taskClass -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
    & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskRuntime $taskClass @taskTestArgs | Tee-Object -FilePath "$taskRoot/$taskClass-output.txt"
    if($LASTEXITCODE -ne 0){throw "Failed $taskClass; $taskRoot"}
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskDigest){throw 'JAR changed during tests'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.68-dev';runtime_jar_sha256=$taskDigest;tests_passed=$taskTests;targeted_only=$TargetedOnly.IsPresent;full_catalogue_scanned=(!$TargetedOnly);historical64_and65_oracles_use_archived_audits=$true;installed_pack_modified=$false;game_started=$false}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output "DUNGEONS68 PASSED: $($taskTests.Count); $taskRoot"
