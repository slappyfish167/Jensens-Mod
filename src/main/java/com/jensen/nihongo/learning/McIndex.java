package com.jensen.nihongo.learning;

import com.jensen.nihongo.learning.Curriculum.VocabItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Maps Minecraft registry ids (items, blocks, entities) to vocabulary, so the world itself can be
 * labelled in Japanese. Patterns look like "item:minecraft:bread", "entity:minecraft:pig",
 * "minecraft:*_log" (any type) and support '*' wildcards.
 */
public final class McIndex {
    public enum Type { ITEM, BLOCK, ENTITY }

    private record Rule(Type type, Pattern pattern, VocabItem vocab) {}

    private final Map<String, List<VocabItem>> exact = new HashMap<>();
    private final List<Rule> globs = new ArrayList<>();
    private final Map<String, List<VocabItem>> cache = new HashMap<>();

    public McIndex(Curriculum curriculum) {
        for (Curriculum.Item i : curriculum.items().values()) {
            if (!(i instanceof VocabItem v)) continue;
            for (String raw : v.mc) add(raw, v);
        }
    }

    private void add(String raw, VocabItem v) {
        Type type = null;
        String id = raw;
        for (Type t : Type.values()) {
            String prefix = t.name().toLowerCase(java.util.Locale.ROOT) + ":";
            if (raw.startsWith(prefix)) {
                type = t;
                id = raw.substring(prefix.length());
            }
        }
        if (!id.contains(":")) id = "minecraft:" + id;
        if (id.contains("*")) {
            String regex = Pattern.quote(id).replace("*", "\\E.*\\Q");
            globs.add(new Rule(type, Pattern.compile(regex), v));
        } else if (type == null) {
            for (Type t : Type.values()) exact.computeIfAbsent(key(t, id), k -> new ArrayList<>()).add(v);
        } else {
            exact.computeIfAbsent(key(type, id), k -> new ArrayList<>()).add(v);
        }
    }

    private static String key(Type t, String id) {
        return t.name() + "|" + id;
    }

    /** All vocabulary that labels this thing, most specific (exact) matches first. */
    public List<VocabItem> lookup(Type type, String id) {
        String k = key(type, id);
        List<VocabItem> hit = cache.get(k);
        if (hit != null) return hit;
        List<VocabItem> out = new ArrayList<>(exact.getOrDefault(k, List.of()));
        for (Rule r : globs) {
            if ((r.type == null || r.type == type) && r.pattern.matcher(id).matches() && !out.contains(r.vocab)) {
                out.add(r.vocab);
            }
        }
        cache.put(k, out);
        return out;
    }
}
