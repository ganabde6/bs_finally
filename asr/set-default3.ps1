$ProgressPreference='SilentlyContinue'
$ErrorActionPreference = 'Stop'
$dir = 'D:\bs_finally\asr\naudio'
New-Item -ItemType Directory -Force -Path $dir | Out-Null

foreach ($pkg in @('NAudio.Core', 'NAudio.Wasapi')) {
  $zip = "$dir\$pkg.nupkg"
  $ok = $true
  if (-not (Test-Path $zip)) {
  $ok = $false
  $lower = $pkg.ToLower()
  $urls = @(
    "https://www.nuget.org/api/v2/package/$pkg/2.2.1",
    "https://api.nuget.org/v3-flatcontainer/$lower/2.2.1/$lower.2.2.1.nupkg",
    "https://nuget.cdn.azure.cn/v3-flatcontainer/$lower/2.2.1/$lower.2.2.1.nupkg"
  )
  foreach ($u in $urls) {
    try {
      Invoke-WebRequest $u -OutFile $zip -UseBasicParsing -TimeoutSec 60
      if ((Test-Path $zip) -and (Get-Item $zip).Length -gt 10000) { $ok = $true; "downloaded $pkg from $u"; break }
    } catch { "download failed: $u : $($_.Exception.Message)" }
  }
  }
  if (-not $ok) { "FATAL: cannot download $pkg"; exit 1 }
  $out = "$dir\$pkg"
  if (-not (Test-Path $out)) {
    Copy-Item $zip "$zip.zip" -Force
    Expand-Archive "$zip.zip" $out -Force
  }
}

function Find-Dll($root, $name) {
  $d = Get-ChildItem $root -Recurse -Filter $name | Where-Object { $_.FullName -like '*net472*' } | Select-Object -First 1
  if (-not $d) { $d = Get-ChildItem $root -Recurse -Filter $name | Select-Object -First 1 }
  return $d.FullName
}
try {
  Add-Type -Path (Find-Dll "$dir\NAudio.Core" 'NAudio.Core.dll')
  Add-Type -Path (Find-Dll "$dir\NAudio.Wasapi" 'NAudio.Wasapi.dll')
} catch {
  "Add-Type failed: $($_.Exception.Message)"
  exit 1
}
"NAudio loaded"

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
[ComImport, Guid("64027770-087A-408C-AE58-39B6A9D9C04F")]
public interface IAdminDeviceEnumerator {
    [PreserveSig] int SetDefaultAudioEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceId, int dataFlow, int role);
}
public class AudioSet3 {
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
$renders | ForEach-Object { "  $($_.Name) => $($_.Id)" }
"=== active capture devices ==="
$captures | ForEach-Object { "  $($_.Name) => $($_.Id)" }

# Intel playback device name contains 'SoundWire' (unique ASCII marker)
$renderTarget = $renders | Where-Object { $_.Name -like '*SoundWire*' } | Select-Object -First 1
# Intel capture device: prefer real hardware (ID not starting with SWD), fallback to first
$captureTarget = $captures | Where-Object { $_.Id -notlike 'SWD*' } | Select-Object -First 1
if (-not $captureTarget) { $captureTarget = $captures | Select-Object -First 1 }

if (-not $renderTarget) { "ERROR: no Intel render device (SoundWire marker not found)"; exit 1 }
if (-not $captureTarget) { "ERROR: no capture device"; exit 1 }

$hr1 = [AudioSet3]::SetDefault(1, $renderTarget.Id)
"set render default: hr=0x$($hr1.ToString('X'))"
$hr2 = [AudioSet3]::SetDefault(2, $captureTarget.Id)
"set capture default: hr=0x$($hr2.ToString('X'))"

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
