package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind
import java.text.Normalizer
import java.time.LocalDate

/** Ý định của câu hỏi (mục 12.2 bước 1). */
enum class QaIntent(val vn: String) {
    FORECAST("Luận theo chủ đề, thời gian"),
    TIMING("Khi nào thuận"),
    ADVICE("Nên / Không nên"),
    FACT("Dữ kiện lá số"),
    DEFINITION("Giải nghĩa"),
    STAR("Thẻ sao"),
    COMPARE("So hai lá số"),
    REFUSE("Ngoài phạm vi"),
    CRISIS("Hỗ trợ"),
    GREETING("Giới thiệu"),
    UNKNOWN("Chưa rõ câu hỏi"),
}

/** Mốc thời gian hiểu được từ câu hỏi. [ageFrom]/[ageTo]: giai đoạn đời (tiền vận, hậu vận…) khi phạm vi là trọn đời. */
data class QaTime(
    val scope: ReadingScope,
    val lunarYear: Int? = null,
    val month: Int? = null,
    val date: LocalDate? = null,
    val ageFrom: Int? = null,
    val ageTo: Int? = null,
    val label: String = "",
    /** Số ngày tính từ [date] (tuần này, cuối tuần…); 1 = một ngày. */
    val days: Int = 1,
)

/** Kết quả phân loại câu hỏi. */
data class QaParse(
    val question: String,
    val norm: String,
    val intent: QaIntent,
    val topics: List<Topic>,
    val event: Event?,
    val time: QaTime?,
    val timingUnit: ReadingScope?,
    val fact: String?,
    val terms: List<String>,
    val star: StarId?,
    val role: PalaceRole?,
    val refuse: String?,
    val reframe: String?,
    val yesNo: Boolean,
    /** Câu hỏi theo hướng cần thận trọng (“năm nào nên chú ý sức khỏe”) → xếp từ thấp lên. */
    val caution: Boolean = false,
    /** Hỏi về người thân (con tôi, mẹ tôi…): chỉ đọc gián tiếp qua cung tương ứng trên lá số đang mở. */
    val relative: String? = null,
    /** Đối chiếu một năm đã qua: người hỏi kể việc đã xảy ra năm [time] và muốn xem lá số có khớp không. */
    val verify: Boolean = false,
)

/** Mở bản luận đầy đủ tương ứng trong app. */
data class QaLink(val scope: ReadingScope, val lunarYear: Int?, val month: Int?, val date: LocalDate?, val label: String)

/** Câu trả lời: [understood] nói rõ app đã hiểu câu hỏi thế nào; mọi đoạn có ruleIds và “Vì sao?”. */
data class QaAnswer(
    val question: String,
    val parse: QaParse,
    val understood: String,
    val score: Int?,
    val stance: Stance?,
    val paragraphs: List<Paragraph>,
    val advice: List<AdviceItem>,
    val followUps: List<String>,
    val link: QaLink?,
    val closing: String?,
) {
    fun fullText(): String = (listOf(understood) + paragraphs.map { it.text } + advice.map { it.text } + listOfNotNull(closing)).joinToString("\n")
}

/** Ngày hôm nay của người hỏi (mặc định giờ Việt Nam). */
data class QaContext(val today: LocalDate, val expert: Boolean = false) {
    val todayLunar: LunarDate = LunarCalendar.solarToLunar(today, 7)
    val lunarYear: Int get() = todayLunar.year
    val lunarMonth: Int get() = todayLunar.month
}

/**
 * Hỏi đáp chạy ngay trên máy (mục 12): hiểu câu hỏi tiếng Việt (có dấu hoặc không dấu) bằng bộ từ khóa, rồi trả lời
 * bằng chính các đoạn luận, điểm, sự kiện, khuyến nghị do [Composer] đã tính. Không tự an sao, không tự luận thêm:
 * câu nào cũng có ruleIds và căn cứ. Câu hỏi khủng hoảng tinh thần không đi qua phần luận số.
 */
class QaEngine(private val kb: KnowledgeBase, private val composer: Composer = Composer(kb)) {

    // ———————————————————————————— bộ nhận diện ————————————————————————————

    companion object {
        /** Chữ thường, bỏ dấu, đ → d, chỉ giữ chữ, số, “/”. */
        fun normalize(s: String): String {
            val lower = s.lowercase().replace('đ', 'd')
            val noMarks = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            return noMarks.replace(Regex("[^a-z0-9/]+"), " ").trim().replace(Regex("\\s+"), " ")
        }

        private fun rx(p: String) = Regex("(?<![a-z0-9])(?:$p)(?![a-z0-9])")

        private val CRISIS = rx(
            "tu tu|tu sat|muon chet|muon tu tu|khong muon song|chang muon song|chan song|het muon song|ket thuc cuoc doi|" +
                "ket lieu|tu hai|tu lam hai|cat tay|nhay lau|nhay cau|uong thuoc ngu|treo co|song khong con y nghia|khong con ly do de song|" +
                "muon bien mat|bien mat khoi the gioi|chet di cho xong|chet cho xong|ra di cho nhe",
        )
        private val REF_EM_BE = rx(
            "con trai hay con gai|trai hay gai|be trai hay be gai|sinh trai|sinh gai|de trai|de gai|sinh con trai|sinh con gai|" +
                "gioi tinh thai|gioi tinh em be|gioi tinh cua be|gioi tinh con|thai nhi la trai|thai la trai|thai la gai|cau trai|cau gai",
        )
        private val REF_SONG_LAU = rx(
            "tuoi tho|song duoc bao lau|song bao lau|song duoc bao nhieu|song bao nhieu (?:nam|tuoi)|song den bao nhieu|song tho|" +
                "chet|qua doi|tu tran|bao gio (?:thi )?het kiep|(?:bao gio|khi nao|nam nao|luc nao|tuoi nao) (?:toi |minh |em |anh |chi |ong |ba |bo |me |cha )?(?:se )?mat(?= *$| nhi| a| vay| khong| ha)",
        )
        private val REF_SO = rx("so de|danh de|lo de|xo so|ve so|so may man|con so may man|trung so|trung xo|vietlott|keno|ca do|ca cuoc|danh bai|co bac|tai xiu|bao lo|so nao (?:se )?ve|danh so")
        private val REF_DICH_VU = rx("giai han|cung sao|dang sao|cung giai|giai sao|lam le giai|bua ngai|bua yeu|yem bua|thay cung|cau sieu|cung bai giai")
        private val REF_NGUOI_KHAC = rx(
            "(?:la so|tu vi|van han|van menh|tuong lai|so menh) (?:cua )?(?:vo|chong|con|nguoi yeu|ban trai|ban gai|bo|me|cha|anh|chi|em|ban|sep|ong|ba|hang xom) (?:toi|minh|em|anh|chi|tao)",
        )
        private val OUT_OF_DOMAIN = rx(
            "thoi tiet|nau an|mon an|cong thuc nau|toan hoc|giai toan|bai toan|bai tap|lap trinh|viet code|dich sang|dich giup|bong da|ty so|gia vang|gia xang|" +
                "tin tuc|chinh tri|bau cu|chung khoan ma|ma co phieu|viet van|lam tho|hat bai",
        )
        private val RF_HON_NHAN = rx("ly hon|ly than|chia tay|tan vo|bo nhau|ngoai tinh|phan boi|cam sung|danh ghen")
        private val RF_BENH = rx("ung thu|tim mach|huyet ap|tieu duong|benh gi|bi benh gi|mac benh|chan doan|ung buou|vo sinh|hiem muon|dot quy|nan y")
        private val RF_PHAP_LY = rx("tu toi|di tu|vao tu|bi bat|ngoi tu|o tu|bi kien|ra toa|lao ly")
        private val RF_TAI_NAN = rx("tai nan|tai hoa|bat trac|xe dam|tai uong|tai ach|gap nan")
        private val RF_HOP_TUOI = rx(
            "hop tuoi|tuoi hop|xung tuoi|khac tuoi|hop menh|menh hop|tam tai|kim lau|hoang oc|xem tuoi|tuoi nao hop|tuoi gi hop|" +
                "hop voi (?:vo|chong|nguoi yeu|ban trai|ban gai|nguoi tuoi|tuoi)|(?:hai vo chong|vo chong toi|chung toi|hai dua) co hop|nguoi tuoi (?:ty|suu|dan|mao|thin|ngo|mui|than|dau|tuat|hoi)",
        )
        private val RF_CHON_NGAY = rx("ngay tot|ngay dep|chon ngay|xem ngay|hoang dao|hac dao|ngoc hap|gio hoang dao|ngay khai truong|ngay dong tho|ngay xuat hanh|ngay nhap trach|ngay an hoi|ngay cuoi")

        private val DEF_Q = rx("la gi|nghia la gi|nghia la sao|co nghia la|nghia gi|y nghia|giai thich|hieu the nao|hieu nhu the nao|la the nao|la sao|la cai gi|dinh nghia")
        private val TIMING_Q = rx(
            "khi nao|bao gio|nam nao|luc nao|tuoi nao|may tuoi thi|nam may|thoi diem nao|giai doan nao|bao lau nua|den bao gio|den khi nao|" +
                "thang nao|ngay nao|hom nao|gio nao|nam bao nhieu|tuoi bao nhieu thi|som nhat|chung nao|dip nao|" +
                // “đoán được năm cưới của tôi”, “tuổi kết hôn”: hỏi mốc năm của một việc (D-64). “cưới” đã đổi thành “cưới hỏi”
                // nên “năm cưới hỏi” không lẫn với “năm cuối”.
                "nam cuoi hoi|tuoi cuoi hoi|nam ket hon|tuoi ket hon|nam lay (?:vo|chong)|tuoi lay (?:vo|chong)|nam lap gia dinh|nam (?:sinh|co) con|" +
                "(?:doan|du doan|biet|bao truoc)(?: duoc| ra| giup toi| cho toi)? (?:nam|tuoi)(?! nay| sau| toi(?! (?:co|se|duoc|lay|cuoi|sinh|ket|doi|gap|phat|thang|di|mua|xay)(?![a-z]))| ngoai| truoc| roi| qua| hien tai| \\d)",
        )
        /** “Tôi đã cưới năm 2015, có khớp không?”: đối chiếu một năm đã qua với lá số (D-64). */
        private val VERIFY_Q = rx("co khop|khop khong|khop voi|dung khong|co dung|kiem chung|kiem tra|doi chieu|nghiem lai|ung nghiem|trung khong|co trung|co dung voi|da|vua")
        private val ADVICE_Q = rx(
            "nen lam gi|can lam gi|luu y gi|luu y nhung gi|nen tranh|can tranh|kieng|kieng ky|tranh gi|lam gi de|nen chu y|chu y gi|chu y nhung gi|" +
                "loi khuyen|khuyen|nen va khong nen|nen gi|khong nen lam gi|can luu y|phai lam sao|lam sao de|lam the nao de|nen the nao|nen ra sao|can chuan bi",
        )
        private val YESNO_Q = rx("co nen|nen khong|duoc khong|co duoc|phai khong|hay khong|co .{1,40} khong")
        private val OVERALL_Q = rx(
            "the nao|ra sao|nhu the nao|nhu nao|sao roi|tot khong|xau khong|tot hay xau|on khong|co tot|co xau|van han|van trinh|van so|tong quan|" +
                "co gi|may man|xui|kho khan|thuan loi|hanh thong|suon se|co on|tien trien|ket qua|dien bien|tuong lai|so phan|van menh|so menh",
        )
        private val RELATIVE = Regex("(?<![a-z])(?<!vo )(con|vo|chong|bo|me|cha|ba|anh|chi|em|nguoi yeu|ban trai|ban gai|ong|ba noi|ba ngoai) (?:cua )?(?:toi|minh|nha toi|nha minh)(?![a-z])")
        private val CAUTION_Q = rx("chu y|can than|than trong|de phong|xau|kho khan|nam han|xui|nen tranh|luu y|coi chung|gap han|bat loi|tro ngai|sa sut|di xuong|kem nhat|te nhat")
        private val LAYER_SHORT = mapOf(
            Layer.GOC to "gốc", Layer.DAI_HAN to "đại hạn", Layer.LUU_NIEN to "lưu niên", Layer.TIEU_HAN to "tiểu hạn", Layer.LUU_THAI_TUE to "Lưu Thái Tuế",
        )
        private val TIMELESS = setOf(Topic.TINH_CACH, Topic.PHUC_DUC)
        /** Chủ đề không có điểm riêng theo năm → xem kèm chủ đề gần nhất (Điền Trạch là cung phụ của tài chính, mục 9). */
        private val YEAR_PROXY = emptyMap<Topic, Topic>()
        private val GREET = rx("xin chao|chao ban|chao app|hello|^hi$|alo|ban la ai|ban la gi|hoi gi duoc|hoi duoc gi|giup gi|huong dan|lam sao de hoi|hoi the nao|cam on|thank")

        /** Chủ đề (mục 9 bảng chủ đề) → từ khóa không dấu. */
        private val TOPIC_RX: List<Pair<Topic, Regex>> = listOf(
            Topic.TINH_DUYEN to rx(
                "tinh duyen|tinh yeu|tinh cam|hon nhan|vo chong|nguoi yeu|ket hon|cuoi hoi|cuoi vo|cuoi chong|lay vo|lay chong|dam cuoi|ban doi|" +
                    "nguoi thuong|chia tay|ly hon|ngoai tinh|yeu duong|hen ho|duyen phan|nhan duyen|co vo|co chong|vo toi|chong toi|vo minh|chong minh|" +
                    "e vo|e chong|doc than|phu the|thoat e|co nguoi yeu|co ban trai|co ban gai|lap gia dinh|tinh duyen",
            ),
            Topic.SU_NGHIEP to rx(
                "lam quan|quan chuc|lam sep|lam lanh dao|cong viec|su nghiep|viec lam|nghe nghiep|cong ty|sep|dong nghiep|thang chuc|thang tien|len chuc|de bat|bo nhiem|doi viec|nhay viec|" +
                    "chuyen viec|xin viec|tim viec|that nghiep|nghi viec|bo viec|mat viec|bi duoi viec|bi sa thai|sa thai|cat giam|khoi nghiep|lam chu|hoc hanh|hoc tap|thi cu|di thi|ky thi|thi dai hoc|" +
                    "thi tot nghiep|dau dai hoc|bang cap|chuc vu|quan loc|cong danh|danh vong|di lam|lam viec|nghe gi|hop nghe|nghe nao|kinh doanh|" +
                    "buon ban|lam an|mo cua hang|mo quan|cong chuc|tang luong|len luong|viec moi|du an|viec hoc|di hoc|thi chuyen|thi vao|hoc gioi|hoc kem",
            ),
            Topic.TAI_CHINH to rx(
                "tai chinh|tai loc|tien bac|(?<!thang )(?<!len )tien(?! van)|giau|ngheo|thu nhap|(?<!thien )luong|kiem tien|lam giau|dau tu|chung khoan|" +
                    "co phieu|mua vang|tien ao|vay tien|vay no|no nan|mac no|het no|tra no|tra het no|khoan no|thua lo|lo von|bi lo|loi nhuan|hao tai|mat tien|pha san|tich luy|tiet kiem|phat tai|" +
                    "lam an|kinh doanh|buon ban|tai san|tai bach|von|doanh thu|thu chi",
            ),
            Topic.GIA_DAO to rx(
                "con cai|sinh con|co con|them con|duong con|nuoi con|day con|con toi|con minh|con cua toi|dua con|chuyen con|con dau long|em be|" +
                    "mang thai|co bau|co thai|hiem muon|cha me|bo me|ba me|me toi|bo toi|cha toi|ba toi|ong ba|anh em|anh chi em|chi em|gia dinh|" +
                    "gia dao|nha minh|ho hang|noi ngoai|phu mau|huynh de|tu tuc|me chong|me vo|bo chong|bo vo|nha chong|nha vo|hanh phuc gia dinh",
            ),
            Topic.SUC_KHOE to rx(
                "suc khoe|benh|om dau|bi om|om yeu|dau om|tat benh|phau thuat|mo xe|nam vien|benh vien|tai nan|chan thuong|the luc|stress|" +
                    "cang thang|mat ngu|tat ach|an toan|ung thu|tim mach|huyet ap|tieu duong|kham benh|tinh than",
            ),
            Topic.DI_CHUYEN to rx(
                "di xa|xuat ngoai|nuoc ngoai|du hoc|dinh cu|xuat khau lao dong|di lam xa|di nhat|di han|di my|di uc|di duc|di dai loan|du lich|" +
                    "xuat hanh|di lai|chuyen cong tac|cong tac|ve que|di choi xa|di choi|di phuot|di chuyen|thien di|xa que|tha huong|lap nghiep xa|doi cho o|chuyen nha|" +
                    "xe co|lai xe|mua xe|di xe|di may bay",
            ),
            Topic.NHA_DAT to rx(
                "nha dat|mua (?:duoc )?nha|ban nha|xay (?:duoc )?nha|sua nha|lam nha|nha cua|nha moi|can nha|ngoi nha|co nha|tau nha|so huu nha|an cu|" +
                    "bat dong san|chung cu|can ho|dat dai|mua (?:duoc )?dat|ban dat|lo dat|manh dat|dien trach|nha o|thue nha|chuyen nha|tan gia|dong tho|nhap trach",
            ),
            Topic.PHAP_LY to rx(
                "phap ly|kien tung|kien cao|bi kien|vu kien|khoi kien|tranh chap|giay to|hop dong|ky ket|ky hop dong|toa an|ra toa|cong an|thi phi|" +
                    "bi lua|lua dao|tieng xau|mieng tieng|tu toi|di tu|vao tu|bi bat|quan phu|tai tieng|phat nguoi|so do",
            ),
            Topic.PHUC_DUC to rx("phuc duc|am duc|duc nang|tam linh|to tien|mo ma|mo phan|tu hanh|di chua|cau an|tin nguong|phuc phan|phuc loc|ong ba to tien|lam phuc|lam viec thien"),
            Topic.TINH_CACH to rx(
                "tinh cach|tinh net|con nguoi toi|ban than toi|diem manh|diem yeu|so truong|so doan|nang khieu|tu chat|ban chat|tinh khi|" +
                    "toi la nguoi|nguoi the nao|hop nghe|nghe gi|tai nang|the manh|ban linh|tinh tinh",
            ),
        )

        /** Sự kiện (mục 9, đồng thanh tương ứng) → từ khóa không dấu. */
        private val EVENT_RX: List<Pair<Event, Regex>> = listOf(
            Event.CUOI_HOI to rx(
                "cuoi(?! nam| thang| tuan| cung| doi| ky| gio| ngay| moi| nhe| nhieu)|(?:nen|duoc|se|dinh|muon|sap|chua|kip|lo|tinh|da|vua) cuoi|ket hon|lay vo|lay chong|cuoi hoi|dam cuoi|thanh hon|co nguoi yeu|" +
                    "gap duoc nguoi thuong|gap nguoi yeu|gap duoc nguoi yeu|tim duoc nguoi yeu|tim nguoi yeu|gap duoc ban doi|thoat e|co vo|co chong|lap gia dinh|ve chung nha|hen ho|yeu duong|co ban trai|co ban gai|co nguoi thuong|ban doi",
            ),
            Event.SINH_CON to rx("sinh con|co con|them con|co em be|co bau|mang thai|co thai|sinh em be|de con|sinh no|hiem muon|dua con dau|con dau long|sinh them|duoc con|lam me|lam bo|lam cha"),
            Event.THANG_TIEN to rx("thang chuc|thang tien|len chuc|de bat|bo nhiem|tang luong|len luong|thang hang|do dat|thi do|dau dai hoc|dau tot nghiep|trung tuyen|thi dau|dat giai|vinh danh|len cap|duoc giao trong trach"),
            Event.DOI_VIEC to rx("doi viec|nhay viec|chuyen viec|nghi viec|bo viec|mat viec|bi duoi viec|sa thai|that nghiep|xin viec|tim viec|viec moi|doi nghe|chuyen nghe|khoi nghiep|mo cong ty|ra rieng|lam rieng|lam chu|chuyen cong ty|doi cong ty|chuyen nganh"),
            Event.DUOC_TAI to rx("giau|lam giau|phat tai|phat dat|co tien|kiem duoc tien|kiem nhieu tien|tien vao|duoc loc|tai loc|tien thuong|thuong lon|thu nhap tang|loi lon|co lai|lai to|duoc tien|ve tien|tien ve|khoan thu lon"),
            Event.HAO_TAI to rx("mat tien|hao tai|thua lo|lo von|bi lo|lo nang|bi lua tien|vo no|pha san|ton tien|tien ra|hao hut|mat cua|mat trom|bi trom|no nan|mac no|ton kem"),
            Event.PHAP_LY to rx("kien tung|kien cao|bi kien|vu kien|tranh chap|ra toa|toa an|thi phi|bi lua|lua dao|tai tieng|tu toi|vao tu|di tu|bi bat|rac roi giay to"),
            Event.DI_CHUYEN to rx(
                "di xa|xuat ngoai|di nuoc ngoai|ra nuoc ngoai|du hoc|dinh cu|xuat khau lao dong|di lam xa|chuyen nha|doi cho o|chuyen cong tac|tha huong|" +
                    "xa que|di choi xa|di du lich xa|chuyen vao nam|chuyen ra bac|di nhat|di han|di my|di uc|di duc|di dai loan|di chau au|lap nghiep xa",
            ),
            Event.SUC_KHOE to rx("bi om|om dau|benh|nam vien|phau thuat|mo xe|tai nan|chan thuong|suc khoe yeu"),
        )

        private val ROLE_RX: List<Pair<PalaceRole, Regex>> = listOf(
            PalaceRole.PHU_MAU to rx("cung phu mau|phu mau|cung cha me|cung bo me"),
            PalaceRole.PHUC_DUC to rx("cung phuc duc|phuc duc|cung phuc"),
            PalaceRole.DIEN_TRACH to rx("cung dien trach|dien trach|cung nha cua|cung nha dat|cung nha"),
            PalaceRole.QUAN_LOC to rx("cung quan loc|quan loc|cung quan|cung su nghiep|cung cong danh"),
            PalaceRole.NO_BOC to rx("cung no boc|no boc|cung no|cung ban be|cung giao huu"),
            PalaceRole.THIEN_DI to rx("cung thien di|thien di|cung di"),
            PalaceRole.TAT_ACH to rx("cung tat ach|tat ach|cung benh|cung suc khoe"),
            PalaceRole.TAI_BACH to rx("cung tai bach|tai bach|cung tai|cung tien bac|cung tien"),
            PalaceRole.TU_TUC to rx("cung tu tuc|tu tuc|cung con cai|cung con"),
            PalaceRole.PHU_THE to rx("cung phu the|phu the|cung vo chong|cung hon nhan|cung tinh duyen"),
            PalaceRole.HUYNH_DE to rx("cung huynh de|huynh de|cung anh em"),
            PalaceRole.MENH to rx("cung menh"),
        )

        /** Thuật ngữ → khóa giải nghĩa (qa.def.*). Thứ tự: cụm dài trước. */
        private val TERM_RX: List<Pair<String, Regex>> = listOf(
            "tu_vi" to rx("tu vi dau so|mon tu vi|xem tu vi|tu vi(?= la gi| la sao| co dung| co that| co tin)"),
            "vo_chinh_dieu" to rx("vo chinh dieu"),
            "ban_menh" to rx("ban menh|nap am|menh nap am"),
            "menh_chu" to rx("menh chu|than chu"),
            "chinh_tinh" to rx("chinh tinh"),
            "phu_tinh" to rx("phu tinh"),
            "luc_cat" to rx("luc cat|cat tinh"),
            "luc_sat" to rx("luc sat|sat tinh|hung tinh"),
            "hoa_loc" to rx("hoa loc"),
            "hoa_quyen" to rx("hoa quyen"),
            "hoa_khoa" to rx("hoa khoa"),
            "hoa_ky" to rx("hoa ky"),
            "tu_hoa" to rx("tu hoa"),
            "tuan" to rx("tuan khong|tuan triet|tuan"),
            "triet" to rx("triet lo|tuan triet|triet"),
            "luu_nien" to rx("luu nien dai han|luu nien"),
            "dai_han" to rx("dai han|dai van"),
            "tieu_han" to rx("tieu han|tieu van"),
            "thai_tue" to rx("luu thai tue|thai tue"),
            "nguyet_han" to rx("nguyet han|nhat han|thoi han"),
            "tam_phuong" to rx("tam phuong tu chinh|tam phuong|tu chinh|tam hop"),
            "xung_chieu" to rx("xung chieu"),
            "nhi_hop" to rx("nhi hop|luc hop"),
            "giap" to rx("giap cung|giap cat|giap hung"),
            "do_sang" to rx("do sang|mieu dia|ham dia|dac dia|binh hoa|mieu vuong|sao ham|sao mieu"),
            "dong_thanh" to rx("dong thanh tuong ung|dong thanh"),
            "the" to rx("the tan|the giu|the thu|the lui|tan giu thu lui|the giai doan"),
            "diem" to rx("thang diem|diem so|diem 0|diem nay|diem tong|diem (?:cua )?(?:nam|thang|ngay|dai van)|diem (?:\\d{1,3})(?:/100)?|cham diem|tinh diem"),
            "thien_thoi" to rx("thien thoi"),
            "sao_luu" to rx("sao luu"),
            "cach_cuc" to rx("cach cuc"),
            "truyen_cung" to rx("truyen cung|truyen tinh"),
            "cuc" to rx("cuc"),
            "than" to rx("cung than|than cu"),
            "menh" to rx("cung menh|menh"),
            "han" to rx("van han|nam han|han"),
        )

        private val CHI_NORM: List<Pair<String, Chi>> = Chi.entries.map { normalize(it.vn) to it }
        private val CAN_NORM: List<Pair<String, Can>> = Can.entries.map { normalize(it.vn) to it }
        private val NUM_WORD = mapOf(
            "mot" to 1, "hai" to 2, "ba" to 3, "bon" to 4, "tu" to 4, "nam" to 5, "sau" to 6, "bay" to 7, "tam" to 8, "chin" to 9, "muoi" to 10,
        )
        private val MONTH_WORD = mapOf(
            "gieng" to 1, "hai" to 2, "ba" to 3, "tu" to 4, "bon" to 4, "bay" to 7, "tam" to 8, "chin" to 9, "muoi mot" to 11, "muoi hai" to 12,
            "muoi" to 10, "chap" to 12, "mot" to 11,
        )

        val SHORT: Map<Topic, String> = mapOf(
            Topic.TINH_CACH to "tính cách", Topic.SU_NGHIEP to "công việc", Topic.TAI_CHINH to "tài chính", Topic.TINH_DUYEN to "tình duyên",
            Topic.GIA_DAO to "gia đạo", Topic.SUC_KHOE to "sức khỏe", Topic.DI_CHUYEN to "đi xa, di chuyển", Topic.NHA_DAT to "nhà đất",
            Topic.PHAP_LY to "giấy tờ, pháp lý", Topic.PHUC_DUC to "phúc đức",
        )
        private val CHILD_TOPICS = setOf(Topic.SU_NGHIEP, Topic.SUC_KHOE, Topic.GIA_DAO, Topic.DI_CHUYEN, Topic.TINH_CACH)
    }

    // ———————————————————————————— bộ nhớ tạm bản luận ————————————————————————————

    private val cache = object : LinkedHashMap<Any, Any>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, Any>?): Boolean = size > 40
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> cached(chart: Chart, key: Any, make: () -> T): T = synchronized(cache) {
        val k = Triple(chart.input, chart.config, key)
        (cache[k] as T?) ?: make().also { cache[k] = it }
    }

    private fun life(chart: Chart, ctx: QaContext) = cached(chart, "life" to ctx.lunarYear) { composer.lifetime(chart, ctx.expert, ctx.lunarYear) }
    private fun yearR(chart: Chart, y: Int, ctx: QaContext) = cached(chart, "year" to y) { composer.year(chart, y, ctx.expert) }
    private fun decadeR(chart: Chart, y: Int, ctx: QaContext) = cached(chart, "decade" to LimitEngine.forYear(chart, y).daiHanPeriod.index) { composer.decade(chart, y, ctx.expert) }
    private val analyst = QaAnalyst(kb)
    private fun yData(chart: Chart, y: Int, ctx: QaContext) = cached(chart, "ydata" to y) { composer.yearData(chart, y, ctx.expert) }
    private fun dData(chart: Chart, y: Int, ctx: QaContext) =
        cached(chart, "ddata" to LimitEngine.forYear(chart, y).daiHanPeriod.index) { composer.decadeData(chart, y, ctx.expert) }
    private fun lData(chart: Chart, ctx: QaContext) = cached(chart, "ldata") { composer.lifeData(chart, ctx.expert) }

    // ———————————————————————————— phân loại ————————————————————————————

    fun parse(question: String, chart: Chart, ctx: QaContext): QaParse {
        // “còn tôi”, “chỉ tôi”, “bỏ tôi” (có dấu) không phải “con tôi”, “chị tôi”, “bố tôi”.
        val lower = question.lowercase().trim()
            .replace(Regex("(?<![\\p{L}])còn (?=tôi|mình|em)"), "về phần ").replace(Regex("(?<![\\p{L}])chỉ (?=tôi|mình)"), "riêng ")
            .replace(Regex("(?<![\\p{L}])bỏ (?=tôi|mình)"), "rời ")
            .replace(Regex("(?<![\\p{L}])cưới(?! hỏi)(?![\\p{L}])"), "cưới hỏi")
        val n = normalize(lower)
        var caution = false
        var relative: String? = null
        var verify = false
        fun res(intent: QaIntent, topics: List<Topic> = emptyList(), event: Event? = null, time: QaTime? = null, unit: ReadingScope? = null,
                fact: String? = null, terms: List<String> = emptyList(), star: StarId? = null, role: PalaceRole? = null,
                refuse: String? = null, reframe: String? = null, yesNo: Boolean = false) =
            QaParse(question, n, intent, topics, event, time, unit, fact, terms, star, role, refuse, reframe, yesNo, caution, relative, verify)

        if (n.isBlank()) return res(QaIntent.GREETING)
        if (CRISIS.containsMatchIn(n)) return res(QaIntent.CRISIS)
        if (REF_EM_BE.containsMatchIn(n)) return res(QaIntent.REFUSE, refuse = "qa.tu_choi.em_be")
        if (REF_SONG_LAU.containsMatchIn(n)) return res(QaIntent.REFUSE, listOf(Topic.SUC_KHOE), refuse = "qa.tu_choi.song_lau")
        if (REF_SO.containsMatchIn(n)) return res(QaIntent.REFUSE, listOf(Topic.TAI_CHINH), refuse = "qa.tu_choi.so")
        if (REF_DICH_VU.containsMatchIn(n)) return res(QaIntent.REFUSE, refuse = "qa.tu_choi.dich_vu")
        if (REF_NGUOI_KHAC.containsMatchIn(n)) return res(QaIntent.REFUSE, refuse = "qa.tu_choi.nguoi_khac")

        if (OUT_OF_DOMAIN.containsMatchIn(n) && TOPIC_RX.none { (_, r) -> r.containsMatchIn(n) } && EVENT_RX.none { (_, r) -> r.containsMatchIn(n) })
            return res(QaIntent.REFUSE, refuse = "qa.tu_choi.ngoai")

        val defQ = DEF_Q.containsMatchIn(n)
        var topics = TOPIC_RX.mapNotNull { (t, r) -> r.find(n)?.let { m -> t to m.range.first } }.sortedBy { it.second }.map { it.first }.distinct()
        var event = EVENT_RX.mapNotNull { (e, r) -> r.find(n)?.let { m -> e to m.range.first } }.minByOrNull { it.second }?.first
        val time = parseTime(n, lower, chart, ctx)
        val star = findStar(n)
        val role = ROLE_RX.firstOrNull { (_, r) -> r.containsMatchIn(n) }?.first
        val terms = TERM_RX.filter { (_, r) -> r.containsMatchIn(n) }.map { it.first }.distinct()
        val yesNo = YESNO_Q.containsMatchIn(n)
        relative = RELATIVE.find(n)?.groupValues?.get(1)
        caution = CAUTION_Q.containsMatchIn(n) && !rx("tot|thuan|may man|hanh thong|phat|thanh cong|thang tien|giau").containsMatchIn(n)
        val reframe = when {
            RF_HON_NHAN.containsMatchIn(n) -> "qa.chuyen.hon_nhan"
            RF_BENH.containsMatchIn(n) -> "qa.chuyen.benh"
            RF_PHAP_LY.containsMatchIn(n) -> "qa.chuyen.phap_ly"
            RF_TAI_NAN.containsMatchIn(n) -> "qa.chuyen.tai_nan"
            relative != null -> "qa.chuyen.nguoi_than"
            else -> null
        }
        if (relative != null) {
            // Lá số đang mở là của người hỏi: việc riêng của người thân không có sự kiện trên lá số này.
            event = null
            topics = listOf(if (relative in setOf("vo", "chong", "nguoi yeu", "ban trai", "ban gai")) Topic.TINH_DUYEN else Topic.GIA_DAO)
        }
        if (event != null && event.topic !in topics) topics = listOf(event.topic) + topics
        when (reframe) {
            "qa.chuyen.hon_nhan" -> if (Topic.TINH_DUYEN !in topics) topics = listOf(Topic.TINH_DUYEN) + topics
            "qa.chuyen.benh" -> if (Topic.SUC_KHOE !in topics) topics = listOf(Topic.SUC_KHOE) + topics
            "qa.chuyen.phap_ly" -> if (Topic.PHAP_LY !in topics) topics = listOf(Topic.PHAP_LY) + topics
            "qa.chuyen.tai_nan" -> if (Topic.SUC_KHOE !in topics) topics = listOf(Topic.SUC_KHOE) + topics
        }

        val mine = rx("cua toi|cua minh|cua em|toi|minh").containsMatchIn(n)
        val factKey = factOf(n, star, role, terms)
        if (factKey != null && (mine || !defQ)) return res(QaIntent.FACT, topics, time = time, fact = factKey, star = star, role = role)

        // Giải nghĩa: sao, cung, thuật ngữ.
        if (defQ) {
            val hoaTerm = terms.firstOrNull { it.startsWith("hoa_") || it == "tu_hoa" }
            if (hoaTerm != null) return res(QaIntent.DEFINITION, terms = terms.filter { it.startsWith("hoa_") || it == "tu_hoa" })
            val specific = terms.filter { it !in setOf("menh", "han", "diem", "cuc") }
            val explicitStar = star != null && rx("sao " + normalize(star.vn)).containsMatchIn(n)
            if (star != null && (explicitStar || specific.isEmpty())) return res(QaIntent.STAR, star = star)
            if (role != null && role != PalaceRole.MENH) return res(QaIntent.DEFINITION, terms = listOf("cung.${role.name}"), role = role)
            if (terms.isNotEmpty() && (topics.isEmpty() || terms.any { it !in setOf("menh", "han", "diem", "cuc") })) return res(QaIntent.DEFINITION, terms = defTerms(terms))
        }

        // Dữ kiện lá số.
        if (factKey != null) return res(QaIntent.FACT, topics, time = time, fact = factKey, star = star, role = role)

        if (RF_HOP_TUOI.containsMatchIn(n)) return res(QaIntent.COMPARE, topics, reframe = "qa.chuyen.hop_tuoi")

        // Việc đã xảy ra ở một năm đã qua (“đã cưới năm 2015”) → đối chiếu năm đó và các năm lân cận với lá số.
        val pastY = time?.lunarYear?.takeIf { time.scope == ReadingScope.NAM && it < ctx.lunarYear && it >= chart.lunar.year }
        if (event != null && pastY != null && VERIFY_Q.containsMatchIn(n)) {
            verify = true
            return res(QaIntent.TIMING, topics, event, time, ReadingScope.NAM, yesNo = yesNo)
        }
        val timing = TIMING_Q.containsMatchIn(n)
        if (timing && (event != null || topics.isNotEmpty() || RF_CHON_NGAY.containsMatchIn(n) || OVERALL_Q.containsMatchIn(n) ||
                Regex("(?<![a-z])(?:tot|thuan|dep|hop|nen)(?![a-z])").containsMatchIn(n))) {
            val unit = when {
                Regex("(?<![a-z])(?:ngay nao|hom nao|gio nao|ngay gi|ngay tot|ngay dep)(?![a-z])").containsMatchIn(n) -> ReadingScope.NGAY
                Regex("(?<![a-z])thang nao(?![a-z])").containsMatchIn(n) -> ReadingScope.THANG
                else -> ReadingScope.NAM
            }
            return res(QaIntent.TIMING, topics, event, time, unit, reframe = if (unit == ReadingScope.NGAY) "qa.chuyen.chon_ngay" else reframe, yesNo = yesNo)
        }
        if (RF_CHON_NGAY.containsMatchIn(n)) return res(QaIntent.TIMING, topics, event, time, ReadingScope.NGAY, reframe = "qa.chuyen.chon_ngay")

        if (ADVICE_Q.containsMatchIn(n) && !(yesNo && event != null)) return res(QaIntent.ADVICE, topics, event, time, reframe = reframe)

        if (topics.isNotEmpty() || event != null || time != null || OVERALL_Q.containsMatchIn(n) || reframe != null) {
            if (topics.isEmpty() && time == null && event == null && reframe == null && GREET.containsMatchIn(n)) return res(QaIntent.GREETING)
            if (topics.isEmpty() && event == null && time == null && star != null) return res(QaIntent.STAR, star = star)
            if (topics.isEmpty() && event == null && time == null && role != null) return res(QaIntent.FACT, fact = "cung", role = role)
            return res(QaIntent.FORECAST, topics, event, time, reframe = reframe, yesNo = yesNo)
        }
        if (GREET.containsMatchIn(n)) return res(QaIntent.GREETING)
        if (OUT_OF_DOMAIN.containsMatchIn(n)) return res(QaIntent.REFUSE, refuse = "qa.tu_choi.ngoai")
        if (star != null) return res(QaIntent.STAR, star = star)
        if (role != null) return res(QaIntent.FACT, fact = "cung", role = role)
        if (terms.isNotEmpty() && terms.first() !in setOf("menh", "han", "diem")) return res(QaIntent.DEFINITION, terms = defTerms(terms))
        return res(QaIntent.UNKNOWN)
    }

    /** Bỏ thuật ngữ chung khi đã có thuật ngữ cụ thể hơn (“cung Mệnh” trong “Mệnh vô chính diệu là gì”). */
    private fun defTerms(terms: List<String>): List<String> {
        val specific = terms.filter { it !in setOf("menh", "han", "diem", "cuc") }
        val base = if (specific.isNotEmpty()) specific else terms
        return base.take(2)
    }

    private fun findStar(n: String): StarId? {
        var best: Pair<StarId, Int>? = null
        for (s in StarId.entries) {
            val name = normalize(s.vn)
            val multi = name.contains(' ') && name != "tu vi"
            val pat = if (multi) "(?:sao )?$name" else "sao $name"
            val m = rx(pat).find(n) ?: continue
            if (s.kind == StarKind.TU_HOA) continue
            val len = m.value.length
            if (best == null || len > best.second) best = s to len
        }
        return best?.first
    }

    private fun factOf(n: String, star: StarId?, role: PalaceRole?, terms: List<String>): String? {
        fun has(p: String) = rx(p).containsMatchIn(n)
        val where = "o dau|cung nao|o cung|nam o|tai cung|nam cung|dong o|vao cung|o cho nao|thuoc cung"
        return when {
            has("hom nay (?:la )?(?:ngay )?(?:bao nhieu|may|gi|thu may)|am lich hom nay|hom nay am|ngay am hom nay|hom nay ngay gi") -> "hom_nay"
            has("nam (?:nay|sau|toi|ngoai|\\d{4}) (?:la )?nam (?:gi|con gi)|can chi (?:cua )?nam|nam con gi") -> "can_chi"
            star != null && has(where) -> "sao"
            has("(?:sao|chinh tinh) (?:gi|nao) (?:thu|o|toa thu|chieu)? ?(?:cung )?menh|menh (?:cua )?(?:toi |minh )?(?:co|gom) (?:nhung )?(?:sao|chinh tinh)|" +
                "(?:cung )?menh (?:cua )?(?:toi |minh )?(?:o|nam|nam o|tai|dong) ?(?:dau|cung nao|cung gi|cho nao)|cung menh (?:co gi|co sao gi|the nao)") -> "menh"
            has("than (?:cu|o|nam|dong) (?:o )?(?:dau|cung nao|cho nao)|than cua toi|cung than (?:cua toi |o |nam )?(?:o )?(?:dau|cung nao)") -> "than"
            has("(?:toi |minh )?(?:la |thuoc |mang )?menh gi|menh (?:cua )?(?:toi|minh) (?:la )?gi|ban menh (?:cua )?(?:toi|minh)|menh (?:kim|moc|thuy|hoa|tho)(?: a| khong| phai khong)|nap am (?:cua )?(?:toi|minh)") -> "ban_menh"
            has("(?:toi |minh )?(?:thuoc )?cuc gi|cuc (?:cua )?(?:toi|minh)|cuc nao") -> "cuc"
            has("(?:menh chu|than chu) (?:cua )?(?:toi|minh)") -> "menh_chu"
            has("(?:toi |minh )?(?:bao nhieu|may) tuoi(?! thi)|tuoi am (?:cua )?(?:toi|minh)") -> "tuoi"
            has("(?:dai van|dai han) (?:cua )?(?:toi |minh )?(?:hien tai|bay gio|nay|dang di|dang)? ?(?:$where|la cung nao|la gi)|dang di (?:dai van|dai han) (?:nao|gi)|dai van thu may") -> "dai_han"
            has("(?:tieu han|luu thai tue|thai tue|luu nien(?: dai han)?) .{0,30}(?:$where)") -> "han_nam"
            role != null && has("co (?:nhung )?sao gi|co sao nao|co gi|nhung sao|chinh tinh|(?:$where)|sao nao") -> "cung"
            else -> null
        }
    }

    // ———————————————————————————— thời gian ————————————————————————————

    private fun numOf(s: String): Int? = s.toIntOrNull() ?: NUM_WORD[s]

    private fun parseTime(n: String, lower: String, chart: Chart, ctx: QaContext): QaTime? {
        val curY = ctx.lunarYear
        fun yLabel(y: Int): String {
            val cc = CanChi.ofYear(y)
            val age = chart.tuoiAm(y)
            return "năm $y (${cc.can.vn} ${cc.chi.vn}${if (age >= 1) ", $age tuổi âm" else ""})"
        }
        fun dLabel(d: LocalDate) = "ngày %02d/%02d/%d".format(d.dayOfMonth, d.monthValue, d.year)
        val lunarHint = Regex("(?<![a-z])(?:am lich|am|mung|ram)(?![a-z])").containsMatchIn(n)
        val solarHint = Regex("(?<![a-z])(?:duong lich|duong)(?![a-z])").containsMatchIn(n)

        // —— năm ——
        var year: Int? = null
        Regex("(?<![0-9/])(19\\d\\d|20\\d\\d|21\\d\\d|2200)(?![0-9/])").findAll(n).forEach { m ->
            val before = n.substring(0, m.range.first)
            if (!Regex("sinh(?: nam| vao nam| ngay| thang)? *$").containsMatchIn(before) && year == null) year = m.value.toInt()
        }
        if (year == null) year = when {
            Regex("(?<![a-z])(?:nam nay|nam hien tai|nam nay toi|ca nam nay)(?![a-z])").containsMatchIn(n) -> curY
            Regex("(?<![a-z])(?:nam sau|nam toi|sang nam|nam moi|nam ke tiep|nam tiep theo)(?![a-z])").containsMatchIn(n) -> curY + 1
            Regex("(?<![a-z])(?:nam ngoai|nam truoc|nam roi|nam vua roi|nam qua)(?![a-z])").containsMatchIn(n) -> curY - 1
            else -> null
        }
        Regex("(?<![a-z0-9])(\\d{1,2}|hai|ba|bon|nam|sau|bay|tam|chin) nam (?:nua|toi|sau)(?![a-z])").find(n)?.let { m ->
            val k = numOf(m.groupValues[1])
            if (k != null && k in 1..9 && year == null) year = curY + k
        }
        // năm theo can chi: “năm Đinh Mùi”, “năm Mùi”
        if (year == null) {
            val m = Regex("(?<![a-z])nam (?:(giap|at|binh|dinh|mau|ky|canh|tan|nham|quy) )?(ty|suu|dan|mao|thin|ngo|mui|than|dau|tuat|hoi)(?![a-z])").find(n)
            if (m != null) {
                val canN = m.groupValues[1]
                val chiN = m.groupValues[2]
                val chis = CHI_NORM.filter { it.first == chiN }.map { it.second }.filter { chi ->
                    when {
                        canN.isNotEmpty() -> CAN_NORM.first { it.first == canN }.second.ordinal % 2 == chi.ordinal % 2
                        chiN == "ty" && lower.contains("tỵ") -> chi == Chi.TI
                        chiN == "ty" && lower.contains("tý") -> chi == Chi.TY
                        else -> true
                    }
                }
                val can = CAN_NORM.firstOrNull { it.first == canN }?.second
                year = (curY..curY + 60).firstOrNull { y -> val cc = CanChi.ofYear(y); cc.chi in chis && (can == null || cc.can == can) }
            }
        }
        // tuổi: “năm 40 tuổi”, “tuổi 35”
        var ageFrom: Int? = null
        var ageTo: Int? = null
        Regex("(?<![a-z0-9])(?:tu |khoang |giai doan )?(\\d{1,2}) ?(?:den|toi|-|/) ?(\\d{1,3}) tuoi(?![a-z])").find(n)?.let { m ->
            ageFrom = m.groupValues[1].toInt(); ageTo = m.groupValues[2].toInt()
        }
        if (ageFrom == null && year == null) {
            Regex("(?<![a-z0-9])(?:(\\d{1,3}) tuoi|tuoi (\\d{1,3}))(?![a-z0-9])").find(n)?.let { m ->
                val age = (m.groupValues[1].ifEmpty { m.groupValues[2] }).toInt()
                if (age in 1..110) year = chart.lunar.year + age - 1
            }
        }
        when {
            Regex("(?<![a-z])(?:hau van|ve gia|tuoi gia|luc gia|cuoi doi|tuoi xe chieu|ve sau nay)(?![a-z])").containsMatchIn(n) -> { ageFrom = 55; ageTo = 110 }
            Regex("(?<![a-z])(?:tien van|tuoi tre|thoi tre|luc tre|thanh nien)(?![a-z])").containsMatchIn(n) -> { ageFrom = 1; ageTo = 30 }
            Regex("(?<![a-z])(?:trung van|trung nien)(?![a-z])").containsMatchIn(n) -> { ageFrom = 31; ageTo = 55 }
        }

        // —— ngày ——
        val today = ctx.today
        var date: LocalDate? = null
        Regex("(?<![0-9])(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?(?![0-9])").findAll(n).firstOrNull { m ->
            !Regex("sinh(?: ngay| vao| vao ngay| nam)? *$").containsMatchIn(n.substring(0, m.range.first))
        }?.let { m ->
            val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt()
            val explicitY = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it } ?: year
            val yy = explicitY ?: (if (lunarHint) curY else today.year)
            date = if (lunarHint) lunarToSolar(d, mo, yy) else runCatching { LocalDate.of(yy, mo, d) }.getOrNull()
            // Không nêu năm mà ngày đã qua → hiểu là lần tới.
            if (explicitY == null && date != null && date!! < today) date = if (lunarHint) lunarToSolar(d, mo, yy + 1) else runCatching { LocalDate.of(yy + 1, mo, d) }.getOrNull()
        }
        // Tết: “mùng 2 Tết”, “Tết năm sau”
        if (date == null) Regex("(?<![a-z])(?:(?:mung|mong) (\\d{1,2}) tet|(?:ngay )?tet(?: nguyen dan| am lich| am)?)(?![a-z])").find(n)?.let { m ->
            val d = m.groupValues[1].toIntOrNull() ?: 1
            val y0 = year ?: curY
            date = lunarToSolar(d, 1, y0)
            if (year == null && date != null && date!! < today) date = lunarToSolar(d, 1, y0 + 1)
        }
        if (date == null) Regex("(?<![a-z])(?:ngay|mung|mong) (\\d{1,2})(?: thang (\\d{1,2}|gieng|chap))?(?: (?:am|am lich))?(?: nam (\\d{4}))?(?![a-z0-9])").find(n)?.let { m ->
            val d = m.groupValues[1].toInt()
            val isLunar = lunarHint || n.substring(m.range.first).startsWith("mung") || n.substring(m.range.first).startsWith("mong")
            val moS = m.groupValues[2]
            val explicitY = m.groupValues[3].toIntOrNull() ?: year
            val yy = explicitY ?: (if (isLunar) curY else today.year)
            if (moS.isNotEmpty()) {
                val mo = moS.toIntOrNull() ?: MONTH_WORD[moS] ?: 1
                date = if (isLunar) lunarToSolar(d, mo, yy) else runCatching { LocalDate.of(yy, mo, d) }.getOrNull()
                if (explicitY == null && date != null && date!! < today) date = if (isLunar) lunarToSolar(d, mo, yy + 1) else runCatching { LocalDate.of(yy + 1, mo, d) }.getOrNull()
            } else if (isLunar) {
                date = lunarToSolar(d, ctx.lunarMonth, year ?: curY)
                if (year == null && date != null && date!! < today) date = if (ctx.lunarMonth == 12) lunarToSolar(d, 1, curY + 1) else lunarToSolar(d, ctx.lunarMonth + 1, curY)
            } else if (d in 1..31) {
                date = runCatching { LocalDate.of(today.year, today.monthValue, d) }.getOrNull()
            }
        }
        if (date == null) Regex("(?<![a-z])ram thang (nay|sau|toi)(?![a-z0-9])").find(n)?.let { m ->
            var mo = ctx.lunarMonth; var yy = curY
            if (m.groupValues[1] != "nay") { mo += 1; if (mo > 12) { mo = 1; yy += 1 } }
            date = lunarToSolar(15, mo, yy)
        }
        if (date == null) Regex("(?<![a-z])ram thang (\\d{1,2}|gieng|chap)(?![a-z0-9])").find(n)?.let { m ->
            val mo = m.groupValues[1].toIntOrNull() ?: MONTH_WORD[m.groupValues[1]] ?: 1
            date = lunarToSolar(15, mo, year ?: curY)
            if (year == null && date != null && date!! < today) date = lunarToSolar(15, mo, curY + 1)
        }
        if (date == null) date = when {
            Regex("(?<![a-z])(?:hom nay|bua nay|ngay hom nay|toi nay|sang nay|chieu nay)(?![a-z])").containsMatchIn(n) -> today
            Regex("(?<![a-z])(?:ngay mai|mai nay|sang mai|toi mai|chieu mai)(?![a-z])").containsMatchIn(n) -> today.plusDays(1)
            Regex("(?<![a-z])(?:ngay kia|ngay mot|mot nay)(?![a-z])").containsMatchIn(n) -> today.plusDays(2)
            Regex("(?<![a-z])hom qua(?![a-z])").containsMatchIn(n) -> today.minusDays(1)
            else -> null
        }
        if (date == null) Regex("(?<![a-z])(?<!con )(?<!lan )(?<!dua )(?<!nguoi )(?<!nam )(?:thu (2|3|4|5|6|7|hai|ba|tu|nam|sau|bay)|chu nhat)(?: (tuan sau|tuan toi|tuan nay|nay))?(?![a-z0-9])").find(n)?.let { m ->
            val w = m.groupValues[1]
            val dow = when (w) { "" -> 7; "2", "hai" -> 1; "3", "ba" -> 2; "4", "tu" -> 3; "5", "nam" -> 4; "6", "sau" -> 5; else -> 6 }
            var d = today
            while (d.dayOfWeek.value != dow) d = d.plusDays(1)
            if (m.groupValues[2] == "tuan sau" || m.groupValues[2] == "tuan toi") d = d.plusDays(7)
            date = d
        }
        date?.let { d -> return QaTime(ReadingScope.NGAY, LunarCalendar.solarToLunar(d, 7).year, null, d, label = dLabel(d)) }
        // tuần: “tuần này”, “tuần sau”, “cuối tuần”
        Regex("(?<![a-z])(cuoi tuan(?: nay| sau| toi)?|tuan nay|tuan sau|tuan toi|tuan ke tiep)(?![a-z])").find(n)?.let { m ->
            val w = m.groupValues[1]
            val dow = today.dayOfWeek.value
            val nextMon = today.plusDays((8 - dow).toLong())
            val (start, len) = when {
                w.startsWith("cuoi tuan") && (w.endsWith("sau") || w.endsWith("toi")) -> nextMon.plusDays(5) to 2
                w.startsWith("cuoi tuan") -> if (dow == 7) today to 1 else today.plusDays((6 - dow).toLong().coerceAtLeast(0)) to (if (dow == 6) 2 else 2)
                w == "tuan nay" -> today to (8 - dow)
                else -> nextMon to 7
            }
            val end = start.plusDays((len - 1).toLong())
            val label = "${if (w.startsWith("cuoi")) "cuối tuần" else if (w == "tuan nay") "tuần này" else "tuần sau"} (%02d/%02d–%02d/%02d)".format(start.dayOfMonth, start.monthValue, end.dayOfMonth, end.monthValue)
            return QaTime(ReadingScope.NGAY, LunarCalendar.solarToLunar(start, 7).year, null, start, label = label, days = len)
        }

        // —— tháng ——
        var month: Int? = null
        var monthYear = year ?: curY
        when {
            Regex("(?<![a-z])thang nay(?![a-z])").containsMatchIn(n) -> { month = ctx.lunarMonth; monthYear = curY }
            lower.contains("tháng sáu") -> { month = 6; if (year == null && 6 < ctx.lunarMonth) monthYear = curY + 1 }
            Regex("(?<![a-z])(?:thang sau|thang toi|thang ke tiep)(?![a-z])").containsMatchIn(n) -> {
                month = ctx.lunarMonth + 1; monthYear = curY
                if (month!! > 12) { month = 1; monthYear = curY + 1 }
            }
            Regex("(?<![a-z])thang truoc(?![a-z])").containsMatchIn(n) -> {
                month = ctx.lunarMonth - 1; monthYear = curY
                if (month!! < 1) { month = 12; monthYear = curY - 1 }
            }
            else -> {
                val m = Regex("(?<![a-z])thang (\\d{1,2}|gieng|chap|muoi mot|muoi hai|muoi|hai|ba|tu|bon|bay|tam|chin)(?:/(\\d{4}))?(?![a-z0-9])").find(n)
                if (m != null) {
                    val mo = m.groupValues[1].toIntOrNull() ?: MONTH_WORD[m.groupValues[1]]
                    if (mo != null && mo in 1..12) {
                        month = mo
                        val explicitY = m.groupValues[2].toIntOrNull() ?: year
                        explicitY?.let { monthYear = it }
                        if (solarHint) {
                            if (explicitY == null && mo < today.monthValue) monthYear = today.year + 1 else if (explicitY == null) monthYear = today.year
                            val l = LunarCalendar.solarToLunar(LocalDate.of(monthYear, mo, 15), 7)
                            month = l.month; monthYear = l.year
                        } else if (explicitY == null && mo < ctx.lunarMonth) {
                            // Tháng đã qua mà không nêu năm → tháng đó của năm sau.
                            monthYear = curY + 1
                        }
                    }
                }
            }
        }
        month?.let { mo ->
            val label = "tháng $mo âm lịch năm $monthYear"
            return QaTime(ReadingScope.THANG, monthYear, mo, label = label)
        }

        // —— đại vận ——
        val decadeRx = Regex("(?<![a-z])(dai van|dai han|van 10 nam|10 nam|muoi nam|thap nien)(?: (toi|sau|ke tiep|tiep theo|sap toi|truoc|vua qua|nay|hien tai))?(?![a-z])")
        decadeRx.find(n)?.let { m ->
            val mod = m.groupValues[2]
            val baseY = year ?: curY
            val period = LimitEngine.forYear(chart, baseY).daiHanPeriod
            val y = when {
                (m.groupValues[1] == "dai van" || m.groupValues[1] == "dai han") && mod in setOf("toi", "sau", "ke tiep", "tiep theo", "sap toi") ->
                    chart.lunar.year + period.ages.last
                mod in setOf("truoc", "vua qua") -> chart.lunar.year + period.ages.first - 2
                else -> baseY
            }
            val p = LimitEngine.forYear(chart, y).daiHanPeriod
            return QaTime(ReadingScope.DAI_VAN, y, label = "đại vận ${p.ages.first}–${p.ages.last} tuổi")
        }
        if (ageFrom != null && ageTo != null && ageTo!! - ageFrom!! <= 12 && ageFrom!! >= 1) {
            val y = chart.lunar.year + ageFrom!! - 1
            val p = LimitEngine.forYear(chart, y).daiHanPeriod
            return QaTime(ReadingScope.DAI_VAN, y, label = "đại vận ${p.ages.first}–${p.ages.last} tuổi")
        }
        if (ageFrom != null) {
            return QaTime(ReadingScope.TRON_DOI, null, ageFrom = ageFrom, ageTo = ageTo, label = "giai đoạn $ageFrom–${ageTo?.coerceAtMost(99)} tuổi")
        }
        year?.let { y -> return QaTime(ReadingScope.NAM, y, label = yLabel(y)) }

        // —— trọn đời ——
        if (Regex("(?<![a-z])(?:ca doi|tron doi|suot doi|cuoc doi|tron kiep|ca cuoc doi|so menh|van menh|so phan|tong quan la so|la so goc|ve lau dai|lau dai|ban chat|(?:co|la) so (?:giau|lam giau|giau sang|phu quy|lam quan|lam sep|lam chu|sung suong|huong phuc|an nhan|vat va|ngheo|kho|lan dan|xuat ngoai|di xa|lay vo|lay chong|muon con|hiem muon|dao hoa|nha cua|tien|phat)|so toi)(?![a-z])").containsMatchIn(n)) {
            return QaTime(ReadingScope.TRON_DOI, label = "trọn đời")
        }
        return null
    }

    private fun lunarToSolar(d: Int, m: Int, y: Int): LocalDate? = runCatching {
        LunarCalendar.lunarToSolar(LunarDate(d.coerceIn(1, 30), m.coerceIn(1, 12), y, false), 7)
    }.getOrNull()

    // ———————————————————————————— trả lời ————————————————————————————

    fun answer(chart: Chart, question: String, ctx: QaContext): QaAnswer {
        val q = question.trim().take(500)
        val parse = parse(q, chart, ctx)
        // Năm trước năm sinh của lá số: chưa có vận hạn (trước đây ném lỗi khi luận năm).
        if (parse.intent in setOf(QaIntent.FORECAST, QaIntent.ADVICE, QaIntent.TIMING) && (parse.time?.lunarYear ?: Int.MAX_VALUE) < chart.lunar.year &&
            parse.time?.scope in setOf(ReadingScope.NAM, ReadingScope.THANG, ReadingScope.NGAY))
            return simple(q, parse, "qa.truoc_nam_sinh", "Năm ${parse.time?.lunarYear} · trước năm sinh của lá số", suggestions(chart, ctx).take(4))
        return when (parse.intent) {
            QaIntent.CRISIS -> crisis(q, parse)
            QaIntent.REFUSE -> refuse(chart, q, parse, ctx)
            QaIntent.GREETING -> simple(q, parse, "qa.gioi_thieu", "Giới thiệu phần hỏi đáp", suggestions(chart, ctx))
            QaIntent.UNKNOWN -> simple(q, parse, "qa.chua_hieu", "Chưa rõ câu hỏi", suggestions(chart, ctx))
            QaIntent.DEFINITION -> definition(chart, q, parse, ctx)
            QaIntent.STAR -> starAnswer(chart, q, parse, ctx)
            QaIntent.FACT -> factAnswer(chart, q, parse, ctx)
            QaIntent.COMPARE -> simple(q, parse, "qa.chuyen.hop_tuoi", "Xem hợp nhau → So hai lá số",
                listOf("Tình duyên của tôi thế nào?", "Năm nay gia đạo thế nào?", "Truyền cung là gì?"))
            QaIntent.TIMING -> timing(chart, q, parse, ctx)
            QaIntent.ADVICE -> advice(chart, q, parse, ctx)
            QaIntent.FORECAST -> forecast(chart, q, parse, ctx)
        }
    }

    /** Câu gợi ý (chip) theo lá số và năm hiện tại. */
    fun suggestions(chart: Chart, ctx: QaContext): List<String> {
        val y = ctx.lunarYear
        val child = chart.tuoiAm(y) < 16
        return if (child) listOf(
            "Năm $y học tập của bé thế nào?", "Tính cách của bé thế nào?", "Năm nay sức khỏe của bé cần lưu ý gì?",
            "Tháng nào năm nay thuận nhất?", "Cung Mệnh có sao gì?", "Tuần Triệt là gì?",
        ) else listOf(
            "Năm $y công việc của tôi thế nào?", "Năm nào thuận cho chuyện cưới hỏi?", "Tôi nên lưu ý gì năm $y?",
            "Tháng nào năm nay thuận nhất?", "Năm ${y + 1} tài chính ra sao?", "Đại vận hiện tại của tôi thế nào?",
            "Cung Mệnh của tôi có sao gì?", "Ngày mai thế nào?", "Tuần Triệt là gì?",
        )
    }

    private fun textPara(key: String, kind: ParaKind = ParaKind.NOTE, topic: Topic? = null, title: String = "Hỏi đáp"): Paragraph? {
        val t = kb.texts[key] ?: return null
        val id = key.uppercase().replace('.', '_')
        // Lời của chính app (từ chối, chuyển hướng, hỗ trợ, ghi chú) không phải câu luận nên không gắn nhãn nháp.
        return Paragraph(t.text, 0, kind, topic, listOf(id), listOf(Why(id, title, t.text, emptyList(), t.source)), !t.reviewed && !key.startsWith("qa."))
    }

    private fun fact(text: String, id: String, title: String, detail: String, ev: List<String> = emptyList(), topic: Topic? = null, source: String = "Lá số (engine an sao)") =
        Paragraph(text, 0, ParaKind.NOTE, topic, listOf(id), listOf(Why(id, title, detail, ev, source)), false)

    private fun crisis(q: String, parse: QaParse): QaAnswer {
        val p = textPara("qa.khung_hoang", ParaKind.NOTE, null, "Hỗ trợ khi khủng hoảng")
        return QaAnswer(q, parse, "Mình nghe thấy bạn đang rất khó khăn", null, null, listOfNotNull(p), emptyList(), emptyList(), null, null)
    }

    private fun simple(q: String, parse: QaParse, key: String, understood: String, follow: List<String>): QaAnswer =
        QaAnswer(q, parse, understood, null, null, listOfNotNull(textPara(key)), emptyList(), follow, null, null)

    private fun refuse(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val y = ctx.lunarYear
        val follow = when (parse.refuse) {
            "qa.tu_choi.em_be" -> listOf("Năm nào thuận cho chuyện con cái?", "Năm $y gia đạo thế nào?")
            "qa.tu_choi.song_lau" -> listOf("Năm $y sức khỏe cần lưu ý gì?", "Năm nào nên chăm sóc sức khỏe kỹ hơn?")
            "qa.tu_choi.so" -> listOf("Năm $y tài chính thế nào?", "Tôi nên giữ tiền thế nào năm $y?")
            else -> suggestions(chart, ctx).take(4)
        }
        return QaAnswer(q, parse, "Ngoài phạm vi trả lời của app", null, null, listOfNotNull(textPara(parse.refuse ?: "qa.tu_choi.ngoai")), emptyList(), follow, null, null)
    }

    private fun palaceDesc(chart: Chart, role: PalaceRole): String {
        val p = chart.palace(role)
        val mains = p.chinhTinh.joinToString(", ") { s -> s.id.vn + (s.brightness?.let { " (${it.vn})" } ?: "") }
        return if (mains.isEmpty()) "vô chính diệu (mượn chính tinh cung ${chart.palace(p.chi.opposite).role.vn}: ${chart.palace(p.chi.opposite).chinhTinh.joinToString(", ") { it.id.vn }.ifEmpty { "không có" }})" else mains
    }

    private fun natalHoaAt(chart: Chart, chi: Chi): List<String> =
        listOf(StarId.HOA_LOC, StarId.HOA_QUYEN, StarId.HOA_KHOA, StarId.HOA_KY).filter { chart.starPositions[it] == chi }.map { it.vn }

    private fun definition(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val paras = ArrayList<Paragraph>()
        val y = ctx.lunarYear
        val lim = LimitEngine.forYear(chart, y)
        val names = ArrayList<String>()
        for (term in parse.terms) {
            if (term.startsWith("cung.")) {
                val role = PalaceRole.valueOf(term.removePrefix("cung."))
                textPara(term, ParaKind.NOTE, Topic.ofRole(role), "Ý nghĩa cung ${role.vn}")?.let { paras.add(it) }
                val p = chart.palace(role)
                paras.add(fact("Trong lá số của bạn, cung ${role.vn} ở ${p.chi.vn} (${p.can.vn} ${p.chi.vn}): chính tinh ${palaceDesc(chart, role)}.",
                    "FACT_CUNG_${role.name}", "Cung ${role.vn}", "Vị trí và chính tinh do engine an sao tính.", p.stars.map { it.toString() }, Topic.ofRole(role)))
                names.add("cung ${role.vn}")
                continue
            }
            val key = "qa.def.$term"
            val t = kb.texts[key] ?: continue
            paras.add(Paragraph(t.text, 0, ParaKind.NOTE, null, listOf("QA_DEF_${term.uppercase()}"), listOf(Why("QA_DEF_${term.uppercase()}", "Giải nghĩa", t.text, emptyList(), t.source)), !t.reviewed))
            names.add(TERM_LABEL[term] ?: term)
            val extra: Paragraph? = when (term) {
                "tuan", "triet" -> if (paras.none { "FACT_TUAN_TRIET" in it.ruleIds }) fact(
                    "Trong lá số của bạn: Tuần ở ${chart.tuan.first.vn}–${chart.tuan.second.vn} (cung ${chart.palace(chart.tuan.first).role.vn}, ${chart.palace(chart.tuan.second).role.vn}); " +
                        "Triệt ở ${chart.triet.first.vn}–${chart.triet.second.vn} (cung ${chart.palace(chart.triet.first).role.vn}, ${chart.palace(chart.triet.second).role.vn}).",
                    "FACT_TUAN_TRIET", "Tuần, Triệt của lá số", "Tuần theo can chi năm sinh, Triệt theo can năm sinh.") else null
                "hoa_loc", "hoa_quyen", "hoa_khoa", "hoa_ky" -> {
                    val sid = StarId.valueOf(term.uppercase())
                    chart.starPositions[sid]?.let { c -> fact("${sid.vn} gốc của bạn ở cung ${chart.palace(c).role.vn} (${c.vn}).", "FACT_${sid.name}", sid.vn, "Tứ Hóa theo can năm sinh (${chart.yearCan.vn}).") }
                }
                "tu_hoa" -> fact("Tứ Hóa gốc của bạn (can năm ${chart.yearCan.vn}): " + listOf(StarId.HOA_LOC, StarId.HOA_QUYEN, StarId.HOA_KHOA, StarId.HOA_KY).mapNotNull { s ->
                    chart.starPositions[s]?.let { "${s.vn} ở ${chart.palace(it).role.vn}" } }.joinToString(", ") + ".", "FACT_TU_HOA", "Tứ Hóa gốc", "Theo can năm sinh.")
                "menh" -> fact("Cung Mệnh của bạn ở ${chart.menh.vn}: chính tinh ${palaceDesc(chart, PalaceRole.MENH)}.", "FACT_MENH", "Cung Mệnh", "Engine an sao.")
                "than" -> fact("Thân của bạn cư ${chart.palace(chart.than).role.vn} (${chart.than.vn}).", "FACT_THAN", "Cung Thân", "Engine an sao.")
                "cuc", "ban_menh" -> fact("Lá số của bạn: ${chart.cuc.vn}, bản mệnh ${chart.banMenh.name} (hành ${chart.banMenh.element.vn}); ${chart.cucMenhRelation.vn}.", "FACT_CUC", "Cục, bản mệnh", "Engine an sao.")
                "menh_chu" -> fact("Mệnh chủ của bạn là ${chart.menhChu.vn}, Thân chủ là ${chart.thanChu.vn}.", "FACT_MENH_CHU", "Mệnh chủ, Thân chủ", "Engine an sao.")
                "dai_han" -> if (lim.beforeFirstDaiHan) fact("Năm $y bạn chưa vào đại hạn đầu (bắt đầu từ ${chart.cuc.number} tuổi âm).", "FACT_DAI_HAN", "Đại hạn", "Mục 8.1.")
                    else fact("Năm $y bạn đang đi đại hạn ${lim.daiHanPeriod.ages.first}–${lim.daiHanPeriod.ages.last} tuổi tại cung ${chart.palace(lim.daiHan).role.vn} (${lim.daiHan.vn}).", "FACT_DAI_HAN", "Đại hạn", "Mục 8.1.")
                "tieu_han", "luu_nien", "thai_tue", "nguyet_han" -> if (paras.none { "FACT_HAN_NAM" in it.ruleIds }) hanNam(chart, y) else null
                "vo_chinh_dieu" -> {
                    val vcd = chart.palaces.values.filter { it.isVoChinhDieu }.sortedBy { it.role.ordinal }
                    fact(if (vcd.isEmpty()) "Lá số của bạn không có cung vô chính diệu." else "Lá số của bạn có ${vcd.size} cung vô chính diệu: ${vcd.joinToString(", ") { "${it.role.vn} (${it.chi.vn})" }}.",
                        "FACT_VCD", "Cung vô chính diệu", "Cung không có chính tinh.")
                }
                else -> null
            }
            extra?.let { paras.add(it) }
        }
        if (paras.isEmpty()) return simple(q, parse, "qa.chua_hieu", "Chưa rõ thuật ngữ", suggestions(chart, ctx))
        return QaAnswer(q, parse, "Giải nghĩa: ${names.joinToString(", ")}", null, null, paras, emptyList(),
            listOf("Cung Mệnh của tôi có sao gì?", "Năm $y công việc của tôi thế nào?", "Đồng thanh tương ứng là gì?"),
            null, null)
    }

    private val TERM_LABEL = mapOf(
        "tu_vi" to "Tử Vi Đẩu Số", "menh" to "cung Mệnh", "than" to "cung Thân", "cuc" to "Cục", "ban_menh" to "bản mệnh", "chinh_tinh" to "chính tinh",
        "phu_tinh" to "phụ tinh", "luc_cat" to "lục cát tinh", "luc_sat" to "lục sát tinh", "tu_hoa" to "Tứ Hóa", "hoa_loc" to "Hóa Lộc", "hoa_quyen" to "Hóa Quyền",
        "hoa_khoa" to "Hóa Khoa", "hoa_ky" to "Hóa Kỵ", "tuan" to "Tuần", "triet" to "Triệt", "dai_han" to "đại hạn", "tieu_han" to "tiểu hạn",
        "luu_nien" to "lưu niên đại hạn", "thai_tue" to "Lưu Thái Tuế", "nguyet_han" to "nguyệt hạn, nhật hạn", "tam_phuong" to "tam phương tứ chính",
        "xung_chieu" to "xung chiếu", "giap" to "giáp cung", "nhi_hop" to "nhị hợp", "vo_chinh_dieu" to "vô chính diệu", "do_sang" to "độ sáng",
        "dong_thanh" to "đồng thanh tương ứng", "the" to "thế Tấn/Giữ/Thủ/Lùi", "diem" to "điểm 0–100", "thien_thoi" to "thiên thời", "sao_luu" to "sao lưu",
        "cach_cuc" to "cách cục", "truyen_cung" to "truyền cung, truyền tinh", "menh_chu" to "Mệnh chủ, Thân chủ", "han" to "vận hạn",
    )

    private fun hanNam(chart: Chart, y: Int): Paragraph {
        val l = LimitEngine.forYear(chart, y)
        fun at(c: Chi) = "${chart.palace(c).role.vn} (${c.vn})"
        val cc = CanChi.ofYear(y)
        val text = "Năm $y (${cc.can.vn} ${cc.chi.vn}): tiểu hạn tại ${at(l.tieuHan)} — chuyện diễn ra; " +
            (if (l.beforeFirstDaiHan) "" else "lưu niên đại hạn tại ${at(l.luuNienDaiHan)} — kết cục; đại hạn tại ${at(l.daiHan)} — nền 10 năm; ") +
            "Lưu Thái Tuế tại ${at(l.luuThaiTue)} — lĩnh vực được đánh thức."
        return fact(text, "FACT_HAN_NAM", "Các tầng vận năm $y", "Tiểu hạn = chuyện gì diễn ra; lưu niên đại hạn = kết cục; Lưu Thái Tuế = lĩnh vực bị đánh thức (mục 8.5).", source = "Prompt mục 8")
    }

    private fun starAnswer(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val sid = parse.star ?: return simple(q, parse, "qa.chua_hieu", "Chưa rõ sao", suggestions(chart, ctx))
        val card = composer.starCard(chart, sid)
            ?: return QaAnswer(q, parse, "Sao ${sid.vn}", null, null,
                listOf(fact("Sao ${sid.vn} không có trong lá số gốc của bạn (có thể là sao lưu, chỉ xuất hiện theo năm).", "FACT_SAO_${sid.name}", "Sao ${sid.vn}", "Không có trong vị trí sao của lá số.")),
                emptyList(), suggestions(chart, ctx).take(3), null, null)
        val paras = ArrayList<Paragraph>()
        val meta = card.meta?.let { m -> " Chòm ${m.dau}, ${m.hanh}, hóa khí ${m.hoaKhi}${if (m.dacTinh.isNotBlank()) " — ${m.dacTinh}" else ""}." } ?: ""
        paras.add(fact("Sao ${sid.vn} (${card.kind}) ở cung ${card.role} (${card.chi.vn})${card.brightness?.let { ", độ sáng $it" } ?: ""}.$meta" +
            (if (card.natalHoa.isNotEmpty()) " Tứ Hóa gốc: ${card.natalHoa.joinToString(", ")}." else ""),
            "FACT_SAO_${sid.name}", "Thẻ sao ${sid.vn}", "Vị trí, độ sáng do engine an sao tính; chòm, hành, hóa khí theo bảng thuộc tính 14 sao.", source = card.meta?.source ?: "Lá số"))
        paras += card.meaning.take(2)
        card.patterns.take(2).forEach { w ->
            paras.add(Paragraph("Cách cục liên quan: ${w.title} — ${w.detail}", 0, ParaKind.NOTE, null, listOf(w.id), listOf(w), true))
        }
        return QaAnswer(q, parse, "Thẻ sao ${sid.vn}", null, null, paras, emptyList(),
            listOf("Cung ${card.role} của tôi có sao gì?", "Năm ${ctx.lunarYear} công việc của tôi thế nào?", "Độ sáng của sao là gì?"), null, null)
    }

    private fun factAnswer(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val y = parse.time?.lunarYear ?: ctx.lunarYear
        val paras = ArrayList<Paragraph>()
        var understood = "Dữ kiện lá số"
        var link: QaLink? = null
        val follow = ArrayList<String>()
        when (parse.fact) {
            "menh" -> {
                understood = "Dữ kiện lá số: cung Mệnh"
                val p = chart.palace(PalaceRole.MENH)
                val hoa = natalHoaAt(chart, p.chi)
                paras.add(fact("Cung Mệnh của bạn ở ${p.chi.vn} (${p.can.vn} ${p.chi.vn}). Chính tinh: ${palaceDesc(chart, PalaceRole.MENH)}." +
                    (if (hoa.isNotEmpty()) " Tứ Hóa gốc tại Mệnh: ${hoa.joinToString(", ")}." else "") +
                    " Thân cư ${chart.palace(chart.than).role.vn} (${chart.than.vn}).", "FACT_MENH", "Cung Mệnh", "Engine an sao.", p.stars.map { it.toString() }, Topic.TINH_CACH))
                life(chart, ctx).summary.firstOrNull { it.topic == Topic.TINH_CACH }?.let { paras.add(it) }
                analyst.bookPara(chart, Topic.TINH_CACH, PalaceRole.MENH)?.let { paras.add(it) }
                follow += listOf("Tính cách của tôi thế nào?", "Thân cư ${chart.palace(chart.than).role.vn} nghĩa là gì?") +
                    (p.chinhTinh.firstOrNull()?.let { listOf("Sao ${it.id.vn} nghĩa là gì?") } ?: emptyList())
                link = QaLink(ReadingScope.TRON_DOI, null, null, null, "Xem luận trọn đời")
            }
            "than" -> {
                understood = "Dữ kiện lá số: cung Thân"
                paras.add(fact("Thân của bạn cư ${chart.palace(chart.than).role.vn} tại ${chart.than.vn}: chính tinh ${palaceDesc(chart, chart.palace(chart.than).role)}.", "FACT_THAN", "Cung Thân", "Engine an sao."))
                life(chart, ctx).summary.firstOrNull { it.ruleIds.any { id -> id.startsWith("TS_THAN_CU_") } }?.let { paras.add(it) }
                textPara("qa.def.than")?.let { paras.add(it) }
                follow += listOf("Cung Mệnh của tôi có sao gì?", "Tính cách của tôi thế nào?")
            }
            "ban_menh", "cuc" -> {
                understood = "Dữ kiện lá số: bản mệnh, Cục"
                paras.add(fact("Bản mệnh của bạn là ${chart.banMenh.name} (hành ${chart.banMenh.element.vn}), năm sinh ${chart.yearCan.vn} ${chart.yearChi.vn}. " +
                    "Lá số thuộc ${chart.cuc.vn}; ${chart.cucMenhRelation.vn}.", "FACT_CUC", "Bản mệnh, Cục", "Nạp âm năm sinh; Cục theo can năm và cung Mệnh."))
                textPara(if (parse.fact == "cuc") "qa.def.cuc" else "qa.def.ban_menh")?.let { paras.add(it) }
                follow += listOf("Cung Mệnh của tôi có sao gì?", "Đại vận hiện tại của tôi thế nào?")
            }
            "menh_chu" -> {
                understood = "Dữ kiện lá số: Mệnh chủ, Thân chủ"
                paras.add(fact("Mệnh chủ của bạn là ${chart.menhChu.vn}, Thân chủ là ${chart.thanChu.vn}.", "FACT_MENH_CHU", "Mệnh chủ, Thân chủ", "Mệnh chủ theo chi cung Mệnh, Thân chủ theo chi năm sinh."))
                textPara("qa.def.menh_chu")?.let { paras.add(it) }
            }
            "tuoi" -> {
                understood = "Tuổi âm năm $y"
                val cc = CanChi.ofYear(y)
                val age = chart.tuoiAm(y)
                paras.add(fact(if (age >= 1) "Năm $y (${cc.can.vn} ${cc.chi.vn}) bạn $age tuổi âm (tuổi mụ)." else "Năm $y bạn chưa sinh.", "FACT_TUOI", "Tuổi âm", "Tuổi âm = năm âm − năm sinh âm + 1."))
                follow += listOf("Năm $y của tôi thế nào?")
            }
            "can_chi" -> {
                val cc = CanChi.ofYear(y)
                understood = "Can chi năm $y"
                paras.add(fact("Năm âm lịch $y là năm ${cc.can.vn} ${cc.chi.vn}.", "FACT_CAN_CHI", "Can chi năm", "Theo lịch âm Việt Nam (thuật toán Hồ Ngọc Đức).", source = "Prompt mục 4"))
                follow += listOf("Năm $y của tôi thế nào?")
            }
            "hom_nay" -> {
                understood = "Ngày âm lịch hôm nay"
                val d = ctx.today
                val l = ctx.todayLunar
                val dc = LimitEngine.forDate(chart, d).dayCanChi
                val cc = CanChi.ofYear(l.year)
                paras.add(fact("Hôm nay %02d/%02d/%d là ngày ${l.day}/${l.month}${if (l.isLeap) " nhuận" else ""} âm lịch năm ${cc.can.vn} ${cc.chi.vn}".format(d.dayOfMonth, d.monthValue, d.year) +
                    (dc?.let { ", ngày ${it.can.vn} ${it.chi.vn}" } ?: "") + ".", "FACT_HOM_NAY", "Lịch âm hôm nay", "Lịch âm Việt Nam, múi giờ UTC+7.", source = "Prompt mục 4"))
                composer.day(chart, d, ctx.expert).summary.firstOrNull()?.let { paras.add(it) }
                link = QaLink(ReadingScope.NGAY, l.year, null, d, "Xem chi tiết ngày")
                follow += listOf("Ngày mai thế nào?", "Ngày nào trong tháng này tốt?")
            }
            "dai_han" -> {
                val l = LimitEngine.forYear(chart, y)
                understood = "Đại vận năm $y"
                if (l.beforeFirstDaiHan) {
                    paras.add(fact("Năm $y bạn chưa vào đại hạn đầu; đại hạn đầu bắt đầu từ ${chart.cuc.number} tuổi âm tại cung Mệnh.", "FACT_DAI_HAN", "Đại hạn", "Mục 8.1: đại hạn đầu ở Mệnh, khởi từ tuổi bằng số Cục.", source = "Prompt mục 8"))
                } else {
                    val p = l.daiHanPeriod
                    val d = decadeR(chart, y, ctx)
                    paras.add(fact("Năm $y bạn đang đi đại vận ${p.ages.first}–${p.ages.last} tuổi tại cung ${chart.palace(p.palace).role.vn} (${p.palace.vn}, ${p.canChi.can.vn} ${p.canChi.chi.vn}); " +
                        "thiên thời: ${p.thienThoi.vn} — ${p.thienThoi.label.lowercase()}. Điểm 10 năm ${d.score}/100, thế ${d.stance.vn}.",
                        "FACT_DAI_HAN", "Đại vận", "Cung đại hạn, can chi, thiên thời (mục 8.1); điểm đại vận (mục 9).", source = "Prompt mục 8, 9"))
                    d.summary.drop(1).firstOrNull { it.kind != ParaKind.NOTE }?.let { paras.add(it) }
                    link = QaLink(ReadingScope.DAI_VAN, y, null, null, "Xem luận đại vận")
                }
                follow += listOf("Đại vận tới của tôi thế nào?", "Năm $y công việc của tôi thế nào?")
            }
            "han_nam" -> {
                understood = "Các tầng vận năm $y"
                paras.add(hanNam(chart, y))
                link = QaLink(ReadingScope.NAM, y, null, null, "Xem luận năm $y")
                follow += listOf("Năm $y của tôi thế nào?", "Lưu Thái Tuế là gì?")
            }
            "sao" -> return starAnswer(chart, q, parse.copy(intent = QaIntent.STAR), ctx)
            else -> {
                val role = parse.role ?: PalaceRole.MENH
                val p = chart.palace(role)
                understood = "Dữ kiện lá số: cung ${role.vn}"
                val minor = p.stars.filter { it.kind != StarKind.CHINH_TINH && it.kind != StarKind.TU_HOA }.map { it.toString() }
                val hoa = natalHoaAt(chart, p.chi)
                paras.add(fact("Cung ${role.vn} của bạn ở ${p.chi.vn} (${p.can.vn} ${p.chi.vn}). Chính tinh: ${palaceDesc(chart, role)}." +
                    (if (hoa.isNotEmpty()) " Tứ Hóa gốc: ${hoa.joinToString(", ")}." else "") +
                    (if (minor.isNotEmpty()) " Phụ tinh: ${minor.take(10).joinToString(", ")}${if (minor.size > 10) "…" else ""}." else "") +
                    (if (p.hasTuan || p.hasTriet) " Có ${listOfNotNull("Tuần".takeIf { p.hasTuan }, "Triệt".takeIf { p.hasTriet }).joinToString(", ")} án ngữ." else ""),
                    "FACT_CUNG_${role.name}", "Cung ${role.vn}", "Engine an sao.", p.stars.map { it.toString() }, Topic.ofRole(role)))
                life(chart, ctx).palaces.firstOrNull { it.id == "palace_${role.name}" }?.paragraphs?.filter { it.kind != ParaKind.NOTE }?.take(2)?.let { paras += it }
                analyst.bookPara(chart, Topic.ofRole(role), role)?.let { paras.add(it) }
                link = QaLink(ReadingScope.TRON_DOI, null, null, null, "Xem 12 cung trong luận trọn đời")
                follow += listOf("Năm $y ${SHORT[Topic.ofRole(role)]} thế nào?", "Cung ${role.vn} là gì?")
            }
        }
        return QaAnswer(q, parse, understood, null, null, paras, emptyList(), follow.distinct().take(3), link, null)
    }

    // ———————————————————————————— luận theo chủ đề ————————————————————————————

    /** Chọn đoạn trong một khối chủ đề: câu mở, câu sự kiện (đúng sự kiện được hỏi nếu có), rồi câu luận khác. */
    private fun pick(s: Section, event: Event?, max: Int): List<Paragraph> {
        val lead = s.paragraphs.filter { it.kind == ParaKind.LEAD }
        val evs = s.paragraphs.filter { it.kind == ParaKind.EVENT && (event == null || "EVENT_${event.name}" in it.ruleIds) }
        val rest = s.paragraphs.filter { it.kind == ParaKind.PLAIN || it.kind == ParaKind.WEIGH }
        val notes = s.paragraphs.filter { it.kind == ParaKind.NOTE }
        return (lead + evs + rest + notes).distinct().take(max)
    }

    private fun prefixed(p: Paragraph, prefix: String) = p.copy(text = prefix + p.text.replaceFirstChar { it.lowercase() })

    private fun adviceFor(r: Reading, topics: List<Topic>, max: Int = 3): List<AdviceItem> = adviceFrom(r.advice, topics, max)

    private fun adviceFrom(all: List<AdviceItem>, topics: List<Topic>, max: Int = 3): List<AdviceItem> {
        val byTopic = if (topics.isEmpty()) emptyList() else all.filter { it.topic in topics }
        val pool = if (byTopic.size >= 2) byTopic else (byTopic + all).distinct()
        val nen = pool.filter { it.type == AdviceType.NEN }.take(2)
        val khong = pool.filter { it.type == AdviceType.KHONG_NEN }.take(1)
        val luuY = pool.filter { it.type == AdviceType.LUU_Y }.take(1)
        return (nen + khong + luuY).take(max)
    }

    private fun childGate(chart: Chart, y: Int, topics: List<Topic>): Pair<List<Topic>, Paragraph?> {
        if (chart.tuoiAm(y) >= 16) return topics to null
        val kept = topics.filter { it in CHILD_TOPICS }
        return kept to (if (kept.size < topics.size || topics.isEmpty()) textPara("qa.note.tre_nho") else null)
    }

    private fun eventLine(chart: Chart, ev: Event, y: Int, ctx: QaContext): Paragraph? {
        val dg = yData(chart, y, ctx)
        if (dg.events.any { it.event == ev }) return null
        if (dg.child && ev !in setOf(Event.DI_CHUYEN, Event.SUC_KHOE)) return null
        val ts = dg.topics[ev.topic]?.score
        return Paragraph("Năm $y chưa thấy tầng vận nào cùng chỉ về ${ev.vn}" + (ts?.let { "; điểm ${ev.topic.vn.lowercase()} cả năm $it/100." } ?: "."),
            0, ParaKind.NOTE, ev.topic, listOf("EVENT_${ev.name}_KHONG"),
            listOf(Why("EVENT_${ev.name}_KHONG", "Đồng thanh tương ứng", "Không tầng nào (đại hạn, lưu niên đại hạn, tiểu hạn, Lưu Thái Tuế) có luật sự kiện ${ev.short} kích hoạt trong năm $y.", emptyList(), "Prompt mục 9")), false)
    }

    /** Chủ đề không có điểm theo năm (tính cách, phúc đức) chỉ đọc ở lá số gốc; nhà đất xem kèm tài chính của giai đoạn. */
    private fun lifeParas(chart: Chart, ctx: QaContext, t: Topic, max: Int): List<Paragraph> =
        life(chart, ctx).sections.firstOrNull { it.topic == t }?.let { s -> pick(s, null, max).mapIndexed { i, p -> if (i == 0) prefixed(p, "Lá số gốc — ") else p } } ?: emptyList()

    private fun periodTopic(t: Topic): Topic? = if (t in TIMELESS) null else YEAR_PROXY[t] ?: t

    private fun topicLabel(topics: List<Topic>): String =
        if (topics.isEmpty()) "tổng quan" else topics.joinToString(", ") { t -> t.vn.lowercase() + (YEAR_PROXY[t]?.let { " (xem kèm ${SHORT[it]})" } ?: "") }

    /** Đoạn luận một chủ đề trong một bản luận theo giai đoạn; chủ đề thiếu thì lấy lá số gốc. */
    private fun periodParas(chart: Chart, ctx: QaContext, r: Reading, t: Topic, event: Event?, max: Int, prefix: String? = null): List<Paragraph> {
        val out = ArrayList<Paragraph>()
        val own = r.sections.firstOrNull { it.topic == t }
        if (own != null) {
            out += pick(own, event, max).mapIndexed { i, p -> if (i == 0 && prefix != null) prefixed(p, prefix) else p }
            return out
        }
        out += lifeParas(chart, ctx, t, if (max > 2) 2 else 1)
        val pt = periodTopic(t)
        if (pt != null && pt != t) {
            r.sections.firstOrNull { it.topic == pt }?.paragraphs?.firstOrNull { it.kind == ParaKind.LEAD }?.let { out.add(prefixed(it, "${SHORT[pt]!!.replaceFirstChar { c -> c.uppercase() }} cùng giai đoạn — ")) }
            textPara("qa.note.nha_dat", topic = t)?.let { out.add(it) }
        }
        return out
    }

    private fun followFor(t: Topic?, y: Int, curY: Int): List<String> = when {
        t == null -> listOf("Tôi nên lưu ý gì năm $y?", "Tháng nào năm $y thuận nhất?")
        t in TIMELESS -> listOf("Năm $curY công việc thế nào?", "Cung Mệnh của tôi có sao gì?")
        else -> listOf("Tháng nào năm $y thuận cho ${SHORT[t]}?", "Năm ${y + 1} ${SHORT[t]} thế nào?")
    }

    private fun forecast(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val curY = ctx.lunarYear
        val topics = parse.topics.take(2)
        // Chỉ hỏi tính cách, phúc đức mà không nêu thời gian → luận trọn đời.
        val time = parse.time ?: if (topics.isNotEmpty() && topics.all { it in TIMELESS }) QaTime(ReadingScope.TRON_DOI, label = "lá số gốc (trọn đời)") else null
        val paras = ArrayList<Paragraph>()
        parse.reframe?.let { textPara(it) }?.let { paras.add(it) }
        var score: Int? = null
        var stance: Stance? = null
        var advice: List<AdviceItem> = emptyList()
        var link: QaLink? = null
        val follow = ArrayList<String>()
        val two = topics.size > 1
        val tLabel = topicLabel(topics).replaceFirstChar { it.uppercase() }
        val understood: String
        when (time?.scope) {
            null, ReadingScope.NAM -> {
                // Năm (không nêu thời gian = năm nay): luận đoán qua 5 tầng vận hạn trên đúng lá số này.
                val y = time?.lunarYear ?: curY
                val (tp, childNote) = childGate(chart, y, topics)
                val d = yData(chart, y, ctx)
                understood = "$tLabel · ${time?.label ?: "năm $curY (${CanChi.ofYear(curY).vn})"}"
                childNote?.let { paras.add(it) }
                val t0 = tp.firstOrNull()
                val ev = parse.event?.takeIf { e -> t0 == null || e.topic == t0 || tp.contains(e.topic) }
                val r = analyst.year(chart, d, t0, ev, parse.relative)
                paras += r.paragraphs
                tp.getOrNull(1)?.let { t1 ->
                    paras += analyst.year(chart, d, t1, null, parse.relative, compact = true).paragraphs.map { p -> p.copy(title = "${SHORT[t1]!!.replaceFirstChar { c -> c.uppercase() }} · ${p.title ?: ""}") }
                }
                val yr = yearR(chart, y, ctx)
                if (t0 == null) paras.add(monthsLine(yr, y))
                score = r.score; stance = Stance.of(r.score)
                advice = adviceFor(yr, tp.mapNotNull { periodTopic(it) })
                link = QaLink(ReadingScope.NAM, y, null, null, "Xem luận năm $y")
                follow += followFor(t0, y, curY)
                parse.event?.let { follow.add(0, "Năm nào thuận cho ${it.short}?") }
                if (time != null) follow += "Tôi nên lưu ý gì năm $y?"
            }
            ReadingScope.TRON_DOI -> {
                val ld = lData(chart, ctx)
                val t0 = topics.firstOrNull()
                understood = "${(if (topics.isEmpty()) "tổng quan" else topics.joinToString(", ") { it.vn.lowercase() }).replaceFirstChar { it.uppercase() }} · ${time.label}"
                val periods = LimitEngine.daiHanPeriods(chart).filter { it.ages.first <= 84 }
                val decades = periods.map { pd ->
                    val dd = dData(chart, chart.lunar.year + pd.ages.first - 1, ctx)
                    Triple(pd.ages, pd.palace, t0?.let { dd.topics[it]?.score } ?: dd.score)
                }
                val r = analyst.life(chart, ld, t0, parse.relative, decades, chart.tuoiAm(curY))
                paras += r.paragraphs
                if (time.ageFrom != null) {
                    val from = time.ageFrom; val to = time.ageTo ?: 110
                    val ds = decades.filter { it.first.last >= from && it.first.first <= to }
                    if (ds.isNotEmpty()) paras.add(Paragraph(
                        "Trong ${time.label}: " + ds.joinToString("; ") { (ages, c, sc) -> "${ages.first}–${ages.last} tuổi, đại hạn tại ${chart.palace(c).role.vn} (${c.vn}) ${analyst.shortStars(chart, c)}: $sc/100" } + ".",
                        0, ParaKind.NOTE, t0, listOf("TIMELINE_DAI_VAN"), listOf(Why("TIMELINE_DAI_VAN", "Đại vận trong khoảng tuổi được hỏi", "Điểm từng đại vận: gốc 0,25 + đại hạn 0,75 (mục 9).", ds.map { "${it.first.first}–${it.first.last}: ${it.third}" }, "Prompt mục 9")), false))
                }
                score = r.score; stance = Stance.of(r.score)
                advice = adviceFrom(ld.advice, topics)
                link = QaLink(ReadingScope.TRON_DOI, null, null, null, "Xem luận trọn đời")
                if (t0 != null && t0 !in TIMELESS) follow += "Năm $curY ${SHORT[t0]} thế nào?"
                follow += listOf("Đại vận hiện tại của tôi thế nào?", "Cung Mệnh của tôi có sao gì?")
                if (t0 in TIMELESS) follow += "Năm $curY công việc thế nào?"
            }
            ReadingScope.DAI_VAN -> {
                val y = time.lunarYear ?: curY
                val dd = dData(chart, y, ctx)
                val t0 = topics.firstOrNull()
                understood = "$tLabel · ${time.label}"
                val l = dd.limits!!
                val y1 = chart.lunar.year + l.daiHanPeriod.ages.first - 1
                val years = if (l.beforeFirstDaiHan) emptyList() else (y1..y1 + 9).map { yy ->
                    val yd = yData(chart, yy, ctx)
                    yy to (t0?.let { yd.topics[it]?.score } ?: yd.score)
                }
                val r = analyst.decade(chart, dd, t0, parse.relative, years)
                paras += r.paragraphs
                score = r.score; stance = Stance.of(r.score)
                advice = adviceFor(decadeR(chart, y, ctx), topics.mapNotNull { periodTopic(it) })
                link = QaLink(ReadingScope.DAI_VAN, y, null, null, "Xem luận đại vận")
                t0?.let(::periodTopic)?.let { t -> follow += "Năm nào trong đại vận này thuận cho ${SHORT[t]}?" }
                follow += listOf("Đại vận tới của tôi thế nào?", "Năm $curY của tôi thế nào?")
            }
            ReadingScope.THANG -> {
                val y = time.lunarYear ?: curY
                val mo = time.month ?: ctx.lunarMonth
                val yr = yearR(chart, y, ctx)
                understood = "$tLabel · ${time.label}"
                val (tp, childNote) = childGate(chart, y, topics)
                childNote?.let { paras.add(it) }
                val ms = yr.months.firstOrNull { it.month == mo }?.score ?: yr.score
                val r = analyst.month(chart, yData(chart, y, ctx), LimitEngine.forMonth(chart, y, mo), ms, tp.firstOrNull(), parse.relative)
                paras += r.paragraphs
                val mr = cached(chart, Triple("month", y, mo)) { composer.month(chart, y, mo, ctx.expert) }
                score = ms; stance = Stance.of(ms)
                advice = adviceFor(mr, tp.mapNotNull { periodTopic(it) })
                link = QaLink(ReadingScope.THANG, y, mo, null, "Xem luận tháng $mo")
                follow += listOf("Tháng ${if (mo == 12) 1 else mo + 1} âm năm ${if (mo == 12) y + 1 else y} thế nào?", "Ngày nào trong tháng này tốt?")
            }
            ReadingScope.NGAY -> if (time.days > 1) {
                val start = time.date ?: ctx.today
                val ds = rangeDays(chart, start, time.days, ctx)
                understood = "$tLabel · ${time.label}"
                val dows = listOf("", "thứ Hai", "thứ Ba", "thứ Tư", "thứ Năm", "thứ Sáu", "thứ Bảy", "Chủ Nhật")
                val avg = Math.round(ds.map { it.score }.average()).toInt()
                val best = ds.maxWith(compareBy<DayScore> { it.score }.thenByDescending { it.date })
                val worst = ds.minWith(compareBy<DayScore> { it.score }.thenBy { it.date })
                paras.add(Paragraph("${time.label.replaceFirstChar { it.uppercase() }}: " + ds.joinToString("; ") { "${dows[it.date.dayOfWeek.value]} %02d/%02d (âm %d/%d) %d".format(it.date.dayOfMonth, it.date.monthValue, it.lunarDay, it.lunarMonth, it.score) } +
                    ". Điểm trung bình $avg/100.", if (avg >= 55) 1 else if (avg <= 45) -1 else 0, ParaKind.NOTE, null, listOf("QA_WEEK"),
                    listOf(Why("QA_WEEK", "Điểm từng ngày", "Ngày = tháng ×0,50 + cung nhật hạn ×0,35 + can chi ngày so với tuổi ×0,15.", ds.map { "${it.date}: ${it.score} (nhật hạn ${it.role ?: "—"})" }, "Prompt mục 9 bước 6")), false))
                if (ds.size > 1) paras.add(Paragraph("Ngày thuận nhất: ${dows[best.date.dayOfWeek.value]} %02d/%02d (%d); nên thận trọng hơn: ${dows[worst.date.dayOfWeek.value]} %02d/%02d (%d).".format(best.date.dayOfMonth, best.date.monthValue, best.score, worst.date.dayOfMonth, worst.date.monthValue, worst.score),
                    0, ParaKind.NOTE, null, listOf("QA_WEEK_BEST"), listOf(Why("QA_WEEK_BEST", "Ngày cao nhất, thấp nhất", "So điểm các ngày trong khoảng được hỏi.", emptyList(), "Prompt mục 9 bước 6")), false))
                val br = cached(chart, "day" to best.date) { composer.day(chart, best.date, ctx.expert) }
                br.summary.firstOrNull { it.ruleIds.contains("GIO_THUAN") }?.let { paras.add(prefixed(it, "Ngày %02d/%02d — ".format(best.date.dayOfMonth, best.date.monthValue))) }
                textPara("note.ngay")?.let { paras.add(it) }
                score = avg; stance = Stance.of(avg)
                advice = adviceFor(br, topics.mapNotNull { periodTopic(it) }, 2)
                link = QaLink(ReadingScope.NGAY, best.lunarYear, null, best.date, "Xem chi tiết ngày %02d/%02d".format(best.date.dayOfMonth, best.date.monthValue))
                follow += listOf("Ngày nào trong tháng này tốt?", "Tháng này thế nào?")
            } else {
                val d = time.date ?: ctx.today
                val dr = cached(chart, "day" to d) { composer.day(chart, d, ctx.expert) }
                understood = "$tLabel · ${time.label}"
                paras += dr.summary.filter { it.kind == ParaKind.LEAD || it.ruleIds.any { id -> id in setOf("NHAT_HAN", "GIO_THUAN", "CAN_CHI_NGAY") } }.take(4)
                textPara("note.ngay")?.let { paras.add(it) }
                score = dr.score; stance = dr.stance
                advice = adviceFor(dr, topics.mapNotNull { periodTopic(it) }, 2)
                val ly = LunarCalendar.solarToLunar(d, 7).year
                link = QaLink(ReadingScope.NGAY, ly, null, d, "Xem chi tiết ngày")
                follow += listOf("Ngày nào trong tháng này tốt?", "Tháng này thế nào?")
            }
        }
        if (paras.none { it.kind != ParaKind.NOTE } && paras.size <= 1) textPara("qa.chua_du")?.let { paras.add(0, it) }
        val bad = (score ?: 50) <= 45 || paras.any { it.kind == ParaKind.LEAD && it.polarity < 0 }
        return QaAnswer(q, parse, understood, score, stance, paras.distinctBy { it.text }, advice, follow.distinct().take(3), link,
            if (bad) kb.texts["note.duc_nang"]?.text else null)
    }

    /** Điểm các ngày liên tiếp từ [start] (lịch tháng của Composer, có thể qua hai tháng). */
    private fun rangeDays(chart: Chart, start: LocalDate, n: Int, ctx: QaContext): List<DayScore> {
        val end = start.plusDays((n - 1).toLong())
        val months = generateSequence(start.withDayOfMonth(1)) { it.plusMonths(1) }.takeWhile { !it.isAfter(end) }.toList()
        return months.flatMap { m -> cached(chart, Triple("days", m.year, m.monthValue)) { composer.days(chart, m.year, m.monthValue, ctx.expert) } }
            .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
    }

    private fun monthsLine(yr: Reading, y: Int): Paragraph {
        val good = yr.months.sortedWith(compareByDescending<MonthScore> { it.score }.thenBy { it.month }).take(3)
        val caution = yr.months.sortedWith(compareBy<MonthScore> { it.score }.thenBy { it.month }).take(2)
        return Paragraph("Năm $y, tháng thuận nhất (âm lịch): ${good.joinToString(", ") { "${it.month} (${it.score})" }}; nên thận trọng: ${caution.joinToString(", ") { "${it.month} (${it.score})" }}.",
            0, ParaKind.NOTE, null, listOf("MONTHS_NAM"),
            listOf(Why("MONTHS_NAM", "Điểm 12 tháng âm", "Tháng = năm ×0,50 + nguyệt hạn ×0,35 + Tứ Hóa tháng ×0,15.", yr.months.map { "T${it.month}: ${it.score}${it.role?.let { r -> " (nguyệt hạn $r)" } ?: ""}" }, "Prompt mục 9 bước 6")), false)
    }

    private fun advice(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val time = parse.time
        val curY = ctx.lunarYear
        val r: Reading
        val label: String
        val link: QaLink
        when (time?.scope) {
            ReadingScope.TRON_DOI -> { r = life(chart, ctx); label = "trọn đời"; link = QaLink(ReadingScope.TRON_DOI, null, null, null, "Xem luận trọn đời") }
            ReadingScope.DAI_VAN -> { val time = time!!; val y = time.lunarYear ?: curY; r = decadeR(chart, y, ctx); label = time.label; link = QaLink(ReadingScope.DAI_VAN, y, null, null, "Xem luận đại vận") }
            ReadingScope.THANG -> { val time = time!!; val y = time.lunarYear ?: curY; val m = time.month ?: ctx.lunarMonth; r = cached(chart, Triple("month", y, m)) { composer.month(chart, y, m, ctx.expert) }; label = time.label; link = QaLink(ReadingScope.THANG, y, m, null, "Xem luận tháng $m") }
            ReadingScope.NGAY -> { val time = time!!; val d = time.date ?: ctx.today; r = cached(chart, "day" to d) { composer.day(chart, d, ctx.expert) }; label = time.label; link = QaLink(ReadingScope.NGAY, LunarCalendar.solarToLunar(d, 7).year, null, d, "Xem chi tiết ngày") }
            else -> { val y = time?.lunarYear ?: curY; r = yearR(chart, y, ctx); label = "năm $y"; link = QaLink(ReadingScope.NAM, y, null, null, "Xem luận năm $y") }
        }
        val y = link.lunarYear ?: curY
        val topics = parse.topics.take(2)
        val paras = ArrayList<Paragraph>()
        parse.reframe?.let { textPara(it) }?.let { paras.add(it) }
        if (time == null || time.scope == ReadingScope.NAM) {
            val d = yData(chart, y, ctx)
            if (topics.isEmpty()) paras += analyst.year(chart, d, null, null, parse.relative, compact = true).paragraphs
            else for (t in topics) paras += analyst.year(chart, d, t, null, parse.relative, compact = true).paragraphs
        } else {
            r.summary.firstOrNull()?.let { paras.add(it) }
            for (t in topics) paras += periodParas(chart, ctx, r, t, null, 1)
        }
        val adviceTopics = topics.mapNotNull { periodTopic(it) }
        val byTopic = if (adviceTopics.isEmpty()) emptyList() else r.advice.filter { it.topic in adviceTopics }
        val pool = (byTopic + r.advice).distinct()
        val adv = pool.filter { it.type == AdviceType.NEN }.take(3) + pool.filter { it.type == AdviceType.KHONG_NEN }.take(3) + pool.filter { it.type == AdviceType.LUU_Y }.take(1)
        val tl = if (topics.isEmpty()) "" else topics.joinToString(", ") { it.vn.lowercase() } + " · "
        val t0 = adviceTopics.firstOrNull()
        return QaAnswer(q, parse, "Nên / Không nên · $tl$label", r.score, r.stance, paras.distinctBy { it.text }, adv,
            listOf(if (t0 != null) "Tháng nào năm $y thuận cho ${SHORT[t0]}?" else "Tháng nào năm $y thuận nhất?", "Năm ${y + 1} của tôi thế nào?", "Thế Tấn, Giữ, Thủ, Lùi là gì?"), link,
            if (r.score <= 45) kb.texts["note.duc_nang"]?.text else null)
    }

    // ———————————————————————————— khi nào ————————————————————————————

    private fun timing(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        // Lá số dưới 16 tuổi âm: không dò năm cho chuyện người lớn (cưới hỏi, tiền bạc, pháp lý…).
        val asked = parse.event?.topic ?: parse.topics.firstOrNull()
        if (chart.tuoiAm(ctx.lunarYear) < 16 && asked != null && asked !in CHILD_TOPICS)
            return simple(q, parse, "qa.note.tre_nho", "Lá số dưới 16 tuổi", suggestions(chart, ctx).take(4))
        return timingBy(chart, q, parse, ctx)
    }

    private fun timingBy(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer = when (parse.timingUnit) {
        ReadingScope.THANG -> timingMonths(chart, q, parse, ctx)
        ReadingScope.NGAY -> timingDays(chart, q, parse, ctx)
        else -> timingYears(chart, q, parse, ctx)
    }

    private fun yearTag(chart: Chart, y: Int): String {
        val cc = CanChi.ofYear(y)
        return "$y (${cc.can.vn} ${cc.chi.vn}, ${chart.tuoiAm(y)} tuổi)"
    }

    private fun timingYears(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        if (parse.verify && parse.event != null && parse.time?.lunarYear != null) return verifyYear(chart, q, parse, ctx, parse.event, parse.time.lunarYear)
        val curY = ctx.lunarYear
        val t = parse.time
        val range: IntRange = when {
            t != null && t.scope == ReadingScope.DAI_VAN -> {
                val p = LimitEngine.forYear(chart, t.lunarYear ?: curY).daiHanPeriod
                val first = chart.lunar.year + p.ages.first - 1
                maxOf(first, curY)..(chart.lunar.year + p.ages.last - 1)
            }
            t != null && t.ageFrom != null -> maxOf(curY, chart.lunar.year + t.ageFrom - 1)..minOf(curY + 30, chart.lunar.year + (t.ageTo ?: 99) - 1)
            t != null && t.scope == ReadingScope.NAM && t.lunarYear != null && t.lunarYear > curY -> t.lunarYear..(t.lunarYear + 10)
            else -> curY..(curY + 10)
        }.let { if (it.isEmpty()) curY..(curY + 10) else it }
        val ev = parse.event
        val asked = ev?.topic ?: parse.topics.firstOrNull()
        // Tính cách, phúc đức không có điểm theo năm → xếp theo điểm cả năm.
        val topic = asked?.let { periodTopic(it) }
        val datas = range.filter { it >= chart.lunar.year }.map { it to yData(chart, it, ctx) }
        val paras = ArrayList<Paragraph>()
        parse.reframe?.let { textPara(it) }?.let { paras.add(it) }
        if (asked != null && topic == null) textPara("qa.note.khong_diem_nam", topic = asked)?.let { paras.add(it) }
        val what = ev?.vn ?: asked?.let { SHORT[it] } ?: "mọi việc"
        val negative = (ev?.polarity ?: 0) < 0 || (parse.caution && (ev == null || ev.polarity <= 0))
        val role = analyst.roleFor(topic, parse.relative)
        fun topicScore(d: Composer.PeriodData) = topic?.let { d.topics[it]?.score } ?: d.score
        fun why(y: Int, d: Composer.PeriodData, extra: List<String>) = Why("QA_YEAR_$y", "Năm $y theo các tầng", 
            "Điểm ${topic?.let { SHORT[it] } ?: "cả năm"} gộp 5 tầng (mục 9 bước 6). ${analyst.yearReason(chart, d, topic, parse.relative, negative)}.",
            (topic?.let { d.topics[it]?.layers } ?: d.layers).map { "${it.layer.vn}: ${it.score.score} (cung ${role.vn} tại ${it.palace.vn})" } + extra, "Prompt mục 8, 9")
        var best: Int? = null
        if (ev != null) {
            val hits = datas.mapNotNull { (y, d) -> d.events.firstOrNull { it.event == ev }?.let { e -> Triple(y, e, d) } }
                .sortedWith(compareByDescending<Triple<Int, YearEvent, Composer.PeriodData>> { pts(it.second.level) }.thenByDescending { topicScore(it.third) }.thenBy { it.first })
            if (hits.isEmpty()) {
                textPara("qa.khong_su_kien")?.let { paras.add(it) }
            } else {
                for ((y, e, d) in hits.take(3)) {
                    val s = topicScore(d)
                    paras.add(Paragraph(
                        "${yearTag(chart, y)}: ${ev.short} ${e.level.vn} (${e.layers.size} tầng cùng chỉ: ${e.layers.joinToString(", ") { LAYER_SHORT.getValue(it) }}); điểm ${SHORT[ev.topic]} $s/100. " +
                            "${analyst.yearReason(chart, d, ev.topic, parse.relative, negative)}.",
                        if (negative) -1 else if (ev.polarity > 0) 1 else 0, ParaKind.EVENT, ev.topic, listOf("EVENT_${ev.name}", "QA_YEAR_$y"),
                        listOf(why(y, d, listOf("Các tầng cùng chỉ: ${e.layers.joinToString(", ") { it.vn }}"))), false, "Năm $y"))
                }
                best = hits.first().first
            }
        }
        if (best == null) {
            val ranked = datas.filter { topic == null || it.second.topics.containsKey(topic) }
                .sortedWith(if (negative) compareBy<Pair<Int, Composer.PeriodData>> { topicScore(it.second) }.thenBy { it.first } else compareByDescending<Pair<Int, Composer.PeriodData>> { topicScore(it.second) }.thenBy { it.first })
            for ((y, d) in ranked.take(3)) {
                val s = topicScore(d)
                paras.add(Paragraph("${yearTag(chart, y)}: ${topic?.let { "điểm ${SHORT[it]}" } ?: "điểm cả năm"} $s/100${if (topic != null) ", điểm cả năm ${d.score}/100" else ""}. " +
                    "${analyst.yearReason(chart, d, topic, parse.relative, negative)}.",
                    if (s >= 55) 1 else if (s <= 45) -1 else 0, ParaKind.NOTE, topic, listOf("QA_YEAR_$y"), listOf(why(y, d, emptyList())), false, "Năm $y"))
            }
            best = ranked.firstOrNull()?.first
        }
        // Không nêu thời gian: dò thêm các năm đã qua để người đã trải qua việc đó tự đối chiếu (D-64).
        if (ev != null && t == null) pastHits(chart, ev, ctx)?.let { paras.add(it) }
        textPara("qa.note.kha_nang")?.let { paras.add(it) }
        val head = if (negative) "Những năm nên thận trọng về $what" else "Năm thuận cho $what"
        val y0 = best ?: curY
        return QaAnswer(q, parse, "$head · dò ${range.first}–${range.last}", null, null, paras, emptyList(),
            listOfNotNull(topic?.let { "Năm $y0 ${SHORT[it]} thế nào?" },
                topic?.let { if (negative) "Tháng nào năm $y0 nên chú ý ${SHORT[it]}?" else "Tháng nào năm $y0 thuận cho ${SHORT[it]}?" } ?: "Tháng nào năm $y0 thuận nhất?",
                if (negative) "Tôi nên lưu ý gì năm $y0?" else "Đồng thanh tương ứng là gì?"),
            QaLink(ReadingScope.NAM, y0, null, null, "Xem luận năm $y0"), null)
    }

    private fun pts(l: Likelihood?) = when (l) { Likelihood.CAO -> 3; Likelihood.CO_THE -> 2; Likelihood.DAU_HIEU -> 1; null -> 0 }

    /**
     * Các năm đã qua (từ 18 tuổi âm, tối đa 20 năm) có ít nhất hai tầng cùng chỉ về [ev]: liệt kê theo thứ tự thời gian để
     * người đã cưới, đã có con… so với năm thật. Năm khớp nhiều là một cách kiểm giờ sinh (D-64).
     */
    private fun pastHits(chart: Chart, ev: Event, ctx: QaContext): Paragraph? {
        val curY = ctx.lunarYear
        val from = maxOf(chart.lunar.year + 17, curY - 20)
        if (from >= curY) return null
        // Mỗi năm tính độc lập (không trạng thái chung) → chạy song song; ghi lại vào bộ nhớ tạm cho lần hỏi sau.
        val all = cached(chart, "yev-past" to (from to curY)) {
            (from until curY).toList().parallelStream().map { y -> y to composer.yearEvents(chart, y, ctx.expert) }
                .collect(java.util.stream.Collectors.toList()).sortedBy { it.first }
        }
        val hits = all.mapNotNull { (y, es) -> es.firstOrNull { it.event == ev && pts(it.level) >= 2 }?.let { y to it } }
        if (hits.isEmpty()) return null
        val top = hits.sortedWith(compareByDescending<Pair<Int, YearEvent>> { pts(it.second.level) }.thenByDescending { it.first }).take(4).sortedBy { it.first }
        return Paragraph(
            "Đối chiếu các năm đã qua: lá số cũng từng nổi tín hiệu ${ev.short} vào ${top.joinToString("; ") { (y, e) -> "${yearTag(chart, y)} — ${e.level.vn}" }}. " +
                "Nếu bạn đã trải qua việc này, hãy so với năm thật: trùng hoặc lệch một năm là lá số và giờ sinh khá khớp; lệch xa thì nên xem lại giờ sinh.",
            0, ParaKind.NOTE, ev.topic, listOf("EVENT_${ev.name}_DA_QUA"),
            listOf(Why("EVENT_${ev.name}_DA_QUA", "Các năm đã qua có tín hiệu ${ev.short}", "Dò từ ${from} đến ${curY - 1}: năm có từ hai tầng vận hạn cùng chỉ về ${ev.vn} (đồng thanh tương ứng).",
                top.map { (y, e) -> "$y: ${e.level.vn} — ${e.layers.joinToString(", ") { it.vn }}" }, "Mục 9 (đồng thanh tương ứng)")),
            false, "Các năm đã qua")
    }

    /** “Tôi đã cưới năm 2015, có khớp không?”: tín hiệu của năm đó và hai năm mỗi bên, theo thứ tự thời gian. */
    private fun verifyYear(chart: Chart, q: String, parse: QaParse, ctx: QaContext, ev: Event, y: Int): QaAnswer {
        val curY = ctx.lunarYear
        val range = maxOf(y - 2, chart.lunar.year)..minOf(y + 2, curY)
        val rows = range.map { yy -> val d = yData(chart, yy, ctx); Triple(yy, d, d.events.firstOrNull { it.event == ev }) }
        val at = rows.first { it.first == y }.third
        val near = rows.filter { it.first != y && it.third != null }.sortedWith(compareByDescending<Triple<Int, Composer.PeriodData, YearEvent?>> { pts(it.third?.level) }.thenBy { kotlin.math.abs(it.first - y) })
        val lead = when {
            at != null && pts(at.level) >= 2 -> "Năm $y lá số có tín hiệu ${ev.short} ở mức ${at.level.vn} (${at.layers.size} tầng cùng chỉ) — khớp với điều bạn kể."
            at != null -> "Năm $y lá số có tín hiệu ${ev.short} ở mức ${at.level.vn} (${at.layers.size} tầng) — khớp một phần."
            near.isNotEmpty() -> "Năm $y lá số chưa nổi tín hiệu ${ev.short}; tín hiệu gần nhất là năm ${near.first().first} (${near.first().third!!.level.vn}). Lệch một, hai năm vẫn thường gặp, nhất là khi giờ sinh chưa thật chắc."
            else -> "Từ ${range.first} đến ${range.last} lá số chưa nổi tín hiệu ${ev.short}. Việc vẫn có thể xảy ra do lựa chọn và hoàn cảnh; nếu nhiều việc lớn đều không khớp, nên xem lại giờ sinh."
        }
        val paras = ArrayList<Paragraph>()
        paras.add(Paragraph(lead, if (at != null) 1 else 0, ParaKind.LEAD, ev.topic, listOf("QA_VERIFY_${ev.name}"),
            listOf(Why("QA_VERIFY_${ev.name}", "Đối chiếu năm $y", "So sự kiện bạn kể với các tầng vận hạn (đại hạn, lưu niên đại hạn, tiểu hạn, Lưu Thái Tuế) của năm $y và hai năm mỗi bên.",
                rows.map { (yy, _, e) -> "$yy: ${e?.let { "${it.level.vn} — ${it.layers.joinToString(", ") { l -> l.vn }}" } ?: "không tầng nào chỉ"}" }, "Mục 9 (đồng thanh tương ứng)")), false, "Kết luận"))
        for ((yy, d, e) in rows) {
            val s = d.topics[ev.topic]?.score
            val sig = e?.let { "${ev.short} ${it.level.vn} (${it.layers.joinToString(", ") { l -> LAYER_SHORT.getValue(l) }})" } ?: "chưa thấy tầng nào chỉ về ${ev.short}"
            paras.add(Paragraph("${yearTag(chart, yy)}${if (yy == y) " — năm bạn nêu" else ""}: $sig${s?.let { "; điểm ${SHORT[ev.topic]} $it/100" } ?: ""}. " +
                "${analyst.yearReason(chart, d, ev.topic, parse.relative, false)}.",
                if (e != null) 1 else 0, ParaKind.EVENT, ev.topic, listOf("QA_YEAR_$yy"),
                listOf(Why("QA_YEAR_$yy", "Năm $yy theo các tầng", "Điểm ${SHORT[ev.topic]} gộp 5 tầng (mục 9 bước 6).",
                    (d.topics[ev.topic]?.layers ?: d.layers).map { "${it.layer.vn}: ${it.score.score} (tại ${it.palace.vn})" }, "Mục 8, 9")), false, "Năm $yy"))
        }
        textPara("qa.note.kha_nang")?.let { paras.add(it) }
        return QaAnswer(q, parse, "Đối chiếu năm $y với lá số · ${ev.vn}", null, null, paras, emptyList(),
            listOf("Năm nào thuận cho ${ev.short}?", "Năm $y của tôi thế nào?", "Đồng thanh tương ứng là gì?"),
            QaLink(ReadingScope.NAM, y, null, null, "Xem luận năm $y"), null)
    }

    private fun timingMonths(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val y = parse.time?.lunarYear ?: ctx.lunarYear
        val yr = yearR(chart, y, ctx)
        val asked = parse.event?.topic ?: parse.topics.firstOrNull()
        val topic = asked?.let { periodTopic(it) }
        val paras = ArrayList<Paragraph>()
        parse.reframe?.let { textPara(it) }?.let { paras.add(it) }
        paras.add(monthsLine(yr, y))
        if (asked != null && asked != topic) textPara(if (topic == null) "qa.note.khong_diem_nam" else "qa.note.nha_dat", topic = asked)?.let { paras.add(it) }
        if (topic != null) {
            val roles = Scorer.TOPICS.getValue(topic).main.map { it.vn }
            val hit = yr.months.filter { it.role in roles }
            paras.add(Paragraph(
                if (hit.isEmpty()) "Năm $y không có tháng nào nguyệt hạn vào cung ${roles.joinToString(", ")}." else
                    "Tháng nguyệt hạn vào cung ${roles.joinToString(", ")} (chủ đề ${topic.vn.lowercase()} được chú ý hơn): ${hit.joinToString(", ") { "tháng ${it.month} (${it.score})" }}.",
                0, ParaKind.NOTE, topic, listOf("NGUYET_HAN_CHU_DE"),
                listOf(Why("NGUYET_HAN_CHU_DE", "Nguyệt hạn theo chủ đề", "Cung đọc chính của ${topic.vn}: ${roles.joinToString(", ")}.", yr.months.map { "T${it.month}: ${it.role ?: "—"}" }, "Prompt mục 8.5, 9")), false))
            yr.sections.firstOrNull { it.topic == topic }?.paragraphs?.firstOrNull { it.kind == ParaKind.LEAD }?.let { paras.add(prefixed(it, "Cả năm — ")) }
            parse.event?.let { ev -> yr.sections.firstOrNull { it.topic == ev.topic }?.paragraphs?.firstOrNull { it.kind == ParaKind.EVENT && "EVENT_${ev.name}" in it.ruleIds }?.let { paras.add(it) } ?: eventLine(chart, ev, y, ctx)?.let { paras.add(it) } }
        }
        textPara("qa.note.thang_tong")?.let { paras.add(it) }
        return QaAnswer(q, parse, "${if (parse.caution) "Tháng nên thận trọng" else "Tháng thuận"} năm $y${asked?.let { " · ${it.vn.lowercase()}" } ?: ""}", yr.score, yr.stance, paras.distinct(), adviceFor(yr, listOfNotNull(topic), 2),
            listOf("Tháng ${yr.goodMonths.firstOrNull() ?: 1} âm năm $y thế nào?", "Ngày nào trong tháng này tốt?", "Năm ${y + 1} thế nào?"),
            QaLink(ReadingScope.NAM, y, null, null, "Xem luận năm $y (12 tháng)"), null)
    }

    private fun timingDays(chart: Chart, q: String, parse: QaParse, ctx: QaContext): QaAnswer {
        val t = parse.time
        val today = ctx.today
        if (t != null && t.days > 1 && t.date != null) {
            val ds = rangeDays(chart, t.date, t.days, ctx).filter { !it.date.isBefore(today) }
            val paras = ArrayList<Paragraph>()
            textPara("qa.chuyen.chon_ngay")?.let { paras.add(it) }
            val dows = listOf("", "thứ Hai", "thứ Ba", "thứ Tư", "thứ Năm", "thứ Sáu", "thứ Bảy", "Chủ Nhật")
            val sorted = ds.sortedWith(compareByDescending<DayScore> { it.score }.thenBy { it.date })
            paras.add(Paragraph("${t.label.replaceFirstChar { it.uppercase() }}, xếp từ điểm cao xuống: " + sorted.joinToString("; ") { "${dows[it.date.dayOfWeek.value]} %02d/%02d (âm %d/%d): %d".format(it.date.dayOfMonth, it.date.monthValue, it.lunarDay, it.lunarMonth, it.score) } + ".",
                0, ParaKind.NOTE, null, listOf("QA_DAYS_GOOD"), listOf(Why("QA_DAYS_GOOD", "Điểm ngày", "Tháng ×0,50 + cung nhật hạn ×0,35 + can chi ngày so với tuổi ×0,15.", sorted.map { "${it.date}: ${it.score}" }, "Prompt mục 9 bước 6")), false))
            val bestDay = sorted.firstOrNull()?.date
            return QaAnswer(q, parse, "Ngày thuận · ${t.label}", null, null, paras, emptyList(),
                listOfNotNull(bestDay?.let { "Ngày %02d/%02d/%d thế nào?".format(it.dayOfMonth, it.monthValue, it.year) }, "Tháng này thế nào?", "Ngày mai thế nào?"),
                bestDay?.let { QaLink(ReadingScope.NGAY, LunarCalendar.solarToLunar(it, 7).year, null, it, "Xem lịch ngày") }, null)
        }
        val (sy, sm) = when {
            t != null && t.date != null -> t.date.year to t.date.monthValue
            t != null && t.scope == ReadingScope.THANG && t.month != null -> {
                val d = lunarToSolar(15, t.month, t.lunarYear ?: ctx.lunarYear) ?: today
                d.year to d.monthValue
            }
            Regex("(?<![a-z])(?:thang sau|thang toi)(?![a-z])").containsMatchIn(parse.norm) -> today.plusMonths(1).let { it.year to it.monthValue }
            else -> today.year to today.monthValue
        }
        val days = composer.days(chart, sy, sm, ctx.expert).filter { it.date >= today || sy != today.year || sm != today.monthValue }
        val paras = ArrayList<Paragraph>()
        textPara("qa.chuyen.chon_ngay")?.let { paras.add(it) }
        val dows = listOf("", "thứ Hai", "thứ Ba", "thứ Tư", "thứ Năm", "thứ Sáu", "thứ Bảy", "Chủ Nhật")
        fun tag(d: DayScore) = "%02d/%02d (%s, âm %d/%d)".format(d.date.dayOfMonth, d.date.monthValue, dows[d.date.dayOfWeek.value], d.lunarDay, d.lunarMonth)
        if (days.isEmpty()) {
            paras.add(fact("Tháng $sm/$sy không còn ngày nào sau hôm nay để chọn.", "QA_DAYS_EMPTY", "Lịch ngày", "Chỉ xét các ngày từ hôm nay."))
        } else {
            val good = days.sortedWith(compareByDescending<DayScore> { it.score }.thenBy { it.date }).take(5)
            val bad = days.sortedWith(compareBy<DayScore> { it.score }.thenBy { it.date }).take(3)
            paras.add(Paragraph("Tháng $sm/$sy (dương lịch), những ngày điểm cao nhất: ${good.joinToString("; ") { "${tag(it)}: ${it.score}" }}.", 1, ParaKind.NOTE, null, listOf("QA_DAYS_GOOD"),
                listOf(Why("QA_DAYS_GOOD", "Điểm ngày", "Tháng ×0,50 + cung nhật hạn ×0,35 + can chi ngày so với tuổi ×0,15.", good.map { "${it.date}: ${it.score} (nhật hạn ${it.role ?: "—"})" }, "Prompt mục 9 bước 6")), false))
            paras.add(Paragraph("Ngày nên thận trọng hơn: ${bad.joinToString("; ") { "${tag(it)}: ${it.score}" }}.", -1, ParaKind.NOTE, null, listOf("QA_DAYS_BAD"),
                listOf(Why("QA_DAYS_BAD", "Điểm ngày", "Ba ngày điểm thấp nhất trong tháng.", bad.map { "${it.date}: ${it.score}" }, "Prompt mục 9 bước 6")), false))
        }
        val bestDay = days.maxByOrNull { it.score }?.date
        return QaAnswer(q, parse, "Ngày thuận trong tháng $sm/$sy (dương lịch)", null, null, paras, emptyList(),
            listOfNotNull(bestDay?.let { "Ngày %02d/%02d/%d thế nào?".format(it.dayOfMonth, it.monthValue, it.year) }, "Tháng này thế nào?", "Ngày mai thế nào?"),
            bestDay?.let { QaLink(ReadingScope.NGAY, LunarCalendar.solarToLunar(it, 7).year, null, it, "Xem lịch ngày") }, null)
    }
}
