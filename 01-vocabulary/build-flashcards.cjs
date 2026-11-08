const fs = require('fs');
const path = require('path');
const vm = require('vm');

const notebooks = [
  ['common-vocabulary.html', 'word'],
  ['less-common-vocabulary.html', 'word'],
  ['english-phrases.html', 'phrase']
];
const cards = new Map();
for (const [notebook, kind] of notebooks) {
  const source = fs.readFileSync(path.join(__dirname, notebook), 'utf8');
  for (const [article, id] of source.matchAll(/<article class="word-card" id="word-(\d+)"[\s\S]*?<\/article>/g)) {
    const heading = article.match(/<h2>([\s\S]*?) <span class="pos">([\s\S]*?)<\/span><\/h2>/);
    if (!heading) throw Error(`Missing heading in ${notebook}#word-${id}`);
    const [, word, pos] = heading;
    const key = word.toLowerCase().trim();
    if (cards.has(key)) continue;
    const back = article.replace(/<article[^>]*>/, '').replace(/<\/article>$/, '')
      .replace(/<button class="learn"[\s\S]*?<\/button>/, '');
    cards.set(key, { id: `${kind}-${id}`, href: `${notebook}#word-${id}`, word, pos, back });
  }
}

const data = JSON.stringify([...cards.values()]).replace(/</g, '\\u003c');
const template = fs.readFileSync(path.join(__dirname, 'vocabulary-flashcards.html.in'), 'utf8');
const placeholder = '/*VOCABULARY_DATA*/[]';
if (!template.includes(placeholder)) throw Error('Missing data placeholder');
const html = template.replace(placeholder, () => data);
for (const [, script] of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) new vm.Script(script);
fs.writeFileSync(path.join(__dirname, 'vocabulary-flashcards.html'), html);
console.log(`Flashcards: ${cards.size} distinct words and phrases embedded`);
