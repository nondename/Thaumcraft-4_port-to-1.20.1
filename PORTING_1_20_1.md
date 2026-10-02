# Thaumcraft 4 → Minecraft 1.20.1 Port Roadmap

This branch ports the community-maintained Thaumcraft 4.2.3.5 codebase from Minecraft 1.12.2 / Forge to Minecraft 1.20.1 / Forge.

## Ground truth

1. Original Thaumcraft 4.2.3.5 behaviour and values.
2. `Atom-gnomov/Thaumcraft-4-port-to-1.12.2` as the maintained, documented reference implementation.
3. The existing parity tests, porting notes and changelog in this repository.

The rule remains: **do not invent values or mechanics when the original can be consulted.** Modernize implementation details, not gameplay semantics.

## Branch strategy

- `main` — stays close to the 1.12.2 upstream and is used for upstream synchronization.
- `port/1.20.1-bootstrap` — current modernization branch.
- Future upstream-compatible fixes should be isolated so they can be proposed back to Atom-gnomov.

## Source layout during migration

- `mod/src/main/**` — legacy 1.12.2 implementation; kept as reference and intentionally excluded from the 1.20.1 compile.
- `mod/src/modern/**` — new Forge 1.20.1 implementation.

Systems are migrated deliberately from legacy to modern rather than bulk-converted.

## Milestones

### 0.1.0-bootstrap
- [x] Java 17 / ForgeGradle 6 build skeleton
- [x] Forge 1.20.1 mod entrypoint
- [x] Isolated modern source set
- [ ] `build` verified in CI/local environment
- [ ] client startup verified manually
- [ ] world create/save/reopen verified manually
- [ ] dedicated server startup verified

### 0.2.0-aspects
- [ ] primal aspects
- [ ] compound aspects
- [ ] aspect combinations
- [ ] stable aspect registry/API
- [ ] parity data/tests against the 1.12.2 reference

### 0.3.0-content-surface
- [ ] core items
- [ ] core blocks
- [ ] sounds/particles
- [ ] blockstates/models/resources
- [ ] legacy metadata → modern registry mapping manifest

### 0.4.0-first-magic
- [ ] scanning / thaumometer
- [ ] player knowledge
- [ ] research / Thaumonomicon
- [ ] aura nodes
- [ ] wands / rods / caps / vis
- [ ] Arcane Workbench

### 0.5.0-alchemy
- [ ] crucible
- [ ] alchemical furnace
- [ ] alembics
- [ ] jars
- [ ] essentia tubes and suction

### 0.6.0-infusion
- [ ] infusion matrix and pedestals
- [ ] stabilizers / instability
- [ ] essentia and ingredient consumption
- [ ] infusion recipes / enchantments

### 0.7.0-golems
- [ ] golem entities
- [ ] cores/upgrades/accessories
- [ ] tasks and marker system
- [ ] modern AI/pathfinding implementation with legacy behaviour parity

### 0.8.0-world-eldritch
- [ ] ores / magical trees / biomes
- [ ] modern worldgen and biome modifiers
- [ ] taint
- [ ] eldritch progression
- [ ] Outer Lands

### 0.9.0-core-parity
- [ ] equipment / special tools / foci
- [ ] renderers / GUIs / particles
- [ ] multiplayer and dedicated-server audit
- [ ] documented parity deviations

### 0.10.x — Thaumic Tinkerer / KAMI
Port only after the TC4 core is stable.

### 0.11.x — End Legacy
Keep original port content clearly separated from TC4 parity work.

## Compatibility decisions

- Minecraft: **1.20.1**
- Forge build baseline: **47.4.22** (chosen to match the target modpack baseline)
- Java: **17**
- ForgeGradle: **6.0.54**
- Accessories: migrate Baubles integration to Curios later, behind a thin Thaumcraft-facing abstraction.
- Old 1.12 binary compatibility is not a goal. Familiar source/API concepts are.

## Testing strategy

Three layers are expected:

1. **Reference/parity data** extracted from the working 1.12.2 implementation.
2. **Unit tests** for pure gameplay logic and values.
3. **Forge GameTests/runtime tests** for machines, multiblocks, networks, world interactions and other behaviour that cannot be proven by static checks.

A system is not considered ported merely because it compiles.
