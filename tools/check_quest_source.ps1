# Focused fresh compilation between substantive passes; no JAR or game launch.
$ErrorActionPreference='Stop'
$taskProject=Split-Path -Parent $PSScriptRoot
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskVerification=Get-ChildItem -LiteralPath (Join-Path $taskProject 'build') -Filter 'jar-verification-0.15.34-dev-*'|Sort-Object LastWriteTime -Descending|Select-Object -First 1
$taskManifest=[IO.File]::ReadAllLines((Join-Path $taskVerification.FullName 'TestChatNotifications-arguments.txt'))
$taskIndex=[Array]::IndexOf($taskManifest,'-cp')
$taskLibraries=@($taskManifest[$taskIndex+1].Trim('"').Split(';')|Where-Object{$_ -notmatch '/build/(jar-verification|libs/WitherSkyRU)'})
$taskOutput=Join-Path $taskProject ('build/quest-source-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskOutput|Out-Null
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $taskProject 'src/main/java') -Recurse -Filter '*.java'|ForEach-Object FullName)
$taskSources+=Join-Path $taskProject 'tests/TestQuestDialogues.java'
$taskSources+=Join-Path $taskProject 'tests/TestQuestInterfaces.java'
$taskSources+=Join-Path $taskProject 'tests/TestStoryDialogues.java'
$taskSources+=Join-Path $taskProject 'tests/TestStoryActions.java'
$taskArgs=@('--release','25','-encoding','UTF-8','-proc:none','-cp',($taskLibraries -join ';'),'-d',$taskOutput)+$taskSources
$taskArgumentFile=Join-Path $taskOutput 'compile-arguments.txt'
[IO.File]::WriteAllLines($taskArgumentFile,@($taskArgs|ForEach-Object{'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" "@$taskArgumentFile"
if($LASTEXITCODE -ne 0){throw 'Quest source compilation failed'}
$taskRuntime=(@($taskOutput,(Join-Path $taskProject 'src/main/resources'))+$taskLibraries)-join ';'
foreach($taskCheck in @('TestStoryActions','TestStoryDialogues','TestQuestInterfaces','TestQuestDialogues')){
    & "$taskJava/java.exe" '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $taskRuntime $taskCheck "$taskOutput/isolated-$taskCheck" "$taskProject/translation-source"|Tee-Object -FilePath (Join-Path $taskOutput "$taskCheck-output.txt")
    if($LASTEXITCODE -ne 0){throw "Quest source test failed: $taskCheck"}
}
Write-Output "QUEST SOURCE VERIFIED: $taskOutput"
