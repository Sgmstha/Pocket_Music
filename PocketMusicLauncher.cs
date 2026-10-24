// Pocket Music launcher. GPL-3.0; see LICENSE.
using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows.Forms;

[assembly: AssemblyTitle("Pocket Music")]
[assembly: AssemblyDescription("Pocket Music desktop application launcher")]
[assembly: AssemblyProduct("Pocket Music")]
[assembly: AssemblyVersion("0.3.0.0")]

internal static class PocketMusicLauncher
{
    [STAThread]
    private static int Main(string[] args)
    {
        bool test = args.Length == 1 && args[0] == "--self-test";
        try
        {
            string root = AppDomain.CurrentDomain.BaseDirectory;
            foreach (string file in new[] { "MusicDownloader.ps1", "Desktop.xaml", "DesktopController.ps1", "Core.ps1", "Search.ps1", "Player.ps1", "Player.xaml", "MetrolistIcons.xaml" })
                if (!File.Exists(Path.Combine(root, file)))
                    throw new FileNotFoundException("Pocket Music is missing " + file + ". Keep Pocket Music.exe inside its complete application folder.");
            var start = new ProcessStartInfo();
            start.FileName = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), @"WindowsPowerShell\v1.0\powershell.exe");
            start.Arguments = "-NoProfile -STA -ExecutionPolicy Bypass -File \"" + Path.Combine(root, "MusicDownloader.ps1") + "\" -Desktop" + (test ? " -SmokeTest" : "");
            start.WorkingDirectory = root;
            start.UseShellExecute = false;
            start.CreateNoWindow = true;
            start.RedirectStandardOutput = true;
            start.RedirectStandardError = true;
            using (var process = Process.Start(start))
            {
                Task<string> output = process.StandardOutput.ReadToEndAsync();
                Task<string> error = process.StandardError.ReadToEndAsync();
                process.WaitForExit();
                string details = output.Result + Environment.NewLine + error.Result;
                if (test || process.ExitCode != 0)
                    File.WriteAllText(Path.Combine(root, "launcher-diagnostics.txt"), details);
                if (process.ExitCode != 0)
                    throw new Exception("Pocket Music could not start or encountered an error. See launcher-diagnostics.txt in the application folder.\n\n" + error.Result);
                return 0;
            }
        }
        catch (Exception ex)
        {
            if (!test) MessageBox.Show(ex.Message, "Pocket Music", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }
}
