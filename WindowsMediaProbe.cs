// GPL-3.0. Test-only observer/controller; targets only our uniquely titled test session.
using System;
using System.Threading.Tasks;
using Windows.Media.Control;
namespace PocketMusic.Tests
{
    public sealed class MediaSnapshot
    {
        public string AppId, Title, Artist, Status;
        public ulong ArtworkBytes;
        public uint ArtworkWidth;
        public bool CanNext, CanPrevious;
    }
    public static class MediaProbe
    {
        private static async Task<GlobalSystemMediaTransportControlsSession> Find(string title)
        {
            var manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync().AsTask().ConfigureAwait(false);
            foreach (var session in manager.GetSessions())
            {
                if (session.SourceAppUserModelId != "PocketMusic.Desktop") continue;
                var properties = await session.TryGetMediaPropertiesAsync().AsTask().ConfigureAwait(false);
                if (properties.Title == title) return session;
            }
            return null;
        }
        public static async Task<MediaSnapshot> Read(string title)
        {
            var session = await Find(title).ConfigureAwait(false);
            if (session == null) return null;
            var properties = await session.TryGetMediaPropertiesAsync().AsTask().ConfigureAwait(false);
            var snapshot = new MediaSnapshot { AppId=session.SourceAppUserModelId, Title=properties.Title, Artist=properties.Artist, Status=session.GetPlaybackInfo().PlaybackStatus.ToString() };
            snapshot.CanNext = session.GetPlaybackInfo().Controls.IsNextEnabled;
            snapshot.CanPrevious = session.GetPlaybackInfo().Controls.IsPreviousEnabled;
            if (properties.Thumbnail != null)
            {
                using (var image = await properties.Thumbnail.OpenReadAsync().AsTask().ConfigureAwait(false))
                {
                    snapshot.ArtworkBytes = image.Size;
                    var decoder = await Windows.Graphics.Imaging.BitmapDecoder.CreateAsync(image).AsTask().ConfigureAwait(false);
                    snapshot.ArtworkWidth = decoder.PixelWidth;
                }
            }
            return snapshot;
        }
        public static async Task<bool> Command(string title, string command)
        {
            var session = await Find(title).ConfigureAwait(false);
            if (session == null) return false;
            switch (command)
            {
                case "play": return await session.TryPlayAsync().AsTask().ConfigureAwait(false);
                case "pause": return await session.TryPauseAsync().AsTask().ConfigureAwait(false);
                case "next": return await session.TrySkipNextAsync().AsTask().ConfigureAwait(false);
                case "previous": return await session.TrySkipPreviousAsync().AsTask().ConfigureAwait(false);
            }
            return false;
        }
    }
}
