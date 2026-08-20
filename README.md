# Ore Detector Reborn

One ore detector, attuned to whatever you want to find, built around the Trial Chamber.
For Minecraft **26.2** on **Fabric** and **NeoForge**.

Instead of carrying a separate detector for every ore, you brew an **Attunement Liquid** from that
ore and pour it in. The detector finds what it is attuned to, and its range depends on how focused
you keep it.

## The loop

1. **Raid a Trial Chamber.** Breeze Rods, Breeze Shards, Breeze Crystals and Heavy Cores are the
   mod's structural materials, and every one of them comes from inside a chamber.
2. **Grind ore into dust.** Hold shears in your off hand and the material in your main hand, then
   right-click. The input is the processed form: the ingot where one exists, otherwise whatever the
   ore drops. Rarer material blunts the shears faster.
3. **Brew Attunement Liquid.** A **Resonance Chamber**, built like a brewing stand but burning
   Breeze Powder, turns ore dust into bottled Attunement Liquid.
4. **Attune the detector.** Pour a bottle in. The detector now finds that ore.
5. **Scan.** Right-click a surface and the beam fires into it, reporting what it finds in the
   action bar, tinted per ore.

## Focus beats capacity

Range is decided by how many **distinct** liquids are loaded, not by how full the tank is.

| Distinct liquids | Down | Sideways | Column |
|---|---|---|---|
| 1 | 24 | 18 | 3x3 |
| 2 | 16 | 12 | 5x5 |
| 3 or more | 8 | 6 | 7x7 |

A focused detector sees deep and narrow; a loaded one sees wide and shallow. The tank holds
**750 mB**, three bottles, across at most six distinct ores.

## Two resource pools

| Pool | Capacity | Drains | Restored by |
|---|---|---|---|
| Durability | 1000 | a flat 1 per scan | Breeze Shard, 250 at a time |
| Attunement Liquid | 750 mB | 1 mB per ore block found, from that ore's own charge | pouring in another bottle |

Draining a charge back out is a held right-click on a cauldron. The liquid is nullified on the way
out, so one **Resonance Cauldron** and one **Nullified Bucket** cover every mix you could make.

## Goggles

Wear the goggles and a scan draws what it found as coloured boxes **through the rock**, for about
twelve seconds. They show nothing on their own: this is not free x-ray vision, it draws only what
the detector already reported and only what the liquid actually paid for, nearest first.

Each visualisation adds **Strain**, which bleeds off on its own. Push past the ceiling and the
goggles give you Nausea and refuse to draw until they recover. The detector itself is never
blocked, so the numbers keep coming even when the picture will not. All three strain values are
configurable, and the effect can be switched off entirely.

An eleven step **advancement tree** lays the whole progression out in game.

## Modded ores

Create's zinc grinds and detects like any vanilla ore, and the ore variants added by **Seamless
Ores** and **Universal Ores** count as their plain counterparts. They are matched by name, so a
mod you do not have installed simply never matches anything.

Wider support that picks up any ore mod through the common ore tags is planned.

## Relationship to Ore Detector Legacy

This is a separate mod from **Ore Detector Legacy**, which is where the original twelve single-ore
detectors live. Different mod id (`ore_detector_reborn`, where Legacy is `oredetector`), so the two
install side by side without colliding: different items, tabs, recipes and config files. Nothing
carries over between them.

If you want the classic detectors, or you play below 1.21, use Legacy. Trial Chambers arrived in
1.21, which is what this rework is built on.

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
| Seamless Ores | optional | optional | its ore variants are detected too |
| Universal Ores | optional | not applicable | its ore variants are detected too |

## Licence and credits

Licensed under the **[PolyForm Shield License 1.0.0](https://polyformproject.org/licenses/shield/1.0.0)**
(see `LICENSE`). The source is public and you are free to read it, learn from it, modify it and
redistribute it. The one thing the licence does not permit is using this code to provide a product
that competes with Ore Detector Reborn.

This project began as an unofficial port of [Ore Detector](https://github.com/restonic4/OreDetector)
by **restonic4** (MIT), and material still descends from it. `NOTICE` records exactly what, along
with the vanilla Minecraft art the new textures are drawn from and the CC0 project template the build
is based on. That MIT material stays available under MIT from its original author.
