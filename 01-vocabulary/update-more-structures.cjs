const fs=require('fs'),path=require('path');const file=path.join(__dirname,'../04-common-structures/common-structures.html');let html=fs.readFileSync(file,'utf8');const groups=[
['more-used-to','Used to, be used to và get used to',3215,'Đã từng có thói quen/trạng thái: used to. Đã quen: be used to. Đang dần quen: get used to.',[
['Thói quen trong quá khứ','S + used to + V','I used to walk to school.','Trước đây tôi thường đi bộ đến trường.','Used to nói thói quen trước đây, thường đối chiếu với hiện tại đã khác. Sau to dùng walk nguyên mẫu.'],
['Trạng thái đã quen','S + be + used to + N / V-ing','I am used to walking to school.','Tôi đã quen đi bộ đến trường.','To ở đây là giới từ, nên theo sau là walking. Câu nói đã quen, không kể riêng thói quen đã chấm dứt.'],
['Quá trình dần quen','S + get(s)/got + used to + N / V-ing','I am getting used to walking to school.','Tôi đang dần quen đi bộ đến trường.','Getting diễn tả quá trình thích nghi; am used to là trạng thái đã quen.'],
['Phủ định thói quen cũ','S + did not + use to + V','I did not use to walk to school.','Trước đây tôi không thường đi bộ đến trường.','Sau did not, dùng use theo mẫu chuẩn thường dạy. Không giữ used trong mẫu này.']]],
['more-suggest-recommend','Suggest và recommend: đề xuất làm gì',2907,'Đề xuất một hành động: suggest/recommend + V-ing. Nêu ai nên làm: dùng một mệnh đề có chủ ngữ riêng.',[
['Suggest doing','S + suggest(s)/suggested + V-ing','I suggest taking the bus.','Tôi đề xuất đi xe buýt.','Taking là hành động được đề xuất; không dùng suggest to take trong mẫu này.'],
['Suggest that','S + suggest(s)/suggested + (that) + S + (should) + V','I suggest that we take the bus.','Tôi đề xuất chúng ta đi xe buýt.','We là người thực hiện. Có thể dùng we should take; không đổi thành suggest us to take.'],
['Recommend doing','S + recommend(s)/recommended + V-ing','I recommend booking the tickets early.','Tôi khuyên nên đặt vé sớm.','Recommend thường gợi lựa chọn tốt dựa trên đánh giá/trải nghiệm; cấu trúc doing phù hợp khi không nêu người riêng.'],
['Recommend that','S + recommend(s)/recommended + (that) + S + (should) + V','I recommend that you book the tickets early.','Tôi khuyên bạn đặt vé sớm.','You là người nên đặt; không cần thêm s vào book trong mẫu đề xuất này.']]],
['more-remember-stop','Remember, forget và stop: đổi dạng, đổi nghĩa',2451,'Nhớ phải làm: remember to. Nhớ đã làm: remember doing. Ngừng một việc: stop doing. Dừng việc khác để làm: stop to.',[
['Nhớ thực hiện','S + remember(s)/remembered + to + V','I remembered to lock the door.','Tôi đã nhớ khóa cửa.','Việc khóa cửa là nhiệm vụ cần nhớ thực hiện; câu khẳng định tôi đã làm.'],
['Nhớ trải nghiệm đã có','S + remember(s)/remembered + V-ing','I remember locking the door.','Tôi nhớ mình đã khóa cửa.','Tôi nhớ lại hành động đã diễn ra, khác với nhớ phải thực hiện.'],
['Quên làm','S + forget(s)/forgot + to + V','I forgot to lock the door.','Tôi quên khóa cửa.','Việc cần làm bị bỏ sót; không phải đã khóa rồi quên mất ký ức.'],
['Ngừng hành động','S + stop(s)/stopped + V-ing','We stopped talking.','Chúng tôi ngừng nói chuyện.','Talking là việc kết thúc.'],
['Dừng để làm việc khác','S + stop(s)/stopped + to + V','We stopped to talk.','Chúng tôi dừng lại để nói chuyện.','Việc dừng có mục đích là nói chuyện; không có nghĩa đã ngừng nói.']]],
['more-too-enough','Too và enough: quá mức hay đủ?',1017,'Quá mức nên khó làm: too + tính từ. Đủ mức: tính từ + enough. Đủ số lượng: enough + danh từ.',[
['Too + tính từ','S + be + too + adjective + to + V','The box is too heavy to carry.','Chiếc hộp quá nặng để mang.','Too nhấn vượt mức phù hợp, không chỉ rất nặng.'],
['Tính từ + enough','S + be + adjective + enough + to + V','The box is light enough to carry.','Chiếc hộp đủ nhẹ để mang.','Enough đứng sau light. Đổi too heavy thành light enough cũng đổi thông tin về khả năng mang.'],
['Enough + danh từ','S + have/has + enough + N + to + V','We have enough time to finish.','Chúng tôi có đủ thời gian để làm xong.','Enough đứng trước danh từ time, không sau như với tính từ.'],
['Not enough','S + do/does not + have + enough + N','We do not have enough time.','Chúng tôi không có đủ thời gian.','Phủ định sự đủ. Với số nhiều có thể nói too few; với không đếm được có thể nói too little.']]],
['more-prefer','Prefer và would rather: thích lựa chọn nào hơn?',2248,'Nêu sở thích giữa hai thứ/hành động: prefer. Chọn việc muốn làm trong tình huống này: would prefer hoặc would rather.',[
['Prefer A to B','S + prefer(s) + N / V-ing + to + N / V-ing','I prefer walking to driving.','Tôi thích đi bộ hơn lái xe.','To là giới từ so sánh; hai vế walking và driving có cùng dạng.'],
['Prefer to','S + prefer(s) + to + V','I prefer to walk in the morning.','Tôi thích đi bộ vào buổi sáng.','Không nêu lựa chọn thứ hai vẫn dùng được prefer to.'],
['Would prefer to','S + would prefer + to + V','I would prefer to walk today.','Hôm nay tôi muốn đi bộ hơn.','Thường nói lựa chọn cụ thể và mềm hơn trong hội thoại.'],
['Would rather','S + would rather + V + than + V','I would rather walk than drive today.','Hôm nay tôi muốn đi bộ hơn lái xe.','Sau would rather dùng walk trực tiếp, không có to; sau than giữ dạng tương ứng.']]],
['more-spend-take','Spend, take, pay và cost: ai dùng thời gian/tiền?',2798,'Người dành thời gian: spend. Một việc mất thời gian: take. Người trả tiền: pay. Món đồ có giá: cost.',[
['Spend time doing','S + spend(s)/spent + time + V-ing','I spent two hours reading.','Tôi dành hai giờ đọc sách.','I là người dùng thời gian; sau cụm thời gian là reading.'],
['It takes someone time to','It + take(s)/took + O + time + to + V','It took me two hours to read the report.','Tôi mất hai giờ để đọc báo cáo.','Cấu trúc đổi chủ ngữ thành it; me chỉ người mất thời gian.'],
['Pay for','S + pay(s)/paid + money + for + N','I paid ten dollars for the book.','Tôi trả mười đô la cho cuốn sách.','I là người trả tiền; for giới thiệu món mua.'],
['Cost someone money','N + cost(s) + O + money','The book cost me ten dollars.','Cuốn sách có giá mười đô la đối với tôi.','Món đồ làm chủ ngữ. Cost trong câu quá khứ vẫn có dạng cost.']]],
['more-prevent','Prevent/stop someone from doing: ngăn ai làm gì',2265,'Muốn ngăn một hành động xảy ra: prevent hoặc stop + người + from + V-ing. Muốn bản thân tránh làm: avoid + V-ing.',[
['Prevent someone from doing','S + prevent(s)/prevented + O + from + V-ing','The rain prevented us from leaving.','Mưa ngăn chúng tôi rời đi.','Us là người không thực hiện được việc đi; leaving theo sau from.'],
['Stop someone from doing','S + stop(s)/stopped + O + from + V-ing','The guard stopped us from entering.','Người bảo vệ ngăn chúng tôi vào.','Đây là ngăn người khác làm; khác stop doing khi chủ ngữ tự ngừng hành động.'],
['Avoid doing','S + avoid(s) + V-ing','We avoid driving in heavy rain.','Chúng tôi tránh lái xe khi mưa to.','We tự lựa chọn tránh lái xe; không nhất thiết có ai/nguyên nhân trực tiếp ngăn cản.']]],
['more-provide','Provide và supply: cung cấp thứ gì cho ai?',2326,'Với provide, có thể đặt người nhận trước với with, hoặc đặt thứ được cung cấp trước với for. Supply thường có with hoặc to.',[
['Provide someone with something','S + provide(s)/provided + O + with + N','The school provides students with books.','Nhà trường cung cấp sách cho học sinh.','Students là người nhận; books là thứ được cung cấp.'],
['Provide something for someone','S + provide(s)/provided + N + for + O','The school provides books for students.','Nhà trường cung cấp sách cho học sinh.','Cùng tình huống nhưng đổi thứ tự và giới từ; không giữ with khi đặt books ngay sau provide.'],
['Supply something to someone','S + supply/supplies/supplied + N + to + O','The company supplies books to schools.','Công ty cung cấp sách cho các trường.','Supply hay gặp với nguồn cung hàng hóa/dịch vụ; provide có phạm vi rộng về việc đáp ứng nhu cầu.']]],
['more-look','Look after, look for và look forward to',1746,'Chăm sóc: look after. Tìm thứ chưa thấy: look for. Mong chờ với cảm xúc tích cực: look forward to.',[
['Look after','S + look(s)/looked + after + N','I look after my younger sister.','Tôi chăm sóc em gái.','After là một phần của cụm mang nghĩa chăm sóc, không dịch riêng là nhìn sau.'],
['Look for','S + look(s)/looked + for + N','I am looking for my keys.','Tôi đang tìm chìa khóa.','Đang tìm chưa đảm bảo tìm thấy. Find thường nói đã tìm thấy kết quả.'],
['Look forward to','S + look(s) + forward to + N / V-ing','I look forward to meeting you.','Tôi mong được gặp bạn.','To là giới từ trong cụm, nên dùng meeting; không dùng meet nguyên mẫu.']]],
['more-intention','Decide, plan, promise và refuse + to V',770,'Quyết định: decide to. Lên dự định: plan to. Hứa: promise to. Từ chối: refuse to. Dạng theo sau giống nhau nhưng ý nghĩa khác.',[
['Decide to','S + decide(s)/decided + to + V','She decided to study abroad.','Cô ấy quyết định đi du học.','Decided nêu lựa chọn đã chốt, không chỉ có ý định.'],
['Plan to','S + plan(s)/planned + to + V','She plans to study abroad next year.','Cô ấy dự định đi du học năm sau.','Plan nói dự định; chưa khẳng định đã thực hiện.'],
['Promise to','S + promise(s)/promised + to + V','He promised to call me.','Anh ấy hứa gọi cho tôi.','Người hứa và người sẽ gọi đều là he.'],
['Refuse to','S + refuse(s)/refused + to + V','He refused to sign the form.','Anh ấy từ chối ký mẫu đơn.','Refuse to không có nghĩa quên hoặc không đủ khả năng; đây là không chịu làm.']]],
['more-person-to','Expect, invite, persuade và remind + người + to V',1089,'Kỳ vọng ai làm: expect. Mời: invite. Thuyết phục: persuade. Nhắc: remind. Tân ngữ xác định người thực hiện hành động sau to.',[
['Expect someone to','S + expect(s) + O + to + V','I expect him to arrive soon.','Tôi nghĩ anh ấy sẽ đến sớm.','Him là người đến. Expect không nhất thiết là mong muốn của tôi, có thể là điều tôi dự đoán.'],
['Invite someone to','S + invite(s)/invited + O + to + V','They invited us to join them.','Họ mời chúng tôi tham gia cùng.','Us là người được mời và có thể tham gia.'],
['Persuade someone to','S + persuade(s)/persuaded + O + to + V','She persuaded me to try again.','Cô ấy thuyết phục tôi thử lại.','Persuaded thường cho thấy sự thuyết phục đã có tác dụng; tried to persuade chỉ nói đã cố thuyết phục.'],
['Remind someone to','S + remind(s)/reminded + O + to + V','Please remind me to call Mai.','Hãy nhắc tôi gọi Mai.','Me là người cần nhớ thực hiện. Remind khác remember: người nhắc và người nhớ có thể khác nhau.']]]
];
const esc=s=>s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
if(!html.includes('id="more-used-to"')){const sections=groups.map(([id,title,n,intro,patterns])=>`<section class="chapter" id="${id}"><div class="section-letter" aria-hidden="true">V</div><div class="section-body"><h2>${esc(title)}</h2><div class="note"><strong>Chọn nhanh:</strong> ${esc(intro)}</div><p>S = chủ ngữ; O = tân ngữ (người/vật nhận tác động); N = danh từ/cụm danh từ; V = động từ nguyên mẫu trong mẫu to/can hoặc động từ đã chia trong mệnh đề; V-ing = dạng thêm -ing. Be đổi theo chủ ngữ và thì.</p>${patterns.map(([title,formula,en,vi,why])=>`<div class="learning-point"><h3>${esc(title)}</h3><p class="formula" lang="en">${esc(formula)}</p><div class="example"><div class="sentence-pair"><p lang="en">${esc(en)}</p><p class="translation">${esc(vi)}</p></div><p class="explanation">${esc(why)}</p></div></div>`).join('')}</div><p class="back-to-lesson"><a href="../01-vocabulary/common-vocabulary.html#word-${n}">← Về từ vựng #${n}</a></p></section>`).join('\n');html=html.replace('</article>',sections+'</article>');html=html.replace('</nav>',`<div class="nav-formulas"><p class="nav-title">Từ vựng #501–3396</p>${groups.map(([id,t])=>`<a href="#${id}">${esc(t)}</a>`).join('')}</div></nav>`);html=html.replace('</template>',`<section><h2>Nguồn cấu trúc từ vựng mở rộng</h2><p>Ngày 01/11/2026. Tất cả ví dụ mới do biên soạn. Các nhóm hướng dẫn chọn theo tình huống; không coi từ gần nghĩa thay thế được trong mọi ngữ cảnh.</p><ul>${[
'https://learnenglish.britishcouncil.org/free-resources/grammar/b1-b2-grammar/different-uses-of-used-to',
'https://learnenglishteens.britishcouncil.org/grammar/a1-a2-grammar/verb-ing-or-verb-infinitive',
'https://dictionary.cambridge.org/grammar/british-grammar/suggest',
'https://dictionary.cambridge.org/grammar/british-grammar/too-enough',
'https://learnenglish.britishcouncil.org/free-resources/grammar/b1-b2/using-enough',
'https://dictionary.cambridge.org/grammar/british-grammar/prefer',
'https://dictionary.cambridge.org/dictionary/english/spend',
'https://dictionary.cambridge.org/dictionary/english/prevent',
'https://dictionary.cambridge.org/dictionary/english/provide'
].map(u=>`<li><a href="${u}">${u}</a></li>`).join('')}</ul></section></template>`);fs.writeFileSync(file,html);}
console.log('Added '+groups.length+' supporting structure groups.');
