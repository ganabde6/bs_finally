# -*- coding: utf-8 -*-
"""第二轮实测:按每个设备自己的原生采样率录 4 秒,统计峰值电平,并标注音频后端(WASAPI/MME)。"""
import json
import struct

import pyaudio

p = pyaudio.PyAudio()
devices = []
for i in range(p.get_device_count()):
    try:
        d = p.get_device_info_by_index(i)
    except Exception:
        continue
    if d['maxInputChannels'] > 0:
        try:
            backend = p.get_host_api_info_by_index(d['hostApi'])['name']
        except Exception:
            backend = '?'
        devices.append({
            'index': i,
            'name': d['name'],
            'rate': int(d['defaultSampleRate']),
            'backend': backend,
        })

# 跳过已测过的: Microsoft 声音映射器(index 0) 和 主声音捕获驱动程序(index 6)
SKIP = {0, 6}

out = {'devices': devices, 'tests': {}}
for dev in devices:
    if dev['index'] in SKIP:
        continue
    i = dev['index']
    name = dev['name']
    rate = dev['rate']
    try:
        stream = p.open(format=pyaudio.paInt16, channels=1, rate=rate, input=True,
                        input_device_index=i, frames_per_buffer=1024)
        n_frames = int(rate * 4 / 1024)
        peak = 0
        total_sq = 0
        total_n = 0
        for _ in range(n_frames):
            data = stream.read(1024, exception_on_overflow=False)
            samples = struct.unpack('1024h', data)
            mx = 0
            sq = 0
            for s in samples:
                a = s if s >= 0 else -s
                if a > mx:
                    mx = a
                sq += s * s
            if mx > peak:
                peak = mx
            total_sq += sq
            total_n += 1024
        stream.close()
        rms = (total_sq / max(1, total_n)) ** 0.5 / 32768.0
        out['tests']['[%s] %s @%d' % (dev['backend'], name, rate)] = {
            'ok': True,
            'peak': round(peak / 32768.0, 4),
            'rms': round(rms, 5),
        }
    except Exception as e:
        out['tests']['[%s] %s @%d' % (dev['backend'], name, rate)] = {
            'ok': False,
            'error': repr(e),
        }

p.terminate()
with open(r'D:\bs_finally\.audio-tmp\pyaudio_result2.txt', 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print('done')
