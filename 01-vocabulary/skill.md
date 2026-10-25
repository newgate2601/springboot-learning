---
name: vocabulary
description: Cập nhật sổ từ vựng HTML của workspace: nghĩa tiếng Việt, loại từ cạnh từ, họ từ, cấu trúc phổ biến, ví dụ có nguồn, IPA và audio US/UK. Dùng khi người dùng yêu cầu note/thêm từ mới hoặc chỉnh sổ từ vựng.
---

# Vocabulary

## Sổ mặc định và cách cập nhật

- Đọc và sửa trực tiếp `01-vocabulary/vocabulary-audio.html`, tính từ gốc workspace. Đây là sổ học duy nhất; không tạo notebook Markdown hay bản HTML thứ hai.
- Yêu cầu thêm/note từ mới đồng nghĩa tự thêm vào HTML trong lượt hiện tại. Nếu chỉ hỏi giải thích và yêu cầu không lưu, trả lời trong chat. Khi trùng mục, bổ sung mục đã có, tránh nhân đôi.
- Giữ nội dung và cơ chế audio hiện có. Thêm mục lục `nav` trỏ tới ID ổn định của `article.word-card`. Không xây lại toàn bộ sổ khi chỉ thêm một từ.
- Giữ bố cục hiện tại: header có số mục, từ tiếng Anh, badge `.pos` cạnh từ và nghĩa tiếng Việt; `.word-body` có ghi chú ngữ pháp, cấu trúc, ví dụ và họ từ. Dùng `.usage-grid`, `.usage`, `.source-example`, `.family-card`, `.structure`, `.translation`, `.example-source` theo mẫu.
- Không thêm bài tập, đáp án, lịch học hoặc thử thách khi chưa được yêu cầu.

## Nội dung của mỗi từ

- Loại từ phải hiển thị cạnh từ bằng `.pos`, không đặt riêng trong bullet. Nêu các từ loại phổ biến của từ gốc; ghi rõ đếm được/không đếm được và ngoại động/nội động khi hữu ích.
- Giải thích nghĩa phổ biến bằng tiếng Việt, phân biệt nghĩa bằng ngữ cảnh. Không giới hạn vào chủ đề của video chọn từ.
- Xóa và không thêm lại phiên âm kiểu tiếng Việt/gợi đọc. Giữ IPA, audio và giải thích phát âm ngắn khi cần; không dùng cách viết tiếng Việt để thay thế âm tiếng Anh.
- Chọn các nghĩa, từ loại và dạng cùng họ **phổ biến trong đời sống và hữu ích cho người học**; không làm một mục từ điển đầy đủ. Việc tra thấy một nghĩa hoặc dạng từ không tự động là lý do thêm vào sổ. Không liệt kê đủ danh từ/động từ/tính từ/trạng từ chỉ để đủ bộ.
- Chỉ mở rộng họ từ khi dạng đó thông dụng và giúp phân biệt cách dùng thực tế. Mỗi dạng được chọn có nghĩa, loại từ, IPA/audio và ví dụ có dịch. Bỏ nghĩa hiếm, chuyên ngành, văn chương và dạng kém hữu ích trừ khi người dùng hỏi hoặc câu nguồn cần nó; không cần liệt kê cả những dạng đã bỏ.
- Ví dụ về mức độ chọn lọc: với firm, ưu tiên tính từ “chắc/kiên quyết”, danh từ “công ty” và firmly nếu hữu ích; không mặc định thêm firm động từ, firm trạng từ và firmness thành các mục riêng. Với film, ưu tiên “phim” và “quay phim”; không tự mở rộng sang nghĩa màng/lớp mỏng hay thuật ngữ điện ảnh.
- Chỉ tạo khối công thức khi có giới từ cần nhớ, mẫu bổ ngữ, giới hạn ngữ pháp, thành ngữ/cụm động từ hoặc sắc thái dễ nhầm đáng học. Không biến tổ hợp giữ nguyên nghĩa như `invent + a method`, tính từ + danh từ bất kỳ hoặc danh từ chỉ đối tượng trong câu thành công thức riêng. Giữ những tổ hợp đó dưới dạng ví dụ của nghĩa tương ứng. Mỗi công thức thực sự được chọn vẫn có ví dụ và phát âm ngay bên dưới.
- Tách cấu trúc thông dụng khỏi thành ngữ/cụm có nghĩa riêng. Nêu rõ nghĩa thay đổi của toàn cụm; tránh bảng collocation dài không giúp phân biệt cách dùng.

## Ví dụ và nguồn

- Trình bày theo từng khối liền mạch: **nghĩa/cách dùng → công thức nếu có điểm đáng học → câu ví dụ → phát âm US/UK (nút audio kèm IPA) → dịch → nguồn**. Không bắt mọi nghĩa hay họ từ phải có nhãn công thức. Nếu có công thức, câu ví dụ và hàng phát âm nằm ngay bên dưới; không gom ví dụ sang phần khác.
- Mỗi `.usage` chứa luôn `.source-example` minh họa cấu trúc đó; mỗi `.family-card` cũng đặt ví dụ/phát âm ngay sau cấu trúc của dạng từ. Nếu một thẻ hiện có chứa nhiều công thức khác nhau, chia thành các khối nhỏ và đặt ví dụ tương ứng dưới từng khối. Ví dụ có thể minh họa nhiều điểm gần nhau khi mối liên hệ rõ ràng, nhưng không để người học tự tìm câu ở phần khác.
- Hàng phát âm của câu nằm ngay dưới câu tiếng Anh, với IPA sát nút US/UK tương ứng. Giữ IPA/audio của chính mục từ cạnh từ như quy ước hiện có. Khi thêm hoặc sửa nội dung, áp dụng cách nhóm này cho phần đang sửa; không tự xây lại toàn bộ sổ nếu chỉ được yêu cầu sửa skill.
- Ưu tiên câu thực tế lấy từ Oxford Learner’s Dictionaries, Cambridge, Merriam-Webster hoặc BBC/British Council. Đọc và xác minh câu trên trang nguồn trước khi gắn nguồn; không coi câu tự soạn là câu trích từ từ điển.
- Mỗi ví dụ gồm câu tiếng Anh, hai nút US/UK, IPA toàn câu bên cạnh từng nút, bản dịch Việt và link trực tiếp dưới câu tới trang có ví dụ. IPA câu do soạn là cách đọc minh họa, không gán cho nguồn từ điển và không đảm bảo khớp tuyệt đối giọng tổng hợp.
- Câu từ ảnh/audio người dùng cung cấp được ghi nguồn đúng là nội dung người dùng; không gắn nhãn Oxford.
- Tôn trọng giới hạn trích dẫn của nguồn; chọn ví dụ ngắn và đại diện thay vì sao chép dài. Nếu cần viết lại, ghi rõ “phỏng theo” và nguồn, không dùng nhãn trích nguyên văn. Nếu giữ ví dụ tự soạn, ghi rõ và chỉ dùng khi có lý do; mặc định ưu tiên ví dụ đã xác minh.
- Có thể thêm cách nói thân mật/tiếng lóng thực sự phổ biến khi giúp hiểu lời nói bản xứ. Kiểm tra trong nguồn uy tín, ghi rõ informal/slang, vùng dùng (US/UK…), nghĩa trong hội thoại, sắc thái và giới hạn ngữ cảnh. Không gọi mọi cách nói tự nhiên là tiếng lóng, không ép thêm slang khi không có cụm phù hợp.
- Mỗi dạng từ liên quan cũng tuân thủ các yêu cầu về ví dụ và nguồn; không chỉ thêm danh sách tên từ.

## IPA và audio

- Mỗi từ/cụm/câu dùng `<span class="spoken" data-say="English text">English text</span>`. `data-say` chỉ chứa tiếng Anh cần đọc. Escape nội dung và thuộc tính đúng cách; JavaScript tự thêm hai nút, không chèn nút trùng lặp.
- Câu ví dụ dùng `data-ipa-uk="/.../"` và `data-ipa-us="/.../"`. Đặt từng IPA sát nút tương ứng qua `.pron-group` và `.inline-ipa`, không tách thành bullet xa audio.
- Từ đơn ưu tiên bản thu và IPA từ trang Oxford/Cambridge chính thức. Xác minh URL MP3 từ trang nguồn; không đoán URL. Cập nhật `dictionary` trong script (key chữ thường, `source`, `us: {audio, ipa}`, `uk: {audio, ipa}`). Giữ link nguồn cạnh nút audio. Nếu IPA và audio khác nguồn, ghi riêng nguồn và biến thể.
- Không dùng Google Dịch. Không mô tả audio tổng hợp là bản thu từ điển hoặc của người trong video. Không âm thầm dùng giọng máy thay audio từ điển khi tải lỗi.
- Cụm/câu không có bản thu gốc dùng giọng tổng hợp đúng locale, có nhãn “Máy đọc”. Các cụm có IPA riêng có thể thêm vào `phraseIPA`; IPA thuộc tính trên phần tử được ưu tiên.
- Hai nút gọi `speak(text, 'en-US'/'en-GB', button)`. Audio từ điển dùng `Audio`; câu dùng `speechSynthesis`, tốc độ 1. Không thêm chọn tốc độ/giọng hoặc nút phát ở đầu trang.
- Bấm lại nút đang phát để dừng; chuyển câu/giọng dừng cả audio cũ lẫn speechSynthesis. Báo rõ thiếu giọng hoặc lỗi audio, không âm thầm đổi locale. Giọng máy có thể tải bất đồng bộ qua `voiceschanged`.
- Giữ aria-label, trạng thái aria-pressed, focus bàn phím và `role="status"`.

## Nối âm khi cần

Phân biệt linking, weak forms, elision, assimilation và contraction. Giải thích bằng IPA và cách đặt lưỡi/miệng khi hữu ích; không thêm gợi đọc tiếng Việt. Biến thể giọng và cách nói nhanh không phải quy tắc bắt buộc; không hướng dẫn luôn bỏ phụ âm cuối hoặc luôn đổi /t/ thành âm khác. Không nhận là phiên âm chính xác của video khi chưa nghe/phân tích audio.

## Kiểm tra trước khi giao

- Mục mới có badge loại từ cạnh từ, nghĩa, cấu trúc và dạng từ thông dụng kèm ví dụ nguồn; không còn gợi đọc tiếng Việt.
- Rà độ cần thiết: chỉ giữ nghĩa/dạng từ phổ biến, tránh mở rộng lan man và lặp ví dụ. Nếu người dùng yêu cầu rút gọn, thực sự bỏ các nhánh ít hữu ích thay vì chỉ đổi tiêu đề hoặc thu gọn chúng vào mục ẩn.
- Mọi câu có dịch, link đúng trang nguồn, IPA sát audio US/UK. Phân biệt nguồn câu, nguồn IPA và nguồn audio.
- Mỗi công thức có câu minh họa và hàng phát âm ngay bên dưới trong cùng khối; không còn danh sách công thức tách rời phần ví dụ ở nội dung vừa thêm/sửa.
- Các mục cũ và mục lục còn hoạt động; không trùng ID, không nút audio lặp, script hợp lệ và không có chọn giọng/tốc độ.
- Nếu công cụ cho phép, kiểm tra bố cục và hành vi audio; không nói đã nghe khi chưa nghe. Giao link HTML và nhắc tải lại trang khi phù hợp.
