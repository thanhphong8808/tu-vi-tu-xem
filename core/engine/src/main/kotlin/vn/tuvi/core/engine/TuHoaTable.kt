package vn.tuvi.core.engine

import vn.tuvi.core.calendar.Can

/**
 * Tứ Hóa theo can (mục 7): cho biết sao nào hóa Lộc/Quyền/Khoa/Kỵ — dùng cho luật kiểu “Phá Quân hóa Quyền”
 * (Tứ Hóa gốc: can năm sinh) hoặc “Mệnh có Thái Dương gặp năm Hóa Kỵ lưu” (can năm xem).
 */
object TuHoaTable {
    fun of(can: Can, config: SchoolConfig = SchoolConfig.DEFAULT): Map<TuHoa, StarId> = Tables.tuHoa(can, config)
}
