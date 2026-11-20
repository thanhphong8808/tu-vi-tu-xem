/* Tự kiểm tra chạy BÊN TRONG app Android (chỉ có trong APK kiểm thử, mở bằng intent extra "selftest").
 * Nhập 4 ca golden qua giao diện thật, gọi engine thật (DEX) qua cầu nối, đối chiếu lưới 12 cung.
 * Kết quả in ra console → logcat thẻ TuViWeb, dòng bắt đầu bằng "TUVI_SELFTEST". */
'use strict';
(function () {
  const GOLDEN = /*__GOLDEN__*/[];
  const CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi'];
  const HOA = { 'Hóa Lộc': 'LOC', 'Hóa Quyền': 'QUYEN', 'Hóa Khoa': 'KHOA', 'Hóa Kỵ': 'KY' };
  const CUC = { 'Thủy 2': 'Thủy nhị cục', 'Mộc 3': 'Mộc tam cục', 'Kim 4': 'Kim tứ cục', 'Thổ 5': 'Thổ ngũ cục', 'Hỏa 6': 'Hỏa lục cục' };
  let pass = 0, fail = 0;
  const log = (s) => console.log('TUVI_SELFTEST ' + s);
  const check = (c, m) => { if (c) pass++; else { fail++; log('FAIL ' + m); } };
  const $ = (s) => document.querySelector(s);
  const need = (s) => { const e = $(s); if (!e) throw new Error('Không thấy phần tử ' + s); return e; };
  const click = (s) => need(s).click();
  const setSel = (s, v) => { const e = need(s); e.value = v; e.dispatchEvent(new Event('change', { bubbles: true })); };
  const setText = (s, v) => { const e = need(s); e.value = v; e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true })); };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  // Mục 14.3: từ cấm trong câu luận (đồng bộ KbLint.FORBIDDEN).
  const FORBIDDEN = ['chắc chắn', '100%', 'không thể tránh', 'định mệnh', 'tử vong', 'chết', 'đoản thọ', 'tuổi thọ', 'sảy thai', 'ly hôn',
    'tù tội', 'lao lý', 'tai nạn chết người', 'hạn chí đầu hà', 'dị bào', 'giới tính thai', 'con trai', 'con gái', 'giải hạn', 'cúng sao', 'dâng sao'];
  const forbiddenIn = (t) => { const low = t.toLowerCase(); return FORBIDDEN.filter((w) => new RegExp('(^|[^0-9A-Za-zÀ-ỹ])' + w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '($|[^0-9A-Za-zÀ-ỹ])').test(low)); };
  // Giai đoạn 3: thẻ luận giải sơ bộ (knowledge base nạp từ APK, RuleEngine chạy trong DEX).
  function verifyReading(tag, expectNatal, expectYear) {
    const card = $('#reading');
    check(!!card, `${tag}: thiếu thẻ luận giải`);
    if (!card) return;
    const txt = card.textContent || '';
    check(txt.indexOf('Chưa tải được') < 0, `${tag}: không tải được luận giải: ${txt.slice(0, 160)}`);
    const items = card.querySelectorAll('.rd').length;
    check(items >= 1, `${tag}: phải có ít nhất 1 cách cục/dấu hiệu`);
    const texts = Array.from(document.querySelectorAll('.rd-t, .pl div')).map((e) => e.textContent || '');
    check(texts.every((t) => t.indexOf('{') < 0 && t.trim()), `${tag}: câu luận còn ô trống/chưa điền`);
    const bad = forbiddenIn(need('#app').textContent || '');
    check(bad.length === 0, `${tag}: lọt từ cấm ${bad}`);
    (expectNatal || []).forEach((n) => check(txt.indexOf(n) >= 0, `${tag}: thiếu “${n}”`));
    const yc = Array.from(document.querySelectorAll('.card')).find((c) => (c.textContent || '').indexOf('Dấu hiệu năm') >= 0);
    check(!!yc, `${tag}: thiếu mục dấu hiệu năm`);
    (expectYear || []).forEach((n) => check(!!yc && (yc.textContent || '').indexOf(n) >= 0, `${tag}: năm thiếu “${n}”`));
    log(`READING ${tag} natal=${items} year=${yc ? yc.querySelectorAll('.rd').length : 0}`);
  }

  function readBoard() {
    const out = {};
    const name = (el) => Array.from(el.childNodes).filter((n) => n.nodeType === 3).map((n) => n.textContent).join('').trim();
    document.querySelectorAll('#board .cell').forEach((c) => {
      const ring = ((c.querySelector('.c-foot span') || {}).textContent || '').trim();
      out[c.dataset.chi] = {
        stars: Array.from(c.querySelectorAll('.ms, .c-minor .st')).map(name).concat(ring ? [ring] : []),
        hoa: Array.from(c.querySelectorAll('.c-hoa .hb:not(.l)')).map((h) => h.className.split(' ')[1]),
        marks: Array.from(c.querySelectorAll('.mk')).map((m) => m.textContent),
        dh: (c.querySelector('.c-dh') || {}).textContent,
        role: (c.querySelector('.c-role') || {}).textContent || '',
      };
    });
    return { cells: out, tt: Array.from(document.querySelectorAll('#board .tt')).map((t) => t.textContent), center: (need('#board .center').textContent || '') };
  }

  function parseInput(s) {
    const m = /(Nam|Nữ) (\d+)\/(\d+)\/(\d+) (\d+):(\d+)/.exec(s);
    return { gender: m[1] === 'Nam' ? 'NAM' : 'NU', day: String(+m[2]), month: String(+m[3]), year: m[4], hour: String(+m[5]), minute: String(+m[6]) };
  }

  function fillForm(inp, name, opt) {
    opt = opt || {};
    click('[data-act="new"]');
    setText('[data-f="name"]', name);
    click(`[data-act="seg"][data-f="gender"][data-v="${inp.gender}"]`);
    if (opt.lunar) click('[data-act="seg"][data-f="calendar"][data-v="lunar"]');
    setSel('[data-f="day"]', inp.day);
    setSel('[data-f="month"]', inp.month);
    setText('[data-f="year"]', inp.year);
    if (opt.leap) { const cb = need('[data-f="leap"]'); cb.checked = true; cb.dispatchEvent(new Event('change', { bubbles: true })); }
    if (opt.canh) { click('[data-act="seg"][data-f="timeMode"][data-v="canh"]'); setSel('[data-f="canh"]', opt.canh); }
    else if (opt.unknown) click('[data-act="seg"][data-f="timeMode"][data-v="unknown"]');
    else { setSel('[data-f="hour"]', inp.hour); setSel('[data-f="minute"]', inp.minute); }
    setText('[data-f="viewYear"]', '2026');
  }

  function verify(g, b, tag) {
    const entries = Object.entries(g.chinhTinh).concat(Object.entries(g.phuTinh));
    entries.forEach(([star, chi]) => {
      const where = Object.keys(b.cells).filter((k) => (HOA[star] ? b.cells[k].hoa.indexOf(HOA[star]) >= 0 : b.cells[k].stars.indexOf(star) >= 0)).map(Number);
      check(where.length === 1 && where[0] === CHI.indexOf(chi), `${tag}: ${star} phải ở ${chi}, đang ở [${where.map((i) => CHI[i])}]`);
    });
    const menh = Object.keys(b.cells).filter((k) => b.cells[k].role.indexOf('Mệnh') === 0).map(Number);
    const than = Object.keys(b.cells).filter((k) => b.cells[k].role.indexOf('THÂN') >= 0).map(Number);
    check(menh.length === 1 && menh[0] === CHI.indexOf(g.menh), `${tag}: Mệnh phải ở ${g.menh}`);
    check(than.length === 1 && than[0] === CHI.indexOf(g.than), `${tag}: Thân phải ở ${g.than}`);
    check(b.center.indexOf(CUC[g.cuc]) >= 0, `${tag}: thiếu ${CUC[g.cuc]}`);
    g.daiHan3.forEach((it) => { const [chi, ages] = it.split(' '); check(b.cells[CHI.indexOf(chi)].dh === ages.split('-')[0], `${tag}: tuổi đại hạn ${chi}`); });
    const n = g.nam2026;
    [['ĐH', 'daiHan'], ['TH', 'tieuHan'], ['LN', 'luuNienDaiHan']].forEach(([mk, key]) => {
      const where = Object.keys(b.cells).filter((k) => b.cells[k].marks.indexOf(mk) >= 0).map(Number);
      check(where.length === 1 && where[0] === CHI.indexOf(n[key]), `${tag}: dấu ${mk} 2026 phải ở ${n[key]}`);
    });
    check(b.center.indexOf(n.tuoiAm + ' tuổi') >= 0, `${tag}: tuổi âm 2026 = ${n.tuoiAm}`);
    check(b.tt.length >= 1, `${tag}: thiếu nhãn Tuần/Triệt`);
  }

  async function run() {
    log('START ' + navigator.userAgent);
    try {
      await sleep(300);
      if ($('#modal.open')) click('#modal [data-act="modal-ok"]');
      for (const g of GOLDEN) {
        const tag = 'Ca ' + g.id;
        fillForm(parseInput(g.input), tag);
        click('[data-act="submit"]');
        check(!!$('[data-act="confirm"]'), `${tag}: phải sang thẻ xác nhận (lỗi form: ${($('#form-error') || {}).textContent || ''})`);
        check((need('#app').textContent || '').indexOf(g.lunar.replace('N', ' nhuận')) >= 0, `${tag}: thẻ xác nhận phải có âm lịch ${g.lunar}`);
        const t0 = performance.now();
        click('[data-act="confirm"]');
        log(`TIME ${tag} lập lá số + luận giải ${Math.round(performance.now() - t0)} ms`);
        await sleep(150);
        verify(g, readBoard(), tag);
        verifyReading(tag, g.id === 'A' ? ['Âm Dương thuận lý', 'Thiên Tướng thủ Mệnh'] : [], g.id === 'A' ? ['Lưu Thái Tuế vào cung Quan Lộc'] : []);
        click('[data-act="layer"][data-k="month"]');
        click('[data-act="month"][data-m="1"]');
        const b = readBoard();
        const nh = Object.keys(b.cells).filter((k) => b.cells[k].marks.indexOf('NH1') >= 0).map(Number);
        check(nh.length === 1 && nh[0] === CHI.indexOf(g.nam2026.nguyetHanThang1), `${tag}: nguyệt hạn tháng 1`);
        click('[data-act="layer"][data-k="month"]');
        click('.cell[data-chi="' + CHI.indexOf(g.menh) + '"]');
        await sleep(100);
        check((need('#sheet').textContent || '').indexOf('Cung Mệnh') >= 0, `${tag}: bảng chi tiết cung Mệnh`);
        check((need('#sheet').textContent || '').indexOf('Luận giải sơ bộ') >= 0 && document.querySelectorAll('#sheet .pl').length >= 1, `${tag}: bảng cung Mệnh phải có luận giải sơ bộ`);
        check(forbiddenIn(need('#sheet').textContent || '').length === 0, `${tag}: bảng cung Mệnh lọt từ cấm`);
        window.tvBack();
        log('OK ' + tag);
        click('[data-act="back"]');
      }
      // Âm lịch tháng nhuận (ca C)
      fillForm({ gender: 'NAM', day: '1', month: '2', year: '2023', hour: '6', minute: '0' }, 'C âm lịch', { lunar: true, leap: true });
      click('[data-act="submit"]'); click('[data-act="confirm"]'); await sleep(100);
      verify(GOLDEN[2], readBoard(), 'Ca C âm lịch'); click('[data-act="back"]');
      // Canh giờ (ca D)
      fillForm(parseInput(GOLDEN[3].input), 'D canh', { canh: 'MUI' });
      click('[data-act="submit"]'); click('[data-act="confirm"]'); await sleep(100);
      verify(GOLDEN[3], readBoard(), 'Ca D canh Mùi'); click('[data-act="back"]');
      // Không rõ giờ (ca A)
      fillForm(parseInput(GOLDEN[0].input), 'A không rõ giờ', { unknown: true });
      click('[data-act="submit"]');
      check(document.querySelectorAll('.hours .h').length === 12, 'Không rõ giờ phải ra 12 lá số');
      click('.hours .h[data-canh="TI"]'); click('[data-act="confirm"]'); await sleep(100);
      verify(GOLDEN[0], readBoard(), 'Ca A chọn giờ'); click('[data-act="back"]');
      // Lá số thay thế (ca B)
      fillForm(parseInput(GOLDEN[1].input), 'B thay thế');
      click('[data-act="submit"]');
      const altBtn = Array.from(document.querySelectorAll('.warn')).find((w) => (w.textContent || '').indexOf('Tý muộn') >= 0);
      check(!!altBtn, 'Ca B phải có cảnh báo Tý muộn');
      if (altBtn) { altBtn.querySelector('[data-act="alt"]').click(); await sleep(100); check(readBoard().center.indexOf('12/2/1985') >= 0, 'Lá số thay thế ca B = 12/2/1985'); }
      window.tvBack(); window.tvBack(); window.tvBack();
      // Mở lại hồ sơ đã lưu + đổi năm
      const prof = Array.from(document.querySelectorAll('.profile')).find((p) => (p.textContent || '').indexOf('Ca A') >= 0);
      check(!!prof, 'Hồ sơ Ca A phải được lưu');
      if (prof) { prof.click(); await sleep(100); verify(GOLDEN[0], readBoard(), 'Mở lại Ca A'); click('[data-act="year"][data-d="1"]'); check((need('.yv').textContent || '').indexOf('2027') >= 0, 'Nút ▶ sang 2027'); }
      check(!document.querySelector('.error-box'), 'Không được có hộp lỗi trên màn hình: ' + ((document.querySelector('.error-box') || {}).textContent || ''));
      // Để ảnh chụp màn hình cuối cùng cho thấy thẻ luận giải.
      const rc = $('#reading'); if (rc) rc.scrollIntoView({ block: 'start' });
    } catch (e) {
      fail++;
      log('ERROR ' + e.message + ' ' + (e.stack || ''));
    }
    log(`RESULT pass=${pass} fail=${fail}`);
  }
  run();
})();
