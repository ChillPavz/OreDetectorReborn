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

NS = "ore_detector_reborn"
# Read from gradle.properties, never hardcoded: a version bump used to break every jar
# path in here, which reads as the jars being missing rather than as a stale constant.
VER = re.search(r"^version=(.+)$", io.open("gradle.properties", encoding="utf-8").read(), re.M).group(1).strip()
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


# --- the Fabric block-event contract ----------------------------------------------------------
# Grinding moved from a shears gesture on ItemEvents.USE to a sneak-click on a grindstone through
# UseBlockCallback, and THE TWO EVENTS DO NOT SHARE A CONTRACT. Both were read out of the mixin
# bytecode, not assumed:
#   ItemEvents.USE     -> NULL means "not handled"; ANY non-null result is returned to the caller
#                         instead of running the item's own use(). Returning PASS there cancelled
#                         equipping, pouring, bucket emptying and draining, on Fabric only.
#   UseBlockCallback   -> PASS means "not handled", the ordinary reading, and it is injected at the
#                         HEAD of ServerPlayerGameMode.useItemOn so it fires while sneaking too.
# So the null translation that was MANDATORY on the old event is now a BUG on the new one, which is
# why this check is inverted rather than deleted.
fabric_main = source("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/OreDetectorFabric.java")
# Checked as specific facts rather than by extracting a block: a non-greedy regex over the whole
# file happily runs past the end of this registration into the next one and then reports whatever
# it finds there, which is a misleading pass or a misleading message.
if "UseBlockCallback.EVENT.register(" not in fabric_main:
    bad("UseBlockCallback is not registered at all, so the grinder never opens on Fabric")
elif "ItemEvents.USE.register(" in fabric_main:
    bad("ItemEvents.USE is still registered. Grinding no longer rides on it, and a handler left "
        "there returning PASS cancels the vanilla interaction for every item in the game")
elif "PASS ? null" in fabric_main.replace("InteractionResult.", ""):
    bad("the UseBlockCallback handler translates PASS to null. That was required for "
        "ItemEvents.USE and is wrong here: UseBlockCallback compares the result against PASS, so "
        "null is not 'not handled' and the callback stops declining cleanly")
else:
    print("  Fabric uses UseBlockCallback and returns PASS to decline: OK")

# NeoForge's is the mirror image: it must CANCEL only on a non-PASS result.
neo_main = source("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/OreDetectorNeoForge.java")
if "PlayerInteractEvent.RightClickBlock" not in neo_main:
    bad("NeoForge listens on the wrong interact event; the grinder needs RightClickBlock")
elif "result != InteractionResult.PASS" not in neo_main:
    bad("NeoForge's grind handler does not gate its cancel on a non-PASS result")
else:
    print("  NeoForge cancels only on a real grind: OK")

# --- the sneak gate ---------------------------------------------------------------------------
# Without it the handler swallows EVERY right-click on a grindstone whenever the player happens to
# be holding an ingot, which takes Repair and Disenchant away with nothing logged. The sneak
# pairing is also what keeps the interaction free: sneak plus a non-placeable item on a block does
# nothing in vanilla, and every grindable material is a non-BlockItem.
grind = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/OreGrindingInteraction.java")
if "isSecondaryUseActive()" not in grind:
    bad("the grind interaction does not check isSecondaryUseActive(), so it hijacks every plain "
        "right-click on a grindstone and breaks vanilla Repair and Disenchant")
elif "Blocks.GRINDSTONE" not in grind:
    bad("the grind interaction does not check the clicked block is a grindstone, so it fires "
        "on every block in the game")
else:
    print("  grinding is gated on sneak AND on the block being a grindstone: OK")

# Redstone must stay out of the grind table: it is the only BlockItem that would collide with the
# sneak-place gesture, and redstone ore already drops its own dust.
table = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/OreGrinding.java")
if 'add("redstone"' in table or "Items.REDSTONE," in table:
    bad("redstone is in the grind table. It is a BlockItem, so sneak-clicking a grindstone with "
        "it would both place it and open the grinder, and redstone ore already drops its dust")
else:
    print("  redstone stays out of the grind table: OK")

# --- the bottle yield ladder --------------------------------------------------------------------
# Two tunables meet here (dust yield and bottle size), so the check belongs on their RELATIONSHIP
# rather than on either number looking sensible alone. Both now point the SAME way: an ore that
# grinds generously should also bottle generously, because both express "this is cheap". That is
# the inverse of the arrangement this replaced, where the second number was the price of reporting
# a block and therefore ran opposite to the yield.
table = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/OreGrinding.java")
entries = re.findall(r'add(?:Modded)?\(\s*(?:"[a-z_]+",\s*"[a-z_]+",\s*)?"([a-z]+)",\s*'
                     r'(?:Items\.[A-Z_]+,\s*)?(\d+),\s*(\d+)\)', table)
foreign_ores = {m[2] for m in re.findall(
    r'addForeign\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', table)}
if len(entries) < 12:
    bad("only %d grindable materials parsed out of OreGrinding; the ladder check is not seeing "
        "the real table" % len(entries))
else:
    broken = [o for o, _, size in entries if int(size) <= 0]
    if broken:
        bad("these ores have a bottle worth 0 mB, so they could never attune anything: "
            + ", ".join(broken))
    else:
        print("  all %d grindable ores have a positive bottle yield: OK" % len(entries))

    # Foreign-dust materials are exempt: their dust yield is pinned to 1 to stop an ingot
    # duplication loop, not because the material is precious, so it carries no information about
    # how cheap the ore is.
    ladder = [(o, int(y), int(s)) for o, y, s in entries if o not in foreign_ores]
    wrong = []
    for oi, yi, si in ladder:
        for oj, yj, sj in ladder:
            if yi > yj and si < sj:
                wrong.append("%s (yield %d, bottle %d) vs %s (yield %d, bottle %d)"
                             % (oi, yi, si, oj, yj, sj))
    if wrong:
        bad("the bottle ladder disagrees with the grinding tiers; an ore that grinds MORE "
            "generously must not bottle LESS generously: " + "; ".join(sorted(set(wrong))[:3]))
    else:
        print("  bottle yield runs with dust yield across every pair: OK")

# ONE MILLIBUCKET IS ONE ORE. The scan must not reintroduce a per-block price: that is what made a
# charge able to fall below the cost of a single block, buy nothing, never drain, and still report
# everything it saw.
scan = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/AttunedDetectorItem.java")
_body = re.search(r"private InteractionResult scan\(.*?\n    \}", scan, re.DOTALL)
scan_body = _body.group(0) if _body else ""
if not _body:
    bad("could not find the scan() method, so every check below it is meaningless")
elif "costPerBlock" in scan_body or "liquidCostOf" in scan_body:
    bad("the scan prices a block at more than one millibucket again. That is what let a charge "
        "fall below the price of a single block, buy nothing, never drain, and still report "
        "every block it found")
elif "Math.min(blocksFound, available)" not in scan_body:
    bad("the scan does not spend one millibucket per ore found")
elif "paidFor.put(ore, affordable)" not in scan_body:
    bad("paidFor is not being filled with a BLOCK count. It drives the highlight and the goggles' "
        "wear, neither of which cares what the liquid cost")
elif "report(player, paidFor" not in scan_body:
    bad("the action bar reports the blocks FOUND rather than the blocks PAID FOR")
elif "ranDry.add(" not in scan_body:   # the NAME alone appears in the declaration and the call
    bad("the scan does not record which ores ran dry, so the player is never told")
else:
    print("  one millibucket per ore, and only paid-for blocks are reported: OK")

# The bottle size has to reach the pour, or every bottle is worth whatever the tank felt like.
pour = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/AttunementLiquidItem.java")
if "OreGrinding.bottleSizeOf(" not in pour or "scaleBottleYield(" not in pour:
    bad("pouring does not read the tiered bottle size, so every ore's bottle is worth the same "
        "and the whole rarity ladder does nothing")
else:
    print("  pouring reads the tiered bottle size and the config scalar: OK")

# The drain hold must follow MILLIBUCKETS, not the ore's bottle size. Pacing it per bottle is
# defensible arithmetic that reads as a bug in play: 12 mB of netherite is a full bottle and drained
# as slowly as 165 mB of coal, so the smaller number took longer.
drain = re.search(r"private static int drainTicks\(.*?\n    \}", scan, re.DOTALL)
if not drain:
    bad("drainTicks is missing")
elif "bottleSizeOf(" in drain.group(0):
    bad("the drain hold is paced by the ore's bottle size again, so a small charge of an expensive "
        "ore takes as long to empty as a large charge of a cheap one")
else:
    print("  the drain hold follows the millibuckets shown to the player: OK")

# --- pour and drain feel -------------------------------------------------------------------------
# Both directions share one curve, on purpose, and it has a FLOOR. Without one the arithmetic is
# honest and the feel is wrong: a twelve millibucket netherite bottle came out at two ticks and read
# as a click. Scaling everything up instead fixes that at the cost of the common case, because the
# multiplier netherite needs makes a coal bottle a six second hold.
_flow = re.search(r"public static int flowTicks\(.*?\n    \}", scan, re.DOTALL)
if not _flow:
    bad("flowTicks is missing, so pouring and draining have no shared timing at all")
elif "int base = MIN_FLOW_TICKS" not in _flow.group(0):   # the floor must be the ADDITIVE base;
    # the name alone appears twice in this method and in its own declaration, so its mere presence
    # proves nothing about whether a floor is actually being applied
    bad("the pour and drain timing has no floor, so a small bottle is effectively instant and "
        "stops reading as a channelled action")
elif "flowTicks(" not in source("common/src/main/java/com/chillpavz/oredetectorreborn/item/"
                                "AttunementLiquidItem.java"):
    bad("pouring does not use the shared flow timing, so pouring and draining can drift apart")
elif "scaleFlowTicks(" not in scan:
    bad("the flow timing ignores the config scalar, so the pour speed option does nothing")
else:
    print("  pouring and draining share one floored, configurable curve: OK")

# --- the scan puff -------------------------------------------------------------------------------
# It must never be drawn at a hit position: that would hand every player the goggles' information
# for free, and the goggles cost durability, liquid and strain.
# The volume outline REPLACED a particle version that could not work: particles are depth
# tested and the beam runs into rock, so everything past the entry face was invisible, and the
# faint face puff that remained only read as "it appears when it finds something". A gizmo
# draws through terrain, which is the whole point.
#
# BOTH halves are checked: a method nobody calls is as silent as no method at all, and deleting
# only the call slipped past an earlier version of this guard in a sabotage run.
volume = re.search(r"private static void showVolume\(.*?\n    \}", scan, re.DOTALL)
if "showVolume(player, clicked, into," not in scan_body:
    bad("the scan never calls showVolume(), so a scan that found nothing is still "
        "indistinguishable from a scan that never fired, and nothing shows how far it reached")
elif not volume:
    bad("the showVolume method is missing entirely")
elif re.search(r"\bhits\b|\bpaidFor\b|\bfound\b", volume.group(0)):
    bad("showVolume reads the scan RESULT. It must send only the shape swept; anything about "
        "where ore is belongs to the goggles, which cost durability, liquid and strain")
else:
    print("  the scan volume is sent for every scan and carries no ore data: OK")

# It must be drawn OUTSIDE the goggles checks, or the one effect meant for every player
# silently becomes goggles-only. That is a one-line mistake with no symptom for anyone who
# happens to be wearing them while testing.
highlight = source("common/src/main/java/com/chillpavz/oredetectorreborn/client/ScanHighlight.java")
_tick = re.search(r"public static void tick\(.*?\n    \}", highlight, re.DOTALL)
if not _tick:
    bad("could not find ScanHighlight.tick, so the volume placement is unchecked")
else:
    body = _tick.group(0)
    if "tickVolume(" not in body:
        bad("ScanHighlight.tick never draws the scan volume")
    elif body.index("tickVolume(") > body.index("entries.isEmpty()"):
        bad("the scan volume is drawn AFTER the goggles checks, so it only appears for players "
            "wearing goggles. It belongs to the scan and must come first")
    else:
        print("  the volume is drawn before every goggles check: OK")

# --- the goggles' Haste reward ------------------------------------------------------------
# Three gates, each of which is a real design decision rather than caution.
if "rewardScan(" not in scan_body:
    bad("a successful scan never grants the goggles reward")
else:
    reward = re.search(r"private static void rewardScan\(.*?\n    \}", scan, re.DOTALL)
    if not reward:
        bad("rewardScan is called but not defined")
    else:
        body = reward.group(0)
        if "ModItems.GOGGLES" not in body:
            bad("the Haste reward does not require the goggles, so the detector grants mining "
                "speed on its own and the goggles stop being the thing that earns it")
        elif "isBlockedAt(" not in body:
            bad("the Haste reward is not gated on strain, so strain has two meanings: it "
                "refuses the highlight while still handing out the bonus")
        elif "if (!foundAnything" not in body:   # the PARAMETER name alone proves nothing; the branch is what matters
            bad("the Haste reward is not gated on finding something, so it pays for clicking "
                "at bare walls")
        elif "types <= 1 ? 1 : 0" not in body:
            bad("the Haste amplifier no longer favours a single loaded liquid. That inversion "
                "is the point: a focused tank scans the smallest volume and needs the reason")
        else:
            print("  Haste needs the goggles, an unstrained pair, and a real find: OK")

# --- shaderpack compatibility -------------------------------------------------------------------
# Iris leaves debug_filled_box unmapped and skips the depth clear that gives setAlwaysOnTop its
# meaning, so with a shaderpack the highlight loses its fill AND stops drawing through terrain.
# The fill is recoverable through Iris's own assignPipeline API; the see-through is not, so the
# goggles have to SAY so rather than look broken.
shader_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/client/ShaderCompat.java")
# Anchored on the reflective CALL, not the bare name: "assignPipelineX" contains "assignPipeline"
# and would pass a substring test while calling nothing.
if not re.search(r'getMethod\(\s*"assignPipeline"', shader_src):
    bad("nothing registers the filled box pipeline with Iris, so the highlight loses its fill "
        "under any shaderpack")
if not re.search(r'getMethod\(\s*"isShaderPackInUse"', shader_src):
    bad("nothing detects a shaderpack, so the goggles cannot explain why they stop drawing "
        "through terrain")
# ScanHighlight is reached from the NeoForge payload handler, which is registered on a dedicated
# server too, so nothing it touches may drag in a client-only class.
# Only an IMPORT is forbidden. Reflecting on a client class BY NAME is the whole point of this
# file, so the string literals must not trip the check.
if re.search(r"^import net\.minecraft\.client\.", shader_src, re.M):
    bad("ShaderCompat imports a client-only class; it is reachable from the payload handler "
        "that a dedicated server registers, so it must stay reflective")
goggles_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/GogglesItem.java")
if "ShaderCompat.shaderPackInUse()" not in goggles_src:
    bad("the goggles tooltip never mentions a shaderpack, so the highlight just looks broken")
for loader in ("fabric", "neoforge"):
    path = ("%s/src/main/java/com/chillpavz/oredetectorreborn/%s/client/OreDetector%sClient.java"
            % (loader, loader, "Fabric" if loader == "fabric" else "NeoForge"))
    text = source(path)
    if "registerFilledBoxPipeline()" not in text:
        bad("%s never registers the filled box pipeline with Iris" % loader)
    if "ShaderCompat.refresh()" not in text:
        bad("%s never refreshes the shaderpack state, so the tooltip would be stale" % loader)
print("  shaderpack compatibility wired on both loaders: OK")

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
