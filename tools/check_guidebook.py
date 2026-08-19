#!/usr/bin/env python3
"""Audit the Patchouli guidebook in the BUILT JARS.

Patchouli fails quietly in the ways that matter. An entry naming a category that does not exist
simply never appears; a crafting page naming a recipe that does not exist renders an error page
that only somebody who opens that exact page will ever see; and the recipe that mints the book is
the one thing that CANNOT fail quietly, because without Patchouli installed it names an unknown
item and the log fills with parse errors instead.
"""
import io, json, os, re, sys, zipfile

NS, VER = "ore_detector_reborn", "2.0.0"
BOOK = "resonance"
MC = re.search(r"^minecraft_version=(.+)$",
               io.open("gradle.properties", encoding="utf-8").read(), re.M).group(1).strip()
ok = True


def bad(message):
    global ok
    ok = False
    print("  FAIL: " + message)


# SINCE 1.20 THE BOOK IS SPLIT ACROSS BOTH ROOTS and Patchouli refuses it otherwise:
# book.json stays in data/, every category and entry lives CLIENT side under assets/, and
# book.json must set "use_resource_pack": true. Getting this wrong is one ERROR at load and
# a book that does not exist, with the files all present and looking correct in the jar.
DATA_ROOT = "common/src/main/resources/data/%s/patchouli_books/%s" % (NS, BOOK)
ASSET_ROOT = "common/src/main/resources/assets/%s/patchouli_books/%s" % (NS, BOOK)
book = json.loads(io.open(os.path.join(DATA_ROOT, "book.json"), encoding="utf-8").read())
print("book: %r, subtitle %r" % (book.get("name"), book.get("subtitle")))
for required in ("name", "landing_text"):
    if not book.get(required):
        bad("book.json has no %s, which Patchouli requires" % required)
if book.get("use_resource_pack") is not True:
    bad("book.json does not set use_resource_pack, so Patchouli refuses the whole book "
        "with a single ERROR at load and the guide silently does not exist")
if book.get("dont_generate_book"):
    bad("dont_generate_book keeps the book out of every creative tab, including ours")
if os.path.isdir(os.path.join(DATA_ROOT, "en_us")):
    bad("categories and entries are still under data/; since 1.20 they must be under "
        "assets/ or Patchouli refuses the book")

categories = {}
for name in os.listdir(os.path.join(ASSET_ROOT, "en_us", "categories")):
    categories[NS + ":" + name[:-5]] = json.loads(
        io.open(os.path.join(ASSET_ROOT, "en_us", "categories", name), encoding="utf-8").read())
entries = {}
for name in os.listdir(os.path.join(ASSET_ROOT, "en_us", "entries")):
    entries[name[:-5]] = json.loads(
        io.open(os.path.join(ASSET_ROOT, "en_us", "entries", name), encoding="utf-8").read())
print("%d categories, %d entries" % (len(categories), len(entries)))

# --- what the mod actually registers -----------------------------------------------------------
SRC = "common/src/main/java/com/chillpavz/oredetectorreborn/registry/"
own_items = set(re.findall(r'fromNamespaceAndPath\(Constants\.MOD_ID,\s*"([^"]+)"\)',
                           io.open(SRC + "ModItems.java", encoding="utf-8").read()))
own_items |= set(re.findall(r'create\("([^"]+)"', io.open(SRC + "ModItems.java", encoding="utf-8").read()))
own_items |= set(re.findall(r'material\("([^"]+)"', io.open(SRC + "ModItems.java", encoding="utf-8").read()))
own_items |= set(re.findall(r'"([a-z_]+)"', io.open(SRC + "ModBlocks.java", encoding="utf-8").read()))

vanilla_jar = None
for name in os.listdir("common/build/moddev/artifacts"):
    if name.endswith("-merged.jar"):
        vanilla_jar = "common/build/moddev/artifacts/" + name
vanilla_items = set()
if vanilla_jar:
    for entry in zipfile.ZipFile(vanilla_jar).namelist():
        # assets/minecraft/items/ is the 1.21.4+ item DEFINITION layer and is the only
        # complete list. models/item/ alone misses every BlockItem (heavy_core among them),
        # which reads as a real failure and is not one.
        m = re.match(r"^assets/minecraft/items/([a-z_]+)\.json$", entry)
        if m:
            vanilla_items.add(m.group(1))


def check_item(where, raw):
    """An item reference, possibly carrying components (Patchouli uses vanilla's ItemParser)."""
    ident = raw.split("[")[0].strip()
    if ":" not in ident:
        bad("%s: item %r has no namespace" % (where, raw))
        return
    ns, path = ident.split(":", 1)
    if ns == NS:
        if path not in own_items:
            bad("%s: %s is not an item or block this mod registers" % (where, ident))
    elif ns == "minecraft" and vanilla_items and path not in vanilla_items:
        bad("%s: %s is not a vanilla item" % (where, ident))


for name, entry in sorted(entries.items()):
    where = "entry %s" % name
    if entry.get("category") not in categories:
        bad("%s names category %r, which does not exist, so the entry never appears"
            % (where, entry.get("category")))
    if entry.get("icon"):
        check_item(where + " icon", entry["icon"])
    if not entry.get("pages"):
        bad("%s has no pages" % where)
    for i, page in enumerate(entry.get("pages", [])):
        kind = page.get("type")
        if kind not in ("patchouli:text", "patchouli:crafting", "patchouli:spotlight",
                        "patchouli:image", "patchouli:link", "patchouli:empty"):
            bad("%s page %d uses an unrecognised type %r" % (where, i, kind))
        if kind == "patchouli:spotlight":
            check_item("%s page %d" % (where, i), page.get("item", ""))
        if kind in ("patchouli:text", "patchouli:spotlight", "patchouli:crafting"):
            if kind != "patchouli:crafting" and not page.get("text"):
                bad("%s page %d has no text" % (where, i))

for cid, cat in sorted(categories.items()):
    if cat.get("icon"):
        check_item("category %s icon" % cid, cat["icon"])
    if not cat.get("description"):
        bad("category %s has no description" % cid)

print("  every entry has a real category, and every item reference resolves: OK")

# --- the recipe that mints the book -------------------------------------------------------------
CONDITION = {
    "fabric": ("fabric:load_conditions", "fabric:registry_contains"),
    "neoforge": ("neoforge:conditions", "neoforge:mod_loaded"),
}
for loader, (key, kind) in CONDITION.items():
    path = "%s/src/main/resources/data/%s/recipe/tome_of_resonance.json" % (loader, NS)
    if not os.path.isfile(path):
        bad("%s ships no recipe for the book" % loader)
        continue
    recipe = json.loads(io.open(path, encoding="utf-8").read())
    # Without a condition this recipe names patchouli:guide_book on an instance that has no
    # Patchouli, which is an error line per world load rather than a quiet skip.
    if key not in recipe:
        bad("%s's book recipe has no %s, so it errors when Patchouli is absent" % (loader, key))
    elif kind not in json.dumps(recipe[key]):
        bad("%s's book recipe uses the wrong condition type; expected %s" % (loader, kind))
    result = recipe.get("result", {})
    if result.get("id") != "patchouli:guide_book":
        bad("%s's book recipe does not produce patchouli:guide_book" % loader)
    got = result.get("components", {}).get("patchouli:book")
    want = "%s:%s" % (NS, BOOK)
    # The component value must match the FOLDER the book lives in, or the item opens nothing.
    if got != want:
        bad("%s's book recipe points at %r but the book folder is %r" % (loader, got, want))
    print("  %-8s book recipe is gated and points at the right book: OK" % loader)

# --- the built jars -------------------------------------------------------------------------------
for loader in ("fabric", "neoforge"):
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-%s-%s.jar" % (loader, NS, loader, MC, VER))
    names = set(jar.namelist())
    # Zip directory entries end in "/" and are not files; counting them inflates this by
    # one per folder.
    shipped = {n for n in names if "/patchouli_books/" in n and not n.endswith("/")}
    in_data = {n for n in shipped if n.startswith("data/")}
    in_assets = {n for n in shipped if n.startswith("assets/")}
    if len(in_data) != 1:
        bad("%s jar has %d book files under data/, expected just book.json"
            % (loader, len(in_data)))
    if len(in_assets) != len(categories) + len(entries):
        bad("%s jar has %d book files under assets/, expected %d"
            % (loader, len(in_assets), len(categories) + len(entries)))
    expected = 1 + len(categories) + len(entries)
    if len(shipped) != expected:
        bad("%s jar carries %d book files, expected %d" % (loader, len(shipped), expected))
    else:
        print("  %-8s jar carries all %d book files: OK" % (loader, expected))
    # A crafting page pointing at a recipe that is not in the jar renders an error page.
    for name, entry in entries.items():
        for page in entry.get("pages", []):
            if page.get("type") != "patchouli:crafting":
                continue
            rid = page.get("recipe", "")
            rns, rpath = rid.split(":", 1)
            if "data/%s/recipe/%s.json" % (rns, rpath) not in names:
                bad("%s: entry %s points at recipe %s, which is not in the %s jar"
                    % (loader, name, rid, loader))

print("\nGUIDEBOOK AUDIT " + ("PASSED" if ok else "FAILED"))
sys.exit(0 if ok else 1)
