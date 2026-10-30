# How NihongoCraft teaches (and why)

Each design decision in the mod follows a well-replicated finding from learning science or
second-language acquisition research. This page maps the features to the research.

## 1. Course order

| Stage | What | Why this order |
|---|---|---|
| 1 | **Hiragana**, one row at a time, with words after every two rows | Every Japanese sound can be written in hiragana, and textbooks (Genki, Minna no Nihongo) teach it first. Reading real words immediately makes the kana meaningful instead of abstract symbols. |
| 2 | **Dakuten, combinations, small っ, long vowels** | These are *rules* applied to kana you already know, so they are taught as rule groups (one card per group) rather than 60 more symbols. |
| 3 | **High-frequency vocabulary** tied to Minecraft | Learning the most frequent words first gives the most comprehension per word learned (Nation, 2001/2006). Words are also chosen because you can *see* them in the game. |
| 4 | **Grammar** in the standard beginner order: は/です → か → これ/の → も/じゃない → adjectives → あります/います → ます verbs → を/に/で → past → が/すき → ましょう → たい → て-form → ている → から | This is the sequence used by Genki I and the JLPT N5 syllabus: each point builds on the ones before it. |
| 5 | **Katakana** after the first grammar, then loanwords | Katakana is the same sound system with new shapes, so it is quick to learn once hiragana is solid. It unlocks Minecraft's loanword names (ゾンビ, クリーパー, ダイヤモンド). |
| 6 | **Kanji through words you already know** | A word's kanji only appears once you've remembered the spoken word for 3+ days. Meaning and sound are already in memory, so the kanji is one new link instead of three. |

**i+1 rule, enforced automatically.** `tools/validate_curriculum.py` checks that every word and
every example sentence only uses kana taught in earlier lessons. Learners are never asked to read
something they couldn't read. This is Krashen's (1985) *comprehensible input*: input just above
the learner's current level.

## 2. Spaced repetition

Every item becomes flashcards scheduled with an SM-2 algorithm (Wozniak, 1990; the algorithm
behind SuperMemo and Anki):

- New cards go through short **learning steps** (1 min, 10 min), then graduate to 1 day.
- Each successful review multiplies the interval by the card's *ease*, so well-known items come
  back rarely and hard ones come back often.
- Distributed practice beats cramming for long-term retention, a result replicated across
  hundreds of studies (Ebbinghaus, 1885; Cepeda et al., 2006 meta-analysis).
- **Daily new-item limit** (15 by default) keeps tomorrow's review load manageable and limits
  cognitive load (Sweller, 1988).
- **Mastery gating**: a new lesson unlocks only when 80% of the previous lesson's cards are past
  the learning stage, so material is never built on a shaky foundation.

## 3. Retrieval practice

Lessons teach about 5 items and then **immediately test them**. Reviews are tests, not re-reading.
The *testing effect* (Roediger & Karpicke, 2006; Karpicke & Roediger, 2008) shows that
recalling information strengthens memory far more than studying it again.

Questions require **recall** (typing the reading or meaning) more than recognition (multiple choice),
because harder retrieval produces stronger learning ("desirable difficulties", Bjork, 1994).
Multiple choice is used where it adds something: telling apart look-alike kana (さ/ち, シ/ツ, ソ/ン).
Distractors are chosen from **confusable** kana on purpose, which trains discrimination.

## 4. Both directions, and production

Vocabulary is tested **Japanese → English** (comprehension) and **English + picture → Japanese**
(production). Producing language forces learners to notice gaps in their knowledge
(Swain's output hypothesis, 1985). Speaking practice and voice answers add **spoken production**,
the part most self-study apps leave out.

## 5. Interleaving

Review sessions shuffle kana, words, grammar and question directions together instead of
practising one type in a block. Interleaved practice improves the ability to choose the right
response (Rohrer & Taylor, 2007; Kornell & Bjork, 2008).

## 6. Mnemonics and dual coding

- Every kana has a **picture mnemonic** ("き looks like a key"). Picture-based mnemonics speed up
  kana learning considerably (Quackenbush & Ohso, *Hiragana in 48 Minutes*, 1983/1990; the
  keyword method: Atkinson & Raugh, 1975).
- Words are shown with the **Minecraft item or mob they name**. Combining a picture with a word
  creates two memory traces instead of one (Paivio's dual coding theory, 1971/1986), and avoids
  translating through English.
- The word's **sound** is played after you answer (text-to-speech), linking spelling, sound and
  meaning.

## 7. Immediate, specific feedback

Every answer gets instant feedback with the correct answer, its reading, its meaning and the
mnemonic. Feedback that explains *what* was right is among the strongest influences on learning
(Hattie & Timperley, 2007). Typos in English meanings are forgiven, since a spelling slip says
nothing about memory; wrong kana are not, since they make a different word.

## 8. Immersion: the world turns Japanese as you learn

- Looking at a block or mob you've studied shows its Japanese name; item tooltips do the same.
  Only studied words are shown, so the labels are *comprehensible* input, not noise.
- Romaji under labels fades away once you know a word well, so it doesn't become a crutch.
- **Japanese quests** ("りんご を さんこ あつめて ください") give a reason to understand a sentence and
  act on it. This is task-based language teaching (Ellis, 2003): language used to get something done.

## 9. Motivation

Streaks, XP rewards, quest emeralds and kotodama (spoken word) spells are designed to keep you
playing. Sustained, regular study matters more than any single technique (Dörnyei, 2001).

## References

- Atkinson, R. C., & Raugh, M. R. (1975). An application of the mnemonic keyword method to the acquisition of a Russian vocabulary. *Journal of Experimental Psychology: Human Learning and Memory*, 1(2).
- Bjork, R. A. (1994). Memory and metamemory considerations in the training of human beings. In *Metacognition: Knowing about knowing*. MIT Press.
- Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in verbal recall tasks: A review and quantitative synthesis. *Psychological Bulletin*, 132(3).
- Dörnyei, Z. (2001). *Motivational Strategies in the Language Classroom*. Cambridge University Press.
- Ebbinghaus, H. (1885). *Über das Gedächtnis*.
- Ellis, R. (2003). *Task-based Language Learning and Teaching*. Oxford University Press.
- Hattie, J., & Timperley, H. (2007). The power of feedback. *Review of Educational Research*, 77(1).
- Karpicke, J. D., & Roediger, H. L. (2008). The critical importance of retrieval for learning. *Science*, 319(5865).
- Kornell, N., & Bjork, R. A. (2008). Learning concepts and categories: Is spacing the "enemy of induction"? *Psychological Science*, 19(6).
- Krashen, S. (1985). *The Input Hypothesis: Issues and Implications*. Longman.
- Nation, I. S. P. (2001). *Learning Vocabulary in Another Language*. Cambridge University Press.
- Nation, I. S. P. (2006). How large a vocabulary is needed for reading and listening? *Canadian Modern Language Review*, 63(1).
- Paivio, A. (1986). *Mental Representations: A Dual Coding Approach*. Oxford University Press.
- Quackenbush, H., & Ohso, M. (1990). *Hiragana in 48 Minutes*. Curriculum Development Centre.
- Roediger, H. L., & Karpicke, J. D. (2006). Test-enhanced learning. *Psychological Science*, 17(3).
- Rohrer, D., & Taylor, K. (2007). The shuffling of mathematics problems improves learning. *Instructional Science*, 35(6).
- Swain, M. (1985). Communicative competence: Some roles of comprehensible input and comprehensible output in its development. In *Input in Second Language Acquisition*. Newbury House.
- Sweller, J. (1988). Cognitive load during problem solving. *Cognitive Science*, 12(2).
- Wozniak, P. A. (1990). *Optimization of learning* (SuperMemo SM-2 algorithm).
