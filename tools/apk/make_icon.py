"""Vẽ icon app (lưới 12 cung + ngôi sao giữa) bằng Pillow. Chạy: python3 make_icon.py <thư mục res>."""
import sys, math
from PIL import Image, ImageDraw

GOLD = (226, 178, 84, 255)
GOLD_SOFT = (226, 178, 84, 70)
RED = (110, 20, 35, 255)

def draw_mark(img, cx, cy, size):
    d = ImageDraw.Draw(img)
    cell = size / 4.0
    gap = cell * 0.12
    x0, y0 = cx - size / 2, cy - size / 2
    w = max(2, int(size * 0.022))
    for r in range(4):
        for c in range(4):
            if 0 < r < 3 and 0 < c < 3:
                continue
            box = [x0 + c * cell + gap / 2, y0 + r * cell + gap / 2, x0 + (c + 1) * cell - gap / 2, y0 + (r + 1) * cell - gap / 2]
            d.rounded_rectangle(box, radius=cell * 0.12, outline=GOLD, width=w, fill=GOLD_SOFT)
    # sao 4 cánh ở ô trung tâm
    R, r_in = cell * 0.88, cell * 0.22
    pts = []
    for i in range(8):
        ang = -math.pi / 2 + i * math.pi / 4
        rad = R if i % 2 == 0 else r_in
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=GOLD)

def foreground(px):
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    draw_mark(img, px / 2, px / 2, px * 0.50)
    return img

def legacy(px):
    s = px * 4
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([s * 0.04, s * 0.04, s * 0.96, s * 0.96], radius=s * 0.22, fill=RED)
    draw_mark(img, s / 2, s / 2, s * 0.62)
    return img.resize((px, px), Image.LANCZOS)

res = sys.argv[1]
foreground(432).save(f"{res}/drawable-xxxhdpi/ic_launcher_fg.png", optimize=True)
for name, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    legacy(px).save(f"{res}/mipmap-{name}/ic_launcher.png", optimize=True)
# ảnh xem trước để kiểm tra bằng mắt
prev = Image.new("RGBA", (432, 432), RED)
prev.alpha_composite(foreground(432))
prev.save("/tmp/icon_preview.png")
print("icons ok")
