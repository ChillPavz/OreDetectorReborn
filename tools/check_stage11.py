#!/usr/bin/env python3
"""Audit the loader interaction contract, the advancement tree and the strain options.

Everything here failed silently in a real test, on one loader only or on both.

The Fabric one is the reason this file exists. Fabric's ItemEvents.USE treats NULL as "not
handled" and returns ANY non-null result straight to the caller, skipping the item's own use().
Returning InteractionResult.PASS therefore CANCELS the vanilla interaction. It broke equipping
the goggles, pouring liquid into the detector, emptying the Nullified Bucket and draining into a
cauldron, all on Fabric only, with nothing logged. NeoForge's event reads PASS correctly, so
testing there proved nothing about it.
"""
import io, json, os, re, sys, zipfile

NS, VER = "ore_detector_reborn", "2.0.0"
ok = True


def bad(message):
    global ok
    ok = False
    print("  FAIL: " + message)


def source(path):
    text = io.open(path, encoding="utf-8").read()
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    text = re.sub(r"//[^\n]*", "", text)
    return text


# --- the Fabric use-event contract ------------------------------------------------------------
fabric_main = source("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/OreDetectorFabric.java")
# Checked as two specific facts rather than by extracting a block: a non-greedy regex over the
# whole file happily runs past the end of this registration into the next one and then reports
# whatever it finds there, which is a misleading pass or a misleading message.
if "ItemEvents.USE.register(" not in fabric_main:
    bad("ItemEvents.USE is not registered at all, so grinding does nothing on Fabric")
elif "ItemEvents.USE.register(OreGrindingInteraction::tryGrind)" in fabric_main:
    bad("ItemEvents.USE is registered with a bare method reference, so it hands Fabric "
        "InteractionResult.PASS. Fabric returns any non-null result INSTEAD of running the item's "
        "own use(), so that cancels equipping, pouring, bucket emptying and draining")
elif "PASS ? null" not in fabric_main.replace("InteractionResult.", ""):
    bad("the ItemEvents.USE handler does not translate PASS to null, so Fabric treats every "
        "right-click as handled and cancels the item's own use()")
else:
    print("  Fabric ItemEvents.USE maps PASS to null: OK")

# NeoForge's is the mirror image: it must CANCEL only on a non-PASS result.
neo_main = source("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/OreDetectorNeoForge.java")
if "result != InteractionResult.PASS" not in neo_main:
    bad("NeoForge's grind handler does not gate its cancel on a non-PASS result")
else:
    print("  NeoForge cancels only on a real grind: OK")

# --- the advancement tree ------------------------------------------------------------------------
ADV = "common/src/main/resources/data/%s/advancement" % NS
tree = {}
for name in os.listdir(ADV):
    if not name.endswith(".json"):
        continue
    data = json.loads(io.open(os.path.join(ADV, name), encoding="utf-8").read())
    tree[NS + ":" + name[:-5]] = data

root_id = NS + ":root"
root = tree[root_id]
triggers = {c.get("trigger") for c in root.get("criteria", {}).values()}
# The tab does not exist for a player until the root is earned, so a root that needs an item hides
# the whole progression behind finding the thing the progression exists to explain.
if triggers != {"minecraft:tick"}:
    bad("the root advancement is not granted on the first tick (triggers: %s), so the tab stays "
        "hidden until the player earns something" % sorted(triggers))
else:
    print("\n  root is granted on the first tick, so the tab always exists: OK")

depths = {}


def depth_of(node):
    if node in depths:
        return depths[node]
    parent = tree[node].get("parent")
    depths[node] = 0 if parent is None else depth_of(parent) + 1
    return depths[node]


# Vanilla hides an unfinished advancement unless itself, its PARENT or its GRANDPARENT is done
# (AdvancementVisibilityEvaluator, VISIBILITY_DEPTH = 2). With the root granted that covers depth
# 0, 1 and 2 and nothing deeper, so a chain three or more below the root is invisible until the
# player is already most of the way along it.
MAX_DEPTH = 2
deepest = 0
for node in tree:
    d = depth_of(node)
    deepest = max(deepest, d)
    if d > MAX_DEPTH:
        bad("%s sits %d below the root; anything past %d is hidden until nearby progress"
            % (node, d, MAX_DEPTH))
print("  %d advancements, deepest is %d below the root: OK" % (len(tree), deepest))

for node, data in tree.items():
    if node != root_id and data.get("parent") not in tree:
        bad("%s names a parent that does not exist: %s" % (node, data.get("parent")))
    if data.get("display", {}).get("hidden"):
        bad("%s is marked hidden, which keeps it out of the tab entirely" % node)

# --- the strain options must exist identically on BOTH loaders -----------------------------------
OPTIONS = ["nauseaSeconds", "strainPerScan", "strainDecayTicks"]
print()
for loader in ("fabric", "neoforge"):
    path = ("%s/src/main/java/com/chillpavz/oredetectorreborn/%s/config/OreDetectorConfigData.java"
            % (loader, loader))
    text = source(path)
    for option in OPTIONS:
        if ("public int %s" % option) not in text:
            bad("%s's config screen is missing %s" % (loader, option))
    if "applyStrain(" not in text:
        bad("%s never pushes the strain options into the runtime holder, so moving the sliders "
            "changes nothing" % loader)
    else:
        print("  %-8s exposes and applies all %d strain options: OK" % (loader, len(OPTIONS)))

# --- the built jars --------------------------------------------------------------------------------
for loader in ("fabric", "neoforge"):
    print("\n=== %s jar ===" % loader)
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-26.2-%s.jar" % (loader, NS, loader, VER))
    lang = json.loads(jar.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))
    for option in OPTIONS:
        key = "text.autoconfig.%s.option.%s" % (NS, option)
        if key not in lang:
            bad("%s has no label, so the slider shows its raw key" % key)
    print("  all %d strain options have labels: OK" % len(OPTIONS))

    # An element-less model still needs a particle texture, or the loader warns on every reload.
    hidden = json.loads(jar.read("assets/%s/models/item/goggles_hidden.json" % NS).decode("utf-8"))
    particle = hidden.get("textures", {}).get("particle")
    if not particle:
        bad("goggles_hidden declares no particle texture, which warns on every resource reload")
    else:
        texture = "assets/%s/textures/%s.png" % (particle.split(":")[0], particle.split(":")[1])
        if texture not in jar.namelist():
            bad("goggles_hidden's particle texture %s is not in the jar" % particle)
        else:
            print("  goggles_hidden's particle texture resolves: OK")

    for name in tree:
        entry = "data/%s/advancement/%s.json" % (NS, name.split(":")[1])
        if entry not in jar.namelist():
            bad("%s is missing from the %s jar" % (entry, loader))
    print("  all %d advancements shipped: OK" % len(tree))

print("\nSTAGE 11 AUDIT " + ("PASSED" if ok else "FAILED"))
sys.exit(0 if ok else 1)
