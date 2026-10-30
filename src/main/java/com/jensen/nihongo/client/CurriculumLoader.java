package com.jensen.nihongo.client;

import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.learning.Curriculum;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Loads the course from assets/nihongo/curriculum/. Because it is a client resource, a resource
 * pack can replace or extend the lessons without touching code.
 */
public class CurriculumLoader extends SimplePreparableReloadListener<Curriculum> {

    @Override
    protected Curriculum prepare(ResourceManager manager, ProfilerFiller profiler) {
        Curriculum c = new Curriculum();
        load(manager, "kana.json", c::loadKana);
        load(manager, "vocab.json", c::loadVocab);
        load(manager, "grammar.json", c::loadGrammar);
        load(manager, "lessons.json", c::loadLessons);
        List<String> problems = c.validate();
        for (String p : problems) NihongoMod.LOGGER.warn("Curriculum: {}", p);
        return c;
    }

    private static void load(ResourceManager manager, String file, java.util.function.Consumer<Reader> loader) {
        var id = NihongoMod.id("curriculum/" + file);
        // Every pack providing the file is loaded in order, so packs can add items as well as override them.
        // Lessons are replaced (not merged) by the top-most pack.
        List<Resource> stack = manager.getResourceStack(id);
        if (stack.isEmpty()) {
            NihongoMod.LOGGER.error("Missing curriculum file {}", id);
            return;
        }
        BiConsumer<Resource, String> read = (res, name) -> {
            try (Reader reader = res.openAsReader()) {
                loader.accept(reader);
            } catch (Exception e) {
                NihongoMod.LOGGER.error("Failed to read curriculum file {} from {}", name, res.sourcePackId(), e);
            }
        };
        if (file.equals("lessons.json")) {
            read.accept(stack.get(stack.size() - 1), file);
        } else {
            for (Resource res : stack) read.accept(res, file);
        }
    }

    @Override
    protected void apply(Curriculum curriculum, ResourceManager manager, ProfilerFiller profiler) {
        Learner.onCurriculumLoaded(curriculum);
    }
}
