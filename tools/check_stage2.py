#!/usr/bin/env python3
"""Audit the component-driven dust item in the BUILT JARS.

Everything here fails silently in game. A component id that does not match the one Java registers
means the select model never matches and every dust renders as the fallback. A missing case or
texture is a purple cube. A missing lang key shows the raw translation string.
"""
import zipfile, json, io, re, sys

NS, VER = "ore_detector_reborn", "2.0.0"
ok = True
def bad(m):
    global ok; ok = False; print("  FAIL: " + m)

# what Java actually registers / grinds
comp_src = io.open("common/src/main/java/com/chillpavz/oredetectorreborn/registry/ModDataComponents.java",
                   encoding="utf-8").read()
registered = set(re.findall(r'register\("([^"]+)"', comp_src))
grind_src = io.open("common/src/main/java/com/chillpavz/oredetectorreborn/item/OreGrinding.java",
                    encoding="utf-8").read()
grind_src = re.sub(r"//.*", "", grind_src)
grindable = [m.group(1) for m in re.finditer(r'add\("([^"]+)"\s*,\s*Items\.(\w+)\s*,\s*(\d+)\s*,\s*(\d+)\)', grind_src)]
tiers = {m.group(1): (m.group(2), int(m.group(3)), int(m.group(4)))
         for m in re.finditer(r'add\("([^"]+)"\s*,\s*Items\.(\w+)\s*,\s*(\d+)\s*,\s*(\d+)\)', grind_src)}
print("registered components: %s" % sorted(registered))
print("grindable materials: %d" % len(grindable))
for ore, (item, yld, dmg) in tiers.items():
    print("  %-10s <- %-16s yield %d, shears -%d" % (ore, item, yld, dmg))
if "redstone" in tiers:
    bad("redstone should NOT be grindable; vanilla redstone dust feeds the chamber directly")

for loader in ("fabric", "neoforge"):
    print("\n=== %s ===" % loader)
    z = zipfile.ZipFile("%s/build/libs/%s-%s-26.2-%s.jar" % (loader, NS, loader, VER))
    files = {n for n in z.namelist() if not n.endswith("/")}
    lang = json.loads(z.read("assets/%s/lang/en_us.json" % NS).decode("utf-8"))

    defn = json.loads(z.read("assets/%s/items/crushed_ore.json" % NS).decode("utf-8"))["model"]
    if defn.get("type") != "minecraft:select":
        bad("crushed_ore is not a select model"); continue
    if defn.get("property") != "minecraft:component":
        bad("select property is %r, expected minecraft:component" % defn.get("property"))

    # the JSON component id must be one Java actually registers
    cid = defn.get("component", "")
    cns, _, cpath = cid.partition(":")
    if cns != NS or cpath not in registered:
        bad("select keys on component %r, which Java does not register (registered: %s)"
            % (cid, sorted(registered)))
    else:
        print("  component %s matches the registered one: OK" % cid)

    def model_ok(m):
        mid = m.get("model", "")
        ns, _, path = mid.partition(":")
        if ns != NS:
            return True
        return ("assets/%s/models/%s.json" % (NS, path) in files
                and "assets/%s/textures/%s.png" % (NS, path.replace("item/", "item/", 1)) in files)

    cases = {c["when"]: c["model"] for c in defn["cases"]}
    for when, m in sorted(cases.items()):
        mid = m["model"]; path = mid.split(":", 1)[1]
        if "assets/%s/models/%s.json" % (NS, path) not in files:
            bad("case %r names a missing model: %s" % (when, mid))
        if "assets/%s/textures/%s.png" % (NS, path) not in files:
            bad("case %r names a missing texture: %s" % (when, mid))
    print("  all %d select cases resolve to a real model and texture: OK" % len(cases))

    fb = defn.get("fallback")
    if not fb:
        bad("no fallback model: a dust with no component would fail to render")
    elif "assets/%s/models/%s.json" % (NS, fb["model"].split(":", 1)[1]) not in files:
        bad("fallback names a missing model: %s" % fb["model"])
    else:
        print("  fallback model present: OK")

    # every grindable ore must be renderable and named
    for ore in grindable:
        if ore not in cases:
            bad("grindable '%s' has no select case, so its dust renders as the fallback" % ore)
        k = "item.%s.crushed_ore.%s" % (NS, ore)
        if k not in lang:
            bad("grindable '%s' has no lang key (%s)" % (ore, k))
    if "item.%s.crushed_ore" % NS not in lang:
        bad("no base lang key for a component-less crushed_ore stack")
    print("  every grindable ore has a case and a name: OK")

    for c in ("com/chillpavz/oredetectorreborn/registry/ModDataComponents.class",
              "com/chillpavz/oredetectorreborn/item/CrushedOreItem.class",
              "com/chillpavz/oredetectorreborn/item/OreGrindingInteraction.class"):
        if c not in files:
            bad("missing class: %s" % c)

print("\n" + ("STAGE 2 AUDIT PASSED" if ok else "STAGE 2 AUDIT FAILED"))
sys.exit(0 if ok else 1)
