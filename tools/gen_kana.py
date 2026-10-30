"""Generates src/main/resources/assets/nihongo/curriculum/kana.json.

Kana content lives here (rather than hand-edited JSON) because the tables are regular.
Run: python3 tools/gen_kana.py
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nihongo", "curriculum", "kana.json")

HIRAGANA = [
    ("a", "あ", "A cross with a big swirling loop: an apple tree shaken by the wind. 'Ah!' an apple drops.", "あお (ao, blue)"),
    ("i", "い", "Two short strokes standing side by side, like two eels poking out of the sand: 'eee'.", "いえ (ie, house)"),
    ("u", "う", "A little dash over a hunched curve: someone bent over with a stomach ache going 'uuu...'.", "うし (ushi, cow)"),
    ("e", "え", "A bird with a feather sticking up on its head, turning to ask 'Eh?'", "え (e, picture)"),
    ("o", "お", "Like あ but with an extra dash flying off to the right: a golf ball sailing away. 'Oh!'", "おの (ono, axe)"),
    ("ka", "か", "A blade swinging next to a spark: 'Ka!' (か also means mosquito - swat!)", "かお (kao, face)"),
    ("ki", "き", "Looks like an old key with two teeth on top: ki = key.", "き (ki, tree)"),
    ("ku", "く", "An open beak calling out 'ku-ku!' like a cuckoo.", "くつ (kutsu, boots)"),
    ("ke", "け", "A keg standing up with a tap sticking out the side: ke(g).", "けん (ken, sword)"),
    ("ko", "こ", "Two worms curled up cosily next to each other: ko(zy).", "こえ (koe, voice)"),
    ("sa", "さ", "A sword (the cross) stuck in a curved hill: a samurai planted it there. sa(murai).", "さかな (sakana, fish)"),
    ("shi", "し", "A fishing hook: she's going fishing. shi.", "いし (ishi, stone)"),
    ("su", "す", "A rope with a loop for a seat: a swing. su(wing).", "すな (suna, sand)"),
    ("se", "せ", "A sailboat: the long line is the deck and the bent strokes are the sail. se(il).", "せきたん (sekitan, coal)"),
    ("so", "そ", "A zig-zag thread being sewn: so (sew).", "そら (sora, sky)"),
    ("ta", "た", "The left half looks like a 't', the right half like an 'a': ta.", "たけ (take, bamboo)"),
    ("chi", "ち", "Like the number 5 sitting down after eating too much cheese: chi(ese).", "つち (tsuchi, dirt)"),
    ("tsu", "つ", "A tsunami wave curling over: tsu.", "つき (tsuki, moon)"),
    ("te", "て", "An arm reaching out with a hand - and て literally means 'hand'.", "てつ (tetsu, iron)"),
    ("to", "と", "A toe with a thorn stuck in it: to(e).", "とり (tori, bird)"),
    ("na", "な", "A cross next to a knotted rope: na (a 'knot').", "なか (naka, inside)"),
    ("ni", "に", "A bent knee next to two lines: ni (knee).", "にく (niku, meat)"),
    ("nu", "ぬ", "A bowl of noodles with chopsticks; the loop at the end is a curl of noodle: nu(dle).", "いぬ (inu, dog)"),
    ("ne", "ね", "A cat curled up with its tail looped. ねこ (cat) starts with ね.", "ねこ (neko, cat)"),
    ("no", "の", "A big round 'no entry' sign with a slash: no!", "きのこ (kinoko, mushroom)"),
    ("ha", "は", "Someone laughing next to a pole: 'Ha!' (As a particle it's read 'wa'.)", "はな (hana, flower)"),
    ("hi", "ひ", "A big wide grin: 'hee hee!' hi.", "ひ (hi, fire)"),
    ("fu", "ふ", "Mount Fuji with clouds swirling around it: fu.", "ふね (fune, boat)"),
    ("he", "へ", "A little pointed hill: climb it and say 'hey!' (As a particle it's read 'e'.)", "へや (heya, room)"),
    ("ho", "ほ", "A Christmas tree next to a pole: 'Ho ho ho!'", "ほね (hone, bone)"),
    ("ma", "ま", "A mama with a long necklace tied in a loop: ma.", "まど (mado, window)"),
    ("mi", "み", "Looks like the number 21: 'me' at 21. mi.", "みず (mizu, water)"),
    ("mu", "む", "A cow's face with a horn on the left and a tail on the right: 'moo' - mu.", "むし (mushi, bug)"),
    ("me", "め", "An eye with a slash through it - and め means 'eye'!", "め (me, eye)"),
    ("mo", "も", "A fishing hook with more bait on it: mo(re).", "もり (mori, forest)"),
    ("ya", "や", "A yak with big horns: ya(k).", "やま (yama, mountain)"),
    ("yu", "ゆ", "A fish caught in a U-turn: yu (U).", "ゆき (yuki, snow)"),
    ("yo", "よ", "A yo-yo dangling from a string: yo.", "よる (yoru, night)"),
    ("ra", "ら", "A rabbit with one ear bent over: ra(bbit).", "そら (sora, sky)"),
    ("ri", "り", "Two reeds swaying by the river: ri(ver).", "りんご (ringo, apple)"),
    ("ru", "る", "A road ending in a loop - a roundabout: ru.", "よる (yoru, night)"),
    ("re", "れ", "A man kneeling to repent: re.", "これ (kore, this)"),
    ("ro", "ろ", "Like る without the loop - the road with no roundabout: ro.", "しろ (shiro, white)"),
    ("wa", "わ", "Like ね but with no loop at the tail: a wasp's tail. wa.", "わたし (watashi, I)"),
    ("wo", "を", "A man with a wobbly head: wo. Read 'o'; only used as the object particle.", "けん を つくります"),
    ("n", "ん", "Looks like a lowercase 'n'. The only kana that is just a consonant.", "ほん (hon, book)"),
]

KATAKANA = [
    ("a", "ア", "An axe swinging down: a(xe).", "アイテム (aitemu, item)"),
    ("i", "イ", "An easel standing on its own: i(sel).", "イカ"),
    ("u", "ウ", "う wearing a little roof: u.", "ウシ"),
    ("e", "エ", "An elevator shaft seen from the side: e.", "エンダーマン"),
    ("o", "オ", "An opera singer with arms spread wide: o.", "オノ"),
    ("ka", "カ", "Just like か without the spark: ka.", "カギ"),
    ("ki", "キ", "A key, like き: ki.", "キツネ"),
    ("ku", "ク", "A cook's knife slicing down: ku(ok).", "クモ"),
    ("ke", "ケ", "A K-shaped kebab skewer: ke(bab).", "ケーキ (keeki, cake)"),
    ("ko", "コ", "The corner of a box: ko(rner).", "コンパス"),
    ("sa", "サ", "A signpost with two posts: sa(ign).", "サバク"),
    ("shi", "シ", "A smiling face looking sideways: she smiles. Dots sit on the left, the long stroke sweeps UP.", "シャベル"),
    ("su", "ス", "A skier with a scarf blowing back: su(ki).", "スライム"),
    ("se", "セ", "Like せ, a sail: se.", "セーブ"),
    ("so", "ソ", "A needle sewing a stitch from above: so (sew). Strokes come DOWN from the top.", "ソード"),
    ("ta", "タ", "Like ク with an extra stroke - a table leg: ta(ble).", "タイマツ"),
    ("chi", "チ", "A cheerleader holding up a tall pom-pom: chi(er).", "チェスト"),
    ("tsu", "ツ", "A tsunami with spray on top. The dots sit across the TOP and the stroke comes down.", "ツルハシ"),
    ("te", "テ", "A telephone pole with wires: te(lephone).", "テーブル"),
    ("to", "ト", "A totem pole with one wing: to(tem).", "トロッコ"),
    ("na", "ナ", "A knife cutting a cross: na(ife).", "ナイフ"),
    ("ni", "ニ", "Two needles. It's also the kanji 二 (two, 'ni')!", "ニワトリ"),
    ("nu", "ヌ", "Chopsticks pinching a noodle: nu(dle).", "ヌードル"),
    ("ne", "ネ", "A necktie with a knot on top: ne(cktie).", "ネザー"),
    ("no", "ノ", "A single slash - just 'No.'", "ノート"),
    ("ha", "ハ", "Two strands of hair: ha(ir).", "ハチ"),
    ("hi", "ヒ", "A heel of a shoe: hi(el).", "ヒツジ"),
    ("fu", "フ", "ふ simplified into a hook: fu.", "ファントム"),
    ("he", "ヘ", "Identical to へ: he.", "ヘルメット"),
    ("ho", "ホ", "A holy cross with two dots: ho(ly).", "ホネ"),
    ("ma", "マ", "A mama's nose in profile: ma.", "マップ"),
    ("mi", "ミ", "Three marks - mi as in mittsu (three).", "ミルク"),
    ("mu", "ム", "A moose's face: mu.", "ムラ"),
    ("me", "メ", "X marks the spot: me (a treasure map).", "メロン"),
    ("mo", "モ", "Like も: more hooks. mo.", "モブ"),
    ("ya", "ヤ", "Like や: a yak. ya.", "ヤリ"),
    ("yu", "ユ", "A U-boat: yu.", "ユキ"),
    ("yo", "ヨ", "A folded yoga mat: yo(ga).", "ヨル"),
    ("ra", "ラ", "A rabbit with a flat hat: ra.", "ランタン"),
    ("ri", "リ", "Like り: reeds. ri.", "リンゴ"),
    ("ru", "ル", "Two roots growing: ru(ts).", "ルビー"),
    ("re", "レ", "A ray of light bouncing off the floor: re.", "レール"),
    ("ro", "ロ", "A square box of rope: ro(pe).", "ロープ"),
    ("wa", "ワ", "A wine glass: wa(ine).", "ワールド"),
    ("wo", "ヲ", "A wobbly-headed man again: wo. Almost never used.", ""),
    ("n", "ン", "Like ソ, but the long stroke sweeps UP like a smile: n.", "パン"),
]

ALT = {"shi": ["si"], "chi": ["ti"], "tsu": ["tu"], "fu": ["hu"], "wo": ["o"], "n": ["nn"]}


def single(prefix, script, row):
    romaji, kana, mnemonic, example = row
    return {
        "id": f"{prefix}_{romaji}",
        "script": script,
        "kana": kana,
        "romaji": romaji,
        "alt": ALT.get(romaji, []),
        "mnemonic": mnemonic,
        "example": example,
    }


def group(item_id, script, title, members, mnemonic):
    return {
        "id": item_id,
        "script": script,
        "title": title,
        "kana": members[0][0],
        "romaji": members[0][1],
        "group": [list(m) for m in members],
        "mnemonic": mnemonic,
    }


def rows(pairs):
    return [tuple(p.split(":")) for p in pairs.split()]


H_DAKUTEN = [
    group("h_ga", "hiragana", "が row (k → g)", rows("が:ga ぎ:gi ぐ:gu げ:ge ご:go"),
          "Two little dots (てんてん, dakuten) make a sound voiced: か ka → が ga. Your throat buzzes!"),
    group("h_za", "hiragana", "ざ row (s → z)", rows("ざ:za じ:ji ず:zu ぜ:ze ぞ:zo"),
          "Dakuten turns s into z: さ → ざ. Remember し shi becomes じ ji."),
    group("h_da", "hiragana", "だ row (t → d)", rows("だ:da ぢ:ji づ:zu で:de ど:do"),
          "Dakuten turns t into d: た → だ. ぢ and づ sound the same as じ and ず and are rare."),
    group("h_ba", "hiragana", "ば row (h → b)", rows("ば:ba び:bi ぶ:bu べ:be ぼ:bo"),
          "Dakuten turns h into b: は → ば."),
    group("h_pa", "hiragana", "ぱ row (h → p)", rows("ぱ:pa ぴ:pi ぷ:pu ぺ:pe ぽ:po"),
          "A small circle (まる, handakuten) turns h into p: は → ぱ. Think of the circle as lips popping: p!"),
]

H_COMBOS = [
    group("h_yoon1", "hiragana", "Combo sounds with き・し・ち",
          rows("きゃ:kya きゅ:kyu きょ:kyo しゃ:sha しゅ:shu しょ:sho ちゃ:cha ちゅ:chu ちょ:cho"),
          "A SMALL ゃ, ゅ or ょ glues onto an i-sound kana to make one beat: き + ゃ = きゃ (kya). Big や would be two beats (きや, ki-ya)."),
    group("h_yoon2", "hiragana", "Combo sounds with に・ひ・み・り",
          rows("にゃ:nya にゅ:nyu にょ:nyo ひゃ:hya ひゅ:hyu ひょ:hyo みゃ:mya みゅ:myu みょ:myo りゃ:rya りゅ:ryu りょ:ryo"),
          "Same rule: drop the i and add the small y-sound. ひ + ょ = ひょ (hyo). にゃ is how Japanese cats meow!"),
    group("h_yoon3", "hiragana", "Combo sounds with ぎ・じ・び・ぴ",
          rows("ぎゃ:gya ぎゅ:gyu ぎょ:gyo じゃ:ja じゅ:ju じょ:jo びゃ:bya びゅ:byu びょ:byo ぴゃ:pya ぴゅ:pyu ぴょ:pyo"),
          "Dakuten kana combine too: ぎ + ゃ = ぎゃ (gya), じ + ゃ = じゃ (ja)."),
    group("h_sokuon", "hiragana", "Small っ (double consonant)",
          rows("きって:kitte まって:matte いっしょ:issho ちょっと:chotto はっぱ:happa がっこう:gakkou"),
          "A small っ is a tiny pause: hold your mouth ready and double the next consonant. きって = ki-t-te."),
    group("h_long", "hiragana", "Long vowels",
          rows("おかあさん:okaasan おにいさん:oniisan くうき:kuuki せんせい:sensei こおり:koori おとうさん:otousan"),
          "Long vowels take two beats and change meaning (おばさん aunt vs おばあさん grandma). E-sounds usually lengthen with い, O-sounds with う."),
]

K_CHOON = group("k_choon", "katakana", "Long vowel mark ー",
                rows("ケーキ:keeki キー:kii カー:kaa"),
                "In katakana a long vowel is a dash ー. ケーキ = ke-e-ki (cake). You can type it with '-'.")

K_GROUPS = [
    group("k_dakuten", "katakana", "Katakana with dakuten ゛",
          rows("ガ:ga ギ:gi グ:gu ゲ:ge ゴ:go ザ:za ジ:ji ズ:zu ゼ:ze ゾ:zo ダ:da デ:de ド:do バ:ba ビ:bi ブ:bu ベ:be ボ:bo"),
          "Exactly the same rule as hiragana: the two dots voice the consonant. カ → ガ, サ → ザ, タ → ダ, ハ → バ."),
    group("k_handakuten", "katakana", "Katakana with handakuten ゜",
          rows("パ:pa ピ:pi プ:pu ペ:pe ポ:po"),
          "The little circle turns h into p, just like hiragana: ハ → パ."),
    group("k_yoon", "katakana", "Katakana combo sounds",
          rows("キャ:kya キュ:kyu キョ:kyo シャ:sha シュ:shu ショ:sho チャ:cha チュ:chu チョ:cho ニャ:nya ヒャ:hya ミャ:mya リャ:rya リュ:ryu リョ:ryo ギャ:gya ジャ:ja ジュ:ju ジョ:jo ビャ:bya ピャ:pya"),
          "Small ャュョ work just like in hiragana: シ + ャ = シャ (sha), as in シャベル (shovel)."),
    group("k_sokuon", "katakana", "Small ッ in katakana",
          rows("ベッド:beddo クッキー:kukkii ロケット:roketto ネット:netto"),
          "Small ッ doubles the next consonant, just like っ: ベッド = be-d-do (bed)."),
    group("k_extended", "katakana", "Extra sounds for foreign words",
          rows("ティ:ti ディ:di ファ:fa フィ:fi フェ:fe フォ:fo ウィ:wi ウェ:we ウォ:wo シェ:she ジェ:je チェ:che ヴァ:va"),
          "Katakana borrows small vowels (ァィゥェォ) to make sounds Japanese doesn't have: フ + ァ = ファ (fa) as in ファントム (phantom)."),
]

CONFUSABLES = [
    ["あ", "お", "め", "ぬ"], ["い", "り", "こ"], ["う", "ら", "つ"], ["さ", "ち", "き"],
    ["は", "ほ", "け"], ["ね", "れ", "わ"], ["る", "ろ"], ["ぬ", "め"], ["ま", "も", "ほ"],
    ["し", "つ", "の"], ["く", "へ", "し"], ["た", "な", "に"], ["そ", "て", "と"], ["す", "む", "お"],
    ["シ", "ツ", "ソ", "ン"], ["ク", "ワ", "ケ", "タ"], ["ア", "マ", "ヤ"], ["ナ", "メ", "ヌ", "ス"],
    ["ウ", "ワ", "フ", "ラ"], ["チ", "テ", "ナ"], ["ユ", "コ", "ヨ", "ロ"], ["ル", "レ", "ノ"],
    ["ニ", "ミ", "エ"], ["セ", "ヤ", "サ"], ["ハ", "ヘ", "ホ"],
]


def main():
    items = [single("h", "hiragana", r) for r in HIRAGANA]
    items += H_DAKUTEN + H_COMBOS
    items += [single("k", "katakana", r) for r in KATAKANA]
    items += [K_CHOON] + K_GROUPS
    data = {"items": items, "confusables": CONFUSABLES}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"wrote {len(items)} kana items")


if __name__ == "__main__":
    main()
