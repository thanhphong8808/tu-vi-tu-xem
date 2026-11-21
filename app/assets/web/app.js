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
    ['CUSTOM', 'Tùy chỉnh từng điểm', 'Tự chọn 12 điểm còn khác nhau giữa các trường phái (Tứ Hóa, Khôi Việt, Hỏa Linh, nguyệt hạn…).'],
  ];
  const COMMON_ZONES = ['America/Los_Angeles', 'America/New_York', 'America/Chicago', 'America/Toronto', 'Europe/Paris', 'Europe/Berlin',
    'Europe/London', 'Europe/Prague', 'Europe/Moscow', 'Australia/Sydney', 'Australia/Melbourne', 'Asia/Tokyo', 'Asia/Seoul',
    'Asia/Taipei', 'Asia/Shanghai', 'Asia/Bangkok', 'Asia/Singapore', 'Asia/Kuala_Lumpur'];
  // Không rõ giờ sinh: các sự kiện đã qua để xếp hạng 12 giờ (mục 2.3).
  const EVENTS_Q = [
    ['CUOI_HOI', 'Năm kết hôn (hoặc về chung một nhà)'],
    ['SINH_CON', 'Năm sinh con đầu lòng'],
    ['THANG_TIEN', 'Năm được thăng chức, đỗ đạt hay có bước tiến lớn'],
    ['DOI_VIEC', 'Năm đổi việc, đổi nghề'],
    ['DUOC_TAI', 'Năm có khoản thu lớn'],
    ['HAO_TAI', 'Năm hao tài, mất mát tiền bạc lớn'],
    ['DI_CHUYEN', 'Năm chuyển nhà hoặc đi xa lập nghiệp'],
  ];
  const DOW = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
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
    settings: { preset: 'NAM_PHAI_VN', onboarded: false, expert: false, config: {} },
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
    rv: null, // màn luận giải: {scope, year, month, data, err}
    readingCache: {},
    palOpen: {},
    cmp: { a: '', b: '', rel: 'VO_CHONG', data: null, err: '', names: ['', ''] }, // so hai lá số
    ev: {}, // câu trả lời sự kiện khi không rõ giờ
    rank: null,
    rankErr: '',
    cfgOptions: null,
    zoom: 1,
    lpFired: false,
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
    if (!S.settings.config || typeof S.settings.config !== 'object') S.settings.config = {};
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
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') { window.tvBack(); return; }
    // Bàn phím / công tắc trợ năng: Enter hoặc phím cách trên phần tử chạm được = chạm.
    if ((e.key === 'Enter' || e.key === ' ') && e.target && e.target.matches && e.target.matches('div[data-act], span[data-act]')) { e.preventDefault(); e.target.click(); }
  });
  // Chạm giữ một sao trên lưới (0,55 giây) → thẻ sao (mục 13).
  let lp = null;
  const lpCancel = () => { if (lp) { clearTimeout(lp.t); lp = null; } };
  document.addEventListener('pointerdown', (e) => {
    S.lpFired = false;
    const st = e.target.closest && e.target.closest('#board [data-k]');
    if (!st) return;
    const k = st.dataset.k;
    lp = { x: e.clientX, y: e.clientY, t: setTimeout(() => { lp = null; S.lpFired = true; openStar(k, null); }, 550) };
  });
  document.addEventListener('pointerup', lpCancel);
  document.addEventListener('pointercancel', lpCancel);
  document.addEventListener('pointermove', (e) => { if (lp && Math.hypot(e.clientX - lp.x, e.clientY - lp.y) > 12) lpCancel(); });
  document.addEventListener('contextmenu', (e) => { if (e.target.closest && e.target.closest('#board')) e.preventDefault(); });

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
    else if (v === 'reading') html = viewReading();
    else if (v === 'compare') html = viewCompare();
    app.innerHTML = html;
    a11y(app);
    if (scrollTop) window.scrollTo(0, 0);
    if (v === 'chart') afterChart();
    if (v === 'form') afterForm();
  }

  // Trợ năng: mọi phần tử chạm được (div/span có data-act) là nút với bàn phím và TalkBack.
  function a11y(root) {
    root.querySelectorAll('div[data-act], span[data-act], dt[data-act]').forEach((el) => {
      if (!el.hasAttribute('role')) el.setAttribute('role', 'button');
      if (!el.hasAttribute('tabindex')) el.tabIndex = 0;
    });
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
        return `<div class="profile"><button type="button" class="pmain" data-act="open-profile" data-id="${esc(p.id)}">` +
          `<span class="av ${i.gender === 'NU' ? 'nu' : ''}" aria-hidden="true">${esc(nm.trim().charAt(0).toUpperCase() || '?')}</span>` +
          `<span class="meta"><b>${esc(nm)}</b><span>${i.gender === 'NU' ? 'Nữ' : 'Nam'} · ${esc(date)} · ${esc(time)}</span></span></button>` +
          `<button class="icon-btn" data-act="del-profile" data-id="${esc(p.id)}" aria-label="Xóa lá số ${esc(nm)}">✕</button></div>`;
      }).join('');
    }
    return `<div class="brand"><h1>Tử Vi Tự Xem</h1><p>Hôm nay ${esc(t.solar)} · Âm lịch ${esc(t.lunar)} · năm ${esc(t.lunarYearCanChi)}</p></div>` +
      `<button class="btn primary block" data-act="new">＋ Lập lá số mới</button>` +
      `<div class="card"><h2>Lá số đã lưu</h2>${list}</div>` +
      `<div class="row"><button class="btn" data-act="settings">⚙ Cài đặt</button>` +
      (S.profiles.length >= 2 ? `<button class="btn" data-act="compare">⚭ So hai lá số</button>` : '') + `</div>` +
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
  function segAct(act, items, value) {
    return `<div class="seg" role="radiogroup">` + items.map(([v, label]) =>
      `<button type="button" class="${value === v ? 'on' : ''}" data-act="${act}" data-v="${v}" role="radio" aria-checked="${value === v}">${label}</button>`).join('') + `</div>`;
  }

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
  const withPreset = (input) => Object.assign({}, input, { preset: S.settings.preset, expert: !!S.settings.expert },
    S.settings.preset === 'CUSTOM' ? { config: Object.assign({}, S.settings.config) } : {});

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
        S.ev = {}; S.rank = null; S.rankErr = '';
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
    const qa = EVENTS_Q.map(([k, q]) => `<div class="qa"><label for="q-${k}">${esc(q)}</label>` +
      `<input id="q-${k}" class="input" data-q="${k}" inputmode="numeric" maxlength="4" placeholder="Năm, vd. 2015 — bỏ trống nếu không có" value="${esc(S.ev[k] || '')}"></div>`).join('');
    const rank = S.rank ? `<div class="card" id="rank"><h2>Xếp hạng 12 giờ <span class="guess">ước đoán</span></h2>` +
      `<p class="small muted" style="margin-top:0">Mức khớp (thang 100) = điểm đồng thanh của các sự kiện bạn nhập (khả năng cao 3, có thể 2, mới là dấu hiệu 1) chia cho tối đa. Giờ đứng đầu chỉ là gợi ý — hãy đọc thêm chính tinh cung Mệnh rồi chạm giờ bạn chọn.</p><div class="hours">` +
      S.rank.map((h, i) => `<div class="h rk" data-act="pick-hour" data-canh="${h.canh}" aria-label="Hạng ${i + 1}: giờ ${esc(h.label)}, mức khớp ${h.score} trên 100, Mệnh ${esc(h.menh)}: ${esc(h.menhStars)}">` +
        `<b>${i + 1}. ${esc(h.label.split(' (')[0])}</b><div><div class="meter"><i style="width:${h.score}%"></i></div>` +
        `<div>Mức khớp <b class="pct">${h.score}</b>/100 · Mệnh ${esc(h.menh)}: ${esc(h.menhStars)}</div><div class="small muted">${esc(h.details.join(' · '))}</div></div></div>`).join('') +
      `</div></div>` : '';
    return bar('Không rõ giờ sinh', '12 lá số theo 12 canh giờ') +
      `<div class="card" id="hq"><h2>Ước đoán giờ theo sự kiện đã qua</h2>` +
      `<p class="small muted" style="margin-top:0">Nhập năm của những việc bạn nhớ rõ (cần ít nhất 2 câu). App dò cả 12 giờ xem giờ nào có dấu hiệu vận hạn khớp nhất. Việc xảy ra tháng 1–2 dương lịch có thể thuộc năm âm trước.</p>` +
      qa + (S.rankErr ? `<div class="error-box" id="rank-error">${esc(S.rankErr)}</div>` : '') +
      `<button class="btn primary block" data-act="rank-hours">Xếp hạng 12 giờ</button></div>` + rank +
      `<div class="card"><h2>Hoặc tự chọn theo cung Mệnh</h2><p class="small muted" style="margin-top:0">Đọc chính tinh cung Mệnh của từng giờ, chọn giờ bạn thấy khớp nhất với bản thân.</p>` +
      `<div class="hours">${rows}</div></div>`;
  }
  function rankHours() {
    const inp = S.hoursInput;
    const nowY = S.today && S.today.year ? S.today.year : new Date().getFullYear();
    const events = [];
    for (const [k, q] of EVENTS_Q) {
      const v = String(S.ev[k] || '').trim();
      if (!v) continue;
      const y = parseInt(v, 10);
      if (!/^\d{4}$/.test(v) || y < inp.year || y > nowY) { S.rankErr = `“${q}”: năm phải từ ${inp.year} đến ${nowY}.`; S.rank = null; return; }
      events.push({ event: k, year: y });
    }
    if (events.length < 2) { S.rankErr = 'Cần trả lời ít nhất 2 câu để xếp hạng.'; S.rank = null; return; }
    try {
      S.rank = api('rankHours', { input: withPreset(inp), events });
      S.rankErr = '';
    } catch (e) { S.rankErr = e.message; S.rank = null; }
  }

  // ——— Cài đặt ———
  function viewSettings() {
    const ver = window.TuViNative && window.TuViNative.version ? window.TuViNative.version() : 'bản thử trên máy tính';
    return bar('Cài đặt') +
      `<div class="card"><h2>Trường phái an sao</h2>` +
      PRESETS.map(([k, n, d]) => `<div class="opt ${S.settings.preset === k ? 'on' : ''}" data-act="preset" data-k="${k}" aria-pressed="${S.settings.preset === k}"><b>${esc(n)}</b><span>${esc(d)}</span></div>`).join('') +
      customConfig() +
      `<p class="small muted">Áp dụng cho các lá số mở sau khi đổi.</p></div>` +
      `<div class="card"><h2>Luận giải</h2><div class="opt ${S.settings.expert ? 'on' : ''}" data-act="expert"><b>Chế độ chuyên sâu ${S.settings.expert ? '· đang bật' : '· đang tắt'}</b>` +
      `<span>Hiện thêm các luật độ tin cậy thấp (C), còn nhiều tranh luận giữa các trường phái.</span></div>` +
      `<p class="small muted">Áp dụng cho các lá số mở sau khi đổi.</p></div>` +
      `<div class="card"><h2>Về ứng dụng</h2><p class="small">Tử Vi Tự Xem ${esc(ver)} — giai đoạn 5: lá số, vận hạn; luận giải trọn đời / đại vận / năm / tháng / ngày (lịch tháng có điểm từng ngày, giờ thuận) với điểm 0–100, thế Tấn/Giữ/Thủ/Lùi, Nên/Không nên, “Vì sao?” cho mọi đoạn; thẻ sao (chạm giữ một sao); so hai lá số (truyền cung, truyền tinh); ước đoán giờ sinh theo sự kiện; tùy chỉnh trường phái từng điểm; xuất ảnh PNG và PDF A4. Câu luận hiện là bản nháp tự sinh, chưa qua thẩm định.</p>` +
      `<p class="small muted">${esc(ONBOARD)}</p></div>`;
  }

  function customConfig() {
    if (S.settings.preset !== 'CUSTOM') return '';
    if (!S.cfgOptions) S.cfgOptions = safe(() => api('configOptions'), []);
    const c = S.settings.config;
    return `<div class="cfg" id="cfg">` + S.cfgOptions.map((f) => `<div class="field"><label for="cfg-${f.key}">${esc(f.label)}</label>` +
      `<select id="cfg-${f.key}" class="input" data-cfg="${f.key}">` + f.options.map((o) =>
        `<option value="${esc(o.value)}"${(c[f.key] || f.default) === o.value ? ' selected' : ''}>${esc(o.label)}${o.value === f.default ? ' (mặc định)' : ''}</option>`).join('') +
      `</select></div>`).join('') + `</div>`;
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
      `<div class="chart-2p"><div class="cl-board">` +
      `<div class="board-wrap${S.zoom > 1 ? ' zoomed' : ''}" id="bwrap"><div class="board-sizer" id="bsizer"><div class="board" id="board" role="group" aria-label="Lưới 12 cung">${boardCells(d)}${centerBox(d)}<svg class="overlay" id="overlay" aria-hidden="true"></svg></div></div></div>` +
      `<div class="zoombar"><span class="small muted">Chụm hai ngón để phóng to lưới</span>` +
      `<button class="icon-btn" data-act="zoom" data-z="-1" aria-label="Thu nhỏ lưới">−</button><span class="zv" aria-live="polite">${zoomLabel()}</span>` +
      `<button class="icon-btn" data-act="zoom" data-z="1" aria-label="Phóng to lưới">＋</button><button class="icon-btn" data-act="zoom" data-z="0" aria-label="Về cỡ gốc">⟲</button></div>` +
      `<div class="legend"><span><i>M</i> Miếu · <i>V</i> Vượng · <i>Đ</i> Đắc · <i>B</i> Bình · <i>H</i> Hãm</span>` +
      `<span><i class="e-kim">Kim</i> · <i class="e-moc">Mộc</i> · <i class="e-thuy">Thủy</i> · <i class="e-hoa">Hỏa</i> · <i class="e-tho">Thổ</i></span>` +
      `<span><i style="color:var(--luu)">L.</i> sao lưu năm · chạm một cung để xem chi tiết · chạm giữ một sao để xem thẻ sao</span></div>` +
      `</div><div class="cl-side">` +
      luanCard(d) + exportCard(d) + readingCard(d) + yearCard(d) + daiHanCard(d) + monthCard(d) +
      `</div></div>` +
      `<p class="note">Luận giải đang là bản nháp tự sinh, chưa thẩm định. Tử Vi là bản đồ thời tiết — đức năng thắng số, lựa chọn của bạn quyết định kết quả.</p>`;
  }

  function starSpan(s, cls) {
    const b = s.b ? `<span class="bri">${esc(s.b)}</span>` : '';
    return `<span class="${cls || 'st'}${s.e ? ' e-' + s.e : ''}" data-k="${esc(s.k)}">${esc(s.n)}${b}</span>`;
  }
  // Nhãn TalkBack của một ô: tên cung, can chi, chính tinh (độ sáng), Tứ Hóa, phụ tinh — đúng thứ tự mục 13.
  function cellLabel(p) {
    const nm = (s) => s.n + (s.b ? ' ' + (BRI[s.b] || s.b) : '');
    const main = p.stars.filter((s) => s.s === 'main');
    const hoa = p.stars.filter((s) => s.s === 'hoa');
    const minor = p.stars.filter((s) => s.s === 'good' || s.s === 'bad' || s.s === 'ring');
    return `Cung ${p.role}${p.isThan ? ', Thân cư' : ''}, ${p.can} ${p.chiVn}. ` +
      `Chính tinh: ${main.length ? main.map(nm).join(', ') : 'vô chính diệu'}. ` +
      (hoa.length ? `Tứ Hóa: ${hoa.map((s) => 'Hóa ' + HOA_SHORT[s.k]).join(', ')}. ` : '') +
      `Phụ tinh: ${minor.length ? minor.map(nm).join(', ') : 'không có'}. Đại hạn ${p.daiHan} tuổi.`;
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
        `role="button" tabindex="0" aria-label="${esc(cellLabel(p))}"><div class="c-in" aria-hidden="true">` +
        `<div class="c-top"><span class="c-cc">${esc(p.can)} ${esc(p.chiVn)}</span><span class="c-dh${isCurDh ? ' cur' : ''}">${p.daiHan}</span></div>` +
        `<div class="c-role">${esc(p.role)}${p.isThan ? '<span class="than-tag">THÂN</span>' : ''}</div>` +
        (o.marks.length ? `<div class="c-marks">${o.marks.map(([k, t]) => `<i class="mk ${k}">${t}</i>`).join('')}</div>` : '') +
        `<div class="c-main">${main.length ? main.map((s) => `<div>${starSpan(s, 'ms')}</div>`).join('') : '<span class="vcd">Vô chính diệu</span>'}</div>` +
        (hoa.length || o.hoa.length ? `<div class="c-hoa">${hoa.map((s) => `<span class="hb ${HOA_KEY[s.k]}">${HOA_SHORT[s.k]}</span>`).join('')}` +
          `${o.hoa.map((h) => `<span class="hb l ${h.k}">${esc(h.n)}</span>`).join('')}</div>` : '') +
        `<div class="c-minor"><div class="grp good">${good.map((s) => starSpan(s)).join(' ')}</div><div class="grp bad">${bad.map((s) => starSpan(s)).join(' ')}</div></div>` +
        (o.luu.length ? `<div class="c-luu">${o.luu.map((s) => `<span class="st">${esc(s.n)}</span>`).join(' ')}</div>` : '') +
        `<div class="c-foot"><span>${ring.map((s) => esc(s.n)).join(' ')}</span><span class="th-chi">${esc(p.tieuHan)}</span></div>` +
        `</div></div>`;
    }).join('');
  }
  function centerBox(d) {
    const i = d.info;
    const row = (k, v) => `<div class="ct-row"><span>${esc(k)}</span><b>${esc(v)}</b></div>`;
    return `<div class="center" role="note" aria-label="Thông tin lá số">` +
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

  // ——— Luận giải (giai đoạn 4): trọn đời / đại vận / năm / tháng ———
  const SCOPES = [['life', 'Trọn đời'], ['decade', 'Đại vận'], ['year', 'Năm'], ['month', 'Tháng'], ['day', 'Ngày']];
  const STANCE_CLS = { TAN: 'st-tan', GIU: 'st-giu', THU: 'st-thu', LUI: 'st-lui' };
  const bandCls = (s) => (s >= 55 ? 'b-good' : s <= 45 ? 'b-bad' : 'b-mid');
  function luanCard(d) {
    const y = d.year.lunarYear;
    return `<div class="card" id="luan"><h2>Luận giải</h2><div class="rbtns">` +
      SCOPES.map(([k, l]) => `<button class="btn" data-act="open-reading" data-s="${k}">${k === 'year' ? 'Năm ' + y : l}</button>`).join('') +
      `</div><p class="small muted" style="margin:8px 0 0">Điểm 0–100 (50 là mức trung bình), thế Tấn/Giữ/Thủ/Lùi, luận theo chủ đề, Nên/Không nên, “Vì sao?” cho mọi đoạn; Ngày: lịch tháng có điểm từng ngày và giờ thuận. Câu luận đang là bản nháp.</p></div>`;
  }
  const cacheBase = () => [S.req && S.req.input, S.req && S.req.alt, S.settings.preset, S.settings.preset === 'CUSTOM' ? S.settings.config : null, !!S.settings.expert];
  function readingKey(rv) {
    if (rv.scope === 'day') return JSON.stringify(cacheBase().concat(['day', rv.date]));
    return JSON.stringify(cacheBase().concat([rv.scope, rv.scope === 'life' ? 0 : rv.year, rv.scope === 'month' ? rv.month : 0]));
  }
  // Tầng ngày (mục 2.3): lịch tháng dương lịch có điểm từng ngày + chi tiết ngày đang chọn.
  function loadDay() {
    const rv = S.rv;
    const dk = JSON.stringify(cacheBase().concat(['days', rv.dy, rv.dm]));
    try {
      if (!S.readingCache[dk]) S.readingCache[dk] = api('days', { input: withPreset(S.req.input), alt: S.req.alt, year: rv.dy, month: rv.dm });
      rv.days = S.readingCache[dk];
      const key = readingKey(rv);
      if (!S.readingCache[key]) S.readingCache[key] = api('day', { input: withPreset(S.req.input), alt: S.req.alt, date: rv.date });
      rv.data = S.readingCache[key]; rv.err = '';
    } catch (e) { rv.data = null; rv.err = e.message; }
  }
  const isoOf = (y, m, d) => `${y}-${m < 10 ? '0' : ''}${m}-${d < 10 ? '0' : ''}${d}`;
  function loadReading() {
    const rv = S.rv;
    if (rv.scope === 'day') { loadDay(); return; }
    const key = readingKey(rv);
    if (S.readingCache[key]) { rv.data = S.readingCache[key]; rv.err = ''; return; }
    try {
      rv.data = api('reading', { input: withPreset(S.req.input), viewYear: rv.year, alt: S.req.alt, scope: rv.scope, month: rv.month });
      rv.err = '';
      S.readingCache[key] = rv.data;
    } catch (e) { rv.data = null; rv.err = e.message; }
  }
  function openReading(scope, year, month) {
    const prev = S.rv || {};
    const todayMonth = S.today && S.today.lunarYear === S.req.viewYear ? S.today.lunarMonth : 1;
    S.rv = { scope: scope, year: year || S.req.viewYear, month: month || prev.month || todayMonth || 1, data: null, err: '',
      dy: prev.dy, dm: prev.dm, date: prev.date, days: null };
    if (scope === 'day' && !S.rv.date) {
      const iso = (S.today && S.today.iso) || new Date().toISOString().slice(0, 10);
      S.rv.date = iso; S.rv.dy = +iso.slice(0, 4); S.rv.dm = +iso.slice(5, 7);
    }
    loadReading();
    if (top() === 'reading') render(true); else go('reading');
  }
  const KIND_ICON = { LEAD: '', PLAIN: '', WEIGH: '⚖ ', EVENT: '◆ ', NOTE: '' };
  function rPara(p, key) {
    const open = !!S.rdOpen[key];
    const why = p.why.map((w) => `<div class="w"><b>${esc(w.title)}</b>${w.detail ? `<div>${esc(w.detail)}</div>` : ''}` +
      (w.evidence.length ? `<ul>${w.evidence.map((e) => `<li>${esc(e)}</li>`).join('')}</ul>` : '') +
      (w.source ? `<small>Nguồn: ${esc(w.source)}</small>` : '') + `</div>`).join('');
    return `<div class="rp k-${p.kind} ${polCls(p.pol)}${open ? ' open' : ''}" data-act="rp" data-key="${esc(key)}">` +
      `<div class="rp-t">${KIND_ICON[p.kind] || ''}${esc(p.text)}${p.draft ? ' <span class="draft-tag">nháp</span>' : ''}</div>` +
      `<div class="rp-why">${why}<small class="ids">Mã: ${esc(p.ids.join(', '))}</small></div>` +
      `<div class="rp-tap">${open ? 'Thu gọn ‹' : 'Vì sao? ›'}</div></div>`;
  }
  function rSection(s, prefix, collapsible) {
    const open = !collapsible || !!S.palOpen[prefix + s.id];
    const head = `<div class="rs-h"${collapsible ? ` data-act="pal" data-id="${esc(prefix + s.id)}"` : ''}><b>${esc(s.title)}</b>` +
      (s.score != null ? `<span class="sc ${bandCls(s.score)}">${s.score}</span>` : '') + (collapsible ? `<span class="chev">${open ? '▾' : '▸'}</span>` : '') + `</div>` +
      (s.subtitle ? `<div class="small muted">${esc(s.subtitle)}</div>` : '');
    return `<div class="card rs" id="rs-${esc(s.id)}">${head}${open ? s.paragraphs.map((p, i) => rPara(p, prefix + s.id + ':' + i)).join('') : ''}</div>`;
  }
  function viewReading() {
    const rv = S.rv;
    if (!rv || !S.chart) return viewHome();
    const info = S.chart.info;
    const tabs = `<div class="chips rtabs">` + SCOPES.map(([k, l]) => `<button class="chip ${rv.scope === k ? 'on' : ''}" data-act="rscope" data-s="${k}">${l}</button>`).join('') + `</div>`;
    const yearBar = rv.scope === 'day' ? `<div class="yearbar"><button class="icon-btn" data-act="rmon" data-d="-1" aria-label="Tháng trước">◀</button>` +
      `<div class="yv"><b>Tháng ${rv.dm}/${rv.dy}</b><span>dương lịch · chạm một ngày để xem chi tiết</span></div>` +
      `<button class="icon-btn" data-act="rmon" data-d="1" aria-label="Tháng sau">▶</button></div>` + calendarCard(rv)
      : rv.scope === 'life' ? '' : `<div class="yearbar"><button class="icon-btn" data-act="ryear" data-d="-1" aria-label="Năm trước">◀</button>` +
      `<div class="yv"><b>Năm ${rv.year}</b><span>${rv.scope === 'decade' ? 'đại vận chứa năm này' : rv.scope === 'month' ? 'chọn tháng âm bên dưới' : 'năm âm lịch'}</span></div>` +
      `<button class="icon-btn" data-act="ryear" data-d="1" aria-label="Năm sau">▶</button></div>`;
    const monthChips = rv.scope === 'month' ? `<div class="chips months">${Array.from({ length: 12 }, (_, i) => i + 1).map((m) =>
      `<button class="chip ${rv.month === m ? 'on' : ''}" data-act="rmonth" data-m="${m}">T${m}</button>`).join('')}</div>` : '';
    let body;
    const r = rv.data;
    if (!r) body = `<div class="card"><p>Chưa lập được bản luận: ${esc(rv.err || 'lỗi không rõ')}</p></div>`;
    else {
      const sum = `<div class="card rsum"><div class="rsum-h"><div class="big ${bandCls(r.score)}">${r.score}<small>/100</small></div>` +
        `<div><div class="rt">${esc(r.title)}</div><div class="small muted">${esc(r.subtitle)}</div><span class="stance ${STANCE_CLS[r.stance] || ''}">Thế ${esc(r.stanceVn)}</span></div></div>` +
        r.summary.map((p, i) => rPara(p, 'sum:' + i)).join('') + `</div>`;
      const heat = r.months && r.months.length && r.scope !== 'THANG' ? `<div class="card"><h2>12 tháng âm năm ${rv.year}</h2><div class="heat">` +
        r.months.map((m) => `<div class="hm ${bandCls(m.score)}${r.goodMonths.indexOf(m.month) >= 0 ? ' good' : ''}${r.cautionMonths.indexOf(m.month) >= 0 ? ' caution' : ''}" data-act="hm" data-m="${m.month}">` +
          `<span>T${m.month}</span><b>${m.score}</b><small>${esc(m.role || '')}</small></div>`).join('') + `</div>` +
        (r.goodMonths.length ? `<p class="small" style="margin:6px 0 0">Tháng thuận: <b>${r.goodMonths.join(', ')}</b> · tháng nên thận trọng: <b>${r.cautionMonths.join(', ')}</b> (âm lịch). Chạm một tháng để xem chi tiết.</p>` : '') + `</div>` : '';
      const tl = r.timeline.length ? `<div class="card"><h2>12 đại vận</h2><div class="tl">` + r.timeline.map((t) =>
        `<div class="dh ${t.current ? 'cur' : ''}" data-act="rdecade" data-y="${t.startYear}"><b>${t.from}–${t.to}</b><span>${esc(t.chiVn)} · ${esc(t.role)}</span>` +
        `<span><span class="sc ${bandCls(t.score)}">${t.score}</span> ${esc(t.stance)}</span><span>${esc(t.thienThoi)}</span></div>`).join('') + `</div></div>` : '';
      const adv = r.advice.length ? `<div class="card radv"><h2>Nên / Không nên</h2>` + [['NEN', 'Nên'], ['KHONG_NEN', 'Không nên'], ['LUU_Y', 'Lưu ý']].map(([k, l]) => {
        const items = r.advice.filter((a) => a.type === k);
        return items.length ? `<h3 class="adv-${k}">${l}</h3><ul>${items.map((a) => `<li>${esc(a.text)}<small> — vì: ${esc(a.because)}</small></li>`).join('')}</ul>` : '';
      }).join('') + `</div>` : '';
      const pal = r.palaces.length ? `<h2 class="rh">12 cung</h2>` + r.palaces.map((s) => rSection(s, 'p:', true)).join('') : '';
      body = sum + hoursCard(r) + heat + `<div class="rcols">` + r.sections.map((s) => rSection(s, 't:', false)).join('') + `</div>` + tl + pal + adv +
        `<p class="note">${esc(r.closing)}</p>`;
    }
    return bar('Luận giải', info.name || 'Lá số') + tabs + yearBar + monthChips + body;
  }

  function calendarCard(rv) {
    const days = rv.days || [];
    if (!days.length) return '';
    const lead = days[0].dow - 1;
    const cells = Array.from({ length: lead }, () => '<div class="cd blank" aria-hidden="true"></div>').join('') + days.map((d) => {
      const lunar = d.lunarDay === 1 ? `${d.lunarDay}/${d.lunarMonth}${d.lunarLeap ? 'n' : ''}` : String(d.lunarDay);
      return `<div class="cd ${bandCls(d.score)}${d.date === rv.date ? ' sel' : ''}${S.today && d.date === S.today.iso ? ' today' : ''}" data-act="rday" data-d="${d.date}" ` +
        `aria-label="Ngày ${d.day} tháng ${rv.dm}, âm lịch ${d.lunarDay}/${d.lunarMonth}${d.lunarLeap ? ' nhuận' : ''}, ngày ${esc(d.canChi)}, ${d.score} điểm${d.date === rv.date ? ', đang chọn' : ''}">` +
        `<b>${d.day}</b><small>${lunar}</small><i>${d.score}</i></div>`;
    }).join('');
    return `<div class="card" id="cal"><h2>Lịch tháng ${rv.dm}/${rv.dy}</h2><div class="cal">${DOW.map((w) => `<div class="dw" aria-hidden="true">${w}</div>`).join('')}${cells}</div>` +
      `<p class="small muted" style="margin:6px 0 0"><span class="lg b-good"></span> thuận (từ 55) · <span class="lg b-mid"></span> trung bình · <span class="lg b-bad"></span> nên thận trọng (từ 45 trở xuống). Số nhỏ là ngày âm lịch.</p>` +
      `<div class="lowconf">Độ tin cậy thấp hơn tầng năm — chỉ dùng để tham khảo khi sắp xếp việc.</div></div>`;
  }
  function hoursCard(r) {
    if (!r.hours || !r.hours.length) return '';
    const best = r.hours.slice().sort((a, b) => b.score - a.score).slice(0, 3).map((h) => h.chi);
    return `<div class="card" id="hrs"><h2>12 giờ trong ngày</h2><div class="hrs" role="list">` + r.hours.map((h) => {
      const [nm, tm] = h.label.split(' (');
      return `<div class="hr ${bandCls(h.score)}${best.indexOf(h.chi) >= 0 ? ' best' : ''}" role="listitem" aria-label="Giờ ${esc(h.label)}, ${h.score} điểm, cung ${esc(h.role)}${best.indexOf(h.chi) >= 0 ? ', giờ thuận' : ''}">` +
        `<b>${esc(nm)}</b><small>${esc((tm || '').replace(')', ''))}</small><i>${h.score}</i><small>${esc(h.role)}</small></div>`;
    }).join('') + `</div><p class="small muted" style="margin:6px 0 0">Viền đậm là 3 giờ thuận nhất. Giờ tính theo cung thời hạn (từ cung nhật hạn coi là giờ Tý); độ tin cậy thấp hơn tầng ngày.</p></div>`;
  }

  // ——— So hai lá số (cẩm nang P.V.4): truyền cung, truyền tinh ———
  function viewCompare() {
    const c = S.cmp;
    const ps = S.profiles.filter((p) => p.input && p.input.timeMode !== 'unknown');
    const opt = (sel) => `<option value="">— Chọn lá số —</option>` + ps.map((p) =>
      `<option value="${esc(p.id)}"${sel === p.id ? ' selected' : ''}>${esc(p.name || 'Không tên')} · ${p.input.gender === 'NU' ? 'Nữ' : 'Nam'} ${esc(p.input.year)}</option>`).join('');
    const vc = c.rel === 'VO_CHONG';
    let res = '';
    if (c.err) res = `<div class="error-box" id="cmp-error">${esc(c.err)}</div>`;
    else if (c.data) {
      const r = c.data;
      res = `<div class="card" id="cmp-res"><h2>${esc(r.title)}</h2>` +
        `<dl class="kv"><dt>${esc(c.names[0])}</dt><dd>Mệnh ${esc(r.a.menh)} · ${vc ? 'Phu Thê ' + esc(r.a.phuThe) : 'Tử Tức ' + esc(r.a.tuTuc)}</dd>` +
        `<dt>${esc(c.names[1])}</dt><dd>Mệnh ${esc(r.b.menh)}${vc ? ' · Phu Thê ' + esc(r.b.phuThe) : ''}</dd></dl>` +
        r.paragraphs.map((p, i) => rPara(p, 'cmp:' + i)).join('') + `<p class="note">${esc(r.closing)}</p></div>`;
    }
    return bar('So hai lá số', 'Truyền cung · truyền tinh') + `<div class="card">` +
      `<div class="field"><span class="label">Quan hệ</span>${segAct('cmp-rel', [['VO_CHONG', 'Vợ chồng'], ['CHA_ME_CON', 'Cha mẹ – con']], c.rel)}</div>` +
      `<div class="field"><label for="cmp-a">${vc ? 'Người thứ nhất' : 'Cha hoặc mẹ'}</label><select id="cmp-a" class="input" data-cmp="a">${opt(c.a)}</select></div>` +
      `<div class="field"><label for="cmp-b">${vc ? 'Người thứ hai' : 'Con'}</label><select id="cmp-b" class="input" data-cmp="b">${opt(c.b)}</select></div>` +
      `<button class="btn primary block" data-act="cmp-run">So sánh</button>` +
      `<p class="small muted">Theo cẩm nang P.V.4: cung ${vc ? 'Phu Thê' : 'Tử Tức'} của ${vc ? 'người này' : 'cha mẹ'} trùng cung Mệnh của ${vc ? 'người kia' : 'con'} là truyền cung; cùng bộ chính tinh là truyền tinh. Chỉ là một yếu tố tham khảo.</p></div>` + res;
  }
  function runCompare() {
    const c = S.cmp;
    const pa = S.profiles.find((p) => p.id === c.a), pb = S.profiles.find((p) => p.id === c.b);
    c.data = null;
    if (!pa || !pb) { c.err = 'Chọn đủ hai lá số.'; return; }
    if (pa.id === pb.id) { c.err = 'Chọn hai lá số khác nhau.'; return; }
    c.names = [pa.name || 'Người thứ nhất', pb.name || 'Người thứ hai'];
    try {
      c.data = api('compare', { a: withPreset(Object.assign({}, pa.input, { name: c.names[0] })), b: withPreset(Object.assign({}, pb.input, { name: c.names[1] })), relation: c.rel });
      c.err = '';
    } catch (e) { c.err = e.message; }
  }

  // ——— Xuất ảnh PNG, PDF A4 (mục 2.4) ———
  function exportCard() {
    return `<div class="card" id="export"><h2>Lưu, chia sẻ lá số</h2><div class="xbtns">` +
      `<button class="btn" data-act="export" data-k="png" data-a="save">🖼 Lưu ảnh</button>` +
      `<button class="btn" data-act="export" data-k="pdf" data-a="save">📄 Lưu PDF</button>` +
      `<button class="btn" data-act="export" data-k="png" data-a="share">↗ Gửi ảnh</button>` +
      `<button class="btn" data-act="export" data-k="pdf" data-a="share">↗ Gửi PDF</button></div>` +
      `<p class="small muted" style="margin:8px 0 0">PNG: ảnh lưới 12 cung. PDF khổ A4: trang 1 là lá số, các trang sau là luận giải trọn đời và năm đang xem; chân trang ghi trường phái và lời ngỏ.</p></div>`;
  }
  const PNG_E = { kim: '#5f6672', moc: '#2e7d32', thuy: '#1d3f9e', hoa: '#b71c1c', tho: '#8a5a00' };
  const PNG_H = { LOC: '#2e7d32', QUYEN: '#8e24aa', KHOA: '#1565c0', KY: '#424242' };
  // Vẽ lá số lên canvas (luôn nền sáng để in), không phụ thuộc kích thước màn hình.
  function drawChartPng(d) {
    const CW = 300, CH = 344, PAD = 28, HEAD = 78, FOOT = 50;
    const W = CW * 4 + PAD * 2, H = HEAD + CH * 4 + FOOT;
    const cv = document.createElement('canvas'); cv.width = W; cv.height = H;
    const g = cv.getContext('2d');
    const F = (w, px) => `${w} ${px}px Roboto, "Noto Sans", system-ui, sans-serif`;
    const i = d.info, y = d.year, X0 = PAD, Y0 = HEAD;
    const text = (t, x, yy, font, color, align) => { g.font = font; g.fillStyle = color; g.textAlign = align || 'left'; g.fillText(t, x, yy); };
    const fit = (t, font, maxW) => { g.font = font; if (g.measureText(t).width <= maxW) return t; let s = t; while (s.length > 1 && g.measureText(s + '…').width > maxW) s = s.slice(0, -1); return s + '…'; };
    const pill = (t, x, yy, font, bg, fg, align) => {
      g.font = font; const w = g.measureText(t).width + 12; const x0 = align === 'right' ? x - w : align === 'center' ? x - w / 2 : x;
      g.fillStyle = bg; g.beginPath(); if (g.roundRect) g.roundRect(x0, yy - 17, w, 22, 6); else g.rect(x0, yy - 17, w, 22); g.fill();
      text(t, x0 + 6, yy, font, fg); return w;
    };
    g.fillStyle = '#fffdf8'; g.fillRect(0, 0, W, H);
    g.textBaseline = 'alphabetic';
    text(fit('Lá số Tử Vi' + (i.name ? ' — ' + i.name : ''), F(800, 34), W / 2), PAD, 50, F(800, 34), '#6e1423');
    text(fit(`${i.gender} · ${i.solar}${i.clock ? ' ' + i.clock.split(' (')[0] : ''} · âm lịch ${i.lunar} · ${i.yearCanChi}`, F(400, 19), W / 2 - PAD), W - PAD, 50, F(400, 19), '#6b6358', 'right');
    const marks = {};
    const mk = (chi, t, col) => { (marks[chi] = marks[chi] || []).push([t, col]); };
    if (!y.beforeFirstDaiHan) mk(y.daiHan, 'ĐH', '#6e1423');
    mk(y.tieuHan, 'TH', '#a35f00'); mk(y.luuNienDaiHan, 'LN', '#5b4bb7');
    d.palaces.forEach((p) => {
      const [r, c] = POS[p.chi], x = X0 + c * CW, yy = Y0 + r * CH;
      g.strokeStyle = '#dccfb8'; g.lineWidth = 1; g.strokeRect(x + .5, yy + .5, CW - 1, CH - 1);
      text(`${p.can} ${p.chiVn}`, x + 10, yy + 26, F(500, 16), '#6b6358');
      text(String(p.daiHan), x + CW - 10, yy + 27, F(800, 21), '#6e1423', 'right');
      let rx = x + 10 + (text(p.role.toUpperCase(), x + 10, yy + 54, F(800, 20), '#6e1423'), g.measureText(p.role.toUpperCase()).width) + 6;
      if (p.isThan) rx += pill('THÂN', rx, yy + 53, F(800, 13), '#9a6b12', '#fff') + 4;
      (marks[p.chi] || []).forEach(([t, col]) => { rx += pill(t, rx, yy + 53, F(800, 13), col, '#fff') + 3; });
      let cy = yy + 88;
      const main = p.stars.filter((s) => s.s === 'main');
      if (!main.length) { text('Vô chính diệu', x + 10, cy, 'italic ' + F(400, 18), '#6b6358'); cy += 28; }
      main.forEach((s) => {
        g.font = F(800, 25); const w = g.measureText(s.n).width;
        text(s.n, x + 10, cy, F(800, 25), PNG_E[s.e] || '#1f1b16');
        if (s.b) text(s.b, x + 14 + w, cy, F(700, 16), PNG_E[s.e] || '#1f1b16');
        cy += 30;
      });
      const hoa = p.stars.filter((s) => s.s === 'hoa');
      if (hoa.length) { let hx = x + 10; hoa.forEach((s) => { hx += pill('Hóa ' + HOA_SHORT[s.k], hx, cy - 2, F(800, 14), PNG_H[HOA_KEY[s.k]], '#fff') + 4; }); cy += 26; }
      const good = p.stars.filter((s) => s.s === 'good'), bad = p.stars.filter((s) => s.s === 'bad');
      const lh = 20, maxLines = Math.max(1, Math.floor((yy + CH - 34 - cy) / lh) + 1);
      [[good, x + 10, '#1f1b16', ''], [bad, x + CW / 2 + 4, '#5a3a32', 'italic ']].forEach(([arr, cx, col, st]) => {
        arr.slice(0, arr.length > maxLines ? maxLines - 1 : maxLines).forEach((s, k) => {
          text(fit(s.n + (s.b ? ' ' + s.b : ''), st + F(400, 16), CW / 2 - 16), cx, cy + k * lh, st + F(400, 16), PNG_E[s.e] || col);
        });
        if (arr.length > maxLines) text(`+${arr.length - maxLines + 1} sao`, cx, cy + (maxLines - 1) * lh, F(600, 15), '#6b6358');
      });
      const ring = p.stars.filter((s) => s.s === 'ring').map((s) => s.n).join(' ');
      text(ring, x + 10, yy + CH - 10, F(400, 15), '#6b6358');
      text(String(p.tieuHan).toUpperCase(), x + CW - 10, yy + CH - 10, F(700, 15), '#6b6358', 'right');
    });
    // Ô trung tâm
    const cx0 = X0 + CW, cy0 = Y0 + CH;
    g.fillStyle = '#fbf6ea'; g.fillRect(cx0 + 1, cy0 + 1, CW * 2 - 2, CH * 2 - 2);
    text(fit(i.name || 'Lá số Tử Vi', F(800, 30), CW * 2 - 40), cx0 + CW, cy0 + 56, F(800, 30), '#6e1423', 'center');
    const rows = [['Dương lịch', `${i.solar}${i.clock ? ' ' + i.clock.split(' (')[0] : ''}`], ['Âm lịch', `${i.lunar}, giờ ${i.hourChi}`],
      ['Tứ trụ', `${i.yearCanChi} · ${i.monthCanChi} · ${i.dayCanChi} · ${i.hourCanChi}`], ['Âm Dương', `${i.amDuong} · ${i.thuanLy ? 'thuận lý' : 'nghịch lý'}`],
      ['Bản mệnh', i.banMenh], ['Cục', i.cuc], ['', i.cucMenh, 1], ['Mệnh chủ', `${i.menhChu} · Thân chủ ${i.thanChu}`],
      ['Năm xem', `${i.viewYear} ${i.viewYearCanChi} · ${i.tuoiAm} tuổi âm`], ['Trường phái', i.preset]];
    let ry = cy0 + 104;
    rows.forEach(([k, v, small]) => {
      if (small) { ry -= 16; text(fit(v, F(400, 17), CW * 2 - 200), cx0 + 170, ry, F(400, 17), '#6b6358'); ry += 34; return; }
      text(k, cx0 + 36, ry, F(400, 19), '#6b6358');
      text(fit(v, F(700, 21), CW * 2 - 200), cx0 + 170, ry, F(700, 21), '#1f1b16');
      ry += 50;
    });
    // Tuần, Triệt trên ranh giới hai cung
    const key = (a) => a.slice().sort((p, q) => p - q).join('-');
    const same = key(d.tuan) === key(d.triet);
    [[d.tuan, same ? 'TUẦN · TRIỆT' : 'TUẦN']].concat(same ? [] : [[d.triet, 'TRIỆT']]).forEach(([pr, label]) => {
      const [ra, ca] = POS[pr[0]], [rb, cb] = POS[pr[1]];
      const bx = ra === rb ? X0 + Math.max(ca, cb) * CW : X0 + ca * CW + CW / 2;
      const by = ra === rb ? Y0 + ra * CH + CH / 2 : Y0 + Math.max(ra, rb) * CH;
      pill(label, bx, by + 7, F(800, 14), '#222', '#fff', 'center');
    });
    g.strokeStyle = '#6e1423'; g.lineWidth = 3; g.strokeRect(X0, Y0, CW * 4, CH * 4);
    text(fit(`Tử Vi Tự Xem · ${i.preset} · lập ${S.today ? S.today.solar : ''} · tham khảo, chiêm nghiệm — đức năng thắng số`, F(400, 16), W - PAD * 2), W / 2, H - 18, F(400, 16), '#6b6358', 'center');
    return cv;
  }
  const slug = (t) => String(t || 'la-so').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').replace(/[^A-Za-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'la-so';
  function readingFor(scope, year) {
    const rv = { scope, year, month: 1 };
    const key = readingKey(rv);
    if (!S.readingCache[key]) S.readingCache[key] = api('reading', { input: withPreset(S.req.input), viewYear: year, alt: S.req.alt, scope, month: 1 });
    return S.readingCache[key];
  }
  // Mô hình tài liệu cho PDF (dựng trang ở phía Android bằng PdfDocument, khổ A4).
  function exportModel(d, pngBase64) {
    const i = d.info;
    const blocks = [];
    const add = (t, x, extra) => blocks.push(Object.assign({ t, x: String(x) }, extra || {}));
    const reading = (r, title) => {
      add('h1', title);
      add('sub', r.subtitle);
      add('score', `Điểm ${r.score}/100 · Thế ${r.stanceVn} — ${r.stanceText}`);
      r.summary.forEach((p) => add(p.kind === 'NOTE' ? 'note' : 'p', p.text, { pol: p.pol }));
      r.sections.forEach((sct) => { add('h2', sct.title + (sct.score != null ? ` · ${sct.score}/100` : '')); sct.paragraphs.forEach((p) => add(p.kind === 'NOTE' ? 'note' : 'p', p.text, { pol: p.pol })); });
      if (r.timeline.length) { add('h2', '12 đại vận'); r.timeline.forEach((t) => add('li', `${t.from}–${t.to} tuổi · ${t.chiVn} (cung ${t.role}) · ${t.score}/100 · ${t.stance} · ${t.thienThoi}${t.current ? ' · đang đi' : ''}`)); }
      if (r.palaces.length) r.palaces.forEach((sct) => { add('h2', sct.title + (sct.score != null ? ` · ${sct.score}/100` : '')); sct.paragraphs.forEach((p) => add(p.kind === 'NOTE' ? 'note' : 'p', p.text, { pol: p.pol })); });
      if (r.months.length) { add('h2', '12 tháng âm'); add('li', r.months.map((m) => `T${m.month}: ${m.score}`).join(' · ')); if (r.goodMonths.length) add('note', `Tháng thuận: ${r.goodMonths.join(', ')} · tháng nên thận trọng: ${r.cautionMonths.join(', ')} (âm lịch).`); }
      [['NEN', 'Nên'], ['KHONG_NEN', 'Không nên'], ['LUU_Y', 'Lưu ý']].forEach(([k, l]) => {
        const items = r.advice.filter((a) => a.type === k);
        if (items.length) { add('h2', l); items.forEach((a) => add('li', `${a.text} — vì: ${a.because}`)); }
      });
    };
    const life = readingFor('life', d.year.lunarYear), yr = readingFor('year', d.year.lunarYear);
    add('note', 'Câu luận là bản nháp tự sinh từ cẩm nang, chưa qua thẩm định — chỉ để tham khảo, chiêm nghiệm.');
    reading(life, 'Luận giải trọn đời');
    add('brk', '');
    reading(yr, yr.title);
    add('p', life.closing);
    const info = [['Họ tên', i.name || '—'], ['Giới tính', i.gender], ['Dương lịch', `${i.solar}${i.clock ? ' · ' + i.clock : ''}`], ['Âm lịch', `${i.lunar} · năm ${i.yearCanChi}`],
      ['Tứ trụ', `Năm ${i.yearCanChi} · Tháng ${i.monthCanChi} · Ngày ${i.dayCanChi} · Giờ ${i.hourCanChi}`], ['Âm Dương', `${i.amDuong} — ${i.thuanLy ? 'thuận lý' : 'nghịch lý'}`],
      ['Bản mệnh', `${i.banMenh} (${i.banMenhHanh})`], ['Cục', `${i.cuc} — ${i.cucMenh}`], ['Mệnh · Thân', `Mệnh tại ${i.menh} · Thân tại ${i.than} (cư ${i.thanRole})`],
      ['Nơi sinh', i.region], ['Trường phái', i.preset]];
    return { title: 'Lá số Tử Vi' + (i.name ? ' — ' + i.name : ''), footer: `Tử Vi Tự Xem · Trường phái: ${i.preset} · lập ngày ${S.today ? S.today.solar : ''}`,
      footer2: 'Lời ngỏ: ' + ONBOARD, image: pngBase64, info, blocks };
  }
  function nativeExport(kind, action, name, payload) {
    const r = JSON.parse(window.TuViNative.exportFile(kind, action, name, payload));
    if (!r.ok) throw new Error(r.error || 'Lỗi xuất tệp');
    return r;
  }
  function doExport(kind, action) {
    const d = S.chart;
    const base = `LaSo-${slug(d.info.name)}-${d.year.lunarYear}`;
    const png = drawChartPng(d).toDataURL('image/png');
    const native = window.TuViNative && window.TuViNative.exportFile;
    if (kind === 'png') {
      if (native) {
        const r = nativeExport('png', action, base + '.png', png.split(',')[1]);
        if (action === 'save') openModal('Đã lưu ảnh', `Đã lưu vào ${r.path}.`, 'Gửi ảnh', () => doExport('png', 'share'), 'Đóng');
        return r;
      }
      const a = document.createElement('a'); a.href = png; a.download = base + '.png'; document.body.appendChild(a); a.click(); a.remove();
      return { ok: true, path: base + '.png' };
    }
    const model = exportModel(d, png.split(',')[1]);
    if (native) {
      const r = nativeExport('pdf', action, base + '.pdf', JSON.stringify(model));
      if (action === 'save') openModal('Đã lưu PDF', `Đã lưu vào ${r.path} (${r.pages} trang A4).`, 'Gửi PDF', () => doExport('pdf', 'share'), 'Đóng');
      return r;
    }
    printFallback(model);
    return { ok: true, pages: 0 };
  }
  // Trình duyệt máy tính (máy chủ thử nghiệm): in ra PDF bằng hộp thoại in của trình duyệt.
  function printFallback(m) {
    const w = window.open('', '_blank');
    if (!w) return;
    const body = m.blocks.map((b) => b.t === 'brk' ? '<div style="break-before:page"></div>' : b.t === 'li' ? `<li>${esc(b.x)}</li>` :
      b.t === 'h1' ? `<h1>${esc(b.x)}</h1>` : b.t === 'h2' ? `<h2>${esc(b.x)}</h2>` : `<p class="${b.t}">${esc(b.x)}</p>`).join('');
    w.document.write(`<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${esc(m.title)}</title><style>@page{size:A4;margin:15mm}` +
      `body{font:10.5pt/1.45 sans-serif;color:#111}h1{color:#6e1423;font-size:16pt}h2{font-size:12pt;margin:12px 0 4px}.note,.sub{color:#555}` +
      `img{width:100%}.foot{font-size:8pt;color:#555;border-top:.5pt solid #999;margin-top:20px;padding-top:4px}</style></head><body>` +
      `<h1>${esc(m.title)}</h1><img src="data:image/png;base64,${m.image}"><table>${m.info.map(([k, v]) => `<tr><td>${esc(k)}</td><td><b>${esc(v)}</b></td></tr>`).join('')}</table>` +
      `<div style="break-before:page"></div>${body}<div class="foot">${esc(m.footer)}<br>${esc(m.footer2)}</div></body></html>`);
    w.document.close(); w.focus(); w.print();
  }
  window.tvExport = { png: () => drawChartPng(S.chart).toDataURL('image/png'), model: () => exportModel(S.chart, '') };

  // ——— Phóng to lưới (chụm hai ngón, nút ＋ −) ———
  const zoomLabel = () => (Math.round(S.zoom * 100) / 100).toString().replace('.', ',') + '×';
  function applyZoom() {
    const wrap = $('#bwrap'), sizer = $('#bsizer'), board = $('#board');
    if (!wrap || !board) return;
    const z = S.zoom;
    const zv = $('.zv'); if (zv) zv.textContent = zoomLabel();
    wrap.classList.toggle('zoomed', z > 1.001);
    if (z <= 1.001) { board.style.transform = ''; board.style.width = ''; sizer.style.width = ''; sizer.style.height = ''; return; }
    const baseW = wrap.clientWidth;
    board.style.width = baseW + 'px'; board.style.transformOrigin = '0 0'; board.style.transform = `scale(${z})`;
    sizer.style.width = baseW * z + 'px'; sizer.style.height = board.offsetHeight * z + 'px';
  }
  function setZoom(z, mx, my) {
    const wrap = $('#bwrap');
    if (!wrap) return;
    const old = S.zoom;
    z = Math.max(1, Math.min(3, z));
    if (Math.abs(z - old) < 0.001) return;
    const r = wrap.getBoundingClientRect();
    if (mx == null) { mx = r.width / 2; my = Math.min(r.height, window.innerHeight) / 2; }
    const cx = (wrap.scrollLeft + mx) / old, top = r.top + window.scrollY, cy = (window.scrollY + r.top + my - top) / old;
    S.zoom = z;
    applyZoom();
    wrap.scrollLeft = cx * z - mx;
    window.scrollTo(window.scrollX, top + cy * z - (r.top + my));
  }
  function attachPinch() {
    const wrap = $('#bwrap');
    if (!wrap) return;
    let pinch = null;
    const dist = (e) => Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
    wrap.addEventListener('touchstart', (e) => {
      if (e.touches.length !== 2) return;
      pinch = { d: dist(e), z: S.zoom };
      e.preventDefault();
    }, { passive: false });
    wrap.addEventListener('touchmove', (e) => {
      if (!pinch || e.touches.length !== 2) return;
      e.preventDefault();
      const r = wrap.getBoundingClientRect();
      const mx = (e.touches[0].clientX + e.touches[1].clientX) / 2 - r.left, my = (e.touches[0].clientY + e.touches[1].clientY) / 2 - r.top;
      setZoom(pinch.z * dist(e) / pinch.d, mx, my);
    }, { passive: false });
    const end = (e) => { if (pinch && e.touches.length < 2) pinch = null; };
    wrap.addEventListener('touchend', end); wrap.addEventListener('touchcancel', end);
  }

  // Sau khi dựng lưới: vẽ Tuần/Triệt trên đường ranh giới và đường tam phương tứ chính.
  function afterChart() {
    applySelection();
    placeTuanTriet();
    applyZoom();
    attachPinch();
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
    const list = (arr, withBri) => arr.length ? `<div class="slist">${arr.map((s) => {
      const inner = `<span class="${s.e ? 'e-' + s.e : ''}"><b>${esc(s.n)}</b></span>${withBri && s.b ? ' · ' + esc(BRI[s.b] || s.b) : ''}${s.e ? ' · ' + HANH[s.e] : ''}`;
      return s.s === 'hoa' ? `<span class="tag">${inner}</span>`
        : `<button type="button" class="tag star-tag" data-act="star" data-k="${esc(s.k)}" data-from="${chi}" aria-label="Thẻ sao ${esc(s.n)}">${inner}</button>`;
    }).join('')}</div>` : '<div class="small muted">—</div>';
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
      `<p class="small muted" style="margin:8px 0 0">Chạm tên một sao để xem thẻ sao.</p>` +
      `<h3>Chính tinh</h3>${p.voChinhDieu ? '<div class="small muted">Vô chính diệu — mượn chính tinh cung xung chiếu ' + esc(CHI[m(chi + 6)]) + ' để luận.</div>' : list(by('main'), true)}` +
      (by('hoa').length ? `<h3>Tứ Hóa gốc</h3>${list(by('hoa'))}` : '') +
      `<h3>Cát tinh</h3>${list(by('good'), true)}` +
      `<h3>Hung tinh</h3>${list(by('bad'), true)}` +
      `<h3>Vòng Tràng Sinh</h3>${list(by('ring'))}` +
      (luuHere.length ? `<h3>Sao lưu năm ${y.lunarYear}</h3><div class="slist">${luuHere.map((n) => `<span class="tag" style="color:var(--luu)">${esc(n)}</span>`).join('')}</div>` : '') +
      `<h3>Liên hệ</h3><div class="small">Tam hợp: <b>${esc(rel([m(chi + 4), m(chi + 8)]))}</b><br>Xung chiếu: <b>${esc(rel([m(chi + 6)]))}</b><br>` +
      `Giáp: <b>${esc(rel([m(chi - 1), m(chi + 1)]))}</b><br>Nhị hợp: <b>${esc(rel([m(1 - chi)]))}</b></div>` +
      palaceReading(d, chi);
    openSheet();
  }
  function openSheet() {
    a11y($('#sheet'));
    $('#scrim').classList.add('open');
    $('#sheet').classList.add('open');
    $('#sheet').scrollTop = 0;
  }

  // ——— Thẻ sao (mục 13: chạm giữ một sao trên lưới, hoặc chạm sao trong bảng cung) ———
  function openStar(k, fromChi) {
    let c;
    try {
      c = api('star', { input: withPreset(S.req.input), alt: S.req.alt, star: k });
    } catch (e) { openModal('Không mở được thẻ sao', e.message, 'Đóng', null, null); return; }
    const fact = (label, v) => (v ? `<div class="fact"><span>${esc(label)}</span><b>${esc(v)}</b></div>` : '');
    const back = fromChi != null && fromChi !== '' ? `<button class="btn small" data-act="palace-back" data-chi="${esc(fromChi)}">‹ Về cung ${esc(S.chart.palaces[+fromChi].role)}</button>` : '';
    $('#sheet').innerHTML = `<div class="grab"></div><div class="starcard" data-star="${esc(c.star)}">` +
      `<div class="row" style="align-items:flex-start"><div><h2>${esc(c.name)}</h2>` +
      `<div class="muted">${esc(c.kind)} · tại ${esc(c.chi)} (cung ${esc(c.role)})${c.brightness ? ' · ' + esc(c.brightness) : ''}</div></div>` +
      `<button class="icon-btn" data-act="close-sheet" style="flex:none" aria-label="Đóng">✕</button></div>` +
      `<div class="facts" style="margin-top:10px">` + fact('Chòm sao', c.dau) + fact('Ngũ hành', c.hanh || (c.element ? c.element : '')) +
      fact('Hóa khí', c.hoaKhi) + fact('Độ sáng tại cung', c.brightness) + `</div>` +
      (c.dacTinh ? `<p class="small" style="margin:8px 0 0">${esc(c.dacTinh)}</p>` : '') +
      (c.natalHoa.length ? `<h3>Tứ Hóa gốc</h3><div class="slist">${c.natalHoa.map((h) => `<span class="tag">${esc(h)}</span>`).join('')}</div>` : '') +
      `<h3>Ý nghĩa tại cung ${esc(c.role)}</h3>` + c.meaning.map((p, i) => rPara(p, 'star:' + c.star + ':' + i)).join('') +
      (c.patterns.length ? `<h3>Cách cục liên quan</h3>` + c.patterns.map((w) => `<div class="pl"><b>${esc(w.title)}</b><div>${esc(w.detail)}</div>` +
        (w.source ? `<div class="small muted">Nguồn: ${esc(w.source)}</div>` : '') + `</div>`).join('') : '') +
      (c.metaSource ? `<p class="small muted">Chòm, hành, hóa khí theo: ${esc(c.metaSource)}</p>` : '') +
      `<div class="row" style="margin-top:10px">${back}</div></div>`;
    openSheet();
    if (location.hash === '#selftest') console.log('TUVI_SELFTEST STARCARD ' + c.star);
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
      case 'compare': S.cmp.data = null; S.cmp.err = ''; go('compare'); break;
      case 'cmp-rel': S.cmp.rel = el.dataset.v; S.cmp.data = null; S.cmp.err = ''; render(false); break;
      case 'cmp-run': runCompare(); render(false); { const r = $('#cmp-res') || $('#cmp-error'); if (r) r.scrollIntoView({ block: 'start' }); } break;
      case 'rank-hours': rankHours(); render(false); { const r = $('#rank') || $('#rank-error'); if (r) r.scrollIntoView({ block: 'start' }); } break;
      case 'star': openStar(el.dataset.k, el.dataset.from); break;
      case 'palace-back': openPalace(+el.dataset.chi); break;
      case 'rday': { S.rv.date = el.dataset.d; const sy = window.scrollY; loadReading(); render(false); window.scrollTo(0, sy); break; }
      case 'rmon': {
        let yy = S.rv.dy, mm = S.rv.dm + parseInt(el.dataset.d, 10);
        if (mm < 1) { mm = 12; yy--; }
        if (mm > 12) { mm = 1; yy++; }
        if (yy < 1900 || yy > 2200) break;
        const dd = Math.min(+S.rv.date.slice(8, 10), new Date(yy, mm, 0).getDate());
        S.rv.dy = yy; S.rv.dm = mm; S.rv.date = isoOf(yy, mm, dd);
        loadReading(); render(false); break;
      }
      case 'zoom': { const z = +el.dataset.z; setZoom(z === 0 ? 1 : S.zoom + z * 0.25); break; }
      case 'export': {
        const k = el.dataset.k, a = el.dataset.a;
        openModal(k === 'pdf' ? 'Đang tạo PDF…' : 'Đang tạo ảnh…', 'Vui lòng chờ trong giây lát.', 'Đóng', null, null);
        setTimeout(() => {
          try { closeModal(); doExport(k, a); } catch (err) { openModal('Không xuất được', err.message, 'Đóng', null, null); }
        }, 40);
        break;
      }
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
      case 'open-reading': openReading(el.dataset.s); break;
      case 'rscope': openReading(el.dataset.s, S.rv.year, S.rv.month); break;
      case 'ryear': openReading(S.rv.scope, Math.max(1900, Math.min(2200, S.rv.year + parseInt(el.dataset.d, 10))), S.rv.month); break;
      case 'rmonth': openReading('month', S.rv.year, +el.dataset.m); break;
      case 'hm': openReading('month', S.rv.year, +el.dataset.m); break;
      case 'rdecade': openReading('decade', parseInt(el.dataset.y, 10)); break;
      case 'pal': { const k = el.dataset.id; S.palOpen[k] = !S.palOpen[k]; const sy = window.scrollY; render(false); window.scrollTo(0, sy); break; }
      case 'rp': {
        const k = el.dataset.key, open = !S.rdOpen[k];
        S.rdOpen[k] = open;
        el.classList.toggle('open', open);
        const t = el.querySelector('.rp-tap');
        if (t) t.textContent = open ? 'Thu gọn ‹' : 'Vì sao? ›';
        break;
      }
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
        if (S.lpFired) { S.lpFired = false; break; } // vừa chạm giữ một sao → đã mở thẻ sao
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
    if (el.dataset.q) { S.ev[el.dataset.q] = el.value; return; }
    const f = el.dataset.f;
    if (!f || !S.form) return;
    if (el.type === 'checkbox') return;
    S.form[f] = el.value;
    if (f === 'year') { if (/^\d{4}$/.test(el.value)) { if (S.form.calendar === 'lunar') { const pos = el.selectionStart; render(false); const n = $('[data-f="year"]'); if (n) { n.focus(); n.setSelectionRange(pos, pos); } } else updateConv(); } }
  });
  document.addEventListener('change', (e) => {
    const el = e.target;
    if (el.dataset.act === 'year-range') { setYear(parseInt(el.value, 10)); return; }
    if (el.dataset.q) { S.ev[el.dataset.q] = el.value; return; }
    if (el.dataset.cmp) { S.cmp[el.dataset.cmp] = el.value; S.cmp.data = null; S.cmp.err = ''; render(false); return; }
    if (el.dataset.cfg) { S.settings.config[el.dataset.cfg] = el.value; saveSettings(); S.readingCache = {}; return; }
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
