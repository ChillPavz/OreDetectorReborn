#!/usr/bin/env python3
"""Audit the BUILT JARS for Stage 1, rather than the source tree.

Every failure this looks for is silent in game: a missing model or lang key renders as a purple
cube or a raw translation key, a pre-1.21.2 ingredient form makes a recipe uncraftable with only a
log line, and a loot modifier in the wrong jar simply never fires.
"""
import zipfile, json, io, re, sys

NS = "ore_detector_reborn"
VER = "2.0.0"
ok = True
def bad(m):
    global ok; ok = False; print("  FAIL: " + m)

src = io.open("common/src/main/java/com/chillpavz/oredetectorreborn/registry/ModItems.java",
              encoding="utf-8").read()
mats = re.findall(r'material\("([^"]+)"\)', src)
ids = mats + re.findall(r'create\("([^"]+)"', src)
print("items registered via ITEMS: %d (%d new materials: %s)" % (len(ids), len(mats), ", ".join(mats)))

for loader in ("fabric", "neoforge"):
    print("\n=== %s ===" % loader)
    z = zipfile.ZipFile("%s/build/libs/%s-%s-26.2-%s.jar" % (loader, NS, loader, VER))
    names = set(z.namelist())
    files = {n for n in names if not n.endswith("/")}      # skip zip directory entries
    lang = json.loads(z.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))

    for i in ids:
        defn_path = "assets/%s/items/%s.json" % (NS, i)
        if defn_path not in files:
            bad("item definition missing for '%s'" % i)
        else:
            # A component-driven item has a select model and no plain texture of its own, so only
            # demand a matching texture and model when the definition actually names one.
            model = json.loads(z.read(defn_path).decode("utf-8"))["model"]
            if model.get("type") == "minecraft:model":
                for p, label in [("assets/%s/textures/item/%s.png" % (NS, i), "texture"),
                                 ("assets/%s/models/item/%s.json" % (NS, i), "model")]:
                    if p not in files: bad("%s missing for '%s'" % (label, i))
        if "item.%s.%s" % (NS, i) not in lang: bad("lang key missing for '%s'" % i)
    print("  all %d items have a definition and a name: OK" % len(ids))

    for t in ("trial_chambers_supply", "trial_chambers_pot", "trial_chambers_reward"):
        p = "data/%s/loot_table/inject/%s.json" % (NS, t)
        if p not in files:
            bad("injection loot table missing: %s" % t); continue
        for pool in json.loads(z.read(p).decode("utf-8"))["pools"]:
            for e in pool["entries"]:
                if e["type"] == "minecraft:item" and not e["name"].startswith(NS + ":"):
                    bad("%s drops a non-mod item: %s" % (t, e["name"]))
    print("  3 injection loot tables present and drop only mod items: OK")

    for rp in sorted(p for p in files if "/recipe/" in p and p.endswith(".json")):
        d = json.loads(z.read(rp).decode("utf-8"))
        for ing in d.get("ingredients", []):
            if isinstance(ing, dict):
                bad("%s uses the pre-1.21.2 object ingredient form" % rp)
        if "item" in d.get("result", {}):
            bad("%s result uses 'item', should be 'id'" % rp)
    wc = "data/minecraft/recipe/wind_charge.json"
    if wc not in files:
        bad("wind charge override missing")
    elif "%s:breeze_powder" % NS not in json.loads(z.read(wc).decode("utf-8"))["ingredients"]:
        bad("wind charge override does not consume breeze powder")
    else:
        print("  recipes use the 26.2 ingredient form; wind_charge override present: OK")

    glms = sorted(p for p in files if "/loot_modifiers/" in p and p.endswith(".json"))
    if loader == "neoforge":
        if len(glms) != 3: bad("expected 3 loot modifiers, found %d: %s" % (len(glms), glms))
        else: print("  3 global loot modifiers present: OK")
        for g in glms:
            d = json.loads(z.read(g).decode("utf-8"))
            if d.get("type") != "neoforge:add_table": bad("%s: wrong modifier type" % g)
    else:
        if glms: bad("NeoForge loot modifiers leaked into the fabric jar: %s" % glms)
        else: print("  no NeoForge loot modifiers in the fabric jar: OK")
        c = "com/chillpavz/oredetectorreborn/fabric/loot/BreezeLootInjection.class"
        if c not in files: bad("Fabric injection class missing")
        else: print("  BreezeLootInjection class present: OK")

    if "com/chillpavz/oredetectorreborn/loot/LootInjections.class" not in files:
        bad("shared LootInjections class missing")

print("\n" + ("STAGE 1 AUDIT PASSED" if ok else "STAGE 1 AUDIT FAILED"))
sys.exit(0 if ok else 1)
