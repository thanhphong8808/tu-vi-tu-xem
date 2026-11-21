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
      const prof = Array.from(document.querySelectorAll('.profile .pmain')).find((p) => (p.textContent || '').indexOf('Ca A') >= 0);
      check(!!prof, 'Hồ sơ Ca A phải được lưu');
      if (prof) { prof.click(); await sleep(100); verify(GOLDEN[0], readBoard(), 'Mở lại Ca A'); click('[data-act="year"][data-d="1"]'); check((need('.yv').textContent || '').indexOf('2027') >= 0, 'Nút ▶ sang 2027'); }
      // Giai đoạn 4: màn Luận giải (bản luận ghép câu + chấm điểm chạy trong DEX).
      if (prof) {
        click('[data-act="year"][data-d="-1"]');
        const t1 = performance.now();
        click('[data-act="open-reading"][data-s="year"]');
        log(`TIME Luận năm 2026 ${Math.round(performance.now() - t1)} ms`);
        const rsum = $('.rsum');
        check(!!rsum && (rsum.textContent || '').indexOf('Năm 2026 (Bính Ngọ)') >= 0, 'Luận năm 2026: thiếu thẻ tóm tắt — ' + ((need('#app').textContent || '').slice(0, 200)));
        check(document.querySelectorAll('.rs').length === 7, `Luận năm: 7 chủ đề, đang có ${document.querySelectorAll('.rs').length}`);
        check(document.querySelectorAll('.heat .hm').length === 12, 'Luận năm: heat-map 12 tháng');
        const nen = document.querySelectorAll('.radv h3.adv-NEN + ul li').length, khong = document.querySelectorAll('.radv h3.adv-KHONG_NEN + ul li').length;
        check(nen >= 3 && nen <= 5 && khong >= 3 && khong <= 5, `Nên ${nen}, Không nên ${khong}`);
        const rps = Array.from(document.querySelectorAll('.rp'));
        check(rps.length > 10 && rps.every((e) => e.querySelector('.rp-why .w')), 'Mọi đoạn luận phải có “Vì sao?”');
        let txt = need('#app').textContent || '';
        check(txt.indexOf('khả năng cao') >= 0, 'Ca A 2026: hạn hỷ “khả năng cao”');
        const t2 = performance.now();
        click('[data-act="rscope"][data-s="life"]');
        log(`TIME Luận trọn đời ${Math.round(performance.now() - t2)} ms`);
        check(document.querySelectorAll('.rs-h[data-act="pal"]').length === 12 && document.querySelectorAll('.tl .dh').length === 12, 'Trọn đời: 12 cung và 12 đại vận');
        txt += need('#app').textContent || '';
        click('[data-act="rscope"][data-s="decade"]');
        check(((($('.rsum') || {}).textContent) || '').indexOf('Đại vận 35–44 tuổi') >= 0, 'Đại vận 35–44 tuổi');
        txt += need('#app').textContent || '';
        click('[data-act="rscope"][data-s="month"]'); click('[data-act="rmonth"][data-m="3"]');
        check(((($('.rsum') || {}).textContent) || '').indexOf('Tháng 3 âm lịch') >= 0, 'Luận tháng 3');
        txt += need('#app').textContent || '';
        check(forbiddenIn(txt).length === 0 && txt.indexOf('{') < 0, 'Bản luận lọt từ cấm hoặc ô chưa điền: ' + forbiddenIn(txt));
        log(`READING luận giải: ${rps.length} đoạn ở luận năm`);
        click('[data-act="rscope"][data-s="year"]');
        window.scrollTo(0, 0);
      }
      // ———————— Giai đoạn 5 ————————
      if (prof) await phase5();
      check(!document.querySelector('.error-box'), 'Không được có hộp lỗi trên màn hình: ' + ((document.querySelector('.error-box') || {}).textContent || ''));
      // Ảnh chụp cuối cùng: màn Luận giải năm (đang mở).
    } catch (e) {
      fail++;
      log('ERROR ' + e.message + ' ' + (e.stack || ''));
    }
    log(`RESULT pass=${pass} fail=${fail}`);
    await externalSteps();
  }

  const txtOf = (s) => (($(s) || {}).textContent) || '';
  const profileBtn = (name) => Array.from(document.querySelectorAll('.profile .pmain')).find((p) => (p.querySelector('.meta b') || {}).textContent === name);
  async function phase5() {
    let txt = '';
    // Tab Ngày: lịch tháng có điểm từng ngày, chi tiết ngày, 12 giờ.
    const t0 = performance.now();
    click('[data-act="rscope"][data-s="day"]');
    log(`TIME Tab Ngày (30 ngày + chi tiết ngày) ${Math.round(performance.now() - t0)} ms`);
    const nd = document.querySelectorAll('.cal .cd:not(.blank)').length;
    check(nd >= 28 && nd <= 31, `Lịch tháng phải có 28–31 ngày, đang ${nd}`);
    check(txtOf('.rsum').indexOf('Ngày ') >= 0 && txtOf('.rsum').indexOf('Âm lịch') >= 0, 'Chi tiết ngày: ' + txtOf('.rsum').slice(0, 120));
    check(document.querySelectorAll('#hrs .hr').length === 12 && document.querySelectorAll('#hrs .hr.best').length === 3, '12 giờ, 3 giờ thuận');
    check(txtOf('#cal').indexOf('Độ tin cậy thấp hơn tầng năm') >= 0, 'Lịch ngày phải ghi độ tin cậy thấp hơn tầng năm');
    txt += txtOf('#app');
    const t1 = performance.now();
    click('[data-act="rmon"][data-d="1"]');
    log(`TIME Sang tháng sau ${Math.round(performance.now() - t1)} ms`);
    check(document.querySelectorAll('.cal .cd:not(.blank)').length >= 28, 'Sang tháng sau');
    const someDay = document.querySelectorAll('.cal .cd:not(.blank)')[9];
    someDay.click();
    check(txtOf('.rsum').indexOf('Ngày 10/') >= 0, 'Chạm ngày 10: ' + txtOf('.rsum').slice(0, 60));
    txt += txtOf('#app');
    window.tvBack(); await sleep(200);
    // Thẻ sao: chạm sao trong bảng cung, chạm giữ sao trên lưới.
    click('.cell[data-chi="2"]'); await sleep(150);
    click('#sheet [data-act="star"][data-k="VU_KHUC"]'); await sleep(150);
    const sc = txtOf('#sheet');
    check(sc.indexOf('Bắc Đẩu') >= 0 && sc.indexOf('Âm Kim') >= 0 && sc.indexOf('Tài Tinh') >= 0 && sc.indexOf('Hóa Quyền (can năm Canh)') >= 0, 'Thẻ sao Vũ Khúc: ' + sc.slice(0, 160));
    txt += sc;
    window.tvBack(); await sleep(450);
    const ms = need('.cell[data-chi="2"] .ms[data-k="THIEN_TUONG"]');
    const r = ms.getBoundingClientRect();
    ms.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, clientX: r.x + 4, clientY: r.y + 4, pointerType: 'touch', isPrimary: true }));
    await sleep(800);
    ms.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, clientX: r.x + 4, clientY: r.y + 4, pointerType: 'touch', isPrimary: true }));
    check(!!$('#sheet.open .starcard[data-star="THIEN_TUONG"]'), 'Chạm giữ Thiên Tướng phải mở thẻ sao');
    window.tvBack(); await sleep(450);
    // Phóng to lưới.
    click('[data-act="zoom"][data-z="1"]');
    check(need('#board').style.transform === 'scale(1.25)', 'Nút ＋ phải phóng 1,25×');
    click('[data-act="zoom"][data-z="0"]');
    check(need('#board').style.transform === '', 'Nút ⟲ về cỡ gốc');
    // Xuất ảnh PNG, PDF A4 (PdfDocument + MediaStore trên máy thật).
    const exp = async (k) => {
      const t = performance.now();
      click(`#export [data-act="export"][data-k="${k}"][data-a="save"]`);
      let title = '';
      for (let i = 0; i < 150; i++) { title = txtOf('#modal.open h2'); if (title.indexOf('Đã lưu') === 0 || title.indexOf('Không') === 0) break; await sleep(100); }
      const m = txtOf('#modal.open').replace(/\s+/g, ' ');
      log(`EXPORT ${k} ${Math.round(performance.now() - t)} ms · ${m.slice(0, 200)}`);
      check(title.indexOf('Đã lưu') === 0, `Xuất ${k}: ${m.slice(0, 200)}`);
      if ($('#modal.open [data-act="modal-cancel"]')) click('#modal [data-act="modal-cancel"]');
      return m;
    };
    const mp = await exp('png');
    check(mp.indexOf('TuViTuXem/LaSo-Ca-A-2026') >= 0, 'Ảnh lưu vào Ảnh/TuViTuXem: ' + mp);
    const md = await exp('pdf');
    const pg = /\((\d+) trang A4\)/.exec(md);
    check(!!pg && +pg[1] >= 5, 'PDF phải có từ 5 trang A4: ' + md);
    check(md.indexOf('Tải xuống/TuViTuXem/LaSo-Ca-A-2026') >= 0, 'PDF lưu vào Tải xuống/TuViTuXem: ' + md);
    // Hiệu năng ngay trên máy (mục 3): lập lá số, luận năm, chấm 30 ngày.
    const bench = JSON.parse(window.TuViNative.call('bench', '{}')).data;
    log(`BENCH chart=${bench.chartMs.toFixed(2)} ms year=${bench.yearMs.toFixed(1)} ms days30=${bench.days30Ms.toFixed(1)} ms lifetime=${bench.lifetimeMs.toFixed(1)} ms`);
    check(bench.chartMs < 25 && bench.yearMs < 250 && bench.days30Ms < 1500, `Hiệu năng vượt ngưỡng máy ảo: ${JSON.stringify(bench)}`);
    // So hai lá số.
    window.tvBack(); await sleep(150);
    const idOf = (name) => { const pb = profileBtn(name); return pb ? pb.dataset.id : ''; };
    const ida = idOf('Ca A'), idb = idOf('Ca B');
    click('[data-act="compare"]');
    setSel('[data-cmp="a"]', ida); setSel('[data-cmp="b"]', idb);
    click('[data-act="cmp-run"]');
    check(txtOf('#cmp-res').indexOf('Ca A và Ca B · Vợ chồng') >= 0 && document.querySelectorAll('#cmp-res .rp').length >= 2, 'So hai lá số: ' + txtOf('#app').slice(0, 160));
    txt += txtOf('#cmp-res');
    window.tvBack(); await sleep(100);
    // Không rõ giờ: hỏi–đáp sự kiện → xếp hạng 12 giờ.
    fillForm(parseInput(GOLDEN[0].input), 'A hỏi giờ', { unknown: true });
    click('[data-act="submit"]');
    check(document.querySelectorAll('#hq .qa input').length === 7, 'Hỏi–đáp 7 câu');
    setText('[data-q="CUOI_HOI"]', '2026'); setText('[data-q="SINH_CON"]', '2012'); setText('[data-q="DI_CHUYEN"]', '2018');
    const t2 = performance.now();
    click('[data-act="rank-hours"]');
    log(`TIME Xếp hạng 12 giờ × 3 sự kiện ${Math.round(performance.now() - t2)} ms`);
    const rows = Array.from(document.querySelectorAll('#rank .h.rk'));
    check(rows.length === 12 && rows[0].dataset.canh === 'TI', `Xếp hạng giờ: giờ thật Tỵ phải đứng đầu (${rows.slice(0, 3).map((e) => e.dataset.canh + ':' + txtOf('#rank .h.rk[data-canh="' + e.dataset.canh + '"] .pct'))})`);
    check(txtOf('#rank').indexOf('ước đoán') >= 0, 'Xếp hạng giờ phải ghi ước đoán');
    window.tvBack(); window.tvBack(); await sleep(100);
    // Tùy chỉnh trường phái từng điểm.
    click('[data-act="settings"]');
    click('[data-act="preset"][data-k="CUSTOM"]');
    check(document.querySelectorAll('#cfg select').length === 12, 'Tùy chỉnh 12 điểm');
    setSel('[data-cfg="tuHoa"]', 'THIEN_LUONG');
    window.tvBack(); await sleep(100);
    profileBtn('Ca A').click(); await sleep(150);
    const b = readBoard();
    check(b.center.indexOf('Tùy chỉnh') >= 0 && b.cells[CHI.indexOf('Sửu')].hoa.indexOf('KHOA') >= 0 && b.cells[CHI.indexOf('Hợi')].hoa.indexOf('KY') >= 0, 'Tùy chỉnh Tứ Hóa Thiên Lương');
    window.tvBack(); await sleep(100);
    click('[data-act="settings"]'); click('[data-act="preset"][data-k="NAM_PHAI_VN"]'); window.tvBack(); await sleep(100);
    check(forbiddenIn(txt).length === 0 && txt.indexOf('{') < 0, 'Giai đoạn 5 lọt từ cấm/ô trống: ' + forbiddenIn(txt));
    // Kết thúc ở lá số Ca A (CI đọc cây trợ năng, chạm giữ thật, mở bảng chia sẻ).
    profileBtn('Ca A').click(); await sleep(300);
    log('PHASE5 done');
  }

  // Sau RESULT: các bước CI điều khiển bằng adb trên màn hình thật — chạm giữ một sao, bảng chia sẻ PDF.
  async function externalSteps() {
    try {
      const ms = $('.cell[data-chi="2"] .ms[data-k="THIEN_TUONG"]');
      if (!ms) { log('LONGPRESS_NO_TARGET'); return; }
      ms.scrollIntoView({ block: 'center' }); await sleep(400);
      const r = ms.getBoundingClientRect();
      log(`LONGPRESS_TARGET x=${(r.x + r.width / 2).toFixed(1)} y=${(r.y + r.height / 2).toFixed(1)} dpr=${window.devicePixelRatio} vw=${window.innerWidth} vh=${window.innerHeight}`);
      for (let i = 0; i < 180 && !$('#sheet.open .starcard'); i++) await sleep(500);
      log($('#sheet.open .starcard') ? 'LONGPRESS_SEEN ' + $('#sheet .starcard').dataset.star : 'LONGPRESS_TIMEOUT');
      await sleep(4000); // CI chụp ảnh thẻ sao
      window.tvBack(); await sleep(600);
      click('#export [data-act="export"][data-k="pdf"][data-a="share"]');
      await sleep(2500);
      log('SHARE_STARTED');
    } catch (e) { log('POST_ERROR ' + e.message); }
  }
  run();
})();
