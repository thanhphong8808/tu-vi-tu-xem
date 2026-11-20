"""Kiểm thử giao diện đầu-cuối (Playwright + máy chủ thử nghiệm chạy đúng mã Kotlin của app).

Cổng giai đoạn 2B: 4 lá số golden hiển thị đúng trên lưới 12 cung (mọi chính tinh, phụ tinh trong golden,
Tuần, Triệt, Mệnh, Thân, Cục, tuổi đại hạn, đại hạn/tiểu hạn/lưu niên đại hạn/nguyệt hạn tháng Giêng năm 2026),
cộng các luồng: nhập âm lịch, canh giờ, không rõ giờ, lá số thay thế, lưu/mở/xóa hồ sơ, đổi trường phái, đổi năm.
Giai đoạn 3: thẻ “Cách cục & dấu hiệu”, dấu hiệu năm, luận giải từng cung, “Vì sao?”, chế độ chuyên sâu, không lọt từ cấm.
Chạy: tools/apk/devserver.sh 8765 2026-11-20 &  rồi  python3 tools/apk/uitest/test_ui.py [thư mục ảnh]
"""
import asyncio, json, os, re, sys
from playwright.async_api import async_playwright

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

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
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
        check('Bản nháp' in rd and 'chưa qua thẩm định' in rd, 'Thẻ luận giải phải ghi rõ là bản nháp chưa thẩm định')
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

        check(not errors, f'Lỗi JavaScript: {errors}')

        # —— ảnh chụp: tối, ngang ——
        dark = await browser.new_context(viewport={'width': 412, 'height': 915}, device_scale_factor=2, is_mobile=True, has_touch=True, color_scheme='dark')
        dp = await dark.new_page()
        await dp.goto(BASE); await dismiss_modal(dp)
        await dp.click('.profile:has-text("Ca A")'); await dp.wait_for_selector('#board')
        await dp.screenshot(path=f'{SHOTS}/dark_chart.png')
        await dp.set_viewport_size({'width': 915, 'height': 412})
        await dp.wait_for_timeout(300)
        await dp.evaluate("document.querySelector('#board').scrollIntoView()")
        await dp.screenshot(path=f'{SHOTS}/dark_landscape.png')
        await browser.close()
    print(f'\n{checks - len(failures)}/{checks} kiểm tra đạt')
    if failures:
        print('THẤT BẠI:'); [print(' -', f) for f in failures]
        sys.exit(1)

asyncio.run(main())
