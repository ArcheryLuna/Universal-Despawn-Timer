# UniversalDespawnTimer

UniversalDespawnTimer is a lightweight Bukkit, Spigot, and Paper plugin that
gives server owners precise control over dropped-item despawn times and mob
pickup. It also supports LifeStealZ hearts without requiring LifeStealZ to be
installed.

## Features

- Set one default rule for all dropped items.
- Override the rule for individual Bukkit materials.
- Apply a dedicated, higher-priority rule to every LifeStealZ heart type.
- Use finite durations, preserve vanilla behaviour with `default`, or prevent
  despawning with `never`.
- Prevent mobs from collecting selected items without affecting players.
- Preserve elapsed timers across reloads, chunk unloads, item merges, and
  server restarts.
- Reload the configuration without restarting the server.

## Requirements

- Java 17 or newer
- Minecraft 1.19 or newer
- Bukkit, Spigot, Paper, or a compatible server implementation
- LifeStealZ is optional

## Installation

1. Download or build `UniversalDespawnTimer-1.1.1.jar`.
2. Copy the jar into the server's `plugins` directory.
3. Start or restart the server to create
   `plugins/UniversalDespawnTimer/config.yml`.
4. Edit the generated configuration and run `/despawntimer reload`.

## Configuration

The default configuration keeps vanilla behaviour for ordinary items and gives
LifeStealZ hearts a 15-minute loaded-time lifetime while preventing mobs from
picking them up.

```yaml
defaults:
  despawn-time: default
  prevent-mob-pickup: false

lifestealz-hearts:
  enabled: true
  despawn-time: 15m
  prevent-mob-pickup: true

material-overrides:
  DIAMOND:
    despawn-time: 10m
    prevent-mob-pickup: true
  COBBLESTONE:
    despawn-time: 2m
    prevent-mob-pickup: false

check-interval-ticks: 20
```

Rules are resolved in this order:

1. LifeStealZ heart rule
2. Material override
3. Default rule

### Durations

Finite durations accept ticks (`t`), seconds (`s`), minutes (`m`), hours (`h`),
or days (`d`). Examples include `6000t`, `30s`, `15m`, `2h`, and `1d`. A number
without a suffix is interpreted as seconds.

- `default` leaves the server's vanilla despawn behaviour unchanged.
- `never` keeps the item from despawning indefinitely.

Durations must be positive. Invalid values prevent the plugin from enabling or
the configuration from reloading, with the reason written to the server log.
Unknown material names are logged and ignored.

### Mob pickup

Set `prevent-mob-pickup: true` on any rule to stop non-player living entities
from collecting matching items. Players can always pick those items up.

### Timer behaviour

Timers advance in server ticks only while an item's chunk is loaded. They pause
while the chunk is unloaded or the server is offline. Timer state is stored on
the item, so it survives plugin reloads, chunk unload/load cycles, item merges,
and server restarts.

`check-interval-ticks` controls how often timer metadata is persisted and stale
tracking data is cleaned up. Item expiry is still checked every server tick.

## LifeStealZ integration

LifeStealZ is declared as a soft dependency and is accessed defensively at
runtime. When it is installed and enabled, UniversalDespawnTimer reads the
LifeStealZ `items.yml` file and applies the heart rule to every custom item whose
`customItemType` is `heart`.

If LifeStealZ is absent, disabled, or incompatible, UniversalDespawnTimer keeps
working normally and disables only the heart-specific integration. Use
`/despawntimer status` to inspect the current connection state.

## Commands and permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/despawntimer reload` | Reload the configuration and LifeStealZ integration | `universaldespawntimer.admin` |
| `/despawntimer status` | Show the plugin version and LifeStealZ status | `universaldespawntimer.admin` |
| `/udt ...` | Short alias for `/despawntimer` | `universaldespawntimer.admin` |

The permission defaults to server operators.

## Building and testing

Use the checked-in Gradle wrapper. Production code targets Java 17, while the
test suite requires a Java 21 toolchain for MockBukkit.

```text
./gradlew test
./gradlew build
```

The completed plugin jar is written to
`build/libs/UniversalDespawnTimer-1.1.1.jar`.

## Project information

- Contributors are listed in [CONTRIBUTORS.md](CONTRIBUTORS.md).
- Development setup, tests, and pull requests are described in
  [CONTRIBUTING.md](CONTRIBUTING.md).
- Security issues should be reported according to [SECURITY.md](SECURITY.md).

## License

Copyright (C) 2026 Archery Luna.

UniversalDespawnTimer is free software licensed under the GNU General Public
License, version 3 or (at your option) any later version. See
[LICENSE](LICENSE) for the complete terms.
