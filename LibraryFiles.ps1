# GPL-3.0. Validate the exact file selected in the chosen library before asking Windows to recycle it.
function Resolve-LibraryFile([string]$Path,[string]$Folder) {
    if (-not $Path -or -not $Folder) { throw 'Choose a track from your music folder.' }
    $root=Get-Item -LiteralPath $Folder -Force -ErrorAction Stop
    $file=Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    if (-not $root.PSIsContainer -or $file.PSIsContainer -or ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) -or ($root.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Links and folders cannot be deleted from the library.' }
    if (-not [IO.Path]::GetFullPath($file.DirectoryName).TrimEnd('\').Equals([IO.Path]::GetFullPath($root.FullName).TrimEnd('\'),[StringComparison]::OrdinalIgnoreCase)) { throw 'This file is outside the selected music folder.' }
    if ($file.Extension.ToLowerInvariant() -notin @('.mp3','.m4a','.opus','.flac','.wav')) { throw 'This is not a supported library audio file.' }
    return $file.FullName
}
function Remove-LibraryFile([string]$Path,[string]$Folder,[scriptblock]$RecycleAction) {
    $validated=Resolve-LibraryFile $Path $Folder
    if ($RecycleAction) { & $RecycleAction $validated }
    else {
        Add-Type -AssemblyName Microsoft.VisualBasic
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile($validated,[Microsoft.VisualBasic.FileIO.UIOption]::AllDialogs,[Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin,[Microsoft.VisualBasic.FileIO.UICancelOption]::ThrowException)
    }
    if (Test-Path -LiteralPath $validated) { throw 'The file was not removed. Your library has been kept.' }
}
