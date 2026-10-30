package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.AnswerChecker.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerCheckerTest {
    @Test
    void readingsMustMatch() {
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("ha", List.of("は")).verdict());
        assertEquals(Verdict.WRONG, AnswerChecker.checkJapanese("wa", List.of("は")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("si", List.of("し")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("ネコ", List.of("ねこ")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("木", List.of("き", "木")).verdict());
        assertEquals(Verdict.WRONG, AnswerChecker.checkJapanese("neko", List.of("いぬ")).verdict());
        assertEquals(Verdict.RETRY, AnswerChecker.checkJapanese("  ", List.of("いぬ")).verdict());
    }

    @Test
    void particlesAreComparedBySoundInLongerAnswers() {
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("konnichiwa", List.of("こんにちは")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkJapanese("keeki", List.of("ケーキ")).verdict());
    }

    @Test
    void meaningsTolerateTypos() {
        assertEquals(Verdict.CORRECT, AnswerChecker.checkMeaning("Tree", List.of("tree", "wood")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkMeaning("to eat", List.of("eat")).verdict());
        assertEquals(Verdict.CLOSE, AnswerChecker.checkMeaning("mountian", List.of("mountain")).verdict());
        assertEquals(Verdict.WRONG, AnswerChecker.checkMeaning("cat", List.of("dog")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkMeaning("free", List.of("free (time)")).verdict());
        assertEquals(Verdict.RETRY, AnswerChecker.checkMeaning("いぬ", List.of("dog")).verdict());
    }

    @Test
    void speechMatchesKanaOrKanjiAnywhere() {
        assertEquals(Verdict.CORRECT, AnswerChecker.checkSpoken(List.of("光"), List.of("ひかり", "光")).verdict());
        assertEquals(Verdict.CORRECT, AnswerChecker.checkSpoken(List.of("えっと ゾンビ"), List.of("ゾンビ")).verdict());
        assertEquals(Verdict.WRONG, AnswerChecker.checkSpoken(List.of("ねこ"), List.of("いぬ")).verdict());
        assertEquals(Verdict.RETRY, AnswerChecker.checkSpoken(List.of(""), List.of("いぬ")).verdict());
    }
}
