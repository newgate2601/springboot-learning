---
name: grammar-from-books
description: Soạn và biên tập bài học ngữ pháp theo sách hoặc unit, giải thích bằng tiếng Việt tự nhiên và trình bày HTML như một bài trong giáo trình. Không dùng cho phân tích câu độc lập.
---

# Bài học ngữ pháp

## Phạm vi và tài liệu

- Cập nhật duy nhất `03-grammar/grammar.html` tại chỗ; không tạo sổ Markdown. Khi chưa có bài học đầu tiên thì để HTML trống. Nếu người dùng yêu cầu chỉ giải thích trong chat, không lưu.
- Sách hiện dùng: Raymond Murphy, *English Grammar in Use*, bản 5 (2019), `C:/Users/TONY PC/Downloads/MUCLecture_2022_5217521.pdf`. Unit 1 ở trang in 2, trang PDF 14. Xác minh vị trí unit thực tế, không mặc định cùng độ lệch cho mọi file. Nếu thiếu tài liệu, tìm tài liệu đã cung cấp trước khi hỏi đường dẫn.
- Khi soạn bài hoặc thêm kiến thức, đọc phần sách liên quan (kể cả ghi chú, bảng, ngoại lệ) và đối chiếu nguồn có uy tín như Cambridge, British Council. Đọc trang nguồn thực tế; không nhận đã đọc khi chưa truy cập được. Với PDF, dùng skill PDF và xem ảnh trang khi bảng/ký hiệu trích xuất không rõ.
- Khi chỉ biên tập câu chữ hoặc đổi bố cục của kiến thức đã có, không cần đọc lại toàn sách. Nếu thay quy tắc hoặc thêm kiến thức thì xác minh phần thay đổi.
- Câu hỏi bổ sung về ví dụ thuộc bài học được cập nhật vào bài tương ứng. Phân tích câu độc lập ngoài bài dùng `02-sentence-structures/skill.md`. Không trộn nội dung giữa các notebook.

## Nguồn không xuất hiện trong giao diện đọc

- Giữ bản ghi đối chiếu trong `<template id="reference-record">` cuối HTML: tài liệu, phiên bản, unit/trang và URL thực sự dùng. Phân biệt câu nguyên văn, câu điều chỉnh, câu tự soạn; không gán sai xuất xứ. Khi thay ví dụ, cập nhật bản ghi để truy được đúng câu.
- Trong bài đọc và lời giao bài, không nói “theo sách”, “sách kể”, “bổ sung nguồn mạng”, tên tác giả, tên tài liệu, số trang hoặc số trích dẫn, trừ khi người dùng hỏi nguồn. Không có mục Nguồn tham khảo trong mục lục. Bản ghi nguồn không tham gia giao diện hoặc trợ năng của bài học.
- Diễn giải bằng lời riêng, dùng trích dẫn ngắn trong giới hạn cho phép. Chỉ tham khảo bố cục, nhịp trình bày và phương pháp giải thích của mẫu; không chép toàn bài, hình minh họa hoặc nhận diện của bên khác.

## Viết cho người mới học bằng tiếng Việt tự nhiên

- Viết như một người Việt đang giảng trực tiếp: câu trọn ý, từ quen thuộc, nói rõ ai đang làm gì. Không dịch theo thứ tự từ tiếng Anh, không dùng khẩu hiệu cụt như “Việc đang xảy ra, chưa xong” để thay lời giải thích.
- Mỗi cách dùng gồm: một quy tắc dễ hiểu → tình huống cụ thể đủ để hiểu ví dụ → câu tiếng Anh và nghĩa → một vài câu chỉ rõ vì sao cấu trúc đó phù hợp. Tình huống và ví dụ phải khớp nhau; ví dụ về việc đang diễn ra nên ưu tiên hành động cụ thể như lái xe trước khi dùng câu có thêm cấu trúc khó như try to.
- Giải thích “vì sao” bằng nội dung của chính câu: “Cô ấy đang lái xe thật, không phải kể thói quen đi làm hằng ngày, nên dùng is driving”. Không chỉ nhắc lại “dùng tiếp diễn vì việc đang diễn ra” mà bỏ thông tin giúp người học nhận ra điều đó.
- Ngắn gọn là bỏ lời thừa, không bỏ điều kiện áp dụng, ngữ cảnh hoặc mối liên hệ giữa các ý. Không giới hạn cứng số câu; dùng đủ lời để hiểu, sau đó cắt chỗ lặp. Một đoạn phát triển một ý, thường 2–3 câu vừa phải. Bối cảnh, ví dụ và giải thích luôn đặt sát nhau.
- Mỗi nhóm quy tắc, điều kiện, thành phần câu hoặc ký hiệu là một dòng danh sách hoặc hàng bảng riêng. Không dồn nhiều công thức thành một dòng bằng dấu chấm giữa, gạch chéo hoặc chấm phẩy. Gạch chéo chỉ nhóm các lựa chọn dùng chung một quy tắc, như He / She / It.
- Ký hiệu S, V, V-ing, mệnh đề, trọng âm… phải có nghĩa bằng lời thông thường ngay khi xuất hiện. Ví dụ: “Chủ ngữ là người hoặc vật được nói đến”; “Trọng âm là âm tiết được đọc nổi bật hơn”. Không giải thích thuật ngữ khó bằng một thuật ngữ khó khác.
- Với am/is/are, phải đủ ba nhóm: I → am; He/She/It và danh từ số ít → is; You/We/They và danh từ số nhiều → are. Mỗi nhóm riêng một hàng. Nêu rõ you dùng are cả số ít lẫn số nhiều, kèm cụm minh họa dễ hiểu.
- Bản dịch tiếng Việt phải tự nhiên và đúng ý trong tình huống. Không cố giữ từng từ “đang”, “có đang”, “trở nên” nếu làm câu gượng. Đối chiếu sắc thái với ví dụ: “Is your English getting better?” có thể dịch “Dạo này tiếng Anh của bạn có khá hơn không?”.
- Thuật ngữ, tiêu đề và lời giảng bằng tiếng Việt; câu tiếng Anh và cụm được phân tích có `lang="en"`. Công thức dùng tiếng Anh/ký hiệu khi cần, không đánh dấu cả đoạn Việt là tiếng Anh.

## Tổ chức bài như trang giáo trình

- Template chuẩn là bản `grammar.html` hiện có: bài đọc liên tục, phân mục bằng chữ A/B/C và tiêu đề, quy tắc ngắn đặt cạnh ví dụ; bảng dành cho nhóm chủ ngữ, dạng câu, so sánh và chính tả. Tham khảo cách tổ chức trang lý thuyết trong mẫu `https://assets.cambridge.org/97805216/65773/sample/9780521665773ws.pdf` (trang PDF 6) và phần Explanation ở `https://test-english.com/grammar-points/a1/present-continuous/`. Đây là nguồn tham khảo bố cục, không phải yêu cầu sao chép nguyên trang.
- Mạch ưu tiên: hiểu tình huống sử dụng → cách tạo câu → so sánh gần nghĩa → lỗi thường gặp → chính tả/ghi chú liên quan → giải thích cấu trúc trong ví dụ → cách dùng ngoài trọng tâm đã có → ghi nhớ. Điều chỉnh theo chủ đề, không ép mọi bài theo cùng số mục.
- Nội dung hiển thị đầy đủ khi mở trang. Không dùng details/summary, accordion, tab ẩn nội dung hoặc nút “xem thêm”. Quy định này thay toàn bộ yêu cầu thu gọn của các phiên bản skill trước, kể cả mục lục unit. Mục lục là danh sách neo hiển thị sẵn theo bài.
- Giải thích dài cần phân cấp bằng heading, đoạn ngắn, danh sách và bảng. Cấu trúc phụ hoặc kiến thức ngoài trọng tâm đặt trong phần riêng ở cuối bài, có neo từ ví dụ liên quan và mục lục. Không chen một bài giảng dài vào giữa quy tắc chính và ví dụ chính.
- Mỗi ví dụ có tiếng Anh và bản dịch cùng hàng, ngay dưới là lời giải thích của chính câu đó. Desktop có thể hai cột; điện thoại xếp dọc trong cùng khối. Tô nhấn đúng cụm đang học, không tô cả câu. Tránh lặp một câu ở nhiều phần chỉ để minh họa cùng một ý.
- Giải thích cấu trúc phụ đi theo: nghĩa → công thức nếu hữu ích → ví dụ → tách các cụm trong câu và lý do dùng. Chỉ thêm ví dụ khi làm rõ một điểm mới, không bắt buộc thêm câu chỉ thay danh từ.
- Bảng so sánh luôn đặt nghĩa sát câu/cụm tiếng Anh. Nếu cột Nghĩa và giải thích gộp chung, tách dòng rõ ràng. Câu sai có nhãn đúng mức độ, câu sửa, nghĩa và lý do; dấu/màu không được là tín hiệu duy nhất.
- Không đưa bài tập, câu đố, đáp án hay vị trí bài tập vào trang. Ví dụ câu hỏi vẫn phải được dịch và giải thích, không để người học đoán như một câu đố.
- Khi refactor toàn bài, được đổi ví dụ hoặc thứ tự để dễ học, nhưng phải giữ đủ quy tắc, ngoại lệ, sắc thái và các nội dung người dùng đã hỏi. Không xóa kiến thức chỉ vì khó bố trí. Các bài khác và các ID cũ phải tiếp tục dùng được.

## Giao diện và hành vi HTML

- HTML UTF-8, `lang="vi"`, viewport và CSS nội bộ duy nhất, không CDN/thư viện. Mỗi bài là một article có ID ổn định. Mục lục và công thức tra nhanh dùng neo tới đầu khối giải thích, không tới giữa ví dụ.
- Bố cục trang giáo trình: nền ngoài xám nhạt, bề mặt đọc trắng, màu mực xanh đậm, điểm nhấn xanh teal; header đơn giản, không hero gradient lớn. Mục lớn liền mạch, ngăn bằng khoảng trắng/đường kẻ, không biến mọi đoạn thành thẻ bo tròn.
- Trang tối đa khoảng 1300 px, mục lục 205–238 px bên trái, sticky và cuộn được; cột nội dung có lề trong 30–44 px. Mỗi phần gồm ký hiệu nhỏ A/B/C và nội dung. Màn hình nhỏ chuyển mục lục lên đầu; lề nội dung khoảng 20 px, không ép chữ nhỏ để nhét.
- Segoe UI, Tahoma, Arial cho dấu tiếng Việt ổn định. Thân bài khoảng 16–17 px, giãn dòng 1.8–1.85; ví dụ tiếng Anh 17–18 px; giải thích 16 px. Không Georgia/Times, letter-spacing âm hoặc font nghiêng cho đoạn Việt. Nhãn điều hướng có thể nhỏ hơn; không biến quy tắc cơ bản thành chú thích bé.
- Ví dụ có đường kẻ trái nhẹ; công thức nền xanh nhạt; ghi chú nền vàng nhạt; bảng có tiêu đề nền nhẹ và đường ngang mảnh. Không khung lồng khung. Một ý cần đọc liên tục phải gần nhau, không tăng khoảng trắng đến mức quy tắc xa ví dụ.
- Responsive: không tràn ngang toàn trang. Bảng thật sự rộng được cuộn trong vùng bảng. Không nowrap câu ví dụ. Dùng neo gốc và focus bàn phím rõ; không cần JavaScript để mở nội dung. Hỗ trợ reduced motion và in trang.

## Kiểm tra trước khi giao

1. Đọc lại bài như người chưa biết chủ đề: mỗi mục có cho biết dùng lúc nào, ví dụ nghĩa gì, vì sao dùng dạng đó không? Chỗ nào cần đoán nghĩa hoặc phải biết thuật ngữ từ trước thì sửa.
2. Rà lời Việt bằng cách đọc riêng, không nhìn bản tiếng Anh. Thay câu gượng, câu cụt, từ ngữ trừu tượng bằng cách nói tự nhiên; không chỉ thay vài từ đồng nghĩa. Giữ đủ nhóm áp dụng và điều kiện quan trọng.
3. Đối chiếu các ý cũ khi refactor: quy tắc, cấu trúc phụ, ví dụ cần giữ và ngoại lệ đều còn hoặc đã được diễn đạt tương đương. Không suy ra “đủ ý” từ số chữ hay số ví dụ.
4. Kiểm tra HTML hợp lệ, ID duy nhất, neo trỏ đúng, không có nội dung ẩn phải bấm mở, nguồn chỉ ở template. Kiểm tra các bảng và cặp câu/dịch ở desktop, điện thoại nếu có công cụ render; chỉ nói đã xem ảnh khi thực sự xem.
5. Giao liên kết tới HTML và skill nếu có sửa; tóm tắt kết quả ngắn, nêu đúng giới hạn kiểm tra. Không lặp tên tài liệu tham khảo trong lời giao trừ khi người dùng đang hỏi nguồn.
