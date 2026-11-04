// Rebuilds Resources/vocabulary.json from the notebook in ../01-vocabulary.
// Run after adding words:  node 06-wordgarden-ios/scripts/sync-vocabulary.cjs
const fs=require('fs'),path=require('path');
const vocab=path.resolve(__dirname,'../../01-vocabulary'),out=path.resolve(__dirname,'../Resources/vocabulary.json');
const read=f=>fs.readFileSync(path.join(vocab,f),'utf8');
const usage=new Map(read('usage-500.tsv').split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>{const[id,usage,contrast]=l.split('|');return[Number(id),{usage,contrast}]}));
const rows=read('words-500.tsv').split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map((l,i)=>{const[word,pos,meaning,,,image,example,translation]=l.split('|');return{id:i+1,word,pos,meaning,image,example,translation,...usage.get(i+1)}});
for(const f of['continuation-501-1500.jsonl','continuation-1501-2500.jsonl','continuation-2501-3396.jsonl'])rows.push(...read(f).trim().split(/\r?\n/).map(JSON.parse));
rows.push(...JSON.parse(read('additional-words.json')));
const dict=JSON.parse(read('oxford-pronunciations.json'));
const unique=new Map();
for(const r of rows){const key=r.word.toLowerCase().trim();if(unique.has(key))continue;
  unique.set(key,{id:r.id,word:r.word.trim(),pos:r.pos,meaning:r.meaning,image:r.image||'🌱',example:r.example,translation:r.translation,usage:r.usage,contrast:r.contrast,
    pronunciations:(dict.entries[r.id]?.variants||[]).map(p=>({word:p.word,pos:p.pos,source:p.source,us:p.us?.[0]||null,uk:p.uk?.[0]||null}))});}
const words=[...unique.values()];
for(const w of words)for(const k of['word','pos','meaning','example','translation','usage','contrast'])if(typeof w[k]!=='string'||!w[k].trim())throw Error(`Missing ${k} for #${w.id} ${w.word}`);
fs.writeFileSync(out,JSON.stringify(words,null,2)+'\n');
const noAudio=words.filter(w=>!w.pronunciations.some(p=>p.us||p.uk));
console.log(`Wrote ${words.length} words (last #${words.at(-1).id} ${words.at(-1).word}); ${noAudio.length} without Oxford audio → iPhone voice fallback.`);
if(noAudio.length)console.log('  e.g. '+noAudio.slice(0,15).map(w=>w.word).join(', '));
