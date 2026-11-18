$ProgressPreference='SilentlyContinue'
$ErrorActionPreference = 'Stop'
$dir = 'D:\bs_finally\asr\naudio'
New-Item -ItemType Directory -Force -Path $dir | Out-Null

# 1. 下载 NAudio(net472 dll)
foreach ($pkg in @('NAudio.Core', 'NAudio.Wasapi')) {
  $zip = "$dir\$pkg.nupkg"
  if (-not (Test-Path $zip)) {
    Invoke-WebRequest "https://www.nuget.org/api/v2/package/$pkg/2.2.1" -OutFile $zip -UseBasicParsing
  }
  $out = "$dir\$pkg"
  if (-not (Test-Path $out)) { Expand-Archive $zip $out -Force }
}
function Find-Dll($root, $name) {
  $d = Get-ChildItem $root -Recurse -Filter $name | Where-Object { $_.FullName -like '*net472*' } | Select-Object -First 1
  if (-not $d) { $d = Get-ChildItem $root -Recurse -Filter $name | Select-Object -First 1 }
  return $d.FullName
}
Add-Type -Path (Find-Dll "$dir\NAudio.Core" 'NAudio.Core.dll')
Add-Type -Path (Find-Dll "$dir\NAudio.Wasapi" 'NAudio.Wasapi.dll')

# 2. 管理员接口:设置默认设备
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
[ComImport, Guid("64027770-087A-408C-AE58-39B6A9D9C04F")]
public interface IAdminDeviceEnumerator {
    [PreserveSig] int SetDefaultAudioEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int dataFlow, int role);
}
public class AudioSet2 {
    [DllImport("ole32.dll")]
    static extern int CoCreateInstance(ref Guid clsid, IntPtr outer, int clsContext, ref Guid iid, out IntPtr ppv);
    public static int SetDefault(int dataFlow, string deviceId) {
        Guid clsid = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
        Guid iid = new Guid("64027770-087A-408C-AE58-39B6A9D9C04F");
        IntPtr p;
        int hr = CoCreateInstance(ref clsid, IntPtr.Zero, 1, ref iid, out p);
        if (hr != 0) return hr;
        IAdminDeviceEnumerator admin = (IAdminDeviceEnumerator)Marshal.GetObjectForIUnknown(p);
        return admin.SetDefaultAudioEndpoint(deviceId, dataFlow, 0);
    }
}
"@

$enum = New-Object NAudio.CoreAudioApi.MMDeviceEnumerator

function Get-ActiveDevices($flow) {
  $list = @()
  foreach ($d in $enum.EnumerateAudioEndPoints($flow, [NAudio.CoreAudioApi.DeviceState]::Active)) {
    $list += [PSCustomObject]@{ Name = $d.FriendlyName; Id = $d.ID }
  }
  return $list
}

$renders = Get-ActiveDevices ([NAudio.CoreAudioApi.DataFlow]::Render)
$captures = Get-ActiveDevices ([NAudio.CoreAudioApi.DataFlow]::Capture)
"=== active render devices ==="
$renders | ForEach-Object { "  $($_.Name)  =>  $($_.Id)" }
"=== active capture devices ==="
$captures | ForEach-Object { "  $($_.Name)  =>  $($_.Id)" }

$renderTarget = $renders | Where-Object { $_.Name -like '*英特尔*' } | Select-Object -First 1
$captureTarget = $captures | Where-Object { $_.Name -like '*英特尔*' } | Select-Object -First 1

if (-not $renderTarget) { "ERROR: no Intel render device found"; exit 1 }
if (-not $captureTarget) { "ERROR: no Intel capture device found"; exit 1 }

$hr1 = [AudioSet2]::SetDefault(1, $renderTarget.Id)
"set render default: hr=0x$($hr1.ToString('X'))  device=$($renderTarget.Name)"
$hr2 = [AudioSet2]::SetDefault(2, $captureTarget.Id)
"set capture default: hr=0x$($hr2.ToString('X'))  device=$($captureTarget.Name)"

""
"=== verify ==="
try {
  $defRender = $enum.GetDefaultAudioEndpoint([NAudio.CoreAudioApi.DataFlow]::Render, [NAudio.CoreAudioApi.Role]::Console)
  "default render: $($defRender.FriendlyName)"
} catch { "default render: still NONE" }
try {
  $defCap = $enum.GetDefaultAudioEndpoint([NAudio.CoreAudioApi.DataFlow]::Capture, [NAudio.CoreAudioApi.Role]::Console)
  "default capture: $($defCap.FriendlyName)"
} catch { "default capture: still NONE" }
