$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskPrism = 'C:\Users\Wither\AppData\Roaming\PrismLauncher'
$taskJavaBin = Join-Path $taskPrism 'java\java-runtime-epsilon\bin'
$taskLog = 'C:\Users\Wither\.codex\attachments\f1b95b6c-22f2-4063-b1a9-4e82fe3a8108\Вставленный текст.txt'
$taskLibraries = @(Get-Content -LiteralPath $taskLog | ForEach-Object {
    $taskLine = $_.Trim()
    if ($taskLine.StartsWith('C:/Users/Wither/AppData/Roaming/PrismLauncher/libraries/') -and $taskLine.EndsWith('.jar') -and $taskLine -notmatch 'intermediary|natives-windows-(arm64|x86)/') {
        if (!(Test-Path -LiteralPath $taskLine)) { throw "Missing library: $taskLine" }
        $taskLine
    }
} | Select-Object -Unique)
$taskClasspath = $taskLibraries -join ';'
$taskVersion = (Get-Content -LiteralPath (Join-Path $taskProject 'src\main\resources\fabric.mod.json') -Raw | ConvertFrom-Json).version
$taskJar = Join-Path $taskProject "build\libs\WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
if (!(Test-Path -LiteralPath $taskJar)) { throw "Current sources have no compiled JAR. Compile only when requested: $taskJar" }
$taskTestClasses = Join-Path $taskProject 'build\editorial-tests-0.14.3'
$taskGameDir = Join-Path $taskProject ('build\editorial-transform-0.14.3-' + [Guid]::NewGuid().ToString('N'))
$taskModsDir = Join-Path $taskGameDir 'mods'
New-Item -ItemType Directory -Path $taskTestClasses,$taskModsDir -Force | Out-Null
Copy-Item -LiteralPath $taskJar -Destination $taskModsDir
$taskSources = @('TestChatNotifications.java','TestHudLayout.java','TestHudMixins.java','TestEditorialQuality.java','TestJarDictionary014.java') | ForEach-Object { Join-Path $taskProject ('tests\' + $_) }
& (Join-Path $taskJavaBin 'javac.exe') --release 25 -encoding UTF-8 -proc:none -classpath "$taskJar;$taskClasspath" -d $taskTestClasses $taskSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
foreach ($taskClass in @('TestChatNotifications','TestHudLayout','TestEditorialQuality','TestJarDictionary014')) {
    & (Join-Path $taskJavaBin 'java.exe') '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -classpath "$taskTestClasses;$taskJar;$taskClasspath" $taskClass (Join-Path $taskTestClasses 'empty-config') (Join-Path $taskProject 'translation-source')
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $taskClass" }
}
Push-Location $taskGameDir
try {
    & (Join-Path $taskJavaBin 'java.exe') '-Djava.awt.headless=true' '-Dmixin.debug.export=true' '-Dmixin.debug.countInjections=true' -classpath "$taskTestClasses;$taskClasspath" TestHudMixins $taskGameDir
    if ($LASTEXITCODE -ne 0) { throw 'Mixin transformation failed' }
} finally { Pop-Location }
Write-Output "TRANSFORM_TEST_DIRECTORY=$taskGameDir"
