---
name: sentence-structures
description: Chọn điểm cấu trúc ít quen thuộc, khác thường hoặc dễ hiểu sai trong câu/ảnh người dùng gửi để thêm vào HTML, kèm ví dụ và IPA/audio US/UK tại chỗ.
---

# Sentence structures

- Đọc và cập nhật `02-sentence-structures/sentence-structures.html` trong workspace. Đây là sổ cấu trúc câu, thay thế mục nối âm cũ. Không tạo Markdown notebook. Từ vựng vẫn theo `01-vocabulary/skill.md`.
- Khi người dùng gửi câu để học cấu trúc, dùng chính câu đó làm điểm xuất phát. Ghi nguyên câu và dịch Việt; nếu ảnh chỉ có một mệnh đề/đoạn phụ đề, nói rõ chưa phải câu độc lập. Không tự bịa phần thiếu rồi nhận là câu gốc. Khi cần hoàn chỉnh câu, ghi rõ “câu điều chỉnh từ ngữ cảnh”.
- Chỉ chọn điểm cấu trúc khác thường, ít quen thuộc với người học hoặc dễ hiểu sai trong chính ảnh/câu được gửi. Không tự tạo bài cho các mẫu cơ bản như when + mệnh đề, S + V + O hoặc start + V-ing nếu chúng không gây vấn đề trong câu. Nếu không có điểm đáng chọn, nói ngắn gọn và không thêm mục chỉ để có nội dung. “Ít gặp” là tiêu chí chọn lọc học tập, không tự khẳng định một cách nói hiếm trong tiếng Anh bản xứ khi chưa có bằng chứng.
- Mỗi mục là một câu hoặc nhóm câu cùng điểm cấu trúc. Giữ template như từ vựng: `article.word-card`, header, `.word-body`, `.usage`, `.source-example`, `.translation`, nguồn và mục lục tới ID ổn định. Chọn một điểm chính; chỉ thêm tối đa vài mẫu liên quan khi làm rõ khác biệt thực sự. Không ép đủ 2–3 mẫu và không mở rộng thành bài ngữ pháp tổng quát.
- Phân tích cơ bản, bằng tiếng Việt dễ hiểu: chủ ngữ (S), động từ (V), tân ngữ (O), bổ ngữ (C) hoặc mệnh đề khi thực sự cần. Giải thích ký hiệu tại chỗ. Nêu nghĩa và khi dùng, không chỉ đưa công thức.
- Mỗi khối đi liền: **cấu trúc → cách dùng → câu ví dụ → phát âm US/UK → dịch → nguồn/ghi chú**. Ưu tiên dùng nguyên câu hoặc phiên bản thay đổi tối thiểu của câu người dùng làm ví dụ. Không liệt kê công thức một nơi rồi gom ví dụ xa phía dưới.
- Ưu tiên sắc thái hội thoại, cách kết hợp từ dễ hiểu nhầm, mẫu rút gọn/lược thành phần, đảo ngữ hoặc giới hạn ngữ pháp không hiển nhiên, khi chúng xuất hiện trong câu nguồn. Không biến việc thay một danh từ cụ thể thành công thức mới. Mẫu liên quan phải làm rõ điểm đang học, không chỉ lặp câu với chủ thể khác.
- **Mọi câu tiếng Anh dùng làm ví dụ**, kể cả câu gốc, câu điều chỉnh, câu trong phần so sánh/ghi chú và mẫu liên quan, đều có dịch Việt, IPA toàn câu UK/US, hai nút 🔊 US / 🔊 UK và nhãn nguồn. Tránh câu sai; nếu cần minh họa lỗi, ghi rõ và cung cấp đầy đủ phiên âm/audio của cả câu sai lẫn câu sửa, không ngụ ý nên học câu sai.
- Dùng `<span class="spoken" data-say="English sentence" data-ipa-uk="/.../" data-ipa-us="/.../">English sentence</span>` theo mẫu. Script thêm nút; IPA sát nút tương ứng, hàng phát âm ở ngay dưới câu. Escape HTML/thuộc tính, không tạo nút trùng. Công thức với ký hiệu S/V/O không cần đọc thành câu.
- Audio câu dùng giọng tổng hợp đúng locale, ghi “Máy đọc”, không phải audio gốc/video/từ điển. Không dùng Google Dịch. Giữ tốc độ 1, không thêm bộ chọn tốc độ/giọng; bấm lại dừng, chuyển câu dừng câu trước, báo rõ giọng thiếu/lỗi. Giữ aria-label, aria-pressed, focus bàn phím và status.
- IPA là cách đọc minh họa nhất quán UK/US, không phải phiên âm xác nhận video khi chưa phân tích audio. Không gợi đọc bằng tiếng Việt. Không cần các cột đọc rõ/nối âm hoặc một bài nối âm riêng.
- Câu từ người dùng ghi nguồn đúng là ảnh/tin nhắn; câu thay đổi/tự soạn phải có nhãn rõ. Dùng Cambridge Grammar, British Council, Oxford hoặc nguồn ngữ pháp uy tín để xác minh điểm chưa chắc và gắn link hỗ trợ quy tắc; không gán nguồn ngữ pháp thành nguồn của câu tự soạn. Không sao chép dài.
- Giữ mức độ phổ biến, thực dụng, không bài tập/lịch học trừ khi được yêu cầu. Đừng tự thêm cấu trúc mỗi lần người dùng chỉ yêu cầu thêm từ vựng; yêu cầu chat-only thì không lưu.
- Trước khi giao: câu/mẫu đúng ngữ pháp, giải thích khớp câu; mỗi ví dụ có hai IPA, hai nút qua script, dịch và nguồn ở cùng khối; ID/mục lục đúng, script hợp lệ, nội dung cũ còn nguyên. Không nhận đã nghe/kiểm tra UI khi chưa làm. Giao link HTML.
