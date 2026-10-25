# Run inside the real desktop window with -Desktop -SmokeTest.
$script:testCount = 0
function Verify($Condition, [string]$Message) { if (-not $Condition) { throw $Message }; $script:testCount++ }
Show-Page 'Search'
Verify ($ui.SearchView.Visibility -eq 'Visible' -and $ui.HomeView.Visibility -eq 'Collapsed') 'Search navigation failed'
Show-SearchResults '{"entries":[{"id":"abcdefghijk","title":"Test title","channel":"Test artist","duration":251},{"id":"abcdefghijk","title":"duplicate"},{"id":"bad-id","title":"invalid"},null]}'
Verify ($ui.SearchResults.Items.Count -eq 1) 'Search normalization/deduplication failed'
$ui.SearchResults.SelectedIndex = 0
Verify ($ui.Url.Text -eq 'https://www.youtube.com/watch?v=abcdefghijk') 'Selection did not populate download URL'
Verify ($ui.SelectedTitle.Text -eq 'Test title') 'Selection title failed'
Verify ($ui.SearchResults.SelectedItem.Duration -eq '4:11') 'Duration formatting failed'
Show-SearchResults '{"entries":[]}'
Verify ($ui.SearchStatus.Text -match 'No results') 'Empty search state failed'
$rejected = $false
try { Get-SearchArguments ' ' | Out-Null } catch { $rejected = $true }
Verify $rejected 'Empty query was accepted'
$arguments = @(Get-SearchArguments 'Artist "quoted" & title')
Verify ($arguments[-1] -eq 'ytsearch12:Artist "quoted" & title') 'Search query changed'
Verify ($arguments -contains '--ignore-config' -and $arguments -contains '--skip-download') 'Search isolation failed'
Show-Page 'Library'
Verify ($ui.LibraryView.Visibility -eq 'Visible') 'Library navigation failed'
Show-Page 'Home'
Verify ($ui.HomeView.Visibility -eq 'Visible') 'Home navigation failed'
$ui.Query.Text = 'https://youtu.be/abcdefghijk'
Start-Search
Verify ($ui.Url.Text -eq 'https://www.youtube.com/watch?v=abcdefghijk' -and $null -eq $script:searchJob) 'Direct-link search failed'
$originalPreview = (Get-Item Function:Start-PreviewTrack).ScriptBlock
try {
    Set-Item Function:script:Start-PreviewTrack -Value { param($Track, $KeepQueue) $script:previewEntryTrack = $Track }
    $script:previewEntryTrack = $null
    Show-SearchResults '{"entries":[{"id":"abcdefghijk","title":"Preview button test","channel":"Test artist","duration":251}]}'
    $ui.SearchResults.SelectedIndex = 0
    Verify ($null -eq $script:previewEntryTrack) 'Selecting a download result unexpectedly started preview'
    $ui.SelectedPreview.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:previewEntryTrack.Id -eq 'abcdefghijk') 'Selected-track Preview button failed'
    $script:previewEntryTrack = $null
    $window.UpdateLayout()
    function Find-PreviewTestButton($Visual) {
        if ($Visual -is [Windows.Controls.Button] -and $Visual.Name -eq 'PreviewResult') { return $Visual }
        for ($i=0; $i -lt [Windows.Media.VisualTreeHelper]::GetChildrenCount($Visual); $i++) {
            $found = Find-PreviewTestButton ([Windows.Media.VisualTreeHelper]::GetChild($Visual,$i))
            if ($found) { return $found }
        }
        return $null
    }
    $rowButton = Find-PreviewTestButton $ui.SearchResults
    Verify ($null -ne $rowButton) 'Search result Preview button was not rendered'
    $rowButton.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:previewEntryTrack.Id -eq 'abcdefghijk' -and $null -eq $script:job) 'Result Preview button failed or invoked download'
} finally { Set-Item Function:script:Start-PreviewTrack -Value $originalPreview }
$originalModePath = $script:previewModePath
$originalMode = $script:previewMode
$script:previewModePath = Join-Path ([IO.Path]::GetTempPath()) ('preview-mode-test-' + [guid]::NewGuid() + '.json')
try {
    Verify ((Get-PreviewMode) -eq 'Audio') 'Missing preference should default to Audio'
    $ui.Format.SelectedIndex = 1
    Verify ($ui.Format.SelectedItem.Content -eq 'MP4 (video)' -and $ui.Start.Content -eq 'Download video') 'MP4 download option missing'
    $ui.PreviewVideoMode.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ((Get-PreviewMode) -eq 'Video' -and $ui.PreviewHost.Width -eq 560 -and $ui.PreviewArtwork.Visibility -eq 'Collapsed') 'Video mode or persistence failed'
    $ui.PreviewAudioMode.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ((Get-PreviewMode) -eq 'Audio' -and $ui.PreviewHost.Height -ge 200 -and $ui.PreviewArtwork.Visibility -eq 'Visible') 'Audio mode or persistence failed'
    Verify ($ui.Format.SelectedItem.Content -eq 'MP4 (video)') 'Preview mode changed download format'
} finally {
    Remove-Item -LiteralPath $script:previewModePath -ErrorAction SilentlyContinue
    $script:previewModePath = $originalModePath
    Set-PreviewMode $originalMode $false
    $ui.Format.SelectedIndex = 0
}
$copyFolder = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-copy-check-' + [guid]::NewGuid())
$savedFolder = $ui.Folder.Text
$savedStartPlayer = (Get-Item Function:Start-PlayerTrack).ScriptBlock
try {
    [IO.Directory]::CreateDirectory($copyFolder) | Out-Null
    $ui.Folder.Text = $copyFolder
    Show-SearchResults '{"entries":[{"id":"abcdefghijk","title":"Matching track"},{"id":"abcdefghijK","title":"Matching track"}]}'
    $ui.SearchResults.SelectedIndex = 0
    [IO.File]::WriteAllText((Join-Path $copyFolder 'Matching track.mp3'),'fixture')
    [IO.File]::WriteAllText((Join-Path $copyFolder 'Matching track [abcdefghijk].mp4'),'fixture')
    Update-LocalCopyActions
    Verify (-not $ui.SelectedLocal.IsEnabled) 'Title-only and video files must not match local audio'
    $copyPath = Join-Path $copyFolder 'Matching track [abcdefghijk].mp3'
    [IO.File]::WriteAllText($copyPath,'fixture')
    Update-Library
    Verify ($ui.SelectedLocal.IsEnabled -and $ui.SearchResults.ItemsSource[0].LocalAvailable -and $null -eq (Find-LocalCopy 'abcdefghijK')) 'Local matching must use exact case-sensitive video ID'
    Set-Item Function:script:Start-PlayerTrack -Value { param($Track) $script:localCopySelected = $Track }
    $script:localCopySelected = $null
    $ui.SelectedLocal.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:localCopySelected.Path -eq $copyPath) 'Selected Play local copy did not use native player'
    Remove-Item -LiteralPath $copyPath
    $script:localCopySelected = $null
    Play-LocalCopy 'abcdefghijk'
    Verify ($null -eq $script:localCopySelected -and -not $ui.SelectedLocal.IsEnabled) 'Deleted file should not start playback'
    Set-PreviewMode 'Audio' $false
    Verify ($ui.PreviewHost.Width -eq 200 -and $ui.PreviewHost.Height -eq 200 -and [Windows.Controls.Grid]::GetRow($previewPane) -eq 0 -and $previewPane.HorizontalAlignment -eq 'Right') 'Audio mini-player is not tucked into the corner'
} finally {
    Set-Item Function:script:Start-PlayerTrack -Value $savedStartPlayer
    $ui.Folder.Text = $savedFolder
    Update-Library
    Set-PreviewMode $originalMode $false
    $resolved = [IO.Path]::GetFullPath($copyFolder)
    if ((Split-Path $resolved) -eq [IO.Path]::GetTempPath().TrimEnd('\') -and (Split-Path $resolved -Leaf) -like 'PocketMusic-copy-check-*') { Remove-Item -LiteralPath $resolved -Recurse -Force }
}

# Verify Next track stepping during search result preview playback (no PropertyNotFoundException on .Path)
$savedStartPreview = (Get-Item Function:Start-PreviewPlaybackCore).ScriptBlock
try {
    $script:mockedPreviewTrack = $null
    Set-Item Function:script:Start-PreviewPlaybackCore -Value { param($Track, $Tok) $script:mockedPreviewTrack = $Track }
    Show-SearchResults '{"entries":[{"id":"11111111111","title":"Preview 1","channel":"Artist 1","duration":100},{"id":"22222222222","title":"Preview 2","channel":"Artist 2","duration":200}]}'
    Play-AudioTrack $ui.SearchResults.ItemsSource[0] 'preview' $false
    Verify ($script:playback.queueIndex -eq 0 -and $script:playback.source -eq 'preview') 'First preview track did not start'
    Step-PlaybackTrack 1
    Verify ($script:playback.queueIndex -eq 1 -and $script:mockedPreviewTrack.Id -eq '22222222222') 'Next track during preview failed or crashed on .Path'
    Step-PlaybackTrack -1
    Verify ($script:playback.queueIndex -eq 0 -and $script:mockedPreviewTrack.Id -eq '11111111111') 'Previous track during preview failed'
} finally {
    Set-Item Function:script:Start-PreviewPlaybackCore -Value $savedStartPreview
    Stop-AllCores
}
Write-Host "PASS: $script:testCount desktop checks."
Verify (-not $timer.IsEnabled -and -not $searchTimer.IsEnabled -and -not $playerTimer.IsEnabled -and -not $previewWatch.IsEnabled) 'Idle app must not poll download, search, local playback or preview'
Write-Host 'PASS: idle background timers are stopped.'

. "$PSScriptRoot\DesktopUXChecks.ps1"

. "$PSScriptRoot\WindowsMediaSmokeChecks.ps1"
