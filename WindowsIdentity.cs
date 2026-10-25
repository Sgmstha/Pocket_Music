// GPL-3.0. Explicit shell identity for the WPF window hosted by PowerShell.
using System;
using System.Runtime.InteropServices;

namespace PocketMusic
{
    public static class WindowsIdentity
    {
        public const string AppId = "PocketMusic.Desktop";
        private static readonly Guid PropertySet = new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3");
        [StructLayout(LayoutKind.Sequential, Pack = 4)]
        private struct PropertyKey { public Guid Format; public uint Id; public PropertyKey(uint id) { Format = PropertySet; Id = id; } }
        [StructLayout(LayoutKind.Explicit, Size = 24)]
        private struct Variant { [FieldOffset(0)] public ushort Type; [FieldOffset(8)] public IntPtr Pointer; }
        [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IPropertyStore
        {
            [PreserveSig] int GetCount(out uint count);
            [PreserveSig] int GetAt(uint index, out PropertyKey key);
            [PreserveSig] int GetValue(ref PropertyKey key, out Variant value);
            [PreserveSig] int SetValue(ref PropertyKey key, ref Variant value);
            [PreserveSig] int Commit();
        }
        [DllImport("shell32.dll", CharSet = CharSet.Unicode)] private static extern int SetCurrentProcessExplicitAppUserModelID(string id);
        [DllImport("shell32.dll")] private static extern int GetCurrentProcessExplicitAppUserModelID(out IntPtr id);
        [DllImport("shell32.dll")] private static extern int SHGetPropertyStoreForWindow(IntPtr window, ref Guid iid, [MarshalAs(UnmanagedType.Interface)] out IPropertyStore store);
        [DllImport("ole32.dll")] private static extern int PropVariantClear(ref Variant value);
        [DllImport("user32.dll")] public static extern IntPtr SendMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);

        public static void InitializeProcess() { Marshal.ThrowExceptionForHR(SetCurrentProcessExplicitAppUserModelID(AppId)); }
        public static string ProcessId()
        {
            IntPtr value;
            Marshal.ThrowExceptionForHR(GetCurrentProcessExplicitAppUserModelID(out value));
            try { return Marshal.PtrToStringUni(value); } finally { Marshal.FreeCoTaskMem(value); }
        }
        private static IPropertyStore Store(IntPtr window)
        {
            Guid iid = typeof(IPropertyStore).GUID; IPropertyStore store;
            Marshal.ThrowExceptionForHR(SHGetPropertyStoreForWindow(window, ref iid, out store));
            return store;
        }
        private static void Set(IPropertyStore store, uint id, string text)
        {
            PropertyKey key = new PropertyKey(id);
            Variant value = new Variant { Type = 31, Pointer = Marshal.StringToCoTaskMemUni(text) };
            try { Marshal.ThrowExceptionForHR(store.SetValue(ref key, ref value)); }
            finally { PropVariantClear(ref value); }
        }
        public static void InitializeWindow(IntPtr window, string launcher, string icon)
        {
            IPropertyStore store = Store(window);
            try
            {
                // Relaunch properties must be installed before ID triggers the taskbar refresh.
                Set(store, 2, "\"" + launcher + "\"");
                Set(store, 3, icon + ",0");
                Set(store, 4, "Pocket Music");
                Set(store, 5, AppId);
                Marshal.ThrowExceptionForHR(store.Commit());
            }
            finally { Marshal.ReleaseComObject(store); }
        }
        public static string WindowProperty(IntPtr window, uint id)
        {
            IPropertyStore store = Store(window); Variant value = new Variant();
            try { PropertyKey key = new PropertyKey(id); Marshal.ThrowExceptionForHR(store.GetValue(ref key, out value)); return value.Type == 31 ? Marshal.PtrToStringUni(value.Pointer) : null; }
            finally { PropVariantClear(ref value); Marshal.ReleaseComObject(store); }
        }
        public static void ClearWindow(IntPtr window)
        {
            IPropertyStore store = Store(window);
            try
            {
                foreach (uint id in new uint[] { 2, 3, 4, 5 })
                {
                    PropertyKey key = new PropertyKey(id); Variant empty = new Variant();
                    Marshal.ThrowExceptionForHR(store.SetValue(ref key, ref empty));
                }
            }
            finally { Marshal.ReleaseComObject(store); }
        }
    }
}
