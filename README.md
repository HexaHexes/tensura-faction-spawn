# Tensura Faction Spawn

Send a player to their faction's capital when they first pick a race or
reincarnate.

## What it does

- When a player picks their race for the first time, they are moved to their
  faction's capital.
- Reincarnating into a new race moves them to the new faction's capital.
- Evolving a race (for example Human into Human Saint) never moves the player -
  evolution stays where you are.
- The capital is the nearest matching city to the world spawn, so factions
  cluster near the world's origin rather than scattering.
- If no capital is found nearby, the search widens before giving up.

The mod ships nothing to explore on its own: it reads a datapack, and capital
cities are added by separate capital datapacks.

## Requirements

| | Mod | Why |
|---|---|---|
| Required | Minecraft | - |
| Required | NeoForge | - |
| Required | Tensura: Reincarnated | Provides the race system |
| Required | ManasCore | Provides races, race tags and the race-set signal |

## Datapack reference

Everything is datapack-driven, so a pack can retarget spawns without touching the
mod.

### Race-group tags

`data/<namespace>/tags/manascore_race/races/<group>.json` - which races belong to
a faction. Shipped groups: `humans`, `elves`, `dwarves`, `spiritfolk`,
`beastfolk`, `aquatic`, `insect`, `monsters`, `goblins`, `lizardman`. They are
additive, so an addon's races can be added with `"replace": false`.

### Capital structure tags

`data/<namespace>/tags/worldgen/structure/capital/<group>.json` - the cities that
count as a faction's capital. A capital datapack adds its structure here and the
spawn mod picks it up automatically.

### Spawn rules

`data/<namespace>/tensura_faction_spawn/spawn_rules/<id>.json`:

```json
{
  "race": "#tensura_faction_spawn:spiritfolk",
  "structures": "#tensura_faction_spawn:capital/spiritfolk",
  "dimension": "tensura:hell",
  "search_radius": 128,
  "expand_on_missing": true,
  "set_respawn": true,
  "priority": 0
}
```

- `race` - the race-group tag this rule applies to.
- `structures` - the capital structure tag to search.
- `dimension` - the dimension the capitals live in (default `minecraft:overworld`).
- `search_radius` - search radius in chunks (default 128). Larger values cost
  more, because the search works outward ring by ring.
- `expand_on_missing` - widen the search once before giving up (default true).
- `set_respawn` - also make the capital the player's respawn point (default false).
- `priority` - when several rules match, the highest priority wins (then the race
  tag name, for a deterministic tie-break).

## Operators

```
/tensurafactionspawn rules      # list the loaded rules
/tensurafactionspawn locate     # where the caller's race would be sent
/tensurafactionspawn relocate   # move the caller there now
```

## Credits

- Built on [NeoForge](https://neoforged.net/).
- Uses the race tag system from ManasCore / Tensura: Reincarnated.

## License

GNU Lesser General Public License v2.1 - see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.
