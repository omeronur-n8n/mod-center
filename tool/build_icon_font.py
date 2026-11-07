"""Flaticon UIcons (Regular Rounded) web fontundan Gelisio'nun kullandığı ikonları
alt küme TTF'ye çevirir ve GelisioIcons.kt dosyasını üretir.
Kullanım: python3 tool/build_icon_font.py <@flaticon/flaticon-uicons paket klasörü>
Lisans: Uicons by Flaticon — ücretsiz kullanım atıf şartıyla (Ben → Hakkında)."""
import re, sys, pathlib
from fontTools.ttLib import TTFont
from fontTools import subset

ICONS = {  # Kotlin adı: UIcons adı
 'today':'home','week':'calendar','focus':'bullseye','growth':'seedling','me':'user',
 'done':'check-circle','streak':'flame','bell':'bell','morning':'sun','evening':'moon',
 'mission':'mountain','values':'compass-alt','review':'refresh','digitalDetox':'phone-slash',
 'add':'plus','project':'rocket','hobby':'palette','milestone':'flag','leadMeasure':'chart-line-up',
 'scoreboard':'chart-histogram','timer':'hourglass','ritual':'list-check','physical':'dumbbell-fitness',
 'mental':'brain','spiritual':'leaf','social':'users','quote':'quote-right','journal':'journal',
 'levelUp':'medal','settings':'settings','signOut':'sign-out-alt',
 # gezinme ve eylem ikonları
 'forward':'angle-small-right','back':'angle-small-left','down':'angle-small-down','close':'cross-small',
 'delete':'trash','edit':'pencil','play':'play','pause':'pause','stop':'stop','check':'check',
 'minus':'minus','clock':'clock',
}
root = pathlib.Path(sys.argv[1]); repo = pathlib.Path(__file__).resolve().parents[1]
css = (root/'css/regular/rounded.css').read_text()
cps = {}
for kt, name in ICONS.items():
    m = re.search(r'\.fi-rr-%s:before\{content:"\\([0-9a-f]+)"\}' % re.escape(name), css)
    if not m: sys.exit(f'ikon bulunamadı: {name}')
    cps[kt] = int(m.group(1), 16)
src = next((root/'css').glob('**/uicons-regular-rounded*.woff2'))
font = TTFont(src); font.flavor = None
opts = subset.Options(); opts.notdef_outline = True; opts.name_IDs = ['*']
sub = subset.Subsetter(opts); sub.populate(unicodes=list(cps.values())); sub.subset(font)
out = repo/'app/src/main/res/font/gelisio_icons.ttf'; font.save(out)
kt = ["// Üretildi: tool/build_icon_font.py — elle düzenleme.",
      "// İkonlar: Uicons by Flaticon (Regular Rounded), atıfla ücretsiz lisans.",
      "package com.omeronur.gelisio.ui.icons", "",
      "import androidx.compose.ui.text.font.Font", "import androidx.compose.ui.text.font.FontFamily",
      "import com.omeronur.gelisio.R", "",
      "val GelisioIconFont = FontFamily(Font(R.font.gelisio_icons))", "",
      "object GelisioIcons {"]
for name, cp in cps.items():
    kt.append(f"    const val {name} = '\\u{cp:04X}' // fi-rr-{ICONS[name]}")
kt.append("}")
(repo/'app/src/main/java/com/omeronur/gelisio/ui/icons/GelisioIcons.kt').write_text("\n".join(kt) + "\n")
print(f'{len(cps)} ikon -> {out} ({out.stat().st_size} bayt)')
