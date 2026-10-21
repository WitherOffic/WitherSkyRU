$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskProject/build/performance67-416d3fe89d4848579abd66c716a5a057/compile-arguments.txt"
$taskLibraries=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskClasses="$taskProject/build/render-hotpaths-edcc9e196b70405eb84e4a905381e0bb"
$taskRoot=Join-Path $taskProject ('build/common-recheck67-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
foreach($taskMode in @('debug_pipeline','context63_churn','context63_oversized')){
    $taskIndex=0
    foreach($taskVersion in @('0.15.66-dev','0.15.67-dev','0.15.67-dev','0.15.66-dev')){
        $taskLabel="$taskMode-$taskIndex-$taskVersion";$taskIndex++
        $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
        $taskCp=(@($taskClasses,$taskJar)+$taskLibraries)-join ';'
        & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dwitherskyru.benchmark.warmup=100000' '-Dwitherskyru.benchmark.measure=20000' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' $taskCp BenchmarkRenderHotPaths "$taskRoot/$taskLabel" $taskVersion "$taskRoot/$taskLabel.json" | Tee-Object -FilePath "$taskRoot/$taskLabel-output.txt"
        if($LASTEXITCODE -ne 0){throw "Failed $taskLabel"}
    }
}
Write-Output "ISOLATED RECHECK ROOT: $taskRoot"
