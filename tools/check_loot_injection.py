#!/usr/bin/env python3
"""Assert the Fabric and NeoForge loot injections describe the same thing.

The loot itself lives in one JSON per injection under
common/src/main/resources/data/<ns>/loot_table/inject/. Fabric reaches those tables through the
Java map in LootInjections, NeoForge through a neoforge:add_table modifier per JSON file. Nothing
in the build makes the two agree, and a mismatch is silent on both loaders: the loot simply never
appears on one of them. This checks they match, and that every table they name exists.

Run: python tools/check_loot_injection.py
"""
import io, json, os, re, sys

NS = "ore_detector_reborn"
JAVA = "common/src/main/java/com/chillpavz/oredetectorreborn/loot/LootInjections.java"
GLM = "neoforge/src/main/resources/data/%s/loot_modifiers" % NS
TABLE_DIR = "common/src/main/resources/data/%s/loot_table" % NS

fail = []

# --- Fabric side: parse the add(...) calls out of the shared map ---------------------------
src = io.open(JAVA, encoding="utf-8").read()
src = re.sub(r"//.*", "", src)                      # ignore commented-out entries
java_pairs = {
    (m.group(1) + ":" + m.group(2), NS + ":" + m.group(3))
    for m in re.finditer(r'add\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', src)
}
if not java_pairs:
    fail.append("parsed zero injections out of %s - has the add() signature changed?" % JAVA)

# --- NeoForge side: parse the global loot modifiers ----------------------------------------
neo_pairs = set()
if not os.path.isdir(GLM):
    fail.append("no loot_modifiers directory at %s" % GLM)
else:
    for f in sorted(os.listdir(GLM)):
        if not f.endswith(".json"):
            continue
        d = json.load(io.open(os.path.join(GLM, f), encoding="utf-8"))
        if d.get("type") != "neoforge:add_table":
            fail.append("%s: type is %r, expected 'neoforge:add_table'" % (f, d.get("type")))
            continue
        conds = [c for c in d.get("conditions", [])
                 if c.get("condition") == "neoforge:loot_table_id"]
        if len(conds) != 1:
            fail.append("%s: expected exactly one neoforge:loot_table_id condition, found %d"
                        % (f, len(conds)))
            continue
        neo_pairs.add((conds[0]["loot_table_id"], d["table"]))

# --- compare -------------------------------------------------------------------------------
for pair in sorted(java_pairs - neo_pairs):
    fail.append("injected on Fabric but NOT on NeoForge: %s -> %s" % pair)
for pair in sorted(neo_pairs - java_pairs):
    fail.append("injected on NeoForge but NOT on Fabric: %s -> %s" % pair)

# --- every table named must actually exist -------------------------------------------------
for _, ours in sorted(java_pairs | neo_pairs):
    ns, path = ours.split(":", 1)
    if ns != NS:
        continue
    p = os.path.join(TABLE_DIR, path + ".json")
    if not os.path.isfile(p):
        fail.append("names a loot table that does not exist: %s (expected %s)" % (ours, p))

print("Fabric injections:   %d" % len(java_pairs))
print("NeoForge injections: %d" % len(neo_pairs))
for pair in sorted(java_pairs & neo_pairs):
    print("  both loaders: %s -> %s" % pair)
if fail:
    print("\nFAILED:")
    for f in fail:
        print("  " + f)
    sys.exit(1)
print("\nloot injection is consistent across both loaders")
