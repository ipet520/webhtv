[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repoRoot 'gradlew.bat'
$javaHome = 'E:\DevTools\jdk-21'
$androidSdk = 'E:\Android\Sdk'
$chaquopyPython = 'E:\DevTools\python-3.10\python.exe'
$gradleUserHome = 'E:\DevTools\GradleCache'
$pipCacheDir = Join-Path $gradleUserHome 'pip'

$requiredPaths = @(
    $gradleWrapper,
    (Join-Path $javaHome 'bin\java.exe'),
    $androidSdk,
    $chaquopyPython,
    $gradleUserHome
)
foreach ($path in $requiredPaths) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required build path not found: $path"
    }
}

$envNames = @(
    'JAVA_TOOL_OPTIONS',
    'JAVA_HOME',
    'ANDROID_HOME',
    'ANDROID_SDK_ROOT',
    'GRADLE_USER_HOME',
    'PIP_CACHE_DIR'
)
$previousEnvironment = @{}
foreach ($name in $envNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    [Environment]::SetEnvironmentVariable('JAVA_TOOL_OPTIONS', '-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp', 'Process')
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'Process')
    [Environment]::SetEnvironmentVariable('ANDROID_HOME', $androidSdk, 'Process')
    [Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $androidSdk, 'Process')
    [Environment]::SetEnvironmentVariable('GRADLE_USER_HOME', $gradleUserHome, 'Process')
    [Environment]::SetEnvironmentVariable('PIP_CACHE_DIR', $pipCacheDir, 'Process')

    if (-not (Test-Path -LiteralPath $pipCacheDir)) {
        New-Item -ItemType Directory -Path $pipCacheDir -Force | Out-Null
    }

    $pythonVersion = (& $chaquopyPython --version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $pythonVersion -notmatch '^Python 3\.10(?:\.|$)') {
        throw "Chaquopy requires Python 3.10; found: $pythonVersion"
    }

    Write-Output "JAVA_TOOL_OPTIONS_EFFECTIVE=$env:JAVA_TOOL_OPTIONS"
    Write-Output "JAVA_HOME_EFFECTIVE=$env:JAVA_HOME"
    Write-Output "ANDROID_HOME_EFFECTIVE=$env:ANDROID_HOME"
    Write-Output "ANDROID_SDK_ROOT_EFFECTIVE=$env:ANDROID_SDK_ROOT"
    Write-Output "GRADLE_USER_HOME_EFFECTIVE=$env:GRADLE_USER_HOME"
    Write-Output "PIP_CACHE_DIR_EFFECTIVE=$env:PIP_CACHE_DIR"
    Write-Output "CHAQUOPY_PYTHON=$chaquopyPython"
    Write-Output "CHAQUOPY_PYTHON_VERSION=$pythonVersion"

    $buildStarted = [DateTimeOffset]::Now
    Write-Output "BUILD_STARTED=$($buildStarted.ToString('yyyy-MM-dd HH:mm:ss zzz'))"

    Push-Location $repoRoot
    try {
        & $gradleWrapper `
            ':app:assembleMobileArm64_v8aRelease' `
            ':app:assembleLeanbackArm64_v8aRelease' `
            '--no-daemon' `
            '--console=plain'
        $gradleExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($gradleExitCode -ne 0) {
        throw "Gradle exited with code $gradleExitCode"
    }

    $buildFinished = [DateTimeOffset]::Now
    Write-Output "BUILD_FINISHED=$($buildFinished.ToString('yyyy-MM-dd HH:mm:ss zzz'))"

    $artifacts = @(
        [pscustomobject]@{
            Name = 'MOBILE_APK'
            Path = Join-Path $repoRoot 'app\build\outputs\apk\mobileArm64_v8a\release\mobile-arm64_v8a.apk'
        },
        [pscustomobject]@{
            Name = 'LEANBACK_APK'
            Path = Join-Path $repoRoot 'app\build\outputs\apk\leanbackArm64_v8a\release\leanback-arm64_v8a.apk'
        }
    )

    foreach ($artifact in $artifacts) {
        if (-not (Test-Path -LiteralPath $artifact.Path -PathType Leaf)) {
            throw "Expected APK not found: $($artifact.Path)"
        }
        $file = Get-Item -LiteralPath $artifact.Path
        $sha256 = (Get-FileHash -LiteralPath $artifact.Path -Algorithm SHA256).Hash.ToLowerInvariant()
        Write-Output "$($artifact.Name)=$($file.FullName)"
        Write-Output "$($artifact.Name)_TIME=$($file.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss zzz'))"
        Write-Output "$($artifact.Name)_SHA256=$sha256"
    }
}
finally {
    foreach ($name in $envNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
}
