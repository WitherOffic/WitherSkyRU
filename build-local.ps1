param([string]$PrismRoot = (Join-Path ([Environment]::GetFolderPath('ApplicationData')) 'PrismLauncher'))
$ErrorActionPreference = 'Stop'
$jbin = Join-Path $PrismRoot 'java\java-runtime-epsilon\bin'
$libraryRoot = Join-Path $PrismRoot 'libraries'
$required = @(
    'com\mojang\minecraft\26.1.2\minecraft-26.1.2-client.jar',
    'net\fabricmc\fabric-loader\0.19.5\fabric-loader-0.19.5.jar',
    'net\fabricmc\sponge-mixin\0.17.4+mixin.0.8.7\sponge-mixin-0.17.4+mixin.0.8.7.jar'
)
$libraries = @($required | ForEach-Object {
    $file = Join-Path $libraryRoot $_
    if (!(Test-Path -LiteralPath $file)) { throw "Missing dependency: $file" }
    $file
})
# Use the actual version manifests, not every installed historical library.
# Old authlib versions have incompatible GameProfile methods.
$taskMinecraftMeta = Get-Content -LiteralPath (Join-Path $PrismRoot 'meta/net.minecraft/26.1.2.json') -Raw | ConvertFrom-Json
$libraries += @($taskMinecraftMeta.libraries | Where-Object {
    $taskAllowed = !$_.rules
    foreach ($taskRule in $_.rules) {
        if (!$taskRule.os -or $taskRule.os.name -eq 'windows') { $taskAllowed = $taskRule.action -eq 'allow' }
    }
    $taskAllowed -and $_.name -notmatch 'natives'
} | ForEach-Object {
    $taskArtifact = $_.downloads.artifact
    if ($taskArtifact.url) {
        $taskPath = Join-Path $libraryRoot ([Uri]$taskArtifact.url).AbsolutePath.TrimStart('/')
        if (!(Test-Path -LiteralPath $taskPath)) { throw "Missing Minecraft dependency: $taskPath" }
        $taskPath
    }
})
$taskFabricMeta = Get-Content -LiteralPath (Join-Path $PrismRoot 'meta/net.fabricmc.fabric-loader/0.19.5.json') -Raw | ConvertFrom-Json
$libraries += @($taskFabricMeta.libraries | ForEach-Object {
    $taskParts = $_.name.Split(':')
    $taskRelative = $taskParts[0].Replace('.','/') + '/' + $taskParts[1] + '/' + $taskParts[2] + '/' + $taskParts[1] + '-' + $taskParts[2] + '.jar'
    $taskPath = Join-Path $libraryRoot $taskRelative
    if (!(Test-Path -LiteralPath $taskPath)) { throw "Missing Fabric dependency: $taskPath" }
    $taskPath
})
$libraries = @($libraries | Select-Object -Unique)
$version = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'src\main\resources\fabric.mod.json') -Raw | ConvertFrom-Json).version
$className = "local-classes-$version-" + [Guid]::NewGuid().ToString('N')
$classes = Join-Path $PSScriptRoot (Join-Path 'build' $className)
$libsDir = Join-Path $PSScriptRoot 'build\libs'
New-Item -ItemType Directory -Path $classes,$libsDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Recurse -Filter *.java | ForEach-Object FullName)
$taskCompilerArguments = Join-Path $classes 'compiler-arguments.txt'
$taskCompilerLines = @('--release','25','-encoding','UTF-8','-proc:none','-classpath',
    ('"' + (($libraries -join ';').Replace('\','/')) + '"'),'-d',
    ('"' + $classes.Replace('\','/') + '"'))
$taskCompilerLines += @($sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' })
[System.IO.File]::WriteAllLines($taskCompilerArguments, $taskCompilerLines, [System.Text.UTF8Encoding]::new($false))
& (Join-Path $jbin 'javac.exe') ('@' + $taskCompilerArguments)
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
Copy-Item -Path (Join-Path $PSScriptRoot 'src\main\resources\*') -Destination $classes -Recurse -Force
$version = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'src\main\resources\fabric.mod.json') -Raw | ConvertFrom-Json).version
$jarPath = Join-Path $libsDir "WitherSkyRU-26.1.2-fabric-$version.jar"
# Compiler manifest is an intermediate build file, not a mod resource.
& (Join-Path $jbin 'jar.exe') --create --file $jarPath -C $classes ru -C $classes data -C $classes assets -C $classes META-INF -C $classes fabric.mod.json -C $classes witherskyru.mixins.json
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Write-Output $jarPath
