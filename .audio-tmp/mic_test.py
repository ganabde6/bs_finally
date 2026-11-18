# 麦克风实测:从系统默认输入设备录 90 秒,每 5 秒输出一次峰值/RMS 电平
import pyaudio, struct, wave, time

p = pyaudio.PyAudio()
info = p.get_default_input_device_info()
print("默认录音设备: %s (idx=%d, rate=%s, ch=%s)" % (
    info['name'], info['index'], info['defaultSampleRate'], info['maxInputChannels']), flush=True)
rate = int(info['defaultSampleRate'])
ch = int(info['maxInputChannels'])
total = 90
chunk = int(rate * 5)  # 每 5 秒一块

wf = wave.open("D:/bs_finally/.audio-tmp/mic_test.wav", "wb")
wf.setnchannels(ch)
wf.setsampwidth(2)
wf.setframerate(rate)

stream = p.open(format=pyaudio.paInt16, channels=ch, rate=rate, input=True,
                input_device_index=info["index"], frames_per_buffer=chunk)
t0 = time.time()
for i in range(total // 5):
    data = stream.read(chunk, exception_on_overflow=False)
    samples = struct.unpack("%dh" % (ch * chunk), data)
    peak = max(abs(s) for s in samples)
    rms = (sum(s * s for s in samples) / len(samples)) ** 0.5
    print("[%3ds] peak=%6d rms=%9.1f" % (int(time.time() - t0), peak, rms), flush=True)
    wf.writeframes(data)

stream.stop_stream()
stream.close()
wf.close()
p.terminate()
print("已保存: D:/bs_finally/.audio-tmp/mic_test.wav", flush=True)
