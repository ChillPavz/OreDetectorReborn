# Ore Detector Reborn

Simple handheld **ore detectors** for Minecraft **26.2** (Fabric & NeoForge).

Craft a detector, **right‑click a surface**, and it scans the blocks *behind* that surface for a
specific ore — a short beep and an action‑bar message tell you whether it's there and how much.
A lightweight, no‑cheats way to decide where to start digging.

> **Unofficial, updated port of [Ore Detector](https://modrinth.com/mod/ore-detector) by restonic4.**
> This is a community continuation for Minecraft 26.2; it is not made by or affiliated with the original author.
> Original mod © restonic4, MIT. Port and expansion by chillpavz, MIT.

## Features

- **Eleven detectors:** Iron, Gold, Diamond, Emerald, Quartz, Copper, Coal, Amethyst, Netherite (Ancient Debris), Lapis Lazuli and Redstone (plus an optional Zinc detector with Create).
- **Directional scanning:** point at the ground to reach deep (16 blocks), or at a wall/ceiling for a
  shorter range (8 blocks), across a 3×3 column.
- **Clear feedback:** an action‑bar message tinted to the ore's colour tells you the exact count
  (e.g. *"Detected 4 Iron Ore nearby"*), plus a beep.
- **Durability & repair:** detectors wear down (1 per scan + 1 per ore found), repair in an anvil
  with their material, and support Mending/Unbreaking.
- **In‑game config** (via Cloth Config) for reach, column radius, cooldown, durability and volume —
  all bounded to sane limits. Fabric uses Mod Menu; NeoForge uses its built‑in mod‑list config button.
- **Optional Create integration:** if [Create Fly](https://modrinth.com/mod/create-fly) is installed,
  a **Zinc Detector** is added automatically.
- **Optional Universal Ores integration:** if [Universal Ores](https://modrinth.com/mod/universal_ores)
  is installed, the Coal, Iron, Gold, Copper, Lapis Lazuli, Redstone, Emerald, Diamond and Quartz
  detectors also pick up its andesite / diorite / granite / tuff / calcite / blackstone / basalt ore
  variants, counted together with the vanilla ore.

## Dependencies

| Mod | Fabric | NeoForge | Notes |
|-----|:---:|:---:|-------|
| Fabric API | required | — | |
| Cloth Config | required | required | powers the config screen |
| Mod Menu | optional | — | adds the config button on Fabric |
| Create Fly | optional | — | unlocks the Zinc Detector |
| Universal Ores | optional | — | its ore variants are detected too (that mod is Fabric/Quilt only) |

## Building

Requires **JDK 25**.

```bash
./gradlew build
```

Output jars are in `fabric/build/libs/` and `neoforge/build/libs/` (ignore the `-sources` /
`-javadoc` files).

## Credits & License

- Original **Ore Detector** by **restonic4** — https://github.com/restonic4/OreDetector
- Multi‑loader project structure based on Jared's [MultiLoader‑Template](https://github.com/jaredlll08/MultiLoader-Template).
- Licensed under the **MIT License** (see `LICENSE`), preserving the original author's copyright.
