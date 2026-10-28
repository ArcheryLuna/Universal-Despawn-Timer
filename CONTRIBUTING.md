# Contributing

Thanks for helping improve UniversalDespawnTimer. Bug reports, documentation
fixes, and code changes are all welcome.

## Reporting issues

Open an issue for bugs, configuration questions, and feature requests. Include
the plugin version, server software, Minecraft version, and Java version, plus
the relevant configuration and logs with secrets removed.

Report suspected vulnerabilities privately. See [SECURITY.md](SECURITY.md).

## Development setup

Compile the plugin with JDK 17 and run the tests with JDK 21. Gradle uses the
checked-in wrapper and selects those toolchains when they are installed.

```text
./gradlew test
./gradlew build
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

`./gradlew test` runs the unit and MockBukkit integration tests.
`./gradlew build` runs the full verification and writes
`build/libs/UniversalDespawnTimer-<version>.jar`. Leave generated `.gradle/`
and `build/` directories out of commits.

## Repository layout

- `src/main/java/dev/seb/despawntimer/` holds the plugin implementation.
- `src/main/resources/plugin.yml` holds Bukkit metadata, the command,
  permission, and the optional LifeStealZ dependency.
- `src/main/resources/config.yml` holds the shipped defaults and user-facing
  configuration notes.
- `src/test/java/dev/seb/despawntimer/` holds JUnit 5 unit tests and MockBukkit
  integration tests.

## Making a change

Match the existing Java style: four-space indentation, braces on the same
line, and descriptive names. Keep new types and methods package-private unless
Bukkit or another public API needs them to be wider. Production code stays on
Java 17. Test code may use Java 21 because MockBukkit requires it.

Keep Bukkit entity and scheduler work on the server thread.

These behaviours are part of the plugin contract:

- Rule precedence is the LifeStealZ heart rule, then the material override,
  then the default rule.
- Elapsed time advances in server ticks only while an item's chunk is loaded.
  Timers survive reloads, chunk unload and load cycles, item merges, and
  server restarts through persistent entity data.
- `default` leaves vanilla despawn behaviour unchanged. `never` prevents
  despawning and leaves player pickup unchanged.
- `prevent-mob-pickup` may block non-player living entities and must leave
  player pickup unchanged.
- Frequent item events stay lightweight. The expiry queue uses lazy
  invalidation and is compacted during periodic maintenance.
- LifeStealZ stays optional. Access it through the existing hook so the plugin
  still enables and works when LifeStealZ is absent, disabled, or
  incompatible.
- A persistent-data key change is a data migration. Existing dropped items
  must remain readable, or the change needs an explicit migration path.

### Configuration, commands, and versions

A configuration change updates `config.yml`, parsing and validation in
`PluginSettings`, the relevant tests, and the README together. Invalid values
fail with a clear path-specific error. Unknown Bukkit material names are
logged and ignored.

A new command or permission updates `plugin.yml` and the command
implementation and tests.

A version change stays consistent across the Gradle project version, the
expanded `plugin.yml` version, the README jar name, and the release artifact
name.

## Tests

Add focused unit tests for parsing and configuration logic. Add MockBukkit
coverage for behaviour that involves ticks, events, reloads, merges, pickup,
or the chunk lifecycle. A timer change needs an exact boundary-tick test and
at least one pause and resume path.

Run `./gradlew test` before opening a pull request. Run `./gradlew build`
before a release.

## Pull requests

Keep each pull request focused on one change. Describe what changed and why,
and note how you tested it. To be listed, add your name and a short
description of your work to [CONTRIBUTORS.md](CONTRIBUTORS.md) in the same
pull request. List people only with their consent.

## License

Contributions are licensed under the GNU General Public License, version 3 or
any later version, the same terms as the rest of UniversalDespawnTimer. See
[LICENSE](LICENSE).
