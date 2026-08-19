#!/usr/bin/env python3
"""Audit the goggles' scan highlight in the BUILT JARS.

Every failure mode of this feature is silent. A payload type registered on one side only, a
sender never installed, a client tick hook never registered, a receiver never registered: each
of them produces a green build, a clean log and goggles that simply never light anything up.
None of it is visible in creative and none of it crashes.

The source-level checks below anchor on the CALL FORM rather than a bare name, and blank out
comments first, so a mention in a javadoc block cannot pass them.
"""
import io, json, re, sys, zipfile

NS, VER = "ore_detector_reborn", "2.0.0"
ok = True


def bad(message):
    global ok
    ok = False
    print("  FAIL: " + message)


def source(path):
    """A source file with comments blanked out, so prose cannot satisfy a check."""
    text = io.open(path, encoding="utf-8").read()
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    text = re.sub(r"//[^\n]*", "", text)
    return text


# --- what Java actually registers ------------------------------------------------------------
components = set(re.findall(
    r'register\("([^"]+)"',
    source("common/src/main/java/com/chillpavz/oredetectorreborn/registry/ModDataComponents.java")))
print("registered components: %s" % sorted(components))
if "strain" not in components:
    bad("no 'strain' data component is registered, so the goggles cannot remember any strain")

strain_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/item/Strain.java")
numbers = {name: int(value) for name, value in re.findall(
    r"public static final int (\w+)\s*=\s*(\d+);", strain_src)}
print("strain: max %(MAX)s, +%(PER_SCAN)s per scan, -%(DECAY_PER_SECOND)s/s" % numbers)
if numbers.get("PER_SCAN", 0) <= 0 or numbers.get("MAX", 0) <= 0:
    bad("strain must actually rise and have a ceiling, or it gates nothing")
if numbers.get("DECAY_PER_SECOND", 0) <= 0:
    bad("strain never decays, so the goggles would lock permanently after enough scans")
if numbers["PER_SCAN"] >= numbers["MAX"]:
    bad("one scan reaches the ceiling; strain would be a flat cooldown, not a gradient")

# The trap this check exists for: strain is only ever added once per scan, and a scan can only
# happen once per cooldown. If the decay over one cooldown matches or beats the gain, strain can
# never reach its ceiling and the whole mechanic is inert -- with a green build, no log line and
# nothing visible in game except goggles that are never refused. That is exactly how the design
# doc's own suggested numbers (+20 against -5 a second, on a five second cooldown) behaved.
config_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/config/OreDetectorConfig.java")
cooldown_ticks = int(re.search(r"DEFAULT_COOLDOWN\s*=\s*(\d+)", config_src).group(1))
decay_per_cooldown = numbers["DECAY_PER_SECOND"] * cooldown_ticks / 20.0
net = numbers["PER_SCAN"] - decay_per_cooldown
print("at the default %d tick cooldown: +%d a scan, -%.0f decayed, net %+.0f"
      % (cooldown_ticks, numbers["PER_SCAN"], decay_per_cooldown, net))
if net <= 0:
    bad("strain decays at least as fast as it is gained at the default cooldown (+%d vs -%.0f), "
        "so it can never reach the ceiling and gates nothing"
        % (numbers["PER_SCAN"], decay_per_cooldown))
else:
    print("  scans of continuous use before the goggles refuse: %d" % -(-numbers["MAX"] // net))

# The strain component is read back by the tooltip and must survive a relog, so it has to be
# persistent as well as network synchronised. A network-only component would read as zero on
# every fresh login with nothing logged.
comp_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/registry/ModDataComponents.java")
strain_block = comp_src[comp_src.find("STRAIN"):]
strain_block = strain_block[:strain_block.find("build()")]
for required in ("persistent(", "networkSynchronized("):
    if required not in strain_block:
        bad("the strain component is missing %s" % required)

# --- the highlight has to be drawn through terrain -------------------------------------------
highlight_src = source("common/src/main/java/com/chillpavz/oredetectorreborn/client/ScanHighlight.java")
if "setAlwaysOnTop()" not in highlight_src:
    bad("the highlight never calls setAlwaysOnTop, so it would be hidden behind the very rock "
        "it exists to see through")
if "Gizmos.cuboid(" not in highlight_src:
    bad("the highlight emits no cuboid gizmo, so nothing is drawn at all")
if "catch (Throwable" not in highlight_src:
    bad("the emit is not wrapped; a rendering fault would take the client down instead of "
        "degrading to no highlight")

# --- per-loader wiring, all four halves ------------------------------------------------------
WIRING = {
    "fabric": [
        ("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/OreDetectorFabric.java",
         [("PayloadTypeRegistry.clientboundPlay().register(", "the payload type is never registered"),
          ("ModNetworking.setSender(", "no sender is installed, so every packet is dropped")]),
        ("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/client/OreDetectorFabricClient.java",
         [("ClientPlayNetworking.registerGlobalReceiver(", "no receiver, so scan results are ignored"),
          ("ScanHighlight.tick(", "no client tick hook, so the highlight is never drawn"),
          ("ScanHighlight.clear(", "the highlight is never dropped when leaving a world")]),
    ],
    "neoforge": [
        ("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/OreDetectorNeoForge.java",
         [("playToClient(", "the payload type is never registered"),
          ("ModNetworking.setSender(", "no sender is installed, so every packet is dropped"),
          ("ScanHighlight\n", None)]),
        ("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/client/OreDetectorNeoForgeClient.java",
         [("ScanHighlight.tick(", "no client tick hook, so the highlight is never drawn"),
          ("ScanHighlight.clear(", "the highlight is never dropped when leaving a world")]),
    ],
}
for loader, files in WIRING.items():
    print("\n=== %s wiring ===" % loader)
    for path, requirements in files:
        text = source(path)
        for needle, reason in requirements:
            if reason is None:
                continue
            if needle not in text:
                bad("%s: %s (looked for %r)" % (path.split("/")[-1], reason, needle))
            else:
                print("  %-46s %s" % (needle, "OK"))

# NeoForge's receiver is a lambda inside the payload registration, so check it separately.
neo_main = source("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/OreDetectorNeoForge.java")
if "ScanHighlight" not in neo_main or "accept(" not in neo_main:
    bad("neoforge registers no handler that reaches ScanHighlight.accept")

# --- the built jars ---------------------------------------------------------------------------
CLASSES = [
    "com/chillpavz/oredetectorreborn/item/Strain.class",
    "com/chillpavz/oredetectorreborn/item/GogglesItem.class",
    "com/chillpavz/oredetectorreborn/client/ScanHighlight.class",
    "com/chillpavz/oredetectorreborn/client/ClientClock.class",
    "com/chillpavz/oredetectorreborn/network/ModNetworking.class",
    "com/chillpavz/oredetectorreborn/network/ScanHighlightPayload.class",
]
LANG_KEYS = [
    "tooltip.%s.strain" % NS,
    "tooltip.%s.strain_rested" % NS,
    "hud.%s.strained" % NS,
]
for loader in ("fabric", "neoforge"):
    print("\n=== %s jar ===" % loader)
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-26.2-%s.jar" % (loader, NS, loader, VER))
    names = set(jar.namelist())
    for cls in CLASSES:
        if cls not in names:
            bad("%s is missing from the %s jar" % (cls, loader))
    print("  all %d highlight classes present: OK" % len(CLASSES))

    lang = json.loads(jar.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))
    for key in LANG_KEYS:
        if key not in lang:
            bad("lang key %s is missing, so it would render as its raw key" % key)
    print("  all %d new lang keys present: OK" % len(LANG_KEYS))

    # Every %s in a format string must have an argument at the call site. Strain is the only
    # new one taking two.
    if lang["tooltip.%s.strain" % NS].count("%s") != 2:
        bad("tooltip.%s.strain must take exactly two arguments (current and max)" % NS)

print("\nSTAGE 8 AUDIT " + ("PASSED" if ok else "FAILED"))
sys.exit(0 if ok else 1)
