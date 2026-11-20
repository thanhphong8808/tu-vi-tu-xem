/* Tử Vi Tự Xem — giao diện. Gọi engine Kotlin qua window.TuViNative (Android) hoặc /api (máy chủ thử nghiệm). */
'use strict';
(function () {
  // ———————————————————————————— tiện ích ————————————————————————————
  const $ = (s, r) => (r || document).querySelector(s);
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi'];
  // Vị trí ô trên lưới 4×4 (hàng, cột) — bố cục truyền thống, Tý ở đáy giữa phải, đi thuận chiều kim đồng hồ.
  const POS = [[3, 2], [3, 1], [3, 0], [2, 0], [1, 0], [0, 0], [0, 1], [0, 2], [0, 3], [1, 3], [2, 3], [3, 3]];
  const BRI = { M: 'Miếu', V: 'Vượng', 'Đ': 'Đắc', B: 'Bình hòa', H: 'Hãm' };
  const HANH = { kim: 'Kim', moc: 'Mộc', thuy: 'Thủy', hoa: 'Hỏa', tho: 'Thổ' };
  const HOA_SHORT = { HOA_LOC: 'Lộc', HOA_QUYEN: 'Quyền', HOA_KHOA: 'Khoa', HOA_KY: 'Kỵ' };
  const HOA_KEY = { HOA_LOC: 'LOC', HOA_QUYEN: 'QUYEN', HOA_KHOA: 'KHOA', HOA_KY: 'KY' };
  const CANH = [
    ['TY_SOM', 'Tý sớm (00:00–00:59)'], ['SUU', 'Sửu (01:00–02:59)'], ['DAN', 'Dần (03:00–04:59)'], ['MAO', 'Mão (05:00–06:59)'],
    ['THIN', 'Thìn (07:00–08:59)'], ['TI', 'Tỵ (09:00–10:59)'], ['NGO', 'Ngọ (11:00–12:59)'], ['MUI', 'Mùi (13:00–14:59)'],
    ['THAN', 'Thân (15:00–16:59)'], ['DAU', 'Dậu (17:00–18:59)'], ['TUAT', 'Tuất (19:00–20:59)'], ['HOI', 'Hợi (21:00–22:59)'],
    ['TY_MUON', 'Tý muộn (23:00–23:59)'],
  ];
  const PRESETS = [
    ['NAM_PHAI_VN', 'Nam phái VN', 'Mặc định. Thái Thứ Lang – Tân Biên, khớp cẩm nang V2.'],
    ['THIEN_LUONG', 'Theo cụ Thiên Lương', 'Năm Canh: Dương Lộc, Vũ Quyền, Đồng Khoa, Âm Kỵ. Năm Nhâm hóa Khoa ở Thiên Phủ.'],
    ['TOAN_THU_TRUNG_CHAU', 'Toàn Thư / Trung Châu', 'Khôi Việt theo Toàn Thư, Hỏa Linh kiểu Trung Châu, nguyệt hạn theo Đẩu Quân, Thân chủ tuổi Tý là Hỏa Tinh.'],
  ];
  const COMMON_ZONES = ['America/Los_Angeles', 'America/New_York', 'America/Chicago', 'America/Toronto', 'Europe/Paris', 'Europe/Berlin',
    'Europe/London', 'Europe/Prague', 'Europe/Moscow', 'Australia/Sydney', 'Australia/Melbourne', 'Asia/Tokyo', 'Asia/Seoul',
    'Asia/Taipei', 'Asia/Shanghai', 'Asia/Bangkok', 'Asia/Singapore', 'Asia/Kuala_Lumpur'];
  const ONBOARD = 'Tử Vi Đẩu Số là di sản văn hóa – triết học phương Đông. Nội dung trong ứng dụng mang tính tham khảo và chiêm nghiệm, không thay thế tư vấn y tế, pháp lý hay tài chính. Bạn luôn là người quyết định con đường của mình.';

  // ———————————————————————————— cầu nối engine ————————————————————————————
  function api(method, arg) {
    const body = JSON.stringify(arg == null ? {} : arg);
    let raw;
    if (window.TuViNative && window.TuViNative.call) {
      raw = window.TuViNative.call(method, body);
    } else {
      const x = new XMLHttpRequest();
      x.open('POST', 'api/' + method, false);
      x.setRequestHeader('Content-Type', 'application/json; charset=utf-8');
      x.send(body);
      raw = x.responseText;
    }
    const r = JSON.parse(raw);
    if (!r.ok) throw new Error(r.error || 'Lỗi không rõ');
    return r.data;
  }
  function safe(fn, fallback) { try { return fn(); } catch (e) { return fallback; } }

  // ———————————————————————————— trạng thái ————————————————————————————
  const S = {
    stack: ['home'],
    today: null,
    settings: { preset: 'NAM_PHAI_VN', onboarded: false, expert: false },
    profiles: [],
    form: null,
    formError: '',
    editingId: null,
    pending: null, // {input, data} chờ xác nhận
    saveOnConfirm: true,
    chart: null,
    req: null, // {input, viewYear, alt}
    profileId: null,
    sel: null,
    layers: { han: true, luu: true, hoa: true, month: false },
    month: 1,
    hours: null,
    hoursInput: null,
    lunarCache: {},
    rdOpen: {}, // mục luận giải đang mở “Vì sao?”
  };

  function defaultForm() {
    return { name: '', gender: '', calendar: 'solar', day: '', month: '', year: '', leap: false, timeMode: 'clock', hour: '', minute: '',
      canh: '', region: 'VN_BAC', zone: '', useSolar: false, longitude: '', viewYear: S.today ? String(S.today.lunarYear) : '' };
  }

  function loadStore() {
    try {
      S.today = api('today');
    } catch (e) {
      S.today = { solar: '', lunar: '', lunarYear: new Date().getFullYear(), lunarYearCanChi: '' };
      setTimeout(() => showFatal('Không gọi được engine — ' + e.message), 0);
    }
    const st = safe(() => api('storeGet', { key: 'settings' }), null);
    if (st && typeof st === 'object') Object.assign(S.settings, st);
    const pr = safe(() => api('storeGet', { key: 'profiles' }), null);
    S.profiles = Array.isArray(pr) ? pr : [];
  }
  function saveSettings() { safe(() => api('storeSet', { key: 'settings', value: S.settings })); }
  function saveProfiles() { safe(() => api('storeSet', { key: 'profiles', value: S.profiles })); }

  // ———————————————————————————— điều hướng ————————————————————————————
  const top = () => S.stack[S.stack.length - 1];
  function go(view, replace) {
    if (replace) S.stack[S.stack.length - 1] = view; else S.stack.push(view);
    render(true);
  }
  function reset(views) { S.stack = views.slice(); render(true); }
  window.tvBack = function () {
    if ($('#modal').classList.contains('open')) { closeModal(); return true; }
    if ($('#sheet').classList.contains('open')) { closeSheet(); return true; }
    if (S.stack.length > 1) { S.stack.pop(); render(true); return true; }
    return false;
  };
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') window.tvBack(); });

  // ———————————————————————————— dựng giao diện ————————————————————————————
  function render(scrollTop) {
    const v = top();
    const app = $('#app');
    let html = '';
    if (v === 'home') html = viewHome();
    else if (v === 'form') html = viewForm();
    else if (v === 'confirm') html = viewConfirm();
    else if (v === 'chart') html = viewChart();
    else if (v === 'hours') html = viewHours();
    else if (v === 'settings') html = viewSettings();
    app.innerHTML = html;
    if (scrollTop) window.scrollTo(0, 0);
    if (v === 'chart') afterChart();
    if (v === 'form') afterForm();
  }

  const bar = (title, sub, right) =>
    `<div class="bar"><button class="icon-btn" data-act="back" aria-label="Quay lại">‹</button>` +
    `<h1>${esc(title)}${sub ? `<span class="sub">${esc(sub)}</span>` : ''}</h1>${right || ''}</div>`;

  // ——— Trang chủ ———
  function viewHome() {
    const t = S.today || {};
    let list = '';
    if (!S.profiles.length) {
      list = `<div class="empty">Chưa có lá số nào.<br>Bấm “Lập lá số mới” để bắt đầu.</div>`;
    } else {
      list = S.profiles.map((p) => {
        const i = p.input || {};
        const nm = p.name || 'Không tên';
        const date = `${i.day}/${i.month}/${i.year}${i.calendar === 'lunar' ? ' (âm' + (i.leap ? ', nhuận' : '') + ')' : ''}`;
        const time = i.timeMode === 'canh' ? 'giờ ' + (CANH.find((c) => c[0] === i.canh) || ['', ''])[1].split(' ')[0]
          : i.timeMode === 'unknown' ? 'không rõ giờ' : `${pad(i.hour)}:${pad(i.minute || 0)}`;
        return `<div class="profile" data-act="open-profile" data-id="${esc(p.id)}">` +
          `<div class="av ${i.gender === 'NU' ? 'nu' : ''}">${esc(nm.trim().charAt(0).toUpperCase() || '?')}</div>` +
          `<div class="meta"><b>${esc(nm)}</b><span>${i.gender === 'NU' ? 'Nữ' : 'Nam'} · ${esc(date)} · ${esc(time)}</span></div>` +
          `<button class="icon-btn" data-act="del-profile" data-id="${esc(p.id)}" aria-label="Xóa">✕</button></div>`;
      }).join('');
    }
    return `<div class="brand"><h1>Tử Vi Tự Xem</h1><p>Hôm nay ${esc(t.solar)} · Âm lịch ${esc(t.lunar)} · năm ${esc(t.lunarYearCanChi)}</p></div>` +
      `<button class="btn primary block" data-act="new">＋ Lập lá số mới</button>` +
      `<div class="card"><h2>Lá số đã lưu</h2>${list}</div>` +
      `<div class="row"><button class="btn" data-act="settings">⚙ Cài đặt</button></div>` +
      `<p class="note">Chạy hoàn toàn trên máy · lá số không rời điện thoại.<br>Đức năng thắng số — lựa chọn của bạn quyết định kết quả.</p>`;
  }

  // ——— Form nhập liệu (mục 2.1) ———
  function opts(from, to, sel, ph) {
    let s = `<option value="">${ph}</option>`;
    for (let i = from; i <= to; i++) s += `<option value="${i}"${String(sel) === String(i) ? ' selected' : ''}>${i}</option>`;
    return s;
  }
  function seg(field, items, value) {
    return `<div class="seg" role="radiogroup">` + items.map(([v, label]) =>
      `<button type="button" class="${value === v ? 'on' : ''}" data-act="seg" data-f="${field}" data-v="${v}" role="radio" aria-checked="${value === v}">${label}</button>`).join('') + `</div>`;
  }
  function zoneOptions(sel) {
    let all = [];
    try { all = Intl.supportedValuesOf('timeZone'); } catch (e) { all = COMMON_ZONES; }
    const rest = all.filter((z) => COMMON_ZONES.indexOf(z) < 0 && !/^Asia\/(Ho_Chi_Minh|Saigon)$/.test(z));
    const o = (z) => `<option value="${z}"${z === sel ? ' selected' : ''}>${z.replace(/_/g, ' ')}</option>`;
    return `<option value="">— Chọn múi giờ —</option><optgroup label="Hay dùng">${COMMON_ZONES.map(o).join('')}</optgroup>` +
      `<optgroup label="Tất cả">${rest.map(o).join('')}</optgroup>`;
  }
  function leapInfo(year) {
    const y = parseInt(year, 10);
    if (!(y >= 1900 && y <= 2100)) return null;
    if (!S.lunarCache[y]) S.lunarCache[y] = safe(() => api('lunarYear', { year: y }), null);
    return S.lunarCache[y];
  }
  function viewForm() {
    const f = S.form;
    const lunar = f.calendar === 'lunar';
    const li = lunar ? leapInfo(f.year) : null;
    const leapMonths = li ? [li.leap7, li.leap8].filter((m) => m > 0) : [];
    const canLeap = lunar && leapMonths.indexOf(parseInt(f.month, 10)) >= 0;
    if (!canLeap) f.leap = false;
    const leapNote = lunar && li ? (leapMonths.length ? `Năm ${li.year} (${li.canChi}) nhuận tháng ${Array.from(new Set(leapMonths)).join(' / ')}.` : `Năm ${li.year} (${li.canChi}) không có tháng nhuận.`) : '';
    return bar(S.editingId ? 'Sửa thông tin' : 'Lập lá số', 'Nhập thông tin sinh') +
      (S.formError ? `<div class="error-box" id="form-error">${esc(S.formError)}</div>` : '') +
      `<div class="card">` +
      `<div class="field"><label for="f-name">Họ tên hoặc biệt danh</label><input id="f-name" class="input" data-f="name" value="${esc(f.name)}" placeholder="Không bắt buộc" autocomplete="off"></div>` +
      `<div class="field"><span class="label req">Giới tính</span>${seg('gender', [['NAM', 'Nam'], ['NU', 'Nữ']], f.gender)}</div>` +
      `<div class="field"><span class="label req">Ngày sinh</span>${seg('calendar', [['solar', 'Dương lịch'], ['lunar', 'Âm lịch']], f.calendar)}</div>` +
      `<div class="grid3">` +
      `<select class="input" data-f="day" aria-label="Ngày">${opts(1, lunar ? 30 : 31, f.day, 'Ngày')}</select>` +
      `<select class="input" data-f="month" aria-label="Tháng">${opts(1, 12, f.month, 'Tháng')}</select>` +
      `<input class="input" data-f="year" inputmode="numeric" maxlength="4" placeholder="Năm (1900–2100)" value="${esc(f.year)}" aria-label="Năm">` +
      `</div>` +
      (lunar ? `<div class="hint">${esc(leapNote)}</div>` : '') +
      (canLeap ? `<label class="check"><input type="checkbox" data-f="leap"${f.leap ? ' checked' : ''}> Tháng nhuận</label>` : '') +
      `<div class="hint" id="conv"></div>` +
      `<div class="field"><span class="label req">Giờ sinh</span>${seg('timeMode', [['clock', 'Giờ:phút'], ['canh', 'Canh giờ'], ['unknown', 'Không rõ']], f.timeMode)}</div>` +
      (f.timeMode === 'clock' ? `<div class="grid2"><select class="input" data-f="hour" aria-label="Giờ">${opts(0, 23, f.hour, 'Giờ')}</select>` +
        `<select class="input" data-f="minute" aria-label="Phút">${opts(0, 59, f.minute, 'Phút')}</select></div>` : '') +
      (f.timeMode === 'canh' ? `<select class="input" data-f="canh" aria-label="Canh giờ"><option value="">— Chọn canh giờ —</option>` +
        CANH.map(([k, l]) => `<option value="${k}"${f.canh === k ? ' selected' : ''}>${l}</option>`).join('') + `</select>` : '') +
      (f.timeMode === 'unknown' ? `<div class="hint">App sẽ lập đủ 12 lá số theo 12 canh giờ để bạn so sánh và chọn.</div>` : '') +
      `<div class="field"><label for="f-region">Nơi sinh</label><select id="f-region" class="input" data-f="region">` +
      [['VN_BAC', 'Việt Nam — miền Bắc'], ['VN_NAM', 'Việt Nam — miền Nam'], ['NUOC_NGOAI', 'Nước ngoài']].map(([k, l]) =>
        `<option value="${k}"${f.region === k ? ' selected' : ''}>${l}</option>`).join('') + `</select>` +
      `<div class="hint">Miền Bắc/Nam chỉ khác nhau với người sinh trước 30/04/1975.</div></div>` +
      (f.region === 'NUOC_NGOAI' ? `<div class="field"><label for="f-zone" class="req">Múi giờ nơi sinh</label><select id="f-zone" class="input" data-f="zone">${zoneOptions(f.zone)}</select></div>` : '') +
      `<details class="adv"${f.useSolar ? ' open' : ''}><summary>Nâng cao: giờ Mặt Trời thật</summary>` +
      `<label class="check"><input type="checkbox" data-f="useSolar"${f.useSolar ? ' checked' : ''}> Hiệu chỉnh theo kinh độ nơi sinh</label>` +
      (f.useSolar ? `<input class="input" data-f="longitude" inputmode="decimal" placeholder="Kinh độ, vd. 105.85 (Hà Nội), 106.70 (TP.HCM)" value="${esc(f.longitude)}">` : '') +
      `<div class="hint">Thường không cần. Chỉ dùng khi bạn theo trường phái tính giờ Mặt Trời thật.</div></details>` +
      `<div class="field"><label for="f-vy">Năm xem vận hạn (âm lịch)</label><input id="f-vy" class="input" data-f="viewYear" inputmode="numeric" maxlength="4" value="${esc(f.viewYear)}"></div>` +
      `</div>` +
      `<button class="btn primary block" data-act="submit">Lập lá số</button>` +
      `<p class="note">Trường phái: ${esc(presetName(S.settings.preset))} · đổi trong Cài đặt.</p>`;
  }
  function afterForm() { updateConv(); }
  function updateConv() {
    const el = $('#conv');
    if (!el) return;
    const f = S.form;
    if (!f.day || !f.month || !/^\d{4}$/.test(f.year)) { el.textContent = ''; el.className = 'hint'; return; }
    try {
      const r = api('convert', { calendar: f.calendar, day: +f.day, month: +f.month, year: +f.year, leap: f.calendar === 'lunar' && !!f.leap, region: f.region });
      el.textContent = r.text; el.className = 'hint ok';
    } catch (e) { el.textContent = e.message; el.className = 'hint bad'; }
  }
  function presetName(k) { return (PRESETS.find((p) => p[0] === k) || PRESETS[0])[1]; }

  function inputFromForm(f) {
    return {
      name: f.name.trim(), gender: f.gender, calendar: f.calendar, day: +f.day, month: +f.month, year: +f.year,
      leap: f.calendar === 'lunar' && !!f.leap, timeMode: f.timeMode,
      hour: f.hour === '' ? null : +f.hour, minute: f.minute === '' ? 0 : +f.minute, canh: f.canh,
      region: f.region, zone: f.zone, trueSolar: !!f.useSolar,
      longitude: f.useSolar && String(f.longitude).trim() !== '' ? parseFloat(String(f.longitude).replace(',', '.')) : null,
    };
  }
  function formFromInput(i) {
    const f = defaultForm();
    Object.assign(f, {
      name: i.name || '', gender: i.gender || '', calendar: i.calendar || 'solar', day: str(i.day), month: str(i.month), year: str(i.year),
      leap: !!i.leap, timeMode: i.timeMode || 'clock', hour: str(i.hour), minute: str(i.minute), canh: i.canh || '', region: i.region || 'VN_BAC',
      zone: i.zone || '', useSolar: !!i.trueSolar, longitude: i.longitude == null ? '' : String(i.longitude),
    });
    if (S.req && S.req.viewYear) f.viewYear = String(S.req.viewYear);
    return f;
  }
  const str = (v) => (v == null ? '' : String(v));
  const pad = (n) => (n == null || n === '' ? '--' : (+n < 10 ? '0' : '') + (+n));
  const withPreset = (input) => Object.assign({}, input, { preset: S.settings.preset, expert: !!S.settings.expert });

  function validate(f) {
    if (!f.gender) return 'Chưa chọn giới tính.';
    if (!f.day || !f.month || !f.year) return 'Chưa nhập đủ ngày, tháng, năm sinh.';
    const y = parseInt(f.year, 10);
    if (!(y >= 1900 && y <= 2100)) return 'Năm sinh phải trong khoảng 1900–2100.';
    if (f.timeMode === 'clock' && f.hour === '') return 'Chưa chọn giờ sinh. Nếu không nhớ giờ, chọn “Không rõ”.';
    if (f.timeMode === 'canh' && !f.canh) return 'Chưa chọn canh giờ.';
    if (f.region === 'NUOC_NGOAI' && !f.zone) return 'Chưa chọn múi giờ nơi sinh.';
    if (f.useSolar && String(f.longitude).trim() === '') return 'Nhập kinh độ hoặc bỏ chọn hiệu chỉnh giờ Mặt Trời thật.';
    const vy = parseInt(f.viewYear, 10);
    if (!(vy >= 1900 && vy <= 2200)) return 'Năm xem phải trong khoảng 1900–2200.';
    return '';
  }
  function submitForm() {
    const f = S.form;
    const err = validate(f);
    if (err) { S.formError = err; render(false); scrollToError(); return; }
    const input = inputFromForm(f);
    try {
      if (f.timeMode === 'unknown') {
        S.hours = api('allHours', { input: withPreset(input) });
        S.hoursInput = input;
        S.formError = '';
        go('hours');
        return;
      }
      const data = api('chart', { input: withPreset(input), viewYear: +f.viewYear });
      S.pending = { input, data, viewYear: +f.viewYear };
      S.saveOnConfirm = true;
      S.formError = '';
      go('confirm');
    } catch (e) { S.formError = e.message; render(false); scrollToError(); }
  }
  function scrollToError() { const el = $('#form-error'); if (el) el.scrollIntoView({ block: 'center' }); }

  // ——— Thẻ xác nhận lá số (mục 2.2) ———
  function infoRows(info) {
    const rows = [
      ['Dương lịch', `${info.solar}${info.clock ? ' · ' + info.clock : ''}`],
      ['Âm lịch', `${info.lunar} · năm ${info.yearCanChi}`],
    ];
    if (info.referenceTime && info.timeMode === 'clock' && info.referenceTime.indexOf(info.clock) !== 0) rows.push(['Giờ quy đổi', info.referenceTime]);
    rows.push(
      ['Tứ trụ', `Năm ${info.yearCanChi} · Tháng ${info.monthCanChi} · Ngày ${info.dayCanChi} · Giờ ${info.hourCanChi}`],
      ['Âm Dương', `${info.amDuong} — Âm Dương ${info.thuanLy ? 'thuận lý' : 'nghịch lý'}`],
      ['Bản mệnh', `${info.banMenh} (${info.banMenhHanh})`],
      ['Cục', `${info.cuc} — ${info.cucMenh}`],
      ['Mệnh · Thân', `Mệnh tại ${info.menh} · Thân tại ${info.than} (cư ${info.thanRole})`],
      ['Mệnh chủ', `${info.menhChu} · Thân chủ ${info.thanChu}`],
      ['Tuổi âm', `${info.tuoiAmNow} tuổi (năm ${S.today ? S.today.lunarYear : ''})`],
      ['Nơi sinh', info.region],
      ['Trường phái', info.preset],
    );
    return `<dl class="kv">${rows.map(([k, v]) => `<dt>${esc(k)}</dt><dd>${esc(v)}</dd>`).join('')}</dl>`;
  }
  function viewConfirm() {
    const p = S.pending;
    if (!p) return viewHome();
    const d = p.data, info = d.info;
    const warns = d.warnings.map((w, i) => `<div class="warn"><b>⚠ ${esc(w.title)}</b><p>${esc(w.message)}</p>` +
      (w.alt ? `<button class="btn small" data-act="alt" data-i="${i}">Xem lá số thay thế</button>` : '') + `</div>`).join('');
    return bar('Xác nhận lá số', info.name || 'Kiểm tra thông tin trước khi xem') +
      `<div class="card"><h2>${esc(info.name || 'Lá số')} · ${esc(info.gender)}</h2>${infoRows(info)}</div>` +
      (warns ? `<div class="card"><h2>Cần lưu ý</h2>${warns}</div>` : '') +
      `<label class="check"><input type="checkbox" data-act="toggle-save"${S.saveOnConfirm ? ' checked' : ''}> Lưu vào danh sách lá số</label>` +
      `<div class="stack"><button class="btn primary block" data-act="confirm">Đúng — xem lá số</button>` +
      `<button class="btn block" data-act="back">Sửa thông tin</button></div>`;
  }
  function confirmChart() {
    const p = S.pending;
    let id = S.editingId;
    if (S.saveOnConfirm) {
      if (id) {
        const pr = S.profiles.find((x) => x.id === id);
        if (pr) { pr.input = p.input; pr.name = p.input.name; pr.updatedAt = Date.now(); }
      } else {
        id = 'p' + Date.now().toString(36);
        S.profiles.unshift({ id, name: p.input.name, input: p.input, createdAt: Date.now() });
      }
      saveProfiles();
    }
    S.profileId = S.saveOnConfirm ? id : null;
    S.editingId = null;
    S.req = { input: p.input, viewYear: p.viewYear, alt: null };
    S.chart = p.data;
    S.sel = null;
    S.pending = null;
    reset(['home', 'chart']);
  }

  // ——— Không rõ giờ sinh: 12 lá số ———
  function viewHours() {
    const rows = (S.hours || []).map((h) => `<div class="h" data-act="pick-hour" data-canh="${h.canh}"><b>${esc(h.label.split(' (')[0])}</b>` +
      `<div><div>Mệnh <b>${esc(h.menh)}</b>: ${esc(h.menhStars)}</div><div class="small muted">Thân ${esc(h.than)} (cư ${esc(h.thanRole)}) · ${esc(h.cuc)} · đại hạn đầu ${esc(h.daiHanDau)}</div></div></div>`).join('');
    return bar('Không rõ giờ sinh', '12 lá số theo 12 canh giờ') +
      `<div class="card"><p class="small muted" style="margin-top:0">Đây chỉ là ước đoán. Hãy đọc chính tinh cung Mệnh của từng giờ, chọn giờ bạn thấy khớp nhất với bản thân.</p>` +
      `<div class="hours">${rows}</div></div>`;
  }

  // ——— Cài đặt ———
  function viewSettings() {
    const ver = window.TuViNative && window.TuViNative.version ? window.TuViNative.version() : 'bản thử trên máy tính';
    return bar('Cài đặt') +
      `<div class="card"><h2>Trường phái an sao</h2>` +
      PRESETS.map(([k, n, d]) => `<div class="opt ${S.settings.preset === k ? 'on' : ''}" data-act="preset" data-k="${k}"><b>${esc(n)}</b><span>${esc(d)}</span></div>`).join('') +
      `<p class="small muted">Áp dụng cho các lá số mở sau khi đổi.</p></div>` +
      `<div class="card"><h2>Luận giải</h2><div class="opt ${S.settings.expert ? 'on' : ''}" data-act="expert"><b>Chế độ chuyên sâu ${S.settings.expert ? '· đang bật' : '· đang tắt'}</b>` +
      `<span>Hiện thêm các luật độ tin cậy thấp (C), còn nhiều tranh luận giữa các trường phái.</span></div>` +
      `<p class="small muted">Áp dụng cho các lá số mở sau khi đổi.</p></div>` +
      `<div class="card"><h2>Về ứng dụng</h2><p class="small">Tử Vi Tự Xem ${esc(ver)} — bản chạy thử (giai đoạn 3): lá số, đại hạn, tiểu hạn, lưu niên, sao lưu, nguyệt hạn và luận giải sơ bộ (cách cục, câu luận từng cung). Câu luận hiện là bản nháp tự sinh, chưa qua thẩm định; phần chấm điểm, tổng hợp theo chủ đề có ở bản sau.</p>` +
      `<p class="small muted">${esc(ONBOARD)}</p></div>`;
  }

  // ——— Lá số (mục 13) ———
  function viewChart() {
    const d = S.chart;
    if (!d) return viewHome();
    const info = d.info, y = d.year;
    const birthYear = parseInt(info.lunar.split('/').pop(), 10);
    const minY = Math.max(1900, birthYear), maxY = Math.min(2200, birthYear + 100);
    const L = S.layers;
    const chips = [['han', 'Đại / tiểu hạn'], ['luu', 'Sao lưu'], ['hoa', 'Tứ Hóa lưu'], ['month', 'Nguyệt hạn']]
      .map(([k, l]) => `<button class="chip ${L[k] ? 'on' : ''}" data-act="layer" data-k="${k}">${l}</button>`).join('');
    const months = L.month ? `<div class="chips months">${d.months.map((m) =>
      `<button class="chip ${S.month === m.month ? 'on' : ''}" data-act="month" data-m="${m.month}">T${m.month}</button>`).join('')}</div>` : '';
    const banner = d.alt ? `<div class="banner"><span>Đang xem <b>lá số thay thế</b> (${esc(d.alt)}).</span><button class="btn small" data-act="alt-off">Về lá số chính</button></div>` : '';
    return bar(info.name || 'Lá số', `${info.gender} · ${info.solar}${info.clock ? ' · ' + info.clock : ''}`,
      `<button class="icon-btn" data-act="edit" aria-label="Sửa">✎</button>`) +
      banner +
      `<div class="card" style="padding:10px 12px">` +
      `<div class="yearbar"><button class="icon-btn" data-act="year" data-d="-1" aria-label="Năm trước">◀</button>` +
      `<div class="yv"><b>Năm ${y.lunarYear} · ${esc(y.canChi)}</b><span>${y.tuoiAm} tuổi âm${y.beforeFirstDaiHan ? ' · chưa vào đại hạn đầu' : ` · đại hạn ${y.daiHanFrom}–${y.daiHanTo} (năm thứ ${y.namThu})`}</span></div>` +
      `<button class="icon-btn" data-act="year" data-d="1" aria-label="Năm sau">▶</button></div>` +
      `<input type="range" min="${minY}" max="${maxY}" value="${y.lunarYear}" data-act="year-range" aria-label="Chọn năm xem">` +
      `<div class="chips">${chips}</div>${months}</div>` +
      `<div class="board-wrap"><div class="board" id="board">${boardCells(d)}${centerBox(d)}<svg class="overlay" id="overlay"></svg></div></div>` +
      `<div class="legend"><span><i>M</i> Miếu · <i>V</i> Vượng · <i>Đ</i> Đắc · <i>B</i> Bình · <i>H</i> Hãm</span>` +
      `<span><i class="e-kim">Kim</i> · <i class="e-moc">Mộc</i> · <i class="e-thuy">Thủy</i> · <i class="e-hoa">Hỏa</i> · <i class="e-tho">Thổ</i></span>` +
      `<span><i style="color:var(--luu)">L.</i> sao lưu năm · chạm một cung để xem chi tiết</span></div>` +
      readingCard(d) + yearCard(d) + daiHanCard(d) + monthCard(d) +
      `<p class="note">Luận giải đang là bản nháp tự sinh, chưa thẩm định. Tử Vi là bản đồ thời tiết — đức năng thắng số, lựa chọn của bạn quyết định kết quả.</p>`;
  }

  function starSpan(s, cls) {
    const b = s.b ? `<span class="bri">${esc(s.b)}</span>` : '';
    return `<span class="${cls || 'st'}${s.e ? ' e-' + s.e : ''}">${esc(s.n)}${b}</span>`;
  }
  function overlaysFor(d) {
    const y = d.year, L = S.layers;
    const by = Array.from({ length: 12 }, () => ({ luu: [], hoa: [], marks: [] }));
    if (L.han) {
      by[y.daiHan].marks.push(['dh', 'ĐH']);
      by[y.tieuHan].marks.push(['th', 'TH']);
      by[y.luuNienDaiHan].marks.push(['ln', 'LN']);
    }
    if (L.month) {
      const m = d.months.find((x) => x.month === S.month);
      if (m && m.chi != null) by[m.chi].marks.push(['nh', 'NH' + m.month]);
    }
    if (L.luu) y.luuStars.forEach((s) => by[s.chi].luu.push(s));
    if (L.hoa && y.luuTuHoa) y.luuTuHoa.forEach((h) => by[h.chi].hoa.push(h));
    return by;
  }
  function boardCells(d) {
    const ov = overlaysFor(d);
    const y = d.year;
    return d.palaces.map((p) => {
      const [r, c] = POS[p.chi];
      const main = p.stars.filter((s) => s.s === 'main');
      const hoa = p.stars.filter((s) => s.s === 'hoa');
      const good = p.stars.filter((s) => s.s === 'good');
      const bad = p.stars.filter((s) => s.s === 'bad');
      const ring = p.stars.filter((s) => s.s === 'ring');
      const o = ov[p.chi];
      const isCurDh = S.layers.han && y.daiHan === p.chi;
      return `<div class="cell" data-act="palace" data-chi="${p.chi}" style="grid-row:${r + 1};grid-column:${c + 1}" ` +
        `aria-label="${esc(p.role)} ${esc(p.can)} ${esc(p.chiVn)}: ${esc(main.map((s) => s.n).join(', ') || 'Vô chính diệu')}">` +
        `<div class="c-top"><span class="c-cc">${esc(p.can)} ${esc(p.chiVn)}</span><span class="c-dh${isCurDh ? ' cur' : ''}">${p.daiHan}</span></div>` +
        `<div class="c-role">${esc(p.role)}${p.isThan ? '<span class="than-tag">THÂN</span>' : ''}</div>` +
        (o.marks.length ? `<div class="c-marks">${o.marks.map(([k, t]) => `<i class="mk ${k}">${t}</i>`).join('')}</div>` : '') +
        `<div class="c-main">${main.length ? main.map((s) => `<div>${starSpan(s, 'ms')}</div>`).join('') : '<span class="vcd">Vô chính diệu</span>'}</div>` +
        (hoa.length || o.hoa.length ? `<div class="c-hoa">${hoa.map((s) => `<span class="hb ${HOA_KEY[s.k]}">${HOA_SHORT[s.k]}</span>`).join('')}` +
          `${o.hoa.map((h) => `<span class="hb l ${h.k}">${esc(h.n)}</span>`).join('')}</div>` : '') +
        `<div class="c-minor"><div class="grp good">${good.map((s) => starSpan(s)).join(' ')}</div><div class="grp bad">${bad.map((s) => starSpan(s)).join(' ')}</div></div>` +
        (o.luu.length ? `<div class="c-luu">${o.luu.map((s) => `<span class="st">${esc(s.n)}</span>`).join(' ')}</div>` : '') +
        `<div class="c-foot"><span>${ring.map((s) => esc(s.n)).join(' ')}</span><span class="th-chi">${esc(p.tieuHan)}</span></div>` +
        `</div>`;
    }).join('');
  }
  function centerBox(d) {
    const i = d.info;
    const row = (k, v) => `<div class="ct-row"><span>${esc(k)}</span><b>${esc(v)}</b></div>`;
    return `<div class="center">` +
      `<div class="ct-name">${esc(i.name || 'Lá số Tử Vi')}</div>` +
      row('Dương lịch:', `${i.solar}${i.clock ? ' ' + i.clock.split(' (')[0] : ''}`) +
      row('Âm lịch:', `${i.lunar}, giờ ${i.hourChi}`) +
      row('Năm:', `${i.yearCanChi} · tháng ${i.monthCanChi}`) +
      row('Ngày:', `${i.dayCanChi} · giờ ${i.hourCanChi}`) +
      row('', `${i.amDuong} · ${i.thuanLy ? 'thuận lý' : 'nghịch lý'}`) +
      row('Bản mệnh:', i.banMenh) +
      row('Cục:', i.cuc) +
      `<div class="small muted" style="font-size:10px">${esc(i.cucMenh)}</div>` +
      row('Mệnh chủ:', i.menhChu) +
      row('Thân chủ:', i.thanChu) +
      row('Năm xem:', `${i.viewYear} ${i.viewYearCanChi} · ${i.tuoiAm} tuổi`) +
      `<div class="ct-foot">${esc(i.preset)}${d.alt ? ' · lá số thay thế' : ''}</div>` +
      `</div>`;
  }
  function yearCard(d) {
    const y = d.year;
    const ch = (i) => CHI[i];
    const dh = y.beforeFirstDaiHan ? `Chưa vào (từ ${d.daiHan[0].from} tuổi)` : `${ch(y.daiHan)} · ${y.daiHanCanChi}`;
    const hoa = (y.luuTuHoa || []).map((h) => `<span class="tag">${esc(h.full)} lưu → ${ch(h.chi)}</span>`).join('');
    const luu = y.luuStars.map((s) => `<span class="tag">${esc(s.n)} → ${ch(s.chi)}</span>`).join('');
    return `<div class="card"><h2>Vận hạn năm ${y.lunarYear} (${esc(y.canChi)}) · ${y.tuoiAm} tuổi</h2>` +
      `<div class="facts">` +
      `<div class="fact"><span>Đại hạn 10 năm</span><b>${esc(dh)}</b><small>${y.beforeFirstDaiHan ? '' : `${y.daiHanFrom}–${y.daiHanTo} tuổi · năm thứ ${y.namThu} · cung ${esc(y.daiHanRoleGoc)}`}</small></div>` +
      `<div class="fact"><span>Thiên thời đại hạn</span><b><span class="score s${scoreOf(d, y.daiHanIndex)}">${fmtScore(scoreOf(d, y.daiHanIndex))}</span> ${esc(y.thienThoi)}</b><small>${esc(y.thienThoiLabel)}</small></div>` +
      `<div class="fact"><span>Tiểu hạn (diễn biến năm)</span><b>${ch(y.tieuHan)}</b><small>cung ${esc(roleAt(d, y.tieuHan))}</small></div>` +
      `<div class="fact"><span>Lưu niên đại hạn (kết quả năm)</span><b>${ch(y.luuNienDaiHan)}</b><small>cung ${esc(roleAt(d, y.luuNienDaiHan))}</small></div>` +
      `<div class="fact"><span>Lưu Thái Tuế · Lưu Mệnh</span><b>${ch(y.luuThaiTue)}</b><small>cung ${esc(roleAt(d, y.luuThaiTue))}</small></div>` +
      `<div class="fact"><span>Can chi năm xem</span><b>${esc(y.canChi)}</b><small>Tứ Hóa lưu theo can năm</small></div>` +
      `</div>` +
      (hoa ? `<h3>Tứ Hóa lưu</h3><div class="tags">${hoa}</div>` : '') +
      `<h3>Sao lưu</h3><div class="tags">${luu}</div>` + yearReading(d) + `</div>`;
  }
  const roleAt = (d, chi) => d.palaces[chi].role;
  const scoreOf = (d, i) => d.daiHan[i].score;
  const fmtScore = (s) => (s > 0 ? '+' + s : String(s));
  function daiHanCard(d) {
    const cur = d.year.beforeFirstDaiHan ? -1 : d.year.daiHanIndex;
    const items = d.daiHan.map((p) => `<div class="dh ${p.i === cur ? 'cur' : ''}" data-act="goto-year" data-y="${p.year}">` +
      `<b>${p.from}–${p.to}</b><span>${esc(CHI[p.chi])} · ${esc(p.canChi)}</span><span>cung ${esc(roleAt(d, p.chi))}</span>` +
      `<span><span class="score s${p.score}">${fmtScore(p.score)}</span> ${esc(p.thienThoi)}</span><span>từ năm ${p.year}</span></div>`).join('');
    return `<div class="card"><h2>12 đại hạn</h2><div class="tl" id="tl">${items}</div><p class="small muted" style="margin:4px 0 0">Chạm một đại hạn để xem năm đầu của vận đó. Điểm theo Thiên thời ngũ hành tam hợp.</p></div>`;
  }
  function monthCard(d) {
    const items = d.months.map((m) => `<div class="mo ${S.layers.month && S.month === m.month ? 'on' : ''}" data-act="month-card" data-m="${m.month}">` +
      `Tháng ${m.month}${m.leapAfter ? '*' : ''}<b>${m.chi == null ? '—' : esc(CHI[m.chi])}</b><span class="small muted">${esc(m.canChi || '')}</span></div>`).join('');
    const leap = d.months.find((m) => m.leapAfter);
    return `<div class="card"><h2>Nguyệt hạn năm ${d.year.lunarYear}</h2><div class="months">${items}</div>` +
      `<p class="small muted" style="margin:6px 0 0">Cung nguyệt hạn của từng tháng âm.${leap ? ` *Năm có tháng ${leap.month} nhuận, dùng chung cung với tháng ${leap.month}.` : ''}</p></div>`;
  }

  // ——— Luận giải sơ bộ (giai đoạn 3): cách cục, dấu hiệu, câu luận từng cung ———
  const polCls = (p) => (p > 0 ? 'pos' : p < 0 ? 'neg' : 'neu');
  const polTxt = (p) => (p >= 2 ? '++' : p === 1 ? '+' : p === 0 ? '·' : p === -1 ? '−' : '−−');
  const ADV = { NEN: 'Nên', KHONG_NEN: 'Không nên', LUU_Y: 'Lưu ý' };
  function rdItem(it, key) {
    const open = !!S.rdOpen[key];
    return `<div class="rd ${polCls(it.pol)}${open ? ' open' : ''}" data-act="rd" data-key="${esc(key)}">` +
      `<div class="rd-h"><span class="pol">${polTxt(it.pol)}</span><b>${esc(it.name)}</b><span class="rd-role">${esc(it.role)}</span></div>` +
      `<div class="rd-t">${esc(it.text)}</div>` +
      `<div class="rd-more"><div class="rd-sub">Vì sao?</div><ul>${it.why.map((w) => `<li>${esc(w)}</li>`).join('')}</ul>` +
      (it.advice.length ? `<div class="rd-sub">Gợi ý</div><ul>${it.advice.map((a) => `<li><b>${ADV[a.type] || ''}:</b> ${esc(a.t)}</li>`).join('')}</ul>` : '') +
      `<div class="rd-src">${esc(it.typeVn)} · độ tin cậy ${esc(it.rel)} · ${esc(it.topics.join(', '))}<br>Nguồn: ${esc(it.source)}</div></div>` +
      `<div class="rd-tap">${open ? 'Thu gọn ‹' : 'Vì sao? ›'}</div></div>`;
  }
  function readingCard(d) {
    const r = d.reading;
    if (!r) return '';
    if (r.error) return `<div class="card" id="reading"><h2>Cách cục &amp; dấu hiệu lá số</h2><p class="small muted">Chưa tải được phần luận giải (${esc(r.error)}). Lá số và vận hạn vẫn dùng bình thường.</p></div>`;
    const groups = [];
    r.natal.forEach((it) => {
      let g = groups.find((x) => x.name === it.typeVn);
      if (!g) { g = { name: it.typeVn, items: [] }; groups.push(g); }
      g.items.push(it);
    });
    const body = groups.map((g) => `<h3>${esc(g.name)} (${g.items.length})</h3>` + g.items.map((it) => rdItem(it, 'n:' + it.id + ':' + it.chi)).join('')).join('');
    return `<div class="card" id="reading"><h2>Cách cục &amp; dấu hiệu lá số</h2>` +
      `<div class="draft">Bản nháp tự sinh từ ${r.counts.rules} luật của cẩm nang, chưa qua thẩm định — chỉ để tham khảo.${r.expert ? ' Đang bật chế độ chuyên sâu.' : ''} Chạm một mục để xem sao, cung làm căn cứ và gợi ý.</div>` +
      (body || '<p class="small muted">Không có cách cục nổi bật nào khớp với lá số này.</p>') + `</div>`;
  }
  function yearReading(d) {
    const r = d.reading;
    if (!r || r.error) return '';
    const yr = r.year.map((it) => rdItem(it, 'y:' + it.id + ':' + it.chi)).join('');
    const dh = r.daiHan.map((it) => rdItem(it, 'd:' + it.id + ':' + it.chi)).join('');
    return `<h3>Dấu hiệu năm ${d.year.lunarYear} (bản nháp)</h3>` + (yr || '<p class="small muted">Không có dấu hiệu nổi bật.</p>') +
      (dh ? `<h3>Đại hạn đang đi (bản nháp)</h3>${dh}` : '');
  }
  function palaceReading(d, chi) {
    const r = d.reading;
    if (!r || r.error) return '';
    const lines = (r.palaces[chi] || []).map((x) => `<div class="pl ${polCls(x.pol)}"><b>${esc(x.title)}</b><div>${esc(x.text)}</div></div>`).join('');
    const rules = r.natal.concat(r.daiHan, r.year).filter((it) => (it.chis || [it.chi]).indexOf(chi) >= 0).map((it) => rdItem(it, 'p:' + it.id + ':' + it.chi)).join('');
    return `<h3>Luận giải sơ bộ (bản nháp)</h3>` + (lines || '<div class="small muted">—</div>') +
      (rules ? `<h3>Cách cục, dấu hiệu tại cung này</h3>${rules}` : '') +
      `<p class="small muted">Câu luận tự sinh, chưa thẩm định. Chấm điểm và tổng hợp theo chủ đề có ở bản sau.</p>`;
  }

  // Sau khi dựng lưới: vẽ Tuần/Triệt trên đường ranh giới và đường tam phương tứ chính.
  function afterChart() {
    applySelection();
    placeTuanTriet();
    const tl = $('#tl'), cur = tl && tl.querySelector('.cur');
    if (cur) tl.scrollLeft = cur.offsetLeft - 8;
  }
  function cellRect(chi) {
    const board = $('#board');
    const el = board && board.querySelector(`.cell[data-chi="${chi}"]`);
    if (!el) return null;
    return { x: el.offsetLeft, y: el.offsetTop, w: el.offsetWidth, h: el.offsetHeight };
  }
  function placeTuanTriet() {
    const board = $('#board');
    if (!board || !S.chart) return;
    board.querySelectorAll('.tt').forEach((e) => e.remove());
    const pairs = [];
    const key = (a, b) => [Math.min(a, b), Math.max(a, b)].join('-');
    const t = S.chart.tuan, r = S.chart.triet;
    const same = key(t[0], t[1]) === key(r[0], r[1]);
    pairs.push([t, same ? 'TUẦN · TRIỆT' : 'TUẦN']);
    if (!same) pairs.push([r, 'TRIỆT']);
    pairs.forEach(([pr, label]) => {
      const a = cellRect(pr[0]), b = cellRect(pr[1]);
      if (!a || !b) return;
      let x, y, vertical = false;
      if (Math.abs(a.y - b.y) < 2) { // cùng hàng → dải dọc trên ranh giới hai cung
        x = (a.x < b.x ? a.x + a.w : b.x + b.w); y = a.y + Math.min(a.h, b.h) / 2; vertical = true;
      } else { // cùng cột → dải ngang
        y = (a.y < b.y ? a.y + a.h : b.y + b.h); x = a.x + a.w / 2;
      }
      const el = document.createElement('div');
      el.className = vertical ? 'tt v' : 'tt'; el.textContent = vertical ? label.replace(/ · /g, '·') : label;
      el.style.left = x + 'px'; el.style.top = y + 'px';
      board.appendChild(el);
    });
  }
  function applySelection() {
    const board = $('#board');
    if (!board) return;
    board.querySelectorAll('.cell').forEach((c) => c.classList.remove('sel', 'tam', 'giap', 'nhihop'));
    const svg = $('#overlay');
    if (svg) svg.innerHTML = '';
    if (S.sel == null) return;
    const s = S.sel, m = (k) => ((k % 12) + 12) % 12;
    const tam = [m(s + 4), m(s + 8), m(s + 6)];
    const mark = (chi, cls) => { const el = board.querySelector(`.cell[data-chi="${chi}"]`); if (el) el.classList.add(cls); };
    mark(s, 'sel');
    tam.forEach((c) => mark(c, 'tam'));
    mark(m(s - 1), 'giap'); mark(m(s + 1), 'giap');
    mark(m(1 - s), 'nhihop');
    if (svg) {
      const c = (chi) => { const r = cellRect(chi); return r ? [r.x + r.w / 2, r.y + r.h / 2] : null; };
      const p0 = c(s);
      svg.setAttribute('width', board.offsetWidth); svg.setAttribute('height', board.offsetHeight);
      const lines = [[s, tam[0]], [s, tam[1]], [tam[0], tam[1]], [s, tam[2]]];
      svg.innerHTML = lines.map(([a, b]) => { const A = c(a), B = c(b); return A && B ? `<line x1="${A[0]}" y1="${A[1]}" x2="${B[0]}" y2="${B[1]}"/>` : ''; }).join('');
      if (!p0) svg.innerHTML = '';
    }
  }
  let resizeTimer = 0;
  window.addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(() => { if (top() === 'chart') afterChart(); }, 120); });

  // ——— Bảng chi tiết một cung ———
  function openPalace(chi) {
    const d = S.chart, p = d.palaces[chi], y = d.year, m = (k) => ((k % 12) + 12) % 12;
    const by = (side) => p.stars.filter((s) => s.s === side);
    const list = (arr, withBri) => arr.length ? `<div class="slist">${arr.map((s) => `<span class="tag"><span class="${s.e ? 'e-' + s.e : ''}"><b>${esc(s.n)}</b></span>` +
      `${withBri && s.b ? ' · ' + esc(BRI[s.b] || s.b) : ''}${s.e ? ' · ' + HANH[s.e] : ''}</span>`).join('')}</div>` : '<div class="small muted">—</div>';
    const dh = d.daiHan.find((x) => x.chi === chi);
    const here = [];
    if (y.daiHan === chi) here.push('Đại hạn đang đi');
    if (y.tieuHan === chi) here.push('Tiểu hạn năm ' + y.lunarYear);
    if (y.luuNienDaiHan === chi) here.push('Lưu niên đại hạn');
    if (y.luuThaiTue === chi) here.push('Lưu Thái Tuế (Lưu Mệnh)');
    const luuHere = y.luuStars.filter((s) => s.chi === chi).map((s) => s.n)
      .concat((y.luuTuHoa || []).filter((h) => h.chi === chi).map((h) => h.full + ' lưu'));
    const rel = (arr) => arr.map((c) => `${CHI[c]} (${d.palaces[c].role})`).join(', ');
    const tt = [p.tuan ? 'Tuần' : '', p.triet ? 'Triệt' : ''].filter(Boolean).join(', ');
    $('#sheet').innerHTML = `<div class="grab"></div>` +
      `<div class="row" style="align-items:flex-start"><div><h2>Cung ${esc(p.role)}${p.isThan ? ' · Thân cư' : ''}</h2>` +
      `<div class="muted">${esc(p.can)} ${esc(p.chiVn)}${p.voChinhDieu ? ' · Vô chính diệu' : ''}${tt ? ' · ' + tt : ''}</div></div>` +
      `<button class="icon-btn" data-act="close-sheet" style="flex:none" aria-label="Đóng">✕</button></div>` +
      `<div class="sline"><span>Đại hạn: </span><b>${dh.from}–${dh.to} tuổi</b> (năm ${dh.year}–${dh.year + 9}) · ${esc(dh.canChi)} · <span class="score s${dh.score}">${fmtScore(dh.score)}</span> ${esc(dh.thienThoi)}</div>` +
      `<div class="sline"><span>Tiểu hạn tại cung này: </span>các năm <b>${esc(p.tieuHan)}</b></div>` +
      `<div class="sline"><span>Năm ${y.lunarYear}: </span>${here.length ? '<b>' + esc(here.join(' · ')) + '</b> · ' : ''}lưu niên là cung <b>${esc(y.luuRoles[chi])}</b>, trong đại hạn là cung <b>${esc(y.daiHanRoles[chi])}</b></div>` +
      `<h3>Chính tinh</h3>${p.voChinhDieu ? '<div class="small muted">Vô chính diệu — mượn chính tinh cung xung chiếu ' + esc(CHI[m(chi + 6)]) + ' để luận.</div>' : list(by('main'), true)}` +
      (by('hoa').length ? `<h3>Tứ Hóa gốc</h3>${list(by('hoa'))}` : '') +
      `<h3>Cát tinh</h3>${list(by('good'), true)}` +
      `<h3>Hung tinh</h3>${list(by('bad'), true)}` +
      `<h3>Vòng Tràng Sinh</h3>${list(by('ring'))}` +
      (luuHere.length ? `<h3>Sao lưu năm ${y.lunarYear}</h3><div class="slist">${luuHere.map((n) => `<span class="tag" style="color:var(--luu)">${esc(n)}</span>`).join('')}</div>` : '') +
      `<h3>Liên hệ</h3><div class="small">Tam hợp: <b>${esc(rel([m(chi + 4), m(chi + 8)]))}</b><br>Xung chiếu: <b>${esc(rel([m(chi + 6)]))}</b><br>` +
      `Giáp: <b>${esc(rel([m(chi - 1), m(chi + 1)]))}</b><br>Nhị hợp: <b>${esc(rel([m(1 - chi)]))}</b></div>` +
      palaceReading(d, chi);
    $('#scrim').classList.add('open');
    $('#sheet').classList.add('open');
    $('#sheet').scrollTop = 0;
  }
  function closeSheet() { $('#sheet').classList.remove('open'); $('#scrim').classList.remove('open'); }

  // ——— Hộp thoại ———
  let modalAction = null;
  function openModal(title, text, okLabel, onOk, cancelLabel) {
    $('#modal').innerHTML = `<div class="box"><h2>${esc(title)}</h2><p>${esc(text)}</p><div class="row">` +
      (cancelLabel !== null ? `<button class="btn" data-act="modal-cancel">${esc(cancelLabel || 'Hủy')}</button>` : '') +
      `<button class="btn primary" data-act="modal-ok">${esc(okLabel)}</button></div></div>`;
    modalAction = onOk;
    $('#modal').classList.add('open');
  }
  function closeModal() { $('#modal').classList.remove('open'); modalAction = null; }

  // ——— Lá số: đổi năm, lớp, lá số thay thế ———
  function reloadChart(keepScroll) {
    const sy = window.scrollY;
    try {
      S.chart = api('chart', { input: withPreset(S.req.input), viewYear: S.req.viewYear, alt: S.req.alt });
    } catch (e) { openModal('Không lập được lá số', e.message, 'Đóng', null, null); return; }
    render(false);
    if (keepScroll) window.scrollTo(0, sy);
  }
  function setYear(y) {
    const info = S.chart.info;
    const birthYear = parseInt(info.lunar.split('/').pop(), 10);
    y = Math.max(Math.max(1900, birthYear), Math.min(Math.min(2200, birthYear + 100), y));
    if (y === S.req.viewYear) return;
    S.req.viewYear = y;
    reloadChart(true);
  }
  function openProfile(id) {
    const p = S.profiles.find((x) => x.id === id);
    if (!p) return;
    const viewYear = S.today ? S.today.lunarYear : new Date().getFullYear();
    try {
      S.chart = api('chart', { input: withPreset(p.input), viewYear });
    } catch (e) { openModal('Không mở được lá số', e.message, 'Đóng', null, null); return; }
    S.req = { input: p.input, viewYear: S.chart.info.viewYear, alt: null };
    S.profileId = id; S.sel = null;
    go('chart');
  }

  // ———————————————————————————— sự kiện ————————————————————————————
  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    const act = el.dataset.act;
    switch (act) {
      case 'back': window.tvBack(); break;
      case 'new': S.form = defaultForm(); S.formError = ''; S.editingId = null; go('form'); break;
      case 'settings': go('settings'); break;
      case 'open-profile': if (!e.target.closest('[data-act="del-profile"]')) openProfile(el.dataset.id); break;
      case 'del-profile': {
        e.stopPropagation();
        const id = el.dataset.id, p = S.profiles.find((x) => x.id === id);
        openModal('Xóa lá số?', `Xóa “${(p && p.name) || 'Không tên'}” khỏi danh sách? Không hoàn tác được.`, 'Xóa', () => {
          S.profiles = S.profiles.filter((x) => x.id !== id); saveProfiles(); render(false);
        });
        break;
      }
      case 'seg': {
        S.form[el.dataset.f] = el.dataset.v;
        if (el.dataset.f === 'calendar') { S.form.leap = false; if (S.form.calendar === 'lunar' && +S.form.day > 30) S.form.day = '30'; }
        render(false); break;
      }
      case 'submit': submitForm(); break;
      case 'toggle-save': S.saveOnConfirm = el.checked; break;
      case 'confirm': confirmChart(); break;
      case 'alt': {
        const p = S.pending;
        try {
          const data = api('chart', { input: withPreset(p.input), viewYear: p.viewYear, alt: +el.dataset.i });
          S.req = { input: p.input, viewYear: p.viewYear, alt: +el.dataset.i };
          S.chart = data; S.sel = null; S.profileId = null;
          go('chart');
        } catch (err) { openModal('Không lập được lá số thay thế', err.message, 'Đóng', null, null); }
        break;
      }
      case 'alt-off': S.req.alt = null; reloadChart(false); break;
      case 'edit': {
        S.form = formFromInput(S.req.input);
        S.editingId = S.profileId; S.formError = '';
        go('form'); break;
      }
      case 'pick-hour': {
        const input = Object.assign({}, S.hoursInput, { timeMode: 'canh', canh: el.dataset.canh });
        S.form.timeMode = 'canh'; S.form.canh = el.dataset.canh;
        try {
          const vy = parseInt(S.form.viewYear, 10) || (S.today && S.today.lunarYear);
          const data = api('chart', { input: withPreset(input), viewYear: vy });
          S.pending = { input, data, viewYear: vy }; S.saveOnConfirm = true;
          go('confirm');
        } catch (err) { openModal('Không lập được lá số', err.message, 'Đóng', null, null); }
        break;
      }
      case 'preset': S.settings.preset = el.dataset.k; saveSettings(); render(false); break;
      case 'expert': S.settings.expert = !S.settings.expert; saveSettings(); render(false); break;
      case 'rd': {
        const k = el.dataset.key, open = !S.rdOpen[k];
        S.rdOpen[k] = open;
        el.classList.toggle('open', open);
        const t = el.querySelector('.rd-tap');
        if (t) t.textContent = open ? 'Thu gọn ‹' : 'Vì sao? ›';
        break;
      }
      case 'year': setYear(S.req.viewYear + parseInt(el.dataset.d, 10)); break;
      case 'goto-year': setYear(parseInt(el.dataset.y, 10)); break;
      case 'layer': S.layers[el.dataset.k] = !S.layers[el.dataset.k]; render(false); break;
      case 'month': S.month = +el.dataset.m; render(false); break;
      case 'month-card': S.layers.month = true; S.month = +el.dataset.m; render(false); { const b = $('#board'); if (b) b.scrollIntoView({ block: 'start' }); } break;
      case 'palace': {
        const chi = +el.dataset.chi;
        if (S.sel === chi) { openPalace(chi); } else { S.sel = chi; applySelection(); openPalace(chi); }
        break;
      }
      case 'close-sheet': closeSheet(); break;
      case 'modal-ok': { const f = modalAction; closeModal(); if (f) f(); break; }
      case 'modal-cancel': closeModal(); break;
    }
  });

  // Ô nhập: cập nhật trạng thái không dựng lại (giữ con trỏ); ô chọn thì dựng lại phần phụ thuộc.
  document.addEventListener('input', (e) => {
    const el = e.target;
    if (el.dataset.act === 'year-range') { const v = $('.yv b'); if (v) v.textContent = 'Năm ' + el.value; return; }
    const f = el.dataset.f;
    if (!f || !S.form) return;
    if (el.type === 'checkbox') return;
    S.form[f] = el.value;
    if (f === 'year') { if (/^\d{4}$/.test(el.value)) { if (S.form.calendar === 'lunar') { const pos = el.selectionStart; render(false); const n = $('[data-f="year"]'); if (n) { n.focus(); n.setSelectionRange(pos, pos); } } else updateConv(); } }
  });
  document.addEventListener('change', (e) => {
    const el = e.target;
    if (el.dataset.act === 'year-range') { setYear(parseInt(el.value, 10)); return; }
    const f = el.dataset.f;
    if (!f || !S.form) return;
    S.form[f] = el.type === 'checkbox' ? el.checked : el.value;
    if (el.tagName === 'SELECT' || el.type === 'checkbox') render(false); else updateConv();
  });

  // ———————————————————————————— khởi động ————————————————————————————
  function showFatal(msg) {
    const d = document.createElement('div');
    d.className = 'error-box';
    d.textContent = 'Lỗi giao diện: ' + msg + ' — hãy chụp màn hình này gửi lại để sửa.';
    document.body.insertBefore(d, document.body.firstChild);
  }
  window.addEventListener('error', (e) => showFatal(e.message || String(e)));

  function boot() {
    document.body.insertAdjacentHTML('beforeend', '<div class="scrim" id="scrim" data-act="close-sheet"></div><div class="sheet" id="sheet"></div><div class="modal" id="modal"></div>');
    loadStore();
    S.form = defaultForm();
    render(true);
    if (!S.settings.onboarded) {
      openModal('Lời ngỏ', ONBOARD, 'Tôi đã hiểu', () => { S.settings.onboarded = true; saveSettings(); }, null);
    }
    if (location.hash === '#selftest') { // chỉ có trong bản kiểm thử (selftest.js không có trong APK phát hành)
      const sc = document.createElement('script'); sc.src = 'selftest.js'; document.body.appendChild(sc);
    }
  }
  try { boot(); } catch (e) { showFatal(e.message); }
})();
