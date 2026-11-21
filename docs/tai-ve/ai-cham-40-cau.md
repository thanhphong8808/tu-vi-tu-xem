# Chấm bộ 40 câu — Trợ lý AI trên máy (Gemma 4 E2B, LiteRT-LM)

Chạy trên máy build Linux, **CPU 4 nhân**, LiteRT-LM 0.19.1 (`litertlm-jvm`), model `gemma-4-E2B-it.litertlm`.
Cùng luồng với app: `AppApi.askAi` → lời nhắc từ QaAnalyst → model → kiểm tra mục 12.3 (`AiGuard`).
Câu nào trượt kiểm tra thì app hiện câu của QaAnalyst (không hiện câu AI).

| Chỉ số | Kết quả |
| --- | --- |
| Câu hỏi | 40 (bỏ qua không gọi model: 0) |
| Qua kiểm tra, hiện câu AI | **36/40** |
| Trượt kiểm tra → hiện câu QaAnalyst | 4 |
| Nạp model lần đầu | 0.984 giây |
| Thời gian một câu (trung vị / lâu nhất) | 19.645 / 28.711 giây |

Lý do trượt (đếm theo loại): chưa nêu điểm chung N/N × 2; gán × 2; điểm N/N không có trong dữ kiện × 1

## 1. Năm sau sự nghiệp của tôi thế nào? — lá số A

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 49/100

**Đạt** (19.645 giây, 185 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, sự nghiệp của bạn vào năm 2027 có mức điểm là 49/100, cho thấy sự nghiệp có thể có những lúc lên xuống. Đại hạn và tiểu hạn đều cho thấy cần phải thận trọng. Cụ thể, cung Quan Lộc của đại hạn và tiểu hạn đều có dấu hiệu mượn Thái Dương vượng – Thiên Lương vượng, điều này gợi ý rằng danh và tài có thể dễ rực rỡ, đặc biệt trong các lĩnh vực học thuật hoặc y tế. Lưu niên đại hạn cho thấy kết quả của năm sẽ là 53/100, tức là trung bình, có lên có xuống. Lưu Thái Tuế năm Đinh Mùi có điểm 52/100, cho thấy sự việc sẽ được đánh thức trong năm. Bạn nên đặt trọng tâm vào lĩnh vực của cung được kích hoạt và lên kế hoạch cụ thể cho nó. Tuy nhiên, bạn không nên thay đổi công việc vì cảm xúc nhất thời, nhất là khi có các sao như Kình Dương và Đà La lưu, vì điều này có thể gây ra những trở ngại.

## 2. Tình duyên năm 2027 ra sao? — lá số A

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 50/100

**Đạt** (20.745 giây, 201 chữ). Câu AI:

> Chào bạn. Về tình duyên năm 2027, theo dữ kiện, kết quả được đánh giá là 50/100, tức là trung bình, có lúc thuận lợi có lúc khó khăn. Nền gốc của bạn là 31/100, cần giữ sự cẩn trọng. Đại hạn của bạn trong giai đoạn này có điểm 58/100, cho thấy sự thuận lợi hơn, và tiểu hạn năm 2027 cũng có điểm 58/100, cho thấy diễn biến trong năm này có xu hướng thuận lợi hơn. Tuy nhiên, Lưu Thái Tuế năm Đinh Mùi có điểm 42/100, cho thấy có những sự việc được đánh thức trong năm này, nên bạn cần thận trọng. Tình duyên năm 2027 có thể phát triển tốt nếu bạn chủ động gặp gỡ và mở rộng quan hệ. Nếu có ý định cưới hỏi, bạn nên thuận để bàn chuyện nếu cả hai bên đã sẵn sàng. Khi cần làm các vấn đề liên quan đến hôn nhân, bạn nên đọc kỹ hợp đồng và lưu giữ chứng từ, hoặc hỏi người có chuyên môn pháp lý. Bạn không nên thay đổi công việc chỉ vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương, Đà La lưu.

## 3. Năm nay tài chính của tôi thế nào? — lá số A

App hiểu là: Tài chính · năm 2026 (Bính Ngọ, 37 tuổi âm) · điểm 41/100

**Đạt** (19.751 giây, 208 chữ). Câu AI:

> Chào bạn. Dựa vào dữ kiện, tài chính của bạn trong năm 2026 được đánh giá là 41/100, mức độ cần thận trọng. Điểm tựa của bạn là Lưu Thái Tuế, với cung Tài Bạch ở Dần có Vũ Khúc vượng và Thiên Tướng miếu, điều này cho thấy có nền tảng tốt. Tuy nhiên, bạn đang đối diện với áp lực từ tiểu hạn, cụ thể là cung Tài Bạch tại Tý có Tham Lang hãm, cho thấy tiền bạc dễ thăng trầm vì có nhiều ham muốn vật chất và tình cảm, bạn cần kỷ luật chi tiêu. Đại hạn của bạn liên quan đến Thiên Đồng hãm và Cự Môn hãm, nên cần học cách diễn đạt mềm mỏng khi giao tiếp về tài chính. Lưu niên đại hạn năm này có điểm số 49/100, cho thấy có sự biến động nhưng vẫn có cơ hội. Lưu ý rằng có dấu hiệu hao tài mới là một tín hiệu cần theo dõi. Nên là lập quỹ dự phòng và chia nhỏ khoản đầu tư. Không nên là mở rộng nóng hoặc vay mượn lớn để tăng quy mô. Bạn nên theo dõi thu chi hàng tháng để nắm bắt tình hình tài chính tốt hơn.

## 4. Đại vận này công việc ra sao? — lá số A

App hiểu là: Sự nghiệp · đại vận 35–44 tuổi · điểm 48/100

**Đạt** (17.24 giây, 178 chữ). Câu AI:

> Chào bạn. Về sự nghiệp trong đại vận 35 đến 44 tuổi, kết quả được đánh giá là 48/100, tức là trung bình, có lúc thuận lợi có lúc gặp trở ngại. Nền gốc của bạn là 59/100, cho thấy tiềm năng tốt. Đại hạn giai đoạn này là 44/100, bạn cần phải thận trọng hơn trong giai đoạn này. Trong 10 năm này, năm thuận lợi nhất là 2027 với điểm 49/100, còn năm cần giữ nhất là 2028 với điểm 39/100. Về mặt cung Quan Lộc, đại hạn rơi vào cung Dậu, có mượn Thái Dương vượng và Thiên Lương vượng, cho thấy danh và tài có thể rực rỡ, đặc biệt trong lĩnh vực học thuật hoặc y tế. Bạn nên chủ động tự lực, xây dựng nền tảng vững chắc từng bước thay vì chờ đợi hoàn cảnh. Đồng thời, bạn nên tận dụng cơ hội để mở rộng công việc, đi xa hoặc mở thị trường mới. Không nên bỏ qua những dấu hiệu bất thường kéo dài hoặc tự ý dùng thuốc.

## 5. Năm 2020 công việc của tôi thế nào? — lá số A

App hiểu là: Sự nghiệp · năm 2020 (Canh Tý, 31 tuổi âm) · điểm 38/100

**Đạt** (27.094 giây, 300 chữ). Câu AI:

> Chào bạn. Dựa vào dữ kiện, sự nghiệp của bạn trong năm 2020 được đánh giá là 38/100, cần thận trọng. Điểm chung cho thấy bạn cần thận trọng trong giai đoạn này.
> 
> Ở lá số gốc, cung Quan Lộc tại Ngọ có Tử Vi miếu, cho thấy bạn có khả năng tổ chức và lãnh đạo tốt, hợp với vai trò quản trị, điều phối. Tuy nhiên, có sự xuất hiện của Hỏa Tinh đắc và Địa Không hãm, cần lưu ý về những yếu tố bất ngờ.
> 
> Trong đại hạn 25–34 tuổi, cung Quan Lộc rơi vào Thân với Phá Quân hãm, điều này có thể khiến bạn dễ trở nên liều lĩnh hoặc ngang tàng. Bạn nên chọn môi trường ổn định, hợp đổi mới, khởi nghiệp, kỹ thuật hoặc quân sự.
> 
> Tiểu hạn năm 2020, cung Quan Lộc rơi vào Dần với Vũ Khúc vượng và Thiên Tướng miếu, cho thấy bạn có thực tế và trách nhiệm trong việc quản lý tài chính. Tuy nhiên, điểm số ở tiểu hạn là 54/100, có thể có những biến động.
> 
> Về Lưu Thái Tuế năm Canh Tý, cung Quan Lộc theo Thái Tuế rơi vào Thìn với Thất Sát hãm và Địa Kiếp hãm, cho thấy có những áp lực cần phải giữ vững.
> 
> Nên: Giai đoạn này nên thủ hơn công, củng cố nền tảng và hoãn những quyết định lớn nếu có thể. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là khi có các sao như Kình Dương, Đà La lưu.
> 
> Lời khuyên cuối cùng là công việc có thể chậm hơn kỳ vọng, bạn nên chia mục tiêu lớn thành từng bước và theo dõi tiến độ. Đức năng thắng số, lựa chọn của bạn sẽ quyết định kết quả.

## 6. Năm nào tôi dễ có tiền nhất? — lá số A

App hiểu là: Năm thuận cho tài lộc, dòng tiền cải thiện · dò 2026–2036

**Đạt** (13.626 giây, 151 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, có một số năm được xem là có khả năng tài lộc cải thiện. Năm 2034 được xem là có khả năng tài lộc, với điểm tài chính là 57/100, do có Thiên thời Đại hạn đồng hành và Hóa Lộc lưu vào Tài Bạch cùng Lưu Thái Tuế. Năm 2030 cũng có khả năng tài lộc, với điểm tài chính 43/100, nhờ Lưu niên đại hạn hội cát tinh và Tiểu hạn gặp Lộc. Còn năm 2031 cũng là dấu hiệu của tài lộc, với điểm tài chính 47/100, do Lưu niên đại hạn hội cát tinh và Tiểu hạn gặp Lộc, cùng với Hóa Lộc lưu xung chiếu cung Tài Bạch. Bạn nên xem xét các yếu tố này để có cái nhìn tổng quan. Không nên quá đặt nặng vào những dự đoán này, quyết định cuối cùng vẫn là của bạn.

## 7. Cung Mệnh của tôi có sao gì? — lá số A

App hiểu là: Dữ kiện lá số: cung Mệnh

**Đạt** (12.788 giây, 168 chữ). Câu AI:

> Chào bạn. Về cung Mệnh của bạn, cung này nằm ở Dần, với các chính tinh là Vũ Khúc Vượng và Thiên Tướng Miếu. Điều này cho thấy tính cách của bạn có sự thực tế, có trách nhiệm trong việc quản lý tài chính và có xu hướng hành động có kế hoạch. Sự kết hợp của Vũ Khúc và Thiên Tướng mang lại sự vững vàng và khả năng quản lý tốt. Tứ Hóa gốc tại Mệnh là Hóa Quyền, điều này bổ sung thêm khía cạnh về sự quyết đoán và khả năng hành động mạnh mẽ. Thân cư Phu Thê ở cung Tý. Bạn nên chú ý đến sự cân bằng giữa tính thực tế của Vũ Khúc và sự hỗ trợ của Thiên Tướng. Nên tập trung vào việc xây dựng kế hoạch rõ ràng cho các vấn đề tài chính và công việc. Không nên quá cứng nhắc trong quyết định mà hãy linh hoạt điều chỉnh theo tình hình thực tế.

## 8. Tháng 3 âm năm sau công việc thế nào? — lá số A

App hiểu là: Sự nghiệp · tháng 3 âm lịch năm 2027 · điểm 47/100

**Đạt** (16.603 giây, 190 chữ). Câu AI:

> Chào bạn, về tháng 3 âm lịch năm 2027, theo dữ kiện, đây là giai đoạn trung bình, có lúc lên có xuống về sự nghiệp, với điểm số là 47/100. Nền sự nghiệp của cả năm 2027 được đánh giá là 49/100. Nguyệt hạn trong tháng này tập trung vào cung Nô Bộc, có Thiên Đồng hãm và Cự Môn hãm từ Sửu, điều này cho thấy bạn bè hoặc cộng sự có thể có nét bất đồng trong lời nói, bạn nên học cách diễn đạt mềm mỏng hơn. Về Tứ Hóa tháng, có Hóa Lộc tại Tài Bạch và Hóa Khoa tại Mệnh, điều này có thể mang lại cơ hội về tài lộc và sự nghiệp. Tuy nhiên, bạn cần lưu ý rằng các tầng không đồng thuận, nên kết quả sẽ phụ thuộc nhiều vào cách bạn lựa chọn thời điểm. Nên giảm bớt việc và ưu tiên những công việc thực sự cần thiết trong giai đoạn này. Không nên thay đổi công việc chỉ vì cảm xúc nhất thời, đặc biệt khi có các sao như Kình Dương và Đà La lưu động.

## 9. nam sau suc khoe can luu y gi — lá số A

App hiểu là: Nên / Không nên · sức khỏe (lưu ý) · năm 2027 · điểm 49/100

**Trượt** (18.206 giây): chưa nêu điểm chung 49/100

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn. Về sức khỏe cho năm 2027, dựa trên dữ kiện, điểm số dự kiến là 43/100, nên bạn cần thận trọng. Điểm tựa của bạn là Lưu Thái Tuế ở cung Tật Ách tại Dần, có sao Vũ Khúc vượng và Thiên Tướng miếu. Lưu niên đại hạn tại cung Tật Ách ở Ngọ có Tử Vi miếu gặp Hỏa Tinh đắc, điều này cho thấy diễn biến đầu có thể vất vả nhưng kết quả cuối năm có xu hướng tốt hơn. Tuy nhiên, đại hạn và tiểu hạn tại cung Tật Ách cùng rơi vào Tý với Tham Lang hãm, cho thấy áp lực có thể xuất hiện. Bạn nên đọc kỹ hợp đồng, lưu giữ chứng từ và hỏi người có chuyên môn pháp lý khi cần. Nếu có kế hoạch sinh con, hãy chăm sóc thai kỳ kỹ và khám định kỳ theo hướng dẫn của bác sĩ. Bạn không nên bỏ qua các dấu hiệu bất thường kéo dài hoặc tự ý dùng thuốc. Ngoài ra, hãy chú ý đến giấc ngủ, ăn uống và vận động, và đi khám khi có dấu hiệu bất thường kéo dài. Bạn không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong tháng có Kình Dương, Đà La lưu, và không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy."}

## 10. Tôi có nên đổi việc năm 2027 không? — lá số A

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 38 tuổi âm) · điểm 49/100

**Đạt** (15.535 giây, 163 chữ). Câu AI:

> Chào bạn. Về việc đổi việc vào năm 2027, theo dữ kiện, sự nghiệp của bạn trong năm đó được đánh giá là trung bình, có lên có xuống với điểm 49/100. Điều này cho thấy có những biến động nhất định. Đại hạn và tiểu hạn đều cho thấy cần phải thận trọng. Lưu niên đại hạn năm đó có điểm 53/100, cho thấy kết quả của năm đó sẽ là trung bình, có lên có xuống. Tuy nhiên, dữ kiện chỉ ra rằng việc đổi việc là một dấu hiệu, và tiểu hạn cũng chỉ ra điều này. Cách vận hạn kích hoạt cho thấy thăng tiến mới là dấu hiệu, và đổi việc cũng là dấu hiệu. Bạn nên đặt trọng tâm vào lĩnh vực của cung được kích hoạt và lên kế hoạch cụ thể cho nó. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương và Đà La lưu.

## 11. Năm 2025 tiền bạc của tôi thế nào? — lá số A

App hiểu là: Tài chính · năm 2025 (Ất Tỵ, 36 tuổi âm) · điểm 45/100

**Đạt** (20.944 giây, 203 chữ). Câu AI:

> Chào bạn. Về tình hình tài chính của bạn trong năm 2025, theo dữ kiện, điểm chung cho năm này là 45/100, bạn nên cần thận trọng. Chúng ta xem xét từng tầng nhé. Ở lá số gốc, cung Tài Bạch tại Tuất có Liêm Trinh miếu, Thiên Phủ vượng, cho thấy nguyên tắc là biết giữ của và ổn định, nhưng có Linh Tinh hãm. Đại hạn giai đoạn này có điểm số 33/100, cho thấy có khó khăn, nên bạn cần giữ vững. Lưu niên đại hạn năm 2025 có điểm 50/100, cho thấy có lên có xuống. Tiểu hạn năm 2025 có điểm 63/100, đây là một điểm thuận lợi, cho thấy có duyên tích lũy tài sản, nhất là tài sản. Lưu Thái Tuế năm Ất Tỵ có điểm 33/100, cho thấy việc này sẽ bị đánh thức trong năm. Tổng hợp lại, kết quả phụ thuộc nhiều vào cách bạn lựa chọn thời điểm. Nên giữ kỷ luật chi tiêu, ghi chép dòng tiền đều đặn. Không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy. Đức năng thắng số: Tử Vi là bản đồ thời tiết, lựa chọn của bạn quyết định kết quả.

## 12. Tôi có số giàu không? — lá số A

App hiểu là: Tài chính · trọn đời · điểm 54/100

**Trượt** (17.316 giây): điểm 38/100 không có trong dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn. Về vấn đề tài chính trọn đời, dựa vào lá số gốc, điểm tài chính của bạn là 54/100, tức là ở mức trung bình, có lúc lên lúc xuống. Mệnh của bạn tại Dần, Vũ Khúc vượng và Thiên Tướng miếu, cho thấy bạn có khả năng quản lý tài chính và có trách nhiệm với việc của mình. Cung Tài Bạch an tại Tuất, có Liêm Trinh miếu và Thiên Phủ vượng, điều này gợi ý về sự ổn định và khả năng giữ gìn của cải. Tuy nhiên, có một số sao như Linh Tinh hãm và Thất Sát hãm ở các vị trí xung chiếu cần lưu ý. Giai đoạn từ 25 đến 34 tuổi, điểm tài chính có xu hướng thấp hơn một chút là 30/100, bạn nên chú ý trong giai đoạn này. Hiện tại bạn đang ở giai đoạn 35-44 tuổi với điểm 38/100. Để cải thiện, bạn nên giữ kỷ luật chi tiêu, ghi chép dòng tiền đều đặn. Không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy."}

## 13. Năm sau tình duyên của tôi thế nào? — lá số B

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 43 tuổi âm) · điểm 52/100

**Đạt** (16.026 giây, 159 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, tình duyên của bạn vào năm 2027 được đánh giá là trung bình, có lên có xuống với điểm số 52/100. Điều này cho thấy kết quả sẽ phụ thuộc nhiều vào cách bạn lựa chọn thời điểm. Lưu niên đại hạn cho thấy kết quả có xu hướng thuận lợi hơn với điểm 62/100. Tiểu hạn năm 2027 lại có điểm 61/100, cho thấy diễn biến trong năm sẽ có xu hướng thuận. Tuy nhiên, lá số gốc có nền tảng cần thận trọng hơn. Bạn nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Khi áp lực tinh thần kéo dài, bạn nên chủ động tìm kiếm sự hỗ trợ từ chuyên gia tâm lý hoặc bác sĩ để được giúp đỡ. Không nên mở rộng nóng hoặc vay mượn lớn để tăng quy mô trong giai đoạn này.

## 14. Năm 2027 có cưới được không? — lá số B

App hiểu là: Tình duyên, hôn nhân · năm 2027 (Đinh Mùi, 43 tuổi âm) · điểm 52/100

**Đạt** (25.076 giây, 266 chữ). Câu AI:

> Chào bạn. Về việc cưới hỏi năm 2027, theo dữ kiện, tình duyên, hôn nhân năm 2027 được đánh giá là 52/100, tức là mức trung bình, có lúc thuận lợi có lúc gặp trở ngại. Chúng ta xem xét các yếu tố khác nhé.
> 
> Ở lá số gốc, cung Phu Thê tại Sửu có Thiên Đồng hãm và Cự Môn hãm, điều này cho thấy người bạn đời có thể có những bất đồng trong lời nói, bạn nên học cách diễn đạt thật mềm mỏng.
> 
> Đại hạn 35-44 tuổi có cung Phu Thê rơi vào Thìn với Thất Sát hãm, cho thấy người bạn đời có thể có tính nóng nảy hoặc có khoảng cách, cần sự thấu hiểu.
> 
> Tuy nhiên, lưu niên đại hạn năm 2027 lại cho kết quả thuận lợi hơn với điểm 62/100, cho thấy có xu hướng tốt hơn trong giai đoạn này.
> 
> Tiểu hạn năm 2027 lại có điểm 61/100, cho thấy diễn biến trong năm có xu hướng thuận theo hướng dịu dàng, tinh tế.
> 
> Lưu Thái Tuế năm Đinh Mùi cũng cho điểm 47/100, cho thấy sự việc này sẽ được đánh thức trong năm.
> 
> Tổng thể, kết quả phụ thuộc nhiều vào cách bạn chọn thời điểm. Bạn nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Khi áp lực tinh thần kéo dài, bạn nên chủ động tìm chuyên gia tâm lý hoặc bác sĩ để được hỗ trợ. Không nên mở rộng nóng hoặc vay mượn lớn để tăng quy mô.

## 15. Gia đạo năm nay ra sao? — lá số B

App hiểu là: Gia đạo · năm 2026 (Bính Ngọ, 42 tuổi âm) · điểm 52/100

**Đạt** (25.084 giây, 277 chữ). Câu AI:

> Chào bạn. Về gia đạo năm nay, dựa trên dữ kiện, kết quả tổng thể là 52/100, tức là trung bình, có lúc lên có lúc xuống. Điểm tựa của bạn là Lưu Thái Tuế với mức 56/100, cho thấy có sự được đánh thức trong năm này.
> Ở tầng lá số gốc, cung Phụ Mẫu tại Thìn có Thất Sát hãm, điều này có thể cho thấy cha mẹ có thể có những lúc nóng tính hoặc có khoảng cách, nên bạn cần thêm sự thấu hiểu với họ.
> Đại hạn 35-44 tuổi, đại hạn tại Ngọ, cung Phụ Mẫu rơi vào Mùi, có Thiên Đồng hãm và Cự Môn hãm, cho thấy cha mẹ có thể có những bất đồng trong lời nói, bạn nên học cách diễn đạt mềm mỏng hơn.
> Tiểu hạn năm 2026 tại Dần, cung Phụ Mẫu rơi vào Mão, có Thái Dương vượng và Thiên Lương vượng, điều này cho thấy cha mẹ có thể mang nét danh tiếng hoặc tài năng dễ nổi bật, hợp với các lĩnh vực học thuật hoặc y tế.
> Về Lưu Thái Tuế, việc bị đánh thức trong năm này là một điểm đáng chú ý.
> Về mặt vận hạn, có một sự kích hoạt liên quan đến việc sinh con, đây là một điểm tích cực trong tiểu hạn.
> Nên bạn nên chăm sóc thai kỳ kỹ lưỡng, khám định kỳ theo lời khuyên của bác sĩ. Bạn cũng nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Không nên mở rộng nóng hoặc vay mượn lớn để tăng quy mô.

## 16. Đại vận tới tài chính thế nào? — lá số B

App hiểu là: Tài chính · đại vận 45–54 tuổi · điểm 58/100

**Đạt** (22.772 giây, 257 chữ). Câu AI:

> Chào bạn. Về vấn đề tài chính trong đại vận 45 đến 54 tuổi, theo dữ kiện, điểm chung cho lĩnh vực này là 58/100, thuộc thế Giữ, cho thấy nhìn chung là thuận lợi. Chúng ta xem xét từng tầng nhé. Ở lá số gốc, cung Tài Bạch an tại Hợi, có Thái Âm miếu, cho thấy có duyên tích lũy tài sản, đặc biệt là về tài sản. Có Thiên Mã là cát tinh, nhưng cũng có Địa Không và Địa Kiếp tại cung Tài Bạch, điều này ám chỉ việc kiếm tiền có thể táo bạo nhưng cũng dễ gặp phải sự hụt hẫng. Xung chiếu từ Phúc Đức có Thiên Cơ vượng, và Mệnh, Quan Lộc tam hợp, tạo nền tảng tốt. Nền gốc được đánh giá là 59/100, thuận. Đại hạn 45-54 tuổi, giai đoạn 2029-2038, cung Tài Bạch của đại hạn rơi vào cung Mão (cung Mệnh gốc), có Thái Dương vượng và Thiên Lương vượng, cho thấy danh và tài có thể rực rỡ, đặc biệt hợp với học thuật hoặc y tế. Đại hạn được đánh giá là 58/100, thuận, và đây là nền tảng cho 10 năm tới. Tổng hợp lại, điểm chung là 58/100, thuận. Để tận dụng điều này, bạn nên lập quỹ dự phòng và chia nhỏ khoản đầu tư. Bạn cũng nên chủ động tự lực, xây nền tảng vững từng bước thay vì trông chờ hoàn cảnh. Không nên lên đường vội vàng khi chưa chuẩn bị giấy tờ, lịch trình, bảo hiểm.

## 17. Năm 2023 sự nghiệp của tôi ra sao? — lá số B

App hiểu là: Sự nghiệp · năm 2023 (Quý Mão, 39 tuổi âm) · điểm 46/100

**Đạt** (22.113 giây, 232 chữ). Câu AI:

> Chào bạn. Về sự nghiệp của bạn trong năm 2023, theo dữ kiện, sự nghiệp của bạn được đánh giá là trung bình, có lên có xuống, với điểm số là 46/100. Điều này cho thấy năm đó có những biến động nhất định.
> 
> Ở lá số gốc, cung Quan Lộc tại Mùi có vô chính di hiệu, mượn Thiên Đồng hãm và Cự Môn hãm từ Sửu, điều này gợi ý rằng có thể có sự bất đồng trong lời nói, bạn nên học cách diễn đạt mềm mỏng hơn.
> 
> Về lưu niên đại hạn, cung Quan Lộc của năm 2023 rơi vào Tỵ với Thiên Cơ vượng và gặp Quan Phù, cho thấy có cơ hội để phát triển.
> 
> Tiểu hạn năm 2023 tại Tỵ, cung Quan Lộc rơi vào Dậu, có Thái Dương vượng và Thiên Lương vượng từ Mão, điều này cho thấy danh và tài có thể dễ rực rỡ, đặc biệt trong các lĩnh vực học thuật hoặc y tế.
> 
> Tuy nhiên, Lưu Thái Tuế năm Quý Mão có điểm số 40/100, cần thận trọng vì có sự đánh thức trong năm.
> 
> Lời khuyên cho bạn là nên giảm bớt việc và ưu tiên những gì thật sự cần trong giai đoạn này. Bạn không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương và Đà La lưu.

## 18. Năm nào thuận để mua nhà? — lá số B

App hiểu là: Năm thuận cho nhà đất · dò 2026–2036

**Đạt** (19.662 giây, 210 chữ). Câu AI:

> Chào bạn. Về việc mua nhà trong giai đoạn 2026 đến 2036, chúng ta sẽ xem xét các năm có điểm số tốt hơn. Năm 2028 có điểm nhà đất là 60/100, đây là mức khá thuận lợi, do có Hóa Lộc lưu xung chiếu cung Điền Trạch và Hóa Quyền lưu nhập cung Điền Trạch năm. Tuy nhiên, bạn cần lưu ý đến Hóa Kỵ lưu xung chiếu cung Điền Trạch và Kình Dương lưu tại cung Điền Trạch. Năm 2032 cũng có điểm nhà đất là 56/100, với Hóa Lộc lưu nhập cung Điền Trạch năm và Hóa Quyền lưu nhập cung Điền Trạch gốc. Năm 2033 có điểm nhà đất là 54/100, nhưng cần xem xét đại hạn 58/100 tại cung Điền Trạch, nơi có Liêm Trinh miếu – Thiên Phủ vượng gặp Linh Tinh hãm, cùng với Hóa Kỵ lưu xung chiếu cung Điền Trạch. Tóm lại, năm 2028 và 2032 có vẻ là những thời điểm có cơ hội tốt hơn cho việc xem xét mua nhà. Nên tập trung vào việc xem xét kỹ lưỡng các yếu tố xung chiếu của các sao tại cung Điền Trạch. Không nên vội vàng quyết định mà hãy cân nhắc kỹ lưỡng các yếu tố cần lưu ý.

## 19. Con cái năm 2028 thế nào? — lá số B

App hiểu là: Gia đạo · năm 2028 (Mậu Thân, 44 tuổi âm) · điểm 53/100

**Đạt** (19.416 giây, 195 chữ). Câu AI:

> Chào bạn. Về con cái năm 2028, theo dữ kiện, tổng thể kết quả được đánh giá là 53/100, tức là trung bình, có lúc thuận lợi có lúc gặp trở ngại. Điểm tựa của bạn là tiểu hạn, có điểm 57/100, cho thấy diễn biến trong năm này có xu hướng thuận lợi hơn một chút. Lưu Thái Tuế năm Mậu Thân có điểm 56/100, cho thấy có sự thức tỉnh hoặc một sự kiện đáng chú ý trong năm. Về mặt vận hạn, có sự kích hoạt thông qua tiểu hạn vào cung Tử Tức gặp sao hỷ, và có Lộc lưu vào Tử Tức trong Lưu Thái Tuế, điều này cho thấy có cơ hội tốt liên quan đến việc sinh con. Tuy nhiên, gốc lá số gốc có điểm 45/100, cho thấy nền tảng cần được củng cố. Bạn nên chăm sóc thai kỳ kỹ lưỡng, khám định kỳ theo hướng dẫn của bác sĩ. Bạn cũng nên giữ kết nối thường xuyên với người thân và bạn bè tin cậy để chia sẻ khi có áp lực. Không nên mở rộng nóng hoặc vay mượn lớn để tăng quy mô.

## 20. Cung Phu Thê của tôi có sao gì? — lá số B

App hiểu là: Dữ kiện lá số: cung Phu Thê

**Đạt** (15.991 giây, 202 chữ). Câu AI:

> Chào bạn. Về cung Phu Thê của bạn, cung này tọa tại Sửu, có Thiên Đồng và Cự Môn là chính tinh, cả hai đều đang ở trạng thái hãm. Thiên Đồng hãm cho thấy mối quan hệ có thể có sự chậm rãi, hoặc cảm xúc không được thể hiện trọn vẹn. Cự Môn hãm lại gợi ý về sự bất đồng trong lời nói, như dữ kiện đã đề cập, người bạn đời có thể có những khác biệt trong cách bày tỏ ý kiến. Phụ tinh như Hoa Cái, Phá Toái, Phục Binh, Thái Tuế và Suy cần được lưu ý. Phá Toái báo hiệu khả năng hao hụt hoặc đổ vỡ nhỏ trong các mối quan hệ. Phục Binh cho thấy cần đề phòng những sự ganh ghét hoặc tiểu nhân ngấm ngầm. Thái Tuế và Suy cần được xem xét trong các giai đoạn cụ thể. Về năm 2026, đây là dấu hiệu để bạn đối chiếu với những gì đã xảy ra trong năm đó. Nên học cách diễn đạt lời nói thật mềm mỏng và kiên nhẫn trong giao tiếp. Không nên quá nóng vội trong việc tranh luận để tránh những đổ vỡ nhỏ.

## 21. tai chinh nam 2028 the nao — lá số B

App hiểu là: Tài chính · năm 2028 (Mậu Thân, 44 tuổi âm) · điểm 49/100

**Trượt** (27.123 giây): gán “Địa Không” với “Hợi” không khớp dữ kiện; gán “Địa Kiếp” với “Hợi” không khớp dữ kiện

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn, về vấn đề tài chính cho năm 2028, theo dữ kiện, kết quả tổng thể được đánh giá là 49/100, tức là trung bình, có lúc lên lúc xuống. Điều này là do có nhiều yếu tố vận hạn khác nhau tác động.\n\nỞ tầng lá số gốc, cung Tài Bạch tại Hợi có Thái Âm miếu, cho thấy có duyên tích lũy tài sản, nhưng cũng có Địa Không và Địa Kiếp, ám chỉ việc kiếm tiền có thể táo bạo nhưng dễ gặp trở ngại.\n\nĐại hạn 35-44 tuổi, cung Tài Bạch rơi vào Dần với Vũ Khúc vượng, Thiên Tướng miếu, cho thấy bạn có trách nhiệm và khả năng quản lý tài chính tốt trong giai đoạn này, tuy nhiên có Đà La hãm và Kiếp Sát.\n\nLưu niên đại hạn năm 2028, cung Tài Bạch cũng rơi vào Dần với Vũ Khúc vượng, Thiên Tướng miếu, cho thấy kết quả của năm này có xu hướng trung bình, có lên có xuống.\n\nTiểu hạn năm 2028 tại Thân, cung Tài Bạch có Phá Quân hãm, cho thấy tiền bạc có thể dễ thăng trầm do sự liều lĩnh hoặc tiêu tiền phóng tay, vì vậy bạn cần chú ý đến việc kỷ luật chi tiêu.\n\nVề Lưu Thái Tuế năm Mậu Thân, cung Tài Bạch rơi vào Thìn với Thất Sát hãm, có Kình Dương đắc, cho thấy cần phải thận trọng trong các quyết định tài chính trong năm đó.\n\nTóm lại, tài chính năm 2028 sẽ có sự dao động. Bạn nên giữ kỷ luật chi tiêu, ghi chép dòng tiền đều đặn. Không nên mở rộng nóng hoặc vay mượn lớn để tăng quy mô."}

## 22. Tôi nên lưu ý gì năm 2027? — lá số B

App hiểu là: Nên / Không nên · năm 2027 · điểm 47/100

**Đạt** (18.908 giây, 237 chữ). Câu AI:

> Chào bạn. Về năm 2027, vận chung của bạn được đánh giá là 47/100, mức trung bình, có lúc lên lúc xuống. Áp lực chính sẽ đến từ đại hạn ở cung Mệnh tại Ngọ với sao Tử Vi miếu, điều này cho thấy sự cần thiết phải chú ý đến việc quản lý áp lực tinh thần. Tiểu hạn ở cung Mệnh tại Sửu có Thiên Đồng hãm và Cự Môn hãm, bạn cần lưu ý đến sự cân bằng trong các mối quan hệ và sự tự chủ của bản thân. Để đối phó với giai đoạn này, bạn nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Khi áp lực tinh thần kéo dài, bạn nên chủ động tìm kiếm sự hỗ trợ từ chuyên gia tâm lý hoặc bác sĩ. Bạn cũng nên giảm bớt việc và ưu tiên những điều thực sự cần thiết. Tuy nhiên, bạn không nên mở rộng nóng, vay mượn lớn để tăng quy mô, không nên bỏ qua các dấu hiệu bất thường kéo dài hoặc tự ý dùng thuốc, và không nên lên đường vội vàng khi chưa chuẩn bị đầy đủ giấy tờ, lịch trình, bảo hiểm. Hãy chú ý đến giấc ngủ, ăn uống và vận động, đồng thời đi khám khi có bất kỳ dấu hiệu bất thường nào kéo dài.

## 23. Tháng 6 năm nay sức khỏe thế nào? — lá số B

App hiểu là: Sức khỏe (lưu ý) · tháng 6 âm lịch năm 2026 · điểm 48/100

**Đạt** (18.428 giây, 214 chữ). Câu AI:

> Chào bạn. Về sức khỏe trong tháng 6 âm lịch năm 2026, kết quả được đánh giá là 48/100, tức là trung bình, có lúc tốt lúc không tốt. Nền sức khỏe chung của cả năm là 46/100. Chúng ta xem xét các yếu tố cụ thể. Nguyệt hạn tháng này liên quan đến cung Điền Trạch, có sao Tử Vi miếu, cho thấy bạn có thể có những điều tốt đẹp về đất đai, tài sản cố định, và có khả năng tổ chức, lãnh đạo tốt. Cát tinh Hóa Khoa cùng Tử Vi tại Điền Trạch cho thấy việc liên quan đến tài sản cố định sẽ được chú ý nhiều hơn trong tháng này. Về Tứ Hóa tháng, có Hóa Lộc và Hóa Quyền, nhưng cũng có Hóa Kỵ tại Tài Bạch, cần lưu ý về các giao dịch tài chính. Vì các tầng không đồng thuận, kết quả sẽ phụ thuộc vào cách bạn hành động. Nên giữ nguyên hiện trạng tài sản lớn và chưa nên vội mua bán nếu dòng tiền chưa vững. Ưu tiên an toàn là giữ dòng tiền và hoàn tất việc dang dở trước khi mở việc mới. Không nên dùng nhà đất làm tài sản bảo đảm cho khoản vay nhiều rủi ro.

## 24. Tính cách của tôi thế nào? — lá số B

App hiểu là: Tính cách · lá số gốc (trọn đời) · điểm 75/100

**Đạt** (14.32 giây, 173 chữ). Câu AI:

> Chào bạn. Tính cách của bạn theo lá số gốc được đánh giá là rất thuận với điểm số 75/100. Mệnh của bạn tọa tại Mão, có Thái Dương vượng và Thiên Lương vượng, điều này cho thấy bạn có xu hướng nổi bật về danh tiếng và tài lộc, rất hợp với các lĩnh vực học thuật hoặc y tế. Các cát tinh như Hóa Quyền và Lộc Tồn cũng góp phần tạo nên sự thuận lợi này. Tuy nhiên, trong giai đoạn 35 đến 44 tuổi, điểm số có phần thấp hơn là 46/100, bạn cần chú ý đến việc giữ vững sự ổn định. Về mặt tính cách, bạn nên tập trung vào việc phát triển bản thân thông qua học tập, lấy bằng cấp và chứng chỉ chuyên môn là điều nên làm. Bạn cũng nên tận dụng cơ hội để mở rộng thị trường mới hoặc công tác xa. Điều không nên là cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy trong giai đoạn này.

## 25. Năm sau sức khỏe của tôi cần lưu ý gì? — lá số D

App hiểu là: Nên / Không nên · sức khỏe (lưu ý) · năm 2027 · điểm 40/100

**Trượt** (16.405 giây): chưa nêu điểm chung 40/100

Đầu ra thô của model (app không hiện):

> {"answer": "Chào bạn. Về sức khỏe của bạn vào năm 2027, dựa trên dữ kiện, sức khỏe được đánh giá là 38/100, cần thận trọng. Điều này là do có áp lực từ đại hạn và lưu niên đại hạn tại cung Tật Ách, nơi có Thiên Lương miếu gặp Địa Kiếp hãm. Ngoài ra, tiểu hạn tại cung Tật Ách ở Thìn có Cự Môn hãm gặp Hỏa Tinh đắc. Để đối phó với áp lực tinh thần kéo dài, bạn nên chủ động tìm kiếm sự hỗ trợ từ chuyên gia tâm lý hoặc bác sĩ. Bạn cũng nên giữ kết nối thường xuyên với người thân, bạn bè để chia sẻ khi cảm thấy áp lực. Hãy giảm bớt công việc và ưu tiên những việc thực sự cần thiết. Tuy nhiên, bạn không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương, Đà La lưu. Đồng thời, không nên lên đường vội vàng mà chưa chuẩn bị đầy đủ giấy tờ, lịch trình và bảo hiểm. Cuối cùng, bạn nên chia nhỏ các mục tiêu lớn thành từng bước để theo dõi tiến độ công việc."}

## 26. Hậu vận của tôi thế nào? — lá số D

App hiểu là: Tổng quan · giai đoạn 55–99 tuổi · điểm 36/100

**Đạt** (20.538 giây, 204 chữ). Câu AI:

> Chào bạn. Về hậu vận của bạn, chúng ta sẽ xem xét các giai đoạn từ 55 đến 99 tuổi dựa trên dữ kiện này. Điểm chung cho tổng quan là 36/100, bạn cần thận trọng. Khi xem xét chi tiết từng giai đoạn, chúng ta có các thông tin sau. Trong giai đoạn 46 đến 55 tuổi, đại hạn tại Quan Lộc là Thiên Đồng hãm với điểm 33/100. Đây là giai đoạn cần giữ sự thận trọng. Sau đó, từ 56 đến 65 tuổi, đại hạn tại Nô Bộc là Vũ Khúc hãm và Phá Quân hãm với điểm 38/100. Giai đoạn 66 đến 75 tuổi, đại hạn tại Thiên Di là Thái Dương hãm với điểm 43/100. Cuối cùng, từ 76 đến 85 tuổi, đại hạn tại Tật Ách là Thiên Phủ bình hòa với điểm 38/100. Về tổng thể, bạn nên cân nhắc các hướng hành chính, giáo dục, y tế, hoặc nghiên cứu. Bạn nên chủ động tự lực, xây nền tảng vững từng bước thay vì trông chờ hoàn cảnh. Không nên cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy. Tử Vi là bản đồ thời tiết, lựa chọn của bạn quyết định kết quả.

## 27. Năm 2027 tài chính ra sao? — lá số D

App hiểu là: Tài chính · năm 2027 (Đinh Mùi, 65 tuổi âm) · điểm 40/100

**Đạt** (26.279 giây, 262 chữ). Câu AI:

> Chào bạn, về tài chính trong năm 2027, theo dữ kiện, điểm chung cho tài chính là 40/100, bạn cần thận trọng. Chúng ta xem xét các tầng khác nhé. Ở lá số gốc, cung Tài Bạch tại Dần có Thiên Cơ hãm và Thái Âm hãm, cho thấy bạn có thể khéo léo, tinh tế nhưng cũng dễ thay đổi môi trường. Cát tinh Hóa Khoa giúp điều tiết. Đại hạn giai đoạn 2018–2027, cung Tài Bạch của đại hạn rơi vào Mùi với Liêm Trinh đắc và Thất Sát đắc, cho thấy giai đoạn này có sự vận động mạnh mẽ nhưng cũng cần cẩn trọng. Lưu niên đại hạn năm 2027 cũng có cung Tài Bạch rơi vào Mùi với Liêm Trinh đắc và Thất Sát đắc, mức độ thận trọng là 44/100. Tiểu hạn năm 2027 tại Tỵ, cung Tài Bạch rơi vào Tỵ với Thiên Tướng đắc, cho thấy việc trọng nghĩa hơn tiền bạc trong năm này, mức độ là 33/100. Lưu Thái Tuế năm Đinh Mùi có cung Tài Bạch rơi vào Mão với Tử Vi bình hòa – Tham Lang hãm gặp Hóa Kỵ, mức độ là 41/100, cần chú ý đến những vấn đề phát sinh. Tổng hợp lại, điểm 40/100 cho thấy bạn nên giữ sự thận trọng. Nên là tích lũy năng lực và vốn đủ lâu trước khi mở rộng. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt khi có Kình Dương, Đà La lưu. Đức năng thắng số, lựa chọn của bạn sẽ quyết định kết quả.

## 28. Con cái của tôi thế nào? — lá số D

App hiểu là: Gia đạo · năm 2026 (Bính Ngọ) · điểm 41/100

**Đạt** (23.204 giây, 261 chữ). Câu AI:

> Chào bạn. Về vấn đề con cái, chúng ta sẽ xem xét các yếu tố liên quan đến cung Phụ Mẫu trong lá số của bạn. Lá số gốc cho thấy cung Phụ Mẫu tại Mùi có Liêm Trinh đắc và Thất Sát đắc, điều này cho thấy cha mẹ bạn có nét mạnh mẽ nhưng cũng cần cẩn trọng trong các hoạt động vận động. Nền gốc của gia đạo được đánh giá là 41/100, bạn nên giữ sự thận trọng. Trong đại hạn, cung Phụ Mẫu rơi vào Tý với Thái Dương hãm, điều này có thể cho thấy cha mẹ có thể có những lúc nóng tính hoặc lo lắng về người khác, nên cần thêm sự thấu hiểu. Tiểu hạn năm 2026, cung Phụ Mẫu lại rơi vào Hợi với Vũ Khúc hãm và Phá Quân hãm, cho thấy có thể có những biến động về tài chính hoặc sự liều lĩnh trong giai đoạn này. Lưu Thái Tuế năm Bính Ngọ cho thấy có sự đánh thức trong năm, với Hóa Kỵ lưu tại cung Phụ Mẫu, bạn cần chú ý đến các mối quan hệ gia đình. Nhìn chung, các tầng đều có mức độ cần thận trọng. Nên giữ kết nối thường xuyên với người thân và bạn bè tin cậy, chia sẻ khi cảm thấy áp lực. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương, Đà La lưu. Đức năng thắng số, bạn cần lựa chọn của mình để quyết định kết quả.

## 29. Năm 2024 gia đạo thế nào? — lá số D

App hiểu là: Gia đạo · năm 2024 (Giáp Thìn, 62 tuổi âm) · điểm 40/100

**Đạt** (22.319 giây, 238 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, gia đạo của bạn trong năm 2024 có mức điểm là 40/100, nên cần thận trọng. Điều này xuất phát từ nhiều yếu tố khác nhau. Ở lá số gốc, cung Phụ Mẫu tại Mùi có Liêm Trinh đắc và Thất Sát đắc, cho thấy cha mẹ bạn có nét mạnh mẽ nhưng cần cẩn trọng khi vận động. Đại hạn giai đoạn 2018–2027, cung Phụ Mẫu rơi vào Tý với Thái Dương hãm, điều này cho thấy cha mẹ có thể nóng tính hoặc lo lắng về người khác, nên cần thêm sự thấu hiểu. Tiểu hạn năm 2024 tại Tý với Thái Dương hãm, cho thấy diễn biến trong năm có thể liên quan đến những vấn đề này. Lưu niên đại hạn năm 2024 tại Dậu với vô chính diệu và Tham Lang hãm, cũng báo hiệu sự cần thiết phải giữ sự bình tĩnh. Lưu Thái Tuế năm Giáp Thìn có Thái Tuế tại Thìn, cho thấy việc bị đánh thức trong năm. Tổng thể, các tầng đều có yếu tố cần giữ sự an toàn. Nên dành thời gian thăm hỏi, quan tâm sức khỏe cha mẹ. Nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi thấy áp lực. Không nên lên đường vội vàng khi chưa chuẩn bị kỹ lưỡng về giấy tờ, lịch trình và bảo hiểm.

## 30. Năm nào tôi nên giữ gìn sức khỏe? — lá số D

App hiểu là: Năm thuận cho sức khỏe · dò 2026–2036

**Đạt** (16.979 giây, 189 chữ). Câu AI:

> Chào bạn. Để xem năm nào bạn nên giữ gìn sức khỏe, chúng ta sẽ xem xét các thông tin về điểm sức khỏe trong các năm bạn đề cập. Năm 2028, với tuổi 66, điểm sức khỏe của bạn là 44/100 và điểm cả năm là 41/100, căn cứ là Hóa Lộc lưu nhập cung Tật Ách năm đó. Năm 2031, bạn cũng có điểm sức khỏe 44/100 và điểm cả năm 40/100, với Hóa Quyền lưu xung chiếu cung Tật Ách, và cần lưu ý Tang Môn lưu tại cung Tật Ách. Năm 2030, điểm sức khỏe là 43/100 và điểm cả năm là 40/100, với Hóa Quyền lưu xung chiếu cung Tật Ách. Các mức điểm này chỉ là khả năng theo số tầng vận cùng, không phải là lời khẳng định. Bạn nên chú ý đến các năm có điểm số thấp hơn để chủ động chăm sóc sức khỏe hơn. Nên tập trung vào việc duy trì lối sống lành mạnh trong các năm này. Không nên quá lo lắng hay đặt nặng những con số này mà hãy giữ tinh thần thoải mái.

## 31. Đại vận này gia đạo thế nào? — lá số D

App hiểu là: Gia đạo · đại vận 56–65 tuổi · điểm 37/100

**Đạt** (17.646 giây, 194 chữ). Câu AI:

> Chào bạn. Về gia đạo trong đại vận 56 đến 65 tuổi, kết quả là 37/100, bạn cần thận trọng. Điều này xuất phát từ cả lá số gốc và đại hạn. Ở lá số gốc, cung Phụ Mẫu tại Mùi có Liêm Trinh đắc và Thất Sát đắc, cho thấy cha mẹ có nét mạnh mẽ nhưng cần cẩn trọng khi vận động. Đại hạn 56-65 tuổi, giai đoạn 2018-2027, tập trung vào cung Phụ Mẫu rơi vào Tý, Thái Dương hãm, điều này cho thấy cha mẹ có thể nóng tính hoặc lo lắng về người khác, quan hệ cần thêm sự thấu hiểu. Tổng hợp lại, có tầng yếu ở gốc và đại hạn, nên bạn cần phòng ngừa ở khía cạnh này. Để tốt hơn, bạn nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Khi áp lực tinh thần kéo dài, bạn nên chủ động tìm chuyên gia tâm lý hoặc bác sĩ để được hỗ trợ. Không nên quyết định các việc lớn theo cảm tính hoặc bỏ qua ý kiến của những người cùng làm.

## 32. Nhà đất năm sau có thuận không? — lá số D

App hiểu là: Nhà đất · năm 2027 (Đinh Mùi, 65 tuổi âm) · điểm 36/100

**Đạt** (28.711 giây, 312 chữ). Câu AI:

> Chào bạn. Về vấn đề nhà đất năm sau, dựa trên dữ kiện, kết quả tổng thể cho thấy mức độ cần thận trọng là 36/100. Điều này xuất phát từ việc đại hạn và lưu niên đại hạn đều cho thấy khó khăn, nên bạn cần giữ sự thận trọng.
> 
> Ở tầng lá số gốc, cung Điền Trạch có sự giằng co giữa mong muốn vật chất và tâm linh, với các sao như Tham Lang hãm, cho thấy sự dao động trong quyết định.
> 
> Đại hạn 56-65 tuổi, cung Điền Trạch rơi vào Dần với Thiên Cơ hãm và Thái Âm hãm, điều này chỉ ra rằng việc liên quan đến đất đai và tài sản cố định có thể cần sự tinh tế và thay đổi môi trường.
> 
> Lưu niên đại hạn cũng tương tự, cung Điền Trạch rơi vào Dần với Thiên Cơ hãm – Thái Âm hãm, nhấn mạnh thêm sự cần thiết phải giữ vững.
> 
> Tiểu hạn năm 2027, cung Điền Trạch rơi vào Tý với Thái Dương hãm, có thể khiến bạn dễ bộc trực quá mức hoặc dễ hao tổn do người khác, cần chú ý đến việc giữ gìn danh tiếng.
> 
> Lưu Thái Tuế năm Đinh Mùi cũng cho thấy cung Điền Trạch liên quan đến Thiên Đồng hãm, cần sự cẩn trọng trong các quyết định liên quan đến tài sản.
> 
> Tóm lại, bạn nên giữ kết nối thường xuyên với người thân, bạn bè tin cậy và chia sẻ khi cảm thấy áp lực. Khi áp lực tinh thần kéo dài, bạn nên chủ động tìm kiếm sự hỗ trợ từ chuyên gia tâm lý hoặc bác sĩ.
> 
> Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương, Đà La lưu. Đức năng thắng số, lựa chọn của bạn sẽ quyết định kết quả.

## 33. Năm sau sự nghiệp của tôi thế nào? — lá số E

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 50/100

**Đạt** (20.189 giây, 213 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, sự nghiệp của bạn trong năm 2027 được dự đoán là mức trung bình, có lúc lên lúc xuống, với điểm số là 50/100. Điểm tựa chính đến từ đại hạn, nơi cung Quan Lộc tại Thân có Liêm Trinh vượng và gặp Quan Phù, cho thấy sự nghiêm minh và có gu thẩm mỹ, hợp với các lĩnh vực tư pháp, kỷ cương hoặc nghệ thuật, thiết kế. Tuy nhiên, tiểu hạn năm 2027 lại có Thiên Lương hãm tại cung Quan Lộc, điều này gợi ý bạn có thể cảm thấy hơi cô độc hoặc cần phải đối mặt với những vấn đề liên quan đến người khác, nên việc chọn môi trường ổn định như y tế, giáo dục, tư vấn, công tác xã hội sẽ rất phù hợp. Lưu niên đại hạn năm này có điểm số 46/100, cho thấy sự diễn biến trung bình. Lưu Thái Tuế năm Đinh Mùi có điểm 47/100, cho thấy đây là một năm có sự đánh thức về những điều bạn cần chú ý. Nên chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục. Không nên đổi hướng lớn khi chưa có lý do rõ ràng và phương án dự phòng.

## 34. Năm 2027 tôi có thăng chức không? — lá số E

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 50/100

**Đạt** (20.768 giây, 200 chữ). Câu AI:

> Chào bạn. Về câu hỏi năm 2027 có thăng chức không, theo dữ kiện, sự nghiệp của bạn được đánh giá là 50/100, tức là trung bình, có lúc lên có lúc xuống. Điểm tựa của bạn nằm ở đại hạn, nơi cung Quan Lộc có sao Liêm Trinh vượng gặp Quan Phù, cho thấy sự nghiêm minh và có gu thẩm mỹ trong công việc. Tuy nhiên, tiểu hạn năm 2027 lại có Thiên Lương hãm tại cung Quan Lộc, điều này có thể khiến bạn cảm thấy hơi cô độc hoặc phải trải qua một chút thử thách trước khi ổn định. Lưu niên đại hạn năm 2027 lại có Thiên Lương hãm, cho thấy sự cần thiết phải thận trọng. Việc thăng tiến được xem là dấu hiệu mới, liên quan đến lưu niên đại hạn. Cách vận hạn kích hoạt cho thấy sự hỗ trợ từ đại hạn đồng hành và sự hóa lộc từ Thái Âm gặp năm Thái Âm hóa lộc. Bạn nên chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục. Không nên đổi hướng lớn khi chưa có lý do rõ ràng và phương án dự phòng.

## 35. Đi xa lập nghiệp năm 2028 được không? — lá số E

App hiểu là: Di chuyển, xuất ngoại · năm 2028 (Mậu Thân, 41 tuổi âm) · điểm 58/100

**Đạt** (16.797 giây, 150 chữ). Câu AI:

> Chào bạn. Dựa vào dữ kiện, việc đi xa lập nghiệp vào năm 2028 có xu hướng thuận lợi hơn. Điểm chung cho việc di chuyển, xuất ngoại năm 2028 là 58/100, được đánh giá là thuận. Điều này đến từ việc điểm tựa từ lưu niên đại hạn và tiểu hạn đều cho kết quả thuận. Cụ thể, lưu niên đại hạn năm đó có điểm 59/100 thuận, và tiểu hạn năm 2028 cũng có điểm 59/100 thuận. Ngoài ra, Lưu Thái Tuế năm Mậu Thân cũng cho kết quả 59/100 thuận. Điều này cho thấy có nhiều yếu tố tích cực hỗ trợ cho kế hoạch của bạn. Tuy nhiên, bạn nên chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là trong những tháng có Kình Dương, Đà La lưu động.

## 36. Tình duyên đại vận này thế nào? — lá số E

App hiểu là: Tình duyên, hôn nhân · đại vận 34–43 tuổi · điểm 55/100

**Đạt** (15.606 giây, 164 chữ). Câu AI:

> Tình duyên, hôn nhân trong đại vận 34–43 tuổi có mức độ 55/100, được đánh giá là thuận. Lá số gốc tại cung Phu Thê ở Hợi có Thiên Đồng đắc, cho thấy người bạn đời thường hiền hòa, dễ chịu và có xu hướng thuận lợi. Đại hạn giai đoạn 2021–2030, cung Phu Thê của đại hạn rơi vào cung Dần với Tham Lang đắc, người bạn đời có tính lôi cuốn, linh hoạt và giỏi giao tiếp, quan hệ có xu hướng thuận. Tổng hợp lại, có tầng thuận là đại hạn, nên nhìn chung là nghiêng về tốt nếu bạn chủ động. Để duy trì điều này, bạn nên chọn môi trường ổn định, đi đường dài thay vì thay đổi hướng liên tục. Khi có cơ hội lợi nhanh, bạn nên chốt một phần và phân tán rủi ro. Tuy nhiên, bạn không nên ký giấy tờ chưa đọc kỹ hoặc tham gia vào các mối quan hệ mập mờ.

## 37. Năm 2019 công việc của tôi thế nào? — lá số E

App hiểu là: Sự nghiệp · năm 2019 (Kỷ Hợi, 32 tuổi âm) · điểm 49/100

**Đạt** (22.569 giây, 235 chữ). Câu AI:

> Chào bạn. Về công việc của bạn trong năm 2019, theo dữ kiện, sự nghiệp của bạn được đánh giá là trung bình, có lúc lên lúc xuống với điểm số là 49/100. Điều này cho thấy bạn có những giai đoạn thuận lợi và khó khăn trong công việc.
> 
> Ở lá số gốc, cung Quan Lộc của bạn tại Tỵ có Thiên Lương hãm, điều này có thể khiến bạn dễ lo lắng về người khác hoặc đôi khi cảm thấy cô độc, cần tìm môi trường ổn định như y tế, giáo dục, tư vấn hoặc công tác xã hội.
> 
> Trong năm 2019, tiểu hạn tại Tỵ, cung Quan Lộc của bạn rơi vào Dậu, cho thấy bạn có thể hoạt ngôn và giỏi kinh doanh nhưng cũng có xu hướng đa nghi.
> 
> Về Lưu Thái Tuế năm Kỷ Hợi, cung Quan Lộc theo Thái Tuế rơi vào Mão, có Thiên Cơ miếu và Cự Môn miếu, kèm theo Hóa Kỵ, điều này cho thấy có sự bị đánh thức hoặc cần phải thận trọng trong công việc.
> 
> Tổng thể, sự nghiệp năm 2019 là một giai đoạn cần cân bằng. Nên giảm bớt việc và ưu tiên những công việc thực sự cần thiết trong giai đoạn này. Không nên đổi việc vì cảm xúc nhất thời, đặc biệt là khi có các yếu tố như Kình Dương, Đà La lưu động.

## 38. Bạn bè đồng nghiệp năm sau ra sao? — lá số E

App hiểu là: Sự nghiệp · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 50/100

**Đạt** (19.915 giây, 209 chữ). Câu AI:

> Chào bạn. Về vấn đề bạn bè đồng nghiệp năm sau, dựa trên dữ kiện, sự nghiệp của bạn năm 2027 được đánh giá là trung bình, có lúc lên lúc xuống, với điểm số là 50/100. Điều này đến từ sự kết hợp của các yếu tố khác nhau. Ở tầng lá số gốc, cung Quan Lộc tại Tỵ có Thiên Lương hãm, điều này có thể khiến bạn đôi khi lo lắng về người khác hoặc cảm thấy hơi cô độc, nên việc chọn môi trường ổn định, hợp y tế, giáo dục, tư vấn, hoặc công tác xã hội sẽ là điều tốt. Ở tiểu hạn năm 2027, cung Quan Lộc vẫn là Thiên Lương hãm, bạn cần thận trọng trong các mối quan hệ công việc. Lưu niên đại hạn cho thấy kết quả năm này là 46/100, tức là trung bình, có lên có xuống. Lưu Thái Tuế năm Đinh Mùi cho thấy sự việc này sẽ được đánh thức trong năm, với điểm 47/100. Để có kết quả tốt hơn, bạn nên chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục. Không nên đổi hướng lớn khi chưa có lý do rõ ràng và phương án dự phòng.

## 39. Cung Quan Lộc của tôi tốt hay xấu? — lá số E

App hiểu là: Sự nghiệp · năm 2026 (Bính Ngọ) · điểm 54/100

**Đạt** (17.005 giây, 149 chữ). Câu AI:

> Cung Quan Lộc của bạn được xem là trung bình, có lên có xuống với điểm số 54/100. Nền gốc của bạn là 51/100, nhưng đại hạn tại Thân với Liêm Trinh vượng và Quan Phù mang lại điểm 58/100, cho thấy có hướng thuận lợi hơn trong giai đoạn đó. Tiểu hạn năm 2026 có điểm 52/100, diễn biến trong năm sẽ có sự kìm hãm nhưng vẫn có cơ hội. Lưu Thái Tuế năm Bính Ngọ có điểm 49/100, cho thấy việc được đánh thức trong năm. Nhìn chung, sự nghiệp của bạn có thể phát triển nếu bạn chọn môi trường ổn định, hợp y tế, giáo dục, tư vấn, công tác xã hội. Nên chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục. Không nên đổi hướng lớn khi chưa có lý do rõ ràng và phương án dự phòng.

## 40. nam 2027 co nen dau tu khong — lá số E

App hiểu là: Tài chính · năm 2027 (Đinh Mùi, 40 tuổi âm) · điểm 59/100

**Đạt** (14.206 giây, 134 chữ). Câu AI:

> Chào bạn. Dựa trên dữ kiện, về tài chính trong năm 2027, có nhiều yếu tố cho thấy xu hướng thuận lợi. Điểm chung cho thấy tài chính năm 2027 có mức 59/100, được đánh giá là thuận. Điều này đến từ sự kết hợp của lưu niên đại hạn, tiểu hạn và Lưu Thái Tuế đều cho thấy sự tích cực. Đại hạn của bạn cũng đang ở giai đoạn tốt, giúp nền tảng tài chính vững vàng. Tuy nhiên, bạn cần chú ý đến việc quản lý chi tiêu và ghi chép dòng tiền một cách đều đặn. Bạn nên đọc kỹ hợp đồng và lưu giữ đầy đủ các chứng từ. Không nên thay đổi hướng đi lớn khi chưa có lý do rõ ràng và có phương án dự phòng.

