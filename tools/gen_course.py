"""Generates the whole course: kana.json, vocab.json, grammar.json and lessons.json.

Grammar follows the order used by mainstream beginner syllabi (Genki I, JLPT N5): the copula and
particles first, then adjectives, existence, verbs and their forms. Every example sentence is
written with spaces between words (as in early Genki chapters) and set in Minecraft situations.

Run: python3 tools/gen_course.py   (then: python3 tools/validate_curriculum.py)
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_kana  # noqa: E402
import gen_vocab  # noqa: E402

DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nihongo", "curriculum")

GRAMMAR = []


def G(id, title, summary, explanation, examples, exercises):
    GRAMMAR.append({
        "id": "g_" + id,
        "title": title,
        "summary": summary,
        "explanation": explanation,
        "examples": [{"jp": jp, "en": en} for jp, en in examples],
        "exercises": exercises,
    })


def choice(prompt, answer, options, en="", explain=""):
    return {"type": "choice", "prompt": prompt, "answer": answer, "options": options, "en": en, "explain": explain}


def order(en, tiles, extra=(), accept=(), explain=""):
    return {"type": "order", "en": en, "tiles": tiles, "extra": list(extra), "accept": list(accept), "explain": explain}


def typed(prompt, answer, en="", accept=(), explain=""):
    return {"type": "type", "prompt": prompt, "answer": answer, "en": en, "accept": list(accept), "explain": explain}


def meaning(jp, answer, wrong, explain=""):
    return {"type": "choice", "prompt": jp, "answer": answer, "options": [answer] + wrong, "en": "What does this mean?",
            "explain": explain}


G("desu", "X は Y です  (X is Y)", "は marks the topic; です politely ends the sentence.", [
    "Japanese sentences often start with a topic followed by は. As a particle, は is read 'wa'.",
    "です (desu - the final u is almost silent) works like 'is / am / are' and makes the sentence polite.",
    "Pattern: [topic] は [description] です. There are no words for 'a' or 'the', and nouns have no plural form.",
], [
    ("わたし は がくせい です。", "I am a student."),
    ("やま は たかい です。", "The mountain is tall."),
    ("ねこ は かわいい です。", "Cats are cute."),
], [
    choice("わたし ___ がくせい です。", "は", ["は", "を", "に", "の"], "I am a student."),
    choice("ともだち は にほんじん ___。", "です", ["です", "は", "か", "の"], "My friend is Japanese."),
    order("The mountain is tall.", ["やま", "は", "たかい", "です"]),
    meaning("ねこ は かわいい です。", "Cats are cute.", ["Cats are scary.", "Dogs are cute.", "I am a cat."]),
    typed("せんせい は にほんじん ___。", "です", "The teacher is Japanese."),
])

G("ka", "Questions with か", "Put か at the end to turn a sentence into a question.", [
    "Add か to the end of a sentence to make it a question. The word order does not change.",
    "Answer with はい (yes) or いいえ (no). Written Japanese often ends a か question with 。 instead of ?",
], [
    ("あなた は がくせい です か。", "Are you a student?"),
    ("はい、 がくせい です。", "Yes, I am a student."),
    ("さかな は おいしい です か。", "Is the fish tasty?"),
], [
    choice("あなた は せんせい です ___。", "か", ["か", "は", "を", "の"], "Are you a teacher?"),
    order("Is the cat cute?", ["ねこ", "は", "かわいい", "です", "か"]),
    meaning("はい、 せんせい です。", "Yes, I'm a teacher.", ["No, I'm a teacher.", "Are you a teacher?", "Yes, I'm a student."]),
    choice("さかな は おいしい です か。  ___、 おいしい です。", "はい", ["はい", "いいえ", "か", "です"], "Is the fish tasty? - Yes, it's tasty."),
])

G("kore", "これ・それ・あれ・どれ", "this / that / that over there / which", [
    "これ = this (near me), それ = that (near you), あれ = that over there (far from both), どれ = which one.",
    "Directly before a noun use この / その / あの / どの: この けん = this sword.",
    "The same こ-そ-あ-ど pattern gives ここ / そこ / あそこ / どこ (here / there / over there / where).",
], [
    ("これ は けん です。", "This is a sword."),
    ("あれ は なん です か。", "What is that over there?"),
    ("この りんご は おいしい です。", "This apple is tasty."),
], [
    choice("___ は ほん です。 (in my hand)", "これ", ["これ", "それ", "あれ", "どれ"], "This is a book."),
    choice("___ ゆみ は あたらしい です。", "この", ["この", "これ", "ここ", "どこ"], "This bow is new."),
    order("What is that over there?", ["あれ", "は", "なん", "です", "か"]),
    choice("いえ は ___ です か。", "どこ", ["どこ", "どれ", "だれ", "なに"], "Where is the house?"),
])

G("no", "の  (possession and description)", "A の B = A's B, or a B made of / related to A.", [
    "の links two nouns: わたし の けん = my sword.",
    "It also describes what something is made of or about: てつ の けん = an iron sword; にほんご の ほん = a Japanese book.",
    "You can chain them: わたし の ともだち の いぬ = my friend's dog.",
], [
    ("これ は わたし の けん です。", "This is my sword."),
    ("きん の りんご", "a golden apple"),
    ("ともだち の いえ は どこ です か。", "Where is my friend's house?"),
], [
    choice("これ は わたし ___ ゆみ です。", "の", ["の", "は", "を", "か"], "This is my bow."),
    order("an iron sword", ["てつ", "の", "けん"]),
    choice("それ は ___ の ほん です か。", "だれ", ["だれ", "どこ", "なに", "どれ"], "Whose book is that?"),
    meaning("せんせい の ねこ は かわいい です。", "The teacher's cat is cute.",
            ["The cat's teacher is cute.", "The teacher is a cute cat.", "My cat is cute."]),
    typed("いし ___ かべ", "の", "a stone wall"),
])

G("mo", "も  (also, too)", "も replaces は to mean 'also'.", [
    "も means 'also / too'. It replaces は (and が / を) - never write はも.",
    "With a negative sentence it means 'not ... either'.",
], [
    ("わたし も がくせい です。", "I'm a student too."),
    ("これ も おいしい です。", "This is tasty too."),
], [
    choice("ねこ は かわいい です。 いぬ ___ かわいい です。", "も", ["も", "は", "の", "か"], "Cats are cute. Dogs are cute too."),
    order("This is tasty too.", ["これ", "も", "おいしい", "です"]),
    meaning("あれ も わたし の いえ です。", "That over there is also my house.",
            ["That house is mine.", "This is also my house.", "My house is over there."]),
])

G("janai", "じゃないです  (is not)", "Noun + じゃないです / じゃありません = is not.", [
    "To say 'X is not Y' with a noun, replace です with じゃないです, or the more formal じゃありません.",
    "In writing you will also see ではありません - the same meaning, more formal.",
], [
    ("これ は けん じゃないです。", "This is not a sword."),
    ("わたし は せんせい じゃありません。", "I am not a teacher."),
], [
    choice("あれ は いぬ ___。 おおかみ です。", "じゃないです", ["じゃないです", "です", "か", "も"], "That's not a dog. It's a wolf."),
    order("I am not a teacher.", ["わたし", "は", "せんせい", "じゃありません"]),
    typed("これ は きん ___。", "じゃないです", "This is not gold.", ["じゃありません", "ではありません", "ではないです"]),
    meaning("それ は わたし の ほん じゃないです。", "That's not my book.",
            ["That is my book.", "Is that my book?", "That's my book too."]),
])

G("adj_i", "い-adjectives", "Before nouns as-is; negative: drop い, add くない.", [
    "い-adjectives end in い and go straight before a noun: たかい やま = a tall mountain.",
    "At the end of a sentence add です: やま は たかい です.",
    "Negative: drop the final い and add くないです: たかい → たかくないです. The irregular いい becomes よくないです.",
], [
    ("おおきい いえ", "a big house"),
    ("この けん は つよい です。", "This sword is strong."),
    ("この りんご は おいしくないです。", "This apple isn't tasty."),
], [
    typed("さかな は おいしく___。", "ないです", "The fish isn't tasty.", ["ない です"]),
    choice("この やま は ___ です。", "たかい", ["たかい", "たかく", "たかいの", "たかな"], "This mountain is tall."),
    order("a big house", ["おおきい", "いえ"]),
    choice("この ほん は ___ です。", "よくない", ["よくない", "いくない", "いいない", "よい"], "This book isn't good."),
    meaning("よる は あぶない です。", "The night is dangerous.",
            ["The night is dark.", "Mornings are dangerous.", "It's not dangerous at night."]),
])

G("adj_na", "な-adjectives", "Need な before a noun; behave like nouns at the end.", [
    "な-adjectives need な before a noun: しずか な むら = a quiet village.",
    "At the end of a sentence they act like nouns: むら は しずか です. Negative: しずか じゃないです.",
    "Watch out: きれい and きらい end in い but are な-adjectives!",
], [
    ("きれい な はな", "a pretty flower"),
    ("むら は しずか です。", "The village is quiet."),
    ("わたし は げんき です。", "I'm well."),
], [
    choice("しずか ___ もり", "な", ["な", "の", "い", "を"], "a quiet forest"),
    choice("この むら は べんり ___。", "じゃないです", ["じゃないです", "くないです", "です か", "な"], "This village isn't convenient."),
    order("a pretty star", ["きれい", "な", "ほし"]),
    meaning("わたし は げんき です。", "I'm well.", ["I'm sick.", "I'm quiet.", "I'm famous."]),
])

G("arimasu", "あります・います  (there is)", "Place に thing が あります (things) / います (living beings).", [
    "あります = there is / to have, for objects and plants. います = there is, for people and animals.",
    "Pattern: [place] に [thing] が あります / います. が marks the thing that exists.",
    "Negative: ありません / いません.",
], [
    ("どうくつ に ゾンビ が います。", "There are zombies in the cave."),
    ("むら に いえ が あります。", "There are houses in the village."),
    ("ここ に ダイヤモンド は ありません。", "There are no diamonds here."),
], [
    choice("もり に おおかみ が ___。", "います", ["います", "あります", "です", "します"], "There are wolves in the forest."),
    choice("チェスト に パン が ___。", "あります", ["あります", "います", "です", "ます"], "There's bread in the chest."),
    choice("どうくつ ___ スケルトン が います。", "に", ["に", "を", "は", "の"], "There's a skeleton in the cave."),
    order("There is a village over there.", ["あそこ", "に", "むら", "が", "あります"]),
    typed("ここ に クリーパー は い___。", "ません", "There's no creeper here."),
])

G("masu", "Polite verbs: ます・ません", "ます = do / will do; ません = don't / won't.", [
    "Polite verbs end in ます. The present form covers habits and the future: たべます = (I) eat / will eat.",
    "Negative: ます → ません. たべません = don't eat / won't eat.",
    "Japanese drops the subject when it's obvious, so you rarely need わたし.",
], [
    ("まいにち はしります。", "I run every day."),
    ("よる は ねません。", "I don't sleep at night."),
    ("あした いきます。", "I'll go tomorrow."),
], [
    choice("まいにち はしり___。", "ます", ["ます", "です", "ません", "ました"], "I run every day."),
    typed("よる は ね___。", "ません", "I don't sleep at night."),
    order("I'll go tomorrow.", ["あした", "いきます"]),
    meaning("きょう は たたかいません。", "I won't fight today.", ["I'll fight today.", "I fought today.", "Let's fight today."]),
])

G("wo", "を  (object marker)", "を marks the thing an action is done to.", [
    "を (pronounced 'o') marks the direct object: パン を たべます = eat bread.",
    "Typical order: [time] [topic は] [object を] [verb].",
], [
    ("ミルク を のみます。", "I drink milk."),
    ("てつ を ほります。", "I mine iron."),
    ("いえ を つくります。", "I'll build a house."),
], [
    choice("りんご ___ たべます。", "を", ["を", "は", "に", "の"], "I eat an apple."),
    order("I'll make a sword.", ["けん", "を", "つくります"]),
    choice("ダイヤモンド を ___。", "みつけます", ["みつけます", "います", "あります", "です"], "I'll find diamonds."),
    typed("みず ___ のみます。", "を", "I drink water.", ["お"]),
])

G("ni_he", "に・へ  (destination and time)", "Go/come/return に (or へ) a place; do something at a time に.", [
    "With movement verbs (いきます, きます, かえります) に or へ marks the destination: むら に いきます.",
    "に also marks a specific time: よる に = at night. Relative words like きょう, あした, まいにち take no particle.",
], [
    ("あした ネザー に いきます。", "Tomorrow I'll go to the Nether."),
    ("いえ に かえります。", "I'll go home."),
    ("エンド へ いきます。", "I'm going to the End."),
], [
    choice("むら ___ いきます。", "に", ["に", "を", "で", "の"], "I'll go to the village."),
    choice("あした どこ ___ いきます か。", "に", ["に", "を", "が", "の"], "Where will you go tomorrow?"),
    order("I'll go home.", ["いえ", "に", "かえります"]),
    meaning("エンド へ いきます。", "I'll go to the End.", ["I came from the End.", "I'll fight in the End.", "The End is far."]),
])

G("de", "で  (place of action, tool)", "Where an action happens, or what you do it with.", [
    "で marks where an action happens: もり で きのこ を あつめます = collect mushrooms in the forest.",
    "で also marks the tool or means: ツルハシ で ほります = dig with a pickaxe.",
    "Compare: に is where something IS or where you GO; で is where you DO something.",
], [
    ("どうくつ で てつ を ほります。", "I mine iron in the cave."),
    ("けん で たたかいます。", "I fight with a sword."),
    ("いえ で ねます。", "I sleep at home."),
], [
    choice("ツルハシ ___ いし を ほります。", "で", ["で", "に", "を", "の"], "I mine stone with a pickaxe."),
    choice("むら ___ パン を かいます。", "で", ["で", "に", "を", "へ"], "I buy bread in the village."),
    choice("どうくつ ___ ゾンビ が います。", "に", ["に", "で", "を", "へ"], "There are zombies in the cave.",
           "Existence (います) uses に, not で."),
    order("I fight with a sword.", ["けん", "で", "たたかいます"]),
    meaning("かわ で さかな を みつけます。", "I find fish in the river.",
            ["I eat fish by the river.", "The fish is in the river.", "I go to the river."]),
])

G("mashita", "Past: ました・ませんでした", "ました = did; ませんでした = didn't; でした = was.", [
    "Past: ます → ました. たべました = ate.",
    "Past negative: ません → ませんでした. たべませんでした = didn't eat.",
    "The past of です is でした.",
], [
    ("きのう ドラゴン を たおしました。", "Yesterday I defeated the dragon."),
    ("きのう は ねませんでした。", "I didn't sleep yesterday."),
    ("きのう は あめ でした。", "It was rainy yesterday."),
], [
    typed("きのう パン を たべ___。", "ました", "I ate bread yesterday."),
    choice("きのう ダイヤモンド を みつけ___。", "ました", ["ました", "ます", "ましょう", "たい"], "I found a diamond yesterday."),
    typed("きのう は いき___。", "ませんでした", "I didn't go yesterday."),
    order("I made a bed yesterday.", ["きのう", "ベッド", "を", "つくりました"]),
    choice("きのう は あめ ___。", "でした", ["でした", "ました", "です", "ません"], "It was rainy yesterday."),
])

G("ga_suki", "が with すき・きらい・じょうず", "I like X: X が すき です.", [
    "Likes, dislikes and skills mark the thing with が: わたし は ねこ が すき です = I like cats.",
    "すき (like), きらい (dislike), じょうず (good at) and へた (bad at) are な-adjectives.",
    "だいすき = love / really like.",
], [
    ("わたし は クッキー が すき です。", "I like cookies."),
    ("クリーパー が きらい です。", "I hate creepers."),
    ("ともだち は ゆみ が じょうず です。", "My friend is good with a bow."),
], [
    choice("わたし は ねこ ___ すき です。", "が", ["が", "を", "に", "で"], "I like cats."),
    order("I dislike creepers.", ["クリーパー", "が", "きらい", "です"]),
    choice("ともだち は ゆみ が ___ です。", "じょうず", ["じょうず", "へた", "すき", "きらい"], "My friend is skilled with a bow."),
    meaning("スライム が だいすき です。", "I love slimes.", ["I hate slimes.", "Slimes like me.", "Slimes are big."]),
])

G("mashou", "ましょう・ませんか  (let's / won't you?)", "Invitations and suggestions.", [
    "ましょう = let's: いきましょう = Let's go!",
    "ませんか = won't you...? - a polite invitation: いっしょ に いきませんか = Won't you come with me?",
    "ましょうか = shall we / shall I?",
], [
    ("いっしょ に ネザー に いきましょう。", "Let's go to the Nether together."),
    ("いえ を つくりませんか。", "Won't you build a house (with me)?"),
], [
    choice("いっしょ に たたかい___!", "ましょう", ["ましょう", "ました", "ません", "たい"], "Let's fight together!"),
    order("Won't you build a house together?", ["いっしょ", "に", "いえ", "を", "つくりませんか"]),
    typed("むら に いき___。", "ましょう", "Let's go to the village."),
    meaning("パン を たべませんか。", "Won't you eat some bread?", ["I don't eat bread.", "I didn't eat bread.", "Let's not eat bread."]),
])

G("tai", "たい  (want to)", "Verb stem + たい = want to.", [
    "Take the ます form, drop ます, add たい: たべます → たべたい (want to eat).",
    "It works like an い-adjective: たべたい です, たべたくない です (don't want to eat).",
    "Use it for your own wishes; asking others directly with たい can sound pushy.",
], [
    ("ダイヤモンド を みつけたい です。", "I want to find diamonds."),
    ("ネザー に いきたくない です。", "I don't want to go to the Nether."),
    ("なに を たべたい です か。", "What do you want to eat?"),
], [
    typed("ケーキ を たべ___ です。", "たい", "I want to eat cake."),
    choice("ドラゴン を たおし___ です。", "たい", ["たい", "ます", "ました", "ない"], "I want to defeat the dragon."),
    order("I want to go to the village.", ["むら", "に", "いきたい", "です"]),
    meaning("ねたくない です。", "I don't want to sleep.", ["I want to sleep.", "I didn't sleep.", "Let's sleep."]),
])

G("te_kudasai", "て-form + ください  (please do)", "Requests, and the key て-form rules.", [
    "The て-form links actions and makes requests: ～て ください = please do ～.",
    "From the ます form - る-verbs: たべます → たべて, みます → みて. う-verbs: い/ち/り → って (かいます → かって); み/び/に → んで (のみます → のんで); き → いて (あるきます → あるいて, but いきます → いって); し → して.",
    "Irregular: します → して, きます → きて.",
], [
    ("たすけて ください!", "Please help me!"),
    ("りんご を あつめて ください。", "Please collect apples."),
    ("ここ で まって ください。", "Please wait here."),
], [
    choice("パン を たべ___ ください。", "て", ["て", "た", "ます", "たい"], "Please eat the bread."),
    choice("ちょっと まっ___ ください。", "て", ["て", "で", "た", "ち"], "Please wait a moment."),
    typed("みず を の___ ください。", "んで", "Please drink the water."),
    order("Please collect apples.", ["りんご", "を", "あつめて", "ください"]),
    choice("むら に ___ ください。", "いって", ["いって", "いきて", "いいて", "いて"], "Please go to the village."),
    meaning("いえ を まもって ください。", "Please protect the house.",
            ["I protected the house.", "Let's protect the house.", "Don't protect the house."]),
])

G("teiru", "ている  (is doing)", "て-form + います = ongoing action or state.", [
    "て-form + います describes an action in progress: たべて います = is eating.",
    "It also describes a resulting state: ねて います = is asleep; きて います = has come / is coming.",
], [
    ("クリーパー が きて います!", "A creeper is coming!"),
    ("いま ほって います。", "I'm mining right now."),
    ("ともだち は ねて います。", "My friend is sleeping."),
], [
    typed("いま パン を たべて ___。", "います", "I'm eating bread now."),
    choice("ゾンビ が ___ います!", "きて", ["きて", "くて", "きって", "きた"], "A zombie is coming!"),
    order("My friend is sleeping.", ["ともだち", "は", "ねて", "います"]),
    meaning("いま なに を して います か。", "What are you doing now?", ["What did you do?", "What will you do?", "Who is doing it now?"]),
])

G("adj_past", "Past adjectives: かった・でした", "い-adj: drop い + かった; な-adj and nouns: でした.", [
    "い-adjective past: drop い, add かったです: たのしい → たのしかったです (was fun).",
    "Past negative: drop い, add くなかったです. いい → よかったです.",
    "な-adjectives and nouns use でした: しずか でした.",
], [
    ("きのう は たのしかった です。", "Yesterday was fun."),
    ("ドラゴン は つよかった です。", "The dragon was strong."),
    ("むら は しずか でした。", "The village was quiet."),
], [
    typed("ネザー は あつ___ です。", "かった", "The Nether was hot."),
    choice("きのう は ___ です。", "よかった", ["よかった", "いかった", "よいでした", "いいでした"], "Yesterday was good."),
    order("The dragon was strong.", ["ドラゴン", "は", "つよかった", "です"]),
    meaning("どうくつ は くらくなかった です。", "The cave wasn't dark.", ["The cave was dark.", "The cave isn't dark.", "Was the cave dark?"]),
])

G("kara", "から  (because / from)", "Reason から, result. Also: place から = from.", [
    "[reason] から、[result]: あぶない です から、いえ に かえります = It's dangerous, so I'll go home.",
    "Noun + から = from: むら から きました = I came from the village.",
], [
    ("よる です から、 ねます。", "It's night, so I'll sleep."),
    ("くらい です から、 たいまつ を つかいます。", "It's dark, so I'll use a torch."),
    ("ネザー から かえりました。", "I came back from the Nether."),
], [
    choice("あめ です ___、 いえ に います。", "から", ["から", "まで", "で", "に"], "It's raining, so I'm at home."),
    order("It's dark, so I'll use a torch.", ["くらい", "です", "から", "たいまつ", "を", "つかいます"]),
    meaning("ゾンビ が います から、 あぶない です。", "There are zombies, so it's dangerous.",
            ["It's dangerous because there are no zombies.", "Zombies are dangerous.", "Zombies came from the cave."]),
    choice("どこ ___ きました か。", "から", ["から", "に", "で", "を"], "Where did you come from?"),
])


def kana_ids(prefix, romaji):
    return [f"{prefix}_{r}" for r in romaji.split()]


def words(name):
    return [i["id"] for i in gen_vocab.LESSONS[name]]


LESSONS = [
    ("hira_a", "Hiragana: あ い う え お", "hiragana", "Japanese has three scripts. Hiragana comes first: every sound in the language can be written with it. We start with the five vowels.", kana_ids("h", "a i u e o")),
    ("hira_ka", "Hiragana: か row", "hiragana", "Each row is a consonant + the five vowels: ka ki ku ke ko.", kana_ids("h", "ka ki ku ke ko")),
    ("words_1", "First words", "vocabulary", "You can already read real words! Each one is linked to something in Minecraft.", words("words_1")),
    ("hira_sa", "Hiragana: さ row", "hiragana", "Note し is 'shi', not 'si'.", kana_ids("h", "sa shi su se so")),
    ("hira_ta", "Hiragana: た row", "hiragana", "Note ち 'chi' and つ 'tsu'.", kana_ids("h", "ta chi tsu te to")),
    ("words_2", "Stone, dirt and iron", "vocabulary", "Words for the things you mine and craft first.", words("words_2")),
    ("hira_na", "Hiragana: な row", "hiragana", "", kana_ids("h", "na ni nu ne no")),
    ("hira_ha", "Hiragana: は row", "hiragana", "ふ is between 'fu' and 'hu' - blow gently.", kana_ids("h", "ha hi fu he ho")),
    ("words_3", "Animals and nature", "vocabulary", "", words("words_3")),
    ("hira_ma", "Hiragana: ま row", "hiragana", "", kana_ids("h", "ma mi mu me mo")),
    ("hira_ya", "Hiragana: や row", "hiragana", "Only three: や ゆ よ.", kana_ids("h", "ya yu yo")),
    ("words_4", "Mountains, mobs and weapons", "vocabulary", "", words("words_4")),
    ("hira_ra", "Hiragana: ら row", "hiragana", "The Japanese r is a light tap, halfway to an l or d.", kana_ids("h", "ra ri ru re ro")),
    ("hira_wa", "Hiragana: わ を ん", "hiragana", "The last three basic hiragana.", kana_ids("h", "wa wo n")),
    ("words_5", "Sky, tools and treasure", "vocabulary", "", words("words_5")),
    ("greetings_1", "Greetings", "phrases", "Whole phrases learned as chunks - you'll use them every day.", words("greetings_1")),
    ("hira_dakuten", "Hiragana: ゛ and ゜", "hiragana", "Two marks turn the kana you know into 25 more sounds.", ["h_ga", "h_za", "h_da", "h_ba", "h_pa"]),
    ("words_6", "Farm and village", "vocabulary", "", words("words_6")),
    ("greetings_2", "Polite phrases", "phrases", "", words("greetings_2")),
    ("hira_combos", "Hiragana: combos, small っ, long vowels", "hiragana", "The last hiragana rules. After this you can read anything written in hiragana!", ["h_yoon1", "h_yoon2", "h_yoon3", "h_sokuon", "h_long"]),
    ("numbers", "Numbers 1-100", "vocabulary", "", words("numbers")),
    ("words_7", "Crops and time", "vocabulary", "", words("words_7")),
    ("people", "People and languages", "vocabulary", "", words("people")),
    ("adjectives_1", "Describing things", "vocabulary", "い-adjectives: they all end in い.", words("adjectives_1")),
    ("grammar_1", "Grammar: X は Y です and questions", "grammar", "Your first sentences.", ["g_desu", "g_ka"]),
    ("pointing_words", "This, that, here, there", "vocabulary", "", words("pointing_words")),
    ("grammar_2", "Grammar: これ / それ and の", "grammar", "", ["g_kore", "g_no"]),
    ("grammar_3", "Grammar: も and じゃないです", "grammar", "", ["g_mo", "g_janai"]),
    ("grammar_4", "Grammar: い-adjectives", "grammar", "", ["g_adj_i"]),
    ("kata_1", "Katakana: ア and カ rows, ー", "katakana", "Katakana writes foreign words - including most Minecraft mobs! Same sounds as hiragana, new shapes.", kana_ids("k", "a i u e o ka ki ku ke ko") + ["k_choon"]),
    ("kata_2", "Katakana: サ and タ rows", "katakana", "Careful with シ/ツ and ソ/ン: watch the stroke direction.", kana_ids("k", "sa shi su se so ta chi tsu te to")),
    ("kata_3", "Katakana: ナ and ハ rows", "katakana", "", kana_ids("k", "na ni nu ne no ha hi fu he ho")),
    ("kata_4", "Katakana: マ and ヤ rows", "katakana", "", kana_ids("k", "ma mi mu me mo ya yu yo")),
    ("kata_5", "Katakana: ラ row, ワ ヲ ン", "katakana", "", kana_ids("k", "ra ri ru re ro wa wo n")),
    ("kata_6", "Katakana: marks, combos and new sounds", "katakana", "", ["k_dakuten", "k_handakuten", "k_yoon", "k_sokuon", "k_extended"]),
    ("katakana_mobs", "Mobs in katakana", "vocabulary", "Japanese Minecraft names these mobs with loanwords.", words("katakana_mobs")),
    ("katakana_things", "Blocks and tools in katakana", "vocabulary", "", words("katakana_things")),
    ("katakana_food", "Food and dimensions", "vocabulary", "", words("katakana_food")),
    ("adjectives_na", "な-adjectives", "vocabulary", "", words("adjectives_na")),
    ("grammar_5", "Grammar: な-adjectives", "grammar", "", ["g_adj_na"]),
    ("places", "Places", "vocabulary", "", words("places")),
    ("grammar_6", "Grammar: あります / います", "grammar", "", ["g_arimasu"]),
    ("time_words", "Time", "vocabulary", "", words("time_words")),
    ("verbs_1", "Action verbs", "vocabulary", "Verbs are taught in their polite ます form first.", words("verbs_1")),
    ("grammar_7", "Grammar: verbs and を", "grammar", "", ["g_masu", "g_wo"]),
    ("grammar_8", "Grammar: に, へ and で", "grammar", "", ["g_ni_he", "g_de"]),
    ("grammar_9", "Grammar: the past", "grammar", "", ["g_mashita"]),
    ("grammar_10", "Grammar: likes and invitations", "grammar", "", ["g_ga_suki", "g_mashou"]),
    ("grammar_11", "Grammar: want to, please do", "grammar", "", ["g_tai", "g_te_kudasai"]),
    ("grammar_12", "Grammar: ている, past adjectives, から", "grammar", "", ["g_teiru", "g_adj_past", "g_kara"]),
]


def write(name, data):
    with open(os.path.join(DIR, name), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")


def main():
    gen_kana.main()
    gen_vocab.main()
    write("grammar.json", {"items": GRAMMAR})
    write("lessons.json", {"lessons": [
        {"id": lid, "title": title, "stage": stage, "intro": intro, "items": items}
        for lid, title, stage, intro, items in LESSONS
    ]})
    print(f"wrote {len(GRAMMAR)} grammar points and {len(LESSONS)} lessons")


if __name__ == "__main__":
    main()
