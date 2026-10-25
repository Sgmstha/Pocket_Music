// GPL-3.0. Windows media-session adapter for the existing WPF/PowerShell player.
using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Threading;
using Windows.Media;
using Windows.Storage;
using Windows.Storage.FileProperties;
using Windows.Storage.Streams;

namespace PocketMusic
{
    public sealed class WindowsMediaBridge : IDisposable
    {
        [ComImport, Guid("DDB0472D-C911-4A1F-86D9-DC3D71A95F5A"), InterfaceType(ComInterfaceType.InterfaceIsIInspectable)]
        private interface IControlsInterop
        {
            [return: MarshalAs(UnmanagedType.IInspectable)]
            object GetForWindow(IntPtr window, [In] ref Guid iid);
        }
        private readonly SystemMediaTransportControls controls;
        private readonly Dispatcher dispatcher;
        private readonly Action<string> command;
        private readonly byte[] fallback;
        private IRandomAccessStream cover;
        private CancellationTokenSource artworkCancellation;
        private string metadataKey;
        private int generation;
        private bool disposed;
        public string LastArtworkError { get; private set; }
        public string Title { get { return controls.DisplayUpdater.MusicProperties.Title; } }
        public string Artist { get { return controls.DisplayUpdater.MusicProperties.Artist; } }
        public bool HasArtwork { get { return controls.DisplayUpdater.Thumbnail != null; } }
        public string Status { get { return controls.PlaybackStatus.ToString(); } }
        public bool Enabled { get { return controls.IsEnabled; } }

        public WindowsMediaBridge(IntPtr window, Dispatcher uiDispatcher, Action<string> onCommand, string fallbackImage)
        {
            dispatcher = uiDispatcher; command = onCommand;
            fallback = File.Exists(fallbackImage) ? File.ReadAllBytes(fallbackImage) : null;
            IControlsInterop interop;
            try { interop = (IControlsInterop)WindowsRuntimeMarshal.GetActivationFactory(typeof(SystemMediaTransportControls)); }
            catch (Exception error) { throw new InvalidOperationException("Cannot obtain desktop SMTC factory", error); }
            Guid iid = new Guid("99FA3FF4-1742-42A6-902E-087D41F965EC");
            try { controls = (SystemMediaTransportControls)interop.GetForWindow(window, ref iid); }
            catch (Exception error) { throw new InvalidOperationException("Cannot obtain desktop SMTC window: " + error.Message, error); }
            controls.DisplayUpdater.Type = MediaPlaybackType.Music;
            controls.IsEnabled = false;
            controls.ButtonPressed += OnButtonPressed;
        }
        private void OnButtonPressed(SystemMediaTransportControls sender, SystemMediaTransportControlsButtonPressedEventArgs args)
        {
            string action;
            switch (args.Button)
            {
                case SystemMediaTransportControlsButton.Play: action = "play"; break;
                case SystemMediaTransportControlsButton.Pause: action = "pause"; break;
                case SystemMediaTransportControlsButton.Stop: action = "stop"; break;
                case SystemMediaTransportControlsButton.Previous: action = "previous"; break;
                case SystemMediaTransportControlsButton.Next: action = "next"; break;
                default: return;
            }
            // WinRT events arrive on background threads; PowerShell delegates need the UI runspace.
            dispatcher.BeginInvoke(DispatcherPriority.Normal, new Action(delegate { if (!disposed) command(action); }));
        }
        public void Update(string title, string artist, string artwork, string localPath, string state, bool hasTrack, bool canPlayPause, bool canPrevious, bool canNext)
        {
            if (disposed) return;
            var status = state == "playing" ? MediaPlaybackStatus.Playing : state == "paused" ? MediaPlaybackStatus.Paused :
                (state == "opening" || state == "preparing" || state == "loading" || state == "buffering") ? MediaPlaybackStatus.Changing : MediaPlaybackStatus.Stopped;
            if (controls.PlaybackStatus != status) controls.PlaybackStatus = status;
            if (controls.IsEnabled != hasTrack) controls.IsEnabled = hasTrack;
            if (controls.IsPlayEnabled != canPlayPause) controls.IsPlayEnabled = canPlayPause;
            if (controls.IsPauseEnabled != canPlayPause) controls.IsPauseEnabled = canPlayPause;
            if (controls.IsPreviousEnabled != canPrevious) controls.IsPreviousEnabled = canPrevious;
            if (controls.IsNextEnabled != canNext) controls.IsNextEnabled = canNext;
            if (controls.IsStopEnabled != hasTrack) controls.IsStopEnabled = hasTrack;
            string key = hasTrack ? title + "\u001f" + artist + "\u001f" + artwork + "\u001f" + localPath : "";
            if (metadataKey == key) return;
            metadataKey = key; int version = ++generation;
            LastArtworkError = null;
            if (artworkCancellation != null) { artworkCancellation.Cancel(); artworkCancellation.Dispose(); }
            artworkCancellation = new CancellationTokenSource();
            controls.DisplayUpdater.ClearAll();
            controls.DisplayUpdater.Type = MediaPlaybackType.Music;
            if (!hasTrack) { ReplaceCover(null); controls.DisplayUpdater.Update(); return; }
            controls.DisplayUpdater.MusicProperties.Title = title;
            controls.DisplayUpdater.MusicProperties.Artist = artist;
            ReplaceCover(fallback == null ? null : new MemoryStream(fallback, false).AsRandomAccessStream());
            controls.DisplayUpdater.Update();
            LoadArtwork(version, artist, artwork, localPath, artworkCancellation.Token);
        }
        private async void LoadArtwork(int version, string artist, string artwork, string path, CancellationToken cancellation)
        {
            IRandomAccessStream image = null;
            try
            {
                if (!String.IsNullOrEmpty(path) && File.Exists(path))
                {
                    try
                    {
                        StorageFile file = await StorageFile.GetFileFromPathAsync(path).AsTask(cancellation);
                        MusicProperties properties = await file.Properties.GetMusicPropertiesAsync().AsTask(cancellation);
                        if (!String.IsNullOrWhiteSpace(properties.Artist)) artist = properties.Artist;
                        if (String.IsNullOrEmpty(artwork))
                        {
                            var thumbnail = await file.GetThumbnailAsync(ThumbnailMode.MusicView, 256, ThumbnailOptions.UseCurrentScale).AsTask(cancellation);
                            if (thumbnail != null && thumbnail.Type == ThumbnailType.Image && thumbnail.Size > 0) image = thumbnail;
                            else if (thumbnail != null) thumbnail.Dispose();
                        }
                    }
                    catch (OperationCanceledException) { throw; }
                    catch (Exception error) { LastArtworkError = error.Message; } // Metadata providers are optional; explicit artwork must still work.
                }
                if (image == null && String.IsNullOrEmpty(artwork) && !String.IsNullOrEmpty(path))
                {
                    // Downloaded files retain a stable video ID even when their tags lack artwork.
                    var match = System.Text.RegularExpressions.Regex.Match(Path.GetFileNameWithoutExtension(path), @"\[([A-Za-z0-9_-]{11})\]$");
                    if (match.Success) artwork = "https://i.ytimg.com/vi/" + match.Groups[1].Value + "/hqdefault.jpg";
                }
                if (image == null && !String.IsNullOrEmpty(artwork))
                {
                    Uri uri;
                    if (Uri.TryCreate(artwork, UriKind.Absolute, out uri))
                    {
                        if (uri.IsFile)
                        {
                            StorageFile file = await StorageFile.GetFileFromPathAsync(uri.LocalPath).AsTask(cancellation);
                            image = await RandomAccessStreamReference.CreateFromFile(file).OpenReadAsync().AsTask(cancellation);
                        }
                        else if (uri.Scheme == "https" && (uri.Host == "i.ytimg.com" || uri.Host == "i9.ytimg.com" || uri.Host == "img.youtube.com"))
                        {
                            image = await RandomAccessStreamReference.CreateFromUri(uri).OpenReadAsync().AsTask(cancellation);
                        }
                    }
                }
                if (cancellation.IsCancellationRequested || disposed || version != generation) { if (image != null) image.Dispose(); return; }
                IRandomAccessStream ready = image; string readyArtist = artist;
                await dispatcher.InvokeAsync(new Action(delegate
                {
                    if (disposed || version != generation) { if (ready != null) ready.Dispose(); return; }
                    controls.DisplayUpdater.MusicProperties.Artist = readyArtist;
                    if (ready != null) ReplaceCover(ready);
                    controls.DisplayUpdater.Update();
                }), DispatcherPriority.Background);
            }
            catch (OperationCanceledException) { if (image != null) image.Dispose(); }
            catch (Exception error) { if (image != null) image.Dispose(); if (version == generation) LastArtworkError = error.Message; }
        }
        private void ReplaceCover(IRandomAccessStream image)
        {
            IRandomAccessStream previous = cover; cover = image;
            controls.DisplayUpdater.Thumbnail = image == null ? null : RandomAccessStreamReference.CreateFromStream(image);
            if (previous != null) previous.Dispose();
        }
        public void Dispose()
        {
            if (disposed) return;
            disposed = true; ++generation;
            if (artworkCancellation != null) { artworkCancellation.Cancel(); artworkCancellation.Dispose(); }
            controls.ButtonPressed -= OnButtonPressed;
            controls.PlaybackStatus = MediaPlaybackStatus.Stopped;
            controls.IsEnabled = false;
            controls.DisplayUpdater.ClearAll(); controls.DisplayUpdater.Update();
            if (cover != null) { cover.Dispose(); cover = null; }
        }
    }
}
