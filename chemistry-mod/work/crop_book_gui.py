#!/usr/bin/env python3
"""Crop the vanilla written-book GUI texture to its visible content,
removing the transparent margins. Pure stdlib (zlib/struct), no PIL."""
import binascii
import os
import struct
import zlib

SRC = os.path.expanduser("~/Desktop/MChemistry/手册材质/原版参考/book_gui.png")
DST = os.path.expanduser("~/Desktop/MChemistry/手册材质/原版参考/book_gui_去边.png")


def read_png(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos = 8
    width = height = bitdepth = colortype = None
    palette = None
    trns = None
    idat = b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if ctype == b"IHDR":
            width, height, bitdepth, colortype = struct.unpack(">IIBB", chunk[:10])
        elif ctype == b"PLTE":
            palette = chunk
        elif ctype == b"tRNS":
            trns = chunk
        elif ctype == b"IDAT":
            idat += chunk
        pos += 12 + length
    return width, height, bitdepth, colortype, palette, trns, idat


def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def decode(width, height, bitdepth, colortype, palette, trns, idat):
    assert bitdepth == 8
    if colortype == 6:
        bpp, channels = 4, 4
    elif colortype == 2:
        bpp, channels = 3, 3
    elif colortype == 3:
        bpp, channels = 1, 3
    else:
        raise ValueError("unsupported colortype %d" % colortype)
    raw = zlib.decompress(idat)
    stride = width * bpp
    out = bytearray()
    prev = bytearray(stride)
    pos = 0
    for _ in range(height):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 0xFF
            elif f == 2:
                line[i] = (line[i] + b) & 0xFF
            elif f == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xFF
            elif f == 4:
                line[i] = (line[i] + paeth(a, b, c)) & 0xFF
        out.extend(line)
        prev = line
    # to rgba
    rgba = bytearray()
    if colortype == 6:
        rgba = out
    elif colortype == 2:
        for i in range(0, len(out), 3):
            rgba += out[i:i + 3] + b"\xff"
    else:  # palette
        if trns and len(trns) >= 3:
            def alpha_for(idx):
                return trns[idx] if idx < len(trns) else 0xFF
        else:
            def alpha_for(idx):
                return 0xFF
        for idx in out:
            rgba += palette[idx * 3:idx * 3 + 3] + bytes([alpha_for(idx)])
    return rgba


def write_png(path, width, height, rgba):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        raw += rgba[y * width * 4:(y + 1) * width * 4]

    def chunk(ctype, payload):
        c = ctype + payload
        return struct.pack(">I", len(payload)) + c + struct.pack(">I", binascii.crc32(c) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def main():
    w, h, bd, ct, pal, trns, idat = read_png(SRC)
    print("source:", w, h, "colortype", ct)
    rgba = decode(w, h, bd, ct, pal, trns, idat)
    minx, miny, maxx, maxy = w, h, -1, -1

    def is_blank(i):
        # 原版成书界面四周是纯黑(#000000)空白，内容区为非黑像素
        return rgba[i] < 8 and rgba[i + 1] < 8 and rgba[i + 2] < 8

    for y in range(h):
        for x in range(w):
            if not is_blank((y * w + x) * 4):
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    cw, ch = maxx - minx + 1, maxy - miny + 1
    cropped = bytearray()
    for y in range(miny, maxy + 1):
        cropped += rgba[(y * w + minx) * 4:(y * w + maxx + 1) * 4]
    write_png(DST, cw, ch, cropped)
    print("content bbox:", (minx, miny, maxx, maxy))
    print("cropped:", cw, "x", ch, "->", DST)


if __name__ == "__main__":
    main()
