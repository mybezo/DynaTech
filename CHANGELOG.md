# 1.1.0 (2026-11-14)

> [!WARNING]
> This version only runs on **Minecraft 26.1+ with Java 25**. Items that were removed (see below) stop working on existing servers.

## Additions:
- Support Minecraft 26.3 (built against Paper 26.3, Java 25)
- InfinityExpansion2 support: adds the Vex Mob Data Card (InfinityExpansion2 has its own Phantom card)
- New Mineralized Apiaries: Electromagnet, Electric Motor, Heating Coil, Sulfate, Bronze, Amethyst Shard, Plastic Sheet and Gold 24K
- All text translated to English
- GitHub Actions: manual builds upload `DynaTech.jar`, and pushing a `v*` tag publishes a GitHub Release

## Changes:
- Mineralized Apiaries always take 30 seconds and no longer depend on bees
- Material Hive always takes 30 seconds and no longer uses bees; it now also stops when its output is full
- Barbed Wire now pushes mobs away from the wire, as its description says (it used to pull them in)
- GuizhanLibPlugin is no longer required
- Removed the auto-updater (it pointed to another project's builds)

## Removals:
- Flight Gem (Angel Gem)
- Antigravity Bubble
- Bee, Robotic Bee, Advanced Robotic Bee and Scoop

## Fixes:
- Wireless Charger, Inventory Filter and Item Band no longer touch player data from async threads
- Wireless Charger no longer drains energy below zero and only checks players in range
- Inventory Filter no longer lets items through while it refreshes, and forgets players who leave
- Barbed Wire no longer gets the push direction wrong after the first mob
- Potion Sprinkler no longer errors every tick when it holds a potion without effects (e.g. a water bottle)
- Attribute rename for 26.x (`GENERIC_MAX_HEALTH` to `MAX_HEALTH`)


# 29/4/24

## Additions:
- Add Dragon Egg Mill
- Add Dragon Egg Turbine
- Allow RecipeRegistry to grab recipe via key

## Fixes:
- Regular block now drops if broken before durability = 0 for degrading generators.
