// Word Garden — Scriptable. Chạy trong app để cài đặt; chọn script này cho widget.
// Từ vựng nhúng offline. Không thay hình nền, không gửi dữ liệu học lên máy chủ.
const WORDS=/*WORDS*/[];
const FIXED_PANEL='';
const fm=FileManager.local();
const settingsPath=fm.joinPath(fm.documentsDirectory(),'wordgarden-widget-settings-v1.json');
function shuffledIDs(){const ids=WORDS.map(w=>w.id);for(let i=ids.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[ids[i],ids[j]]=[ids[j],ids[i]]}return ids}
function validMinutes(n){return Number.isInteger(n)&&n>=1&&n<=1440}
function loadSettings(){let s;try{s=JSON.parse(fm.readString(settingsPath))}catch{}
 if(!s||!validMinutes(s.minutes)||!Number.isFinite(s.epoch)||!Number.isInteger(s.base)||!Array.isArray(s.order)){s={minutes:30,epoch:Date.now(),base:0,order:shuffledIDs(),showMeaning:true};fm.writeString(settingsPath,JSON.stringify(s));return s}
 const valid=new Set(WORDS.map(w=>w.id)),seen=new Set(),order=s.order.filter(id=>{if(!valid.has(id)||seen.has(id))return false;seen.add(id);return true});
 for(const id of WORDS.map(w=>w.id))if(!seen.has(id))order.push(id);
 if(order.length!==s.order.length||order.some((id,i)=>id!==s.order[i])){s.order=order;fm.writeString(settingsPath,JSON.stringify(s))}return s;
}
let settings=loadSettings();
function position(now=Date.now()){return settings.base+Math.max(0,Math.floor((now-settings.epoch)/(settings.minutes*60000)))}
function currentWord(now=Date.now()){const id=settings.order[position(now)%settings.order.length];return WORDS.find(w=>w.id===id)||WORDS[0]}
function nextDate(now=Date.now()){const interval=settings.minutes*60000;return new Date(settings.epoch+(Math.max(0,Math.floor((now-settings.epoch)/interval))+1)*interval)}
function saveSettings(){fm.writeString(settingsPath,JSON.stringify(settings))}
function runURL(id){const base=URLScheme.forRunningScript();return base+(base.includes('?')?'&':'?')+'action=detail&wordID='+encodeURIComponent(id)}
function makeWidget(){const word=currentWord(),w=new ListWidget();w.url=runURL(word.id);w.refreshAfterDate=nextDate();
 const family=config.widgetFamily||'accessoryRectangular',accessory=family.startsWith('accessory'),panel=FIXED_PANEL||String(args.widgetParameter||'').trim().toLowerCase(),large=settings.largeText!==false;
 if(family==='accessoryInline'){w.addText(word.word+(settings.showMeaning?' · '+word.meaning:''));return w}
 if(accessory){w.addAccessoryWidgetBackground=false;w.setPadding(0,1,0,1)}
 if(accessory&&panel==='meaning'){const meaning=w.addText(word.meaning);meaning.font=Font.boldSystemFont(22);meaning.lineLimit=2;meaning.minimumScaleFactor=0.6;return w}
 if(!accessory){w.backgroundColor=new Color('F3F4EA');w.setPadding(14,16,14,16);const label=w.addText('WORD GARDEN  ·  '+settings.minutes+' phút');label.font=Font.mediumSystemFont(10);label.textColor=new Color('66805D');w.addSpacer(8)}
 const title=w.addText(word.word);title.font=Font.boldSystemFont(accessory?(panel==='english'?30:(large?24:20)):32);title.lineLimit=accessory?1:2;title.minimumScaleFactor=0.65;
 if(!accessory)title.textColor=new Color('254A36');w.addSpacer(accessory?1:5);
 const locale=settings.pronunciation==='uk'?'uk':'us',p=(word.pronunciations||[]).find(p=>p[locale]?.ipa);
 if(p){const label=p.word.toLowerCase()===word.word.toLowerCase()?'':p.word+' ';const phon=w.addText(locale.toUpperCase()+' '+label+p[locale].ipa);phon.font=Font.systemFont(accessory?(panel==='english'?14:12):15);phon.lineLimit=1;phon.minimumScaleFactor=0.65;if(!accessory)phon.textColor=new Color('63805D');w.addSpacer(accessory?1:4)}
 if(settings.showMeaning&&panel!=='english'){const meaning=w.addText(accessory?word.meaning.split(/[;；]/)[0]:word.meaning);meaning.font=Font.systemFont(accessory?12:15);meaning.lineLimit=accessory?1:3;meaning.minimumScaleFactor=0.75;if(!accessory)meaning.textColor=new Color('4F6655')}
 if(!accessory&&family!=='small'){w.addSpacer(7);const ex=w.addText(word.example);ex.font=Font.systemFont(12);ex.lineLimit=2;ex.textColor=new Color('6D7E6B')}
 return w;
}
const escapeHTML=s=>String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const DETAIL_TEMPLATE=/*DETAIL_TEMPLATE*/'';
function detailHTML(word){return DETAIL_TEMPLATE.replace('__WORD_JSON__',()=>JSON.stringify({...word,nativeBridge:true}).replace(/</g,'\\u003c'))}
function advanceWord(){settings.base=position()+1;settings.epoch=Date.now();saveSettings();return currentWord()}
async function showDetail(word){const view=new WebView();let changing=false;view.shouldAllowRequest=request=>{if(request.url!=='wordgarden-next://change')return true;if(changing)return false;changing=true;const next=advanceWord();view.loadHTML(detailHTML(next)).then(()=>{changing=false}).catch(async()=>{changing=false;await view.evaluateJavaScript("document.querySelector('#next-word').disabled=false;document.querySelector('#status').textContent='Không mở được từ tiếp theo. Hãy đóng và mở lại trang.'")});return false};await view.loadHTML(detailHTML(word));await view.present();}
async function changeMinutes(){while(true){const a=new Alert();a.title='Chu kỳ đổi từ';a.message='Nhập 1–1440 phút. iOS có thể cập nhật chậm hơn, đặc biệt khi pin yếu. 15–60 phút thường thực tế hơn; đây không phải bộ hẹn giờ chính xác.';a.addTextField('Số phút',String(settings.minutes));a.addAction('Lưu');a.addCancelAction('Hủy');if(await a.presentAlert()===-1)return;const n=Number(a.textFieldValue(0));if(!validMinutes(n))continue;settings.base=position();settings.epoch=Date.now();settings.minutes=n;saveSettings();const done=new Alert();done.title='Đã lưu '+n+' phút';done.message='Widget sẽ dùng cấu hình này ở lần chạy tiếp theo. Nếu chưa đổi, chạm widget hoặc sửa chọn lại script; iOS không bảo đảm làm mới ngay.';done.addAction('OK');await done.presentAlert();return}}
async function appMenu(){const selected=Number((args.queryParameters||{}).wordID);if((args.queryParameters||{}).action==='detail'){await showDetail(WORDS.find(w=>w.id===selected)||currentWord());return}
 while(true){const a=new Alert();a.title='Word Garden ✿';a.message=WORDS.length+' từ · '+settings.minutes+' phút/từ (mốc mong muốn)\nHiện tại: '+currentWord().word+'\nKhông lặp trong một vòng '+WORDS.length+' từ; khi hết bộ sẽ quay vòng. iOS có thể bỏ qua mốc khi widget chưa được làm mới.';a.addAction('Xem từ hiện tại');a.addAction('Cài số phút đổi từ');a.addAction(settings.showMeaning?'Ẩn nghĩa trên widget':'Hiện nghĩa trên widget');a.addAction('Xem trước widget màn hình khóa');a.addAction('Chuyển sang từ kế tiếp');a.addAction('IPA trên widget: '+(settings.pronunciation==='uk'?'UK':'US')+' — chạm để đổi');a.addAction(settings.largeText!==false?'Chữ widget: Lớn — đổi sang Gọn':'Chữ widget: Gọn — đổi sang Lớn');a.addCancelAction('Đóng');const choice=await a.presentSheet();if(choice===-1)return;if(choice===0)await showDetail(currentWord());if(choice===1)await changeMinutes();if(choice===2){settings.showMeaning=!settings.showMeaning;saveSettings()}if(choice===3)await makeWidget().presentAccessoryRectangular();if(choice===6){settings.largeText=settings.largeText===false;saveSettings()}if(choice===5){settings.pronunciation=settings.pronunciation==='uk'?'us':'uk';saveSettings()}if(choice===4){await showDetail(advanceWord())}}
}
async function main(){if(!WORDS.length)throw Error('Đây là file mẫu. Hãy dùng Word Garden.js trong thư mục package.');if(config.runsInWidget)Script.setWidget(makeWidget());else await appMenu();Script.complete()}
await main();
