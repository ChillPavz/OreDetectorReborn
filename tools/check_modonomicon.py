"""Audits the Modonomicon guidebook, from the BUILT JARS.

This branch uses Modonomicon where 26.1.x uses Patchouli, because Patchouli has no 26.2 build.
The formats differ enough that this is its own script rather than a fork of check_guidebook.py.

The checks are chosen from how Modonomicon actually loads a book, read out of its BookDataManager
rather than from docs. The expensive mistake to prevent is the same one the Patchouli book shipped
with: fourteen files all present in the jar, every one of them looking correct, and the book dead.

Three things are structural and silent if wrong:
  * an entry's "id" must be "<category>/<entry>" and must match its own path. Modonomicon derives a
    path id for error reporting but takes the REAL id from the file, so a mismatch loads an entry
    nobody can link to.
  * a category id comes from the PATH and is never in the file, so an entry pointing at a category
    that does not exist is attached to nothing.
  * "creative_tab" naming a tab we do not register matches nothing, silently. Same trap Patchouli
    had, and it is what kept the 26.1.x book out of the tab.
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


ROOT = "common/src/main/resources/data/%s/modonomicon/books/%s" % (NS, BOOK)
if not os.path.isdir(ROOT):
    print("no Modonomicon book in this branch, nothing to check")
    sys.exit(0)


def load(rel):
    return json.load(io.open(os.path.join(ROOT, rel), encoding="utf-8"))


book = load("book.json")
print("book: %r, creative tab %s" % (book.get("name"), book.get("creative_tab")))

# --- the creative tab has to be one we actually register ---------------------------------------
tabs = io.open("common/src/main/java/com/chillpavz/oredetectorreborn/registry/ModCreativeTabs.java",
               encoding="utf-8").read()
tab_names = set(re.findall(r'fromNamespaceAndPath\(\s*Constants\.MOD_ID\s*,\s*"([^"]+)"', tabs))
tab_names |= set(re.findall(r'"%s:([a-z_]+)"' % NS, tabs))
declared = book.get("creative_tab", "")
if not declared.startswith(NS + ":"):
    bad("book.json's creative_tab %r is not one of ours" % declared)
elif declared.split(":")[1] not in tab_names:
    bad("book.json's creative_tab %r is not a tab this mod registers (%s), so Modonomicon adds the "
        "book to nothing and logs nothing" % (declared, ", ".join(sorted(tab_names)) or "none"))
else:
    print("  creative_tab names a tab this mod registers: OK")

if book.get("generate_book_item") is False:
    bad("generate_book_item is false, so the book never appears in any creative tab")

# "model" is an ITEM DEFINITION id, resolved through getModelManager().getItemModel(). Unset it
# silently defaults to modonomicon:modonomicon_purple, so the book ships with THEIR cover and
# nothing anywhere says so.
model = book.get("model")
if not model:
    bad("book.json sets no \"model\", so the Tome renders with Modonomicon's own default cover")
elif not model.startswith(NS + ":"):
    bad("book.json's model %r is not one of ours" % model)
else:
    defn = "common/src/main/resources/assets/%s/items/%s.json" % (NS, model.split(":")[1])
    if not os.path.isfile(defn):
        bad("book.json's model %r has no item definition at %s. It names a DEFINITION, not a "
            "model file" % (model, defn))
    else:
        print("  the book uses our own cover, and its item definition exists: OK")

# --- categories and entries ---------------------------------------------------------------------
categories = {}
for name in sorted(os.listdir(os.path.join(ROOT, "categories"))):
    if name.endswith(".json"):
        categories["%s:%s" % (NS, name[:-5])] = load("categories/" + name)
print("  %d categories: %s" % (len(categories), ", ".join(sorted(c.split(":")[1] for c in categories))))

entries = {}
for cat_dir in sorted(os.listdir(os.path.join(ROOT, "entries"))):
    d = os.path.join(ROOT, "entries", cat_dir)
    if not os.path.isdir(d):
        continue
    for name in sorted(os.listdir(d)):
        if not name.endswith(".json"):
            continue
        rel = "entries/%s/%s" % (cat_dir, name)
        data = load(rel)
        # THE ONE THAT SHIPPED BROKEN. BookEntry.CODEC dispatches on "type" with NO default, so
        # an entry without one fails to decode outright. Categories are not dispatched, which is
        # why the book opened with three bookmarks and not one entry behind any of them, and why
        # search found nothing. This script checked PAGE types from the first version and simply
        # never thought to check the entries' own.
        if data.get("type") not in ("modonomicon:content", "modonomicon:category_link",
                                    "modonomicon:entry_link"):
            bad("%s has no valid entry \"type\" (found %r). BookEntry dispatches on it with no "
                "default, so the entry fails to decode and the category is left empty"
                % (rel, data.get("type")))
        expected = "%s:%s/%s" % (NS, cat_dir, name[:-5])
        if data.get("id") != expected:
            bad("%s declares id %r but its path means %r. Modonomicon takes the id from the FILE, "
                "so anything linking to this entry by its path will miss" % (rel, data.get("id"), expected))
        if data.get("category") not in categories:
            bad("%s belongs to category %r, which has no file. It attaches to nothing"
                % (rel, data.get("category")))
        # BookEntry's "background" defaults to GuiSprite.EMPTY, which is literally
        # Identifier("minecraft", "missingno"). "Optional" here means "draws the missing texture",
        # not "draws nothing", so every node without one shows a purple checkerboard and nothing
        # anywhere logs it.
        bg = data.get("background")
        if not bg:
            bad("%s declares no \"background\". The default is minecraft:missingno, so the node "
                "renders a missing-texture checkerboard behind its icon" % rel)
        elif isinstance(bg, str) and bg.startswith(NS + ":"):
            sprite = "common/src/main/resources/assets/%s/textures/gui/sprites/%s.png" % (
                NS, bg.split(":", 1)[1])
            if not os.path.isfile(sprite):
                bad("%s's background %r has no sprite at %s" % (rel, bg, sprite))
        for key in ("x", "y"):
            if not isinstance(data.get(key), int):
                bad("%s has no integer %r. Node coordinates are REQUIRED, not optional" % (rel, key))
        entries[data.get("id")] = data
print("  %d entries" % len(entries))

# --- parents resolve, and nothing is its own ancestor -------------------------------------------
for eid, data in entries.items():
    for parent in data.get("parents", []):
        pid = parent.get("entry")
        if pid not in entries:
            bad("%s names a parent %r that does not exist" % (eid, pid))
        elif pid == eid:
            bad("%s is its own parent" % eid)
for eid in entries:
    seen, cur = set(), eid
    while True:
        parents = [p.get("entry") for p in entries.get(cur, {}).get("parents", [])]
        if not parents:
            break
        cur = parents[0]
        if cur in seen or cur == eid:
            bad("the entry graph has a cycle through %s" % eid)
            break
        seen.add(cur)
print("  every parent resolves and the graph is acyclic: OK")

# --- pages --------------------------------------------------------------------------------------
PAGE_TYPES = {"modonomicon:text", "modonomicon:spotlight", "modonomicon:crafting_recipe",
              "modonomicon:smelting_recipe", "modonomicon:blasting_recipe", "modonomicon:image",
              "modonomicon:entity", "modonomicon:empty", "modonomicon:multiblock",
              "modonomicon:smoking_recipe", "modonomicon:campfire_cooking_recipe",
              "modonomicon:stonecutting_recipe", "modonomicon:smithing_recipe"}
item_refs, recipe_refs, page_count = set(), set(), 0


def _check_item(where, what, value):
    """An item reference, in one of the forms Modonomicon actually parses.

    THE ONE THAT CRASHED THE BOOK. Patchouli takes item[component="value"] because it parses
    through vanilla's ItemParser. Modonomicon does not: an icon or spotlight item is a plain
    resource location, a texture object, or vanilla's ItemStackTemplate JSON. The bracket form
    fails to decode, which takes the whole ENTRY with it and, through it, anything naming that
    entry as a parent.
    """
    if isinstance(value, str):
        if "[" in value or "]" in value:
            bad("%s %s uses Patchouli's item[component=...] syntax (%s). Modonomicon cannot parse "
                "it; use {\"id\": ..., \"components\": {...}} instead"
                % (where, what, value))
            return
        item_refs.add(value)
        return
    if isinstance(value, dict):
        if "texture" in value:
            return
        if "id" not in value:
            bad("%s %s is an object with no \"id\"" % (where, what))
            return
        item_refs.add(value["id"])
        return
    bad("%s %s is neither a string nor an object" % (where, what))
for eid, data in entries.items():
    ids = []
    for page in data.get("pages", []):
        page_count += 1
        if page.get("type") not in PAGE_TYPES:
            bad("%s has a page of unknown type %r" % (eid, page.get("type")))
        if not page.get("id"):
            bad("%s has a page with no id; every Modonomicon page needs one" % eid)
        # A BLANK LINE IS NOT A PARAGRAPH BREAK HERE. Verified against the commonmark 0.30 that
        # Modonomicon jars in: a blank line parses to two Paragraph nodes, and its renderer has no
        # visit(Paragraph) at all, so the break emits NOTHING and the sentences run together as
        # ".Netherite". Only a HARD line break (two spaces then a newline) produces a real newline,
        # through visit(HardLineBreak). Lists are exempt: ListItem finalizes its own component, so
        # it already starts on a new line.
        _chunks = str(page.get("text", "")).split("\n\n")
        for _i, part in enumerate(_chunks[1:], start=1):
            # A blank line is fine in exactly two places: opening a list, and CLOSING one. The
            # list's own visitor finalises the component, so prose after a list starts on its own
            # line anyway. It is prose-to-prose that emits nothing.
            _after_list = _chunks[_i - 1].rstrip().split("\n")[-1].lstrip().startswith("- ")
            if not part.lstrip().startswith("- ") and not _after_list:
                bad("%s page %r separates prose with a blank line, which renders as NOTHING and "
                    "runs the sentences together. Use a hard line break, two spaces then a "
                    "newline" % (eid, page.get("id")))
                break
        ids.append(page.get("id"))
        if page.get("item") is not None:
            _check_item(eid, "page " + str(page.get("id")), page["item"])
        if page.get("recipe_id_1"):
            recipe_refs.add(page["recipe_id_1"])
    dupes = {i for i in ids if ids.count(i) > 1}
    if dupes:
        bad("%s reuses page ids %s" % (eid, ", ".join(sorted(dupes))))
    if data.get("icon") is not None:
        _check_item(eid, "icon", data["icon"])
print("  %d pages, all typed and uniquely identified: OK" % page_count)

# --- page length ---------------------------------------------------------------------------------
# Modonomicon SHRINKS text to fit: renderBookTextHolder calls getBookTextHolderScaleForRenderSize
# and scales the pose whenever the content is taller than the page. So a page's font size is a
# function of how much is on it, and one overlong page renders visibly smaller than the rest of the
# book with nothing logged. That is what happened to the Attunement Liquid page, which held about
# twice what fits.
#
# The estimate is deliberately rough: 124px of page at roughly 6px a glyph is about 20 characters a
# line, and 155px at a 9px line height is about 17 lines, less whatever the title and any item take.
# Fifteen is the budget, which leaves room for the estimate being wrong in either direction.
PAGE_LINE_BUDGET = 15


def _estimate_lines(text):
    total = 0
    for para in re.sub(r"\*\*", "", str(text)).split("\n"):
        para = para.strip()
        total += max(1, -(-len(para) // 20)) if para else 1
    return total


_long = []
for eid, data in entries.items():
    for page in data.get("pages", []):
        n = _estimate_lines(page.get("text", ""))
        if n > PAGE_LINE_BUDGET:
            _long.append("%s page %r (about %d lines)" % (eid, page.get("id"), n))
if _long:
    bad("these pages hold more than fits, so Modonomicon will shrink their text and they will "
        "render smaller than the rest of the book: " + "; ".join(_long[:4]))
else:
    print("  every page fits, so the whole book renders at one size: OK")

# --- the recipe that mints the book, per loader --------------------------------------------------
GATES = {"fabric": "fabric:load_conditions", "neoforge": "neoforge:conditions"}
for loader, gate in GATES.items():
    p = "%s/src/main/resources/data/%s/recipe/tome_of_resonance.json" % (loader, NS)
    if not os.path.isfile(p):
        bad("%s has no recipe to craft the book" % loader)
        continue
    r = json.load(io.open(p, encoding="utf-8"))
    if gate not in r:
        bad("%s's book recipe is NOT gated on Modonomicon being present, so it is a parse error "
            "on every world load for anyone without it" % loader)
    elif r["result"].get("components", {}).get("modonomicon:book_id") != "%s:%s" % (NS, BOOK):
        bad("%s's book recipe points at the wrong book" % loader)
    else:
        print("  %-8s book recipe is gated and points at the right book: OK" % loader)

# --- the gift matches the book -------------------------------------------------------------------
gift = io.open("common/src/main/java/com/chillpavz/oredetectorreborn/welcome/GuidebookGift.java",
               encoding="utf-8").read()
for needle, why in (('"modonomicon", "modonomicon"', "the book ITEM id"),
                    ('"modonomicon", "book_id"', "the book COMPONENT id"),
                    ('Constants.MOD_ID, "%s"' % BOOK, "the book's own id")):
    if needle not in gift:
        bad("GuidebookGift does not use %s, so the book handed out on first join opens nothing" % why)
# SCOPED to available(), and BOTH registries checked. A file-wide search for "containsKey(" passes
# with the item lookup broken, because the component lookup still uses it, which is exactly what a
# sabotage run caught. Same weakness as searching a whole file for liquidCostOf.
_avail = re.search(r"public static boolean available\(\).*?\n    \}", gift, re.DOTALL)
if not _avail:
    bad("GuidebookGift has no available() method to check")
elif "ITEM.containsKey(" not in _avail.group(0):
    bad("GuidebookGift.available() does not check the ITEM registry with containsKey. "
        "BuiltInRegistries.ITEM is DEFAULTED, so getValue on an absent mod returns AIR and every "
        "new player is handed a stack of it")
elif "DATA_COMPONENT_TYPE.containsKey(" not in _avail.group(0):
    bad("GuidebookGift.available() does not check the component registry with containsKey, so a "
        "Modonomicon that changed its component id would throw rather than degrade")
else:
    print("  the first-join gift matches the book and guards a missing Modonomicon: OK")

# --- Modonomicon resolves recipes SERVER side, so no Fabric sync opt-in belongs here -------------
fab = io.open("fabric/src/main/java/com/chillpavz/oredetectorreborn/fabric/OreDetectorFabric.java",
              encoding="utf-8").read()
if "RecipeSynchronization" in fab:
    bad("this branch opts recipe serializers into Fabric sync. Patchouli needed that because it "
        "reads ClientRecipes; Modonomicon resolves recipes on the SERVER into display entries and "
        "syncs them with the book, so this only ships every crafting recipe in the game to every "
        "client for nothing")
else:
    print("  no needless Fabric recipe-sync opt-in: OK")

# --- the built jars carry all of it ---------------------------------------------------------------
want = set()
for dirpath, _, names in os.walk(ROOT):
    for n in names:
        rel = os.path.relpath(os.path.join(dirpath, n), "common/src/main/resources").replace("\\", "/")
        want.add(rel)
for loader in ("fabric", "neoforge"):
    jar = zipfile.ZipFile("%s/build/libs/%s-%s-%s-%s.jar" % (loader, NS, loader, MC, VER))
    names = set(jar.namelist())
    missing = sorted(want - names)
    if missing:
        bad("%s jar is missing %d book files: %s" % (loader, len(missing), ", ".join(missing[:3])))
    else:
        print("  %-8s jar carries all %d book files: OK" % (loader, len(want)))
    # every item and recipe the book names, for OUR namespace, must actually be in the jar
    for ref in sorted(item_refs):
        base = ref.split("[")[0]
        ns, path = base.split(":")
        if ns != NS:
            continue
        if "assets/%s/items/%s.json" % (ns, path) not in names:
            bad("%s jar: the book references item %s, which has no item definition" % (loader, base))
    for ref in sorted(recipe_refs):
        ns, path = ref.split(":")
        if ns != NS:
            continue
        if not any(n.endswith("data/%s/recipe/%s.json" % (ns, path)) for n in names):
            bad("%s jar: the book shows recipe %s, which does not exist" % (loader, ref))
    print("  %-8s every item and recipe the book names resolves: OK" % loader)

print("\nMODONOMICON AUDIT " + ("PASSED" if ok else "FAILED"))
sys.exit(0 if ok else 1)
