"""Generates src/main/resources/assets/nihongo/curriculum/vocab.json.

Words are grouped by the lesson that introduces them. A word only uses kana that earlier lessons
taught (checked by tools/validate_curriculum.py), so every new word is readable the moment it
appears. Words are chosen for frequency in beginner courses (JLPT N5 / Genki I) and for being
visible in Minecraft, so the game world becomes picture-based practice.

mc patterns: "item:", "block:" or "entity:" prefix, or none for any; '*' is a wildcard.
Run: python3 tools/gen_vocab.py
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nihongo", "curriculum", "vocab.json")

LESSONS = {}
_current = None


def lesson(name):
    global _current
    _current = name
    LESSONS.setdefault(name, [])


def V(id, kana, kanji, meanings, mc=(), icon=None, collect=None, note="", pos="noun",
      hint=None, kq=True, accept=(), ex=None):
    item = {"id": "v_" + id, "kana": kana, "meanings": list(meanings), "pos": pos}
    if kanji:
        item["kanji"] = kanji
        if not kq:
            item["kanjiQuiz"] = False
    if mc:
        item["mc"] = list(mc)
    if icon:
        item["icon"] = icon
    if collect:
        item["collect"] = collect
    if note:
        item["note"] = note
    if hint:
        item["hint"] = hint
    if accept:
        item["accept"] = list(accept)
    if ex:
        item["example"] = {"jp": ex[0], "en": ex[1]}
    LESSONS[_current].append(item)


def mobs(*names):
    return [f"entity:minecraft:{n}" for n in names]


# --------------------------------------------------------------------------- after あ/か rows
lesson("words_1")
V("ki", "き", "木", ["tree", "wood"], ["minecraft:*_log", "minecraft:*_wood"], "minecraft:oak_log", "minecraft:oak_log",
  "木 looks like a tree with branches and roots.")
V("ie", "いえ", "家", ["house", "home"], icon="minecraft:oak_door")
V("aka", "あか", "赤", ["red"], ["minecraft:red_*"], "minecraft:red_dye", "minecraft:red_dye")
V("ao", "あお", "青", ["blue"], ["minecraft:blue_*", "minecraft:light_blue_*"], "minecraft:blue_dye",
  note="あお can also describe green things like traffic lights and fresh leaves.")
V("kao", "かお", "顔", ["face"], icon="minecraft:player_head")
V("koe", "こえ", "声", ["voice"], icon="minecraft:note_block")
V("e", "え", "絵", ["picture", "painting", "drawing"], ["minecraft:painting"], "minecraft:painting", "minecraft:painting")
V("ika", "いか", "烏賊", ["squid"], mobs("squid", "glow_squid"), "minecraft:squid_spawn_egg", kq=False,
  note="Animal names are often written in katakana: イカ.")
V("ue", "うえ", "上", ["above", "up", "on top"], note="上 points up from a baseline.")

# --------------------------------------------------------------------------- after さ/た rows
lesson("words_2")
V("ishi", "いし", "石", ["stone", "rock"], ["minecraft:stone", "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:smooth_stone"],
  "minecraft:cobblestone", "minecraft:cobblestone")
V("tsuchi", "つち", "土", ["dirt", "soil", "earth"], ["minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt"],
  "minecraft:dirt", "minecraft:dirt", "土 is a sprout pushing up out of the ground.")
V("take", "たけ", "竹", ["bamboo"], ["minecraft:bamboo", "minecraft:bamboo_block"], "minecraft:bamboo", "minecraft:bamboo")
V("tetsu", "てつ", "鉄", ["iron"], ["minecraft:iron_ingot", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:iron_block",
                                  "minecraft:raw_iron", "minecraft:iron_nugget"], "minecraft:iron_ingot", "minecraft:iron_ingot")
V("ushi", "うし", "牛", ["cow", "cattle"], mobs("cow"), "minecraft:cow_spawn_egg")
V("tate", "たて", "盾", ["shield"], ["minecraft:shield"], "minecraft:shield", "minecraft:shield")
V("ito", "いと", "糸", ["string", "thread"], ["minecraft:string"], "minecraft:string", "minecraft:string")
V("kutsu", "くつ", "靴", ["shoes", "boots"], ["minecraft:*_boots"], "minecraft:leather_boots", kq=False)
V("asa", "あさ", "朝", ["morning"], icon="minecraft:sunflower")
V("suika", "すいか", "西瓜", ["watermelon", "melon"], ["minecraft:melon", "minecraft:melon_slice"], "minecraft:melon_slice",
  "minecraft:melon_slice", kq=False, note="Minecraft's melons are Japanese スイカ (watermelon).")
V("satou", "さとう", "砂糖", ["sugar"], ["minecraft:sugar"], "minecraft:sugar", "minecraft:sugar")
V("tokei", "とけい", "時計", ["clock", "watch"], ["minecraft:clock"], "minecraft:clock")
V("teki", "てき", "敵", ["enemy"], icon="minecraft:zombie_head")
V("te", "て", "手", ["hand"], hint="body part")
V("shita", "した", "下", ["below", "down", "under"])
V("soto", "そと", "外", ["outside"])

# --------------------------------------------------------------------------- after な/は rows
lesson("words_3")
V("inu", "いぬ", "犬", ["dog"], icon="minecraft:bone", note="Minecraft's tameable wolves are おおかみ (wolf), not いぬ.")
V("neko", "ねこ", "猫", ["cat"], mobs("cat", "ocelot"), "minecraft:cat_spawn_egg")
V("sakana", "さかな", "魚", ["fish"], ["minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish",
                                     "minecraft:cooked_cod", "minecraft:cooked_salmon"], "minecraft:cod", "minecraft:cod")
V("niku", "にく", "肉", ["meat"], ["minecraft:beef", "minecraft:cooked_beef", "minecraft:porkchop", "minecraft:cooked_porkchop",
                                "minecraft:mutton", "minecraft:cooked_mutton", "item:minecraft:chicken", "minecraft:cooked_chicken",
                                "item:minecraft:rabbit", "minecraft:cooked_rabbit", "minecraft:rotten_flesh"],
  "minecraft:cooked_beef", "minecraft:beef")
V("kinoko", "きのこ", "茸", ["mushroom"], ["minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:*_mushroom_block"],
  "minecraft:red_mushroom", "minecraft:brown_mushroom", kq=False)
V("hana", "はな", "花", ["flower"], ["minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium",
                                  "minecraft:azure_bluet", "minecraft:*_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower",
                                  "minecraft:lily_of_the_valley", "minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush",
                                  "minecraft:peony", "minecraft:torchflower"], "minecraft:poppy", "minecraft:poppy",
  "Also はな 鼻 = nose (same sound, different word).")
V("hi", "ひ", "火", ["fire"], ["block:minecraft:fire", "block:minecraft:soul_fire", "minecraft:campfire", "minecraft:fire_charge"],
  "minecraft:flint_and_steel", note="火 looks like a campfire with sparks flying out.")
V("hoshi", "ほし", "星", ["star"], ["minecraft:nether_star"], "minecraft:nether_star")
V("fune", "ふね", "船", ["boat", "ship"], ["minecraft:*_boat", "minecraft:*_raft"], "minecraft:oak_boat",
  note="Minecraft calls the boat ボート, a loanword. ふね is the everyday word.")
V("hone", "ほね", "骨", ["bone"], ["minecraft:bone", "minecraft:bone_block"], "minecraft:bone", "minecraft:bone")
V("hito", "ひと", "人", ["person", "people"], icon="minecraft:player_head", note="人 is a person walking: two legs.")
V("suna", "すな", "砂", ["sand"], ["minecraft:sand", "minecraft:red_sand", "minecraft:suspicious_sand"], "minecraft:sand", "minecraft:sand")
V("kusa", "くさ", "草", ["grass", "weeds"], ["minecraft:short_grass", "minecraft:tall_grass"], "minecraft:short_grass")
V("kitsune", "きつね", "狐", ["fox"], mobs("fox"), "minecraft:fox_spawn_egg", kq=False)
V("hachi", "はち", "蜂", ["bee"], mobs("bee"), "minecraft:bee_spawn_egg", kq=False, hint="animal",
  note="はち also means 8 (八). Context tells you which.")
V("nani", "なに", "何", ["what"], pos="question", note="Before です it often shortens to なん: なん です か (What is it?).")
V("anata", "あなた", None, ["you"], pos="pronoun", note="Japanese avoids あなた with people you know - use their name instead.")
V("hai", "はい", None, ["yes"], pos="phrase")
V("iie", "いいえ", None, ["no"], pos="phrase")

# --------------------------------------------------------------------------- after ま/や rows
lesson("words_4")
V("umi", "うみ", "海", ["sea", "ocean"], icon="minecraft:heart_of_the_sea")
V("yama", "やま", "山", ["mountain"], icon="minecraft:stone", note="山 is three mountain peaks.")
V("yuki", "ゆき", "雪", ["snow"], ["minecraft:snow", "minecraft:snow_block", "minecraft:snowball", "minecraft:powder_snow"],
  "minecraft:snowball", "minecraft:snowball")
V("yumi", "ゆみ", "弓", ["bow"], ["minecraft:bow"], "minecraft:bow", "minecraft:bow", "弓 is the shape of a bent bow.")
V("ya", "や", "矢", ["arrow"], ["minecraft:arrow", "minecraft:spectral_arrow", "minecraft:tipped_arrow"], "minecraft:arrow", "minecraft:arrow")
V("me", "め", "目", ["eye"], ["minecraft:spider_eye", "minecraft:fermented_spider_eye"], "minecraft:spider_eye",
  note="目 is an eye turned on its side.")
V("mushi", "むし", "虫", ["bug", "insect"], mobs("silverfish", "endermite"), "minecraft:silverfish_spawn_egg")
V("atama", "あたま", "頭", ["head"], ["minecraft:*_head", "minecraft:*_skull"], "minecraft:zombie_head")
V("uma", "うま", "馬", ["horse"], mobs("horse", "skeleton_horse", "zombie_horse"), "minecraft:horse_spawn_egg")
V("kumo", "くも", "雲", ["spider", "cloud"], mobs("spider", "cave_spider"), "minecraft:spider_eye", kq=False,
  note="Two words, one sound: 蜘蛛 (spider) and 雲 (cloud).")
V("kuma", "くま", "熊", ["bear"], mobs("polar_bear"), "minecraft:polar_bear_spawn_egg", kq=False)
V("kame", "かめ", "亀", ["turtle", "tortoise"], mobs("turtle") + ["minecraft:turtle_egg", "minecraft:turtle_scute"],
  "minecraft:turtle_spawn_egg", kq=False)
V("kami", "かみ", "紙", ["paper"], ["minecraft:paper"], "minecraft:paper", "minecraft:paper",
  note="Also 髪 (hair) and 神 (god) - same sound.")
V("ookami", "おおかみ", "狼", ["wolf"], mobs("wolf"), "minecraft:wolf_spawn_egg", kq=False)
V("taimatsu", "たいまつ", "松明", ["torch"], ["minecraft:torch", "minecraft:wall_torch", "minecraft:soul_torch", "minecraft:soul_wall_torch"],
  "minecraft:torch", "minecraft:torch", kq=False)
V("mame", "まめ", "豆", ["bean", "beans"], ["minecraft:cocoa_beans"], "minecraft:cocoa_beans")
V("yasai", "やさい", "野菜", ["vegetable", "vegetables"], icon="minecraft:beetroot")
V("mono", "もの", "物", ["thing", "object"])
V("ima", "いま", "今", ["now"], pos="noun")
V("namae", "なまえ", "名前", ["name"], ["minecraft:name_tag"], "minecraft:name_tag")
V("tsuki", "つき", "月", ["moon", "month"], note="月 also means month: いちがつ = January.")

# --------------------------------------------------------------------------- after ら/わ rows
lesson("words_5")
V("yoru", "よる", "夜", ["night"], icon="minecraft:black_bed")
V("sora", "そら", "空", ["sky"], icon="minecraft:feather")
V("tori", "とり", "鳥", ["bird"], mobs("parrot"), "minecraft:parrot_spawn_egg")
V("kuro", "くろ", "黒", ["black"], ["minecraft:black_*"], "minecraft:black_dye")
V("shiro", "しろ", "白", ["white"], ["minecraft:white_*"], "minecraft:white_dye", note="Also 城 (castle).")
V("niwatori", "にわとり", "鶏", ["chicken"], mobs("chicken"), "minecraft:chicken_spawn_egg", kq=False,
  note="Literally 'garden bird': にわ (garden) + とり (bird).")
V("kuwa", "くわ", "鍬", ["hoe"], ["minecraft:*_hoe"], "minecraft:iron_hoe", "minecraft:wooden_hoe", kq=False)
V("ono", "おの", "斧", ["axe"], ["minecraft:*_axe"], "minecraft:iron_axe", "minecraft:wooden_axe", kq=False)
V("hon", "ほん", "本", ["book"], ["minecraft:book", "minecraft:writable_book", "minecraft:written_book", "minecraft:enchanted_book"],
  "minecraft:book", "minecraft:book", "本 is a tree (木) with a line marking its root - the origin of knowledge.")
V("kawa", "かわ", "川", ["river", "leather", "skin"], ["minecraft:leather"], "minecraft:leather", "minecraft:leather",
  "川 (river) is three flowing streams. 革/皮 (leather, skin) sound the same.")
V("koori", "こおり", "氷", ["ice"], ["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:frosted_ice"], "minecraft:ice")
V("kusuri", "くすり", "薬", ["medicine", "drug"], icon="minecraft:potion")
V("iruka", "いるか", "海豚", ["dolphin"], mobs("dolphin"), "minecraft:dolphin_spawn_egg", kq=False)
V("kaeru", "かえる", "蛙", ["frog"], mobs("frog", "tadpole"), "minecraft:frog_spawn_egg", kq=False,
  note="Also 帰る (to return home) - same sound.")
V("koumori", "こうもり", "蝙蝠", ["bat"], mobs("bat"), "minecraft:bat_spawn_egg", kq=False)
V("hikari", "ひかり", "光", ["light"], ["block:minecraft:light"], "minecraft:glowstone_dust")
V("ken", "けん", "剣", ["sword"], ["minecraft:*_sword"], "minecraft:iron_sword", "minecraft:wooden_sword")
V("kin", "きん", "金", ["gold"], ["minecraft:gold_ingot", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
                                "minecraft:gold_block", "minecraft:raw_gold", "minecraft:gold_nugget"], "minecraft:gold_ingot",
  "minecraft:gold_nugget", "金 also means money: おかね.")
V("sekitan", "せきたん", "石炭", ["coal"], ["minecraft:coal", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:coal_block"],
  "minecraft:coal", "minecraft:coal", "石 (stone) + 炭 (charcoal) = coal.")
V("watashi", "わたし", "私", ["I", "me"], pos="pronoun")
V("mura", "むら", "村", ["village"], ["minecraft:bell"], "minecraft:bell")
V("ame", "あめ", "雨", ["rain"], note="Also 飴 (candy), with a different pitch.")

lesson("greetings_1")
V("ohayou", "おはよう", None, ["good morning"], pos="phrase", accept=["おはようございます"],
  note="Polite version: おはようございます.")
V("konnichiwa", "こんにちは", None, ["hello", "good afternoon"], pos="phrase", accept=["こんにちわ"],
  note="The final は is the old topic particle, so it's pronounced 'wa'.")
V("sayounara", "さようなら", None, ["goodbye"], pos="phrase", note="Sounds final; friends say じゃあね or またね.")
V("oyasumi", "おやすみ", None, ["good night"], pos="phrase", note="Polite: おやすみなさい.")
V("sumimasen", "すみません", None, ["excuse me", "sorry"], pos="phrase",
  note="Use it to get attention, apologise lightly, or even to say thanks for trouble taken.")

# --------------------------------------------------------------------------- after dakuten
lesson("words_6")
V("ninjin", "にんじん", "人参", ["carrot"], ["minecraft:carrot", "minecraft:carrots"], "minecraft:carrot", "minecraft:carrot", kq=False)
V("mizu", "みず", "水", ["water"], ["block:minecraft:water", "minecraft:water_bucket"], "minecraft:water_bucket")
V("kaze", "かぜ", "風", ["wind"], ["minecraft:wind_charge"] + mobs("breeze"), "minecraft:wind_charge",
  note="Also 風邪 (a cold, illness).")
V("hitsuji", "ひつじ", "羊", ["sheep"], mobs("sheep"), "minecraft:sheep_spawn_egg")
V("buta", "ぶた", "豚", ["pig"], mobs("pig"), "minecraft:pig_spawn_egg")
V("ringo", "りんご", "林檎", ["apple"], ["minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple"],
  "minecraft:apple", "minecraft:apple", kq=False)
V("tamago", "たまご", "卵", ["egg"], ["minecraft:egg", "minecraft:sniffer_egg"], "minecraft:egg", "minecraft:egg")
V("bou", "ぼう", "棒", ["stick", "pole"], ["minecraft:stick"], "minecraft:stick", "minecraft:stick")
V("kagi", "かぎ", "鍵", ["key"], ["minecraft:trial_key", "minecraft:ominous_trial_key"], "minecraft:trial_key", kq=False)
V("mado", "まど", "窓", ["window"], ["minecraft:glass_pane", "minecraft:*_stained_glass_pane"], "minecraft:glass_pane")
V("kabe", "かべ", "壁", ["wall"], ["minecraft:*_wall"], "minecraft:cobblestone_wall")
V("hashigo", "はしご", "梯子", ["ladder"], ["minecraft:ladder"], "minecraft:ladder", "minecraft:ladder", kq=False)
V("kamado", "かまど", "竈", ["furnace", "stove"], ["minecraft:furnace"], "minecraft:furnace", "minecraft:furnace", kq=False,
  note="The Japanese Minecraft name for the furnace.")
V("chizu", "ちず", "地図", ["map"], ["minecraft:map", "minecraft:filled_map"], "minecraft:map")
V("dou", "どう", "銅", ["copper"], ["minecraft:copper_ingot", "minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:raw_copper",
                                 "minecraft:copper_block"], "minecraft:copper_ingot", "minecraft:raw_copper")
V("mugi", "むぎ", "麦", ["wheat", "barley"], ["minecraft:wheat"], "minecraft:wheat", "minecraft:wheat")
V("usagi", "うさぎ", "兎", ["rabbit", "bunny"], mobs("rabbit"), "minecraft:rabbit_spawn_egg", kq=False)
V("rakuda", "らくだ", "駱駝", ["camel"], mobs("camel"), "minecraft:camel_spawn_egg", kq=False)
V("murabito", "むらびと", "村人", ["villager"], mobs("villager"), "minecraft:villager_spawn_egg",
  note="むら (village) + ひと (person); the ひ softens to び.")
V("yougan", "ようがん", "溶岩", ["lava"], ["block:minecraft:lava", "minecraft:lava_bucket"], "minecraft:lava_bucket", kq=False)
V("kaidan", "かいだん", "階段", ["stairs", "staircase"], ["minecraft:*_stairs"], "minecraft:oak_stairs", "minecraft:oak_stairs")
V("gohan", "ごはん", "ご飯", ["meal", "cooked rice"], icon="minecraft:bowl", note="Rice is so central that 'rice' also means 'meal'.")

lesson("greetings_2")
V("arigatou", "ありがとう", None, ["thank you", "thanks"], pos="phrase", accept=["ありがとうございます"],
  note="Polite: ありがとうございます.")
V("konbanwa", "こんばんは", None, ["good evening"], pos="phrase", accept=["こんばんわ"])
V("itadakimasu", "いただきます", None, ["let's eat", "thanks for the food"], pos="phrase",
  note="Said before eating - try it before your next steak!")
V("gochisousama", "ごちそうさま", None, ["thanks for the meal"], pos="phrase", note="Said after eating.")
V("gomennasai", "ごめんなさい", None, ["I'm sorry", "sorry"], pos="phrase")
V("douzo", "どうぞ", None, ["please", "go ahead", "here you are"], pos="phrase", note="Offering something: どうぞ.")
V("onegaishimasu", "おねがいします", None, ["please"], pos="phrase", note="Requesting something: みず を おねがいします.")

# --------------------------------------------------------------------------- after combos
lesson("numbers")
V("ichi", "いち", "一", ["one", "1"], pos="number", hint="number")
V("ni", "に", "二", ["two", "2"], pos="number", hint="number")
V("san", "さん", "三", ["three", "3"], pos="number", hint="number")
V("yon", "よん", "四", ["four", "4"], pos="number", hint="number", accept=["し"], note="Also read し, but し sounds like 死 (death).")
V("go", "ご", "五", ["five", "5"], pos="number", hint="number")
V("roku", "ろく", "六", ["six", "6"], pos="number", hint="number")
V("nana", "なな", "七", ["seven", "7"], pos="number", hint="number", accept=["しち"])
V("hachi_num", "はち", "八", ["eight", "8"], pos="number", hint="number")
V("kyuu", "きゅう", "九", ["nine", "9"], pos="number", hint="number", accept=["く"])
V("juu", "じゅう", "十", ["ten", "10"], pos="number", hint="number", note="Bigger numbers combine: じゅうご = 15, にじゅう = 20.")
V("hyaku", "ひゃく", "百", ["hundred", "100"], pos="number", hint="number")

lesson("words_7")
V("kabocha", "かぼちゃ", "南瓜", ["pumpkin"], ["minecraft:pumpkin", "minecraft:carved_pumpkin"], "minecraft:pumpkin", "minecraft:pumpkin", kq=False)
V("jagaimo", "じゃがいも", None, ["potato"], ["minecraft:potato", "minecraft:potatoes", "minecraft:baked_potato", "minecraft:poisonous_potato"],
  "minecraft:potato", "minecraft:potato")
V("happa", "はっぱ", "葉っぱ", ["leaf", "leaves"], ["minecraft:*_leaves"], "minecraft:oak_leaves")
V("sagyoudai", "さぎょうだい", "作業台", ["crafting table", "workbench"], ["minecraft:crafting_table"], "minecraft:crafting_table",
  "minecraft:crafting_table", "作業 (work) + 台 (stand).")
V("kyou", "きょう", "今日", ["today"], note="今日 is a special reading: 今 (now) + 日 (day).")
V("ashita", "あした", "明日", ["tomorrow"])
V("kinou", "きのう", "昨日", ["yesterday"])
V("daijoubu", "だいじょうぶ", "大丈夫", ["okay", "all right", "fine"], kq=False, pos="na-adjective")
V("chotto", "ちょっと", None, ["a little", "a bit"], pos="adverb", note="Also a soft way to refuse: ちょっと... (that's a bit...)")
V("issho", "いっしょ", "一緒", ["together"], kq=False, note="いっしょ に = together (with).")

lesson("people")
V("gakusei", "がくせい", "学生", ["student"])
V("sensei", "せんせい", "先生", ["teacher"], note="Also used as a title: やまだ せんせい.")
V("tomodachi", "ともだち", "友達", ["friend"])
V("nihonjin", "にほんじん", "日本人", ["Japanese person"], note="Country + じん = person from there.")
V("nihongo", "にほんご", "日本語", ["Japanese language", "Japanese"], note="Country + ご = language.")
V("eigo", "えいご", "英語", ["English", "English language"])
V("dare", "だれ", "誰", ["who"], kq=False, pos="question")

lesson("adjectives_1")
V("ookii", "おおきい", "大きい", ["big", "large"], pos="i-adjective")
V("chiisai", "ちいさい", "小さい", ["small", "little"], pos="i-adjective")
V("takai", "たかい", "高い", ["tall", "high", "expensive"], pos="i-adjective")
V("hikui", "ひくい", "低い", ["low", "short"], pos="i-adjective")
V("atsui", "あつい", "暑い", ["hot"], pos="i-adjective", note="暑い for weather, 熱い for hot things.")
V("samui", "さむい", "寒い", ["cold"], pos="i-adjective", note="For weather. Cold things are つめたい.")
V("kowai", "こわい", "怖い", ["scary", "frightening"], pos="i-adjective")
V("tsuyoi", "つよい", "強い", ["strong"], pos="i-adjective")
V("yowai", "よわい", "弱い", ["weak"], pos="i-adjective")
V("hayai", "はやい", "速い", ["fast", "quick", "early"], pos="i-adjective", note="速い = fast, 早い = early.")
V("osoi", "おそい", "遅い", ["slow", "late"], pos="i-adjective")
V("akarui", "あかるい", "明るい", ["bright"], pos="i-adjective")
V("kurai", "くらい", "暗い", ["dark"], pos="i-adjective")
V("ii", "いい", "良い", ["good"], pos="i-adjective", kq=False, note="Irregular: its other forms come from よい (よくない, よかった).")
V("warui", "わるい", "悪い", ["bad"], pos="i-adjective")
V("oishii", "おいしい", "美味しい", ["tasty", "delicious"], pos="i-adjective", kq=False)
V("atarashii", "あたらしい", "新しい", ["new"], pos="i-adjective")
V("furui", "ふるい", "古い", ["old"], pos="i-adjective", note="For things, not people.")
V("abunai", "あぶない", "危ない", ["dangerous"], pos="i-adjective", note="Shout あぶない! when a creeper sneaks up.")
V("kawaii", "かわいい", None, ["cute"], pos="i-adjective")
V("tanoshii", "たのしい", "楽しい", ["fun", "enjoyable"], pos="i-adjective")

lesson("pointing_words")
V("kore", "これ", None, ["this", "this one"], pos="pronoun", note="Near the speaker.")
V("sore", "それ", None, ["that", "that one"], pos="pronoun", note="Near the listener.")
V("are", "あれ", None, ["that over there"], pos="pronoun", accept=[], note="Far from both.")
V("dore", "どれ", None, ["which", "which one"], pos="question")
V("koko", "ここ", None, ["here"], pos="pronoun")
V("soko", "そこ", None, ["there"], pos="pronoun")
V("asoko", "あそこ", None, ["over there"], pos="pronoun")
V("doko", "どこ", None, ["where"], pos="question")

# --------------------------------------------------------------------------- katakana words
lesson("katakana_mobs")
V("zonbi", "ゾンビ", None, ["zombie"], mobs("zombie", "zombie_villager"), "minecraft:zombie_head")
V("sukeruton", "スケルトン", None, ["skeleton"], ["entity:minecraft:*skeleton"], "minecraft:skeleton_skull")
V("kuriipaa", "クリーパー", None, ["creeper"], mobs("creeper"), "minecraft:creeper_head")
V("endaaman", "エンダーマン", None, ["enderman"], mobs("enderman"), "minecraft:ender_pearl")
V("suraimu", "スライム", None, ["slime"], mobs("slime") + ["minecraft:slime_ball", "minecraft:slime_block"], "minecraft:slime_ball",
  "minecraft:slime_ball")
V("gooremu", "ゴーレム", None, ["golem"], mobs("iron_golem", "snow_golem"), "minecraft:carved_pumpkin")
V("doragon", "ドラゴン", None, ["dragon"], mobs("ender_dragon") + ["minecraft:dragon_egg", "minecraft:dragon_head"], "minecraft:dragon_head")
V("witchi", "ウィッチ", None, ["witch"], mobs("witch"), "minecraft:witch_spawn_egg")
V("gasuto", "ガスト", None, ["ghast"], mobs("ghast") + ["minecraft:ghast_tear"], "minecraft:ghast_tear")
V("bureizu", "ブレイズ", None, ["blaze"], mobs("blaze") + ["minecraft:blaze_rod", "minecraft:blaze_powder"], "minecraft:blaze_rod")
V("fantomu", "ファントム", None, ["phantom"], mobs("phantom") + ["minecraft:phantom_membrane"], "minecraft:phantom_membrane")

lesson("katakana_things")
V("daiyamondo", "ダイヤモンド", None, ["diamond"], ["minecraft:diamond", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                                                "minecraft:diamond_block"], "minecraft:diamond", "minecraft:diamond")
V("emerarudo", "エメラルド", None, ["emerald"], ["minecraft:emerald", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                                            "minecraft:emerald_block"], "minecraft:emerald", "minecraft:emerald")
V("reddosutoon", "レッドストーン", None, ["redstone"], ["minecraft:redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                                                   "minecraft:redstone_block", "minecraft:redstone_wire"], "minecraft:redstone", "minecraft:redstone")
V("burokku", "ブロック", None, ["block"], icon="minecraft:grass_block")
V("beddo", "ベッド", None, ["bed"], ["minecraft:*_bed"], "minecraft:red_bed")
V("chesuto", "チェスト", None, ["chest"], ["minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest"], "minecraft:chest", "minecraft:chest")
V("doa", "ドア", None, ["door"], ["minecraft:*_door"], "minecraft:oak_door", "minecraft:oak_door")
V("garasu", "ガラス", None, ["glass"], ["minecraft:glass", "minecraft:*_stained_glass", "minecraft:tinted_glass"], "minecraft:glass", "minecraft:glass")
V("renga", "レンガ", "煉瓦", ["brick", "bricks"], ["minecraft:brick", "minecraft:bricks"], "minecraft:bricks", "minecraft:brick", kq=False)
V("rantan", "ランタン", None, ["lantern"], ["minecraft:lantern", "minecraft:soul_lantern"], "minecraft:lantern", "minecraft:lantern")
V("reeru", "レール", None, ["rail", "rails"], ["minecraft:rail", "minecraft:powered_rail", "minecraft:detector_rail", "minecraft:activator_rail"],
  "minecraft:rail", "minecraft:rail")
V("torokko", "トロッコ", None, ["minecart", "mine cart"], ["minecraft:minecart", "minecraft:*_minecart"], "minecraft:minecart")
V("tsuruhashi", "ツルハシ", None, ["pickaxe"], ["minecraft:*_pickaxe"], "minecraft:iron_pickaxe", "minecraft:wooden_pickaxe",
  note="A native word (鶴嘴, 'crane's beak') usually written in katakana.")
V("shaberu", "シャベル", None, ["shovel"], ["minecraft:*_shovel"], "minecraft:iron_shovel", "minecraft:wooden_shovel")
V("baketsu", "バケツ", None, ["bucket"], ["minecraft:bucket", "minecraft:*_bucket"], "minecraft:bucket", "minecraft:bucket")
V("konpasu", "コンパス", None, ["compass"], ["minecraft:compass", "minecraft:recovery_compass"], "minecraft:compass")

lesson("katakana_food")
V("pan", "パン", None, ["bread"], ["minecraft:bread"], "minecraft:bread", "minecraft:bread", "From Portuguese 'pão'.")
V("keeki", "ケーキ", None, ["cake"], ["minecraft:cake"], "minecraft:cake")
V("kukkii", "クッキー", None, ["cookie"], ["minecraft:cookie"], "minecraft:cookie", "minecraft:cookie")
V("pooshon", "ポーション", None, ["potion"], ["minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion"], "minecraft:potion")
V("miruku", "ミルク", None, ["milk"], ["minecraft:milk_bucket"], "minecraft:milk_bucket")
V("suupu", "スープ", None, ["soup", "stew"], ["minecraft:mushroom_stew", "minecraft:beetroot_soup", "minecraft:rabbit_stew",
                                           "minecraft:suspicious_stew"], "minecraft:mushroom_stew")
V("nezaa", "ネザー", None, ["the Nether", "Nether"], ["minecraft:netherrack"], "minecraft:netherrack")
V("endo", "エンド", None, ["the End", "End"], ["minecraft:end_stone"], "minecraft:end_stone")
V("geemu", "ゲーム", None, ["game"])

lesson("adjectives_na")
V("shizuka", "しずか", "静か", ["quiet"], pos="na-adjective")
V("kirei", "きれい", None, ["pretty", "clean", "beautiful"], pos="na-adjective", note="Ends in い but it's a な-adjective!")
V("genki", "げんき", "元気", ["healthy", "energetic", "well"], pos="na-adjective")
V("suki", "すき", "好き", ["like", "liked"], pos="na-adjective", note="X が すき です = I like X.")
V("kirai", "きらい", "嫌い", ["dislike", "hate"], pos="na-adjective", kq=False, note="Ends in い but it's a な-adjective!")
V("jouzu", "じょうず", "上手", ["skilled", "good at"], pos="na-adjective")
V("heta", "へた", "下手", ["bad at", "unskilled"], pos="na-adjective")
V("taihen", "たいへん", "大変", ["tough", "hard", "terrible"], pos="na-adjective")
V("benri", "べんり", "便利", ["convenient", "useful"], pos="na-adjective")
V("hima", "ひま", "暇", ["free (time)", "free", "bored"], pos="na-adjective", kq=False)

lesson("places")
V("doukutsu", "どうくつ", "洞窟", ["cave"], icon="minecraft:stone", kq=False)
V("mori", "もり", "森", ["forest", "woods"], icon="minecraft:oak_sapling", note="森 = three trees (木).")
V("shima", "しま", "島", ["island"])
V("sabaku", "さばく", "砂漠", ["desert"], icon="minecraft:cactus", kq=False, note="砂 (sand) appears in it!")
V("naka", "なか", "中", ["inside", "middle"])
V("mae", "まえ", "前", ["front", "before"])
V("ushiro", "うしろ", "後ろ", ["behind", "back"])
V("chikaku", "ちかく", "近く", ["near", "nearby"])
V("heya", "へや", "部屋", ["room"])

lesson("time_words")
V("mainichi", "まいにち", "毎日", ["every day"])
V("hiru", "ひる", "昼", ["noon", "daytime"])
V("yuugata", "ゆうがた", "夕方", ["evening"])
V("itsu", "いつ", None, ["when"], pos="question")
V("jikan", "じかん", "時間", ["time", "hour"])

lesson("verbs_1")
VERBS = [
    ("tabemasu", "たべます", "食べます", ["eat", "to eat"], "たべる", "minecraft:cooked_beef"),
    ("nomimasu", "のみます", "飲みます", ["drink", "to drink"], "のむ", "minecraft:milk_bucket"),
    ("mimasu", "みます", "見ます", ["see", "look", "watch", "to see"], "みる", "minecraft:spyglass"),
    ("ikimasu", "いきます", "行きます", ["go", "to go"], "いく", None),
    ("kimasu", "きます", "来ます", ["come", "to come"], "くる", None),
    ("kaerimasu", "かえります", "帰ります", ["go home", "return", "to go home"], "かえる", "minecraft:red_bed"),
    ("shimasu", "します", None, ["do", "to do"], "する", None),
    ("tsukurimasu", "つくります", "作ります", ["make", "build", "craft", "to make"], "つくる", "minecraft:crafting_table"),
    ("horimasu", "ほります", "掘ります", ["dig", "mine", "to dig"], "ほる", "minecraft:iron_pickaxe"),
    ("nemasu", "ねます", "寝ます", ["sleep", "go to bed", "to sleep"], "ねる", "minecraft:white_bed"),
    ("okimasu", "おきます", "起きます", ["wake up", "get up", "to wake up"], "おきる", None),
    ("kaimasu", "かいます", "買います", ["buy", "to buy"], "かう", "minecraft:emerald"),
    ("atsumemasu", "あつめます", "集めます", ["collect", "gather", "to collect"], "あつめる", "minecraft:chest"),
    ("tatakaimasu", "たたかいます", "戦います", ["fight", "to fight"], "たたかう", "minecraft:iron_sword"),
    ("taoshimasu", "たおします", "倒します", ["defeat", "knock down", "to defeat"], "たおす", "minecraft:diamond_sword"),
    ("hashirimasu", "はしります", "走ります", ["run", "to run"], "はしる", "minecraft:sugar"),
    ("tobimasu", "とびます", "飛びます", ["jump", "fly", "to fly"], "とぶ", "minecraft:elytra"),
    ("arukimasu", "あるきます", "歩きます", ["walk", "to walk"], "あるく", "minecraft:leather_boots"),
    ("tsukaimasu", "つかいます", "使います", ["use", "to use"], "つかう", None),
    ("mamorimasu", "まもります", "守ります", ["protect", "defend", "to protect"], "まもる", "minecraft:shield"),
    ("mitsukemasu", "みつけます", "見つけます", ["find", "to find"], "みつける", "minecraft:compass"),
    ("tasukemasu", "たすけます", "助けます", ["help", "save", "rescue", "to help"], "たすける", None),
    ("machimasu", "まちます", "待ちます", ["wait", "to wait"], "まつ", "minecraft:clock"),
]
for vid, kana, kanji, meanings, dictionary, icon in VERBS:
    V(vid, kana, kanji, meanings, icon=icon, pos="verb", kq=vid not in ("horimasu",),
      note=f"Polite ます form. Dictionary form: {dictionary}.")


def main():
    items = [i for group in LESSONS.values() for i in group]
    ids = [i["id"] for i in items]
    dupes = {x for x in ids if ids.count(x) > 1}
    if dupes:
        raise SystemExit(f"duplicate vocab ids: {dupes}")
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"items": items}, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"wrote {len(items)} words in {len(LESSONS)} groups")


if __name__ == "__main__":
    main()
