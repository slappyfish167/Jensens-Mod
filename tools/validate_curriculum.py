"""Checks the course content for consistency.

- every lesson item exists and every item is in exactly one lesson
- (i+1 rule) words and sentences only use kana taught in earlier lessons (or the same lesson,
  for the kana being taught)
- choice exercises contain their answer; order exercises have tiles

Run: python3 tools/validate_curriculum.py
"""
import json
import os
import sys

DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nihongo", "curriculum")


def load(name):
    with open(os.path.join(DIR, name), encoding="utf-8") as f:
        return json.load(f)


def is_kana(c):
    return "ぁ" <= c <= "ゖ" or "ァ" <= c <= "ヺ" or c == "ー"


def kana_chars(text):
    return {c for c in text if is_kana(c)}


def main():
    kana = {i["id"]: i for i in load("kana.json")["items"]}
    vocab = {i["id"]: i for i in load("vocab.json")["items"]}
    grammar = {i["id"]: i for i in load("grammar.json")["items"]}
    lessons = load("lessons.json")["lessons"]
    items = {**kana, **vocab, **grammar}
    problems = []

    if len(items) != len(kana) + len(vocab) + len(grammar):
        problems.append("duplicate ids across kana/vocab/grammar")

    seen = {}
    for lesson in lessons:
        for iid in lesson["items"]:
            if iid not in items:
                problems.append(f"{lesson['id']}: unknown item {iid}")
            if iid in seen:
                problems.append(f"{iid} appears in both {seen[iid]} and {lesson['id']}")
            seen[iid] = lesson["id"]
    for iid in items:
        if iid not in seen:
            problems.append(f"{iid} is not in any lesson")

    known = set()
    for lesson in lessons:
        # Kana taught in this lesson become known for the rest of it.
        for iid in lesson["items"]:
            k = kana.get(iid)
            if k:
                known |= kana_chars(k["kana"])
                for member in k.get("group", []):
                    known |= kana_chars(member[0])
        for iid in lesson["items"]:
            texts = []
            if iid in vocab:
                v = vocab[iid]
                texts.append(v["kana"])
                if "example" in v:
                    texts.append(v["example"]["jp"])
            elif iid in grammar:
                g = grammar[iid]
                texts += [e["jp"] for e in g["examples"]]
                for ex in g["exercises"]:
                    texts += [ex.get("prompt", "")] + ex.get("tiles", []) + ex.get("extra", [])
                    if ex["type"] == "choice" and any(is_kana(c) for c in ex["answer"]):
                        texts += ex.get("options", [])
                    if ex["type"] == "choice" and ex["answer"] not in ex.get("options", []):
                        problems.append(f"{iid}: answer '{ex['answer']}' missing from options")
                    if ex["type"] == "order" and not ex.get("tiles"):
                        problems.append(f"{iid}: order exercise without tiles")
            for t in texts:
                unknown = kana_chars(t) - known
                if unknown:
                    problems.append(f"{lesson['id']}/{iid}: '{t}' uses kana not taught yet: {''.join(sorted(unknown))}")

    for v in vocab.values():
        if not v.get("meanings"):
            problems.append(f"{v['id']}: no meanings")
        if any(not is_kana(c) for c in v["kana"]):
            problems.append(f"{v['id']}: kana field contains non-kana: {v['kana']}")

    if problems:
        print(f"{len(problems)} problem(s):")
        for p in problems:
            print("  - " + p)
        sys.exit(1)
    print(f"OK: {len(kana)} kana, {len(vocab)} words, {len(grammar)} grammar points, {len(lessons)} lessons")


if __name__ == "__main__":
    main()
