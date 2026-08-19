# Ore Detector Reborn 2.0

A ground-up rework of Ore Detector Reborn for Minecraft **26.1, 26.1.1 and 26.1.2** (Fabric and
NeoForge), built around the Trial Chamber. Where the 1.x line gave you twelve single-ore detectors,
2.0 gives you **one** detector that you attune to whichever ore you want to find.

> **In development.** The 1.x line continues separately and still supports Minecraft 1.20 through
> 26.2.

This version ships the **Tome of Resonance**, an in-game guide covering the whole loop. It needs
[Patchouli](https://modrinth.com/mod/patchouli), which is optional: without it you lose the book
and nothing else. Craft it from a book and a Breeze Shard.

## The loop

1. **Raid a Trial Chamber.** Breeze Rods, Breeze Shards, Breeze Crystals and Heavy Cores are the
   mod's structural materials, and every one of them comes from inside a chamber.
2. **Grind ore into dust.** Hold shears in your off hand and raw ore material in your main hand, then
   right-click. Rarer material blunts the shears faster.
3. **Brew Attunement Liquid.** A **Resonance Chamber**, built like a brewing stand but burning Breeze
   Powder, turns ore dust into bottled Attunement Liquid.
4. **Attune the detector.** Pour the liquid in. The detector now finds that ore. Load it with a
   single ore type for full range, or mix several and trade range for coverage.
5. **Scan.** Right-click a surface and the detector reports what is behind it, exactly as before.

## Two resource pools

The Attuned Ore Detector tracks wear and fuel separately:

| Pool | Capacity | Drains |
|---|---|---|
| Durability | 1000 | 1 per scan, plus more for each ore block actually found, scaled by rarity |
| Attunement Liquid | 600 units (6 bottles) | a flat 15 per scan |

## Modded ores

Modded ores are a first-class feature, not an add-on. Any ore that carries the usual common tags
plugs into the same pipeline: dust, liquid, detection. Create's zinc, Universal Ores and Seamless
Ores all work through that one mechanism rather than through special cases.

## Relationship to the 1.x line

2.0 is a **separate mod id** (`ore_detector_reborn`, where the 1.x line is `oredetector`), so the two
install side by side without colliding. Nothing carries over between them: different items, tabs,
recipes and config files. If you want the twelve classic detectors, or you play below 1.21, stay on
the 1.x line. Trial Chambers arrived in 1.21, which is why this rework cannot be backported further.

## Building

Requires **JDK 25**.

```bash
./gradlew build
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`. Ignore the `-sources` and `-javadoc`
files.

## Dependencies

| Mod | Fabric | NeoForge | Notes |
|-----|:---:|:---:|-------|
| Fabric API | required | not applicable | |
| Cloth Config | required | required | powers the config screen |
| Mod Menu | optional | not applicable | adds the config button on Fabric |
| Create | optional | optional | adds zinc to the ore pipeline |
| Universal Ores | optional | not applicable | its ore variants are detected too |

## Licence and credits

Licensed under the **[PolyForm Shield License 1.0.0](https://polyformproject.org/licenses/shield/1.0.0)**
(see `LICENSE`). The source is public and you are free to read it, learn from it, modify it and
redistribute it. The one thing the licence does not permit is using this code to provide a product
that competes with Ore Detector Reborn.

To be clear about what that does and does not cover: it is a restriction on **this code and these
assets**, not on the ideas. Game mechanics are not anyone's property, and nothing here stops you
building your own ore detection mod.

This project began as an unofficial port of [Ore Detector](https://github.com/restonic4/OreDetector)
by **restonic4** (MIT), and material still descends from it. `NOTICE` records exactly what, along
with the vanilla Minecraft art the new textures are drawn from and the CC0 project template the build
is based on. That MIT material stays available under MIT from its original author.
