# Rebuild the optional startup helper. Without the DLL, startup compiles WindowsIdentity.cs.
$ErrorActionPreference = 'Stop'
$compiler = Join-Path ([Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()) 'csc.exe'
if (-not (Test-Path -LiteralPath $compiler)) { throw 'Run this script with Windows PowerShell 5.1 / .NET Framework.' }
Push-Location $PSScriptRoot
try {
    & $compiler /nologo /target:library /out:PocketMusic.Shell.dll WindowsIdentity.cs
    if ($LASTEXITCODE -ne 0) { throw 'Could not rebuild the identity helper. Close Pocket Music and try again.' }
} finally { Pop-Location }
