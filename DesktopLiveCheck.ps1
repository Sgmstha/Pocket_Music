# Invoked in the desktop UI by -LiveSearchTest. Retrieves metadata only.
$window.Add_ContentRendered({
    $ui.Query.Text = 'Creative Commons piano music'
    Start-Search
    if (-not $script:searchJob) { throw 'Search process did not start' }
    Stop-Search
    if ($script:searchJob -or -not $ui.SearchGo.IsEnabled) { throw 'Search cancellation failed' }
    Start-Search
})
$liveTimer = New-Object Windows.Threading.DispatcherTimer
$liveTimer.Interval = [TimeSpan]::FromMilliseconds(500)
$liveTimer.Add_Tick({
    if ($script:searchJob) { return }
    $liveTimer.Stop()
    if ($ui.SearchResults.Items.Count -eq 0) { throw ('Live search failed: ' + $ui.SearchStatus.Text) }
    $ui.SearchResults.SelectedIndex = 0
    if ($ui.Url.Text -notmatch '^https://www.youtube.com/watch\?v=') { throw 'Live result selection failed' }
    Write-Host "PASS: live desktop search, cancellation, and result selection ($($ui.SearchResults.Items.Count) results). No audio downloaded."
    $window.Close()
})
$liveTimer.Start()
