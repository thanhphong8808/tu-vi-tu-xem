# Chấm bộ 40 câu — Trợ lý AI trên máy (Gemma 4 E4B, LiteRT-LM)

Chạy trên máy build Linux, **CPU 4 nhân**, LiteRT-LM 0.19.1 (`litertlm-jvm`), model `gemma-4-E4B-it.litertlm`.
Cùng luồng với app: `AppApi.askAi` → lời nhắc từ QaAnalyst → model → kiểm tra mục 12.3 (`AiGuard`).
Câu nào trượt kiểm tra thì app hiện câu của QaAnalyst (không hiện câu AI).

| Chỉ số | Kết quả |
| --- | --- |
| Câu hỏi | 40 (bỏ qua không gọi model: 0) |
| Qua kiểm tra, hiện câu AI | **36/40** |
| Trượt kiểm tra → hiện câu QaAnalyst | 4 |
| Nạp model lần đầu | 1.023 giây |
| Thời gian một câu (trung vị / lâu nhất) | 44.282 / 64.085 giây |

Lý do trượt (đếm theo loại): gán × 6; điểm N/N không có trong dữ kiện × 1

## 1. Năm sau sự nghiệp của tôi thế nào? — lá số A

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 49/100

**Đạt** (50.411 giây, 229 chữ). Câu AI:

> Chào bạn, về sự nghiệp của bạn trong năm 2027 (Đinh Mùi), kết quả chung là 49/100, mức trung bình, có lúc thăng có lúc xuống. Nền gốc của bạn rất tốt, cung Quan Lộc tại Ngọ có Tử Vi miếu và Thiên Khôi, cho thấy bạn có khả năng lãnh đạo và được cấp trên trọng dụng, nhưng cũng có sự đôn hậu, thích quản trị. Tuy nhiên, trong năm 2027, sự nghiệp của bạn chịu ảnh hưởng của đại hạn và tiểu hạn, khiến mức độ cần thận trọng hơn. Cụ thể trong năm, Hóa Lộc lưu nhập cung Quan Lộc theo Thái Tuế, báo hiệu bạn có thêm cơ hội công việc hoặc dự án mới, thu nhập có thể ổn định hơn. Tuy nhiên, bạn cũng sẽ gặp một chút phiền lòng, mệt mỏi trong công việc do Tang Môn lưu và Thiên Khốc lưu ảnh hưởng. Về mặt hành động, bạn nên chủ động nhận việc mới và mở rộng quan hệ công việc vào các tháng 2, 7 và 11. Ngược lại, bạn nên giữ nhịp và không nên đổi việc vì cảm xúc nhất thời, đặc biệt là khi có các sao như Kình Dương hay Đà La lưu. Hãy chú ý lên kế hoạch cụ thể cho lĩnh vực mà cung được kích hoạt để đạt được kết quả tốt nhất.

## 2. Tình duyên năm 2027 ra sao? — lá số A

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 50/100

**Đạt** (49.556 giây, 325 chữ). Câu AI:

> Chào bạn, tình duyên và hôn nhân của bạn trong năm 2027 (Đinh Mùi, 38 tuổi âm) được đánh giá ở mức 50/100, tức là trung bình, có những lúc thuận lợi nhưng cũng có lúc cần phải giữ nhịp. Về cơ bản, nền tảng gốc của cung Phu Thê bạn an tại Tý với sao Tham Lang hãm, cho thấy người bạn đời có thể khá ham vui, khó nắm bắt, nên các mối quan hệ cần sự thấu hiểu sâu sắc. Tuy nhiên, khi nhìn vào các tầng vận hạn, đại hạn và tiểu hạn năm 2027 đều cho thấy sự thuận lợi ở mức khá, đặc biệt là cung Phu Thê của tiểu hạn năm 2027 rơi vào Mão, nơi có Thái Dương vượng và Thiên Lương vượng cùng Hóa Lộc, Thiên Hỷ, báo hiệu người bạn đời có thể mang nét danh tiếng, tài năng và sự rực rỡ. Trong năm này, có sự xuất hiện của Hóa Khoa lưu (Thiên Cơ) nhập cung Phu Thê theo Thái Tuế, điều này giúp quan hệ của bạn dễ nhận được sự ủng hộ từ người xung quanh, có thể có người mai mối. Tuy nhiên, bạn cũng cần lưu ý đến Đà La lưu nhập cung Phu Thê, khiến chuyện tình cảm có thể trở nên chậm chạp, dây dưa và khó dứt điểm. Ngoài ra, Đào Hoa lưu cũng xuất hiện, dễ khiến bạn có nhiều dịp gặp gỡ và có người để ý. Về tháng, bạn nên chủ động trong các tháng 7, 10 và 11 âm lịch để phát triển mối quan hệ, còn tháng 8 và 9 thì nên giữ nhịp. Lời khuyên dành cho bạn là nên chủ động gặp gỡ, mở rộng quan hệ và nếu hai bên đã sẵn sàng thì nên thuận để bàn chuyện cưới hỏi, đồng thời cần đặt ra những mốc thời gian rõ ràng cho mối quan hệ của mình.

## 3. Năm nay tài chính của tôi thế nào? — lá số A

App hiểu là: Tài chính · năm 2026 (Bính Ngọ, 37 tuổi âm) · điểm 41/100

**Đạt** (52.382 giây, 317 chữ). Câu AI:

> Chào bạn, về tài chính trong năm 2026 (Bính Ngọ), kết quả chung là 41/100, cho thấy bạn cần phải rất thận trọng trong mọi quyết định liên quan đến tiền bạc. Dựa trên lá số gốc, cung Tài Bạch của bạn an tại Tuất có Liêm Trinh miếu và Thiên Phủ vượng, cho thấy bạn có nguyên tắc và biết giữ của, nền tảng gốc ở mức trung bình 54/100. Tuy nhiên, đại hạn từ 2024 đến 2033 lại rơi vào cung Sửu, nơi có Thiên Đồng hãm và Cự Môn hãm gặp Hóa Kỵ, khiến giai đoạn này có nhiều khó khăn, nên giữ mức độ 33/100. Trong năm 2026, tiểu hạn tại Thìn lại khiến cung Tài Bạch rơi vào Tý với Tham Lang hãm, báo hiệu tiền bạc dễ thăng trầm do ham muốn vật chất, nên giữ mức độ 27/100. May mắn là Lưu Thái Tuế năm Bính Ngọ lại mang đến điểm 56/100, với Vũ Khúc vượng và Thiên Tướng miếu tại Dần, cho thấy có những cơ hội tốt được đánh thức. Cụ thể trong năm 2026, Hóa Kỵ lưu nhập cung Tài Bạch gốc, nên bạn dễ gặp những khoản chi bất ngờ hoặc nợ chậm thu. Đồng thời, Bạch Hổ lưu nhập cung Tài Bạch theo Thái Tuế, có thể phát sinh chi phí cho sức khỏe hoặc sửa chữa nhà cửa. Về mặt tháng, bạn nên chủ động quản lý tài chính vào tháng 8, tháng 11 và tháng 12, nhưng cần giữ nhịp và cẩn trọng vào tháng 1 và tháng 7. Lời khuyên cho bạn là nên lập quỹ dự phòng và chia nhỏ các khoản đầu tư, đồng thời không nên mở rộng kinh doanh nóng hoặc vay mượn lớn để tăng quy mô. Hãy nhớ rằng, Đức năng thắng số, lựa chọn của bạn sẽ quyết định kết quả tài chính của mình.

## 4. Đại vận này công việc ra sao? — lá số A

App hiểu là: Sự nghiệp · đại vận 35–44 tuổi · điểm 48/100

**Đạt** (39.841 giây, 269 chữ). Câu AI:

> Chào bạn, về câu hỏi sự nghiệp trong đại vận 35 đến 44 tuổi, điểm chung được đánh giá là 48/100, tức là ở mức trung bình, có lúc thuận lợi nhưng cũng có lúc cần giữ vững sự thận trọng. Xét ở lá số gốc, cung Quan Lộc tại Ngọ có Tử Vi miếu, điềm đạm và có tài tổ chức, lãnh đạo, hợp với vai trò quản trị. Bạn được Thiên Khôi chiếu giúp đỡ, và cách cục gốc rất tốt với điểm 59/100, cho thấy nền tảng sự nghiệp của bạn là vững vàng và có quý nhân phù trợ. Tuy nhiên, cung này cũng có Hỏa Tinh đắc địa và Địa Không hãm, điều này báo hiệu công danh có thể có những lúc thành bại thất thường, cần sự bền bỉ. Khi nhìn vào đại hạn 35-44 tuổi, cung Quan Lộc của đại hạn rơi vào cung Tật Ách, mượn Thái Dương và Thiên Lương vượng, cho thấy danh tiếng và tài năng có thể rực rỡ, đặc biệt trong các lĩnh vực liên quan đến học thuật hay y tế. Dù vậy, đại hạn này có điểm 44/100 và được đánh giá là cần thận trọng. Tóm lại, sự nghiệp của bạn trong giai đoạn này có sự lên xuống, cần dựa vào cách bạn chọn thời điểm để phát huy tốt nhất. Lưu ý rằng năm 2027 sẽ là năm thuận lợi nhất với điểm 49/100, còn năm 2028 là năm cần giữ nhất với điểm 39/100. Bạn nên giữ sự bình tĩnh và cẩn trọng trong mọi quyết định sự nghiệp.

## 5. Năm 2020 công việc của tôi thế nào? — lá số A

App hiểu là: Sự nghiệp · năm 2020 (Canh Tý, 31 tuổi âm) · điểm 38/100

**Đạt** (49.147 giây, 305 chữ). Câu AI:

> Chào bạn, về công việc của bạn trong năm 2020 (Canh Tý), lá số cho thấy một mức độ cần thận trọng với điểm số 38/100. Về cơ bản, nền tảng sự nghiệp gốc của bạn khá tốt với Tử Vi miếu tại cung Quan Lộc, cho thấy bạn có tài lãnh đạo và được trọng dụng, nhưng năm 2020 lại có nhiều yếu tố gây áp lực. Cụ thể, đại hạn của bạn đang ở cung Thân với Phá Quân hãm, dễ khiến bạn có những hành động liều lĩnh hoặc ngang tàng, nên bạn cần chọn môi trường ổn định. Trong năm 2020, cung Quan Lộc của tiểu hạn rơi vào Dần, nơi có Vũ Khúc vượng và Thiên Tướng miếu, giúp bạn có trách nhiệm và hợp với quản lý tài chính. Tuy nhiên, Lưu Thái Tuế lại đóng tại Thìn, cung Quan Lộc của Thái Tuế rơi vào Thìn với Thất Sát hãm và Địa Kiếp hãm, mang lại sự khó khăn. Trong năm này, bạn có thể cảm thấy tâm trạng chùng xuống vì công sức chưa được ghi nhận do Thiên Khốc lưu nhập cung Quan Lộc. Đồng thời, có sự xuất hiện của Hóa Quyền lưu (Vũ Khúc) và Tang Môn lưu tại cung Quan Lộc tiểu hạn, cho thấy bạn dễ được trao quyền nhưng công việc cũng dễ gặp chuyện phiền lòng và mệt mỏi. Về các tháng, bạn nên chủ động ở tháng 2, 5 và 6, còn tháng 3 và 9 thì nên giữ nhịp. Lời khuyên cho bạn là nên thủ hơn công, tức là nên củng cố nền tảng và hoãn các quyết định lớn nếu có thể. Bạn cũng không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương hay Đà La lưu.

## 6. Năm nào tôi dễ có tiền nhất? — lá số A

App hiểu là: Năm thuận cho tài lộc, dòng tiền cải thiện · dò 2026–2036

**Đạt** (27.71 giây, 175 chữ). Câu AI:

> Chào bạn, dựa vào lá số của bạn, năm 2034 có khả năng là năm bạn dễ có tiền nhất trong giai đoạn từ 2026 đến 2036. Điểm tài chính của năm này đạt 57/100. Điều này được căn cứ vào việc năm 2034 có sự hội tụ của cả lưu niên đại hạn và Lưu Thái Tuế, đồng thời Hóa Lộc lưu vào cung Tài Bạch và lưu niên đại hạn hội cát tinh. Ngoài ra, năm 2030 cũng có tín hiệu tài lộc với điểm 43/100, do Lưu niên đại hạn hội cát tinh và Tiểu hạn gặp Lộc. Năm 2031 có điểm 47/100, nhờ Tiểu hạn gặp Lộc và Hóa Lộc lưu xung chiếu cung Tài Bạch. Năm 2024 đã qua cũng có tín hiệu tài lộc, nhưng năm 2034 được đánh giá cao hơn về mặt vận hội. Bạn nên lưu ý rằng đây chỉ là khả năng, quyết định cuối cùng vẫn thuộc về bạn. Nên giữ sự chủ động và cẩn trọng khi đầu tư trong những năm này.

## 7. Cung Mệnh của tôi có sao gì? — lá số A

App hiểu là: Dữ kiện lá số: cung Mệnh

**Đạt** (29.901 giây, 242 chữ). Câu AI:

> Chào bạn, cung Mệnh của bạn nằm ở cung Dần và có sự hội tụ của hai chính tinh là Vũ Khúc và Thiên Tướng. Cả hai sao này đều ở trạng thái tốt, cụ thể Vũ Khúc ở trạng thái Vượng và Thiên Tướng ở trạng thái Miếu. Điều này tạo nên một bộ sao Vũ Tướng đồng cung, cho thấy bạn là người có tính cách thực tế, có trách nhiệm cao và có xu hướng rất hợp với việc quản lý tài chính. Về mặt sự nghiệp, sự kết hợp này gợi ý bạn sẽ phù hợp hơn với các ngành nghề kỹ thuật hoặc buôn bán thay vì làm công chức. Hơn nữa, bạn còn được hưởng Hóa Quyền tại cung Mệnh, và khi được các sao tốt như Thiên Khôi, Thiên Việt, Lộc Tồn phù trợ, điều này giúp sự nghiệp của bạn trở nên hiên ngang và đảm lược hơn người khác. Theo sách cổ bàn rằng, dù cung Mệnh có Vũ Khúc bị nhiều sát tinh xung phá, bạn vẫn có khả năng đủ cơm áo nhờ Thiên Trù là lộc ăn mặc do tổ tiên để lại, nhưng bạn vẫn nên giữ của và tránh những hành động liều lĩnh vì của cải dễ bị hao hụt.
> 
> Lời khuyên cho bạn là nên giữ sự cẩn trọng trong việc quản lý tài chính và tránh mạo hiểm quá mức trong các quyết định lớn.

## 8. Tháng 3 âm năm sau công việc thế nào? — lá số A

App hiểu là: Sự nghiệp · tháng 3 âm lịch năm 2027 · điểm 47/100

**Đạt** (33.844 giây, 258 chữ). Câu AI:

> Chào bạn, về công việc trong tháng 3 âm lịch năm 2027, điểm số được đánh giá là 47/100, tức là mức trung bình, có lúc thăng lúc trầm. Điều này được tính toán dựa trên các tầng số khác nhau, và đáng chú ý là nguyệt hạn tháng này không chạm trực tiếp vào cung Quan Lộc gốc của bạn. Cụ thể, nguyệt hạn rơi vào cung Nô Bộc, nơi bạn có thể chú ý nhiều hơn đến các mối quan hệ bạn bè hoặc cộng sự. Ở cung này, bạn sẽ gặp Thiên Đồng hãm và Cự Môn hãm mượn từ cung Sửu, điều này gợi ý rằng bạn bè hoặc đồng nghiệp có thể mang lại những ý kiến dễ gây bất đồng trong lời nói, vì vậy bạn cần học cách diễn đạt mềm mỏng hơn. Về Tứ Hóa tháng Giáp, có Hóa Lộc và Hóa Khoa chạm vào cung Quan Lộc gốc của bạn tại Ngọ, điều này là một tín hiệu tích cực cho sự nghiệp. Hóa Lộc và Hóa Khoa ở đây cho thấy có những cơ hội về tài lộc và sự công nhận trong công việc. Tuy nhiên, bạn cũng cần lưu ý rằng trong năm 2027, có các sao như Kình Dương và Đà La lưu, nên bạn không nên đưa ra quyết định thay đổi công việc chỉ vì cảm xúc nhất thời. Lời khuyên cho bạn là nên giảm bớt khối lượng công việc, ưu tiên những việc thực sự cần thiết trong giai đoạn này.

## 9. nam sau suc khoe can luu y gi — lá số A

App hiểu là: Nên / Không nên · sức khỏe (lưu ý) · năm 2027 · điểm 49/100

**Đạt** (38.653 giây, 274 chữ). Câu AI:

> Điểm chung: 49/100. Chào bạn, về sức khỏe trong năm 2027, lá số cho thấy bạn cần phải thận trọng vì điểm chung của năm này là 43/100. Dù có những điểm thuận lợi từ Lưu Thái Tuế và lưu niên đại hạn, nhưng đại hạn và tiểu hạn lại có sự áp lực khiến sức khỏe cần được lưu tâm. Cụ thể, trong cung Tật Ách gốc tại Dậu, có Thái Dương vượng và Thiên Lương vượng, cho thấy bạn có sự chính trực và được người khác giúp đỡ khi gặp vấn đề sức khỏe. Tuy nhiên, Kình Dương hãm và Thiên Sứ cũng xuất hiện, nhắc nhở bạn cần cẩn trọng khi vận động và tránh những sự trắc trở. Theo ngũ hành, bạn nên để ý đến Hỏa như tim mạch, huyết áp và Mộc như gan mật, mắt. Trong năm 2027, có những diễn biến cụ thể cần lưu ý như Tang Môn lưu nhập cung Tật Ách gốc, nên bạn cần chú ý sức khỏe của bản thân và người lớn tuổi trong nhà, đặc biệt khi đi lại. Về mặt thời gian, bạn nên chủ động chăm sóc sức khỏe vào các tháng 5, 7 và 11 âm lịch, còn các tháng 9 và 12 thì nên giữ nhịp sinh hoạt ổn định. Để sức khỏe tốt hơn, bạn nên giữ chừng mực khi ăn uống, giữ nếp sinh hoạt đều đặn, và tuyệt đối không nên bỏ qua những dấu hiệu bất thường kéo dài hoặc tự ý dùng thuốc. Bạn cũng nên đưa người lớn tuổi đi khám và giữ an toàn khi di chuyển.

## 10. Tôi có nên đổi việc năm 2027 không? — lá số A

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 49/100

**Đạt** (46.838 giây, 274 chữ). Câu AI:

> Chào bạn, về câu hỏi có nên đổi việc vào năm 2027 hay không, kết quả tổng thể cho sự nghiệp năm đó là 49/100, mức trung bình, có lúc thăng có lúc xuống. Dựa trên lá số gốc, cung Quan Lộc của bạn an tại Ngọ, có Tử Vi miếu và Thiên Khôi, cho thấy bạn là người có tài tổ chức, lãnh đạo và được cấp trên trọng dụng, nền tảng sự nghiệp gốc rất thuận lợi. Tuy nhiên, khi xem xét các tầng vận hạn, năm 2027 có những dấu hiệu cần thận trọng. Cụ thể, tiểu hạn năm 2027 rơi vào cung Dậu, vô chính diệu và gặp Kình Dương hãm, điều này cho thấy trong năm có thể có những áp lực và sự trắc trở về công việc. Về việc đổi việc, dữ kiện chỉ ra rằng đây là dấu hiệu của tiểu hạn, nhưng không có sự đồng thuận tuyệt đối với các tầng khác. Trong năm 2027, Hóa Lộc lưu nhập cung Quan Lộc theo Thái Tuế, báo hiệu có thêm cơ hội hoặc dự án mới, nhưng cũng có Thiên Khốc lưu khiến bạn dễ cảm thấy công sức chưa được ghi nhận. Vì vậy, lời khuyên là bạn không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương hay Đà La lưu. Thay vào đó, bạn nên giữ việc chính, tận dụng cơ hội mới để mở rộng quan hệ hoặc chốt các thỏa thuận đang chờ, và nên chủ động trong các tháng 2, 7 và 11 âm lịch để đạt kết quả tốt hơn.

## 11. Năm 2025 tiền bạc của tôi thế nào? — lá số A

App hiểu là: Tài chính · năm 2025 (Ất Tỵ, 36 tuổi âm) · điểm 45/100

**Trượt** (53.295 giây): gán “Địa Không” với “Tuất” không khớp dữ kiện; gán “Địa Kiếp” với “Tuất” không khớp dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn, về tình hình tài chính của bạn trong năm 2025 (Ất Tỵ, 36 tuổi âm), kết quả tổng thể là 45/100, cho thấy bạn cần phải hết sức thận trọng trong việc quản lý tiền bạc. Điểm tựa tích cực nhất của bạn nằm ở tiểu hạn, nơi cung Tài Bạch tại Hợi có Thái Âm miếu và được Hóa Khoa chiếu vào, mang lại duyên tích lũy tài sản. Tuy nhiên, áp lực lớn đến từ đại hạn và Lưu Thái Tuế, khi cung Tài Bạch rơi vào Sửu với Thiên Đồng hãm và Cự Môn hãm, lại gặp Hóa Kỵ, điều này dễ khiến bạn gặp bất đồng trong lời nói hoặc phát sinh những khoản chi tiêu không như ý. Nhìn vào lá số gốc, cung Tài Bạch tại Tuất có Liêm Trinh miếu và Thiên Phủ vượng, cho thấy bạn vốn là người biết giữ của và có khả năng tích lũy, nhưng sự xuất hiện của Linh Tinh hãm và các sao như Địa Không, Địa Kiếp trong lá số gốc cũng cảnh báo về sự hao tán tiền bạc thất thường. Trong năm 2025, có những dấu hiệu cụ thể cần lưu ý: Hồng Loan lưu nhập cung Tài Bạch khiến chi tiêu cho tiệc tùng, quà cáp tăng lên, trong khi Thiên Khốc và Bạch Hổ lưu nhập cũng báo hiệu có thể phát sinh chi phí cho sức khỏe hoặc việc gia đình. Đặc biệt, Hóa Kỵ lưu vào cung Tài Bạch của tiểu hạn có thể khiến tiền bạc dễ vướng mắc do các khoản chi bất ngờ hoặc hiểu lầm về tài chính. Về mặt thời điểm, bạn nên chủ động và tích cực trong tài chính vào các tháng 8, 9 và 12 âm lịch, nhưng cần giữ nhịp và cẩn trọng vào tháng 2 và tháng 10. Lời khuyên cho bạn là hãy giữ kỷ luật chi tiêu và ghi chép dòng tiền đều đặn. Bạn không nên cho vay lớn, bả

## 12. Tôi có số giàu không? — lá số A

App hiểu là: Tài chính · trọn đời · điểm 54/100

**Trượt** (36.909 giây): gán “Linh Tinh” với “Phúc Đức” không khớp dữ kiện; điểm 38/100 không có trong dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn, dựa trên lá số của bạn, về mặt tài chính trọn đời, điểm chung là 54/100, tức là ở mức trung bình, có những lúc thăng trầm. Cung Tài Bạch của bạn an tại Tuất, có Liêm Trinh miếu và Thiên Phủ vượng, cho thấy bạn là người có nguyên tắc, biết cách giữ của và có khả năng tích lũy tiền bạc ổn định. Tuy nhiên, cung này cũng có Linh Tinh hãm, và khi xét đến các sao chiếu về như Địa Kiếp từ cung Phúc Đức, cùng với sự hiện diện của Địa Không, Địa Kiếp trong lá số, điều này cảnh báo rằng tiền bạc của bạn có thể dễ bị hao tán hoặc gặp những biến động bất ngờ. Mệnh của bạn tại Dần với Vũ Khúc vượng và Thiên Tướng miếu, cho thấy bạn là người thực tế và có trách nhiệm trong việc quản lý tài chính. Trong các đại vận, giai đoạn từ 15 đến 24 tuổi là thuận lợi nhất với điểm 66/100, nhưng giai đoạn từ 25 đến 34 tuổi lại cần giữ gìn vì điểm chỉ là 30/100. Hiện tại bạn đang ở đại vận 35–44 tuổi với điểm 38/100. Để có sự ổn định về tài chính, bạn nên giữ kỷ luật chi tiêu và ghi chép dòng tiền đều đặn. Bạn không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy."}

## 13. Năm sau tình duyên của tôi thế nào? — lá số B

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 43 tuổi âm) · điểm 52/100

**Đạt** (51.881 giây, 338 chữ). Câu AI:

> Chào bạn, về tình duyên và hôn nhân của bạn trong năm 2027 (Đinh Mùi), kết quả chung là 52/100, ở mức trung bình, có lúc thuận có lúc cần giữ nhịp. Nền gốc của bạn tại cung Phu Thê ở Sửu có Thiên Đồng và Cự Môn đều ở trạng thái hãm, điều này cho thấy người bạn đời có thể có nét dễ bất đồng trong lời nói, nên bạn cần học cách diễn đạt mềm mỏng hơn. Đại hạn từ 2019 đến 2028, cung Phu Thê của đại hạn rơi vào Thìn với Thất Sát hãm, gợi ý rằng trong giai đoạn này, mối quan hệ có thể cần thêm sự thấu hiểu để tránh khoảng cách. Đến năm 2027, tiểu hạn tại cung Phu Thê rơi vào Hợi với Thái Âm miếu, cho thấy người bạn đời thường dịu dàng, tinh tế, và quan hệ có xu hướng thuận lợi. Tuy nhiên, năm này cũng có Hóa Kỵ, Địa Không, Địa Kiếp đắc tại cung này, nên cần cẩn trọng với những hiểu lầm hay sự thay đổi bất ngờ. Về các yếu tố năm, Hóa Quyền lưu nhập cung Phu Thê gốc, báo hiệu một bên có xu hướng muốn làm chủ trong quan hệ, dễ dẫn đến các quyết định lớn như ra mắt gia đình hoặc cưới hỏi. Đồng thời, Hóa Khoa lưu nhập cung Phu Thê theo Thái Tuế, giúp quan hệ được người xung quanh ủng hộ và dễ có người mai mối. Tuy nhiên, Hóa Kỵ lưu và Đà La lưu cũng nhập cung Phu Thê, cảnh báo về những chuyện tình cảm có thể chậm trễ hoặc dễ xảy ra hiểu lầm, ghen tuông. Về tháng, bạn nên chủ động trong tháng 3, tháng 4 và tháng 12, còn tháng 1 và tháng 2 thì nên giữ nhịp. Bạn nên lắng nghe nhau và tránh áp đặt, đồng thời nên giới thiệu người ấy với gia đình và bạn bè để mọi chuyện được thuận lợi hơn.

## 14. Năm 2027 có cưới được không? — lá số B

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 43 tuổi âm) · điểm 52/100

**Trượt** (49.337 giây): gán “Địa Không” với “Hợi” không khớp dữ kiện; gán “Địa Kiếp” với “Hợi” không khớp dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn, về câu hỏi của bạn về chuyện cưới hỏi trong năm 2027, lá số cho thấy mức độ trung bình với điểm số 52/100, nghĩa là có những lúc thuận lợi nhưng cũng có lúc cần thận trọng. Về cơ bản, năm này chưa có tầng vận hạn nào chỉ rõ việc cưới hỏi sẽ diễn ra, nhưng có những dấu hiệu đáng chú ý. Ở lá số gốc, cung Phu Thê tại Sửu có Thiên Đồng và Cự Môn đều ở trạng thái hãm, điều này cho thấy người bạn đời có thể là người thông minh, lanh lợi nhưng đôi khi dễ có bất đồng trong lời nói. Trong năm 2027, tiểu hạn tại cung Phu Thê rơi vào Hợi, nơi có Thái Âm miếu và gặp Hóa Kỵ, Địa Không, Địa Kiếp, cho thấy người bạn đời có thể dịu dàng, tinh tế nhưng cũng dễ có những hiểu lầm hoặc sự biến động trong mối quan hệ. Đặc biệt, năm Đinh Mùi này, Hóa Quyền lưu nhập cung Phu Thê gốc, báo hiệu một bên trong mối quan hệ có xu hướng muốn làm chủ và có thể có những quyết định lớn như ra mắt gia đình hoặc cưới hỏi. Tuy nhiên, Hóa Kỵ lưu cũng nhập cung Phu Thê, dễ gây ra sự hiểu lầm hoặc ghen tuông. Về mặt tích cực, Hóa Khoa lưu nhập cung Phu Thê theo Thái Tuế, cho thấy mối quan hệ được người xung quanh ủng hộ và có thể có người mai mối. Nếu bạn muốn tiến tới hôn nhân, bạn nên chủ động vào các tháng 3, 4 và 12 âm lịch. Tuy nhiên, bạn cũng cần lưu ý rằng Đà La lưu nhập cung Phu Thê, khiến chuyện tình cảm có thể chậm và dây dưa. Lời khuyên cho bạn là nên lắng nghe nhau và tránh áp đặt trong mối quan hệ, đồng thời nên giới thiệu người ấy với gia đình và bạn bè để mọi chuyện thuận lợi h

## 15. Gia đạo năm nay ra sao? — lá số B

App hiểu là: Gia đạo · năm 2026 (Bính Ngọ, 42 tuổi âm) · điểm 52/100

**Đạt** (49.015 giây, 317 chữ). Câu AI:

> Gia đạo của bạn trong năm 2026 (Bính Ngọ) được đánh giá ở mức 52/100, tức là trung bình, có những lúc thuận lợi nhưng cũng có lúc cần phải giữ nhịp và cẩn trọng. Điểm tựa chính của năm này nằm ở cung Phụ Mẫu tại Mùi, nơi có sự mượn của Thiên Đồng hãm và Cự Môn hãm, cho thấy trong năm, cha mẹ hoặc người thân có thể có những bất đồng trong lời nói, cần sự thấu hiểu và diễn đạt mềm mỏng hơn. Xét về lá số gốc, cung Phụ Mẫu tại Thìn có Thất Sát hãm và Kình Dương đắc, cho thấy mối quan hệ gia đình có thể có sự nóng tính hoặc cần sự quyết đoán, độc lập. Tuy nhiên, trong năm 2026, có những dấu hiệu tích cực xuất hiện. Cụ thể, Đà La lưu nhập cung Phụ Mẫu gốc, báo hiệu việc nhà có thể có những vấn đề dây dưa, khó dứt điểm. Ngược lại, Thiên Hỷ lưu nhập cung Phụ Mẫu của tiểu hạn, mang lại tin vui cho gia đình như chuyện cưới hỏi hay đón thành viên mới. Hóa Quyền lưu nhập cung Phụ Mẫu của lưu niên đại hạn cho thấy có người trong nhà sẽ đứng ra quyết định những việc lớn, nhưng cũng dễ dẫn đến bất đồng ý kiến. Về mặt tài chính và sự ổn định, Lộc Tồn lưu nhập cung Phụ Mẫu của lưu niên đại hạn giúp nền nếp gia đình có sự ổn định và có của để dành. Trong năm này, nếu có kế hoạch sinh con, bạn nên chăm sóc thai kỳ kỹ lưỡng và khám định kỳ theo hướng dẫn của bác sĩ. Về việc phân công công việc, bạn nên phân công rõ ràng để tránh xung đột. Bạn cũng nên chuẩn bị chu đáo cho những dịp vui sắp tới.

## 16. Đại vận tới tài chính thế nào? — lá số B

App hiểu là: Tài chính · đại vận 45–54 tuổi · điểm 58/100

**Đạt** (35.424 giây, 218 chữ). Câu AI:

> Chào bạn, về tài chính trong đại vận 45 đến 54 tuổi, kết quả chung là 58/100, mức độ này được đánh giá là thuận lợi. Trong 10 năm này, năm thuận nhất là 2034 và năm cần giữ nhất là 2036. Nhìn vào lá số gốc, cung Tài Bạch của bạn an tại Hợi có Thái Âm miếu, cho thấy bạn có duyên tích lũy tài sản, đặc biệt là tài sản điền sản. Tuy nhiên, sự hiện diện của Địa Không và Địa Kiếp đắc địa ở cung này cảnh báo rằng bạn có thể kiếm tiền táo bạo nhưng cũng dễ gặp tình trạng hụt hẫng. Về đại hạn 45–54 tuổi, cung Tài Bạch của đại hạn rơi vào Mão, nơi có Thái Dương vượng và Thiên Lương vượng, cùng với Hóa Quyền và Lộc Tồn, điều này báo hiệu danh tiếng và tài chính của bạn có thể rực rỡ, đặc biệt là trong các lĩnh vực học thuật hoặc y tế. Sự kết hợp giữa nền gốc 59/100 và đại hạn 58/100 đã mang lại mức điểm chung 58/100, cho thấy cơ hội tài chính là tốt nếu bạn biết cách chủ động. Bạn nên lập quỹ dự phòng và chia nhỏ các khoản đầu tư để quản lý rủi ro tốt hơn.

## 17. Năm 2023 sự nghiệp của tôi ra sao? — lá số B

App hiểu là: Sự nghiệp · năm 2023 (Quý Mão, 39 tuổi âm) · điểm 46/100

**Đạt** (53.424 giây, 370 chữ). Câu AI:

> Chào bạn, về sự nghiệp của bạn trong năm 2023, tổng thể nhận định là ở mức trung bình, có lúc thăng lúc xuống, với điểm số 46/100. Điều này cho thấy bạn sẽ trải qua một giai đoạn có nhiều biến động và áp lực. Xét trên lá số gốc, cung Quan Lộc tại Mùi có Thiên Đồng hãm và Cự Môn hãm, khiến bạn dễ gặp bất đồng trong lời nói, nên cần học cách diễn đạt mềm mỏng hơn. Về đại hạn 35–44 tuổi, cung Quan Lộc rơi vào Tuất với Liêm Trinh miếu và Thiên Phủ vượng, cho thấy bạn có xu hướng làm việc theo nguyên tắc và biết cách giữ gìn sự ổn định. Tuy nhiên, đại hạn này cũng cần bạn thận trọng vì có Linh Tinh hãm và Thiên Hình. Trong năm 2023, tiểu hạn tại Dậu cho thấy danh tiếng và tài lộc có thể rực rỡ, đặc biệt nếu liên quan đến học thuật hoặc y tế, nhờ có Thái Dương và Thiên Lương vượng. Tuy nhiên, Lưu Thái Tuế tại Mão cho thấy cung Quan Lộc bị ảnh hưởng bởi Thiên Đồng hãm và Cự Môn hãm, cùng với sự xuất hiện của Hóa Quyền lưu và Hóa Khoa lưu, điều này gợi ý rằng bạn có thể nhận được sự công nhận hoặc có cơ hội thăng tiến mới. Về những diễn biến cụ thể, năm 2023 có Hỷ Thần lưu nhập cung Quan Lộc, mang lại niềm vui trong quan hệ đồng nghiệp, nhưng cũng có Thiên Hư lưu khiến bạn cảm thấy công sức chưa được ghi nhận. Đồng thời, Thiên Mã lưu nhập cung Quan Lộc của lưu niên đại hạn cho thấy công việc có nhiều sự di chuyển, có thể là đi công tác xa hoặc mở rộng thị trường. Tháng 4 là tháng công việc nổi lên rõ rệt nhất, bạn nên chủ động trong các tháng 8, 11 và 12. Lời khuyên cho bạn là nên giảm bớt việc, ưu tiên những gì thật cần trong giai đoạn này, và không nên đổi việc vì cảm xúc nhất thời, nhất là trong những tháng có Kình Dương hay Đà La lưu.

## 18. Năm nào thuận để mua nhà? — lá số B

App hiểu là: Năm thuận cho nhà đất · dò 2026–2036

**Đạt** (33.345 giây, 223 chữ). Câu AI:

> Chào bạn, dựa vào các tầng vận được an sao, tôi xin luận giải về những năm thuận lợi để bạn cân nhắc mua nhà trong giai đoạn 2026 đến 2036. Đầu tiên, tôi thấy năm 2028 có điểm nhà đất đạt 60/100, đây là mức điểm khá tốt nhất trong các năm được xem xét. Điều này là do Hóa Lộc lưu xung chiếu cung Điền Trạch và Hóa Quyền lưu nhập cung Điền Trạch trong năm đó. Tuy nhiên, bạn cũng cần lưu ý rằng Hóa Kỵ lưu xung chiếu và Kình Dương lưu tại cung Điền Trạch, nên việc mua bán cần cân nhắc kỹ lưỡng. Tiếp theo, năm 2032 có điểm nhà đất là 56/100, với sự hỗ trợ của Hóa Lộc lưu nhập cung Điền Trạch và Hóa Quyền lưu nhập cung Điền Trạch gốc. Năm 2033 có điểm nhà đất là 54/100, được dựa trên đại hạn có điểm 58/100 tại cung Điền Trạch, nơi có Liêm Trinh miếu và Thiên Phủ vượng, nhưng cũng cần lưu ý Hóa Kỵ lưu xung chiếu. Tóm lại, năm 2028 có vẻ là năm thuận lợi nhất để bạn xem xét việc mua nhà. Lưu ý rằng đây chỉ là mức khả năng theo số tầng vận, quyết định cuối cùng vẫn nên do bạn tự cân nhắc kỹ lưỡng.

## 19. Con cái năm 2028 thế nào? — lá số B

App hiểu là: Gia đạo · năm 2028 (Mậu Thân, 44 tuổi âm) · điểm 53/100

**Đạt** (46.751 giây, 295 chữ). Câu AI:

> Chào bạn, về vấn đề con cái trong năm 2028 (Mậu Thân, 44 tuổi âm), tổng thể lá số cho thấy mức độ trung bình với điểm số 53/100, nghĩa là có những lúc thuận lợi nhưng cũng có những lúc cần phải giữ vững tâm lý. Điểm tựa chính đến từ tiểu hạn và Lưu Thái Tuế. Cụ thể, cung Phụ Mẫu của tiểu hạn năm 2028 rơi vào Sửu, gặp Thiên Đồng hãm và Cự Môn hãm, điều này cho thấy cha mẹ có thể dễ bất đồng trong lời nói, nên cần học cách diễn đạt mềm mỏng hơn. Tuy nhiên, khi xem xét Lưu Thái Tuế, cung Phụ Mẫu theo Thái Tuế rơi vào Dậu, gặp Thái Dương vượng và Thiên Lương vượng, cùng với Hữu Bật, Hóa Khoa lưu tọa thủ, điều này mang lại tin vui về gia đình, con cái có thể học hành tiến bộ và gia đình có tiếng tốt. Về các sự kiện cụ thể trong năm, Hóa Khoa lưu nhập cung Phụ Mẫu theo Thái Tuế, báo hiệu con cái có thể học hành tốt. Đồng thời, Đà La lưu nhập cung Phụ Mẫu gốc tại Thìn, có thể khiến việc nhà có chút dây dưa, khó dứt điểm. Nếu bạn có kế hoạch sinh con, năm này là thời điểm cần chăm sóc thai kỳ kỹ lưỡng và khám định kỳ theo hướng dẫn của bác sĩ. Về tháng, bạn nên chủ động vào tháng 1, tháng 5 và tháng 11, còn các tháng 2, 4, 6 là những tháng cần giữ nhịp vì các vấn đề gia đạo sẽ nổi lên rõ rệt. Bạn nên khích lệ người nhà và phân công công việc rõ ràng để mọi việc suôn sẻ hơn.

## 20. Cung Phu Thê của tôi có sao gì? — lá số B

App hiểu là: Dữ kiện lá số: cung Phu Thê

**Đạt** (32.694 giây, 265 chữ). Câu AI:

> Chào bạn, tôi đã xem qua cung Phu Thê của bạn rồi. Cung này nằm ở cung Sửu và có sự hội tụ của hai chính tinh là Thiên Đồng và Cự Môn, cả hai đều đang ở trạng thái Hãm. Bên cạnh đó, cung này còn có các phụ tinh là Hoa Cái, Phá Toái, Phục Binh, Thái Tuế và Suy. Với sự kết hợp của bộ Đồng Cự đồng cung tại cung Phu Thê, bạn nên lưu ý rằng người bạn đời của bạn có thể mang nét dễ bất đồng trong lời nói, vì vậy hai bạn cần học cách diễn đạt mềm mỏng hơn để tránh xung đột. Về các sao phụ tinh, Phá Toái có thể báo hiệu những hao hụt hay đổ vỡ nhỏ trong mối quan hệ, còn Phục Binh nhắc nhở bạn cần đề phòng những tiểu nhân ngấm ngầm. Theo sách cổ bàn rằng, khi cung Phu Thê có Thiên Đồng thì bạn nên cân nhắc việc lập gia đình muộn hơn. Đồng thời, vì có Cự Môn cùng Thiên Đồng, người phối ngẫu của bạn có thể là người thông minh và lanh lợi. Cự Môn còn gợi ý rằng bạn nên chọn người hơn tuổi. Thái Tuế ở cung này cho thấy người bạn đời có trách nhiệm với các việc chung và lời nói của họ có trọng lượng, và mối quan hệ sẽ được thuận lợi khi bạn lấy chính danh làm gốc. Bạn nên chú ý đến việc giao tiếp và giữ sự hòa nhã trong cuộc sống hôn nhân nhé.

## 21. tai chinh nam 2028 the nao — lá số B

App hiểu là: Tài chính · năm 2028 (Mậu Thân, 44 tuổi âm) · điểm 49/100

**Đạt** (48.822 giây, 298 chữ). Câu AI:

> Chào bạn, về vấn đề tài chính của bạn trong năm 2028 (Mậu Thân, 44 tuổi âm), kết quả tổng thể là 49/100, mức trung bình, có lúc lên có lúc xuống. Nền gốc của bạn về tài chính khá tốt với 59/100, nhờ cung Tài Bạch an tại Hợi có Thái Âm miếu và Thiên Mã, cho thấy bạn có duyên tích lũy tài sản, nhưng cũng có sự táo bạo trong kiếm tiền do có Địa Không và Địa Kiếp. Tuy nhiên, khi xét đến năm 2028, các tầng vận hạn cho thấy sự cần thận trọng. Cụ thể, trong năm này, Hóa Quyền lưu nhập cung Tài Bạch gốc, giúp bạn có khả năng nắm quyền quyết định về tiền bạc, có thể đứng ra quản lý ngân sách hoặc khoản đầu tư. Nhưng đồng thời, Đà La lưu nhập cung Tài Bạch theo Thái Tuế, báo hiệu tiền về có thể chậm trễ hoặc các thủ tục tài chính bị kéo dài, rườm rà. Ngoài ra, Bạch Hổ lưu cũng có thể khiến bạn phát sinh chi phí liên quan đến sức khỏe hoặc gia đình. Về mặt chi tiêu, tiểu hạn tại Thân với Phá Quân hãm cảnh báo bạn dễ thăng trầm vì sự liều lĩnh, ngang tàng, nên cần phải giữ kỷ luật chi tiêu. Theo tháng, bạn nên chủ động và quyết đoán vào tháng 1, tháng 5 và tháng 11, còn tháng 2 và tháng 6 thì nên giữ nhịp. Lưu ý quan trọng nhất là bạn nên dự phòng tiền mặt và mọi việc cho vay hay đòi nợ đều phải có giấy tờ rõ ràng. Đồng thời, bạn không nên mở rộng quy mô kinh doanh hoặc vay mượn lớn để tăng quy mô trong năm này.

## 22. Tôi nên lưu ý gì năm 2027? — lá số B

App hiểu là: Nên / Không nên · năm 2027 · điểm 47/100

**Đạt** (43.783 giây, 328 chữ). Câu AI:

> Chào bạn, tôi đã xem qua lá số của bạn cho năm 2027 (Đinh Mùi, 43 tuổi âm). Vận chung của bạn năm nay được đánh giá là 47/100, ở mức trung bình, có lúc thuận có lúc cần giữ. Điều này cho thấy năm 2027 sẽ có những thăng trầm nhất định, và kết quả phụ thuộc rất nhiều vào cách bạn lựa chọn hành động. Về công việc, Hóa Lộc lưu (Thái Âm) nhập cung Quan Lộc theo Thái Tuế (Hợi) mang lại cơ hội mới và nguồn thu nhập tốt, nhưng đồng thời Kình Dương lưu cũng nhập cung Quan Lộc gốc (Mùi), khiến bạn phải đối mặt với sự cạnh tranh gay gắt và áp lực về chỉ tiêu công việc. Về tài chính, Hóa Lộc lưu (Thái Âm) nhập cung Tài Bạch gốc (Hợi) báo hiệu dòng tiền vào khá hơn, có thể có khoản thu thêm, tuy nhiên Thiên Khốc lưu cũng nhập cung Tài Bạch gốc (Hợi) khiến bạn dễ nảy sinh lo lắng về tiền bạc. Trong tình cảm, Hóa Quyền lưu (Thiên Đồng) nhập cung Phu Thê gốc (Sửu) cho thấy một bên trong mối quan hệ có xu hướng muốn làm chủ hơn, và có thể có những quyết định lớn liên quan đến gia đình hoặc hôn nhân. Để năm 2027 suôn sẻ, bạn nên giữ kết nối thường xuyên với những người thân và bạn bè đáng tin cậy để chia sẻ áp lực. Khi cảm thấy căng thẳng kéo dài, hãy chủ động tìm đến chuyên gia tâm lý hoặc bác sĩ để được hỗ trợ. Bạn cũng nên giảm bớt các việc không cần thiết và không nên mở rộng kinh doanh hay vay mượn lớn một cách nóng vội. Hãy lưu ý chăm sóc sức khỏe, đặc biệt là giấc ngủ và chế độ ăn uống, đồng thời không nên bỏ qua những dấu hiệu bất thường kéo dài của cơ thể nhé.

## 23. Tháng 6 năm nay sức khỏe thế nào? — lá số B

App hiểu là: Sức khỏe (lưu ý) · tháng 6 âm lịch năm 2026 · điểm 48/100

**Đạt** (28.875 giây, 195 chữ). Câu AI:

> Chào bạn, về sức khỏe trong tháng 6 âm lịch năm 2026, điểm số được đánh giá là 48/100, tức là ở mức trung bình, có lúc tốt có lúc cần chú ý. Nền sức khỏe chung của cả năm 2026 là 46/100. Tháng này có tác động gián tiếp đến sức khỏe vì Nguyệt hạn nằm tại cung Điền Trạch, và cung này lại nằm trong thế tam hợp với cung Tật Ách gốc của bạn. Cụ thể hơn, Tứ Hóa tháng của năm Ất mang Hóa Khoa đến cung Điền Trạch (Ngọ), nơi có Tử Vi miếu, điều này cho thấy các vấn đề liên quan đến nơi ở hoặc tài sản cố định có thể được chú ý nhiều hơn. Tuy nhiên, vì Hóa Khoa này lại chạm vào cung Tật Ách gốc, nên bạn cần lưu ý đến các vấn đề sức khỏe tiềm ẩn. Trong tháng này, bạn nên chú ý đến việc giữ gìn sức khỏe và không nên chủ quan. Lời khuyên dành cho bạn là nên giữ gìn sức khỏe và chú ý đến các vấn đề liên quan đến nơi ở hoặc tài sản cố định.

## 24. Tính cách của tôi thế nào? — lá số B

App hiểu là: Tính cách · lá số gốc (trọn đời) · điểm 75/100

**Đạt** (39.65 giây, 285 chữ). Câu AI:

> Chào bạn, dựa trên lá số gốc của bạn, tính cách của bạn được đánh giá là rất thuận, đạt điểm 75/100. Bạn có một nền tảng tính cách rất tốt, được hoàn cảnh nâng đỡ. Ở cung Mệnh tại Mão, bạn có Thái Dương vượng và Thiên Lương vượng, điều này cho thấy bạn là người có danh tiếng và tài năng dễ dàng tỏa sáng, đặc biệt hợp với các lĩnh vực học thuật hay y tế. Bạn mang tính cách quang minh chính trực, uy quyền nhờ Thái Dương, đồng thời cũng rất có lòng nhân ái, thích giúp đỡ người khác như Thiên Lương. Bạn là người có khả năng quản lý và ảnh hưởng nhất định nhờ Hóa Quyền, và bạn cũng biết cách giữ gìn tài lộc nhờ Lộc Tồn. Tuy nhiên, vì có Hỏa Tinh đắc địa, bạn cũng có thể là người nhanh nhạy và quyết đoán, nhưng đôi khi cũng dễ nóng vội. Bạn có xu hướng dè dặt vì Lộc Tồn bị Kình Đà kẹp, và đôi khi cũng có những nỗi buồn phiền hay sự thay đổi bất ngờ do Tang Môn. Về mặt trí tuệ, cung Phúc Đức có Thiên Cơ vượng, cho thấy bạn là người thông minh, giỏi phân tích và có khả năng ứng biến tốt. Trong các giai đoạn đại vận, tính cách của bạn có sự thay đổi nhẹ theo từng độ tuổi, nhưng giai đoạn từ 65 đến 74 tuổi là lúc tính cách của bạn đạt mức thuận nhất. Bạn nên giữ sự điềm tĩnh và cân bằng trong cuộc sống, tránh để sự nóng vội của Hỏa Tinh chi phối các quyết định quan trọng.

## 25. Năm sau sức khỏe của tôi cần lưu ý gì? — lá số D

App hiểu là: Nên / Không nên · sức khỏe (lưu ý) · năm 2027 · điểm 40/100

**Đạt** (35.545 giây, 248 chữ). Câu AI:

> Điểm chung: 40/100. Chào bạn, dựa vào lá số của bạn, sức khỏe năm 2027 (Đinh Mùi) được dự báo ở mức 38/100, nên bạn cần phải hết sức thận trọng. Về mặt đại hạn và lưu niên đại hạn, cung Tật Ách của bạn rơi vào Ngọ, nơi có Thiên Lương miếu gặp Địa Kiếp hãm, điều này cho thấy có những áp lực nhất định liên quan đến sức khỏe. Ở cung Tật Ách gốc (Sửu), bạn có Thiên Phủ, Kình Dương, Thai Phụ và Quả Tú. Thiên Phủ chủ về sự tích lũy, nhưng Kình Dương lại báo hiệu cần cẩn trọng khi vận động mạnh. Theo ngũ hành, bạn nên để ý đến các vấn đề liên quan đến Thổ như dạ dày hay đường tiêu hóa và nên đi khám khi có dấu hiệu kéo dài. Cụ thể trong năm 2027, dù có Hóa Lộc lưu và Hóa Khoa lưu nhập cung Tật Ách giúp sức khỏe hồi phục tốt và dễ tìm được thầy thuốc giỏi, nhưng bạn cũng sẽ gặp Thiên Hư lưu khiến tinh thần dễ buồn bã, mất ngủ và căng thẳng. Về các tháng, bạn nên chủ động chăm sóc sức khỏe vào tháng 2, tháng 3 và tháng 8, đồng thời nên giữ nhịp sinh hoạt ổn định vào tháng 1 và tháng 9. Lời khuyên dành cho bạn là nên đi khám định kỳ và luôn hỏi ý kiến chuyên môn trước khi dùng thuốc nhé.

## 26. Hậu vận của tôi thế nào? — lá số D

App hiểu là: Tổng quan · giai đoạn 55–99 tuổi · điểm 36/100

**Đạt** (37.604 giây, 230 chữ). Câu AI:

> Chào bạn, về hậu vận của bạn, lá số cho thấy bạn cần phải thận trọng trong giai đoạn này với điểm chung là 36/100. Nhìn vào lá số gốc, cung Mệnh của bạn an tại Ngọ với sao Thiên Lương ở vị trí miếu, điều này cho thấy bạn là người lương thiện, bác ái và hay giúp đỡ người khác. Tuy nhiên, sự hiện diện của sao Địa Kiếp hãm lại mang đến sự biến động và hao tổn. Về các đại vận sau này, giai đoạn 46 đến 55 tuổi, đại hạn tại Quan Lộc ở Tuất với sao Thiên Đồng hãm, điểm số chỉ là 33/100, đây là giai đoạn cần giữ và thận trọng. Sau đó, từ 56 đến 65 tuổi, đại hạn tại Nô Bộc ở Hợi với Vũ Khúc hãm và Phá Quân hãm, điểm số là 38/100. Giai đoạn thuận lợi nhất cho bạn là từ 66 đến 75 tuổi, đại hạn tại Thiên Di ở Tý với Thái Dương hãm, đạt điểm 43/100. Trong giai đoạn này, bạn có thể cân nhắc các hướng đi liên quan đến hành chính, giáo dục hoặc y tế. Bạn nên chủ động tự lực, xây dựng nền tảng vững chắc từng bước thay vì trông chờ hoàn cảnh. Lưu ý là không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy.

## 27. Năm 2027 tài chính ra sao? — lá số D

App hiểu là: Tài chính · năm 2027 (Đinh Mùi, 65 tuổi âm) · điểm 40/100

**Trượt** (47.686 giây): gán “Linh Tinh” với “Mão” không khớp dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn, về tình hình tài chính trong năm 2027 (Đinh Mùi, 65 tuổi âm), tổng thể nhận định là 40/100, tức là bạn cần phải hết sức thận trọng trong việc quản lý tiền bạc. Lá số gốc của bạn cho thấy cung Tài Bạch tại Dần có Thiên Cơ và Thái Âm đều ở thế hãm, điều này báo hiệu tiền tài khó tụ và bạn cần phải rất khéo léo, tinh tế trong mọi quyết định tài chính. Tuy nhiên, trong năm 2027, có những yếu tố tích cực xuất hiện. Cụ thể, Hóa Lộc lưu (Thái Âm) và Hóa Khoa lưu (Thiên Cơ) đều nhập thẳng vào cung Tài Bạch gốc tại Dần, điều này mang lại dòng tiền vào khá hơn, có thể có khoản thu thêm hoặc cơ hội làm ăn mới. Đồng thời, Hóa Khoa lưu cũng giúp tài chính của bạn ổn định nhờ uy tín cá nhân và dễ nhận được sự hỗ trợ khi cần. Tuy nhiên, Lưu Thái Tuế tại Mão lại cho thấy sự xuất hiện của Tham Lang hãm gặp Hóa Kỵ, cùng với Linh Tinh, báo hiệu có thể phát sinh chi phí bất ngờ cho sức khỏe hoặc việc gia đình. Về mặt thời điểm, bạn nên chủ động tìm nguồn thu vào các tháng 2, 3, 8 âm lịch, nhưng cũng cần giữ nhịp và cẩn trọng vào tháng 1 và tháng 10. Dù có những cơ hội thu nhập, nhưng do phần lớn các tầng vận hạn đều có sự yếu kém, nên bạn hãy ưu tiên việc giữ an toàn và tích lũy vốn trước khi nghĩ đến việc mở rộng quy mô kinh doanh. Lời khuyên dành cho bạn là nên chủ động tìm nguồn thu nhưng vẫn giữ phần dự phòng, đồng thời luôn giữ chữ tín và minh bạch trong mọi giao dịch tiền bạc."}

## 28. Con cái của tôi thế nào? — lá số D

App hiểu là: Gia đạo · năm 2026 (Bính Ngọ) · điểm 41/100

**Đạt** (44.014 giây, 287 chữ). Câu AI:

> Chào bạn, tôi sẽ luận giải về con cái của bạn dựa trên lá số Tử Vi. Điểm chung cho gia đạo năm 2026 là 41/100, mức độ này cho thấy bạn cần phải thận trọng trong việc chăm lo cho con cái và gia đình. Nhìn vào lá số gốc, cung Tử Tức (nơi đại diện cho con cái) có sự hiện diện của các sao tốt như Hóa Khoa, Văn Xương, Thiên Khôi, Linh Tinh, nhưng đồng thời cũng bị ảnh hưởng bởi Hóa Kỵ, Tả Phù, Văn Xương, Thiên Khôi, Linh Tinh từ cung Tử Tức tam hợp, điều này cho thấy con cái có những nét tốt đẹp nhưng cũng dễ gặp phải những vấn đề về giấy tờ hoặc sự thị phi nhỏ. Trong năm 2026, cung Phụ Mẫu của tiểu hạn rơi vào cung Nô Bộc, nơi có Vũ Khúc hãm và Phá Quân hãm, điều này gợi ý rằng trong năm này, các vấn đề liên quan đến con cái có thể liên quan đến tài chính hoặc sự thay đổi lớn trong cuộc sống của chúng. Cụ thể trong năm 2026, có Hóa Kỵ lưu chạm vào cung Phụ Mẫu, dễ khiến trong nhà có những hiểu lầm hoặc những chuyện cũ được nhắc lại, ảnh hưởng đến không khí gia đình. Về mặt thời điểm, bạn nên chủ động quan tâm và giải quyết các vấn đề liên quan đến gia đạo vào tháng 2, tháng 8 và tháng 10. Tuy nhiên, bạn cũng nên giữ nhịp và cẩn trọng vào tháng 9 và tháng 12. Lời khuyên dành cho bạn là nên chăm lo việc nhà đều đặn để duy trì sự ổn định cho gia đình.

## 29. Năm 2024 gia đạo thế nào? — lá số D

App hiểu là: Gia đạo · năm 2024 (Giáp Thìn, 62 tuổi âm) · điểm 40/100

**Đạt** (49.312 giây, 328 chữ). Câu AI:

> Chào bạn, về tình hình gia đạo trong năm 2024, lá số cho thấy mức độ cần thận trọng, với điểm chung là 40/100. Điều này có nghĩa là bạn cần giữ sự cẩn trọng trong các mối quan hệ gia đình và các vấn đề liên quan đến cha mẹ. Xét ở lá số gốc, cung Phụ Mẫu tại Mùi có Liêm Trinh và Thất Sát đắc địa, cho thấy cha mẹ bạn là người mạnh mẽ, quyết đoán nhưng cũng cần chú ý khi đi lại. Trong năm 2024, tiểu hạn tại Tý, cung Phụ Mẫu rơi vào Sửu với Thiên Phủ bình hòa, mang lại sự chu đáo, rộng lượng từ cha mẹ, nhưng sự xuất hiện của Kình Dương và Quả Tú cũng báo hiệu có những điều cần lưu ý về mặt sức khỏe hoặc sự bảo thủ trong gia đình. Về đại hạn 56-65 tuổi, cung Phụ Mẫu của đại hạn rơi vào Tý với Thái Dương hãm, cho thấy cha mẹ có thể nóng tính hoặc hay lo lắng cho người khác. Đặc biệt, năm 2024 có Hóa Lộc lưu nhập thẳng cung Phụ Mẫu gốc, và Thiên Hỷ lưu cũng nhập cung này theo Thái Tuế, đây là những dấu hiệu tốt cho thấy gia đình có thể đón tin vui, có sự êm ấm hoặc có thêm tài sản chung. Tuy nhiên, Hóa Quyền lưu xung chiếu và Đà La lưu cũng xuất hiện, dễ gây ra những bất đồng ý kiến hoặc sự dây dưa trong việc nhà. Về các tháng cụ thể, bạn nên chủ động quan tâm và thăm hỏi cha mẹ vào các tháng 5, 6 và 11. Ngược lại, những tháng như tháng 8, 10, 12 cần giữ nhịp và cẩn trọng hơn vì các vấn đề gia đạo có thể nổi lên rõ rệt. Lời khuyên dành cho bạn là nên dành thời gian quan tâm đến sức khỏe của cha mẹ mình.

## 30. Năm nào tôi nên giữ gìn sức khỏe? — lá số D

App hiểu là: Năm thuận cho sức khỏe · dò 2026–2036

**Đạt** (28.868 giây, 204 chữ). Câu AI:

> Chào bạn, dựa trên các thông tin về vận trình sức khỏe của bạn trong giai đoạn từ năm 2028 đến 2031, tôi xin chia sẻ một vài điều để bạn tham khảo nhé. Trong những năm này, điểm sức khỏe của bạn đều ở mức cần lưu ý, nên bạn nên đặc biệt chú trọng việc giữ gìn sức khỏe. Cụ thể, năm 2028, điểm sức khỏe của bạn là 44/100, và năm 2031 cũng có điểm sức khỏe là 44/100. Những năm này, có sự xuất hiện của các yếu tố như Hóa Lộc lưu nhập cung Tật Ách năm và Hóa Quyền lưu xung chiếu cung Tật Ách, cùng với sự xuất hiện của Tang Môn lưu tại cung Tật Ách trong năm 2031. Điều này cho thấy bạn cần phải cẩn trọng hơn trong việc chăm sóc bản thân. Năm 2030 cũng có điểm sức khỏe là 43/100, với sự ảnh hưởng của Hóa Quyền lưu xung chiếu cung Tật Ách. Vì vậy, bạn nên chủ động giữ gìn sức khỏe trong suốt giai đoạn này. Lưu ý là những mức điểm này chỉ là khả năng theo vận trình, bạn vẫn là người quyết định mọi việc của mình.

## 31. Đại vận này gia đạo thế nào? — lá số D

App hiểu là: Gia đạo · đại vận 56–65 tuổi · điểm 37/100

**Đạt** (39.291 giây, 268 chữ). Câu AI:

> Chào bạn, tôi là trợ lý Tử Vi Tự Xem. Về câu hỏi của bạn về gia đạo trong đại vận 56 đến 65 tuổi, điểm chung cho giai đoạn này là 37/100, nên bạn cần giữ sự thận trọng. Trong 10 năm này, năm thuận nhất là 2026 với điểm 41/100, còn năm cần giữ nhất là 2021 với điểm 36/100. Nhìn vào lá số gốc, cung Phụ Mẫu của bạn an tại Mùi, có Liêm Trinh và Thất Sát đều đắc địa. Điều này cho thấy cha mẹ bạn là những người mạnh mẽ, quyết đoán, nhưng cũng cần cẩn trọng khi đi lại hoặc vận động. Cung này còn có các sao như Long Trì, Phượng Các, Giải Thần, Thiên Giải, Hoa Cái, Hỷ Thần, Quan Phù, mang ý nghĩa về sự thanh lịch, khoa danh, nhưng cũng có thể liên quan đến giấy tờ pháp lý. Khi xem xét đại hạn từ 2018 đến 2027, cung Phụ Mẫu của đại hạn rơi vào cung Thiên Di tại Tý, nơi có Thái Dương hãm địa. Điều này có thể khiến cha mẹ bạn có xu hướng nóng tính hơn, hoặc hay lo lắng cho người khác, vì Thái Dương cũng tượng trưng cho người cha. Tuy nhiên, ở cung này vẫn có Lộc Tồn và Thiên Đức, mang lại một chút sự an ủi. Tổng hợp lại, vì cả lá số gốc và đại hạn đều có tầng yếu, nên bạn nên phòng trước ở mặt này. Lời khuyên cho bạn là nên phòng trước ở mặt này để gia đạo được ổn định hơn.

## 32. Nhà đất năm sau có thuận không? — lá số D

App hiểu là: Nhà đất · năm 2027 (Đinh Mùi, 65 tuổi âm) · điểm 36/100

**Đạt** (55.153 giây, 364 chữ). Câu AI:

> Nhà đất năm 2027 (Đinh Mùi) với điểm chung là 36/100, nên bạn cần phải hết sức thận trọng trong mọi quyết định liên quan đến tài sản cố định. Nhìn vào lá số gốc, cung Điền Trạch an tại Dậu có Tử Vi bình hòa và Tham Lang hãm, cho thấy bạn có sự giằng co giữa ham muốn vật chất và tâm linh, nền tảng này được đánh giá là 40/100, cần thận trọng. Khi xem xét đại hạn từ năm 2018 đến 2027, cung Điền Trạch rơi vào Dần với Thiên Cơ hãm và Thái Âm hãm, mang lại cảm giác cần thay đổi môi trường nhưng lại gặp khó khăn, được đánh giá là 32/100, nên giữ vững hiện trạng. Lưu niên đại hạn cũng rơi vào Dần với Thiên Cơ hãm – Thái Âm hãm, điểm số cũng là 32/100, cho thấy kết quả trong năm này cũng cần sự kiên nhẫn. Đối với tiểu hạn năm 2027, cung Điền Trạch rơi vào Tý với Thái Dương hãm, điểm số là 42/100, cảnh báo về việc dễ hao tổn vì người khác hoặc danh tiếng chưa tương xứng với công sức bỏ ra. Lưu Thái Tuế năm Đinh Mùi cho thấy cung Điền Trạch theo Thái Tuế rơi vào Tuất với Thiên Đồng hãm, điểm số là 37/100, cho thấy có sự thay đổi trong việc quản lý tài chính. Cụ thể trong năm 2027, Hóa Quyền lưu nhập cung Điền Trạch theo Thái Tuế, bạn có thể nắm quyền quyết định về tiền bạc hoặc đầu tư, nhưng cũng có Tang Môn lưu nhập cung Điền Trạch gốc, dễ phát sinh chi phí sửa chữa hoặc sức khỏe. Hóa Lộc lưu và Hóa Khoa lưu cũng xuất hiện, báo hiệu có dòng tiền vào hoặc cơ hội làm ăn. Về mặt thời gian, bạn nên chủ động trong các tháng 2, 3, 8 âm lịch, nhưng cần giữ nhịp ở tháng 1 và 5 âm lịch. Lời khuyên cho bạn là nên quyết đoán nhưng phải có kế hoạch rõ ràng, đồng thời hãy giữ quỹ dự phòng và cân nhắc bảo hiểm phù hợp để an toàn hơn.

## 33. Năm 2015 tôi gặp chuyện gì? — lá số E

App hiểu là: Năm 2015 (Ất Mùi) theo lá số · đã có gì · điểm 53/100

**Đạt** (44.282 giây, 337 chữ). Câu AI:

> Chào bạn, dựa trên lá số của bạn, năm 2015 (Ất Mùi) là một năm có vận chung ở mức 53/100, tức là trung bình, có những lúc thuận lợi và có lúc cần giữ vững. Về mặt công việc, năm đó bạn dễ được trao thêm quyền hạn hoặc nhận thêm trách nhiệm quản lý, có tiếng nói lớn hơn trong tập thể nhờ Hóa Quyền lưu (Thiên Lương) nhập cung Quan Lộc gốc. Tuy nhiên, cũng có thể bạn cảm thấy tâm trạng chùng xuống vì công sức chưa được ghi nhận, do Thiên Khốc lưu nhập cung Quan Lộc theo Thái Tuế. Về tiền bạc, năm 2015 có những tín hiệu tích cực với Hóa Lộc lưu (Thiên Cơ) nhập cung Tài Bạch và Lộc Tồn lưu nhập cung Tài Bạch, cho thấy dòng tiền vào khá hơn, có cơ hội làm ăn hoặc thu nhập ổn định. Trong tình cảm, Hóa Quyền lưu (Thiên Lương) nhập cung Phu Thê có thể cho thấy một bên trong mối quan hệ muốn nắm quyền chủ động, hoặc có những quyết định quan trọng liên quan đến gia đình như cưới hỏi. Ngoài ra, Tứ Hóa năm Ất cho thấy Hóa Lộc (Thiên Cơ) vào Phúc Đức mang lại lợi ích về tinh thần và họ hàng, Hóa Quyền (Thiên Lương) vào Quan Lộc giúp tăng thêm quyền hạn, Hóa Khoa (Tử Vi) vào Điền Trạch mang lại tiếng tốt về nhà cửa, và Hóa Kỵ (Thái Âm) vào Mệnh có thể khiến bạn cảm thấy có điều gì đó vướng bận, cần phải lo lắng nhiều hơn về bản thân. Về sức khỏe, năm đó bạn có thể cần lưu ý vì có Đà La lưu tại cung Tật Ách. Về tháng, tháng thuận nhất là tháng 7, tháng 1 và tháng 11, còn tháng cần thận trọng là tháng 4 và tháng 12. Bạn nên giữ vững sự ổn định trong công việc và chú ý đến sức khỏe của mình trong năm đó.

## 34. Năm 2027 tôi có thăng chức không? — lá số E

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 50/100

**Đạt** (49.83 giây, 324 chữ). Câu AI:

> Chào bạn, về câu hỏi của bạn về việc có thăng chức trong năm 2027 hay không, kết quả tổng thể cho sự nghiệp năm Đinh Mùi là 50/100, mức trung bình, có lên có xuống. Dựa trên các tầng luận giải, năm 2027 có những dấu hiệu cho thấy bạn có thể được trao thêm quyền hạn hoặc có tiếng nói lớn hơn trong tập thể, nhưng cũng đi kèm với nhiều sự chậm trễ và áp lực. Cụ thể, trong năm này, Hóa Quyền lưu (tức là quyền lực) sẽ nhập vào cung Quan Lộc theo Thái Tuế, điều này dễ mang lại cơ hội được trao trách nhiệm quản lý hoặc có vị thế cao hơn. Tuy nhiên, Đà La lưu cũng nhập vào cung Quan Lộc gốc, khiến mọi việc dễ bị chậm trễ hoặc vướng vào các thủ tục rườm rà, đòi hỏi bạn phải kiên nhẫn. Bên cạnh đó, Thiên Mã lưu cũng xuất hiện, báo hiệu công việc có thể liên quan đến việc di chuyển, đi công tác xa hoặc mở rộng thị trường mới. Về mặt cảm xúc, Thiên Khốc lưu có thể khiến bạn cảm thấy công sức bỏ ra chưa được ghi nhận đúng mức. Trong các tháng, bạn nên chủ động trong công việc vào tháng 3, tháng 4 và tháng 7, còn tháng 8 và tháng 10 thì nên giữ nhịp độ ổn định. Lá số gốc của bạn cho thấy cung Quan Lộc có Thiên Lương hãm, nên bạn hợp với các môi trường ổn định như y tế, giáo dục hoặc tư vấn. Lời khuyên cho bạn là nên nhận vai trò dẫn dắt nhưng hãy giữ cách nói mềm mỏng để tránh va chạm, và đừng quên chia việc nhỏ ra để kiểm tra tiến độ thường xuyên. Bạn cũng nên chọn môi trường ổn định và đi đường dài thay vì thay đổi hướng đi liên tục.

## 35. Đi xa lập nghiệp năm 2028 được không? — lá số E

App hiểu là: Di chuyển, xuất ngoại · năm 2028 (Mậu Thân, 41 tuổi âm) · điểm 58/100

**Đạt** (39.687 giây, 256 chữ). Câu AI:

> Chào bạn, về câu hỏi đi xa lập nghiệp vào năm 2028, kết quả chung là 58/100, cho thấy xu hướng là thuận lợi. Lá số gốc của bạn có cung Thiên Di ở Mùi, mượn Thái Dương và Thái Âm đắc địa, cho thấy bạn có sự cân bằng giữa hướng ngoại và nội tâm, tuy nhiên đôi khi cũng có chút do dự. Năm 2028, đại hạn của bạn đang ở cung Điền Trạch với Tử Vi và Thiên Tướng vượng, cho thấy một giai đoạn 10 năm khá thuận lợi. Cụ thể trong năm 2028, tiểu hạn tại Dần với Liêm Trinh vượng, và lưu niên đại hạn cũng rơi vào Thân với Liêm Trinh vượng, đều cho thấy sự nghiêm minh và có gu thẩm mỹ trong các mối quan hệ mới. Đặc biệt, năm này có nhiều yếu tố hỗ trợ cho việc đi xa, như Hóa Lộc lưu nhập cung Thiên Di, giúp công tác, buôn bán ở nơi khác thuận lợi, và Thiên Mã lưu cũng cho thấy bạn sẽ có nhiều chuyến đi hoặc có thể đổi chỗ ở, chỗ làm. Tuy nhiên, Thiên Hư lưu cũng nhắc nhở rằng khi đi xa, bạn dễ cảm thấy nhớ nhà và cô đơn. Về mặt thời điểm cụ thể, bạn nên chủ động nắm bắt cơ hội ở nơi khác vào tháng 5, tháng 6 và tháng 7 âm lịch. Bạn nên chủ động mở rộng quan hệ ở nơi mới để thuận lợi hơn cho việc lập nghiệp.

## 36. Tình duyên đại vận này thế nào? — lá số E

App hiểu là: Tình duyên, hôn nhân · đại vận 34–43 tuổi · điểm 55/100

**Đạt** (33.05 giây, 225 chữ). Câu AI:

> Chào bạn, về tình duyên và hôn nhân trong đại vận 34 đến 43 tuổi, tổng thể được đánh giá là 55/100, mức độ này cho thấy tình duyên có xu hướng thuận lợi nhưng vẫn cần sự chủ động của bạn. Cung Phu Thê gốc của bạn an tại Hợi, có sao Thiên Đồng đắc, người bạn đời thường là người hiền hòa, dễ chịu và biết hưởng thụ cuộc sống, quan hệ có xu hướng thuận. Tuy nhiên, sách cổ bàn rằng cung Phu Thê có Thiên Đồng gặp sát tinh thì dễ có sự xa cách, nên bạn nên cân nhắc việc lập gia đình muộn hơn. Trong đại vận này, cung Phu Thê của đại hạn rơi vào Dần, nơi có sao Tham Lang đắc cùng Hóa Lộc và Thiên Mã, điều này cho thấy người bạn đời trong giai đoạn này thường rất lôi cuốn, linh hoạt và giỏi giao tiếp, và quan hệ có xu hướng thuận lợi hơn. Điểm chung của bạn là 55/100, nên bạn cần giữ sự chủ động để mọi việc tốt đẹp hơn. Trong 10 năm này, năm thuận nhất là 2024 với điểm 59/100, còn năm cần giữ nhất là 2027 với điểm 53/100. Bạn nên chủ động vun đắp mối quan hệ và giữ thái độ tích cực trong giai đoạn này.

## 37. Năm 2019 công việc của tôi thế nào? — lá số E

App hiểu là: Sự nghiệp · năm 2019 (Kỷ Hợi, 32 tuổi âm) · điểm 49/100

**Đạt** (45.262 giây, 293 chữ). Câu AI:

> Chào bạn, về công việc của bạn trong năm 2019, nhìn chung có thể đánh giá ở mức trung bình, cụ thể là 49/100, có lúc lên có lúc xuống. Nền gốc của bạn tại cung Quan Lộc ở Tỵ có Thiên Lương hãm, cho thấy bạn dễ lo chuyện người khác và đôi khi cảm thấy cô độc, nên chọn môi trường ổn định như y tế, giáo dục hoặc tư vấn. Trong năm 2019, tiểu hạn tại cung Tài Bạch, cho thấy bạn có thể hoạt ngôn và giỏi kinh doanh nhưng cũng dễ đa nghi. Đặc biệt, Lưu Thái Tuế tại Mão, cung Quan Lộc của Thái Tuế rơi vào Mão với Thiên Cơ miếu và Cự Môn miếu, nhưng lại gặp Hóa Kỵ, điều này khiến bạn cần phải thận trọng trong công việc. Về các sự việc cụ thể trong năm, có Hóa Khoa lưu nhập cung Quan Lộc gốc, giúp uy tín của bạn được chú ý và có sự nâng đỡ từ người có vị thế. Tuy nhiên, Đà La lưu nhập cung Quan Lộc cũng khiến mọi việc dễ chậm trễ, vướng thủ tục, và Thiên Hư lưu làm tâm trạng bạn dễ chùng xuống vì cảm thấy công sức chưa được ghi nhận. Đồng thời, Thiên Mã lưu cho thấy công việc của bạn có nhiều di chuyển, có thể là đi công tác xa hoặc mở rộng thị trường mới. Về các tháng, bạn nên chủ động trong công việc vào tháng 3, tháng 7 và tháng 12, còn tháng 4 và tháng 6 thì nên giữ nhịp. Lưu ý rằng bạn không nên đổi việc vì cảm xúc nhất thời, nhất là trong những tháng có Kình Dương và Đà La lưu.

## 38. Bạn bè đồng nghiệp năm sau ra sao? — lá số E

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 50/100

**Đạt** (45.778 giây, 294 chữ). Câu AI:

> Chào bạn, về câu hỏi của bạn về sự nghiệp và mối quan hệ đồng nghiệp năm 2027, tổng thể lá số cho thấy mức độ trung bình, có lên có xuống với điểm số 50/100. Điểm tựa chính của bạn nằm ở đại hạn, nơi cung Quan Lộc tại Thân có Liêm Trinh vượng, gặp Quan Phù, cho thấy một nền tảng 10 năm tương đối thuận lợi. Tuy nhiên, tiểu hạn năm 2027 lại cho thấy cung Quan Lộc tại Tỵ có Thiên Lương hãm, gặp Linh Tinh đắc, khiến bạn cần thận trọng hơn. Cụ thể trong năm 2027, Hóa Quyền lưu nhập cung Quan Lộc theo Thái Tuế, điều này dễ mang lại cho bạn cơ hội được trao quyền hoặc có tiếng nói lớn hơn trong tập thể. Tuy nhiên, Đà La lưu nhập cung Quan Lộc gốc, có thể khiến các việc làm chậm trễ hoặc vướng mắc thủ tục, đòi hỏi bạn phải kiên nhẫn. Đồng thời, Thiên Mã lưu cũng cho thấy công việc có thể liên quan đến sự di chuyển, đi công tác xa hoặc mở rộng thị trường mới. Về mặt tinh thần, Thiên Khốc lưu có thể khiến bạn cảm thấy công sức mình bỏ ra chưa được ghi nhận như mong muốn. Theo các tháng, bạn nên chủ động trong công việc vào tháng 3, tháng 4 và tháng 7. Tháng 4 là tháng mà các vấn đề công việc sẽ nổi lên rõ rệt nhất. Bạn nên nhận vai trò dẫn dắt nhưng hãy giữ cách nói mềm mỏng để tránh va chạm với đồng nghiệp. Lưu ý là bạn nên chọn môi trường làm việc ổn định và đi đường dài thay vì liên tục thay đổi hướng đi.

## 39. Cung Quan Lộc của tôi tốt hay xấu? — lá số E

App hiểu là: Sự nghiệp · năm 2026 (Bính Ngọ) · điểm 54/100

**Đạt** (64.085 giây, 470 chữ). Câu AI:

> Chào bạn, tôi sẽ luận giải về cung Quan Lộc của bạn trong năm 2026 (Bính Ngọ) dựa trên lá số của bạn nhé. Nhìn chung, sự nghiệp của bạn năm nay được đánh giá ở mức 54/100, tức là trung bình, có những lúc thăng lên nhưng cũng có lúc cần giữ vững. Điểm tựa chính của bạn là đại hạn đang thuận lợi, giúp sự nghiệp có nền tảng vững chắc hơn.
> 
> Ở lá số gốc, cung Quan Lộc của bạn an tại Tỵ, có sao Thiên Lương hãm, Văn Xương, Lộc Tồn và Linh Tinh. Thiên Lương hãm cho thấy bạn dễ có xu hướng lo lắng cho người khác, đôi khi cảm thấy cô độc trong công việc, nhưng bạn có khả năng được giúp đỡ khi gặp khó khăn. Sự hiện diện của Văn Xương và Lộc Tồn cho thấy bạn có học vấn và khả năng giữ gìn tài chính tốt. Tuy nhiên, vì Lộc Tồn bị Kình Đà kẹp nên bạn thường có xu hướng dè dặt trong việc kiếm tiền.
> 
> Nhìn vào đại hạn (34-43 tuổi), cung Quan Lộc rơi vào Thân, có Liêm Trinh vượng và gặp Quan Phù, cho thấy giai đoạn này bạn có sự nghiêm minh, thích hợp với các lĩnh vực liên quan đến kỷ cương, tư pháp hoặc nghệ thuật. Đại hạn này được đánh giá là thuận lợi với điểm 58/100.
> 
> Trong năm 2026, có nhiều yếu tố tích cực xuất hiện. Hóa Khoa lưu (tức là sự ghi nhận về uy tín) nhập thẳng cung Quan Lộc gốc, giúp bạn được chú ý về thành tích và thuận lợi trong thi cử hoặc bằng cấp. Đồng thời, Lộc Tồn lưu cũng nhập cung, báo hiệu thu nhập từ công việc sẽ ổn định hơn.
> 
> Tuy nhiên, bạn cũng cần lưu ý Đà La lưu nhập vào cung Tiểu hạn, điều này có thể khiến các thủ tục công việc dễ bị chậm trễ hoặc kéo dài. Về mặt cơ hội, Hóa Lộc lưu xung chiếu cho thấy bạn có thêm các dự án hoặc nguồn thu mới.
> 
> Về các tháng trong năm, bạn nên chủ động và tích cực trong công việc vào tháng 1, tháng 5 và tháng 7. Những tháng này có điểm số cao, rất tốt để bạn thể hiện bản thân. Ngược lại, bạn nên giữ nhịp độ ổn định vào tháng 6 và tháng 11.
> 
> Lời khuyên cho bạn là nên học thêm kiến thức hoặc lấy chứng chỉ để trình bày thành tích với cấp trên. Đồng thời, bạn nên chọn môi trường làm việc ổn định, đi đường dài thay vì thay đổi hướng đi liên tục. Không nên đưa ra những thay đổi lớn trong sự nghiệp khi chưa có phương án dự phòng rõ ràng.

## 40. nam 2027 co nen dau tu khong — lá số E

App hiểu là: Tài chính · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 59/100

**Đạt** (42.652 giây, 282 chữ). Câu AI:

> Chào bạn, về câu hỏi đầu tư trong năm 2027, nhìn chung tài chính của bạn có xu hướng thuận lợi với điểm số 59/100, nên bạn có thể cân nhắc các cơ hội nhưng cần hết sức thận trọng. Điểm tựa chính là đại hạn của bạn đang ở cung Tài Bạch với Vũ Khúc và Thiên Phủ, cho thấy khả năng giữ tiền và kiếm tiền vững vàng trong giai đoạn này. Tuy nhiên, khi xét đến năm 2027, cung Tài Bạch của bạn rơi vào trạng thái vô chính diệu, mượn Thiên Cơ và Cự Môn, điều này cho thấy bạn sẽ hoạt ngôn và giỏi trong kinh doanh nhưng cũng dễ trở nên đa nghi. Trong năm này, Hóa Khoa lưu (Thiên Cơ) nhập cung Tài Bạch theo Thái Tuế, giúp tài chính ổn định nhờ uy tín, bạn có thể dễ dàng được hỗ trợ hoặc vay mượn khi cần. Tuy nhiên, cũng có Hóa Kỵ lưu (Cự Môn) nhập cung Tài Bạch, cảnh báo tiền bạc dễ vướng mắc như khoản chi bất ngờ, nợ chậm thu hoặc hiểu lầm về tiền bạc. Ngoài ra, Tang Môn lưu cũng cho thấy có thể phát sinh chi phí liên quan đến sức khỏe hoặc gia đình. Về mặt tháng, bạn nên chủ động đầu tư hoặc giao dịch vào tháng 3 và tháng 7, nhưng cần giữ nhịp ở tháng 4 và tháng 10. Lời khuyên dành cho bạn là hãy giữ chữ tín và minh bạch mọi giấy tờ tiền bạc, đồng thời không nên cho vay lớn hay đứng ra bảo lãnh, và luôn ghi chép dòng tiền đều đặn để kiểm soát chi tiêu.

