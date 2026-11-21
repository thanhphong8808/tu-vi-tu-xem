/* Tử Vi Tự Xem — bộ icon (D-63). Nét 1,7 px trên lưới 24 px, màu theo chữ (currentColor). Cùng bộ đã dựng trên Figma
 * (file “Tử Vi Tự Xem — Hệ thiết kế”, trang Icons). Dùng: TVIcon('la-so') → chuỗi <svg>. */
(function () {
  const P = {
    'bat-quai': '<path d="M8.2 2.8h7.6l5.4 5.4v7.6l-5.4 5.4H8.2l-5.4-5.4V8.2z"/><circle cx="12" cy="12" r="5"/><path d="M12 7a2.5 2.5 0 0 1 0 5a2.5 2.5 0 0 0 0 5"/><circle cx="12" cy="9.5" r=".7" fill="currentColor" stroke="none"/><circle cx="12" cy="14.5" r=".7" fill="currentColor" stroke="none"/>',
    'la-so': '<rect x="3" y="3" width="18" height="18" rx="2"/><rect x="7.5" y="7.5" width="9" height="9" rx="1"/><path d="M12 3v4.5M12 16.5V21M3 12h4.5M16.5 12H21"/>',
    'lap-la-so': '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M12 8.5v7M8.5 12h7"/><path d="M3 7.5h3M3 16.5h3M18 7.5h3M18 16.5h3M7.5 3v3M16.5 3v3M7.5 18v3M16.5 18v3"/>',
    'luan-giai': '<path d="M6 4h11a2 2 0 0 1 2 2v11"/><path d="M6 4a2 2 0 0 0-2 2v1h3"/><path d="M7 7v11a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2v-1h-9v1a2 2 0 0 1-2 2"/><path d="M10 9.5h6M10 12.5h6"/>',
    'hoi-dap': '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v8a2.5 2.5 0 0 1-2.5 2.5H10l-4.5 4v-4A1.5 1.5 0 0 1 4 14.5z"/><path d="M12 6.4l1 2.1 2.3.3-1.7 1.6.4 2.3-2-1.1-2 1.1.4-2.3-1.7-1.6 2.3-.3z"/>',
    'tro-ly-ai': '<path d="M11 3l1.8 4.7 4.7 1.8-4.7 1.8L11 16l-1.8-4.7L4.5 9.5l4.7-1.8z"/><path d="M18 14l.8 1.9 1.9.8-1.9.8-.8 1.9-.8-1.9-1.9-.8 1.9-.8z"/>',
    'bac-dau': '<path d="M3.5 7.5l4-1 3 2.5 3 2 1 5 5.5 1.5.5-5.5-6.5-1"/><circle cx="3.5" cy="7.5" r="1.3" fill="currentColor" stroke="none"/><circle cx="7.5" cy="6.5" r="1.3" fill="currentColor" stroke="none"/><circle cx="10.5" cy="9" r="1.3" fill="currentColor" stroke="none"/><circle cx="13.5" cy="11" r="1.3" fill="currentColor" stroke="none"/><circle cx="14.5" cy="16" r="1.3" fill="currentColor" stroke="none"/><circle cx="20" cy="17.5" r="1.3" fill="currentColor" stroke="none"/><circle cx="20.5" cy="12" r="1.3" fill="currentColor" stroke="none"/>',
    'lich': '<rect x="3.5" y="5" width="17" height="15" rx="2"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/><path d="M13.5 12a3 3 0 1 0 1.8 5.5a3.4 3.4 0 0 1-1.8-5.5z"/>',
    'la-ban': '<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/><path d="M12 3v2M12 19v2M3 12h2M19 12h2"/>',
    'trang': '<path d="M15 3.5a8.5 8.5 0 1 0 5.5 13A7 7 0 0 1 15 3.5z"/>',
    'dai-van': '<path d="M20 12a8 8 0 0 1-13.7 5.6M4 12a8 8 0 0 1 13.7-5.6"/><path d="M17.7 3v3.6h-3.6M6.3 21v-3.6h3.6"/>',
    'so-sanh': '<circle cx="9" cy="12" r="5.5"/><circle cx="15" cy="12" r="5.5"/>',
    'gio-sinh': '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    'cai-dat': '<path d="M4 7h10M18 7h2M4 17h4M12 17h8"/><circle cx="16" cy="7" r="2"/><circle cx="10" cy="17" r="2"/>',
    'chia-se': '<circle cx="18" cy="5.5" r="2.5"/><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="18.5" r="2.5"/><path d="M8.2 10.8l7.6-4M8.2 13.2l7.6 4"/>',
    'xuat': '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M12 11v6M9.5 14.5L12 17l2.5-2.5"/>',
    'dien-thoai': '<path d="M5 4h3.5l1.7 4.2-2.2 1.4a11 11 0 0 0 6.4 6.4l1.4-2.2 4.2 1.7V19a1.5 1.5 0 0 1-1.6 1.5A16 16 0 0 1 3.5 5.6 1.5 1.5 0 0 1 5 4z"/>',
    'sach': '<path d="M12 6.5C10 5 7 4.5 3.5 5v13c3.5-.5 6.5 0 8.5 1.5 2-1.5 5-2 8.5-1.5V5C17 4.5 14 5 12 6.5z"/><path d="M12 6.5v13"/>',
    'khoa': '<rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/>',
    'quay-lai': '<path d="M15 5l-7 7 7 7"/>',
    'cong': '<path d="M12 5v14M5 12h14"/>',
    'dong': '<path d="M6 6l12 12M18 6L6 18"/>',
  };
  window.TV_ICON_PATHS = P;
  window.TVIcon = function (name, cls) {
    const p = P[name];
    if (!p) return '';
    return '<svg class="ic' + (cls ? ' ' + cls : '') + '" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" ' +
      'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">' + p + '</svg>';
  };
})();
