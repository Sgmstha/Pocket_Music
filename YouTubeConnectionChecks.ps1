# Runs inside the actual WPF smoke test; account state remains isolated.
Verify (-not (Test-YouTubeSession)) 'Smoke tests inherited the listener account session'
Verify ($window.FindName('YouTubeConnection').Content -eq 'Connect YouTube') 'YouTube connection entry point is missing'
$connectionDialog=New-YouTubeConnectionDialog
try {
    Verify ($connectionDialog.Owner -eq $window) 'Connection dialog is not owned by Pocket Music'
    Verify (-not $connectionDialog.FindName('SessionRecommendations').IsEnabled) 'Recommendations are enabled without an imported session'
    Verify (-not $connectionDialog.FindName('SessionDisconnect').IsEnabled) 'Disconnect is enabled without an imported session'
    Verify ($connectionDialog.FindName('SessionImport').Content -match 'Import YouTube cookies') 'Cookie import action is missing'
    $connectionDialog.Show()
    $connectionDialog.UpdateLayout()
    Verify ($connectionDialog.IsVisible) 'Native YouTube connection dialog did not open'
    if ($PreviewImage) {
        $connectionDialog.Dispatcher.Invoke([Action]{},[Windows.Threading.DispatcherPriority]::ContextIdle)
        $bitmap=New-Object Windows.Media.Imaging.RenderTargetBitmap ([int]$connectionDialog.Content.ActualWidth),([int]$connectionDialog.Content.ActualHeight),96,96,([Windows.Media.PixelFormats]::Pbgra32)
        $bitmap.Render($connectionDialog.Content)
        $encoder=New-Object Windows.Media.Imaging.PngBitmapEncoder
        $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
        $target=Join-Path (Split-Path $PreviewImage) (([IO.Path]::GetFileNameWithoutExtension($PreviewImage))+'-connection.png')
        $file=[IO.File]::Create($target)
        try { $encoder.Save($file) } finally { $file.Dispose() }
    }
    $connectionDialog.FindName('SessionClose').RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify (-not $connectionDialog.IsVisible) 'Connection close button did not close its own dialog'
} finally { if ($connectionDialog.IsVisible) { $connectionDialog.Close() } }
Start-Search -Recommendations
Verify ($null -eq $script:searchJob -and $ui.SearchStatus.Text -match 'Import YouTube cookies') 'Account recommendations started without a session'
Verify (-not $script:youtubeLeases.search -and -not $script:youtubeLeases.download) 'Anonymous UI flow created credential leases'
Show-Page 'Home'
Write-Host "PASS: $script:testCount desktop checks including the YouTube connection UI."
