# -*- coding: utf-8 -*-
"""本地 Whisper 语音识别脚本(供 Java 后端以子进程方式调用)

用法: python transcribe.py <音频文件路径> [模型名称或路径]
输出: 识别文本打印到 stdout(无其他内容);失败时退出码非 0,错误信息在 stderr
"""
import array
import os
import shutil
import subprocess
import sys
import tempfile
import wave

# 本机 ffmpeg 完整路径(不在 PATH 中也能工作)
FFMPEG_FALLBACK = r"C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links\ffmpeg.exe"


def find_ffmpeg():
    p = shutil.which("ffmpeg")
    if p:
        return p
    if os.path.exists(FFMPEG_FALLBACK):
        return FFMPEG_FALLBACK
    return None


def load_16k_mono(path):
    """把任意格式音频转成 16kHz 单声道 float32 numpy 数组(Whisper 要求)"""
    import numpy as np
    ff = find_ffmpeg()
    if ff:
        raw = tempfile.NamedTemporaryFile(suffix=".pcm", delete=False)
        raw.close()
        try:
            subprocess.check_call(
                [ff, "-hide_banner", "-loglevel", "error", "-y", "-i", path,
                 "-ar", "16000", "-ac", "1", "-f", "s16le", raw.name],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            with open(raw.name, "rb") as f:
                data = f.read()
        finally:
            os.unlink(raw.name)
    else:
        # 无 ffmpeg 时仅支持本来就是 16kHz 单声道 16bit 的 WAV
        w = wave.open(path, "rb")
        if w.getnchannels() != 1 or w.getframerate() != 16000 or w.getsampwidth() != 2:
            raise RuntimeError("ffmpeg 不可用,且输入不是 16kHz/16bit/单声道 WAV")
        data = w.readframes(w.getnframes())
    a = array.array("h")
    a.frombytes(data)
    return np.array(a, dtype=np.float32) / 32768.0


def main():
    if len(sys.argv) < 2:
        sys.stderr.write("usage: transcribe.py <audio> [model]\n")
        sys.exit(2)
    wav_path = sys.argv[1]
    model_arg = sys.argv[2] if len(sys.argv) > 2 else "base.en"
    if not os.path.exists(wav_path):
        sys.stderr.write("audio file not found: %s\n" % wav_path)
        sys.exit(2)

    # 静音快速判断:全程 0 直接返回,不浪费模型推理
    audio = load_16k_mono(wav_path)
    if len(audio) == 0 or float(abs(audio).max()) < 1e-5:
        sys.stdout.write("")
        sys.exit(0)

    import whisper
    model = whisper.load_model(model_arg)
    result = model.transcribe(audio, language="en", fp16=False)
    sys.stdout.write(str(result.get("text", "")).strip())


if __name__ == "__main__":
    main()
