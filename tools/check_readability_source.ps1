# Source-only audit: never packages a JAR or starts a Minecraft client.
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskJava = 'C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerification = Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Filter 'jar-verification-0.15.14-dev-*' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (!$taskVerification) { throw 'Missing verified dependency manifest' }
$taskManifest = [IO.File]::ReadAllLines((Join-Path $taskVerification.FullName 'TestChatNotifications-arguments.txt'))
$taskIndex = [Array]::IndexOf($taskManifest, '-cp')
if ($taskIndex -lt 0) { throw 'Missing classpath in dependency manifest' }
$taskLibraries = @($taskManifest[$taskIndex+1].Trim('"').Split(';') | Where-Object { $_ -notmatch '/build/(jar-verification|libs/WitherSkyRU)' })
foreach ($taskLibrary in $taskLibraries) { if (!(Test-Path -LiteralPath $taskLibrary)) { throw "Missing dependency: $taskLibrary" } }
$taskOutput = Join-Path $taskProject ('build/source-readability-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskOutput | Out-Null
$taskSources = @(Get-ChildItem -LiteralPath (Join-Path $taskProject 'src/main/java') -Filter '*.java' -Recurse | ForEach-Object FullName)
$taskSources += @(Get-ChildItem -LiteralPath (Join-Path $taskProject 'tests') -Filter '*.java' | ForEach-Object FullName)
$taskCompileArgs = @('--release','25','-encoding','UTF-8','-proc:none','-classpath',($taskLibraries -join ';'),'-d',$taskOutput) + $taskSources
$taskCompileFile = Join-Path $taskOutput 'compile-arguments.txt'
[IO.File]::WriteAllLines($taskCompileFile, @($taskCompileArgs | ForEach-Object { '"' + $_.Replace('\','/') + '"' }), [Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskCompileFile"
if ($LASTEXITCODE -ne 0) { throw 'Source compilation failed' }
$taskRuntime = (@($taskOutput,(Join-Path $taskProject 'src/main/resources')) + $taskLibraries) -join ';'
& "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -classpath $taskRuntime TestPerformanceCaches "$taskOutput/isolated-TestPerformanceCaches" | Tee-Object -FilePath (Join-Path $taskOutput 'TestPerformanceCaches-output.txt')
if ($LASTEXITCODE -ne 0) { throw 'Failed source check: TestPerformanceCaches' }
& "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -classpath $taskRuntime TestTooltipPlans63 "$taskOutput/isolated-TestTooltipPlans63" "$taskProject/translation-source" | Tee-Object -FilePath (Join-Path $taskOutput 'TestTooltipPlans63-output.txt')
if($LASTEXITCODE -ne 0){throw 'Failed source check: TestTooltipPlans63'}
& "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -classpath $taskRuntime TestScoreboardConsistency "$taskOutput/isolated-TestScoreboardConsistency" | Tee-Object -FilePath (Join-Path $taskOutput 'TestScoreboardConsistency-output.txt')
if ($LASTEXITCODE -ne 0) { throw 'Failed source check: TestScoreboardConsistency' }
foreach($taskDocumentCheck in @('TestRenderFastPaths62','TestUnknownWriter62','TestSackMenus61','TestNpcLookup61','TestNpcContinuations60','TestNumericHeadIndex59','TestEnchantmentBatch59','TestEnchantmentBatch58','TestEnchantBookFooter58','TestExperimentCards57','TestExperimentDisplay57','TestExperimentNotices57','TestHeadIndex56','TestSackUpgrades56','TestBagGuiLabels56','TestDisplayDates56','TestSacks55','TestSlayerCounters54','TestEnchantmentBatch54','TestSlayerScreenshot53','TestMinionFuel52','TestWinterEvent51','TestGuiScreenshots51','TestBestiaryScreenshots','TestEnchantmentBatch50','TestEnchantmentExpansion','TestReviewedLoreIndex','TestSafetyContinuation','TestSafetyScreenshot','TestEnchantmentParagraphs','TestCommissionExpansion','TestRomanceChain','TestRareCropNotice','TestUpgradeScreenshots','TestDocumentUpdate','TestOverlaySearch','TestGardenDocAdditions','TestGreenhouseTutorial','TestQuestDialogues','TestQuestInterfaces','TestStoryDialogues','TestStoryActions')){
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -classpath $taskRuntime $taskDocumentCheck "$taskOutput/isolated-$taskDocumentCheck" "$taskProject/translation-source" | Tee-Object -FilePath (Join-Path $taskOutput "$taskDocumentCheck-output.txt")
    if($LASTEXITCODE -ne 0){throw "Failed source check: $taskDocumentCheck"}
}
foreach ($taskClass in @('TestPlayerTextProtection','TestReorderedTitles','TestMiningInstructions','TestDurationLabels','TestNpcClarity','TestMiningNotifications','TestNpcRequests','TestNpcRequestLegacy','TestNpcDynamicInstructions','TestNpcHuntDirections','TestNpcMeetingReward','TestNpcPurchaseTotals','TestNpcAuctionClarity','TestForestNpcResults','TestForestRecords','TestNpcLoreClarity','TestNpcRelatedCards','TestForestBoosterCards','TestForestCombatCards','TestCommonWisdomBoosters','TestBoosterCrossRows','TestPlainBoosterContexts','TestArmorStoneReadability','TestSkillHeaders','TestCalendarLabels','TestMinionLabels','TestDungeonReadability','TestDungeonUiLabels','TestDungeonClassStats','TestDungeonAbilityProse','TestDungeonUtilityProse','TestDungeonBossProse','TestDungeonChatResources','TestDungeonProgress','TestDungeonCombat','TestMinionProse','TestMinionIntervals','TestPrimaryNames','TestPrimaryLabels','TestScreenshotProse','TestScreenshotLabels','TestDocumentProse','TestDocumentInterfaces','TestRankProse','TestReadableParagraphs','TestGardenParagraphs','TestSkillRequirements','TestNeighbourProse','TestPowerStones','TestCollectionRequirements','TestMiningArrowProse','TestMiningStyles','TestNumericLore','TestNpcNight','TestNpcCounters','TestNpcPets','TestNpcObserved','TestLatinCard','TestProofread','TestEditorialQuality','TestChatNotifications','TestAdditionalScreenshots','TestTooltipReadability','TestHudLayout','TestInteractionModes','TestRussianSearch','TestTooltipFont','TestForgedItems','TestArmorTooltip','TestJarDictionary014')) {
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -classpath $taskRuntime $taskClass "$taskOutput/isolated-$taskClass" "$taskProject/translation-source" | Tee-Object -FilePath (Join-Path $taskOutput "$taskClass-output.txt")
    if ($LASTEXITCODE -ne 0) { throw "Failed source check: $taskClass" }
}
& "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -classpath $taskRuntime AuditReadableProse $taskProject "$taskOutput/after-screen.json" 'source-proofread'
if ($LASTEXITCODE -ne 0) { throw 'Candidate screening failed' }
Write-Output "SOURCE-ONLY VERIFIED: $taskOutput"
