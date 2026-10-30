# Repo rules — Tensura Faction Spawn

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim — **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/tensura-faction-spawn` · **Mod ID:** `tensura_faction_spawn` · **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/tensura_faction_spawn-<version>.jar`

Places a player at their faction's capital when they first pick a race or
reincarnate. Datapack-driven: race-group tags match the player's race, spawn
rules map a race-group tag to a capital structure tag, and the capital nearest
to the world spawn is chosen. Capital cities are separate worldgen datapacks
that only append their structures to the central `#tensura_faction_spawn:capital/*`
tags.

**Dependencies**
- Required: Minecraft, NeoForge, `tensura`, `manascore`. The race API
  (`RaceAPI`, `RaceEvents`, `ManasRaceInstance`) lives in ManasCore's `race`
  module, extracted from the core artifact's nested `META-INF/jars`.

**Test:** `./gradlew runGameTestServer` — `FactionSpawnGameTests` (3 tests).
`expectedTests = 3` in `build.gradle` must match the `@GameTest` count. JUnit:
`SpawnRuleFieldsTest`, `NearestSelectionTest`.

**Gotchas**
- **Evolution vs. reincarnation is decided by the `Changeable<Boolean>` relocate
  flag on ManasCore's `SET_RACE`, never by the evolution lineage.** Tensura passes
  `true` for a first pick / reincarnation (`ReincarnationMenu`) and `false` for
  every evolution, including Cross Evolution cross-evolutions. A lineage test
  would be fragile here; mirror the flag.
- **The relocation is deferred one server tick.** Tensura does its own default
  relocation synchronously right after `SET_RACE`; running ours in the same call
  would be overwritten. The queued player is flushed on the next
  `ServerTickEvent.Post` and the destination is read from live race data then.
- **`findNearestMapStructure` cost is quadratic in `search_radius`** (it searches
  ring by ring). Keep radii modest; the shipped default is 128 chunks and the
  expand step is bounded at 512, not truly whole-dimension.
- **Rule `dimension` is optional and defers to the race.** Tensura's
  `getRespawnDimension` is per-race — only `LesserDaemonRace` overrides it (to
  `tensura:hell`) and the whole daemon chain inherits that override, so a
  hardcoded faction dimension would misplace addon races that join the
  celestials or hell tags later (the spiritfolk faction was split into
  celestials and hell per the dimension-home criterion). `set_respawn` is a tri-state
  (`never`/`if_unset`/`always`, default `if_unset`); in a bedless dimension
  (`tensura:hell` has `bed_works: false`) `if_unset` skips rather than store a
  point vanilla respawn would ignore.
- Race-group and capital tags are additive (`"replace": false`) so capital
  datapacks and addons extend them without editing this mod.
- Ecosystem roadmap lives on the tracker: #11 is the governing-spec decision
  record, #12 the generation-engine spike, #13 the future repository split.

## CI & release model - this repo

- `ci.yml` runs the template's three checks (junit, gametest, version-bump).
- `release.yml` cuts the GitHub release for `v<mod_version>` on a push to `main`
  (pre-releases only when the repo variable `ALPHA_RELEASES` is true), and
  `publish.yml` publishes the Maven artifact to the org's GitHub Packages
  registry when that release is published - see the org rulebook for the
  consumer-side auth details (`PACKAGE_READ_TOKEN`).