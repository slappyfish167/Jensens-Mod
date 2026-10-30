package com.jensen.nihongo.learning;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The course content: kana, vocabulary and grammar items, plus the ordered list of lessons that
 * introduces them. Loaded from JSON so resource packs can extend or replace it.
 */
public final class Curriculum {

    public enum Kind { KANA, VOCAB, GRAMMAR }

    /** Common base for anything that can be introduced in a lesson and reviewed. */
    public abstract static class Item {
        public String id;

        public abstract Kind kind();

        /** Card directions that exist for this item (e.g. "read", "pick", "meaning"). */
        public abstract List<String> directions();

        /** Japanese text worth reading aloud for this item, if any. */
        public abstract String speakText();
    }

    public static final class KanaItem extends Item {
        public String script = "hiragana";
        public String kana;
        public String romaji;
        public List<String> alt = new ArrayList<>();
        public String mnemonic = "";
        public String title;
        /** For grouped items (combination sounds, small っ...): [prompt, romaji] pairs. */
        public List<List<String>> group;
        public String example;

        @Override
        public Kind kind() {
            return Kind.KANA;
        }

        public boolean isGroup() {
            return group != null && !group.isEmpty();
        }

        @Override
        public List<String> directions() {
            return isGroup() ? List.of("read") : List.of("read", "pick");
        }

        @Override
        public String speakText() {
            return isGroup() ? group.get(0).get(0) : kana;
        }

        public String displayTitle() {
            if (title != null) return title;
            return kana + "  (" + romaji + ")";
        }
    }

    public static final class VocabItem extends Item {
        public String kana;
        public String kanji;
        /** Set false for words whose kanji are too rare to be worth drilling. */
        public boolean kanjiQuiz = true;
        public List<String> meanings = new ArrayList<>();
        public String pos = "noun";
        /** Disambiguates homophones on meaning cards, e.g. "number" for はち (8) vs はち (bee). */
        public String hint;
        /** Extra accepted readings, e.g. romaji alternatives. */
        public List<String> accept = new ArrayList<>();
        /** Minecraft ids this word labels: "item:minecraft:bread", "entity:minecraft:pig", globs allowed. */
        public List<String> mc = new ArrayList<>();
        public String icon;
        public String note = "";
        public Example example;
        /** Item id the player can be asked to collect in a quest. */
        public String collect;

        @Override
        public Kind kind() {
            return Kind.VOCAB;
        }

        @Override
        public List<String> directions() {
            return List.of("meaning", "reading");
        }

        @Override
        public String speakText() {
            return kana;
        }

        public boolean hasKanjiCard() {
            return kanji != null && !kanji.isEmpty() && kanjiQuiz;
        }

        public String written() {
            return kanji != null && !kanji.isEmpty() ? kanji : kana;
        }

        public String romaji() {
            return Kana.toRomaji(kana);
        }

        public List<String> acceptedReadings() {
            List<String> all = new ArrayList<>();
            all.add(kana);
            if (kanji != null && !kanji.isEmpty()) all.add(kanji);
            all.addAll(accept);
            return all;
        }
    }

    public static final class Example {
        public String jp;
        public String en;
        public String note;
    }

    public static final class Exercise {
        /** "choice", "order" or "type". */
        public String type = "choice";
        public String prompt = "";
        public String en = "";
        public String answer = "";
        public List<String> options = new ArrayList<>();
        public List<String> tiles = new ArrayList<>();
        public List<String> extra = new ArrayList<>();
        public List<String> accept = new ArrayList<>();
        public String explain = "";
    }

    public static final class GrammarPoint extends Item {
        public String title;
        public String summary = "";
        public List<String> explanation = new ArrayList<>();
        public List<Example> examples = new ArrayList<>();
        public List<Exercise> exercises = new ArrayList<>();

        @Override
        public Kind kind() {
            return Kind.GRAMMAR;
        }

        @Override
        public List<String> directions() {
            return List.of("ex");
        }

        @Override
        public String speakText() {
            return examples.isEmpty() ? null : examples.get(0).jp;
        }
    }

    public static final class Lesson {
        public String id;
        public String title;
        public String intro = "";
        public List<String> items = new ArrayList<>();
        public String stage = "";
    }

    private final Map<String, Item> items = new LinkedHashMap<>();
    private final List<Lesson> lessons = new ArrayList<>();
    private final Map<String, List<String>> confusables = new LinkedHashMap<>();
    private final Map<String, Lesson> lessonOfItem = new LinkedHashMap<>();

    public Map<String, Item> items() {
        return Collections.unmodifiableMap(items);
    }

    public Item item(String id) {
        return items.get(id);
    }

    public List<Lesson> lessons() {
        return Collections.unmodifiableList(lessons);
    }

    public Lesson lessonOf(String itemId) {
        return lessonOfItem.get(itemId);
    }

    /** Visually similar kana, used as tricky distractors. */
    public List<String> confusablesFor(String kana) {
        return confusables.getOrDefault(kana, List.of());
    }

    private static final Gson GSON = new Gson();

    public void loadKana(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        for (JsonElement e : root.getAsJsonArray("items")) {
            KanaItem k = GSON.fromJson(e, KanaItem.class);
            if (k.alt == null) k.alt = new ArrayList<>();
            items.put(k.id, k);
        }
        if (root.has("confusables")) {
            for (JsonElement e : root.getAsJsonArray("confusables")) {
                List<String> set = new ArrayList<>();
                for (JsonElement c : e.getAsJsonArray()) set.add(c.getAsString());
                for (String c : set) {
                    List<String> others = confusables.computeIfAbsent(c, x -> new ArrayList<>());
                    for (String o : set) if (!o.equals(c) && !others.contains(o)) others.add(o);
                }
            }
        }
    }

    public void loadVocab(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        for (JsonElement e : root.getAsJsonArray("items")) {
            VocabItem v = GSON.fromJson(e, VocabItem.class);
            if (v.meanings == null) v.meanings = new ArrayList<>();
            if (v.accept == null) v.accept = new ArrayList<>();
            if (v.mc == null) v.mc = new ArrayList<>();
            if (v.note == null) v.note = "";
            items.put(v.id, v);
        }
    }

    public void loadGrammar(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        for (JsonElement e : root.getAsJsonArray("items")) {
            GrammarPoint g = GSON.fromJson(e, GrammarPoint.class);
            if (g.explanation == null) g.explanation = new ArrayList<>();
            if (g.examples == null) g.examples = new ArrayList<>();
            if (g.exercises == null) g.exercises = new ArrayList<>();
            for (Exercise x : g.exercises) {
                if (x.options == null) x.options = new ArrayList<>();
                if (x.tiles == null) x.tiles = new ArrayList<>();
                if (x.extra == null) x.extra = new ArrayList<>();
                if (x.accept == null) x.accept = new ArrayList<>();
            }
            items.put(g.id, g);
        }
    }

    public void loadLessons(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray arr = root.getAsJsonArray("lessons");
        lessons.clear();
        lessonOfItem.clear();
        for (JsonElement e : arr) {
            Lesson l = GSON.fromJson(e, Lesson.class);
            if (l.items == null) l.items = new ArrayList<>();
            lessons.add(l);
            for (String id : l.items) lessonOfItem.putIfAbsent(id, l);
        }
    }

    /** Returns a list of problems (unknown ids etc.). Empty means the content is consistent. */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        for (Lesson l : lessons) {
            for (String id : l.items) {
                if (!items.containsKey(id)) problems.add("Lesson " + l.id + " references unknown item " + id);
            }
        }
        for (Item i : items.values()) {
            if (!lessonOfItem.containsKey(i.id)) problems.add("Item " + i.id + " is not in any lesson");
        }
        return problems;
    }
}
