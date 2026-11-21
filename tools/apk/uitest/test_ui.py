"""Kiểm thử giao diện đầu-cuối (Playwright + máy chủ thử nghiệm chạy đúng mã Kotlin của app).

Cổng giai đoạn 2B: 4 lá số golden hiển thị đúng trên lưới 12 cung (mọi chính tinh, phụ tinh trong golden,
Tuần, Triệt, Mệnh, Thân, Cục, tuổi đại hạn, đại hạn/tiểu hạn/lưu niên đại hạn/nguyệt hạn tháng Giêng năm 2026),
cộng các luồng: nhập âm lịch, canh giờ, không rõ giờ, lá số thay thế, lưu/mở/xóa hồ sơ, đổi trường phái, đổi năm.
Giai đoạn 3: thẻ “Cách cục & dấu hiệu”, dấu hiệu năm, luận giải từng cung, “Vì sao?”, chế độ chuyên sâu, không lọt từ cấm.
Giai đoạn 4: màn Luận giải (trọn đời, đại vận, năm, tháng): điểm, thế, chủ đề, 12 cung, 12 đại vận, heat-map tháng, Nên/Không nên.
Giai đoạn 6: Hỏi đáp (thẻ trên lá số, tab Hỏi đáp; câu có dấu/không dấu; “App hiểu là”, điểm, “Vì sao?”, Nên/Không nên,
hỏi tiếp, mở bản luận đầy đủ, 👍/👎, lời hỗ trợ khi khủng hoảng, từ chối, So hai lá số; “Hay hỏi” và xóa lịch sử trong Cài đặt).
Giai đoạn 5: tab Ngày (lịch tháng, chi tiết ngày, 12 giờ), thẻ sao (chạm, chạm giữ), phóng to lưới (nút, chụm hai ngón),
so hai lá số, ước đoán giờ theo sự kiện, tùy chỉnh trường phái từng điểm, xuất ảnh PNG / mô hình PDF, hai cột trên máy tính bảng;
cổng trợ năng (axe-core: không lỗi mức serious/critical; nhãn TalkBack của ô theo thứ tự tên cung, chính tinh, phụ tinh)
và cổng ảnh chụp (so từng điểm ảnh với ảnh chuẩn trong tools/apk/uitest/baseline, lệch tối đa 0,5%).
Chạy: tools/apk/devserver.sh 8765 2026-11-20 &  rồi  python3 tools/apk/uitest/test_ui.py [thư mục ảnh]
Cần: pip playwright + Chromium, Pillow; axe-core: (cd tools/apk/uitest && npm install). Cập nhật ảnh chuẩn: TUVI_UPDATE_BASELINE=1.
"""
import asyncio, json, os, re, shutil, sys, urllib.request
from playwright.async_api import async_playwright
from PIL import Image, ImageChops

BASE = os.environ.get('TUVI_BASE', 'http://127.0.0.1:8765/')
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
GOLDEN = json.load(open(os.path.join(ROOT, 'core/engine/src/test/resources/golden.json'), encoding='utf-8'))
SHOTS = sys.argv[1] if len(sys.argv) > 1 else '/tmp/uishots'
os.makedirs(SHOTS, exist_ok=True)
CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi']
HOA = {'Hóa Lộc': 'LOC', 'Hóa Quyền': 'QUYEN', 'Hóa Khoa': 'KHOA', 'Hóa Kỵ': 'KY'}
KB = os.path.join(ROOT, 'data/kb/src/main/resources/kb')
RULE_NAME = {r['id']: r['name'] for f in json.load(open(os.path.join(KB, 'index.json'), encoding='utf-8'))['files']['rules']
             for r in json.load(open(os.path.join(KB, f), encoding='utf-8'))['rules']}
# Mục 14.3: các từ không được xuất hiện trong câu luận (đồng bộ với KbLint.FORBIDDEN).
FORBIDDEN = ['chắc chắn', '100%', 'không thể tránh', 'định mệnh', 'tử vong', 'chết', 'đoản thọ', 'tuổi thọ', 'sảy thai', 'ly hôn',
             'tù tội', 'lao lý', 'tai nạn chết người', 'hạn chí đầu hà', 'dị bào', 'giới tính thai', 'con trai', 'con gái',
             'giải hạn', 'cúng sao', 'dâng sao']
def forbidden_in(text):
    low = text.lower()
    return [w for w in FORBIDDEN if re.search(r'(?<![\wÀ-ỹ])' + re.escape(w) + r'(?![\wÀ-ỹ])', low)]

HERE = os.path.dirname(os.path.abspath(__file__))
BASELINE = os.path.join(HERE, 'baseline')
AXE = os.environ.get('TUVI_AXE') or os.path.join(HERE, 'node_modules/axe-core/axe.min.js')
UPDATE_BASELINE = os.environ.get('TUVI_UPDATE_BASELINE') == '1'

def api(method, arg):
    req = urllib.request.Request(BASE + 'api/' + method, data=json.dumps(arg).encode(), headers={'Content-Type': 'application/json'})
    r = json.loads(urllib.request.urlopen(req).read())
    assert r['ok'], r
    return r['data']

failures, checks = [], 0
def check(cond, msg):
    global checks
    checks += 1
    if not cond:
        failures.append(msg)
        print('  ✗', msg)

# Đọc toàn bộ lưới: mỗi cung → tên sao gốc (không gồm sao lưu), huy hiệu hóa gốc, dấu hạn, tuổi đại hạn.
READ_BOARD = """() => {
  const out = {};
  document.querySelectorAll('#board .cell').forEach(c => {
    const name = el => Array.from(el.childNodes).filter(n => n.nodeType === 3).map(n => n.textContent).join('').trim();
    const stars = Array.from(c.querySelectorAll('.ms, .c-minor .st')).map(name);
    const ring = (c.querySelector('.c-foot span') || {}).textContent || '';
    out[c.dataset.chi] = {
      stars: stars.concat(ring.trim() ? [ring.trim()] : []),
      hoa: Array.from(c.querySelectorAll('.c-hoa .hb:not(.l)')).map(h => h.className.split(' ')[1]),
      marks: Array.from(c.querySelectorAll('.mk')).map(m => m.textContent),
      dh: (c.querySelector('.c-dh') || {}).textContent,
      role: (c.querySelector('.c-role') || {}).textContent,
    };
  });
  const tt = Array.from(document.querySelectorAll('#board .tt')).map(t => t.textContent);
  return { cells: out, tt, center: document.querySelector('#board .center').textContent };
}"""

def parse_input(s):
    m = re.match(r'(Nam|Nữ) (\d+)/(\d+)/(\d+) (\d+):(\d+)', s)
    g, d, mo, y, h, mi = m.groups()
    return {'gender': 'NAM' if g == 'Nam' else 'NU', 'day': str(int(d)), 'month': str(int(mo)), 'year': y, 'hour': str(int(h)), 'minute': str(int(mi))}

# Cổng ảnh chụp: so với ảnh chuẩn (cùng kích thước, ≤ 0,5% điểm ảnh lệch quá 40/255 ở một kênh màu).
async def shot(page, name, full_page=False):
    path = f'{SHOTS}/{name}.png'
    await page.evaluate('document.activeElement && document.activeElement.blur && document.activeElement.blur()')
    await page.wait_for_timeout(350)
    await page.screenshot(path=path, full_page=full_page)
    compare_png(path, name)

def compare_png(path, name):
    base = os.path.join(BASELINE, name + '.png')
    if UPDATE_BASELINE or not os.path.exists(base):
        os.makedirs(BASELINE, exist_ok=True)
        shutil.copy(path, base)
        print(f'  (ảnh chuẩn mới: {name})')
        return
    a, b = Image.open(path).convert('RGB'), Image.open(base).convert('RGB')
    if a.size != b.size:
        check(False, f'Ảnh chụp {name}: kích thước {a.size} khác ảnh chuẩn {b.size}')
        return
    diff = ImageChops.difference(a, b).convert('L').point(lambda v: 255 if v > 40 else 0)
    ratio = diff.histogram()[255] / (a.size[0] * a.size[1])
    if ratio > 0.005:
        diff.save(f'{SHOTS}/{name}.diff.png')
    check(ratio <= 0.005, f'Ảnh chụp {name} lệch ảnh chuẩn {ratio:.2%} (xem {name}.diff.png)')

# Cổng trợ năng: axe-core, các quy tắc WCAG 2.1 A/AA; không được có lỗi mức serious hoặc critical.
async def axe_check(page, tag):
    if not os.path.exists(AXE):
        check(False, f'Thiếu axe-core ({AXE}) — chạy: cd tools/apk/uitest && npm install')
        return
    if not await page.evaluate('!!window.axe'):
        await page.add_script_tag(path=AXE)
    res = await page.evaluate("""async () => {
      const r = await axe.run(document, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] } });
      return r.violations.filter(v => v.impact === 'serious' || v.impact === 'critical')
        .map(v => v.id + ' (' + v.impact + '): ' + v.nodes.slice(0, 3).map(n => n.target.join(' ')).join(' | ') + ' — ' + v.nodes.length + ' chỗ');
    }""")
    check(not res, f'Trợ năng {tag}: {res}')

async def pick(page, sel, prefix):
    v = await page.evaluate("([s, p]) => (Array.from(document.querySelector(s).options).find(o => o.text.startsWith(p)) || {}).value", [sel, prefix])
    await page.select_option(sel, v)

async def dismiss_modal(page):
    if await page.locator('#modal.open').count():
        await page.click('#modal [data-act="modal-ok"]')

async def fill_form(page, inp, name, calendar='solar', time_mode='clock', canh=None, leap=False, view_year='2026'):
    await page.click('[data-act="new"]')
    await page.fill('[data-f="name"]', name)
    await page.click(f'[data-act="seg"][data-f="gender"][data-v="{inp["gender"]}"]')
    if calendar == 'lunar':
        await page.click('[data-act="seg"][data-f="calendar"][data-v="lunar"]')
    await page.select_option('[data-f="day"]', inp['day'])
    await page.select_option('[data-f="month"]', inp['month'])
    await page.fill('[data-f="year"]', inp['year'])
    if leap:
        await page.check('[data-f="leap"]')
    if time_mode == 'clock':
        await page.select_option('[data-f="hour"]', inp['hour'])
        await page.select_option('[data-f="minute"]', inp['minute'])
    else:
        await page.click(f'[data-act="seg"][data-f="timeMode"][data-v="{time_mode}"]')
        if time_mode == 'canh':
            await page.select_option('[data-f="canh"]', canh)
    await page.fill('[data-f="viewYear"]', view_year)

def find_star(board, star):
    return [int(k) for k, v in board['cells'].items() if star in v['stars']]

def verify_golden(g, board, tag):
    for star, chi in list(g['chinhTinh'].items()) + list(g['phuTinh'].items()):
        if star in HOA:
            where = [int(k) for k, v in board['cells'].items() if HOA[star] in v['hoa']]
        else:
            where = find_star(board, star)
        check(where == [CHI.index(chi)], f'{tag}: {star} phải ở {chi}, giao diện hiện ở {[CHI[i] for i in where]}')
    cells = board['cells']
    menh = [int(k) for k, v in cells.items() if v['role'].startswith('Mệnh')]
    than = [int(k) for k, v in cells.items() if 'THÂN' in v['role']]
    check(menh == [CHI.index(g['menh'])], f'{tag}: cung Mệnh phải ở {g["menh"]}')
    check(than == [CHI.index(g['than'])], f'{tag}: Thân phải ở {g["than"]}')
    cuc_name = {'Thủy 2': 'Thủy nhị cục', 'Mộc 3': 'Mộc tam cục', 'Kim 4': 'Kim tứ cục', 'Thổ 5': 'Thổ ngũ cục', 'Hỏa 6': 'Hỏa lục cục'}[g['cuc']]
    check(cuc_name in board['center'], f'{tag}: ô trung tâm phải ghi {cuc_name}')
    check(g['lunar'].replace('N', ' nhuận') in board['center'] or g['lunar'] in board['center'], f'{tag}: ô trung tâm phải ghi âm lịch {g["lunar"]}')
    for item in g['daiHan3']:
        chi, ages = item.split(' ')
        check(cells[str(CHI.index(chi))]['dh'] == ages.split('-')[0], f'{tag}: cung {chi} phải có tuổi đại hạn {ages.split("-")[0]}')
    n = g['nam2026']
    for mark, key in [('ĐH', 'daiHan'), ('TH', 'tieuHan'), ('LN', 'luuNienDaiHan')]:
        where = [int(k) for k, v in cells.items() if mark in v['marks']]
        check(where == [CHI.index(n[key])], f'{tag}: dấu {mark} năm 2026 phải ở {n[key]}, đang ở {[CHI[i] for i in where]}')
    check(f'{n["tuoiAm"]} tuổi' in board['center'], f'{tag}: tuổi âm 2026 phải là {n["tuoiAm"]}')
    tuan_same = sorted(g['tuan']) == sorted(g['triet'])
    check(('TUẦN · TRIỆT' in board['tt'] or 'TUẦN·TRIỆT' in board['tt']) if tuan_same else ('TUẦN' in board['tt'] and any(t.startswith('TRI') for t in board['tt'])),
          f'{tag}: thiếu nhãn Tuần/Triệt trên lưới ({board["tt"]})')

async def launch_chromium(p):
    """Chromium của Playwright; nếu bản Playwright không khớp trình duyệt đã cài (máy không tải được trình duyệt mới),
    dùng Chromium có sẵn của máy (TUVI_CHROMIUM hoặc /opt/pw-browsers)."""
    try:
        return await p.chromium.launch()
    except Exception as e:
        for exe in (os.environ.get('TUVI_CHROMIUM'), '/opt/pw-browsers/chromium_headless_shell', '/opt/pw-browsers/chromium'):
            if exe and os.path.exists(exe):
                print(f'  (dùng Chromium có sẵn: {exe})')
                return await p.chromium.launch(executable_path=exe)
        raise e

async def main():
    async with async_playwright() as p:
        browser = await launch_chromium(p)
        ctx = await browser.new_context(viewport={'width': 412, 'height': 915}, device_scale_factor=2, is_mobile=True, has_touch=True, locale='vi-VN')
        page = await ctx.new_page()
        errors = []
        page.on('pageerror', lambda e: errors.append(str(e)))
        page.on('console', lambda m: errors.append(m.text) if m.type == 'error' else None)
        await page.goto(BASE)
        check(await page.locator('#modal.open').count() == 1, 'Lần đầu mở phải hiện lời ngỏ')
        check('di sản văn hóa' in await page.inner_text('#modal'), 'Lời ngỏ phải đúng nguyên văn mục 11')
        await dismiss_modal(page)

        # —— 4 ca golden, nhập dương lịch giờ:phút ——
        for g in GOLDEN:
            tag = 'Ca ' + g['id']
            print(tag, g['input'])
            await fill_form(page, parse_input(g['input']), tag)
            await page.click('[data-act="submit"]')
            check(await page.locator('h1:has-text("Xác nhận lá số")').count() == 1, f'{tag}: phải sang thẻ xác nhận')
            conf = await page.inner_text('#app')
            check(g['lunar'].replace('N', ' nhuận') in conf, f'{tag}: thẻ xác nhận phải ghi âm lịch {g["lunar"]}')
            check(f'năm {g["nam"]}' in conf, f'{tag}: thẻ xác nhận phải ghi năm {g["nam"]}')
            if g['id'] == 'B':
                check('Tý muộn' in conf, 'Ca B: phải cảnh báo giờ Tý muộn')
            if g['id'] == 'C':
                check('tháng nhuận' in conf, 'Ca C: phải cảnh báo tháng nhuận')
            await page.screenshot(path=f'{SHOTS}/{g["id"]}_1_confirm.png', full_page=True)
            await page.click('[data-act="confirm"]')
            await page.wait_for_selector('#board')
            board = await page.evaluate(READ_BOARD)
            verify_golden(g, board, tag)
            # nguyệt hạn tháng Giêng
            await page.click('[data-act="layer"][data-k="month"]')
            await page.click('[data-act="month"][data-m="1"]')
            board = await page.evaluate(READ_BOARD)
            where = [int(k) for k, v in board['cells'].items() if 'NH1' in v['marks']]
            check(where == [CHI.index(g['nam2026']['nguyetHanThang1'])], f'{tag}: nguyệt hạn tháng 1 phải ở {g["nam2026"]["nguyetHanThang1"]}')
            await page.click('[data-act="layer"][data-k="month"]')
            await page.screenshot(path=f'{SHOTS}/{g["id"]}_2_chart.png')
            await page.click('[data-act="back"]')  # về trang chủ

        # —— hồ sơ đã lưu ——
        names = await page.locator('.profile .meta b').all_inner_texts()
        check(names == ['Ca D', 'Ca C', 'Ca B', 'Ca A'], f'Danh sách lá số đã lưu sai: {names}')
        await page.click('.profile:has-text("Ca A")')
        await page.wait_for_selector('#board')
        verify_golden(GOLDEN[0], await page.evaluate(READ_BOARD), 'Mở lại Ca A')

        # —— luận giải sơ bộ (giai đoạn 3) trên ca A, năm 2026 ——
        rd = await page.inner_text('#reading')
        check('nháp' not in rd.lower() and 'thẩm định' not in rd, 'Thẻ luận giải không còn ghi “bản nháp” (D-61)')
        n_items = await page.locator('#reading .rd').count()
        check(n_items >= 3, f'Ca A phải có ít nhất 3 cách cục/dấu hiệu, đang có {n_items}')
        for rid in ['TS_THIEN_TUONG_XET_GIAP', 'TS_AM_DUONG_THUAN_LY']:
            check(RULE_NAME[rid] in rd, f'Ca A phải có “{RULE_NAME[rid]}” ({rid})')
        for rid in ['CC_TAI_AM_GIAP_AN', 'CC_PHU_TUONG_TRIEU_VIEN']:
            check(RULE_NAME[rid] not in rd, f'Ca A không được có “{RULE_NAME[rid]}” ({rid})')
        yc = await page.inner_text('.card:has-text("Vận hạn năm 2026")')
        check('Dấu hiệu năm 2026' in yc, 'Thẻ vận hạn phải có mục dấu hiệu năm 2026')
        for rid in ['VH_LUU_THAI_TUE_QUAN_LOC', 'VH_THIEN_THOI_TUOI_KHAC_DAI_HAN']:
            check(RULE_NAME[rid] in yc, f'Năm 2026 ca A phải có “{RULE_NAME[rid]}” ({rid})')
        texts = await page.evaluate("() => Array.from(document.querySelectorAll('.rd-t')).map(e => e.textContent)")
        check(all('{' not in t and '}' not in t for t in texts), 'Câu luận còn ô {…} chưa điền')
        check(all(t.strip() for t in texts), 'Có câu luận rỗng')
        # “Vì sao?”: chạm mở, chạm lần nữa đóng; danh sách căn cứ phải có sao và cung.
        first = page.locator('#reading .rd >> nth=0')
        await first.click()
        check(await first.evaluate('e => e.classList.contains("open")'), 'Chạm mục luận giải phải mở phần “Vì sao?”')
        why = await first.locator('.rd-more').inner_text()
        check('tại' in why and 'Nguồn' in why, f'Phần “Vì sao?” phải có căn cứ và nguồn: {why[:120]}')
        await page.screenshot(path=f'{SHOTS}/A_4_reading.png', full_page=True)
        await first.click()
        check(not await first.evaluate('e => e.classList.contains("open")'), 'Chạm lần nữa phải thu gọn')
        # Bảng chi tiết cung Mệnh: câu luận bộ Vũ Khúc – Thiên Tướng.
        await page.click('.cell[data-chi="2"]')
        await page.wait_for_selector('#sheet.open')
        sh = await page.inner_text('#sheet')
        check('luận giải sơ bộ' in sh.lower() and 'Vũ Khúc' in sh and 'Thiên Tướng' in sh, 'Bảng cung Mệnh phải có luận giải sơ bộ bộ Vũ Tướng')
        check(await page.locator('#sheet .pl').count() >= 2, 'Bảng cung Mệnh phải có từ 2 câu luận sao')
        await page.screenshot(path=f'{SHOTS}/A_5_sheet_reading.png')
        bad = forbidden_in(await page.inner_text('#app') + sh)
        check(not bad, f'Lọt từ cấm (mục 14.3): {bad}')
        await page.evaluate('window.tvBack()')
        # Quét từ cấm trên nhiều năm và cả 12 cung.
        all_text = ''
        for yv in range(2020, 2032):
            await page.evaluate(f"(() => {{ const r = document.querySelector('[data-act=year-range]'); r.value = '{yv}'; r.dispatchEvent(new Event('change', {{bubbles: true}})); }})()")
            all_text += await page.inner_text('#app')
        check('Năm 2031' in await page.inner_text('.yv'), 'Thanh trượt năm phải chuyển tới 2031')
        for chi in range(12):
            await page.click(f'.cell[data-chi="{chi}"]'); await page.wait_for_selector('#sheet.open')
            all_text += await page.inner_text('#sheet')
            await page.evaluate('window.tvBack()')
        bad = forbidden_in(all_text)
        check(not bad, f'Lọt từ cấm khi xem 12 cung: {bad}')
        # Chế độ chuyên sâu.
        await page.evaluate('window.tvBack()')
        await page.click('[data-act="settings"]')
        await page.click('[data-act="expert"]')
        check('đang bật' in await page.inner_text('[data-act="expert"]'), 'Bật chế độ chuyên sâu không thành công')
        await page.evaluate('window.tvBack()')
        await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
        check('chế độ chuyên sâu' in await page.inner_text('#reading'), 'Thẻ luận giải phải ghi đang bật chế độ chuyên sâu')
        await page.evaluate('window.tvBack()')
        await page.click('[data-act="settings"]'); await page.click('[data-act="expert"]'); await page.evaluate('window.tvBack()')
        await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
        check('chế độ chuyên sâu' not in await page.inner_text('#reading'), 'Tắt chế độ chuyên sâu không thành công')

        # —— màn Luận giải (giai đoạn 4) ——
        await page.click('[data-act="open-reading"][data-s="year"]')
        await page.wait_for_selector('.rsum')
        rs = await page.inner_text('#app')
        check('Năm 2026 (Bính Ngọ)' in rs and 'Thế ' in rs, 'Luận năm phải có tiêu đề năm 2026 và thế giai đoạn')
        check(await page.locator('.rs').count() == 7, f'Luận năm phải có 7 chủ đề, đang có {await page.locator(".rs").count()}')
        check(await page.locator('.heat .hm').count() == 12, 'Luận năm phải có heat-map 12 tháng')
        nen = await page.locator('.radv h3.adv-NEN + ul li').count()
        khong = await page.locator('.radv h3.adv-KHONG_NEN + ul li').count()
        check(3 <= nen <= 5 and 3 <= khong <= 5, f'Nên {nen}, Không nên {khong} phải trong 3–5')
        check('khả năng cao' in rs and 'cưới hỏi' in rs, 'Ca A năm 2026: hạn hỷ đủ ba tầng phải thành “khả năng cao”')
        check('Đức năng thắng số' in rs, 'Bản luận phải kết thúc bằng câu Đức năng thắng số')
        n_rp = await page.locator('.rp').count()
        n_ids = await page.evaluate("() => Array.from(document.querySelectorAll('.rp')).filter(e => e.querySelector('.rp-why .w') && e.querySelector('.ids').textContent.trim().length > 4).length")
        check(n_rp > 10 and n_ids == n_rp, f'Mọi đoạn luận phải có “Vì sao?” và mã luật ({n_ids}/{n_rp})')
        first = page.locator('.rp >> nth=1')
        await first.click()
        check(await first.evaluate('e => e.classList.contains("open")'), 'Chạm đoạn luận phải mở “Vì sao?”')
        await page.screenshot(path=f'{SHOTS}/A_6_luan_nam.png', full_page=True)
        all_luan = rs
        await page.click('.heat .hm[data-m="3"]')
        await page.wait_for_selector('.rsum')
        check('Tháng 3 âm lịch' in await page.inner_text('.rsum'), 'Chạm ô tháng 3 trên heat-map phải mở luận tháng 3')
        all_luan += await page.inner_text('#app')
        await page.click('[data-act="rscope"][data-s="decade"]')
        await page.wait_for_selector('.rsum')
        dv = await page.inner_text('#app')
        check('Đại vận 35–44 tuổi' in dv, 'Ca A năm 2026 đang ở đại vận 35–44 tuổi')
        all_luan += dv
        await page.click('[data-act="rscope"][data-s="life"]')
        await page.wait_for_selector('.rsum')
        check('Tử vi trọn đời' in await page.inner_text('.rsum'), 'Tab Trọn đời')
        check(await page.locator('.rs-h[data-act="pal"]').count() == 12, '12 cung từng mục')
        check(await page.locator('.tl .dh').count() == 12, 'Dòng thời gian 12 đại vận')
        await page.click('.rs-h[data-act="pal"] >> nth=0')
        check(await page.locator('#rs-palace_MENH .rp').count() >= 2, 'Mở cung Mệnh phải hiện các đoạn luận')
        all_luan += await page.inner_text('#app')
        await page.screenshot(path=f'{SHOTS}/A_7_tron_doi.png', full_page=True)
        await page.click('.tl .dh >> nth=4')
        await page.wait_for_selector('.rsum')
        check('Đại vận 45–54 tuổi' in await page.inner_text('.rsum'), 'Chạm đại vận thứ 5 phải mở đại vận 45–54 tuổi')
        bad = forbidden_in(all_luan)
        check(not bad, f'Lọt từ cấm trong bản luận: {bad}')
        check('{' not in all_luan and '}' not in all_luan, 'Bản luận còn ô {…} chưa điền')
        await page.evaluate('window.tvBack()')
        check(await page.locator('#board').count() == 1, 'Back từ màn luận giải phải về lá số')
        # đổi năm: ▶ sang 2027 rồi chạm đại hạn đầu
        await page.click('[data-act="year"][data-d="1"]')
        check('Năm 2027' in await page.inner_text('.yv'), 'Nút ▶ phải chuyển sang năm 2027')
        await page.click('.tl .dh >> nth=0')
        check('Năm 1994' in await page.inner_text('.yv'), 'Chạm đại hạn đầu (5–14) phải chuyển tới năm 1994')
        # chạm cung → bảng chi tiết
        await page.click('.cell[data-chi="2"]')
        await page.wait_for_selector('#sheet.open')
        sheet = await page.inner_text('#sheet')
        check('Cung Mệnh' in sheet and 'Vũ Khúc' in sheet and 'Vượng' in sheet, 'Bảng chi tiết cung Mệnh ca A phải có Vũ Khúc (Vượng)')
        check('Tam hợp' in sheet and 'Xung chiếu' in sheet, 'Bảng chi tiết phải có tam hợp, xung chiếu')
        sel = await page.evaluate("() => ['sel','tam','giap','nhihop'].map(c => Array.from(document.querySelectorAll('#board .cell.'+c)).map(e => +e.dataset.chi).sort((a,b)=>a-b))")
        check(sel == [[2], [6, 8, 10], [1, 3], [11]], f'Tô sáng tam phương tứ chính/giáp/nhị hợp của Dần sai: {sel}')
        await page.screenshot(path=f'{SHOTS}/A_3_sheet.png')
        await page.evaluate('window.tvBack()')
        check(await page.locator('#sheet.open').count() == 0, 'Nút Back phải đóng bảng chi tiết trước')
        await page.evaluate('window.tvBack()')
        check(await page.locator('[data-act="new"]').count() == 1, 'Nút Back từ lá số phải về trang chủ')

        # —— nhập âm lịch, tháng nhuận (ca C) ——
        await fill_form(page, {'gender': 'NAM', 'day': '1', 'month': '2', 'year': '2023', 'hour': '6', 'minute': '0'}, 'C âm lịch', calendar='lunar', leap=True)
        check('Dương lịch: Thứ Tư, 22/03/2023' in await page.inner_text('#conv'), 'Gợi ý đổi lịch: 1/2 nhuận/2023 phải là 22/03/2023')
        await page.click('[data-act="submit"]')
        await page.click('[data-act="confirm"]')
        await page.wait_for_selector('#board')
        verify_golden(GOLDEN[2], await page.evaluate(READ_BOARD), 'Ca C nhập âm lịch')
        await page.click('[data-act="back"]')

        # —— canh giờ (ca D: giờ Mùi) ——
        await fill_form(page, parse_input(GOLDEN[3]['input']), 'D canh giờ', time_mode='canh', canh='MUI')
        await page.click('[data-act="submit"]')
        await page.click('[data-act="confirm"]')
        await page.wait_for_selector('#board')
        verify_golden(GOLDEN[3], await page.evaluate(READ_BOARD), 'Ca D chọn canh Mùi')
        await page.click('[data-act="back"]')

        # —— không rõ giờ → chọn giờ Tỵ (ca A) ——
        await fill_form(page, parse_input(GOLDEN[0]['input']), 'A không rõ giờ', time_mode='unknown')
        await page.click('[data-act="submit"]')
        rows = await page.locator('.hours .h').count()
        check(rows == 12, f'Không rõ giờ phải ra 12 lá số, đang có {rows}')
        await page.screenshot(path=f'{SHOTS}/hours.png', full_page=True)
        await page.click('.hours .h[data-canh="TI"]')
        await page.click('[data-act="confirm"]')
        await page.wait_for_selector('#board')
        verify_golden(GOLDEN[0], await page.evaluate(READ_BOARD), 'Ca A chọn từ 12 giờ')
        await page.click('[data-act="back"]')

        # —— lá số thay thế (ca B, Tý muộn → giữ nguyên ngày) ——
        await fill_form(page, parse_input(GOLDEN[1]['input']), 'B thay thế')
        await page.click('[data-act="submit"]')
        await page.click('.warn:has-text("Tý muộn") [data-act="alt"]')
        await page.wait_for_selector('#board')
        alt = await page.evaluate(READ_BOARD)
        check('lá số thay thế' in alt['center'], 'Lá số thay thế phải ghi rõ trên ô trung tâm')
        check('12/2/1985' in alt['center'], f'Lá số thay thế ca B (giữ ngày) phải là 12/2/1985')
        await page.click('[data-act="alt-off"]')
        verify_golden(GOLDEN[1], await page.evaluate(READ_BOARD), 'Ca B sau khi về lá số chính')
        await page.evaluate('window.tvBack()')
        check(await page.locator('h1:has-text("Xác nhận lá số")').count() == 1, 'Back từ lá số thay thế phải về thẻ xác nhận')
        await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')
        check(await page.locator('[data-act="new"]').count() == 1, 'Back tiếp phải về trang chủ')

        # —— kiểm tra lỗi nhập ——
        await page.click('[data-act="new"]')
        await page.click('[data-act="submit"]')
        check('Chưa chọn giới tính' in await page.inner_text('#app'), 'Phải báo lỗi khi chưa chọn giới tính')
        await page.click('[data-act="seg"][data-f="gender"][data-v="NU"]')
        await page.select_option('[data-f="day"]', '31'); await page.select_option('[data-f="month"]', '2')
        await page.fill('[data-f="year"]', '2001'); await page.select_option('[data-f="hour"]', '8')
        await page.click('[data-act="submit"]')
        check('không có trong dương lịch' in await page.inner_text('#app'), 'Phải báo ngày 31/2 không tồn tại')
        await page.evaluate('window.tvBack()')

        # —— xóa hồ sơ ——
        before = await page.locator('.profile').count()
        await page.click('.profile:has-text("A không rõ giờ") [data-act="del-profile"]')
        await page.click('#modal [data-act="modal-ok"]')
        check(await page.locator('.profile').count() == before - 1, 'Xóa hồ sơ không thành công')

        # —— đổi trường phái ——
        await page.click('[data-act="settings"]')
        await page.click('[data-act="preset"][data-k="THIEN_LUONG"]')
        await page.evaluate('window.tvBack()')
        await page.click('.profile:has-text("Ca A")')
        await page.wait_for_selector('#board')
        b = await page.evaluate(READ_BOARD)
        check('Theo cụ Thiên Lương' in b['center'], 'Ô trung tâm phải ghi trường phái đang dùng')
        # năm Canh theo Thiên Lương: Thiên Đồng hóa Khoa (Sửu), Thái Âm hóa Kỵ (Hợi)
        check('KHOA' in b['cells'][str(CHI.index('Sửu'))]['hoa'] and 'KY' in b['cells'][str(CHI.index('Hợi'))]['hoa'],
              'Preset Thiên Lương: năm Canh phải Đồng Khoa (Sửu), Âm Kỵ (Hợi)')
        await page.evaluate('window.tvBack()')
        await page.click('[data-act="settings"]'); await page.click('[data-act="preset"][data-k="NAM_PHAI_VN"]'); await page.evaluate('window.tvBack()')

        # ———————————————— giai đoạn 5 ————————————————
        await phase5(page, browser)
        # ———————————————— giai đoạn 6 ————————————————
        await phase6(page, browser)
        # ———————————————— giai đoạn 7: Trợ lý AI (máy chủ thử dùng model giả lập) ————————————————
        await phase7(page)
        # ———————————————— 0.8.0: sách bàn, liên hệ, bỏ nhãn nháp, icon ————————————————
        await phase8(page)
        # ———————————————— 0.8.1: đoán năm cưới, đối chiếu năm đã cưới ————————————————
        await phase81(page)

        check(not errors, f'Lỗi JavaScript: {errors}')

        # —— ảnh chụp: tối, ngang ——
        dark = await browser.new_context(viewport={'width': 412, 'height': 915}, device_scale_factor=2, is_mobile=True, has_touch=True, color_scheme='dark')
        dp = await dark.new_page()
        await dp.goto(BASE); await dismiss_modal(dp)
        await dp.click('.profile:has-text("Ca A")'); await dp.wait_for_selector('#board')
        await dp.screenshot(path=f'{SHOTS}/dark_chart.png')
        await dp.set_viewport_size({'width': 860, 'height': 390})  # S22 Ultra xoay ngang (trừ thanh điều hướng)
        await dp.wait_for_timeout(300)
        await dp.evaluate("document.querySelector('#board').scrollIntoView()")
        await dp.screenshot(path=f'{SHOTS}/dark_landscape.png')
        check(await dp.evaluate("getComputedStyle(document.querySelector('.chart-2p')).gridTemplateColumns.split(' ').length") == 2,
              'Màn ngang 860 px: lá số phải chia hai cột')
        await browser.close()
    print(f'\n{checks - len(failures)}/{checks} kiểm tra đạt')
    if failures:
        print('THẤT BẠI:'); [print(' -', f) for f in failures]
        sys.exit(1)

GOLDEN_A_INPUT = {'gender': 'NAM', 'calendar': 'solar', 'day': 15, 'month': 8, 'year': 1990, 'timeMode': 'clock', 'hour': 10, 'minute': 30,
                  'region': 'VN_BAC', 'preset': 'NAM_PHAI_VN'}

async def phase5(page, browser):
    print('Giai đoạn 5')
    all_text = ''
    # —— trang chủ: nút so hai lá số; lá số Ca A ——
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    # nhãn TalkBack của ô: tên cung → chính tinh → phụ tinh (mục 13)
    lab = await page.get_attribute('.cell[data-chi="2"]', 'aria-label')
    check(lab.startswith('Cung Mệnh, Mậu Dần. Chính tinh: Vũ Khúc Vượng, Thiên Tướng Miếu.') and 'Tứ Hóa: Hóa Quyền' in lab and lab.index('Chính tinh') < lab.index('Phụ tinh'),
          f'Nhãn TalkBack ô Mệnh sai thứ tự hoặc thiếu: {lab}')
    check('Thiên Việt' in lab and 'Thiên Hình' in lab, f'Nhãn TalkBack phải đọc cả phụ tinh: {lab}')
    check(await page.get_attribute('.cell[data-chi="2"] .c-in', 'aria-hidden') == 'true', 'Nội dung ô phải ẩn với TalkBack (đọc nhãn gộp)')
    check(await page.evaluate("Array.from(document.querySelectorAll('#app div[data-act]')).every(e => e.getAttribute('role') === 'button' && e.tabIndex === 0)"),
          'Mọi phần tử chạm được phải có role=button và tabindex')
    await shot(page, 'p5_chart_A')
    await axe_check(page, 'lá số')
    # bàn phím: Enter trên ô = chạm
    await page.focus('.cell[data-chi="2"]'); await page.keyboard.press('Enter')
    await page.wait_for_selector('#sheet.open')
    check('Cung Mệnh' in await page.inner_text('#sheet'), 'Enter trên ô phải mở bảng cung')

    # —— thẻ sao: chạm sao trong bảng cung ——
    check(await page.locator('#sheet [data-act="star"]').count() >= 6, 'Bảng cung Mệnh phải có các sao chạm được')
    await page.click('#sheet [data-act="star"][data-k="VU_KHUC"]')
    await page.wait_for_selector('#sheet .starcard')
    sc = await page.inner_text('#sheet')
    for w in ['Vũ Khúc', 'Chính tinh', 'Dần', 'Vượng', 'Bắc Đẩu', 'Âm Kim', 'Tài Tinh', 'Hóa Quyền (can năm Canh)', 'Ý NGHĨA TẠI CUNG MỆNH']:
        check(w.lower() in sc.lower(), f'Thẻ sao Vũ Khúc thiếu “{w}”')
    await shot(page, 'p5_star_vu_khuc')
    await axe_check(page, 'thẻ sao')
    all_text += sc
    await page.click('#sheet [data-act="palace-back"]')
    check('Cung Mệnh' in await page.inner_text('#sheet'), 'Nút “Về cung Mệnh” phải quay lại bảng cung')
    # phụ tinh, sao vòng Tràng Sinh cũng có thẻ
    await page.click('#sheet [data-act="star"][data-k="THIEN_VIET"]'); await page.wait_for_selector('#sheet .starcard[data-star="THIEN_VIET"]')
    check('Thiên Việt' in await page.inner_text('#sheet'), 'Thẻ sao phụ tinh Thiên Việt')
    await page.evaluate('window.tvBack()')
    await page.wait_for_timeout(400)  # chờ bảng trượt xuống hết (chuột thô không tự chờ như click)
    # chạm giữ một sao trên lưới (0,55 s) → thẻ sao, không mở bảng cung
    box = await page.locator('.cell[data-chi="2"] .ms[data-k="THIEN_TUONG"]').bounding_box()
    await page.mouse.move(box['x'] + box['width'] / 2, box['y'] + box['height'] / 2)
    await page.mouse.down(); await page.wait_for_timeout(800); await page.mouse.up()
    await page.wait_for_timeout(200)
    check(await page.locator('#sheet.open .starcard[data-star="THIEN_TUONG"]').count() == 1, 'Chạm giữ Thiên Tướng trên lưới phải mở thẻ sao')
    await page.evaluate('window.tvBack()')
    # chạm nhanh vẫn mở bảng cung như cũ
    await page.click('.cell[data-chi="2"] .ms[data-k="THIEN_TUONG"]'); await page.wait_for_selector('#sheet.open')
    check(await page.locator('#sheet .starcard').count() == 0 and 'Cung Mệnh' in await page.inner_text('#sheet'), 'Chạm nhanh một sao phải mở bảng cung')
    await page.evaluate('window.tvBack()')

    # —— phóng to lưới: nút ＋/−/⟲ và chụm hai ngón (CDP touch) ——
    await page.click('[data-act="zoom"][data-z="1"]'); await page.click('[data-act="zoom"][data-z="1"]')
    tr = await page.evaluate("document.querySelector('#board').style.transform")
    check(tr == 'scale(1.5)' and (await page.inner_text('.zv')) == '1,5×', f'Nút ＋ hai lần phải phóng 1,5× ({tr})')
    check(await page.evaluate("(() => { const w = document.querySelector('#bwrap'); return w.scrollWidth > w.clientWidth + 100; })()"), 'Lưới phóng to phải cuộn ngang được')
    await page.click('.cell[data-chi="2"]'); await page.wait_for_selector('#sheet.open')
    check('Cung Mệnh' in await page.inner_text('#sheet'), 'Khi phóng to, chạm ô vẫn mở đúng cung')
    await page.evaluate('window.tvBack()')
    await page.click('[data-act="zoom"][data-z="0"]')
    check(await page.evaluate("document.querySelector('#board').style.transform") == '', 'Nút ⟲ phải về cỡ gốc')
    await page.evaluate("document.querySelector('#board').scrollIntoView({block: 'center'})")
    bb = await page.locator('#board').bounding_box()
    cx, cy = bb['x'] + bb['width'] / 2, bb['y'] + min(bb['height'], 600) / 2
    cdp = await page.context.new_cdp_session(page)
    async def touch(t, pts):
        await cdp.send('Input.dispatchTouchEvent', {'type': t, 'touchPoints': [{'x': x, 'y': y, 'id': i} for i, (x, y) in enumerate(pts)]})
    await touch('touchStart', [(cx - 40, cy), (cx + 40, cy)])
    for k in range(1, 6):
        await touch('touchMove', [(cx - 40 - 8 * k, cy), (cx + 40 + 8 * k, cy)])
    await touch('touchEnd', [])
    zv = await page.inner_text('.zv')
    check(zv in ('2×', '1,99×', '2,01×'), f'Chụm hai ngón (khoảng cách 80 → 160 px) phải phóng ~2×, đang {zv}')
    await page.click('[data-act="zoom"][data-z="0"]')

    # —— Luận giải: tab Ngày ——
    await page.click('[data-act="open-reading"][data-s="day"]'); await page.wait_for_selector('.cal')
    check(await page.locator('.cal .cd:not(.blank)').count() == 30, 'Lịch tháng 11/2026 phải có 30 ngày')
    check(await page.locator('.cal .cd.blank').count() == 6, 'Ngày 1/11/2026 là Chủ Nhật → 6 ô trống đầu tuần')
    check(await page.locator('.cal .cd.sel[data-d="2026-11-20"]').count() == 1 and await page.locator('.cal .cd.today').count() == 1, 'Mặc định chọn hôm nay 20/11/2026')
    check('Ngày 20/11/2026' in await page.inner_text('.rsum') and 'Âm lịch 12/10' in await page.inner_text('.rsum'), 'Chi tiết ngày 20/11/2026 (âm 12/10)')
    check('Độ tin cậy thấp hơn tầng năm' in await page.inner_text('#cal'), 'Lịch ngày phải ghi rõ độ tin cậy thấp hơn tầng năm')
    check(await page.locator('#hrs .hr').count() == 12 and await page.locator('#hrs .hr.best').count() == 3, '12 giờ, đánh dấu 3 giờ thuận')
    day_txt = await page.inner_text('#app')
    check('Giờ thuận:' in day_txt and 'Chi ngày' in day_txt, 'Chi tiết ngày phải có giờ thuận và can chi ngày so với tuổi')
    cal_scores = await page.evaluate("Array.from(document.querySelectorAll('.cal .cd:not(.blank) i')).map(e => +e.textContent)")
    api_scores = [d['score'] for d in api('days', {'input': GOLDEN_A_INPUT, 'year': 2026, 'month': 11})]
    check(cal_scores == api_scores, 'Điểm trên lịch phải khớp engine')
    await shot(page, 'p5_day', full_page=True)
    await axe_check(page, 'tab Ngày')
    all_text += day_txt
    await page.click('.cal .cd[data-d="2026-11-03"]')
    check('Ngày 03/11/2026' in await page.inner_text('.rsum'), 'Chạm ngày 3 phải mở chi tiết ngày 3')
    await page.click('[data-act="rmon"][data-d="1"]')
    check('Tháng 12/2026' in await page.inner_text('.yv') and await page.locator('.cal .cd:not(.blank)').count() == 31, 'Sang tháng 12/2026 (31 ngày)')
    check('Ngày 03/12/2026' in await page.inner_text('.rsum'), 'Sang tháng giữ ngày đang chọn (3/12)')
    all_text += await page.inner_text('#app')
    for _ in range(13):
        await page.click('[data-act="rmon"][data-d="-1"]')
    check('Tháng 11/2025' in await page.inner_text('.yv'), 'Lùi 13 tháng phải về 11/2025')
    await page.click('[data-act="rscope"][data-s="year"]')
    check(await page.locator('.rs').count() == 7, 'Từ tab Ngày về tab Năm vẫn đủ 7 chủ đề')
    await page.click('[data-act="rscope"][data-s="day"]')
    check('Tháng 11/2025' in await page.inner_text('.yv'), 'Quay lại tab Ngày giữ tháng đang xem')
    await page.evaluate('window.tvBack()')

    # —— xuất ảnh PNG, mô hình PDF ——
    check(await page.locator('#export [data-act="export"]').count() == 4, 'Thẻ xuất phải có 4 nút (lưu/gửi ảnh, PDF)')
    async with page.expect_download() as dl:
        await page.click('#export [data-act="export"][data-k="png"][data-a="save"]')
    d = await dl.value
    png_path = f'{SHOTS}/export_{d.suggested_filename}'
    await d.save_as(png_path)
    check(d.suggested_filename == 'LaSo-Ca-A-2026.png', f'Tên tệp ảnh: {d.suggested_filename}')
    im = Image.open(png_path)
    check(im.size == (1256, 1504), f'Ảnh PNG lá số phải 1256×1504, đang {im.size}')
    compare_png(png_path, 'p5_export_png')
    m = await page.evaluate('window.tvExport.model()')
    kinds = [b['t'] for b in m['blocks']]
    check(kinds.count('h1') == 2 and kinds.count('brk') == 1 and kinds.count('h2') >= 20 and kinds.count('p') >= 40, f'Mô hình PDF: {dict((k, kinds.count(k)) for k in set(kinds))}')
    check(m['blocks'][1]['x'] == 'Luận giải trọn đời' and len(m['info']) == 11, 'PDF: luận trọn đời sau trang lá số, 11 dòng thông tin')
    scores = [b['x'] for b in m['blocks'] if b['t'] == 'score']
    check(len(scores) == 2 and all(x.count('Điểm') == 1 for x in scores) and all(x.count('Thế') <= 1 for x in scores), f'Dòng điểm PDF không lặp chữ: {scores}')
    check('Nam phái VN' in m['footer'] and 'di sản văn hóa' in m['footer2'], 'Chân trang PDF: trường phái + lời ngỏ')
    pdf_text = ' '.join(b['x'] for b in m['blocks'])
    check(not forbidden_in(pdf_text) and '{' not in pdf_text, f'PDF lọt từ cấm/ô trống: {forbidden_in(pdf_text)}')
    await page.evaluate('window.tvBack()')

    # —— so hai lá số ——
    check(await page.locator('[data-act="compare"]').count() == 1, 'Trang chủ phải có nút So hai lá số')
    await page.click('[data-act="compare"]')
    await pick(page, '[data-cmp="a"]', 'Ca A')
    await pick(page, '[data-cmp="b"]', 'Ca B')
    await page.click('[data-act="cmp-run"]')
    await page.wait_for_selector('#cmp-res')
    cr = await page.inner_text('#cmp-res')
    check('Ca A và Ca B · Vợ chồng' in cr and 'tương tác hai chiều' in cr, f'Kết quả so vợ chồng: {cr[:200]}')
    check(await page.locator('#cmp-res .rp .rp-why .w').count() >= 2, 'Mỗi đoạn so sánh phải có “Vì sao?”')
    await shot(page, 'p5_compare')
    await axe_check(page, 'so hai lá số')
    all_text += cr
    await page.click('[data-act="cmp-rel"][data-v="CHA_ME_CON"]')
    check(await page.locator('#cmp-res').count() == 0 and 'Cha hoặc mẹ' in await page.inner_text('#app'), 'Đổi quan hệ phải xóa kết quả cũ, đổi nhãn')
    await pick(page, '[data-cmp="b"]', 'Ca C')
    await page.click('[data-act="cmp-run"]'); await page.wait_for_selector('#cmp-res')
    check('Cha mẹ – con' in await page.inner_text('#cmp-res'), 'So cha mẹ – con')
    all_text += await page.inner_text('#cmp-res')
    await pick(page, '[data-cmp="b"]', 'Ca A')
    await page.click('[data-act="cmp-run"]')
    check('hai lá số khác nhau' in await page.inner_text('#cmp-error'), 'Chọn trùng một lá số phải báo lỗi')
    await page.evaluate('window.tvBack()')

    # —— không rõ giờ: hỏi–đáp sự kiện → xếp hạng 12 giờ ——
    found = []
    for y in range(2008, 2027):
        r = api('reading', {'input': GOLDEN_A_INPUT, 'viewYear': y, 'scope': 'year'})
        for sct in r['sections']:
            for p_ in sct['paragraphs']:
                if p_['kind'] == 'EVENT':
                    ev = next(i[6:] for i in p_['ids'] if i.startswith('EVENT_'))
                    lv = 3 if 'khả năng cao' in p_['text'] else 2 if 'có thể' in p_['text'] else 1
                    found.append((lv, y, ev))
    q_events = {k for k, _ in [('CUOI_HOI', 0), ('SINH_CON', 0), ('THANG_TIEN', 0), ('DOI_VIEC', 0), ('DUOC_TAI', 0), ('HAO_TAI', 0), ('DI_CHUYEN', 0)]}
    chosen = {}
    for lv, y, ev in sorted(found, key=lambda t: (-t[0], t[1])):
        if ev in q_events and ev not in chosen:
            chosen[ev] = y
    chosen = dict(list(chosen.items())[:3])
    check(len(chosen) >= 2, f'Ca A phải có ít nhất 2 sự kiện để thử xếp hạng giờ: {found[:6]}')
    await fill_form(page, parse_input(GOLDEN[0]['input']), 'A hỏi giờ', time_mode='unknown')
    await page.click('[data-act="submit"]')
    check(await page.locator('#hq .qa input').count() == 7, 'Hỏi–đáp không rõ giờ phải có 7 câu')
    first_ev = list(chosen)[0]
    await page.fill(f'[data-q="{first_ev}"]', str(chosen[first_ev]))
    await page.click('[data-act="rank-hours"]')
    check('ít nhất 2 câu' in await page.inner_text('#rank-error'), 'Một câu trả lời phải báo cần ít nhất 2')
    await page.fill('[data-q="SINH_CON"]' if first_ev != 'SINH_CON' else '[data-q="CUOI_HOI"]', '1980')
    await page.click('[data-act="rank-hours"]')
    check('năm phải từ 1990' in await page.inner_text('#rank-error'), 'Năm trước năm sinh phải báo lỗi')
    await page.fill('[data-q="SINH_CON"]' if first_ev != 'SINH_CON' else '[data-q="CUOI_HOI"]', '')
    for ev, y in chosen.items():
        await page.fill(f'[data-q="{ev}"]', str(y))
    await page.click('[data-act="rank-hours"]')
    await page.wait_for_selector('#rank')
    rows = await page.evaluate("Array.from(document.querySelectorAll('#rank .h.rk')).map(e => [e.dataset.canh, +e.querySelector('.pct').textContent])")
    check(len(rows) == 12, f'Xếp hạng phải đủ 12 giờ, đang {len(rows)}')
    scores = [r_[1] for r_ in rows]
    check(scores == sorted(scores, reverse=True), f'Xếp hạng phải giảm dần: {scores}')
    ti = next(r_[1] for r_ in rows if r_[0] == 'TI')
    check(ti == scores[0] and scores[-1] < ti, f'Giờ thật (Tỵ) phải đứng đầu với sự kiện lấy từ chính lá số: {rows} · sự kiện {chosen}')
    check('ước đoán' in await page.inner_text('#rank'), 'Xếp hạng giờ phải ghi “ước đoán”')
    await shot(page, 'p5_hours_rank', full_page=True)
    await axe_check(page, 'không rõ giờ')
    await page.click('#rank .h.rk >> nth=0')
    check(await page.locator('h1:has-text("Xác nhận lá số")').count() == 1, 'Chạm giờ trong bảng xếp hạng phải sang thẻ xác nhận')
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')

    # —— tùy chỉnh trường phái từng điểm ——
    await page.click('[data-act="settings"]')
    await page.click('[data-act="preset"][data-k="CUSTOM"]')
    check(await page.locator('#cfg select').count() == 12, 'Tùy chỉnh trường phái phải có 12 điểm')
    await page.select_option('[data-cfg="tuHoa"]', 'THIEN_LUONG')
    await shot(page, 'p5_settings_custom', full_page=True)
    await axe_check(page, 'cài đặt')
    await page.evaluate('window.tvBack()')
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    b = await page.evaluate(READ_BOARD)
    check('Tùy chỉnh' in b['center'], 'Ô trung tâm phải ghi “Tùy chỉnh”')
    check('KHOA' in b['cells'][str(CHI.index('Sửu'))]['hoa'] and 'KY' in b['cells'][str(CHI.index('Hợi'))]['hoa'], 'Tùy chỉnh Tứ Hóa kiểu Thiên Lương: năm Canh Đồng Khoa (Sửu), Âm Kỵ (Hợi)')
    await page.click('[data-act="open-reading"][data-s="year"]'); await page.wait_for_selector('.rsum')
    check(await page.locator('.rs').count() == 7, 'Luận năm với trường phái tùy chỉnh')
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')
    await page.click('[data-act="settings"]'); await page.click('[data-act="preset"][data-k="NAM_PHAI_VN"]')
    check(await page.locator('#cfg').count() == 0, 'Chọn lại Nam phái phải ẩn bảng tùy chỉnh')
    await page.click('[data-act="bench"]')
    await page.wait_for_selector('#bench dl.kv')
    bt = await page.inner_text('#bench')
    check('Lập lá số' in bt and 'Chấm điểm 30 ngày' in bt and 'chỉ tiêu 50 ms' in bt, f'Nút đo hiệu năng: {bt[:200]}')
    await page.evaluate('window.tvBack()')
    await shot(page, 'p5_home')
    await axe_check(page, 'trang chủ')
    await page.click('[data-act="new"]')
    await axe_check(page, 'form nhập')
    await page.evaluate('window.tvBack()')

    bad = forbidden_in(all_text)
    check(not bad and '{' not in all_text, f'Giai đoạn 5 lọt từ cấm hoặc ô trống: {bad}')

    # —— máy tính bảng: hai cột, bảng cung trượt bên phải ——
    tab = await browser.new_context(viewport={'width': 1280, 'height': 800}, device_scale_factor=1, locale='vi-VN')
    tp = await tab.new_page()
    await tp.goto(BASE); await dismiss_modal(tp)
    await tp.click('.profile:has-text("Ca A")'); await tp.wait_for_selector('#board')
    check(await tp.evaluate("getComputedStyle(document.querySelector('.chart-2p')).gridTemplateColumns.split(' ').length") == 2, 'Máy tính bảng: lá số hai cột')
    await shot(tp, 'p5_tablet_chart')
    await tp.click('.cell[data-chi="2"]'); await tp.wait_for_selector('#sheet.open')
    sw = await tp.evaluate("document.querySelector('#sheet').getBoundingClientRect().width")
    check(440 <= sw <= 470, f'Máy tính bảng: bảng cung là cột phải rộng ~460 px, đang {sw}')
    await shot(tp, 'p5_tablet_sheet')
    await tp.evaluate('window.tvBack()')
    await tp.click('[data-act="open-reading"][data-s="year"]'); await tp.wait_for_selector('.rsum')
    await axe_check(tp, 'máy tính bảng, luận năm')
    await tab.close()

async def qa_ask(page, q):
    n = await page.locator('.qa-turn').count()
    await page.fill('#qa-in', q)
    await page.press('#qa-in', 'Enter')
    await page.wait_for_function(f"document.querySelectorAll('.qa-turn').length > {n}")
    return page.locator('.qa-turn').nth(n)

async def phase6(page, browser):
    print('Giai đoạn 6')
    all_text = ''
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    card = page.locator('#qa-card')
    check(await card.count() == 1, 'Lá số phải có thẻ Hỏi đáp')
    chips = await page.locator('#qa-card [data-act="qa-ask"]').all_inner_texts()
    check(len(chips) == 3 and chips[0] == 'Năm 2026 công việc của tôi thế nào?', f'Thẻ Hỏi đáp: 3 câu gợi ý theo năm hiện tại: {chips}')
    # chạm câu gợi ý → sang tab Hỏi đáp và có ngay câu trả lời
    await page.click('#qa-card [data-act="qa-ask"] >> nth=0')
    await page.wait_for_selector('.qa-turn')
    check(await page.locator('.rtabs .chip.on').inner_text() == 'Hỏi đáp', 'Tab Hỏi đáp phải đang chọn')
    t0 = page.locator('.qa-turn').nth(0)
    und = await t0.locator('.qa-und').inner_text()
    check('Sự nghiệp · năm 2026' in und and 'Thế' in und, f'“App hiểu là” phải nêu chủ đề, năm, điểm, thế: {und}')
    check(await t0.locator('.rp').count() >= 2, 'Câu trả lời phải có các đoạn luận')
    heads = await t0.locator('.rp-h').all_inner_texts()
    check(all(any(k in h for h in heads) for k in ('Kết luận', 'Lá số gốc', 'Đại hạn', 'Lưu niên đại hạn', 'Tiểu hạn', 'Lưu Thái Tuế', 'Tổng hợp')),
          f'Câu trả lời năm phải luận đủ các tầng: {heads}')
    g = await t0.locator('.rp:has(.rp-h:has-text("Lá số gốc")) .rp-t').inner_text()
    check(g.startswith('Cung Quan Lộc an tại') and '/100' in g, f'Tầng gốc phải nêu cung Quan Lộc, sao tọa thủ và điểm: {g[:80]}')
    check(await t0.locator('.qa-adv').count() == 1, 'Câu trả lời luận năm phải có Nên/Không nên')
    check(await t0.locator('.qa-follow .chip').count() >= 2, 'Phải có câu hỏi tiếp')
    check('Trả lời ngay trên máy, lá số không rời điện thoại' in await page.inner_text('.qa-intro'), 'Nhãn “trả lời ngay trên máy”')
    # “Vì sao?”
    await t0.locator('.rp').nth(0).click()
    check(await t0.locator('.rp.open').count() == 1 and 'Nguồn' in await t0.locator('.rp.open .rp-why').inner_text(), '“Vì sao?” phải mở căn cứ và nguồn')
    all_text += await t0.locator('.qb.bot').inner_text()
    # câu không dấu
    t1 = await qa_ask(page, 'nam sau tai chinh ra sao')
    u1 = await t1.locator('.qa-und').inner_text()
    check('Tài chính · năm 2027' in u1, f'Câu không dấu phải hiểu đúng: {u1}')
    all_text += await t1.locator('.qb.bot').inner_text()
    # câu hỏi tiếp
    f = await t1.locator('.qa-follow .chip').nth(0).inner_text()
    n = await page.locator('.qa-turn').count()
    await t1.locator('.qa-follow .chip').nth(0).click()
    await page.wait_for_function(f"document.querySelectorAll('.qa-turn').length > {n}")
    check((await page.locator('.qa-turn').nth(n).locator('.qb.me').inner_text()).endswith(f), 'Chạm câu hỏi tiếp phải hỏi đúng câu đó')
    # khi nào thuận
    t2 = await qa_ask(page, 'Khi nào tôi lấy vợ?')
    u2 = await t2.locator('.qa-und').inner_text()
    tx2 = await t2.locator('.qb.bot').inner_text()
    check('Năm thuận cho' in u2 and 'tầng cùng chỉ' in tx2 and 'không phải lời khẳng định' in tx2 and 'Căn cứ:' in tx2, f'Hỏi “khi nào” phải dò năm theo đồng thanh tương ứng, có căn cứ: {u2}')
    all_text += tx2
    # đánh giá
    await t2.locator('[data-act="qa-rate"][data-v="1"]').click()
    check(await page.locator('.qa-turn').nth(3).locator('[data-act="qa-rate"][data-v="1"]').get_attribute('aria-pressed') == 'true', '👍 phải được ghi nhận')
    # khủng hoảng
    t3 = await qa_ask(page, 'Tôi muốn chết')
    tx3 = await t3.inner_text()
    check(await t3.locator('.qb.bot.crisis').count() == 1 and '115' in tx3 and '096 306 1414' in tx3 and '111' in tx3,
          'Câu khủng hoảng phải hiện lời hỗ trợ và số điện thoại')
    check(await t3.locator('.rp, .sc, [data-act="qa-rate"]').count() == 0, 'Câu khủng hoảng không đi qua phần luận, không chấm điểm, không đánh giá')
    await t3.scroll_into_view_if_needed()
    await shot(page, 'p6_qa_crisis')
    # từ chối, so hai lá số
    t4 = await qa_ask(page, 'Bao giờ tôi chết?')
    check('Ngoài phạm vi' in await t4.locator('.qa-und').inner_text() and await t4.locator('.sc').count() == 0, 'Câu hỏi sống chết phải bị từ chối, không chấm điểm')
    t5 = await qa_ask(page, 'Tôi và vợ có hợp tuổi không?')
    check(await t5.locator('[data-act="compare"]').count() == 1, 'Câu hợp tuổi phải có nút So hai lá số')
    t6 = await qa_ask(page, 'Sao Vũ Khúc nghĩa là gì?')
    check('Thẻ sao Vũ Khúc' in await t6.locator('.qa-und').inner_text() and 'Mệnh' in await t6.inner_text(), 'Hỏi nghĩa sao → thẻ sao trên lá số')
    for t in (t3, t4, t5, t6): all_text += await t.locator('.qb.bot').inner_text()
    await axe_check(page, 'hỏi đáp')
    await page.evaluate("window.scrollTo(0, 0)")
    await shot(page, 'p6_qa_top')
    # mở bản luận đầy đủ từ câu trả lời, quay lại vẫn còn hội thoại
    await page.locator('.qa-turn').nth(1).locator('[data-act="qa-link"]').click()
    await page.wait_for_selector('.rsum')
    check(await page.locator('.rtabs .chip.on').inner_text() == 'Năm' and 'Năm 2027' in await page.inner_text('.yv'), 'Nút “Xem luận năm 2027” phải mở luận năm 2027')
    await page.click('.rtabs [data-s="qa"]')
    check(await page.locator('.qa-turn').count() == 8, 'Quay lại tab Hỏi đáp phải giữ hội thoại')
    # “Hay hỏi”: câu hỏi hai lần
    await qa_ask(page, 'nam sau tai chinh ra sao')
    await page.evaluate('window.tvBack()')
    await page.click('[data-act="open-reading"][data-s="year"]'); await page.wait_for_selector('.rsum')
    await page.click('.rtabs [data-s="qa"]')
    hay = await page.locator('.qa-sugg:has-text("Hay hỏi") .chip').all_inner_texts()
    check('nam sau tai chinh ra sao' not in hay, 'Câu đã có trong hội thoại không lặp ở “Hay hỏi”')
    bad = forbidden_in(all_text)
    check(not bad and '{' not in all_text, f'Hỏi đáp lọt từ cấm hoặc ô trống: {bad}')
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')
    # Cài đặt: lịch sử hỏi đáp
    await page.click('[data-act="settings"]')
    st = await page.inner_text('#qa-set')
    check('(7 câu)' in st, f'Cài đặt phải đếm câu đã hỏi trên máy (không gồm câu khủng hoảng): {st}')
    await page.click('[data-act="qa-clear"]'); await page.click('#modal [data-act="modal-ok"]')
    check('(0 câu)' in await page.inner_text('#qa-set'), 'Xóa lịch sử hỏi đáp')
    check(api('storeGet', {'key': 'qaLog'}) == [], 'Lịch sử hỏi đáp trong bộ nhớ phải rỗng sau khi xóa')
    await page.evaluate('window.tvBack()')
    # tối
    dark = await browser.new_context(viewport={'width': 412, 'height': 915}, device_scale_factor=2, is_mobile=True, has_touch=True, color_scheme='dark', locale='vi-VN')
    dp = await dark.new_page()
    await dp.goto(BASE); await dismiss_modal(dp)
    await dp.click('.profile:has-text("Ca A")'); await dp.wait_for_selector('#board')
    await dp.click('#qa-card [data-act="open-reading"]')
    await qa_ask(dp, 'Tháng sau thế nào?')
    await dp.evaluate("window.scrollTo(0, 0)")
    await shot(dp, 'p6_qa_dark')
    await axe_check(dp, 'hỏi đáp, nền tối')
    await dark.close()

async def phase7(page):
    print('Giai đoạn 7 — Trợ lý AI')
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    await page.click('#qa-card [data-act="open-reading"]'); await page.wait_for_selector('#ai-card')
    card = await page.inner_text('#ai-card')
    check('chỉ qua Wi-Fi' in card and 'huggingface.co' in card and '2,6 GB' in card and 'không gửi đi' in card,
          f'Thẻ Trợ lý AI phải nói rõ dung lượng, nguồn tải, chỉ Wi-Fi, không gửi lá số: {card[:200]}')
    await page.evaluate("window.scrollTo(0, 0)")
    await shot(page, 'p7_ai_card')
    await page.click('[data-act="ai-download"]')
    check('huggingface.co' in await page.inner_text('#modal'), 'Bấm tải phải hỏi lại trước khi tải')
    await page.click('#modal [data-act="modal-ok"]')
    check(await page.locator('#ai-card .bar-p').count() == 1 and 'Đang tải' in await page.inner_text('#ai-card'), 'Đang tải phải hiện tiến độ')
    await page.wait_for_selector('#ai-card [data-act="ai-toggle"]', timeout=15000)
    check('đang bật' in await page.inner_text('#ai-card'), 'Tải xong thì Trợ lý AI tự bật')
    # câu đạt kiểm tra: hiện lời AI, bản luận của app thu gọn dưới nút
    t = await qa_ask(page, 'Năm sau sự nghiệp của tôi thế nào?')
    check(await t.locator('.qa-ai.wait').count() == 1, 'Đang chờ AI phải hiện dòng “đang viết”')
    await t.locator('.qa-ai-h').wait_for(timeout=15000)
    t = page.locator('.qa-turn').last
    ai = await t.locator('.qa-ai').inner_text()
    check('Trợ lý AI giải thích' in ai and '/100' in ai and 'Quan Lộc' in ai, f'Câu AI phải nêu điểm và cung của chủ đề: {ai[:160]}')
    check(not forbidden_in(ai), f'Câu AI lọt từ cấm: {forbidden_in(ai)}')
    check(await t.locator('.rp').count() == 0 and await t.locator('[data-act="qa-detail"]').count() == 1, 'Có câu AI thì bản luận chi tiết thu gọn')
    await page.evaluate("[...document.querySelectorAll('.qa-turn')].pop().scrollIntoView({block: 'start'})")
    await shot(page, 'p7_ai_answer')
    await t.locator('[data-act="qa-detail"]').click()
    t = page.locator('.qa-turn').last
    check(await t.locator('.rp').count() >= 5, 'Mở bản luận chi tiết phải thấy các tầng')
    await axe_check(page, 'hỏi đáp có Trợ lý AI')
    # câu AI trượt kiểm tra → hiện bản luận của app
    t = await qa_ask(page, 'Năm sau sự nghiệp của tôi thế nào? (thử lỗi AI)')
    await t.locator('.qa-ai.fail').wait_for(timeout=15000)
    t = page.locator('.qa-turn').last
    check(await t.locator('.rp').count() >= 5 and 'chưa đưa ra được câu trả lời đạt kiểm tra' in await t.inner_text(),
          'Câu AI sai dữ kiện không được hiện; app hiện bản luận gốc')
    check('2099' not in await t.inner_text(), 'Lời AI sai không được lọt ra màn hình')
    # khủng hoảng: không gọi AI
    t = await qa_ask(page, 'Tôi muốn chết')
    await page.wait_for_timeout(800)
    check(await page.locator('.qa-turn').last.locator('.qa-ai').count() == 0, 'Câu khủng hoảng không gọi Trợ lý AI')
    # Cài đặt: nhật ký câu trượt, xóa model
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')
    await page.click('[data-act="settings"]')
    check('1 câu AI viết chưa qua kiểm tra' in await page.inner_text('#ai-log'), 'Cài đặt phải đếm câu AI trượt kiểm tra')
    log = api('storeGet', {'key': 'aiLog'})
    check(isinstance(log, list) and len(log) == 1 and log[0]['issues'], f'Nhật ký AI trên máy phải ghi lý do trượt: {log}')
    await page.click('#ai-card [data-act="ai-delete"]'); await page.click('#modal [data-act="modal-ok"]')
    check(await page.locator('#ai-card [data-act="ai-download"]').count() == 1, 'Xóa model thì quay về nút tải')
    await page.click('[data-act="ai-log-clear"]')
    check(api('storeGet', {'key': 'aiLog'}) == [], 'Xóa nhật ký AI')
    await page.evaluate('window.tvBack()')

async def phase8(page):
    print('0.8.0 — Sách bàn, liên hệ, icon')
    home = await page.inner_text('#app')
    check('0378 373 373' in home and await page.locator('a[href="tel:0378373373"]').count() == 1, 'Trang chủ phải có số liên hệ bấm gọi được')
    check(await page.locator('.brand .logo svg.ic').count() == 1 and await page.locator('[data-act="new"] svg.ic').count() == 1, 'Logo bát quái và icon SVG trên nút')
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    await page.click('#qa-card [data-act="open-reading"]')
    t = await qa_ask(page, 'Năm sau sự nghiệp của tôi thế nào?')
    await page.wait_for_timeout(700)
    t = page.locator('.qa-turn').last
    if await t.locator('[data-act="qa-detail"]').count():
        await t.locator('[data-act="qa-detail"]').click(); t = page.locator('.qa-turn').last
    book = t.locator('.rp-book')
    check(await book.count() == 1 and 'Sách bàn về cung Quan Lộc' in await book.inner_text(), 'Câu trả lời năm phải có đoạn “Sách bàn về cung Quan Lộc”')
    await book.click()
    why = await t.locator('.rp-book .rp-why').inner_text()
    check('Tử Vi Đẩu Số Toàn Thư' in why and 'Căn cứ trên lá số' in why, f'“Vì sao?” của đoạn sách phải ghi sách và căn cứ: {why[:120]}')
    await book.scroll_into_view_if_needed()
    await shot(page, 'p8_sach_ban')
    alltext = await page.inner_text('#app')
    check('nháp' not in alltext.lower() and 'prompt' not in alltext.lower(), 'Không còn chữ “nháp”/“prompt” trên màn Hỏi đáp')
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')
    await page.click('[data-act="settings"]')
    st = await page.inner_text('#app')
    check('nháp' not in st.lower() and await page.locator('#contact a[href="tel:0378373373"]').count() == 1, 'Cài đặt: có thẻ Liên hệ, không còn chữ nháp')
    await page.evaluate('window.tvBack()')

async def phase81(page):
    print('0.8.1 — Đoán năm cưới')
    await page.click('.profile:has-text("Ca A")'); await page.wait_for_selector('#board')
    await page.click('#qa-card [data-act="open-reading"]')

    async def full(q):
        await qa_ask(page, q)
        await page.wait_for_timeout(500)
        t = page.locator('.qa-turn').last
        if await t.locator('[data-act="qa-detail"]').count():
            await t.locator('[data-act="qa-detail"]').click(); t = page.locator('.qa-turn').last
        return await t.inner_text()

    t = await full('Có thể xem từ lá số của tôi và đoán được năm cưới của tôi không?')
    check('Năm thuận cho chuyện tình cảm, cưới hỏi' in t and '2026 (Bính Ngọ' in t and 'Đối chiếu các năm đã qua' in t,
          f'Câu “đoán năm cưới” phải dò năm cưới và đối chiếu năm đã qua: {t[:200]}')
    await shot(page, 'p81_doan_nam_cuoi')
    t = await full('Tôi đã cưới năm 2015, lá số có khớp không?')
    check('Đối chiếu năm 2015 với lá số' in t and 'năm bạn nêu' in t and '2013 (Quý Tỵ' in t and '2017 (Đinh Dậu' in t,
          f'Câu “đã cưới năm 2015” phải đối chiếu năm đó và hai năm mỗi bên: {t[:200]}')
    await page.evaluate('window.tvBack()'); await page.evaluate('window.tvBack()')

asyncio.run(main())
