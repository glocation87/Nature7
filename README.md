# Nature7
Minigame engine for Paper 26.2. Runs game sessions with a lobby, map voting and a fresh copy of the map world for every match. Games are small classes built from reusable modules.

![Build](https://github.com/glocation87/Nature7/actions/workflows/build.yml/badge.svg)

<!-- TODO: screenshot or short gif of a match -->

## Games
| Game | Players | What happens |
| --- | --- | --- |
| Last Standing | 1 | Survive 10 buffed zombies at night with a sword that's nearly broken |
| Spleef | 2-16 | Dig the snow out from under everyone else, snowballs knock out blocks from range |
| SkyWars | 2-12 | Loot your island, fight for the richer centre, chests refill at 3:00, shrinking border deathmatch at 5:00 |
| Capture the Flag | 2-16 | Red vs blue with kits and respawns, first to 3 captures or the higher score at 10:00 |

## Engine
- Join from the lobby compass or `/n7 join`, wait in a waiting room, then a 15 second countdown starts the match
- Map vote between 3 random maps, closes 5 seconds before the start, ties are broken fairly
- Every match plays on its own copy of the map world, deleted afterwards. Copies left over from a crash are cleaned up on startup
- Your inventory, location, health, hunger and XP are saved before a game and restored after, even if the server crashes mid match
- Events reach only the session they belong to, by player or by match world, with priorities inside a session
- A game that throws only takes down its own session

## Modules
A game installs what it needs in its constructor. Modules run before the game in install order and tear down after it in reverse.

| Module | Gives a game |
| --- | --- |
| `TeamsModule` | Balanced teams, coloured names, no friendly fire |
| `KitModule` | Kits from `kits/<game>.yml` with a picker in the waiting hotbar |
| `SpectatorModule` | Invisible flying spectators with a teleport compass |
| `DeathModule` | No death screens, lives, respawn timers, spawn protection, kill credit |
| `StatsModule` | Games, wins, kills and deaths saved to SQLite |
| `TimelineModule` | Timed events like a chest refill, with a countdown on the sidebar |

## Installing
1. Needs Paper 26.2 and Java 25
2. Drop the jar in `plugins/` and start the server. Configurate and the SQLite driver are downloaded on first start, so that start needs internet
3. Stand where you want them and run `/n7 setlobby` and `/n7 setwaiting`

## Commands
`/nature7`, alias `/n7`

| Command | Does | Permission |
| --- | --- | --- |
| `/n7 join <game>` | Joins or starts a session | everyone |
| `/n7 leave` | Back to the lobby | everyone |
| `/n7 list` | Running sessions and their player counts | everyone |
| `/n7 stats [player]` | Games, wins, kills, deaths and K/D for each game | everyone |
| `/n7 forcestart` | Starts your session now | `nature7.admin` |
| `/n7 setlobby` / `setwaiting` | Sets the lobby or waiting room spawn | `nature7.admin` |

## Permissions
| Permission | Default | Allows |
| --- | --- | --- |
| `nature7.admin` | op | Force starting sessions, setting the lobby |
| `nature7.build` | op | Breaking and placing blocks in the lobby |

## Maps
Maps live in `plugins/Nature7/maps/<game>/<map>/`, a `map.yml` next to a `world/` folder holding the world's `region/` files. They're loaded on startup, and a broken map is skipped with the reason in the log.

```yaml
name: Colosseum 4242
authors: [LmsMaps]
spectator-spawn: {x: 0.5, y: 88.0, z: 0.5, yaw: 0.0, pitch: 90.0}
bounds:
  min: {x: -58, y: 30, z: -58}
  max: {x: 58, y: 90, z: 58}
game:                 # different for each game
  spawns:
    - {x: 32.5, y: 65.0, z: 9.5, yaw: 105.7, pitch: 0.0}
    - {x: 23.5, y: 65.0, z: 23.5, yaw: 135.0, pitch: 0.0}
```

| Game | `game:` keys |
| --- | --- |
| `last_standing` | `spawns` (the first is the player, the rest are zombie spawns) |
| `spleef` | `spawns` |
| `skywars` | `spawns`, `center`, `island-chests`, `center-chests` |
| `capture_the_flag` | `red-spawns`, `blue-spawns`, `red-flag`, `blue-flag` |

`source` and `license` are optional extra fields for crediting a map.

## Adding a game
Extend `MinigameProcess`, install the modules you need in the constructor and override the hooks you care about (`onSetup`, `onPlayerJoin`, `onPlayerLeave`, `onStart`, `onTick`, `onEnd`, `onDispose`). Then register its `GameType` in `NatureEngine`:

```java
public static GameType type(GameResources resources) {
    return new GameType("spleef", Component.text("Spleef", NamedTextColor.AQUA), Material.DIAMOND_SHOVEL,
        2, 16, SpleefMap.class, session -> new Spleef(session, resources));
}

private Spleef(SessionProcess session, GameResources resources) {
    super(session);
    install(new SpectatorModule(resources.plugin(), resources.tags()));
    deaths = install(DeathModule.builder().onEliminated(player -> checkForWinner()).build());
    install(new StatsModule(resources.stats()));
}
```

## Testing
- JUnit and MockBukkit unit tests for the engine, maps, modules, kits, stats and loot, with a JaCoCo coverage report on every run
- `tools/playtest/bots.js` plays real matches with Mineflayer bots: a Last Standing win and loss, Spleef digging and eliminations, SkyWars kits and loot, three Capture the Flag captures, then reads the stats back

## Building
```
./gradlew build      # build/libs/Nature7-1.1.0.jar
./gradlew test       # unit tests and build/reports/jacoco/test/html
./gradlew runServer  # dev server on port 25566
```

## Code
- `engine/` sessions and their state machine, modules, routing events to sessions, the HUD
- `module/` teams, kits, spectators, deaths, stats and timelines
- `games/` one package per game
- `map/` loading and validating `map.yml`, the map registry and voting
- `world/` copying a map world for each match and deleting it afterwards
- `player/` saving and restoring player state around a game
- `kit/`, `stats/`, `loot/` kit files, the SQLite stats service, weighted loot tables
- `lobby/`, `ui/`, `item/` the lobby, menus and tagged hotbar items

## License
MIT, see [LICENSE](LICENSE).
