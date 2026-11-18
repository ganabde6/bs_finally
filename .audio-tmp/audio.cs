using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;

[ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IMMDeviceEnumerator {
    [PreserveSig] int GetDefaultAudioEndpoint([In] int dataFlow, [In] int role, [MarshalAs(UnmanagedType.Interface)] out IMMDevice ppDevice);
    [PreserveSig] int GetAudioEndpoint([MarshalAs(UnmanagedType.LPWStr)] string pwstrId, [MarshalAs(UnmanagedType.Interface)] out IMMDevice ppDevice);
    [PreserveSig] int EnumAudioEndpoints([In] int dataFlow, [In] uint dwStateMask, [MarshalAs(UnmanagedType.Interface)] out IMMDeviceCollection ppDevices);
    [PreserveSig] int GetComInitialized();
    [PreserveSig] int RegisterEndpointNotificationCallback(IntPtr pClient);
    [PreserveSig] int UnregisterEndpointNotificationCallback(IntPtr pClient);
}

[ComImport, Guid("02744195-6C6E-4553-A066-44D923A85DE2"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IMMDeviceCollection {
    [PreserveSig] int GetCount(out uint count);
    [PreserveSig] int Item([In] uint n, [MarshalAs(UnmanagedType.Interface)] out IMMDevice ppDevice);
}

[ComImport, Guid("D666063F-1587-4E43-81F1-B948E8012013"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IMMDevice {
    [PreserveSig] int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);
    [PreserveSig] int GetState(out uint state);
    [PreserveSig] int Activate([In] ref Guid riid, [In] uint dwFlags, [MarshalAs(UnmanagedType.Interface)] out IAudioEndpointVolume ppInterface);
    [PreserveSig] int GetProperty(IntPtr pk, [Out] IntPtr pv);
    [PreserveSig] int SetProperty(IntPtr pk, [In] IntPtr pv);
    [PreserveSig] int OpenPropertyStore([In] uint stgmMode, [MarshalAs(UnmanagedType.Interface)] out object ppStore);
}

[ComImport, Guid("F8677F1F-0E2B-4DF4-9A66-5438F823D584"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IPolicyConfig {
    [PreserveSig] int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, [In] int role, [In] int orientation);
    [PreserveSig] int GetDefaultEndpoint([In] int role, [In] int orientation, [MarshalAs(UnmanagedType.LPWStr)] out string deviceId);
    [PreserveSig] int GetEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, [MarshalAs(UnmanagedType.LPWStr)] out string endpointId);
    [PreserveSig] int GetSubstitutableEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, [In] int role, [MarshalAs(UnmanagedType.LPWStr)] out string endpointId);
    [PreserveSig] int SetEndpointSubstitute([MarshalAs(UnmanagedType.LPWStr)] string deviceId, [In] int role, [MarshalAs(UnmanagedType.LPWStr)] string substituteId);
    [PreserveSig] int IsEndpointSubstituted([MarshalAs(UnmanagedType.LPWStr)] string deviceId, out uint isSubstituted);
}

[ComImport, Guid("5CDF2C88-84F1-4D5A-9520-080DC2629AFF"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IAudioEndpointVolume {
    int FInitializeWithParClient(IntPtr a, [In] Guid riid);
    int FGetChannelCount(out uint c);
    int FSetChannelVolume(uint i, float f, [In] Guid g);
    int FGetChannelVolume(uint i, float f, [In] Guid g);
    int FSetMasterVolumeLevel(float f, [In] Guid g);
    int FGetMasterVolumeLevel(float f, [In] Guid g);
    int FSetMasterVolumeLevelScalar(float f, [In] Guid g);
    int FGetMasterVolumeLevelScalar(out float f, [In] Guid g);
    int FSetVolumeMatch(uint i, float f, [In] Guid g);
    int FSetMasterVolumeStepUp([In] Guid g);
    int FSetMasterVolumeStepDown([In] Guid g);
    int FMute(uint b, [In] Guid g);
    int FGetMute(out uint b, [In] Guid g);
    int FSetVolumeCallbackNotification(IntPtr p);
    int FQueryHardwareSupport(out uint c);
    int FGetVolumeRange(float f1, float f2, float f3);
}

public static class AudioFix {
    [DllImport("ole32.dll")]
    static extern int CoCreateInstance(ref Guid rclsid, IntPtr pUnkOuter, uint dwClsContext, ref Guid riid, out IntPtr ppv);

    const uint CLSCTX_INPROC_SERVER = 1;
    static readonly Guid CLSID_MMDeviceEnumerator = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
    static readonly Guid IID_MMDeviceEnumerator = new Guid("A95664D2-9614-4F35-A746-DE8DB63617E6");
    static readonly Guid CLSID_IPolicyConfig = new Guid("568AB4A1-9B42-4604-B1A8-0F7378C8E1D2");
    static readonly Guid IID_IPolicyConfig = new Guid("F8677F1F-0E2B-4DF4-9A66-5438F823D584");
    static readonly Guid IID_IAudioEndpointVolume = new Guid("5CDF2C88-84F1-4D5A-9520-080DC2629AFF");

    static IMMDeviceEnumerator NewEnumerator() {
        Guid c = CLSID_MMDeviceEnumerator, i = IID_MMDeviceEnumerator;
        IntPtr p; int hr = CoCreateInstance(ref c, IntPtr.Zero, CLSCTX_INPROC_SERVER, ref i, out p);
        if (hr != 0) throw new Exception("CoCreateInstance=0x" + hr.ToString("X8"));
        return (IMMDeviceEnumerator)Marshal.GetTypedObjectForIUnknown(p, typeof(IMMDeviceEnumerator));
    }
    static IPolicyConfig NewPolicy() {
        Guid c = CLSID_IPolicyConfig, i = IID_IPolicyConfig;
        IntPtr p; int hr = CoCreateInstance(ref c, IntPtr.Zero, CLSCTX_INPROC_SERVER, ref i, out p);
        if (hr != 0) throw new Exception("CoCreateInstance=0x" + hr.ToString("X8"));
        return (IPolicyConfig)Marshal.GetTypedObjectForIUnknown(p, typeof(IPolicyConfig));
    }

    public static string GetDefaultEndpointInfo(int dataFlow) {
        IMMDevice d = null;
        int hr = NewEnumerator().GetDefaultAudioEndpoint(dataFlow, 0, out d);
        if (hr != 0) return "hr=0x" + hr.ToString("X8") + ";id=";
        string id = null;
        d.GetId(out id);
        return "hr=0x00000000;id=" + (id ?? "-");
    }

    public static string EnumEndpointsDebug(int dataFlow, uint stateMask) {
        var e = NewEnumerator();
        IMMDeviceCollection c = null;
        int hr = e.EnumAudioEndpoints(dataFlow, stateMask, out c);
        if (hr != 0) return "enum hr=0x" + hr.ToString("X8");
        uint n = 0;
        hr = c.GetCount(out n);
        if (hr != 0) return "enum ok; count hr=0x" + hr.ToString("X8");
        var sb = new System.Text.StringBuilder("count=" + n + ";");
        for (uint i = 0; i < n; i++) {
            IMMDevice d = null;
            hr = c.Item(i, out d);
            if (hr != 0) { sb.Append(" item" + i + "=0x" + hr.ToString("X8")); continue; }
            string id = null;
            hr = d.GetId(out id);
            sb.Append(" item" + i + "=0x" + hr.ToString("X8") + ":" + (id ?? "-"));
        }
        return sb.ToString();
    }

    public static string[] EnumEndpoints(int dataFlow, uint stateMask) {
        IMMDeviceCollection c = null;
        int hr = NewEnumerator().EnumAudioEndpoints(dataFlow, stateMask, out c);
        if (hr != 0) return new string[0];
        uint n = 0;
        c.GetCount(out n);
        var list = new List<string>();
        for (uint i = 0; i < n; i++) {
            IMMDevice d = null;
            c.Item(i, out d);
            string id = null;
            d.GetId(out id);
            if (id != null) list.Add(id);
        }
        return list.ToArray();
    }

    public static int SetDefaultEndpoint(string id, int orientation) {
        return NewPolicy().SetDefaultEndpoint(id, 0, orientation);
    }

    public static int GetEndpointVolumeInfo(string id, out double scalarVolume, out bool muted) {
        scalarVolume = 0; muted = false;
        IMMDevice d = null;
        int hr = NewEnumerator().GetAudioEndpoint(id, out d);
        if (hr != 0) return hr;
        Guid iid = IID_IAudioEndpointVolume;
        IAudioEndpointVolume v = null;
        hr = d.Activate(ref iid, 0, out v);
        if (hr != 0) return hr;
        float s = 0f;
        uint m = 0;
        Guid g = Guid.Empty;
        v.FGetMasterVolumeLevelScalar(out s, g);
        v.FGetMute(out m, g);
        scalarVolume = s;
        muted = (m != 0);
        return 0;
    }
}
