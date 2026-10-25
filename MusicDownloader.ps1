param([switch]$SmokeTest, [string]$PreviewImage, [switch]$Desktop, [string]$PreviewSearch, [switch]$LiveSearchTest, [string]$PlaybackTestFolder, [switch]$PreviewLiveTest, [switch]$WindowsMediaTest)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Core.ps1"
if ($Desktop) {
    $identityDll = Join-Path $PSScriptRoot 'PocketMusic.Shell.dll'
    if (Test-Path -LiteralPath $identityDll) { [Reflection.Assembly]::Load([IO.File]::ReadAllBytes($identityDll)) | Out-Null }
    else { Add-Type -Path (Join-Path $PSScriptRoot 'WindowsIdentity.cs') }
    [PocketMusic.WindowsIdentity]::InitializeProcess()
}
Add-Type -AssemblyName PresentationFramework, System.Windows.Forms
[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Title="Pocket Music • PC" Width="740" Height="700" MinWidth="640" MinHeight="640" Background="#101827" Foreground="#E8EEF7" WindowStartupLocation="CenterScreen">
 <Window.Resources>
  <Style TargetType="TextBox"><Setter Property="Padding" Value="12"/><Setter Property="FontSize" Value="14"/><Setter Property="Background" Value="#202D42"/><Setter Property="Foreground" Value="White"/><Setter Property="BorderBrush" Value="#41516B"/></Style>
  <Style TargetType="Button"><Setter Property="Padding" Value="16,10"/><Setter Property="Margin" Value="6,0,0,0"/><Setter Property="FontSize" Value="14"/></Style>
 </Window.Resources>
 <Grid Margin="30">
  <Grid.RowDefinitions><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="*"/><RowDefinition Height="Auto"/></Grid.RowDefinitions>
  <StackPanel Grid.Row="0" Margin="0,0,0,24"><TextBlock Text="Pocket Music" FontSize="32" FontWeight="Bold"/><TextBlock Text="Your audio. Saved on your PC." Foreground="#AABAD0" Margin="0,6,0,0" FontSize="16"/></StackPanel>
  <StackPanel Grid.Row="1"><TextBlock Text="YouTube video link" Margin="0,0,0,8"/><TextBox Name="Url" ToolTip="Paste an HTTPS YouTube video URL"/></StackPanel>
  <Grid Grid.Row="2" Margin="0,18,0,0"><Grid.ColumnDefinitions><ColumnDefinition Width="160"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions><StackPanel><TextBlock Text="Download format" Margin="0,0,0,8"/><ComboBox Name="Format" SelectedIndex="0" FontSize="15" Padding="10"><ComboBoxItem Content="MP3"/><ComboBoxItem Content="MP4 (video)" ToolTip="Video with audio"/><ComboBoxItem Content="M4A"/><ComboBoxItem Content="Opus"/><ComboBoxItem Content="FLAC"/><ComboBoxItem Content="WAV"/></ComboBox></StackPanel><TextBlock Grid.Column="1" Margin="18,23,0,0" Text="MP3 works with most players. FLAC and WAV use more space; they cannot restore quality lost in the source." TextWrapping="Wrap" Foreground="#AABAD0"/></Grid>
  <StackPanel Grid.Row="3" Margin="0,18,0,0"><TextBlock Text="Save to folder" Margin="0,0,0,8"/><DockPanel><Button Name="Browse" Content="Browse…" DockPanel.Dock="Right"/><TextBox Name="Folder"/></DockPanel></StackPanel>
  <CheckBox Grid.Row="4" Name="Permission" Margin="0,20,0,18" Foreground="#E8EEF7" Content="I own this content or have permission to download it."/>
  <DockPanel Grid.Row="5"><Button Name="Start" Content="Download audio" Background="#83E2C1" DockPanel.Dock="Right"/><Button Name="Cancel" Content="Cancel" IsEnabled="False" DockPanel.Dock="Right"/><TextBlock Name="Status" Text="Ready when you are" VerticalAlignment="Center" TextWrapping="Wrap" Margin="0,0,12,0"/></DockPanel>
  <ProgressBar Grid.Row="6" Name="Progress" Height="7" Margin="0,18,0,14" Foreground="#83E2C1" Maximum="100"/>
  <TextBox Grid.Row="7" Name="Details" IsReadOnly="True" TextWrapping="Wrap" VerticalScrollBarVisibility="Auto" FontFamily="Consolas" FontSize="12" MinHeight="90"/>
  <DockPanel Grid.Row="8" Margin="0,16,0,0"><Button Name="OpenFolder" Content="Open folder" DockPanel.Dock="Right"/><TextBlock Text="Public videos only • One video at a time&#10;No account login or Premium offline export" Foreground="#AABAD0" VerticalAlignment="Center"/></DockPanel>
 </Grid>
</Window>
'@
if ($Desktop) { [xml]$xaml = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Desktop.xaml')) }
$window = [Windows.Markup.XamlReader]::Load((New-Object System.Xml.XmlNodeReader $xaml))
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'PocketMusic.ico')) { $window.Icon = New-Object Windows.Media.Imaging.BitmapImage ([uri](Join-Path $PSScriptRoot 'PocketMusic.ico')) }
if ($Desktop) {
    $window.Add_SourceInitialized({
        $script:desktopWindowHandle = (New-Object Windows.Interop.WindowInteropHelper $window).Handle
        [PocketMusic.WindowsIdentity]::InitializeWindow($script:desktopWindowHandle, (Join-Path $PSScriptRoot 'Pocket Music.exe'), (Join-Path $PSScriptRoot 'PocketMusic.ico'))
    })
}
$ui = @{}
foreach ($name in @('Url','Format','Folder','Browse','Permission','Start','Cancel','Status','Progress','Details','OpenFolder')) { $ui[$name] = $window.FindName($name) }
$ui.Folder.Text = [Environment]::GetFolderPath('MyMusic')
$ui.Format.Add_SelectionChanged({ $ui.Start.Content = $(if ($ui.Format.SelectedItem.Content -eq 'MP4 (video)') { 'Download video' } else { 'Download audio' }) })
if (-not (Test-Path -LiteralPath $ui.Folder.Text)) { $ui.Folder.Text = [Environment]::GetFolderPath('MyDocuments') }
$script:job = $null
$script:outTask = $null
$script:errTask = $null
$script:cancelled = $false
$script:savedFile = $null
$script:log = ''

function Add-Log([string]$Line) {
    if ($Line -match '^PROGRESS:\s*([\d.]+)%') {
        $ui.Progress.IsIndeterminate = $false
        $ui.Progress.Value = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
        $ui.Status.Text = "Downloading • $($Matches[1])%"
    } elseif ($Line.StartsWith('FILE:')) {
        $script:savedFile = $Line.Substring(5)
    } else {
        if ($Line -match '\[ExtractAudio\]|\[Metadata\]|\[Merger\]|\[VideoRemuxer\]') { $ui.Status.Text = 'Finishing media file…'; $ui.Progress.IsIndeterminate = $true }
        $script:log += "$Line`r`n"
        if ($script:log.Length -gt 24000) { $script:log = $script:log.Substring($script:log.Length - 20000) }
        $ui.Details.Text = $script:log
        $ui.Details.ScrollToEnd()
    }
    if ($Desktop -and $ui['ToggleDownloads']) { Update-DownloadAffordance }
}
function Set-Busy([bool]$Busy) {
    foreach ($name in @('Start','Url','Format','Folder','Browse','Permission')) { $ui[$name].IsEnabled = -not $Busy }
    $ui.Cancel.IsEnabled = $Busy
    if ($Desktop) { $ui.SearchResults.IsEnabled = -not $Busy; Update-DownloadAffordance }
}
function Stop-Download {
    if ($script:job -and -not $script:job.HasExited) {
        $script:cancelled = $true
        $ui.Cancel.IsEnabled = $false
        $ui.Status.Text = 'Cancelling…'
        $kill = New-Object Diagnostics.ProcessStartInfo
        $kill.FileName = "$env:SystemRoot\System32\taskkill.exe"
        $kill.Arguments = "/PID $($script:job.Id) /T /F"
        $kill.UseShellExecute = $false
        $kill.CreateNoWindow = $true
        $killer = [Diagnostics.Process]::Start($kill)
        $killer.WaitForExit(5000) | Out-Null
        $killer.Dispose()
    }
}
$ui.Browse.Add_Click({
    $dialog = New-Object Windows.Forms.FolderBrowserDialog
    $dialog.Description = 'Choose where to save your audio'
    $dialog.SelectedPath = $ui.Folder.Text
    if ($dialog.ShowDialog() -eq 'OK') { $ui.Folder.Text = $dialog.SelectedPath }
    $dialog.Dispose()
})
$ui.OpenFolder.Add_Click({
    if (Test-Path -LiteralPath $ui.Folder.Text -PathType Container) { Start-Process explorer.exe -ArgumentList (Quote-Argument $ui.Folder.Text) }
})
$ui.Cancel.Add_Click({ Stop-Download })
$ui.Start.Add_Click({
    try {
        if (-not $ui.Permission.IsChecked) { throw 'Confirm that you own the content or have permission to download it.' }
        $exe = Find-Tool 'yt-dlp'
        if (-not $exe) { throw 'The download tool is missing. Run Setup.cmd first.' }
        $arguments = Get-DownloadArguments $ui.Url.Text ($ui.Format.SelectedItem.Content.ToString().Split(' ')[0].ToLowerInvariant()) $ui.Folder.Text
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $exe
        $info.Arguments = ($arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
        $info.UseShellExecute = $false
        $info.CreateNoWindow = $true
        $info.RedirectStandardOutput = $true
        $info.RedirectStandardError = $true
        $info.StandardOutputEncoding = [Text.Encoding]::UTF8
        $info.StandardErrorEncoding = [Text.Encoding]::UTF8
        $info.EnvironmentVariables['PYTHONIOENCODING'] = 'utf-8'
        $script:log = ''; $ui.Details.Text = ''; $script:savedFile = $null; $script:cancelled = $false
        $script:job = [Diagnostics.Process]::Start($info)
        $timer.Start()
        $script:outTask = $script:job.StandardOutput.ReadLineAsync()
        $script:errTask = $script:job.StandardError.ReadLineAsync()
        Set-Busy $true
        $ui.Status.Text = 'Connecting to YouTube…'
        $ui.Progress.Value = 0; $ui.Progress.IsIndeterminate = $true
    } catch { $ui.Status.Text = $_.Exception.Message; Add-Log $_.Exception.Message; if ($Desktop) { $ui.DetailsExpander.IsExpanded = $true } }
})
$timer = New-Object Windows.Threading.DispatcherTimer
$timer.Interval = [TimeSpan]::FromMilliseconds(300)
$timer.Add_Tick({
    if (-not $script:job) { return }
    foreach ($stream in @('out','err')) {
        for ($i = 0; $i -lt 100; $i++) {
            $task = if ($stream -eq 'out') { $script:outTask } else { $script:errTask }
            if (-not $task -or -not $task.IsCompleted) { break }
            $line = $task.GetAwaiter().GetResult()
            if ($null -ne $line) { Add-Log $line }
            $next = $null
            if ($null -ne $line) {
                $reader = if ($stream -eq 'out') { $script:job.StandardOutput } else { $script:job.StandardError }
                $next = $reader.ReadLineAsync()
            }
            if ($stream -eq 'out') { $script:outTask = $next } else { $script:errTask = $next }
        }
    }
    if ($script:job.HasExited -and -not $script:outTask -and -not $script:errTask) {
        $ui.Progress.IsIndeterminate = $false
        if ($script:cancelled) { $ui.Status.Text = 'Cancelled'; Add-Log 'Partial files may remain in the destination. Retrying can resume the download.' }
        elseif ($script:job.ExitCode -eq 0 -and $script:savedFile -and (Test-Path -LiteralPath $script:savedFile)) {
            $ui.Status.Text = 'Saved • ready to play'; $ui.Progress.Value = 100; Add-Log "Saved: $script:savedFile"
            if ($Desktop) { Update-Library; $ui.LibraryStatus.Text = 'Download saved. Select it in Library to play inside Pocket Music.' }
        } else { $ui.Status.Text = 'Download did not finish'; Add-Log (Get-FriendlyError $script:log) }
        $script:job.Dispose(); $script:job = $null
        $timer.Stop()
        Set-Busy $false
    }
})
$window.Add_Closing({
    if ($script:job -and -not $script:job.HasExited) {
        if ([Windows.MessageBox]::Show('Cancel the active download and close?', 'Pocket Music', 'YesNo') -ne 'Yes') { $_.Cancel = $true; return }
        Stop-Download
    }
    $timer.Stop()
})
if ($Desktop) { . "$PSScriptRoot\DesktopController.ps1" }
if ($Desktop -and $PreviewSearch) { Show-Page 'Search'; $ui.Query.Text = 'Creative Commons piano music'; Show-SearchResults ([IO.File]::ReadAllText($PreviewSearch)) }
if ($PreviewImage) {
    $window.Add_ContentRendered({
        $bitmap = New-Object Windows.Media.Imaging.RenderTargetBitmap ([int]$window.Content.ActualWidth), ([int]$window.Content.ActualHeight), 96, 96, ([Windows.Media.PixelFormats]::Pbgra32)
        $window.UpdateLayout(); $bitmap.Render($window.Content)
        $encoder = New-Object Windows.Media.Imaging.PngBitmapEncoder
        $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
        $file = [IO.File]::Create($PreviewImage)
        try { $encoder.Save($file) } finally { $file.Dispose() }
    })
}
if ($SmokeTest) {
    $window.Add_ContentRendered({
        $ui.Start.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
        if ($ui.Details.Text -notmatch 'permission') { throw 'Permission validation did not display.' }
        if ($Desktop) { . "$PSScriptRoot\DesktopChecks.ps1" }
        $window.Close()
    })
}
if ($Desktop -and $LiveSearchTest) { . "$PSScriptRoot\DesktopLiveCheck.ps1" }
if ($Desktop -and $PlaybackTestFolder -and -not $PreviewLiveTest -and -not $WindowsMediaTest) { . "$PSScriptRoot\PlayerChecks.ps1" }
if ($Desktop -and $PreviewLiveTest) { . "$PSScriptRoot\PreviewLiveCheck.ps1" }
if ($Desktop -and $WindowsMediaTest) { . "$PSScriptRoot\WindowsMediaChecks.ps1" }
if ($Desktop) {
    $window.Add_Closing({
        if (-not $_.Cancel) { [PocketMusic.WindowsIdentity]::ClearWindow($script:desktopWindowHandle) }
    })
}
$window.ShowDialog() | Out-Null
