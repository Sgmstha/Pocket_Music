# Runs in -Desktop -SmokeTest. All filesystem operations target generated fixture files only.
$fixture=Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-refine-'+[guid]::NewGuid())
New-Item -ItemType Directory $fixture | Out-Null
$library=New-Item -ItemType Directory (Join-Path $fixture 'library')
$recycled=New-Item -ItemType Directory (Join-Path $fixture 'test-recycle')
$previousFolder=$ui.Folder.Text
$previousPlayback=$script:playback
$previousState=$script:playerState
function Save-RefinementSnapshot([string]$Name){
    $window.UpdateLayout();Refresh-StudioImages
    $bitmap=New-Object Windows.Media.Imaging.RenderTargetBitmap ([int]$window.Content.ActualWidth),([int]$window.Content.ActualHeight),96,96,([Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($window.Content);$encoder=New-Object Windows.Media.Imaging.PngBitmapEncoder;$encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap));$out=[IO.File]::Create((Join-Path (Split-Path $PreviewImage) $Name));try{$encoder.Save($out)}finally{$out.Dispose()}
}
try {
    $first=Join-Path $library.FullName 'Blue hour [1].mp3';$second=Join-Path $library.FullName 'Open water.wav';$outside=Join-Path $fixture 'outside.mp3';$unknown=Join-Path $library.FullName 'keep.txt'
    foreach($path in @($first,$second,$outside,$unknown)){[IO.File]::WriteAllBytes($path,[byte[]]@(1,2,3))}
    Verify ((Resolve-LibraryFile $first $library.FullName) -eq $first) 'Literal bracket filename validation failed'
    foreach($path in @($outside,$unknown,$library.FullName)){$rejected=$false;try{Resolve-LibraryFile $path $library.FullName | Out-Null}catch{$rejected=$true};Verify $rejected 'Deletion accepted a path outside the audio-file boundary'}
    $cancelled=$false;try{Remove-LibraryFile $first $library.FullName {param($path)throw [OperationCanceledException]::new('Test cancellation')}}catch [OperationCanceledException]{$cancelled=$true}
    Verify ($cancelled -and (Test-Path -LiteralPath $first)) 'Cancelled deletion changed the file'
    $script:playback=$previousPlayback.Clone();$script:playback.queue=[Collections.ArrayList]::new();$script:playback.shuffledOrder=[Collections.ArrayList]::new()
    $a=[pscustomobject]@{Title='Blue hour';Subtitle='Demo artist · MP3';Path=$first;Artwork=(Get-StudioCover 'Blue hour');Duration='3:20'}
    $b=[pscustomobject]@{Title='Open water';Subtitle='Demo artist · WAV';Path=$second;Artwork=(Get-StudioCover 'Open water');Duration='2:10'}
    $script:playback.queue.Add($a) | Out-Null;$script:playback.queue.Add($b) | Out-Null;$script:playback.currentTrack=$a;$script:playback.source='local';$script:playback.queueIndex=0;$script:playback.position=42;$script:playback.duration=200;$script:playback.isShuffled=$false
    $ui.Folder.Text=$library.FullName
    Update-Library
    $window.FindName('LibraryFileFormat').SelectedIndex=5
    Verify ($ui.LibraryTracks.Items.Count -eq 1 -and $ui.LibraryTracks.Items[0].Path -eq $second) 'WAV format filter failed'
    $window.FindName('LibraryFileFormat').SelectedIndex=0
    Verify ($ui.LibraryTracks.Items.Count -eq 2) 'Clearing the format filter omitted saved audio'
    Set-PlaybackUIState 'paused' 'Paused'
    $token=$script:playbackToken
    Set-FocusedPlayer $true
    Verify ($script:focusView.Visibility -eq 'Visible' -and $mainView.Visibility -eq 'Collapsed') 'Focused view did not replace the browsing UI'
    Verify ($script:playback.position -eq 42 -and $script:playbackToken -eq $token -and $script:playback.currentTrack -eq $a) 'Opening focused view restarted or replaced playback'
    Verify ($ui.FocusElapsed.Text -eq '0:42' -and $ui.FocusTitle.Text -eq 'Blue hour') 'Focused metadata/position did not synchronize'
    $ui.FocusSound.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.FocusVolume.Visibility -eq 'Visible') 'Sound did not reveal volume'
    $ui.FocusVolume.Value=.37
    Verify ([math]::Abs($script:playback.volume-.37) -lt .01) 'Focused volume did not control shared playback'
    if($PreviewImage){
        $window.Width=1280;$window.Height=800;Save-RefinementSnapshot 'desktop-focused-player.png'
        $window.Width=$window.MinWidth;$window.Height=$window.MinHeight;Save-RefinementSnapshot 'desktop-focused-compact.png'
        Verify ($script:focusView.Visibility -eq 'Visible' -and $ui.FocusToggle.ActualWidth -ge 44) 'Focused transport collapsed on the compact window'
    }
    Set-FocusedPlayer $false
    Verify ($mainView.Visibility -eq 'Visible' -and $script:playback.position -eq 42) 'Closing focused view changed playback'
    if($PreviewImage){$window.Width=1280;$window.Height=800;$ui.LibraryFilter.Clear();Show-Page 'Library';Update-Library;Save-RefinementSnapshot 'desktop-refined-library.png'}
    Remove-LibraryFile $second $library.FullName {param($path)Move-Item -LiteralPath $path -Destination (Join-Path $recycled.FullName ([IO.Path]::GetFileName($path)))}
    Remove-DeletedLibraryTrack $second
    Verify ($script:playback.currentTrack -eq $a -and $script:playback.queue.Count -eq 1) 'Deleting an upcoming file disturbed the current track'
    Remove-LibraryFile $first $library.FullName {param($path)Move-Item -LiteralPath $path -Destination (Join-Path $recycled.FullName ([IO.Path]::GetFileName($path)))}
    Remove-DeletedLibraryTrack $first
    Verify ($null -eq $script:playback.currentTrack -and $script:playback.queue.Count -eq 0 -and $script:playback.source -eq 'none') 'Deleted current track survived in playback state'
    Verify ((Test-Path -LiteralPath $outside) -and (Test-Path -LiteralPath $unknown)) 'Unselected files were changed'
} finally {
    Set-FocusedPlayer $false
    $script:playback=$previousPlayback;$script:playerState=$previousState;$ui.Folder.Text=$previousFolder
    foreach($directory in @($library.FullName,$recycled.FullName,$fixture)){if(Test-Path -LiteralPath $directory){Get-ChildItem -LiteralPath $directory -File | Remove-Item -Force;Remove-Item -LiteralPath $directory -ErrorAction SilentlyContinue}}
}
Write-Host "PASS: $script:testCount desktop checks including focused player and restricted file deletion."
