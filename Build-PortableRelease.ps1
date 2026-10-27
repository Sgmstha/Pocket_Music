param([Parameter(Mandatory=$true)][string]$Destination,[Parameter(Mandatory=$true)][string]$ZipPath)
$ErrorActionPreference='Stop'
$destinationPath=[IO.Path]::GetFullPath($Destination)
$zip=[IO.Path]::GetFullPath($ZipPath)
if(Test-Path -LiteralPath $destinationPath){throw 'Choose a new empty package directory; existing folders are never overwritten.'}
if(Test-Path -LiteralPath $zip){throw 'The ZIP already exists; choose a new output path.'}
New-Item -ItemType Directory $destinationPath | Out-Null
# Explicit file types; runtime diagnostics, account data and development caches are not included.
foreach($file in Get-ChildItem -LiteralPath $PSScriptRoot -File){
    if($file.Name -eq 'LICENSE' -or ($file.Extension -in @('.ps1','.xaml','.cs','.cmd','.md','.ico') -and $file.Name -notmatch 'cookie|session\.bin|request-')){Copy-Item -LiteralPath $file.FullName -Destination $destinationPath}
}
foreach($file in @('PocketMusic.Media.dll','PocketMusic.Shell.dll')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $destinationPath}
foreach($directory in @('assets','preview','metrolist-source')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $directory) -Destination $destinationPath -Recurse}
New-Item -ItemType Directory (Join-Path $destinationPath 'tools') | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'tools\webview2') -Destination (Join-Path $destinationPath 'tools') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'tools\yt-dlp.exe') -Destination (Join-Path $destinationPath 'tools')
$compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if(-not (Test-Path -LiteralPath $compiler)){$compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'}
Push-Location $PSScriptRoot
try{& $compiler /nologo /target:winexe /reference:System.Windows.Forms.dll /win32icon:PocketMusic.ico ('/out:'+(Join-Path $destinationPath 'Pocket Music.exe')) PocketMusicLauncher.cs;if($LASTEXITCODE -ne 0){throw 'Launcher compilation failed.'}}finally{Pop-Location}
Compress-Archive -Path (Join-Path $destinationPath '*') -DestinationPath $zip -CompressionLevel Optimal
Write-Output $zip
