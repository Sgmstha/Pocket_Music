param([switch]$IncludeTests)
$ErrorActionPreference = 'Stop'
$framework = [Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()
$compiler = Join-Path $framework 'csc.exe'
$sdkRoot = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\UnionMetadata'
$metadata = Get-ChildItem -LiteralPath $sdkRoot -Filter Windows.winmd -Recurse | Where-Object { $_.Directory.Name -ne 'Facade' } | Sort-Object { [version]$_.Directory.Name } -Descending | Select-Object -First 1
if (-not $metadata) { throw 'Install the Windows 10/11 SDK to build the Windows media adapter.' }
$wpf = Join-Path $framework 'WPF'
$references = @($metadata.FullName, (Join-Path $framework 'System.Runtime.WindowsRuntime.dll'),  (Join-Path $wpf 'WindowsBase.dll'))
foreach ($name in @('System.Runtime','System.Threading.Tasks','System.ObjectModel','System.Runtime.InteropServices.WindowsRuntime')) {
    $references += [Reflection.Assembly]::Load("$name, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a").Location
}
Push-Location $PSScriptRoot
try {
    $arguments = @('/nologo','/target:library','/out:PocketMusic.Media.dll') + @($references | ForEach-Object { '/reference:' + $_ }) + @('WindowsMediaBridge.cs')
    & $compiler @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Windows media adapter build failed.' }
    if ($IncludeTests) {
        $testArguments = @('/nologo','/target:library','/out:PocketMusic.MediaTests.dll') + @($references | ForEach-Object { '/reference:' + $_ }) + @('WindowsMediaProbe.cs')
        & $compiler @testArguments
        if ($LASTEXITCODE -ne 0) { throw 'Windows media probe build failed.' }
    }
} finally { Pop-Location }
