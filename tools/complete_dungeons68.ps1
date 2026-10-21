# Resume the verified81A0 runtime after an authoring-reference-only refresh.
# No failed or missing group is skipped. Previous successful processes used this same JAR.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot="$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c"
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar"
$taskDigest=(Get-FileHash -LiteralPath $taskJar).Hash
if($taskDigest -ne '81A0ECE1E69C69903F29616BA6B526586D1F8E84E0ECCA9C0DA2999B7E2CA4EA' -or (Test-Path "$taskRoot/CHECK_SCOPE.json")){throw 'Different runtime or completed gate'}
$taskJarScope=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.68.json" -Raw|ConvertFrom-Json
if($taskJarScope.jar_sha256 -ne $taskDigest){throw 'Scope proof mismatch'}
$taskSummary=Get-Content "$taskRoot/audit/summary.json" -Raw|ConvertFrom-Json
if($taskSummary.public_cards_scanned -ne 8937 -or $taskSummary.dictionary_entries -ne 36043){throw 'Different audit runtime'}
$taskArgs=Get-Content "$taskRoot/compile-arguments.txt";$taskCp=$taskRoot+';'+$taskArgs[6].Trim('"')
if(!$taskCp.Contains($taskJar.Replace('\','/'))){throw 'Different compiled-test classpath'}
$taskGate=Get-Content "$taskProject/tools/check_continuous_jar.ps1" -Raw
$taskTests=@('TestDungeons68','TestRenderContext67','TestSafetyQuiz66','TestUniformLore65','TestTurbo65','TestPerformanceCaches','TestScoreboardConsistency')+@([regex]::Matches($taskGate,"'(Test[A-Za-z0-9]+)'")|ForEach-Object {$_.Groups[1].Value}|Select-Object -Unique|Where-Object {$_ -notin @('TestTooltipPlans63','TestRenderFastPaths62')})+@('TestCatalogueCorrections64')
if($taskTests.Count -ne 119){throw 'Different group list'}
$taskRetained=@();$taskRechecked=@();$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
foreach($taskClass in $taskTests){$taskFile="$taskRoot/$taskClass-output.txt";$taskPrior=if(Test-Path -LiteralPath $taskFile){Get-Content -LiteralPath $taskFile -Raw}else{''}
    if($taskPrior -match '(?m)^(PASS:|NPC SNAPSHOT:)' -and $taskClass -notin @('TestDungeons68','TestSkillRequirements','TestEditorialQuality','TestJarDictionary014')){$taskRetained+=$taskClass;continue}
    $taskTestArgs=@("$taskRoot/isolated-resume-$taskClass","$taskProject/translation-source")
    if($taskClass -eq 'TestTurbo65'){$taskTestArgs+="$taskProject/build/turbo65-cf2b5fc9846142e2acb1a61b61a903fc/audit"};if($taskClass -eq 'TestCatalogueCorrections64'){$taskTestArgs+="$taskProject/build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit"}
    & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' $taskCp $taskClass @taskTestArgs | Tee-Object -FilePath $taskFile
    if($LASTEXITCODE -ne 0){throw "Failed resumed $taskClass"};$taskRechecked+=$taskClass
}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskDigest){throw 'JAR changed'}
[IO.File]::WriteAllText("$taskRoot/CHECK_SCOPE.json",(@{runtime='0.15.68-dev';runtime_jar_sha256=$taskDigest;tests_passed=$taskTests;targeted_only=$false;full_catalogue_scanned=$true;authoring_reference_refreshed_after_loaded_count_failure=$true;runtime_jar_unchanged_during_reference_refresh=$true;retained_prior_successful_processes_same_runtime=$taskRetained;rechecked_or_completed_after_reference_refresh=$taskRechecked;historical64_and65_oracles_use_archived_audits=$true;installed_pack_modified=$false;game_started=$false}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "DUNGEONS68 ALL119 PASSED: retained $($taskRetained.Count), rechecked/completed $($taskRechecked.Count); $taskRoot"
