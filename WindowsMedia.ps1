# WPF thumbnail toolbar and SMTC adapter. All commands use the existing playback service.
$script:windowsMediaSession = $null
$script:windowsMediaFailureLogged = $false
$script:taskbarStateKey = ''
$script:taskbarIcons = @{}
foreach ($entry in @(
    @('play','M 7,3 L 20,12 L 7,21 Z'),
    @('pause','M 5,3 L 10,3 L 10,21 L 5,21 Z M 14,3 L 19,3 L 19,21 L 14,21 Z'),
    @('previous','M 3,3 L 6,3 L 6,21 L 3,21 Z M 21,3 L 7,12 L 21,21 Z'),
    @('next','M 18,3 L 21,3 L 21,21 L 18,21 Z M 3,3 L 17,12 L 3,21 Z')
)) {
    $drawing = New-Object Windows.Media.GeometryDrawing ([Windows.Media.Brushes]::White), $null, ([Windows.Media.Geometry]::Parse($entry[1]))
    $image = New-Object Windows.Media.DrawingImage $drawing
    $image.Freeze()
    $script:taskbarIcons[$entry[0]] = $image
}
$script:taskbarInfo = New-Object Windows.Shell.TaskbarItemInfo
$script:thumbPrevious = New-Object Windows.Shell.ThumbButtonInfo
$script:thumbToggle = New-Object Windows.Shell.ThumbButtonInfo
$script:thumbNext = New-Object Windows.Shell.ThumbButtonInfo
$script:thumbPrevious.Description = 'Previous'
$script:thumbPrevious.ImageSource = $script:taskbarIcons.previous
$script:thumbToggle.Description = 'Play'
$script:thumbToggle.ImageSource = $script:taskbarIcons.play
$script:thumbNext.Description = 'Next'
$script:thumbNext.ImageSource = $script:taskbarIcons.next
foreach ($button in @($script:thumbPrevious,$script:thumbToggle,$script:thumbNext)) {
    $button.DismissWhenClicked = $false
    $button.IsEnabled = $false
    $script:taskbarInfo.ThumbButtonInfos.Add($button)
}
$window.TaskbarItemInfo = $script:taskbarInfo
foreach ($pair in @(@($script:thumbPrevious,'previous'),@($script:thumbToggle,'toggle'),@($script:thumbNext,'next'))) {
    $command = New-Object Windows.Input.RoutedCommand
    $pair[0].Command = $command
    $pair[0].CommandTarget = $window
    $binding = New-Object Windows.Input.CommandBinding $command
    switch ($pair[1]) {
        'previous' {
            $binding.Add_Executed({ Invoke-WindowsMediaCommand 'previous'; $_.Handled = $true })
            $binding.Add_CanExecute({ $_.CanExecute = $ui.PlayerPrevious.IsEnabled; $_.Handled = $true })
        }
        'toggle' {
            $binding.Add_Executed({ Invoke-WindowsMediaCommand 'toggle'; $_.Handled = $true })
            $binding.Add_CanExecute({ $_.CanExecute = $ui.PlayerToggle.IsEnabled; $_.Handled = $true })
        }
        'next' {
            $binding.Add_Executed({ Invoke-WindowsMediaCommand 'next'; $_.Handled = $true })
            $binding.Add_CanExecute({ $_.CanExecute = $ui.PlayerNext.IsEnabled; $_.Handled = $true })
        }
    }
    $window.CommandBindings.Add($binding) | Out-Null
}
function Invoke-WindowsMediaCommand([string]$Command) {
    switch ($Command) {
        'previous' { if ($ui.PlayerPrevious.IsEnabled) { Step-PlaybackTrack -1 } }
        'next' { if ($ui.PlayerNext.IsEnabled) { Step-PlaybackTrack 1 } }
        'toggle' { if ($ui.PlayerToggle.IsEnabled) { Toggle-PlayPause } }
        'play' { if ($ui.PlayerToggle.IsEnabled -and -not $script:playback.isPlaying) { Toggle-PlayPause } }
        'pause' { if ($ui.PlayerToggle.IsEnabled -and $script:playback.isPlaying) { Toggle-PlayPause } }
        'stop' {
            Stop-AllCores
            $script:playerDesired = $false
            $script:playback.source = 'none'
            $script:playback.currentTrack = $null
            $script:playerTrack = $null
            $script:playback.position = 0; $script:playback.duration = 0
            Set-PlaybackUIState 'idle' 'Stopped'
        }
    }
}

function Sync-WindowsPlayback {
    $track = $script:playback.currentTrack
    $title = [string](Get-TrackProp $track 'Title' 'Pocket Music')
    $hasTrack = $null -ne $track -and $script:playback.source -ne 'none'
    $canToggle = [bool]$ui.PlayerToggle.IsEnabled
    $canPrevious = [bool]$ui.PlayerPrevious.IsEnabled
    $canNext = [bool]$ui.PlayerNext.IsEnabled
    $key = "$title|$($script:playback.isPlaying)|$canToggle|$canPrevious|$canNext"
    if ($key -ne $script:taskbarStateKey) {
        $script:taskbarStateKey = $key
        [Windows.Input.CommandManager]::InvalidateRequerySuggested()
        $script:thumbPrevious.IsEnabled = $canPrevious
        $script:thumbNext.IsEnabled = $canNext
        $script:thumbToggle.IsEnabled = $canToggle
        $script:thumbToggle.Description = if ($script:playback.isPlaying) { 'Pause' } else { 'Play' }
        $script:thumbToggle.ImageSource = if ($script:playback.isPlaying) { $script:taskbarIcons.pause } else { $script:taskbarIcons.play }
        $script:taskbarInfo.Description = if ($hasTrack) { "$title — Pocket Music" } else { 'Pocket Music' }
    }
    if (-not $script:windowsMediaSession) { return }
    $artist = if ($script:playback.source -eq 'preview') { [string](Get-TrackProp $track 'Subtitle' 'YouTube preview') } else { 'Local audio' }
    $artwork = [string](Get-TrackProp $track 'Artwork' '')
    $path = [string](Get-TrackProp $track 'Path' '')
    $id = [string](Get-TrackProp $track 'Id' '')
    if (-not $artwork -and $script:playback.source -eq 'preview' -and $id -match '^[A-Za-z0-9_-]{11}$') { $artwork = "https://i.ytimg.com/vi/$id/hqdefault.jpg" }
    try { $script:windowsMediaSession.Update($title,$artist,$artwork,$path,$script:playerState,$hasTrack,$canToggle,$canPrevious,$canNext) }
    catch {
        if (-not $script:windowsMediaFailureLogged) { Add-Log ('Windows media-session update failed: ' + $_.Exception.Message); $script:windowsMediaFailureLogged = $true }
    }
}

$window.Add_SourceInitialized({
    try {
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        [Reflection.Assembly]::Load([IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'PocketMusic.Media.dll'))) | Out-Null
        $callback = [Action[string]]{ param($command) Invoke-WindowsMediaCommand $command }
        $script:windowsMediaSession = New-Object PocketMusic.WindowsMediaBridge $script:desktopWindowHandle, $window.Dispatcher, $callback, (Join-Path $PSScriptRoot 'assets\app-icon.png')
        Sync-WindowsPlayback
    } catch { Add-Log ('Windows media controls unavailable: ' + $_.Exception.Message) }
})
$window.Add_Closing({
    if (-not $_.Cancel -and $script:windowsMediaSession) { $script:windowsMediaSession.Dispose(); $script:windowsMediaSession = $null }
})
Sync-WindowsPlayback
