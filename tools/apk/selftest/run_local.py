"""Chạy selftest.js trên máy tính (Chromium + máy chủ thử nghiệm), giả lập cầu nối Android `TuViNative`:
call → /api, exportFile → kiểm tra dữ liệu gửi sang (ảnh PNG giải mã được, mô hình PDF hợp lệ), bench → /api/bench.
Cũng tự làm bước “chạm giữ thật” mà CI làm bằng adb (đọc LONGPRESS_TARGET).
Dùng: python3 tools/apk/selftest/run_local.py http://127.0.0.1:8767/   (máy chủ phục vụ thư mục web đã chèn golden vào selftest.js)
"""
import asyncio, base64, json, re, sys
from playwright.async_api import async_playwright

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1:8767/'
MOCK = r"""
window.TuViNative = {
  version: () => 'thử trên máy tính',
  call: (m, a) => { const x = new XMLHttpRequest(); x.open('POST', 'api/' + m, false); x.setRequestHeader('Content-Type', 'application/json; charset=utf-8'); x.send(a); return x.responseText; },
  exportFile: (kind, action, name, payload) => {
    window.__exports = (window.__exports || []).concat([{ kind, action, name, size: payload.length }]);
    if (kind === 'pdf') {
      const m = JSON.parse(payload);
      if (!m.image || !m.blocks || m.blocks.length < 40 || !m.footer2) return JSON.stringify({ ok: false, error: 'mô hình PDF thiếu dữ liệu' });
      window.__pdfModel = m;
      return JSON.stringify({ ok: true, path: action === 'share' ? '' : 'Tải xuống/TuViTuXem/' + name, pages: 2 + Math.ceil(m.blocks.length / 30), bytes: payload.length });
    }
    window.__png = payload;
    return JSON.stringify({ ok: true, path: action === 'share' ? '' : 'Ảnh/TuViTuXem/' + name, pages: 1, bytes: payload.length });
  },
};
"""

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        ctx = await b.new_context(viewport={'width': 412, 'height': 915}, device_scale_factor=2.625, is_mobile=True, has_touch=True)
        await ctx.add_init_script(MOCK)
        pg = await ctx.new_page()
        logs, result, share = [], asyncio.Event(), asyncio.Event()
        target = {}
        def on(m):
            t = m.text
            if 'TUVI_SELFTEST' in t:
                logs.append(t)
                if ' RESULT ' in t: result.set()
                if 'SHARE_STARTED' in t or 'POST_ERROR' in t or 'LONGPRESS_NO_TARGET' in t: share.set()
                mt = re.search(r'LONGPRESS_TARGET x=([\d.]+) y=([\d.]+)', t)
                if mt: target['xy'] = (float(mt.group(1)), float(mt.group(2)))
        pg.on('console', on)
        pg.on('pageerror', lambda e: logs.append('PAGEERROR ' + str(e)))
        await pg.goto(BASE + '#selftest')
        await asyncio.wait_for(result.wait(), 300)
        for _ in range(40):
            if 'xy' in target: break
            await asyncio.sleep(0.25)
        if 'xy' in target:
            x, y = target['xy']
            await pg.mouse.move(x, y); await pg.mouse.down(); await asyncio.sleep(0.9); await pg.mouse.up()
        await asyncio.wait_for(share.wait(), 120)
        png = await pg.evaluate('window.__png || ""')
        if png:
            open('/tmp/selftest_export.png', 'wb').write(base64.b64decode(png))
        print('\n'.join(logs))
        print('EXPORTS', await pg.evaluate('JSON.stringify(window.__exports || [])'))
        await b.close()
        ok = any(re.search(r'RESULT pass=\d+ fail=0', l) for l in logs) and any('LONGPRESS_SEEN' in l for l in logs) and any('SHARE_STARTED' in l for l in logs)
        print('LOCAL_SELFTEST', 'ĐẠT' if ok else 'LỖI')
        sys.exit(0 if ok else 1)

asyncio.run(main())
