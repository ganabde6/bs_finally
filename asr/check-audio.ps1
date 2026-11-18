$ProgressPreference='SilentlyContinue'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

[ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6")]
public interface IMMDeviceEnumerator {
    [PreserveSig] int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice device);
}

[ComImport, Guid("D666063F-1587-4E43-81F1-B948E8013B88")]
public interface IMMDevice {
    int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);
    [PreserveSig] int Activate(ref Guid iid, int clsCtx, [MarshalAs(UnmanagedType.IUnknown)] out object ppv);
    [PreserveSig] int OpenPropertyStore(int stgmMode, [MarshalAs(UnmanagedType.IUnknown)] out object properties);
    [PreserveSig] int GetState([MarshalAs(UnmanagedType.U4)] out int state);
    [PreserveSig] int GetFriendlyName([MarshalAs(UnmanagedType.U4)] int type, [MarshalAs(UnmanagedType.LPWStr)] out string name);
}

public class AudioHelper {
    [DllImport("ole32.dll")]
    static extern int CoCreateInstance(ref Guid clsid, IntPtr outer, int clsContext, ref Guid iid, out IntPtr ppv);

    public static string DefaultEndpoint(int dataFlow) {
        Guid clsid = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
        Guid iid = new Guid("A95664D2-9614-4F35-A746-DE8DB63617E6");
        IntPtr pEnum;
        int hr = CoCreateInstance(ref clsid, IntPtr.Zero, 1, ref iid, out pEnum);
        if (hr != 0) return "CoCreateInstance failed 0x" + hr.ToString("X");
        object en = Marshal.GetObjectForIUnknown(pEnum);
        IMMDeviceEnumerator enumerator = (IMMDeviceEnumerator)en;
        IMMDevice dev;
        hr = enumerator.GetDefaultAudioEndpoint(dataFlow, 0, out dev);
        if (hr != 0) return "NO DEFAULT (hr=0x" + hr.ToString("X") + ")";
        string name;
        dev.GetFriendlyName(1, out name);
        return name;
    }
}
"@
"default PLAYBACK: " + [AudioHelper]::DefaultEndpoint(1)
"default RECORD:   " + [AudioHelper]::DefaultEndpoint(2)
""
"=== all audio endpoints (PnP) ==="
Get-PnpDevice -Class AudioEndpoint -PresentOnly | ForEach-Object { "$($_.Status) | $($_.FriendlyName)" }
