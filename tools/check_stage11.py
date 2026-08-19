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
# The Minecraft version is read from gradle.properties rather than hardcoded, so these
# scripts work unchanged on every backport branch.
MC = re.search(r"^minecraft_version=(.+)$",
               io.open("gradle.properties", encoding="utf-8").read(),
               re.M).group(1).strip()

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


# NOTE ON DEPTH, deliberately not asserted. Vanilla hides an unfinished advancement unless itself,
# its parent or its grandparent is done (AdvancementVisibilityEvaluator, VISIBILITY_DEPTH = 2), so
# anything more than two below the root appears only as the player approaches it. The tree was
# flattened to two levels once to make everything visible from the start, and the owner tried both
# and preferred the chains: the progression reads as a dependency chain, which is worth more than
# seeing every node up front. So depth is a free choice here and only the ROOT grant matters for
# the tab existing at all.
deepest = max(depth_of(node) for node in tree)
print("  %d advancements, deepest is %d below the root (chains, revealed as reached)"
      % (len(tree), deepest))

for node, data in tree.items():
    if node != root_id and data.get("parent") not in tree:
        bad("%s names a parent that does not exist: %s" % (node, data.get("parent")))
    if data.get("display", {}).get("hidden"):
        bad("%s is marked hidden, which keeps it out of the tab entirely" % node)
    if "display" not in data:
        bad("%s has no display block, so it is invisible in the tab whatever else is true" % node)

# Every node must actually hang off the root, or it forms a second tab nobody expects.
for node in tree:
    chain, seen = node, set()
    while tree[chain].get("parent") is not None:
        if chain in seen:
            bad("%s is part of a parent cycle" % node)
            break
        seen.add(chain)
        chain = tree[chain]["parent"]
    else:
        if chain != root_id:
            bad("%s hangs off %s rather than the mod's root, so it makes its own tab"
                % (node, chain))
print("  every advancement descends from the root: OK")

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

# --- EVERY config option needs a label AND a hover description ---
# Cloth falls back to the raw key for a missing label, and shows no tooltip at all for a
# missing one, so an option can look finished while telling the player nothing about what
# it does.
# The ANNOTATION is what makes cloth look a tooltip key up at all. Without
# @ConfigEntry.Gui.Tooltip the entry is given no tooltip, however many lang keys exist, so
# checking only the keys verifies the wrong half and passes on a screen with no hover text
# anywhere. count() defaults to 1 and selects "<key>.@Tooltip"; higher selects the indexed
# "<key>.@Tooltip[0]", "[1]", ... form, and the two must agree or the lines come out blank.
config_java = source(
    "fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/config/OreDetectorConfigData.java")
option_fields = re.findall(r"public int (\w+)\s*=", config_java)
tooltip_counts = {}
for annot, count, field in re.findall(
        r"@ConfigEntry\.Gui\.Tooltip(\(count\s*=\s*(\d+)\))?\s*public int (\w+)", config_java):
    tooltip_counts[field] = int(count) if count else 1
print()
print("  config options: %s" % ", ".join(option_fields))

# --- the built jars --------------------------------------------------------------------------------
for loader in ("fabric", "neoforge"):
    print("\n=== %s jar ===" % loader)
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-%s-%s.jar" % (loader, NS, loader, MC, VER))
    lang = json.loads(jar.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))
    for option in option_fields:
        base = "text.autoconfig.%s.option.%s" % (NS, option)
        if base not in lang:
            bad("%s has no label, so the slider shows its raw key" % base)
        if option not in tooltip_counts:
            bad("%s has no @ConfigEntry.Gui.Tooltip annotation, so cloth never looks up a tooltip for it" % option)
            continue
        count = tooltip_counts[option]
        if count == 1:
            if base + ".@Tooltip" not in lang:
                bad("%s.@Tooltip is missing, so the option explains nothing" % base)
        else:
            for i in range(count):
                if "%s.@Tooltip[%d]" % (base, i) not in lang:
                    bad("%s.@Tooltip[%d] is missing; count = %d promises that many lines"
                        % (base, i, count))
            if "%s.@Tooltip[%d]" % (base, count) in lang:
                bad("%s has a @Tooltip[%d] line that count = %d will never show"
                    % (base, count, count))
    print("  all %d config options have a label and a description: OK"
          % len(option_fields))

    # A label left behind for an option that no longer exists reads as a missing feature
    # to anyone grepping for it.
    prefix = "text.autoconfig.%s.option." % NS
    labelled = {k[len(prefix):] for k in lang if k.startswith(prefix) and "@" not in k}
    for stale in sorted(labelled - set(option_fields)):
        bad("there is a label for %r but no such config option" % stale)

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
