# Nature7
Minigame engine for Paper 26.2. Runs game sessions with a lobby, map voting and a fresh copy of the map world for every match. Games plug in as small classes.

<!-- TODO: screenshot or short gif of a match -->

## Features
- Join from the lobby compass or `/n7 join`, wait in a waiting room, then a 15 second countdown starts the match
- Map vote between 3 random maps, closes 5 seconds before the start, ties are broken fairly
- Every match plays on its own copy of the map world, deleted afterwards. Copies left over from a crash are cleaned up on startup
- Your inventory, location, health, hunger and XP are saved before a game and restored after, even if the server crashes mid match
- Protected lobby, sidebar and bossbar HUD, game selector and vote menus whose items can't be taken out

## Games
| Game | Players | What happens |
| --- | --- | --- |
| Last Standing | 1 | Survive 10 buffed zombies at night on the voted map with a sword that's nearly broken |

## Installing
1. Needs Paper 26.2 and Java 25
2. Drop the jar in `plugins/` and start the server. Configurate is downloaded on first start, so that start needs internet
3. Stand where you want them and run `/n7 setlobby` and `/n7 setwaiting`

## Commands
`/nature7`, alias `/n7`

| Command | Does | Permission |
| --- | --- | --- |
| `/n7 join <game>` | Joins or starts a session | everyone |
| `/n7 leave` | Back to the lobby | everyone |
| `/n7 list` | Running sessions and their player counts | everyone |
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
game:                 # per game, Last Standing wants spawns
  spawns:             # the first is the player, the rest are where zombies come from
    - {x: 32.5, y: 65.0, z: 9.5, yaw: 105.7, pitch: 0.0}
    - {x: 23.5, y: 65.0, z: 23.5, yaw: 135.0, pitch: 0.0}
```
`source` and `license` are optional extra fields for crediting a map.

## Adding a game
Extend `MinigameProcess` and override the hooks you need (`onSetup`, `onPlayerJoin`, `onPlayerLeave`, `onStart`, `onTick`, `onEnd`, `onDispose`). Describe it with a `GameType` and register it in `NatureEngine`:

```java
public static final GameType TYPE = new GameType(
    "last_standing",                                   // id, also the maps folder name
    Component.text("Last Standing", NamedTextColor.GREEN),
    Material.IRON_SWORD,                               // icon in the game selector
    1, 1,                                              // min and max players
    LastStandingMap.class,                             // record the game: section of map.yml loads into
    LastStanding::new);

registry.register(LastStanding.TYPE);
```

## Building
```
./gradlew build      # build/libs/Nature7-0.1.0-SNAPSHOT.jar
./gradlew test       # JUnit + MockBukkit
./gradlew runServer  # dev server on port 25566
```

## Code
- `engine/` sessions, the state machine (waiting, starting, active, ending), routing events to the right session and the HUD
- `games/` one package per game
- `map/` loading and validating `map.yml`, the map registry and voting
- `world/` copying a map world for each match and deleting it afterwards
- `player/` saving and restoring player state around a game
- `lobby/`, `ui/`, `item/` the lobby, menus and tagged hotbar items

## License
<!-- TODO: pick a license -->
