# Desktop UI adaptation of Metrolist's visual patterns; see NOTICE.md.
. "$PSScriptRoot\Search.ps1"
$desktopNames = @('HomeNav','SearchNav','LibraryNav','PageTitle','Query','SearchGo','HomeView','SearchView','LibraryView','SearchStatus','SearchResults','SearchCancel','LibraryTracks','LibraryStatus','RefreshLibrary','PlayLocal','PlayAllLocal','QueueAllLocal','RecentEmpty','RecentTracks','ShowLibrary','SelectedTitle','SelectedSubtitle','DetailsExpander','MoodEnergize','MoodWorkout','MoodRelax','MoodFeelGood','DiscoverPiano','DiscoverJazz','DiscoverAcoustic','QueuePanel','QueueList','QueueClose','QueueClear','QueueCountStatus')
foreach ($name in $desktopNames) { $ui[$name] = $window.FindName($name) }
$iconFile = [IO.File]::OpenRead((Join-Path $PSScriptRoot 'MetrolistIcons.xaml'))
try { $icons = [Windows.Markup.XamlReader]::Load($iconFile) } finally { $iconFile.Dispose() }
$window.Resources.MergedDictionaries.Add($icons)
$script:searchJob = $null
$script:searchOut = $null
$script:searchErr = $null
$script:searchStarted = $null

function Show-Page([string]$Page) {
    foreach ($name in @('Home','Search','Library')) {
        $ui["${name}View"].Visibility = if ($name -eq $Page) { 'Visible' } else { 'Collapsed' }
        $ui["${name}Nav"].Background = if ($name -eq $Page) { '#303F59' } else { '#223049' }
    }
    $window.FindName('SearchBar').Visibility = 'Visible'
    $ui.PageTitle.Text = $Page
    Update-ListsActiveRow
}

function Update-Library {
    $script:libraryRefreshing = $true
    $tracks = @()
    $libraryError = $null
    try {
        if (Test-Path -LiteralPath $ui.Folder.Text -PathType Container) {
            $tracks = @(Get-ChildItem -LiteralPath $ui.Folder.Text -File -ErrorAction Stop | Where-Object { $_.Extension.ToLowerInvariant() -in @('.mp3','.m4a','.opus','.flac','.wav') } | Sort-Object LastWriteTime -Descending | ForEach-Object {
                [pscustomobject]@{Title=($_.BaseName -replace '\s+\[[A-Za-z0-9_-]{11}\]$',''); Subtitle=($_.Extension.TrimStart('.').ToUpperInvariant() + ' • ' + [math]::Round($_.Length / 1MB, 1) + ' MB'); Duration=''; Artwork=$null; Bytes=$_.Length; Path=$_.FullName}
            })
        }
        $ui.LibraryStatus.Text = "$($tracks.Count) audio files in your selected folder. Select a track to play here."
    } catch { $libraryError = 'This folder could not be read. Choose another folder.' }
    foreach ($track in @($tracks | Select-Object -First 256)) { $track.Artwork = Get-StudioCover $track.Title }
    $script:libraryTracksCache = $tracks
    Apply-LibraryFilter
    if ($libraryError) { $ui.LibraryStatus.Text = $libraryError }
    $ui.RecentTracks.ItemsSource = @($tracks | Select-Object -First 6)
    Update-StudioLibrary
    $ui.HomeLibraryCount.Text = if ($tracks.Count -eq 1) { '1 track in your library' } else { "$($tracks.Count) tracks in your library" }
    $ui.RecentEmpty.Visibility = if ($tracks.Count) { 'Collapsed' } else { 'Visible' }
    $script:libraryRefreshing = $false
    Update-LocalCopyActions
    Update-ListsActiveRow
}

function Open-LocalTrack($Track, $Context = $null) {
    if ($null -ne $Context -and @($Context).Count -gt 0) {
        $script:playerQueue = @($Context)
        Start-PlayerTrack $Track $true
        if ($script:playback.isShuffled) { Rebuild-ShuffleOrder; Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text }
    } else { Start-PlayerTrack $Track }
}

function Stop-Search([string]$Message = 'Search cancelled.') {
    $searchTimer.Stop()
    if ($script:searchJob) {
        if (-not $script:searchJob.HasExited) { $script:searchJob.Kill(); $script:searchJob.WaitForExit(2000) | Out-Null }
        $script:searchJob.Dispose(); $script:searchJob = $null
    }
    Clear-YouTubeLease 'search'
    $ui.SearchGo.IsEnabled = $true; $ui.SearchCancel.IsEnabled = $false
    $ui.SearchStatus.Text = $Message
    Update-SearchPresentation
}

function Show-SearchResults([string]$Json) {
    $tracks = @(ConvertFrom-SearchResults $Json)
    $ui.SearchResults.ItemsSource = $tracks
    Update-LocalCopyActions
    Update-ListsActiveRow
    Update-SearchPresentation $(if ($tracks.Count) { 'ready' } else { 'empty' })
    $ui.SearchStatus.Text = if ($tracks.Count) { "$($tracks.Count) YouTube results • Double-click to preview · Save opens download settings." } else { 'No results found. Try a different song or artist name.' }
}

function Start-Search([switch]$Recommendations) {
    $window.FindName('SearchSuggestions').Visibility = 'Collapsed'
    $ui.SearchSuggestionPopup.IsOpen = $false
    $candidate = $ui.Query.Text.Trim()
    if (-not $Recommendations -and $candidate -and $candidate -notmatch '^https?://') {
        $script:desktopSearchHistory = @(@($candidate) + @($script:desktopSearchHistory | Where-Object { $_ -ne $candidate }) | Select-Object -First 20)
    }
    Show-Page 'Search'
    try {
        if (-not $Recommendations -and $ui.Query.Text.Trim() -match '^https?://') {
            $ui.Url.Text = ConvertTo-VideoUrl $ui.Query.Text
            $ui.SelectedTitle.Text = 'Video link selected'
            $ui.SelectedSubtitle.Text = 'Choose your format and download'
            $ui.SearchStatus.Text = 'Your video link is ready in the download panel.'
            Set-DownloadPanel $true
            return
        }
        $wait=Get-YouTubeRetryDelaySeconds $script:downloadRetryAfter
        if ($wait -gt 0) { throw "YouTube rate-limited the last request. Wait $wait seconds before trying again." }
        $exe = Find-Tool 'yt-dlp'
        if (-not $exe) { throw 'Run Setup.cmd to install the search tool.' }
        if ($script:searchJob) { Stop-Search }
        $arguments = if ($Recommendations) { Get-YouTubeRecommendationArguments } else { Get-SearchArguments $ui.Query.Text }
        $script:recommendationRequest = [bool]$Recommendations
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $exe; $info.Arguments = ($arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
        $info.UseShellExecute = $false; $info.CreateNoWindow = $true
        $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
        $info.StandardOutputEncoding = [Text.Encoding]::UTF8; $info.StandardErrorEncoding = [Text.Encoding]::UTF8
        $info.EnvironmentVariables['PYTHONIOENCODING'] = 'utf-8'
        $script:searchJob = [Diagnostics.Process]::Start($info)
        $searchTimer.Start()
        $script:searchOut = $script:searchJob.StandardOutput.ReadToEndAsync()
        $script:searchErr = $script:searchJob.StandardError.ReadToEndAsync()
        $script:searchStarted = [DateTime]::UtcNow
        $ui.SearchResults.ItemsSource = @()
        $ui.SearchStatus.Text = if ($Recommendations) { 'Loading your YouTube recommendations…' } else { 'Searching YouTube…' }
        Update-SearchPresentation 'loading'
        $ui.SearchGo.IsEnabled = $false; $ui.SearchCancel.IsEnabled = $true
    } catch { if (-not $script:searchJob) { Clear-YouTubeLease 'search' }; $ui.SearchStatus.Text = $_.Exception.Message; Update-SearchPresentation 'error' }
}

$searchTimer = New-Object Windows.Threading.DispatcherTimer
$searchTimer.Interval = [TimeSpan]::FromMilliseconds(300)
$searchTimer.Add_Tick({
    if (-not $script:searchJob) { return }
    if (([DateTime]::UtcNow - $script:searchStarted).TotalSeconds -gt 60) { Stop-Search 'Search timed out. Check your connection and try again.'; return }
    if ($script:searchJob.HasExited -and $script:searchOut.IsCompleted -and $script:searchErr.IsCompleted) {
        try {
            $output = $script:searchOut.GetAwaiter().GetResult()
            $errorOutput = $script:searchErr.GetAwaiter().GetResult()
            if ($script:searchJob.ExitCode -ne 0) {
                if ($errorOutput -match '(?i)429|too many requests') { $script:downloadRetryAfter=[DateTime]::UtcNow.AddSeconds(60) }
                Add-Log $errorOutput
                throw (Get-FriendlyError $errorOutput)
            }
            Show-SearchResults $output
            if ($script:recommendationRequest) { $ui.SearchStatus.Text = "$($ui.SearchResults.Items.Count) YouTube account recommendations. Preview here, or choose Save separately." }
        } catch { $ui.SearchStatus.Text = 'Search failed: ' + $_.Exception.Message; Update-SearchPresentation 'error' }
        $script:searchJob.Dispose(); $script:searchJob = $null
        Clear-YouTubeLease 'search'
        $searchTimer.Stop()
        $ui.SearchGo.IsEnabled = $true; $ui.SearchCancel.IsEnabled = $false
    }
})

$ui.HomeNav.Add_Click({ Show-Page 'Home'; Update-Library })
$ui.SearchNav.Add_Click({ Show-Page 'Search'; $ui.Query.Focus() | Out-Null })
$ui.LibraryNav.Add_Click({ Show-Page 'Library'; Update-Library })
$ui.ShowLibrary.Add_Click({ Show-Page 'Library'; Update-Library })
$ui.RefreshLibrary.Add_Click({ Update-Library })
$ui.SearchGo.Add_Click({ Start-Search })
$ui.Query.Add_KeyDown({ if ($_.Key -eq 'Return') { Start-Search } })
$ui.SearchCancel.Add_Click({ Stop-Search })

$ui.SearchResults.Add_SelectionChanged({
    if ($script:job) { $ui.SearchStatus.Text = 'Wait for the current download to finish before choosing another track.'; return }
    $track = $ui.SearchResults.SelectedItem
    if ($track) { $ui.Url.Text = $track.Url; $ui.SelectedTitle.Text = $track.Title; $ui.SelectedSubtitle.Text = $track.Subtitle }
})

$ui.LibraryTracks.Add_SelectionChanged({
    $ui.PlayLocal.IsEnabled = $null -ne $ui.LibraryTracks.SelectedItem

})
$ui.PlayLocal.Add_Click({ Open-LocalTrack $ui.LibraryTracks.SelectedItem })
if ($ui.PlayAllLocal) {
    $ui.PlayAllLocal.Add_Click({
        if ($ui.LibraryTracks.ItemsSource -and @($ui.LibraryTracks.ItemsSource).Count -gt 0) {
            $first = @($ui.LibraryTracks.ItemsSource)[0]
            Open-LocalTrack $first
        }
    })
}
if ($ui.QueueAllLocal) {
    $ui.QueueAllLocal.Add_Click({
        if ($ui.LibraryTracks.ItemsSource) {
            foreach ($t in $ui.LibraryTracks.ItemsSource) { Enqueue-Track $t }
        }
    })
}

# Recent and library playback is explicit: double-click, Enter, or Play selected.
$ui.Folder.Add_LostKeyboardFocus({ Update-Library })
$ui.Browse.Add_Click({ Update-Library })

foreach ($pair in @(@('MoodEnergize','energetic music'),@('MoodWorkout','workout music'),@('MoodRelax','relaxing music'),@('MoodFeelGood','feel good music'),@('DiscoverPiano','quiet piano music'),@('DiscoverJazz','jazz session music'),@('DiscoverAcoustic','acoustic music'))) {
    $ui[$pair[0]].Tag = $pair[1]
    $ui[$pair[0]].Add_Click({ $ui.Query.Text = $this.Tag; Start-Search })
}
$window.Add_Closing({ if (-not $_.Cancel) { Stop-Search; $searchTimer.Stop() } })

. "$PSScriptRoot\Player.ps1"
. "$PSScriptRoot\Preview.ps1"
. "$PSScriptRoot\LocalCopies.ps1"

# Queue panel buttons
if ($ui.QueueClose) { $ui.QueueClose.Add_Click({ Toggle-QueueView }) }
if ($ui.QueueClear) { $ui.QueueClear.Add_Click({ Clear-Queue }) }
if ($ui.QueueList) {
    $ui.QueueList.AddHandler([Windows.Controls.Button]::ClickEvent, [Windows.RoutedEventHandler]{
        param($sender,$eventArgs)
        $button = $eventArgs.OriginalSource
        while ($button -and $button -isnot [Windows.Controls.Button]) { $button = [Windows.Media.VisualTreeHelper]::GetParent($button) }
        if (-not $button) { return }
        $data = $button.DataContext
        if (-not $data) { return }
        $eventArgs.Handled = $true
        switch ($button.Name) {
            'QueueRemove'   { Remove-FromQueue $data.RealIndex }
            'QueueMoveUp'   { Move-QueueTrack $data.RealIndex -1 }
            'QueueMoveDown' { Move-QueueTrack $data.RealIndex 1 }
        }
    })
}

# Row action handlers for Library and Recent lists (Play Next and Enqueue)
$handleTrackRowClick = [Windows.RoutedEventHandler]{
    param($sender,$eventArgs)
    $button = $eventArgs.OriginalSource
    while ($button -and $button -isnot [Windows.Controls.Button]) { $button = [Windows.Media.VisualTreeHelper]::GetParent($button) }
    if (-not $button) { return }
    $track = $button.DataContext
    if (-not $track) { return }
    if ($button.Name -eq 'RowPlayNext') {
        $eventArgs.Handled = $true
        Play-NextInQueue $track
    } elseif ($button.Name -eq 'RowEnqueue') {
        $eventArgs.Handled = $true
        Enqueue-Track $track
    }
}
$ui.LibraryTracks.AddHandler([Windows.Controls.Button]::ClickEvent, $handleTrackRowClick)
$ui.RecentTracks.AddHandler([Windows.Controls.Button]::ClickEvent, $handleTrackRowClick)

# Global keyboard hotkeys
$window.Add_PreviewKeyDown({
    param($sender, $e)
    Handle-PlaybackGlobalKey $e
})

$ui['SelectedPreview'] = $window.FindName('SelectedPreview')
$ui.SelectedPreview.Add_Click({
    try {
        $url = ConvertTo-VideoUrl $ui.Url.Text
        $track = [pscustomobject]@{Id=$url.Substring($url.Length-11); Title=$ui.SelectedTitle.Text; Url=$url}
        Start-PreviewTrack $track
    } catch { $ui.Status.Text = 'Select a search result or enter a valid video link to preview.' }
})

$ui.SearchResults.AddHandler([Windows.Controls.Button]::ClickEvent, [Windows.RoutedEventHandler]{
    param($sender,$eventArgs)
    $button = $eventArgs.OriginalSource
    while ($button -and $button -isnot [Windows.Controls.Button]) { $button = [Windows.Media.VisualTreeHelper]::GetParent($button) }
    if ($button -and $button.Name -eq 'PreviewResult') {
        $eventArgs.Handled = $true
        if (-not $script:job) { $ui.SearchResults.SelectedItem = $button.DataContext }
        Start-PreviewTrack $button.DataContext
    }
    elseif ($button -and $button.Name -eq 'DownloadResult') {
        $eventArgs.Handled = $true
        if (-not $script:job) { $ui.SearchResults.SelectedItem = $button.DataContext }
        Set-DownloadPanel $true
    }
    elseif ($button -and $button.Name -eq 'LocalResult') {
        $eventArgs.Handled = $true
        Play-LocalCopy $button.DataContext.Id
    }
    elseif ($button -and $button.Name -eq 'SearchPlayNext') {
        $eventArgs.Handled = $true
        Play-NextInQueue $button.DataContext
    }
    elseif ($button -and $button.Name -eq 'SearchEnqueue') {
        $eventArgs.Handled = $true
        Enqueue-Track $button.DataContext
    }
})

. "$PSScriptRoot\DesktopUX.ps1"
. "$PSScriptRoot\DesktopRefinement.ps1"
. "$PSScriptRoot\WindowsMedia.ps1"
. "$PSScriptRoot\YouTubeConnection.ps1"
$window.FindName('OpenYouTubeVideo').Add_Click({
    $url='https://www.youtube.com/'
    try { $url=ConvertTo-VideoUrl $ui.Url.Text } catch { }
    Start-Process $url
})
Update-Library
Restore-PlaybackSession
Sync-StudioPlayback
