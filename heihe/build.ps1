[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$IdeHome
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$pluginRoot = [IO.Path]::GetFullPath($PSScriptRoot)

$javacPath = Join-Path $IdeHome 'jbr\bin\javac.exe'
$sdkPath = Join-Path $IdeHome 'lib'
$resourcesPath = Join-Path $pluginRoot 'src\main\resources'
$descriptorPath = Join-Path $resourcesPath 'META-INF\plugin.xml'
$resourceNames = @('META-INF/plugin.xml', 'web/theme.css', 'web/theme.js', 'web/scrollbars.css', 'icons/heihei.svg')
$sourcePaths = @(
    (Join-Path $pluginRoot 'src\main\java\cn\local\heihei\HeiHeiToolWindowFactory.java'),
    (Join-Path $pluginRoot 'src\main\java\cn\local\heihei\HeiHeiBrowserPanel.java')
)

foreach ($requiredPath in @($javacPath, $descriptorPath) + $sourcePaths) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required plugin build input does not exist: $requiredPath"
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $sdkPath 'app-client.jar') -PathType Leaf)) {
    throw "IntelliJ Platform 2024.1 SDK is missing: $sdkPath"
}

[xml]$descriptor = [IO.File]::ReadAllText($descriptorPath)
$version = [string]$descriptor.'idea-plugin'.version
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    throw "Unexpected plugin version: $version"
}

# 构建产物留在本项目内，每次使用独立目录，避免影响其它任务。
$buildRoot = Join-Path $pluginRoot ('.tmp\build\heihe\' + [Guid]::NewGuid().ToString('N'))
$classesPath = Join-Path $buildRoot 'classes'
$packagePath = Join-Path $buildRoot 'package'
$libraryPath = Join-Path $packagePath 'heihe\lib'
$distPath = Join-Path $pluginRoot 'dist'
$null = New-Item -ItemType Directory -Path $classesPath, $libraryPath, $distPath -Force

# 只编译本插件一个包中的两个源文件；SDK 直接读取现成 JAR，不下载、不编译依赖。
$compilerArgs = @(
    '--release', '17', '-encoding', 'UTF-8', '-g:none', '-proc:none', '-implicit:none',
    '-classpath', (Join-Path $sdkPath '*'), '-d', $classesPath
) + $sourcePaths
& $javacPath @compilerArgs
if ($LASTEXITCODE -ne 0) {
    throw "Plugin javac failed with exit code $LASTEXITCODE. Output directory: $buildRoot"
}
# 发布资源逐项列出，防止工作区、日志或个人配置意外进入安装包。
foreach ($resourceName in $resourceNames) {
    $resourceSource = Join-Path $resourcesPath $resourceName
    $resourceTarget = Join-Path $classesPath $resourceName
    if (-not (Test-Path -LiteralPath $resourceSource -PathType Leaf)) {
        throw "Required plugin resource is missing: $resourceName"
    }
    $null = New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($resourceTarget)) -Force
    Copy-Item -LiteralPath $resourceSource -Destination $resourceTarget
}

function Write-PluginArchive {
    <# 将相对路径写为标准 ZIP 分隔符，保证 JAR 和 IDE 安装包跨平台可读取。 #>
    param([string]$SourcePath, [string]$ArchivePath)
    $archive = [IO.Compression.ZipFile]::Open($ArchivePath, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($file in Get-ChildItem -LiteralPath $SourcePath -File -Recurse | Sort-Object FullName) {
            $entryName = $file.FullName.Substring($SourcePath.Length + 1).Replace('\', '/')
            $entry = $archive.CreateEntry($entryName, [IO.Compression.CompressionLevel]::Optimal)
            # 固定时间只作发布元数据，避免携带本机文件修改时间。
            $entry.LastWriteTime = [DateTimeOffset]::new(2000, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
            $sourceStream = [IO.File]::OpenRead($file.FullName)
            try {
                $entryStream = $entry.Open()
                try { $sourceStream.CopyTo($entryStream) }
                finally { $entryStream.Dispose() }
            }
            finally { $sourceStream.Dispose() }
        }
    }
    finally {
        $archive.Dispose()
    }
}

$jarPath = Join-Path $libraryPath 'heihe.jar'
Write-PluginArchive -SourcePath $classesPath -ArchivePath $jarPath
$stagedZip = Join-Path $buildRoot 'heihe.zip'
Write-PluginArchive -SourcePath $packagePath -ArchivePath $stagedZip
$outputPath = Join-Path $distPath 'heihe.zip'
Copy-Item -LiteralPath $stagedZip -Destination $outputPath -Force
Write-Output "Plugin package: $outputPath"
Write-Output "Build output: $buildRoot"
