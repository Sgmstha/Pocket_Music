# Integration test of the real WPF player, using generated audio only.
$script:playCheckCount = 0
$script:playCheckStage = 'start'
$script:playCheckIndex = 0
$script:playCheckAt = [DateTime]::UtcNow
$script:playCheckStarted = [DateTime]::UtcNow
$script:playCheckPosition = 0.0
$script:playCheckTracks = @()
function Assert-Playback($Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:playCheckCount++
}
$window.Add_ContentRendered({
    $ui.PlayerVolume.Value = 0
    $ui.Folder.Text = $PlaybackTestFolder
    Update-Library; Show-Page 'Library'
    foreach ($ext in @('.mp3','.m4a','.opus','.flac','.wav')) {
        $script:playCheckTracks += @($ui.LibraryTracks.ItemsSource | Where-Object { [IO.Path]::GetExtension($_.Path) -eq $ext })[0]
    }
    Assert-Playback ($script:playCheckTracks.Count -eq 5) 'Five fixture formats were not found'
    $ui.LibraryTracks.SelectedItem = $script:playCheckTracks[0]
    $ui.PlayLocal.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    $script:playCheckStage = 'playing'
})
$playCheckTimer = New-Object Windows.Threading.DispatcherTimer
$playCheckTimer.Interval = [TimeSpan]::FromMilliseconds(200)
$playCheckTimer.Add_Tick({
    if (([DateTime]::UtcNow - $script:playCheckStarted).TotalSeconds -gt 100) { throw "Playback test timed out: $script:playCheckStage / $script:playerState / $($ui.PlayerStatus.Text)" }
    if ($script:playerState -eq 'error' -and $script:playCheckStage -ne 'missing') { throw "Playback failed: $($ui.PlayerStatus.Text)" }
    switch ($script:playCheckStage) {
        'playing' {
            if ($script:playerState -ne 'playing' -or $script:media.Position.TotalSeconds -lt 0.3) { return }
            Assert-Playback ($script:media.NaturalDuration.TimeSpan.TotalSeconds -gt 7) 'Duration unavailable'
            Assert-Playback ($ui.PlayerTitle.Text -eq $script:playCheckTracks[$script:playCheckIndex].Title) 'Track title mismatch'
            $ui.PlayerVolume.Value = 0.25
            Assert-Playback ([math]::Abs($script:media.Volume - 0.25) -lt 0.01) 'Volume failed'
            $ui.PlayerVolume.Value = 0
            Toggle-Player
            Assert-Playback ($script:playerState -eq 'paused') 'Pause state failed'
            $script:playCheckPosition = $script:media.Position.TotalSeconds
            $script:playCheckAt = [DateTime]::UtcNow; $script:playCheckStage = 'paused'
        }
        'paused' {
            if (([DateTime]::UtcNow - $script:playCheckAt).TotalSeconds -lt 0.5) { return }
            Assert-Playback ([math]::Abs($script:media.Position.TotalSeconds - $script:playCheckPosition) -lt 0.2) 'Paused playback kept advancing'
            $native=$script:media;$token=$script:playbackToken
            Set-FocusedPlayer $true
            Assert-Playback ($script:media -eq $native -and $script:playbackToken -eq $token -and $script:playerState -eq 'paused') 'Focused view replaced native media or resumed a paused track'
            $ui.FocusSeek.Value=3
            $seekEvent=New-Object Windows.Input.MouseButtonEventArgs ([Windows.Input.Mouse]::PrimaryDevice),0,([Windows.Input.MouseButton]::Left)
            $seekEvent.RoutedEvent=[Windows.UIElement]::PreviewMouseLeftButtonUpEvent
            $ui.FocusSeek.RaiseEvent($seekEvent)
            $script:playCheckAt = [DateTime]::UtcNow; $script:playCheckStage = 'seek'
        }
        'seek' {
            if (([DateTime]::UtcNow - $script:playCheckAt).TotalSeconds -lt 0.4) { return }
            Assert-Playback ([math]::Abs($script:media.Position.TotalSeconds - 3) -lt 0.3) 'Seek failed'
            $ui.FocusToggle.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
            $script:playCheckStage = 'resume'
        }
        'resume' {
            if ($script:media.Position.TotalSeconds -lt 3.3) { return }
            Assert-Playback ($script:playerState -eq 'playing') 'Resume failed'
            Set-FocusedPlayer $false
            Assert-Playback ($script:playerState -eq 'playing') 'Closing focused player interrupted music'
            Write-Host ('PASS playback: ' + [IO.Path]::GetExtension($script:playerTrack.Path))
            $script:playCheckIndex++
            if ($script:playCheckIndex -lt 5) {
                $ui.LibraryTracks.SelectedItem = $script:playCheckTracks[$script:playCheckIndex]
                $ui.PlayLocal.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
                $script:playCheckStage = 'playing'
            } else {
                # A sidebar/shelf selection owns its queue, even if the main library has another order.
                $context = @($script:playCheckTracks[1], $script:playCheckTracks[0])
                Open-LocalTrack $context[0] $context
                Assert-Playback ($script:playback.queue[0].Path -eq $context[0].Path -and $script:playback.queue.Count -eq 2) 'Shelf playback did not preserve its collection order'
                Step-Player 1
                Assert-Playback ($script:playerTrack.Path -eq $script:playerQueue[1].Path) 'Next track failed'
                Step-Player -1
                Assert-Playback ($script:playerTrack.Path -eq $script:playerQueue[0].Path) 'Previous track failed'
                $script:playCheckStage = 'ending'
            }
        }
        'ending' {
            if ($script:playerState -ne 'playing') { return }
            Assert-Playback ($ui.NowTitle.Text -eq $script:playerTrack.Title -and $ui.NowToggle.Content -eq 'Pause') 'Now-playing panel is out of sync with native playback'
            Seek-Player ($ui.PlayerSeek.Maximum - 0.35)
            $script:playCheckStage = 'auto-next'
        }
        'auto-next' {
            if ($script:playerIndex -ne 1 -or $script:playerState -ne 'playing') { return }
            Assert-Playback ($script:playerTrack.Path -eq $script:playerQueue[1].Path) 'Automatic next failed'
            Seek-Player ($ui.PlayerSeek.Maximum - 0.35)
            $script:playCheckStage = 'queue-end'
        }
        'queue-end' {
            if ($script:playerState -ne 'ended') { return }
            Assert-Playback ($script:playerIndex -eq 1 -and -not $ui.PlayerNext.IsEnabled) 'Queue did not stop at the last track'
            Start-PlayerTrack ([pscustomobject]@{Title='Missing test';Path=(Join-Path $PlaybackTestFolder 'missing.mp3')})
            Assert-Playback ($script:playerState -eq 'error' -and $ui.PlayerStatus.Text -match 'missing') 'Missing-file error failed'
            $playCheckTimer.Stop()
            Write-Host "PASS: $script:playCheckCount playback checks. Muted generated audio; no live download."
            $window.Close()
        }
    }
})
$playCheckTimer.Start()
