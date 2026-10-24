# Integration probe: official YouTube sample video, never invokes downloader.
$script:previewCheckStart = [DateTime]::UtcNow
$script:previewCheckStage = 'begin'
$script:previewCheckLast = ''
$script:localCopyTestDir = $null
if ($PlaybackTestFolder) {
    $script:localCopyTestDir = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-local-test-' + [guid]::NewGuid())
    [IO.Directory]::CreateDirectory($script:localCopyTestDir) | Out-Null
    Copy-Item -LiteralPath (Join-Path $PlaybackTestFolder 'Playback test.mp3') -Destination (Join-Path $script:localCopyTestDir 'Demo [M7lc1UVf-VE].mp3')
    $ui.Folder.Text = $script:localCopyTestDir
    $window.Add_Closed({
        $root = [IO.Path]::GetFullPath($script:localCopyTestDir)
        if ((Split-Path $root) -eq [IO.Path]::GetTempPath().TrimEnd('\') -and (Split-Path $root -Leaf) -like 'PocketMusic-local-test-*') { Remove-Item -LiteralPath $root -Recurse -Force }
    })
}
$window.Add_ContentRendered({
    $ui.PlayerVolume.Value = 0
    $track = [pscustomobject]@{Id='M7lc1UVf-VE';Title='YouTube IFrame API demo';Url='https://www.youtube.com/watch?v=M7lc1UVf-VE'}
    Start-PreviewTrack $track
    $script:previewCheckStage = 'playing'
})
$previewCheckTimer = New-Object Windows.Threading.DispatcherTimer
$previewCheckTimer.Interval = [TimeSpan]::FromMilliseconds(300)
$previewCheckTimer.Add_Tick({
    if ($script:playerState -ne $script:previewCheckLast) {
        Write-Host "Preview state: $script:playerState - $($ui.PlayerStatus.Text)"
        $script:previewCheckLast = $script:playerState
    }
    if ($script:playerState -eq 'error' -and $script:previewCheckStage -ne 'offline') { $previewCheckTimer.Stop(); Write-Host $script:log; throw ('Preview integration error: ' + $ui.PreviewStatus.Text) }
    if (([DateTime]::UtcNow - $script:previewCheckStart).TotalSeconds -gt 120) { $previewCheckTimer.Stop(); throw ('Preview test timed out: ' + $ui.PreviewStatus.Text) }
    switch ($script:previewCheckStage) {
        'playing' {
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt 0.5) { return }
            if ($ui.PlayerSeek.Maximum -le 1) { throw 'Missing stream duration' }
            $script:backgroundPosition = $script:previewPosition
            $script:backgroundBrowser = $script:previewBrowser
            $script:backgroundAt = [DateTime]::UtcNow
            $window.WindowState = 'Minimized'
            $script:previewCheckStage = 'minimized-playing'
        }
        'minimized-playing' {
            if (([DateTime]::UtcNow-$script:backgroundAt).TotalSeconds -lt 25) { return }
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt ($script:backgroundPosition+20) -or $script:previewBrowser.CoreWebView2.IsSuspended -or -not [object]::ReferenceEquals($script:backgroundBrowser,$script:previewBrowser)) { throw 'Preview did not keep playing while minimized' }
            $window.WindowState = 'Normal'
            Write-Host 'PASS: minimized preview advanced for 25 seconds without suspension, restart or watchdog error.'
            Toggle-Player
            $script:previewCheckStage = 'paused'
        }
        'paused' {
            if ($script:playerState -ne 'paused') { return }
            $script:modeTestBrowser = $script:previewBrowser
            $script:modeTestPosition = $script:previewPosition
            $script:modeTestToken = $script:previewToken
            Set-PreviewMode 'Video' $false
            $window.WindowState = 'Minimized'
            $script:modeTestTime = [DateTime]::UtcNow
            $script:previewCheckStage = 'mode-paused'
        }
        'mode-paused' {
            if (([DateTime]::UtcNow - $script:modeTestTime).TotalSeconds -lt 1) { return }
            if (-not [object]::ReferenceEquals($script:modeTestBrowser,$script:previewBrowser) -or $script:modeTestToken -ne $script:previewToken -or $script:playerState -ne 'paused' -or [math]::Abs($script:previewPosition-$script:modeTestPosition) -gt 0.5) { throw 'Mode switch disturbed paused playback' }
            $window.WindowState = 'Normal'
            Seek-Player 4
            $script:previewCheckStage = 'seeked'
        }
        'seeked' {
            if ([math]::Abs($script:previewPosition - 4) -gt 1) { return }
            Toggle-Player
            $script:previewCheckStage = 'resumed'
        }
        'resumed' {
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt 4.3) { return }
            $script:modeTestPosition = $script:previewPosition
            Set-PreviewMode 'Audio' $false
            $script:modeTestTime = [DateTime]::UtcNow
            $script:previewCheckStage = 'mode-playing'
        }
        'mode-playing' {
            if (([DateTime]::UtcNow - $script:modeTestTime).TotalSeconds -lt 1) { return }
            if (-not [object]::ReferenceEquals($script:modeTestBrowser,$script:previewBrowser) -or $script:modeTestToken -ne $script:previewToken -or $script:playerState -ne 'playing' -or $script:previewPosition -lt $script:modeTestPosition) { throw 'Mode switch disturbed playing preview' }
            Write-Host 'PASS: Audio/Video switches preserved the same browser, token, position and play/pause state.'
            Write-Host 'PASS: official IFrame preview played, paused, sought, resumed, and reported duration/position.'
            $script:playerQueue = @($script:playerTrack, [pscustomobject]@{Id='M7lc1UVf-VE';Title='Second preview queue entry';Url='https://www.youtube.com/watch?v=M7lc1UVf-VE'})
            $script:playerIndex = 0
            Step-Player 1
            $script:previewCheckStage = 'next-preview'
        }
        'next-preview' {
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt 0.2) { return }
            if ($script:playerIndex -ne 1 -or $ui.PlayerTitle.Text -ne 'Second preview queue entry') { throw 'Next preview failed' }
            Step-Player -1
            $script:previewCheckStage = 'previous-preview'
        }
        'previous-preview' {
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt 0.2) { return }
            if ($script:playerIndex -ne 0) { throw 'Previous preview failed' }
            Write-Host 'PASS: next and previous rebuilt the official player with the expected queue entry.'
            $script:previewBrowser.CoreWebView2.ExecuteScriptAsync("window.dispatchEvent(new Event('offline'))") | Out-Null
            $script:previewCheckStage = 'offline'
        }
        'offline' {
            if ($script:playerState -ne 'error') { return }
            if ($ui.PreviewStatus.Text -notmatch 'offline') { throw 'Offline message was not shown' }
            Write-Host 'PASS: simulated browser offline event reached the native error state.'
            if ($PlaybackTestFolder) {
                if (-not $ui.PreviewLocal.IsEnabled) { throw 'Downloaded copy action was not enabled' }
                $ui.PreviewLocal.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
                if ($script:previewBrowser -or $previewPane.Visibility -ne 'Collapsed') { throw 'Preview continued after local selection' }
                $script:previewCheckStage = 'local'
            } else { Stop-Preview; $previewCheckTimer.Stop(); $window.Close() }
        }
        'local' {
            if ($script:playerState -ne 'playing' -or $script:media.Position.TotalSeconds -lt 0.2) { return }
            Write-Host 'PASS: preview-to-local switch disposed browser and started local audio.'
            $script:backgroundAt = [DateTime]::UtcNow
            $script:backgroundPosition = $script:media.Position.TotalSeconds
            $window.WindowState = 'Minimized'
            $script:previewCheckStage = 'local-minimized'
        }
        'local-minimized' {
            if (([DateTime]::UtcNow-$script:backgroundAt).TotalSeconds -lt 2) { return }
            if ($script:playbackSource -ne 'local' -or $script:media.Position.TotalSeconds -lt ($script:backgroundPosition+1) -or $script:previewBrowser) { throw 'Local copy failed minimized playback' }
            $window.WindowState = 'Normal'
            Write-Host 'PASS: Play local copy used native audio, disposed video, and kept playing minimized.'
            $track = [pscustomobject]@{Id='M7lc1UVf-VE';Title='YouTube IFrame API demo';Url='https://www.youtube.com/watch?v=M7lc1UVf-VE'}
            Start-PreviewTrack $track
            if ($script:media -or $script:playerDecode) { throw 'Local playback continued under preview' }
            $script:previewCheckStage = 'preview-again'
        }
        'preview-again' {
            if ($script:playerState -ne 'playing' -or $script:previewPosition -lt 0.2) { return }
            Write-Host 'PASS: local-to-preview switch closed local media and started YouTube.'
            $state = $script:playerState
            Receive-PreviewMessage ([pscustomobject]@{token='stale';type='error';code='100'})
            if ($script:playerState -ne $state) { throw 'Stale preview event altered the current player' }
            foreach ($pair in @(@('100','unavailable'),@('101','disabled embedding'),@('150','disabled embedding'),@('153','identity'))) {
                Receive-PreviewMessage ([pscustomobject]@{token=$script:previewToken;type='error';code=$pair[0]})
                if ($ui.PreviewStatus.Text -notmatch $pair[1]) { throw ('Error message mismatch: ' + $pair[0]) }
            }
            Write-Host 'PASS: stale-event protection and simulated unavailable/embedding/identity errors.'
            Stop-Preview
            if ($script:previewBrowser -or $previewPane.Visibility -ne 'Collapsed') { throw 'Preview teardown failed' }
            Write-Host 'PASS: preview browser disposed on stop.'
            $previewCheckTimer.Stop(); $window.Close()
        }
    }
})
$previewCheckTimer.Start()
