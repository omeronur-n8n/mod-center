// Gelisio logosundan Android (klasik + adaptif) ve web ikonlarını üretir.
// Kullanım: node tool/render_icons.js  (playwright gerekir)
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
const R = path.resolve(__dirname, '..');
const defs = `<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1A1F2E"/><stop offset="1" stop-color="#07090F"/></linearGradient>
<linearGradient id="ring" gradientUnits="userSpaceOnUse" x1="124" y1="124" x2="388" y2="388"><stop offset="0" stop-color="#7CF5D2"/><stop offset="1" stop-color="#8B7CFF"/></linearGradient>
<linearGradient id="gloss" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF" stop-opacity="0.16"/><stop offset="0.5" stop-color="#FFF" stop-opacity="0"/></linearGradient></defs>`;
const mark = `<path d="M 362 178 A 132 132 0 1 0 388 256" fill="none" stroke="url(#ring)" stroke-width="44" stroke-linecap="round"/><path d="M 388 256 L 284 256" fill="none" stroke="url(#ring)" stroke-width="44" stroke-linecap="round"/><circle cx="392" cy="150" r="20" fill="#7CF5D2"/>`;
// scale: işaretin tuval içindeki ölçeği (1 = 512 karede orijinal)
const svg = ({ bg, rounded, scale }) => {
  const t = 256 - 256 * scale;
  const back = !bg ? '' : rounded
    ? `<rect width="512" height="512" rx="116" fill="url(#bg)"/><rect x="6" y="6" width="500" height="500" rx="110" fill="none" stroke="#FFF" stroke-opacity="0.10" stroke-width="3"/><rect width="512" height="512" rx="116" fill="url(#gloss)"/>`
    : `<rect width="512" height="512" fill="url(#bg)"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${defs}${back}<g transform="translate(${t} ${t}) scale(${scale})">${mark}</g></svg>`;
};
const jobs = [];
const add = (file, size, opt) => jobs.push({ file: path.join(R, file), size, opt });
// Android klasik (API < 26)
for (const [d, s] of [['mdpi', 48], ['hdpi', 72], ['xhdpi', 96], ['xxhdpi', 144], ['xxxhdpi', 192]]) {
  add(`app/src/main/res/mipmap-${d}/ic_launcher.png`, s, { bg: true, rounded: true, scale: 1 });
}
// Android adaptif ön plan (108dp tuval, güvenli alan 66dp → işaret ~%58)
for (const [d, s] of [['mdpi', 108], ['hdpi', 162], ['xhdpi', 216], ['xxhdpi', 324], ['xxxhdpi', 432]]) {
  add(`app/src/main/res/mipmap-${d}/ic_launcher_foreground.png`, s, { bg: false, scale: 0.58 });
}
// Web
// Uygulama içi
(async () => {
  const b = await chromium.launch();
  for (const j of jobs) {
    fs.mkdirSync(path.dirname(j.file), { recursive: true });
    const p = await b.newPage({ viewport: { width: j.size, height: j.size } });
    await p.setContent(`<html><body style="margin:0;background:transparent">${svg(j.opt).replace('<svg ', `<svg width="${j.size}" height="${j.size}" `)}</body></html>`);
    await p.screenshot({ path: j.file, omitBackground: true, clip: { x: 0, y: 0, width: j.size, height: j.size } });
    await p.close();
  }
  await b.close();
  console.log(jobs.length + ' ikon üretildi');
})();
