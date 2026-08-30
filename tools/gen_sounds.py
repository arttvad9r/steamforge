"""Генерирует маленькие WAV-звуки для Steamforge в app/src/main/res/raw."""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), "app/src/main/res/raw")
os.makedirs(OUT, exist_ok=True)


def write_wav(name, samples):
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples)
        w.writeframes(frames)
    print(name, len(samples) / RATE, "s")


def tone(freq, dur, vol=0.5, decay=18.0, noise=0.0):
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        env = math.exp(-decay * t)
        phase += 2 * math.pi * freq / RATE
        s = math.sin(phase) + 0.35 * math.sin(2 * phase) + 0.12 * math.sin(3 * phase)
        if noise:
            s += noise * (2.0 * math.sin(i * 12.9898) % 1.0 - 0.5)
        out.append(vol * env * s / 1.5)
    return out


def click(dur=0.045, vol=0.35):
    # механический щелчок: короткий шумовой импульс
    n = int(RATE * dur)
    out = []
    for i in range(n):
        t = i / RATE
        env = math.exp(-90 * t)
        s = ((i * 1103515245 + 12345) % 65536 / 32768.0 - 1.0) * 0.8 + math.sin(2 * math.pi * 640 * t) * 0.4
        out.append(vol * env * s)
    return out


def merge(level):
    # thunk с высотой, растущей с уровнем
    base = 220 * (2 ** (min(level, 11) / 12.0))
    return tone(base, 0.14, vol=0.5, decay=22) + tone(base * 1.5, 0.05, vol=0.2, decay=30)


def steam(dur=0.5, vol=0.4):
    # шум пара
    n = int(RATE * dur)
    out = []
    lp = 0.0
    for i in range(n):
        t = i / RATE
        env = min(1.0, t * 12) * math.exp(-4.5 * t)
        raw = ((i * 7919) % 65536 / 32768.0 - 1.0)
        lp = lp * 0.86 + raw * 0.14
        out.append(vol * env * lp * 2.2)
    return out


def fanfare():
    seq = [(392, 0.11), (523, 0.11), (659, 0.11), (784, 0.22)]
    out = []
    for f, d in seq:
        out += tone(f, d, vol=0.4, decay=8)
    return out


def sad():
    seq = [(392, 0.16), (311, 0.16), (233, 0.30)]
    out = []
    for f, d in seq:
        out += tone(f, d, vol=0.35, decay=9)
    return out


def coin():
    out = tone(988, 0.06, vol=0.35, decay=26)
    out += tone(1319, 0.12, vol=0.3, decay=18)
    return out


def chime():
    return tone(1046, 0.25, vol=0.3, decay=9) + tone(1568, 0.3, vol=0.22, decay=8)


write_wav("sfx_move", click())
write_wav("sfx_merge_low", merge(2))
write_wav("sfx_merge_mid", merge(6))
write_wav("sfx_merge_high", merge(10))
write_wav("sfx_overdrive", steam(0.7, 0.45) + tone(140, 0.5, vol=0.3, decay=5))
write_wav("sfx_gameover", sad())
write_wav("sfx_win", fanfare())
write_wav("sfx_coin", coin())
write_wav("sfx_levelup", chime())
write_wav("sfx_undo", click(0.03, 0.25) + tone(330, 0.08, vol=0.25, decay=26))
