const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'..');
const words=JSON.parse(fs.readFileSync(path.join(root,'Resources/vocabulary.json'),'utf8'));
assert(words.length>=3396,'Missing full vocabulary');
assert.equal(new Set(words.map(w=>w.id)).size,words.length,'Duplicate IDs');
assert.equal(new Set(words.map(w=>w.word.toLowerCase().trim())).size,words.length,'Duplicate headwords');
for(const w of words){for(const k of ['word','pos','meaning','example','translation','usage','contrast'])assert(typeof w[k]==='string'&&w[k].trim(),`${w.id}: ${k}`);for(const p of w.pronunciations){assert(new URL(p.source).hostname.endsWith('oxfordlearnersdictionaries.com'));for(const loc of ['us','uk'])if(p[loc]){assert(p[loc].ipa);assert(new URL(p[loc].audio).protocol==='https:')}}}
for(const file of ['project.yml','Core/Package.swift','App/WordGardenApp.swift','App/NativeAudio.swift','Shared/Library.swift','Shared/WidgetIntents.swift','Widgets/WordGardenWidget.swift','Resources/Assets.xcassets/AppIcon.appiconset/Contents.json'])assert(fs.existsSync(path.join(root,file)),`Missing ${file}`);
const intents=fs.readFileSync(path.join(root,'Shared/WidgetIntents.swift'),'utf8');assert(intents.includes('struct NextWordIntent: AppIntent'));assert(intents.includes('struct PlayWordIntent: AudioPlaybackIntent'));assert(!fs.readFileSync(path.join(root,'project.yml'),'utf8').includes('application-groups'));
console.log(`Source validation passed: ${words.length} complete unique words. Swift/Xcode compilation runs on macOS CI.`);
