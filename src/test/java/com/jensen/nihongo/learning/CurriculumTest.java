package com.jensen.nihongo.learning;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurriculumTest {
    static final Path DIR = Path.of("src/main/resources/assets/nihongo/curriculum");

    static Curriculum load() throws IOException {
        Curriculum c = new Curriculum();
        try (Reader r = Files.newBufferedReader(DIR.resolve("kana.json"), StandardCharsets.UTF_8)) {
            c.loadKana(r);
        }
        try (Reader r = Files.newBufferedReader(DIR.resolve("vocab.json"), StandardCharsets.UTF_8)) {
            c.loadVocab(r);
        }
        try (Reader r = Files.newBufferedReader(DIR.resolve("grammar.json"), StandardCharsets.UTF_8)) {
            c.loadGrammar(r);
        }
        try (Reader r = Files.newBufferedReader(DIR.resolve("lessons.json"), StandardCharsets.UTF_8)) {
            c.loadLessons(r);
        }
        return c;
    }

    @Test
    void contentIsConsistent() throws IOException {
        Curriculum c = load();
        assertEquals(List.of(), c.validate());
        assertFalse(c.lessons().isEmpty());
    }

    @Test
    void everySingleKanaCanBeTypedFromItsRomaji() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Curriculum.Item i : load().items().values()) {
            if (i instanceof Curriculum.KanaItem k && !k.isGroup()) {
                if (!AnswerChecker.checkJapanese(k.romaji, List.of(k.kana)).passed()) bad.add(k.id);
                for (String alt : k.alt) {
                    List<String> accepted = new ArrayList<>(List.of(k.kana));
                    accepted.addAll(k.alt);
                    if (!AnswerChecker.checkJapanese(alt, accepted).passed()) bad.add(k.id + "/" + alt);
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyGroupMemberCanBeTypedFromItsRomaji() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Curriculum.Item i : load().items().values()) {
            if (i instanceof Curriculum.KanaItem k && k.isGroup()) {
                for (List<String> m : k.group) {
                    String typed = Kana.normalize(Kana.romajiToHiragana(m.get(1), true));
                    boolean ok = typed.equals(Kana.normalize(m.get(0)))
                            || AnswerChecker.checkJapanese(m.get(1), List.of(m.get(0), m.get(1))).passed();
                    if (!ok) bad.add(k.id + ":" + m);
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    /** Every word's romanisation must convert back to the same kana, or learners couldn't type it. */
    @Test
    void everyWordRoundTripsThroughRomaji() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Curriculum.Item i : load().items().values()) {
            if (i instanceof Curriculum.VocabItem v) {
                String romaji = v.romaji();
                if (!AnswerChecker.checkJapanese(romaji, v.acceptedReadings()).passed()) bad.add(v.id + " (" + romaji + ")");
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void grammarExercisesAreAnswerable() throws IOException {
        for (Curriculum.Item i : load().items().values()) {
            if (i instanceof Curriculum.GrammarPoint g) {
                assertFalse(g.exercises.isEmpty(), g.id);
                for (Curriculum.Exercise ex : g.exercises) {
                    if (ex.type.equals("choice")) assertTrue(ex.options.contains(ex.answer), g.id + ": " + ex.prompt);
                    if (ex.type.equals("order")) assertFalse(ex.tiles.isEmpty(), g.id);
                    if (ex.type.equals("type")) {
                        assertTrue(AnswerChecker.checkJapanese(Kana.toRomaji(ex.answer), List.of(ex.answer)).passed(), g.id + ": " + ex.answer);
                    }
                }
            }
        }
    }

    @Test
    void minecraftIdsResolve() throws IOException {
        McIndex index = new McIndex(load());
        assertEquals("v_ki", index.lookup(McIndex.Type.BLOCK, "minecraft:oak_log").get(0).id);
        assertEquals("v_ono", index.lookup(McIndex.Type.ITEM, "minecraft:iron_axe").get(0).id);
        assertEquals("v_tsuruhashi", index.lookup(McIndex.Type.ITEM, "minecraft:iron_pickaxe").get(0).id);
        assertEquals("v_niwatori", index.lookup(McIndex.Type.ENTITY, "minecraft:chicken").get(0).id);
        assertEquals("v_niku", index.lookup(McIndex.Type.ITEM, "minecraft:chicken").get(0).id);
        assertTrue(index.lookup(McIndex.Type.ITEM, "minecraft:oak_trapdoor").isEmpty());
    }
}
