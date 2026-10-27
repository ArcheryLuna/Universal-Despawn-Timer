# AGENTS.md

## Project overview

UniversalDespawnTimer is a Bukkit/Spigot/Paper plugin that applies configurable
despawn times and mob-pickup rules to dropped items. LifeStealZ support is
optional and must not become a hard runtime or compile-time dependency.

## Build and test

- Use the checked-in Gradle wrapper: `./gradlew test` for tests and
  `./gradlew build` for the full verification and plugin jar.
- Production sources target Java 17. Do not introduce APIs or language features
  newer than Java 17.
- Tests use Java 21 because the current MockBukkit dependency requires it.
- The built jar is written under `build/libs/`.

## Repository layout

- `src/main/java/dev/seb/despawntimer/`: plugin implementation.
- `src/main/resources/plugin.yml`: Bukkit plugin metadata, command, permission,
  and optional dependency declaration.
- `src/main/resources/config.yml`: shipped defaults and user-facing config
  documentation.
- `src/test/java/dev/seb/despawntimer/`: JUnit 5 unit and MockBukkit integration
  tests.

## Implementation guidelines

- Keep code in the `dev.seb.despawntimer` package and follow the existing Java
  style: four-space indentation, braces on the same line, and descriptive names.
- Prefer package-private implementation types and methods unless Bukkit or
  another public API requires wider visibility.
- Keep all Bukkit entity and scheduler work on the server thread. Do not add
  asynchronous access to Bukkit objects.
- Preserve rule precedence: LifeStealZ heart rule, then material override, then
  the default rule.
- Preserve timer semantics: elapsed time advances in server ticks only while an
  item's chunk is loaded. Timers must survive reloads, chunk unload/load cycles,
  item merges, and server restarts through persistent entity data.
- `default` must leave vanilla despawn behavior untouched. `never` must prevent
  despawning without changing player pickup behavior.
- A `prevent-mob-pickup` rule may block non-player living entities, but must
  never block players.
- Keep frequent item events lightweight. The expiry queue intentionally uses
  lazy invalidation and is compacted during periodic maintenance.
- Access LifeStealZ defensively through its optional hook. The plugin must still
  enable and function when LifeStealZ is absent, disabled, or incompatible.
- Treat persistent-data key changes as a data migration. Retain compatibility
  with existing dropped items or add an explicit migration path.

## Configuration and metadata

- When adding or changing configuration, update `config.yml`, parsing and
  validation in `PluginSettings`, relevant tests, and the README together.
- Reject invalid configuration with a clear path-specific error. Unknown Bukkit
  material overrides may be warned about and ignored, matching current behavior.
- When adding commands or permissions, update both `plugin.yml` and the command
  implementation/tests.
- Keep the Gradle project version, expanded `plugin.yml` version, README jar
  name, and release artifact name consistent when changing versions.

## Testing expectations

- Add focused unit tests for parsing and configuration logic.
- Add MockBukkit integration coverage for behavior involving ticks, events,
  reloads, merges, pickup, or chunk lifecycle.
- For timer changes, test exact boundary ticks and at least one pause/resume
  path; off-by-one errors are especially important here.
- Run `./gradlew test` after code changes and `./gradlew build` before release.
- Do not commit generated `.gradle/` or `build/` contents.
