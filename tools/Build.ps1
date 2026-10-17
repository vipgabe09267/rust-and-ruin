param([switch]$Test)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    $portable = Get-ChildItem (Join-Path $projectRoot '.tools/java') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($portable) { $env:JAVA_HOME = $portable.FullName }
    elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install Java JDK 17 and set JAVA_HOME first.' }
}
Push-Location $projectRoot
try {
    $tasks = @('build', '--console', 'plain')
    if ($Test) { $tasks = @('build', 'runGameTest', '--console', 'plain') }
    # Windows PowerShell 5 treats javac warnings on stderr as errors when output is redirected.
    # Trust Gradle's exit code so a compiler warning cannot interrupt an otherwise valid build.
    $ErrorActionPreference = 'Continue'
    & (Join-Path $projectRoot 'gradlew.bat') @tasks
    $gradleExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($gradleExit -ne 0) { throw "Gradle failed with exit code $gradleExit" }
} finally { Pop-Location }
