$ProgressPreference='SilentlyContinue'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

[ComImport, Guid("F19F0C80-663B-11D0-A18C-00A0C9223196")]
public interface IEnumAudioEndpoints {
    [PreserveSig] int Next(int cElements, [Out, MarshalAs(UnmanagedType.LPArray, ArraySubType = UnmanagedType.Interface)] IMMDevice[] rgElements, out int pcFetched);
    [PreserveSig] int Skip(int cElements);
    [PreserveSig] int Reset();
    [PreserveSig] int Clone(out IEnumAudioEndpoints ppenum);
}

[ComImport, Guid("D666063F-1587-4E43-81F1-B948E8013B88")]
public interface IMMDevice {
    [PreserveSig] int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);
    [PreserveSig] int Activate(ref Guid iid, int clsCtx, [MarshalAs(UnmanagedType.IUnknown)] out object ppv);
    [PreserveSig] int OpenPropertyStore(int stgmMode, [MarshalAs(UnmanagedType.IUnknown)] out object properties);
    [PreserveSig] int GetState([MarshalAs(UnmanagedType.U4)] out int state);
    [PreserveSig] int GetFriendlyName([MarshalAs(UnmanagedType.U4)] int type, [MarshalAs(UnmanagedType.LPWStr)] out string name);
}

[ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6")]
public interface IMMDeviceEnumerator {
    [PreserveSig] int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice device);
    [PreserveSig] int EnumAudioEndpoints(int dataFlow, int stateMask, out IEnumAudioEndpoints ppenum);
    [PreserveSig] int GetDevice([MarshalAs(UnmanagedType.LPWStr)] string id, out IMMDevice ppDevice);
    [PreserveSig] int RegisterEndpointNotificationCallback([MarshalAs(UnmanagedType.IUnknown)] object pclient);
    [PreserveSig] int UnregisterEndpointNotificationCallback([MarshalAs(UnmanagedType.IUnknown)] object pclient);
}

[ComImport, Guid("64027770-087A-408C-AE58-39B6A9D9C04F")]
public interface IAdminDeviceEnumerator {
    [PreserveSig] int SetDefaultAudioEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int dataFlow, int role);
}

public class AudioSet {
    [DllImport("ole32.dll")]
    static extern int CoCreateInstance(ref Guid clsid, IntPtr outer, int clsContext, ref Guid iid, out IntPtr ppv);

    static IMMDeviceEnumerator GetEnum() {
        Guid clsid = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
        Guid iid = new Guid("A95664D2-9614-4F35-A746-DE8DB63617E6");
        IntPtr p;
        int hr = CoCreateInstance(ref clsid, IntPtr.Zero, 1, ref iid, out p);
        if (hr != 0) throw new Exception("CoCreateInstance failed 0x" + hr.ToString("X"));
        return (IMMDeviceEnumerator)Marshal.GetObjectForIUnknown(p);
    }

    public static string FindDeviceId(int dataFlow, string nameContains) {
        IMMDeviceEnumerator en = GetEnum();
        IEnumAudioEndpoints list;
        int hr = en.EnumAudioEndpoints(dataFlow, 0, out list);
        if (hr != 0) return "enum failed 0x" + hr.ToString("X");
        IMMDevice[] arr = new IMMDevice[16];
        int fetched;
        while (true) {
            for (int i = 0; i < 16; i++) arr[i] = null;
            hr = list.Next(16, arr, out fetched);
            if (hr != 0 || fetched == 0) break;
            for (int i = 0; i < fetched; i++) {
                if (arr[i] == null) continue;
                string name;
                arr[i].GetFriendlyName(1, out name);
                int state;
                arr[i].GetState(out state);
                if (state != 0) continue;
                if (name.IndexOf(nameContains, StringComparison.OrdinalIgnoreCase) >= 0) {
                    string id;
                    arr[i].GetId(out id);
                    return name + "  ||ID||  " + id;
                }
            }
            if (fetched < 16) break;
        }
        return "NOT FOUND";
    }

    public static string SetDefault(int dataFlow, string deviceId) {
        Guid clsid = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
        Guid iid = new Guid("64027770-087A-408C-AE58-39B6A9D9C04F");
        IntPtr p;
        int hr = CoCreateInstance(ref clsid, IntPtr.Zero, 1, ref iid, out p);
        if (hr != 0) return "admin activate failed 0x" + hr.ToString("X");
        IAdminDeviceEnumerator admin = (IAdminDeviceEnumerator)Marshal.GetObjectForIUnknown(p);
        hr = admin.SetDefaultAudioEndpoint(deviceId, dataFlow, 0);
        return hr == 0 ? "OK" : "SetDefault failed 0x" + hr.ToString("X");
    }

    public static string DefaultName(int dataFlow) {
        IMMDeviceEnumerator en = GetEnum();
        IMMDevice dev;
        int hr = en.GetDefaultAudioEndpoint(dataFlow, 0, out dev);
        if (hr != 0) return "NO DEFAULT (0x" + hr.ToString("X") + ")";
        string name;
        dev.GetFriendlyName(1, out name);
        return name;
    }
}
"@

$playback = [AudioSet]::FindDeviceId(1, '英特尔')
$record = [AudioSet]::FindDeviceId(2, '英特尔')
"playback candidate: $playback"
"record candidate:   $record"

if ($playback -and $playback -ne 'NOT FOUND' -and $playback -notlike 'enum*') {
  $id = ($playback -split '\|\|ID\|\|')[1]
  "set playback default: " + [AudioSet]::SetDefault(1, $id)
}
if ($record -and $record -ne 'NOT FOUND' -and $record -notlike 'enum*') {
  $id = ($record -split '\|\|ID\|\|')[1]
  "set record default:   " + [AudioSet]::SetDefault(2, $id)
}
""
"verify playback: " + [AudioSet]::DefaultName(1)
"verify record:   " + [AudioSet]::DefaultName(2)
