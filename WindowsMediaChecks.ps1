# Real native media-session + thumbnail command integration, using generated audio only.
if (-not (Test-Path -LiteralPath $PlaybackTestFolder -PathType Container)) { throw 'Provide the generated audio folder with -PlaybackTestFolder.' }
[Reflection.Assembly]::Load([IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'PocketMusic.MediaTests.dll'))) | Out-Null
$script:mediaControlChecks = 0
$script:mediaControlStage = 'start'
$script:mediaControlStarted = [DateTime]::UtcNow
$script:mediaControlTask = $null
$script:mediaControlNonce = [guid]::NewGuid().ToString('N')
$script:playbackSessionPath = Join-Path ([IO.Path]::GetTempPath()) ("PocketMusic-media-$script:mediaControlNonce.json")
$script:mediaControlFirst = [pscustomobject]@{Title="Pocket Music transport test $script:mediaControlNonce"; Subtitle='Generated fixture'; Path=(Join-Path $PlaybackTestFolder 'Playback test.mp3'); Artwork=([uri](Join-Path $PSScriptRoot 'assets\home-studio.png')).AbsoluteUri; Duration='0:08'}
$script:mediaControlSecond = [pscustomobject]@{Title="Pocket Music next test $script:mediaControlNonce"; Subtitle='Generated fixture'; Path=(Join-Path $PlaybackTestFolder 'Playback test.m4a'); Artwork=([uri](Join-Path $PSScriptRoot 'assets\app-icon.png')).AbsoluteUri; Duration='0:08'}
function Check-MediaControl($Condition,[string]$Message) { if (-not $Condition) { throw $Message }; $script:mediaControlChecks++ }
function Start-ControlProbe([string]$Title) { $script:mediaControlTask = [PocketMusic.Tests.MediaProbe]::Read($Title) }
function Send-ControlProbe([string]$Title,[string]$Action) { $script:mediaControlTask = [PocketMusic.Tests.MediaProbe]::Command($Title,$Action) }
$window.Add_ContentRendered({
    Check-MediaControl ($null -ne $script:windowsMediaSession) ('SMTC not initialized: ' + $ui.Details.Text)
    Stop-AllCores
    $ui.PlayerVolume.Value = 0
    $script:playerQueue = @($script:mediaControlFirst,$script:mediaControlSecond)
    Start-PlayerTrack $script:mediaControlFirst $true
    $script:mediaControlStage = 'first-playing'
})
$mediaControlTimer = New-Object Windows.Threading.DispatcherTimer
$mediaControlTimer.Interval = [TimeSpan]::FromMilliseconds(150)
$mediaControlTimer.Add_Tick({
    if (([DateTime]::UtcNow-$script:mediaControlStarted).TotalSeconds -gt 65) { throw "Windows control test timed out in $script:mediaControlStage / $script:playerState / $($ui.Details.Text)" }
    if ($script:playerState -eq 'error') { throw $ui.PlayerStatus.Text }
    switch ($script:mediaControlStage) {
        'first-playing' {
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl ($script:thumbToggle.Description -eq 'Pause') 'Playing thumbnail does not show Pause'
            $window.WindowState = 'Minimized'
            $script:thumbToggle.Command.Execute($null,$window)
            Check-MediaControl ($script:playerState -eq 'paused' -and $script:thumbToggle.Description -eq 'Play') 'Minimized thumbnail Pause command failed'
            Check-MediaControl (-not $playerTimer.IsEnabled) 'Minimized UI polling was restarted by Windows integration'
            Start-ControlProbe $script:mediaControlFirst.Title
            $script:mediaControlStage = 'first-metadata'
        }
        'first-metadata' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            $snapshot = $script:mediaControlTask.GetAwaiter().GetResult()
            if (-not $snapshot) { Start-ControlProbe $script:mediaControlFirst.Title; return }
            if ($snapshot.ArtworkBytes -lt 1024) { Start-ControlProbe $script:mediaControlFirst.Title; return }
            Check-MediaControl ($snapshot.AppId -eq 'PocketMusic.Desktop' -and $snapshot.Title -eq $script:mediaControlFirst.Title) 'Windows media overlay exposes wrong app or title'
            Check-MediaControl ($snapshot.ArtworkBytes -gt 1024 -and $snapshot.ArtworkWidth -gt 0) 'Windows could not decode the track artwork from the media session'
            Check-MediaControl ($snapshot.Status -eq 'Paused' -and $snapshot.CanNext) 'Windows playback status or queue availability is stale'
            Send-ControlProbe $script:mediaControlFirst.Title 'play'
            $script:mediaControlStage = 'remote-play'
        }
        'remote-play' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            if (-not $script:mediaControlTask.GetAwaiter().GetResult()) { throw 'Windows rejected Play' }
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl $true 'Windows rejected Play'
            Check-MediaControl ($script:thumbToggle.Description -eq 'Pause') 'Windows Play did not update thumbnail icon'
            Send-ControlProbe $script:mediaControlFirst.Title 'pause'
            $script:mediaControlStage = 'remote-pause'
        }
        'remote-pause' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            if (-not $script:mediaControlTask.GetAwaiter().GetResult()) { throw 'Windows rejected Pause' }
            if ($script:playerState -ne 'paused') { return }
            Check-MediaControl $true 'Windows rejected Pause'
            Check-MediaControl ($script:thumbToggle.Description -eq 'Play') 'Windows Pause did not update thumbnail icon'
            Send-ControlProbe $script:mediaControlFirst.Title 'pause'
            $script:mediaControlStage = 'duplicate-pause'
        }
        'duplicate-pause' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            $script:mediaControlTask.GetAwaiter().GetResult() | Out-Null
            Check-MediaControl ($script:playerState -eq 'paused') 'Repeated Windows Pause accidentally resumed playback'
            $script:thumbNext.Command.Execute($null,$window)
            $script:mediaControlStage = 'second-playing'
        }
        'second-playing' {
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl ($script:playerTrack.Title -eq $script:mediaControlSecond.Title) 'Minimized thumbnail Next failed'
            $script:thumbToggle.Command.Execute($null,$window)
            Start-ControlProbe $script:mediaControlSecond.Title
            $script:mediaControlStage = 'second-metadata'
        }
        'second-metadata' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            $snapshot = $script:mediaControlTask.GetAwaiter().GetResult()
            if (-not $snapshot) { Start-ControlProbe $script:mediaControlSecond.Title; return }
            if ($snapshot.ArtworkBytes -gt 2048) { Start-ControlProbe $script:mediaControlSecond.Title; return }
            Check-MediaControl ($snapshot.Title -eq $script:mediaControlSecond.Title -and -not $snapshot.CanNext -and $snapshot.CanPrevious) 'Windows metadata/Next availability did not follow the new track'
            Send-ControlProbe $script:mediaControlSecond.Title 'previous'
            $script:mediaControlStage = 'remote-previous'
        }
        'remote-previous' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            if (-not $script:mediaControlTask.GetAwaiter().GetResult()) { throw 'Windows rejected Previous' }
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl $true 'Windows rejected Previous'
            Check-MediaControl ($script:playerTrack.Title -eq $script:mediaControlFirst.Title) 'Windows Previous failed'
            $script:thumbToggle.Command.Execute($null,$window)
            Send-ControlProbe $script:mediaControlFirst.Title 'next'
            $script:mediaControlStage = 'remote-next'
        }
        'remote-next' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            if (-not $script:mediaControlTask.GetAwaiter().GetResult()) { throw 'Windows rejected Next' }
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl $true 'Windows rejected Next'
            Check-MediaControl ($script:playerTrack.Title -eq $script:mediaControlSecond.Title) 'Windows Next failed'
            $script:thumbToggle.Command.Execute($null,$window)
            $script:thumbPrevious.Command.Execute($null,$window)
            $script:mediaControlStage = 'thumbnail-previous'
        }
        'thumbnail-previous' {
            if ($script:playerState -ne 'playing') { return }
            Check-MediaControl ($script:playerTrack.Title -eq $script:mediaControlFirst.Title) 'Minimized thumbnail Previous failed'
            $script:thumbToggle.Command.Execute($null,$window)
            Start-ControlProbe $script:mediaControlFirst.Title
            $script:mediaControlStage = 'finished'
        }
        'finished' {
            if (-not $script:mediaControlTask.IsCompleted) { return }
            $snapshot = $script:mediaControlTask.GetAwaiter().GetResult()
            if (-not $snapshot) { Start-ControlProbe $script:mediaControlFirst.Title; return }
            if ($snapshot.ArtworkBytes -lt 1024) { Start-ControlProbe $script:mediaControlFirst.Title; return }
            Check-MediaControl ($snapshot.Status -eq 'Paused' -and $snapshot.ArtworkBytes -gt 0 -and $script:windowsMediaSession.Title -eq $snapshot.Title) 'Final media overlay state is inconsistent'
            Check-MediaControl ($window.WindowState -eq 'Minimized') 'Transport buttons unexpectedly restored the window'
            $mediaControlTimer.Stop()
            Write-Host "PASS: $script:mediaControlChecks Windows transport checks: native session metadata/artwork, thumbnail controls and OS media commands while minimized."
            $window.Close()
        }
    }
})
$window.Add_Closed({ Remove-Item -LiteralPath $script:playbackSessionPath -ErrorAction SilentlyContinue })
$mediaControlTimer.Start()
