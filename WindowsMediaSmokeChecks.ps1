Verify ($null -ne $script:windowsMediaSession) ('Windows media session did not initialize: ' + $ui.Details.Text)
Verify ($window.TaskbarItemInfo.ThumbButtonInfos.Count -eq 3) 'Taskbar must have exactly three transport buttons'
Verify ($script:thumbPrevious.Description -eq 'Previous' -and $script:thumbNext.Description -eq 'Next') 'Taskbar transport order is incorrect'
$mediaSavedState = $script:playerState
$mediaSavedTrack = $script:playback.currentTrack
$mediaSavedSource = $script:playback.source
try {
    $script:playback.currentTrack = [pscustomobject]@{Title='Windows media smoke check'; Subtitle='Test artist'; Id='abcdefghijk'; Artwork=''}
    $script:playback.source = 'preview'
    Set-PlaybackUIState 'playing' 'Media test playing'
    Verify ($script:thumbToggle.Description -eq 'Pause' -and $script:thumbToggle.ImageSource -eq $script:taskbarIcons.pause) 'Thumbnail did not switch to Pause'
    Verify ($script:windowsMediaSession.Status -eq 'Playing' -and $script:windowsMediaSession.Title -eq 'Windows media smoke check') 'Windows media metadata/status did not synchronize'
    Verify ($script:windowsMediaSession.HasArtwork) 'Windows media overlay is missing fallback artwork'
    Set-PlaybackUIState 'paused' 'Media test paused'
    Verify ($script:thumbToggle.Description -eq 'Play' -and $script:thumbToggle.ImageSource -eq $script:taskbarIcons.play) 'Thumbnail did not switch to Play'
    Verify ($script:windowsMediaSession.Status -eq 'Paused') 'Windows media pause state is stale'
    $script:playback.currentTrack = $null; $script:playback.source = 'none'
    Set-PlaybackUIState 'idle' 'No track'
    Verify (-not $script:thumbToggle.IsEnabled -and -not $script:windowsMediaSession.Enabled) 'Idle Windows controls should be disabled'
    Verify (-not $script:thumbToggle.Command.CanExecute($null,$window)) 'Idle thumbnail command should not execute'
} finally {
    $script:playback.currentTrack = $mediaSavedTrack
    $script:playback.source = $mediaSavedSource
    Set-PlaybackUIState $mediaSavedState 'Select a track to play'
}
Write-Host "PASS: $script:testCount desktop checks including thumbnail toolbar and Windows media session."
