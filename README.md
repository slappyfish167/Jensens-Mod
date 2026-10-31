# NihongoCraft (Jensen's Japanese Mod)

A **Minecraft 1.21.1 NeoForge** mod that teaches Japanese while you play, from the very first
hiragana up to beginner (JLPT N5-level) grammar. It uses spaced repetition, retrieval practice
and offline voice recognition, and it labels the Minecraft world in Japanese as you learn.

> Why it teaches the way it does: see **[docs/PEDAGOGY.md](docs/PEDAGOGY.md)** (spaced repetition,
> the testing effect, i+1 comprehensible input, dual coding, interleaving, with references).

## Features

- **Research-ordered course**: 50 lessons covering 108 kana items (all hiragana and katakana,
  dakuten, combinations, small っ, long vowels), 261 words and 21 grammar points.
  - Four stages, in order: **hiragana** (with picture mnemonics), then **katakana**, then **words**
    (and grammar), then **kanji**. Words only use kana you've learned (checked automatically).
  - Vocabulary is high-frequency **and visible in Minecraft**: 木 tree, 石 stone, 剣 sword, ゾンビ, クリーパー, ダイヤモンド…
  - Grammar follows Genki I / JLPT N5 order, with every example set in Minecraft.
  - **Kanji come last, through words you already know**: once the word lessons are done, a word's kanji
    is taught after you've remembered the word for a few days (hub → **Kanji**).
- **Placement test**: already know some Japanese? An adaptive test samples a few items from each lesson
  in order and skips you past everything you can already do. It stops at the first lesson that gives you
  trouble, so it is only a couple of questions for a true beginner. Skipped items come back as reviews
  over the following days to catch any gaps.
- **Spaced repetition (SM-2)** with learning steps and mastery gating. An optional daily new-item limit
  is off by default; turn it on or off from the hub.
- **Study while you play (field study)**: once you know your kana, mining, collecting, attacking or looking
  at something whose word is due for review pops up a small prompt. Press **K** to answer on the spot
  (or to learn the word, if you haven't yet) and earn a little XP, then you're straight back in the game.
  Press **K** while looking at a mob or block to ask about it any time.
- **Reset progress** from the hub to start the course over (a backup of the old progress is kept).
- **Many question types**: type the reading (romaji is converted to kana as you type, so no
  Japanese keyboard is needed), type the meaning, pick the right kana among look-alikes, build
  sentences from word tiles, fill in particles, and **answer by speaking**.
- **Offline voice recognition** (Vosk; nothing is sent over the internet):
  - Answer production questions out loud.
  - **Speaking practice** mode.
  - **Kotodama (言霊) spells**: hold **V** in-world and say a word you've learned
    (ひかり → night vision, みず → water breathing, はやい → speed, つよい → strength, …).
- **Listening**: words and sentences are read aloud by your OS's Japanese voice.
- **Immersion**:
  - Look at a block or mob you've studied to see its Japanese name.
  - Item tooltips show the Japanese name too.
  - Romaji fades out as you master each word.
  - **Japanese collection quests** reward you with XP and emeralds.
- **Progress follows you**: it is saved per Minecraft account (not per world), in `.minecraft/nihongo/`.
- **Extensible**: the whole course is JSON in `assets/nihongo/curriculum/`, so a resource pack can add words.

## Getting started (in game)

1. Press **J** (or craft a **Japanese Textbook**: book + red dye, shapeless) to open the study hub.
2. Click **Learn** to start the first lesson. Each new item is taught, then tested right away.
   Already know some Japanese? Click **Placement test** first to skip ahead.
3. Come back for **Reviews** when they're due. The hub tells you when.
   Out in the world, reviews also come to you: when a prompt appears on the right of the screen, press **K**.
4. Optional: open **Voice & audio** to download the Japanese speech model (~50 MB, one time) and
   test your microphone and the speech voice.

| Key | Action |
|---|---|
| J | Open the study hub |
| V (hold) | Speak a kotodama spell |
| H | Show/hide the hint for the current quest |
| K | Answer a field-study prompt (or ask about what you're looking at) |
| Enter | Check answer / continue |
| 1-4 | Pick a multiple-choice answer |

### Hearing Japanese (text-to-speech)

The mod uses your operating system's speech voices:

- **Windows**: Settings → Time & language → Speech → *Add voices* → Japanese.
- **macOS**: System Settings → Accessibility → Spoken Content → System voice → add *Kyoko*.
- **Linux**: install `espeak-ng` (or set a custom command in the config, e.g. one that runs a better TTS engine).

### Voice recognition notes

- Uses [Vosk](https://alphacephei.com/vosk/) with the `vosk-model-small-ja-0.22` model, downloaded
  on request from alphacephei.com into `.minecraft/nihongo/voice/`.
- Supported on Windows x64, macOS and Linux x64 (the platforms Vosk ships native libraries for).
- Each recording is decoded twice: once against a short list of expected words (forgiving of
  learner accents) and once freely (so wrong words are still caught).

## Configuration

Mods → NihongoCraft → Config, or edit `config/nihongo-client.toml`:
- daily new-item limit (on/off and how many), unlock threshold, look-at labels, tooltips, romaji, quests,
  field-study prompts and how often they appear
- TTS on/off and a custom TTS command, voice recognition and spells

Per-world settings (`serverconfig/nihongo-server.toml`): spell cooldown, XP per correct answer, quest rewards.

The mod is needed on the **client**. Install it on the server too if you want the rewards and
spells; without it on the server, learning still works and rewards are simply skipped.

## Building

Requires JDK 21.

```bash
./gradlew build
```

The jar is written to `build/libs/`. Run a dev client with `./gradlew runClient`.

Course content is generated from the Python sources in `tools/`:

```bash
python3 tools/gen_course.py
```

```bash
python3 tools/validate_curriculum.py
```

The validator checks that each lesson only uses kana taught earlier. The unit tests
(`./gradlew test`) cover the kana converter, answer checking, the scheduler, and that every word
in the course can be typed from its romaji.

## Project layout

```
src/main/java/com/jensen/nihongo/
  learning/      pure-Java learning engine (no Minecraft code; unit-tested)
    Kana, AnswerChecker, Srs, Curriculum, LearningEngine, StudySession, Quests, McIndex
  client/        screens, HUD, tooltips, quests, keybinds
    voice/       Vosk speech recognition + model download
    tts/         OS text-to-speech
  network/       reward and spell packets
src/main/resources/assets/nihongo/curriculum/   kana.json, vocab.json, grammar.json, lessons.json
tools/           course generators and validator
docs/PEDAGOGY.md research basis
```

## Credits

- Speech recognition: [Vosk](https://github.com/alphacep/vosk-api) (Apache 2.0) and its Japanese model.
- Teaching method: see the references in [docs/PEDAGOGY.md](docs/PEDAGOGY.md).
