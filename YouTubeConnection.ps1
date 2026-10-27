function Update-YouTubeConnectionBadge {
    $window.FindName('YouTubeConnection').Content = if (Test-YouTubeSession) { 'YouTube session' } else { 'Connect YouTube' }
}

function New-YouTubeConnectionDialog {
    $dialog=[Windows.Markup.XamlReader]::Parse(@'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Title="Pocket Music - YouTube connection" Width="560" Height="650" MinWidth="520" MinHeight="520" WindowStartupLocation="CenterOwner" ResizeMode="CanResize" Background="#0B101B" Foreground="#F1F5FF" FontFamily="Segoe UI">
 <ScrollViewer Background="#0B101B" VerticalScrollBarVisibility="Auto"><StackPanel Margin="28">
  <TextBlock Text="Connect your YouTube session" FontSize="24" FontWeight="SemiBold" TextWrapping="Wrap"/>
  <TextBlock Text="Sign in on YouTube in your browser, then import a YouTube-only Netscape cookie file. Pocket Music never asks for your Google password or reads your browser cookie database." Foreground="#B7C5DA" TextWrapping="Wrap" Margin="0,14,0,18"/>
  <WrapPanel><Button Name="SessionOpenBrowser" Content="Open YouTube / sign in" Margin="0,0,8,8"/><Button Name="SessionHelp" Content="Export instructions" Margin="0,0,0,8"/></WrapPanel>
  <Border Background="#172133" CornerRadius="16" Padding="18" Margin="0,10,0,18"><StackPanel>
   <TextBlock Text="YouTube-only cookie import" FontSize="17" FontWeight="SemiBold"/>
   <TextBlock Text="Only youtube.com cookies are kept. The saved session is encrypted for your Windows user. Temporary request files are private and deleted when the request ends. Your original exported file stays where you saved it." Foreground="#B7C5DA" TextWrapping="Wrap" Margin="0,10,0,14"/>
   <Button Name="SessionImport" Content="Import YouTube cookies…" Background="#C9DFFF" Foreground="#101A2B"/>
  </StackPanel></Border>
  <TextBlock Name="SessionStatus" TextWrapping="Wrap" Foreground="#B7C5DA" MinHeight="50" Margin="0,0,0,12"/>
  <WrapPanel><Button Name="SessionRecommendations" Content="Load my recommendations" Margin="0,0,8,8"/><Button Name="SessionDisconnect" Content="Disconnect" Margin="0,0,0,8"/></WrapPanel>
  <TextBlock Text="Importing a session does not guarantee YouTube will lift a rate limit. Refresh the import if YouTube expires the session." Foreground="#B7C5DA" FontSize="12" TextWrapping="Wrap" Margin="0,10,0,16"/>
  <Button Name="SessionClose" Content="Done" HorizontalAlignment="Right"/>
 </StackPanel></ScrollViewer>
</Window>
'@)
    $dialog.Owner=$window
    $dialog.Height=[math]::Min($dialog.Height,[Windows.SystemParameters]::WorkArea.Height-40)
    $dialog.Resources.MergedDictionaries.Add($window.Resources)
    foreach ($name in @('SessionOpenBrowser','SessionHelp','SessionImport','SessionRecommendations','SessionDisconnect','SessionClose')) { $dialog.FindName($name).Tag=$dialog }
    Update-YouTubeDialogStatus $dialog
    $dialog.FindName('SessionOpenBrowser').Add_Click({ Start-Process 'https://www.youtube.com/' })
    $dialog.FindName('SessionHelp').Add_Click({ Start-Process 'https://github.com/yt-dlp/yt-dlp/wiki/Extractors#exporting-youtube-cookies' })
    $dialog.FindName('SessionImport').Add_Click({
        $owner=[Windows.Window]$this.Tag
        $picker=New-Object Microsoft.Win32.OpenFileDialog
        $picker.Title='Choose your YouTube-only Netscape cookie export'
        $picker.Filter='Cookie text files (*.txt)|*.txt|All files (*.*)|*.*'
        $picker.CheckFileExists=$true
        if ($picker.ShowDialog($owner)) {
            try {
                Import-YouTubeCookieFile $picker.FileName
                Update-YouTubeDialogStatus $owner
                Update-YouTubeConnectionBadge
                $owner.FindName('SessionStatus').Text='Session saved encrypted on this PC. YouTube access will be checked when you download or load recommendations.'
            } catch { $owner.FindName('SessionStatus').Text=$_.Exception.Message }
        }
    })
    $dialog.FindName('SessionDisconnect').Add_Click({
        $owner=[Windows.Window]$this.Tag
        try { Disconnect-YouTube; Update-YouTubeDialogStatus $owner; Update-YouTubeConnectionBadge }
        catch { $owner.FindName('SessionStatus').Text='Could not remove the session. Close active requests and try again.' }
    })
    $dialog.FindName('SessionRecommendations').Add_Click({ $owner=[Windows.Window]$this.Tag; $owner.Tag='recommendations'; $owner.Close() })
    $dialog.FindName('SessionClose').Add_Click({ ([Windows.Window]$this.Tag).Close() })
    return $dialog
}

function Update-YouTubeDialogStatus($Dialog) {
    $saved=Test-YouTubeSession
    $Dialog.FindName('SessionStatus').Text=if ($saved) { 'An encrypted YouTube session is saved. Its access has not been verified by this dialog.' } else { 'No account connected. Public downloads remain available when YouTube allows anonymous requests.' }
    $Dialog.FindName('SessionRecommendations').IsEnabled=$saved
    $Dialog.FindName('SessionDisconnect').IsEnabled=$saved
}

function Show-YouTubeConnection {
    if ($script:job -or $script:searchJob) { $ui.Status.Text='Finish or cancel the active request before changing the YouTube session.'; return }
    $dialog=New-YouTubeConnectionDialog
    $dialog.ShowDialog() | Out-Null
    if ($dialog.Tag -eq 'recommendations') { Start-Search -Recommendations }
}

$window.FindName('YouTubeConnection').Add_Click({ Show-YouTubeConnection })
$window.FindName('YouTubeRecommendations').Add_Click({ if (Test-YouTubeSession) { Start-Search -Recommendations } else { Show-YouTubeConnection } })
Update-YouTubeConnectionBadge
