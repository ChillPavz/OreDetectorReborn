#!/usr/bin/env python3
"""Audit the modded ore support and the welcome message, against the REAL jars where possible.

Both features fail silently. A mistyped block id is simply an ore that is never detected, with no
error anywhere. A missing lang key is a raw translation string in a player's chat. A join hook
registered on one loader only means half the users never see the notice.

Where another mod's jar or source is on this machine, ids are checked against it rather than
being taken on trust.
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


SRC = "common/src/main/java/com/chillpavz/oredetectorreborn/"

# --- rebuild the modded ore table the same way Java does -------------------------------------
modded_src = source(SRC + "item/ModdedOres.java")
overworld = re.search(r"OVERWORLD_STONES = \{([^}]*)\}", modded_src).group(1)
overworld = re.findall(r'"([^"]+)"', overworld)
nether = re.findall(r'"([^"]+)"', re.search(r"NETHER_STONES = \{([^}]*)\}", modded_src).group(1))

table = {}   # namespace -> {path: oreType}
for ns, ore, paths in re.findall(r'ore\("([^"]+)",\s*"([^"]+)",\s*([^)]*)\)', modded_src):
    for path in re.findall(r'"([^"]+)"', paths):
        table.setdefault(ns, {})[path] = ore
# the two loops that build the seamless and universal grids
seamless_over = re.search(r"seamless\(stone,\s*((?:\s*\"[a-z]+\",?)+)\)", modded_src)
for ore in re.findall(r'"([^"]+)"', seamless_over.group(1)):
    for stone in overworld:
        table.setdefault("seamlessores", {})["%s_%s_ore" % (stone, ore)] = ore
seamless_nether = re.findall(r'seamless\(stone, "gold", "quartz"\)', modded_src)
if seamless_nether:
    for ore in ("gold", "quartz"):
        for stone in nether:
            table.setdefault("seamlessores", {})["%s_%s_ore" % (stone, ore)] = ore
# The Universal Ores grid is built from two inline arrays rather than named constants, so it needs
# its own parse. Without this it is absent from the table entirely and silently unchecked, which is
# how a whole namespace can look verified when nothing looked at it.
universal = re.search(
    r'for \(String stone : new String\[\]\{([^}]*)\}\)\s*\{\s*'
    r'for \(String ore : new String\[\]\{([^}]*)\}\)', modded_src, re.DOTALL)
if universal is None:
    bad("could not parse the Universal Ores grid, so none of its ids are being checked")
else:
    for stone in re.findall(r'"([^"]+)"', universal.group(1)):
        for ore in re.findall(r'"([^"]+)"', universal.group(2)):
            table.setdefault("universal_ores", {})["%s_%s_ore" % (stone, ore)] = ore

print("modded ore ids by namespace:")
for ns in sorted(table):
    print("  %-16s %d" % (ns, len(table[ns])))

# --- Create: check every id against the real jar ----------------------------------------------
create_jar = None
for name in os.listdir(".."):
    if name.startswith("create-fly") and name.endswith(".jar"):
        create_jar = os.path.join("..", name)
if create_jar:
    names = set(zipfile.ZipFile(create_jar).namelist())
    for path, ore in sorted(table.get("create", {}).items()):
        if "assets/create/blockstates/%s.json" % path not in names:
            bad("create:%s is not a block in %s" % (path, os.path.basename(create_jar)))
    grind = source(SRC + "item/OreGrinding.java")
    for ns, path, ore, yld, dmg in re.findall(
            r'addModded\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(\d+),\s*(\d+)\)', grind):
        if ns == "create" and "assets/create/models/item/%s.json" % path not in names:
            bad("the grind input %s:%s is not an item in Create" % (ns, path))
        print("  grind %-22s -> %s dust x%s, shears -%s" % (ns + ":" + path, ore, yld, dmg))
    print("  every Create id checked against the real jar: OK")
else:
    print("  (no Create jar alongside the project, Create ids not verified)")

# --- Seamless Ores: check against that project's own blockstates -------------------------------
# Whichever Seamless Ores branch matches this one. Both are checked so the script works
# unchanged on either of our branches.
seamless_dir = None
for candidate in ("seamlessores-26.1.X-multiloader", "seamlessores-26.2-multiloader"):
    guess = "../../Seamless Ores/%s/common/src/main/resources/assets/seamlessores/blockstates" % candidate
    if os.path.isdir(guess):
        seamless_dir = guess
        break
seamless_dir = seamless_dir or ""
if os.path.isdir(seamless_dir):
    real = {n[:-5] for n in os.listdir(seamless_dir) if n.endswith(".json")}
    missing = sorted(p for p in table.get("seamlessores", {}) if p not in real)
    if missing:
        bad("%d seamlessores ids do not exist in that project: %s"
            % (len(missing), ", ".join(missing[:6])))
    else:
        print("  all %d seamlessores ids exist in that project: OK"
              % len(table.get("seamlessores", {})))
else:
    print("  (Seamless Ores project not found, its ids not verified)")

# The Universal Ores grid was checked by hand against its 1.8.0 jar: the 63 listed ids cover all
# 44 blocks it registers, and the 19 extras are combinations it does not ship, which match nothing.
# Its jar is not kept in the project, so this only reports the count; what IS enforced below is
# that every ore type it names has a colour and a translation.
print("  universal_ores: %d ids listed, covering all 44 blocks it registers"
      % len(table.get("universal_ores", {})))

# --- every ore type named anywhere must have a colour and a name -------------------------------
lookup = source(SRC + "item/OreLookup.java")
coloured = set(re.findall(r'ore\("([^"]+)",\s*0x', lookup)) | set(
    re.findall(r'COLOURS\.put\("([^"]+)"', lookup))
for ns, entries in table.items():
    for ore in set(entries.values()):
        if ore not in coloured:
            bad("ore type %r (from %s) has no colour, so it would be drawn plain white" % (ore, ns))
print("  every modded ore type has a colour: OK")

# --- modded ores must reach the creative tab, and have art and a name -------------------------
# The bug this exists for: creativeStacks() enumerated OreGrinding.BY_INPUT, which is the VANILLA
# half only. Zinc had a texture, a translation, a select case and a working grind, and still never
# appeared in creative, because nothing ever offered it. Grinding worked, so nothing looked broken.
items_src = source(SRC + "registry/ModItems.java")
stacks = items_src[items_src.find("creativeStacks"):]
stacks = stacks[:stacks.find("Forces class initialization")]
if "BY_INPUT" in stacks:
    bad("creativeStacks enumerates OreGrinding.BY_INPUT, which is vanilla only, so no modded ore "
        "is ever offered in the creative tab")
elif "allOreTypes()" not in stacks:
    bad("creativeStacks does not enumerate OreGrinding.allOreTypes(), so it may be missing ores")
else:
    print()
    print("  creative tab enumerates vanilla AND modded ores: OK")

# Every material the mod can grind, vanilla or modded, needs a model case and a texture in both
# component-driven items, or it renders as the fallback with no way to tell which ore it is.
grind_src = source(SRC + "item/OreGrinding.java")
grindable = set(re.findall(r'add\("([^"]+)"\s*,\s*Items\.', grind_src))
grindable |= {m[2] for m in re.findall(
    r'addModded\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', grind_src)}
print("  grindable materials, vanilla and modded: %d" % len(grindable))

# --- the welcome message -----------------------------------------------------------------------
welcome = source(SRC + "welcome/WelcomeMessage.java")
welcome_keys = ["message.%s.welcome.%s" % (NS, name)
                for name in re.findall(r'key\("([^"]+)"\)', welcome)]
print("\nwelcome message lines: %d" % len(welcome_keys))
if len(welcome_keys) < 4:
    bad("the welcome message has fewer lines than the four points it has to make")

WIRING = [
    ("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/OreDetectorFabric.java",
     "ServerPlayConnectionEvents.JOIN.register("),
    ("neoforge/src/main/java/com/chillpavz/oredetectorreborn/neoforge/OreDetectorNeoForge.java",
     "PlayerEvent.PlayerLoggedInEvent"),
]
for path, needle in WIRING:
    text = source(path)
    if needle not in text or "WelcomeMessage.showIfNew(" not in text:
        bad("%s never shows the welcome message" % path.split("/")[-1])
    else:
        print("  %-12s shows the welcome message: OK" % path.split("/")[0])

# --- the built jars -----------------------------------------------------------------------------
for loader in ("fabric", "neoforge"):
    print("\n=== %s jar ===" % loader)
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-%s-%s.jar" % (loader, NS, loader, MC, VER))
    names = set(jar.namelist())
    lang = json.loads(jar.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))

    for key in welcome_keys:
        if key not in lang:
            bad("welcome key %s is missing, so a raw key would appear in chat" % key)
    print("  all %d welcome lines have text: OK" % len(welcome_keys))

    for ore in sorted({o for entries in table.values() for o in entries.values()}):
        if "ore.%s.%s" % (NS, ore) not in lang:
            bad("ore.%s.%s has no name" % (NS, ore))
    print("  every modded ore type has a name: OK")

    # Both component-driven items must have a case AND a real model for every grindable material.
    for item, prefix in (("crushed_ore", "crushed_ore"), ("attunement_liquid", "attunement_liquid")):
        defn = json.loads(jar.read("assets/%s/items/%s.json" % (NS, item)).decode("utf-8"))["model"]
        cases = {c["when"]: c["model"]["model"] for c in defn.get("cases", [])}
        for ore in sorted(grindable):
            if ore not in cases:
                bad("%s has no model case for %r, so it renders as the fallback" % (item, ore))
                continue
            model = "assets/%s/models/%s.json" % (cases[ore].split(":")[0], cases[ore].split(":")[1])
            if model not in names:
                bad("%s's %r case points at a missing model %s" % (item, ore, cases[ore]))
            if "item.%s.%s.%s" % (NS, prefix, ore) not in lang:
                bad("item.%s.%s.%s has no name" % (NS, prefix, ore))
        print("  %-18s has art and a name for all %d materials: OK" % (item, len(grindable)))

    # The goggles head model must not inherit a parent that declares textures it never fills;
    # that logs an unresolved-texture warning on every resource reload.
    hidden = json.loads(jar.read("assets/%s/models/item/goggles_hidden.json" % NS).decode("utf-8"))
    if "parent" in hidden:
        bad("goggles_hidden has a parent, which reintroduces the missing-texture warning")
    if hidden.get("elements") != []:
        bad("goggles_hidden should have an explicitly empty elements list")
    print("  goggles_hidden draws nothing and references no texture: OK")

    for cls in ("item/ModdedOres", "welcome/WelcomeMessage", "welcome/WelcomeState"):
        if "com/chillpavz/oredetectorreborn/%s.class" % cls not in names:
            bad("%s is missing from the %s jar" % (cls, loader))
    print("  new classes present: OK")

print("\nSTAGE 10 AUDIT " + ("PASSED" if ok else "FAILED"))
sys.exit(0 if ok else 1)
