$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskJava = 'C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskLog = 'C:/Users/Wither/.codex/attachments/f1b95b6c-22f2-4063-b1a9-4e82fe3a8108/Вставленный текст.txt'
$taskLibraries = @(Get-Content -LiteralPath $taskLog | ForEach-Object {
    $taskLine = $_.Trim()
    if ($taskLine.StartsWith('C:/Users/Wither/AppData/Roaming/PrismLauncher/libraries/') -and $taskLine.EndsWith('.jar') -and $taskLine -notmatch 'intermediary|natives-windows-(arm64|x86)/') { $taskLine }
} | Select-Object -Unique)
$taskOldJar = Join-Path $taskProject 'build/libs/WitherSkyRU-26.1.2-fabric-0.14.3-dev.jar'
$taskClasses = Join-Path $taskProject 'build/source-tests-0.15'
New-Item -ItemType Directory -Path $taskClasses -Force | Out-Null
$taskCompilePath = (@($taskOldJar)+$taskLibraries) -join ';'
$taskSources = @('src/main/java/ru/wither/skyru/OfflineDictionary.java','src/main/java/ru/wither/skyru/TranslationGrammar.java','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','tests/TestEditorialQuality.java','tests/TestChatNotifications.java','tests/TestHudLayout.java','tests/TestJarDictionary014.java') | ForEach-Object { Join-Path $taskProject $_ }
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -classpath $taskCompilePath -d $taskClasses $taskSources
if ($LASTEXITCODE -ne 0) { throw 'Source checks compilation failed' }
# Current source classes/resources take priority over old, unchanged helper classes.
$taskRuntimePath = "$taskClasses;$taskProject/src/main/resources;$taskCompilePath"
foreach ($taskClass in @('TestEditorialQuality','TestChatNotifications','TestHudLayout','TestJarDictionary014')) {
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -classpath $taskRuntimePath $taskClass "$taskClasses/empty-config" "$taskProject/translation-source"
    if ($LASTEXITCODE -ne 0) { throw "Source check failed: $taskClass" }
}
# No Minecraft main, Fabric launch, network, installed mod writes or JAR packaging.
