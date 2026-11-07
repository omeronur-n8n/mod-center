"""Flaticon UIcons (Regular Rounded) web fontundan Gelisio'nun kullandığı ikonları
alt küme olarak TTF'ye çevirir ve lib/icons/gelisio_icons.dart dosyasını üretir.
Kullanım: python3 tool/build_icon_font.py <uicons paket klasörü>
Lisans: Uicons by Flaticon — ücretsiz kullanım atıf şartıyla (Hakkında ekranında)."""
import re, sys, pathlib
from fontTools.ttLib import TTFont
from fontTools import subset

ICONS = {  # Dart adı: UIcons adı
 'today':'home','week':'calendar','focus':'bullseye','growth':'seedling','me':'user',
 'done':'check-circle','streak':'flame','bell':'bell','morning':'sun','evening':'moon',
 'mission':'mountain','values':'compass-alt','review':'refresh','digitalDetox':'phone-slash',
 'add':'plus','project':'rocket','hobby':'palette','milestone':'flag','leadMeasure':'chart-line-up',
 'scoreboard':'chart-histogram','timer':'hourglass','ritual':'list-check','physical':'dumbbell-fitness',
 'mental':'brain','spiritual':'leaf','social':'users','quote':'quote-right','journal':'journal',
 'levelUp':'medal','settings':'settings','signOut':'sign-out-alt',
}
root = pathlib.Path(sys.argv[1]); repo = pathlib.Path(__file__).resolve().parents[1]
css = (root/'css/regular/rounded.css').read_text()
cps = {}
for dart, name in ICONS.items():
    m = re.search(r'\.fi-rr-%s:before\{content:"\\([0-9a-f]+)"\}' % re.escape(name), css)
    if not m: sys.exit(f'ikon bulunamadı: {name}')
    cps[dart] = int(m.group(1), 16)
src = next((root/'css').glob('uicons-regular-rounded-*.woff2'))
font = TTFont(src); font.flavor = None
opts = subset.Options(); opts.notdef_outline = True; opts.name_IDs = ['*']
sub = subset.Subsetter(opts); sub.populate(unicodes=list(cps.values())); sub.subset(font)
out = repo/'assets/fonts/GelisioIcons.ttf'; font.save(out)
lines = ["// Üretildi: tool/build_icon_font.py — elle düzenleme.",
         "// İkonlar: Uicons by Flaticon (Regular Rounded), atıfla ücretsiz lisans.",
         "import 'package:flutter/widgets.dart';", "",
         "class GelisioIcons {", "  GelisioIcons._();", "  static const _f = 'GelisioIcons';"]
for dart, cp in cps.items():
    lines.append(f"  static const IconData {dart} = IconData(0x{cp:x}, fontFamily: _f); // fi-rr-{ICONS[dart]}")
lines.append("}")
(repo/'lib/icons/gelisio_icons.dart').write_text("\n".join(lines) + "\n")
print(f'{len(cps)} ikon -> {out} ({out.stat().st_size} bayt)')
