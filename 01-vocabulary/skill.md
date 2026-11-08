---
name: vocabulary
description: Thêm hoặc sửa từ tiếng Anh trong sổ HTML của workspace, gồm nghĩa Việt, hình gợi nhớ, ví dụ, cách dùng, phân biệt từ gần nghĩa và IPA/audio Oxford US/UK. Dùng khi người dùng nói thêm, add, note hoặc sửa từ; tôn trọng yêu cầu chỉ giải thích trong chat.
---

# Vocabulary notebook

## Nơi lưu và nguồn dữ liệu

Hai sổ từ đơn dùng cùng template: sổ thông dụng `01-vocabulary/common-vocabulary.html` và sổ ít thông dụng `01-vocabulary/less-common-vocabulary.html` (dữ liệu `less-common-words.json`). Cập nhật tại chỗ; không tạo notebook Markdown. Ngoại lệ theo yêu cầu người dùng: sổ cụm từ riêng tại `01-vocabulary/english-phrases.html`. Khi người dùng nói “thêm từ X”, thực hiện luôn trong lượt hiện tại. Nếu họ yêu cầu chỉ giải thích, không lưu.

Mặc định áp dụng cho mọi danh sách từ, không cần người dùng nhắc lại: **chuẩn hóa về dạng từ điển rồi chỉ thêm mục chưa có**. Động từ đưa về nguyên mẫu không `to` (kể cả dạng bất quy tắc); danh từ đưa về số ít khi thích hợp; tính từ/trạng từ đưa về dạng cơ bản khi đúng nghĩa. Giữ nguyên danh từ vốn dùng ở số nhiều, từ đã từ vựng hóa và cụm cố định; không máy móc bỏ `-s`, `-ed`, `-ing`, không đổi danh từ như `dresser` thành động từ `dress`. Xác định theo nghĩa/ngữ cảnh; tra từ điển nếu mơ hồ.

Tra headword trước trong toàn bộ TSV, JSONL và additional-words.json của sổ thông dụng, rồi trong less-common-words.json, đối chiếu cả biến thể chính tả và các tên cùng nghĩa được ghi trong mục trước khi cấp ID. Từ chỉ xuất hiện trong câu ví dụ/ghi chú chưa được tính là có một mục riêng. Nếu đã có mục phù hợp thì giữ nguyên, chỉ bổ sung khi người dùng yêu cầu sửa hoặc còn thiếu nghĩa đang hỏi. Không tự tạo lại các mục trùng của PDF hoặc thay đổi ID cũ. Với cách gọi không chuẩn/chưa xác minh, ưu tiên headword chuẩn và ghi lại cách người dùng nhập trong ghi chú để tìm kiếm; giải thích ngắn thay vì tạo mục sai. Báo rõ từ đã có và từ thực sự vừa thêm.


Mặc định khi nhập danh sách từ tài liệu: không liệt kê lại mục trùng, biến thể chính tả hoặc tên tương đương đã được ghi trong mục hiện có. Nếu từ được yêu cầu là dạng cùng họ khác loại từ (N/Adj/Adv/V…) với mục đã có, vẫn ghi dạng mới vào sổ ít thông dụng khi chưa có đúng dạng/nghĩa/loại từ; không tự thêm cả họ từ khi người dùng chưa yêu cầu. Tách cụm ghép nhiều từ thành các từ đơn có nghĩa để chuẩn hóa và kiểm tra trùng (ví dụ `office supplies` → `office`, `supply`; `printer paper` → `printer`, `paper`); không thêm lại cả cụm như mục mới. Không tách cơ học một từ viết liền đã từ vựng hóa như `notebook`, `deadline`, hoặc ép nghĩa từng thành tố thay cho nghĩa cả từ. Quy tắc này ưu tiên hơn hướng dẫn chung về việc giữ cụm cố định khi nhập danh sách; nếu người dùng yêu cầu học riêng cả cụm thì làm theo yêu cầu đó.

HTML được sinh bởi `node 01-vocabulary/build-vocabulary.cjs`. Không chỉ sửa HTML rồi để lần build sau ghi đè mất nội dung. Đọc mục hiện có và sửa dữ liệu tương ứng:

Builder tự cập nhật `01-vocabulary/vocabulary-flashcards.html` từ cả ba sổ qua `build-flashcards.cjs` và `vocabulary-flashcards.html.in`, kể cả khi chạy riêng với `--less-common` hoặc `--phrases`. Sửa dữ liệu nguồn rồi chạy builder để mục mới xuất hiện trong sổ và bộ thẻ. Mỗi lượt bốc lấy từ hoặc cụm từ không trùng, mặt trước tiếng Anh, mặt sau nội dung đầy đủ.

Flashcards bốc ngẫu nhiên từ nhóm thẻ chưa chấm, thẻ có kết quả gần nhất là Sai và thẻ đánh dấu. Nếu lượt có thẻ chưa chấm và chưa đánh dấu, đưa một thẻ như vậy lên đầu sau khi bốc. Có thể bốc riêng thẻ Sai hoặc thẻ đánh dấu. Thẻ đang xem được tính là đúng khi chuyển tiếp hoặc thoát nếu không bấm Sai. Hoàn thành lượt sẽ mở màn hình kết quả. Thoát sớm lưu kết quả đã làm và giữ thẻ chưa xem ở trạng thái chưa chấm. Tab Lịch sử ghi số đúng, sai, chưa chấm và liên kết đến thẻ sai. Dữ liệu được lưu trong localStorage. Xóa một hoặc toàn bộ lượt sẽ tính lại kết quả gần nhất từ các lượt còn lại và giữ dấu ôn. Xuất và nhập JSON để chuyển dữ liệu giữa các máy. Nhập tệp thay thế dữ liệu hiện có sau khi xác nhận.

- #1–500: `words-500.tsv` (phân cách bằng `|`), `usage-500.tsv`.
- #501–3396: ba file `continuation-*.jsonl`, mỗi dòng là một object. Giữ ID PDF dù nguồn có mục trùng. Khi sửa các dòng này, build sẽ cập nhật snapshot tiến độ.
- Từ thêm trước khi chia sổ: `additional-words.json`, giữ nguyên nội dung và ID để các liên kết cũ tiếp tục hoạt động. Từ mới từ nay ghi vào `less-common-words.json`, mảng object cùng schema bên dưới. ID mới là số lớn nhất trong cả PDF, additional-words.json và less-common-words.json cộng 1; không đổi ID cũ và không dùng chung ID giữa hai sổ. Tìm trước trong cả bộ; từ đã có thì bổ sung mục phù hợp, không nhân đôi vô ý. Với từ đồng tự nhưng khác cách phát âm, nêu rõ từ loại/nghĩa.

Schema nội dung:

```json
{"id":3397,"word":"headword","pos":"n.","meaning":"nghĩa tiếng Việt","image":"🌱","example":"An original English example.","translation":"Bản dịch Việt.","usage":"Cách dùng chính; mẫu bổ ngữ hoặc giới từ đáng nhớ.","contrast":"Phân biệt với một từ gần nghĩa, có tiêu chí cụ thể.","anchor":"ID-cấu-trúc-nếu-có"}
```

`anchor` tùy chọn, trỏ tới ID thực trong `04-common-structures/common-structures.html`. Dùng dấu backtick trong usage/contrast để đánh dấu cụm tiếng Anh. Không chèn HTML vào dữ liệu; bộ sinh escape nội dung. Chọn emoji/hình gợi nhớ hợp ngữ cảnh, giữ giao diện hiện có.


## Chọn sổ và tránh trùng (áp dụng cho mọi lần thêm từ)

1. Chuẩn hóa dạng từ điển; tìm mục từ trong sổ thông dụng trước, gồm biến thể chính tả và tên tương đương.
2. Nếu đúng từ, nghĩa và loại từ đã được bao phủ, giữ mục hiện có; không chép lại sang sổ ít thông dụng. Từ chỉ xuất hiện trong ví dụ không tính là một mục đã có.
3. Nếu chưa có, tìm tiếp trong less-common-words.json. Nếu đã có đúng mục thì không thêm; nếu chưa có thì lưu ở đây, kể cả dạng N/Adj/Adv/V cùng họ với từ ở sổ thông dụng. Không suy ra hai dạng cùng họ là một headword; cũng không tự tạo mục chỉ vì viết hoa, số nhiều hoặc chia thì.
4. Với cùng một headword nhưng loại từ/nghĩa mới chưa được bao phủ: bổ sung vào mục của sổ ít thông dụng nếu đã có, hoặc tạo một mục tại sổ ít thông dụng, ghi rõ khác biệt với mục thông dụng. Không nhân đôi cùng headword + loại từ + nghĩa. Nếu cần nhiều cách phát âm, đồng bộ variant đúng loại từ.
5. Báo rõ từ mới ở sổ ít thông dụng và từ đã có ở sổ nào. Không tự di chuyển các mục cũ. Tên “ít thông dụng” là nhóm học mở rộng theo yêu cầu, không phải khẳng định tần suất đã được đo.

Chạy builder mặc định để sinh cả hai sổ, sổ cụm từ và flashcards chung. Sổ ít thông dụng có tiến độ học riêng; giữ ID ổn định cho liên kết từ cụm từ và cấu trúc. Không tự thêm từ minh họa để lấp sổ trống.

## Nội dung học

Mỗi từ có loại từ cạnh headword, nghĩa Việt phổ biến, cách sử dụng chính, phân biệt từ dễ nhầm, một câu tiếng Anh tự nhiên và bản dịch. Giải thích bằng tình huống hoặc khác biệt về sắc thái/ngữ pháp; tránh ghi chung chung “tùy ngữ cảnh”. Không ép hai từ đồng nghĩa thành hai nghĩa loại trừ nếu thực tế chúng có thể thay thế nhau.

Viết mỗi mục từ và cụm từ cho người mới mở sổ lần đầu. Dòng nghĩa phải tự hiểu được bằng tiếng Việt quen thuộc. Nêu nghĩa chính đang học, bỏ cách nói riêng của video hoặc ý phụ khiến người đọc phải đoán, chẳng hạn “đoạn khoe”. Với `grocery haul`, ghi “những món vừa mua trong một lần đi chợ hoặc siêu thị”. Đọc riêng dòng nghĩa để kiểm tra người học có hiểu ngay từ hoặc cụm đang chỉ gì không.

Ảnh và video chỉ cung cấp ví dụ cho mục đang học. Cách dùng, bản dịch và phần phân biệt phải đủ nghĩa khi mở riêng thẻ. Nếu một ví dụ phụ thuộc vào lời nói trước đó, giải thích phần cần thiết ngay cạnh ví dụ khi câu hiện tại có đủ căn cứ, hoặc dùng câu tự soạn độc lập. Giữ thông tin xuất xứ trong nhãn nguồn. Bỏ các nhận xét chỉ kể lại chuyện trong video mà không làm rõ cách dùng từ.

Ưu tiên nghĩa và dạng thông dụng. Chỉ thêm họ từ/cụm khi hữu ích cho yêu cầu; mỗi dạng được chọn cũng cần nghĩa và ví dụ. Không tự thêm bài tập, lịch học hay danh sách collocation dài. Không dùng phiên âm kiểu tiếng Việt.

Tra nguồn từ điển chính thức khi cần xác minh nghĩa, giới từ, mức độ trang trọng hoặc sự khác biệt tinh tế. Không coi lỗi OCR/PDF là nghĩa chuẩn. Giữ số thứ tự gốc và ghi chú sửa lỗi khi có ích. Ví dụ tự soạn phải được ghi là tự soạn (bộ sinh hiện có nhãn này); không gán câu tự viết cho Oxford. Nếu dùng câu nguồn, xác minh và lưu link trực tiếp cùng nhãn trích/phỏng theo chính xác, sửa bộ sinh khi cần thay vì giữ nhãn sai.

Cấu trúc đáng học liên kết trực tiếp tới anchor dùng chung. Nếu chưa có, đọc `04-common-structures/skill.md` và thêm tại đó; giữ ghi chú trong thẻ từ ngắn gọn. Bài grammar dựa trên sách tuân thủ `03-grammar/skill.md`, không tự tạo bài grammar khi chỉ thêm từ.

## IPA và audio

`oxford-pronunciations.json` chứa metadata theo ID: headword, variants với word/pos/source, us/uk gồm audio và ipa. Lấy IPA và URL MP3 từ trang Oxford thực tế bằng helper `lookup` trong `sync-oxford.cjs`; tuyệt đối không đoán URL audio. Kiểm tra đúng headword, từ loại, biến thể US/UK, đặc biệt với live, record, present, use… Không lấy cách đọc từ đồng tự khác nghĩa để lấp chỗ thiếu.

Với từ mới trong `less-common-words.json` (hoặc chỉnh sửa mục cũ trong `additional-words.json`), chạy `node 01-vocabulary/sync-additional-oxford.cjs` (cần mạng). `--refresh` chỉ khi cần làm mới metadata. Nếu từ có nhiều cách đọc theo từ loại, tra và bổ sung các variant được xác minh. Các script sync bộ PDF dành cho bảo trì toàn bộ, không cần chạy lại khi chỉ thêm một từ.

Chỉ ghi Oxford khi có nguồn xác minh. Khi không có bản thu đúng cả cụm, dùng nhãn “Máy đọc”; không giả bản thu của một từ thành bản thu cả câu. Câu ví dụ dùng speechSynthesis US/UK, không nhận là giọng Oxford. Nếu bổ sung IPA câu, ghi rõ là phiên âm minh họa tự soạn, không bịa nguồn.

Khi từ mới chưa có metadata Oxford hoặc cụm có IPA nguyên cụm trên trang từ điển, có thể lưu IPA US/UK trực tiếp vào `us` và `uk` (không kèm dấu /) cùng `ipaSource` trỏ tới trang Oxford hoặc Cambridge đã đối chiếu. Bộ sinh hiển thị liên kết “Nguồn IPA” cạnh hàng phát âm và giữ nhãn “Máy đọc” nếu nút dùng giọng tổng hợp. Nếu chỉ đối chiếu được cách đọc từng từ trong cụm, ghi IPA tham khảo và để trống `ipaSource`.

Giữ nút US/UK, IPA sát nút tương ứng, link nguồn, tốc độ 1, aria-label/aria-pressed và thông báo lỗi. Bấm lại để dừng; đổi mục/giọng phải dừng cả audio và speech trước đó. Không âm thầm chuyển sang máy đọc khi bản thu lỗi; không dùng Google Dịch. Giữ trạng thái tìm kiếm, mục lục và tiến độ đã nhớ.

## Lưu và kiểm tra

Với một lô lấy từ video, ảnh hoặc tài liệu, rà từng mục mới và từng mục vừa sửa trước khi build. Đọc dòng nghĩa như thể chưa xem nguồn, rồi đọc ví dụ, bản dịch và cách dùng ngay trong thẻ. Nếu phải nhớ một câu khác, đoán từ chỉ người/vật, hoặc hiểu tiếng lóng tiếng Việt mới nắm được ý, hãy viết lại ngay. Kiểm tra toàn bộ lô, kể cả các mục không được nêu đích danh khi người dùng báo một lỗi có tính lặp lại. Sau khi build, đối chiếu các mục đã sửa trong HTML sinh ra.

Lưu dữ liệu rồi chạy builder sau mỗi lô nhỏ khi thêm nhiều từ; không chờ viết hết mới lưu HTML. Đảm bảo header, nhóm điều hướng và số mục phản ánh cả từ thêm ngoài PDF.

Chạy `node 01-vocabulary/validate-notebook.cjs --complete`: ID duy nhất, đủ nội dung, JavaScript hợp lệ, liên kết anchor tồn tại. Với audio mới chạy `node 01-vocabulary/verify-oxford-audio.cjs`; cache tránh kiểm tra lại URL đã đạt. Kiểm tra mục vừa thêm trong trình duyệt khi có công cụ, gồm tìm kiếm và liên kết cấu trúc. Không nói đã nghe audio nếu chỉ kiểm tra HTTP.

Giao link tuyệt đối tới HTML và nêu từ đã thêm; nhắc tải lại trang khi cần. Không tự commit/publish hoặc sửa phần học khác ngoài phạm vi cần thiết.

## Sổ cụm từ

Khi người dùng cung cấp cụm từ, lưu cả cụm vào `phrases.json`, sinh `english-phrases.html` theo cùng template vocab bằng `node 01-vocabulary/build-vocabulary.cjs` (tự build cả hai sổ). Không sửa HTML sinh ra trực tiếp. Mỗi cụm có ID ổn định, word/pos/meaning/image/example/translation/usage/contrast, source là link từ điển đã xác minh, us/uk là IPA nguyên cụm từ từ điển hoặc IPA tham khảo được ghi nhãn như trên, components là mảng {word,id} trỏ tới mục vocab. Builder tự chọn common-vocabulary.html hoặc less-common-vocabulary.html theo ID. Nếu ví dụ tự soạn dùng nhãn tự soạn; nếu phỏng theo nguồn phải nêu đúng nguồn trong dữ liệu và giao diện. Tách tất cả từ trong cụm, chuẩn hóa và tìm trong toàn bộ dữ liệu vocab; chỉ thêm từ còn thiếu vào less-common-words.json rồi sync Oxford. Giữ nguyên nghĩa thành ngữ của cả cụm, không suy nghĩa máy móc từ từng từ. Cụm dùng Máy đọc US/UK khi chưa có bản thu cả cụm xác minh. Các từ đã có chỉ liên kết, không tạo trùng. Thêm điều hướng hai chiều, tìm kiếm, đánh dấu đã nhớ riêng cho sổ cụm từ. Báo cụm vừa thêm, từ mới và từ đã có.
