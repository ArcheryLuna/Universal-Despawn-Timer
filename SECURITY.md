# Security Policy

## Supported versions

Security fixes are provided for the latest release line. Users should reproduce
an issue on the newest available version before reporting it.

| Version | Supported |
| --- | --- |
| 1.1.x | Yes |
| 1.0.x and earlier | No |

## Reporting a vulnerability

Please do not open a public issue for a suspected vulnerability. Report it
privately by emailing `me@archery-luna.com` with the subject
`UniversalDespawnTimer security report`.

Include as much of the following information as possible:

- the affected plugin version;
- the server software, Minecraft version, and Java version;
- a description of the issue and its potential impact;
- clear steps or a minimal example that reproduces it;
- relevant configuration, logs, or stack traces with secrets removed; and
- any suggested mitigation or fix.

You will receive an acknowledgement when the report has been reviewed. Please
allow time to investigate and prepare a fix before disclosing the issue
publicly. If the report is accepted, the project will coordinate a reasonable
disclosure date and credit the reporter unless anonymity is requested.

## Scope

The policy covers vulnerabilities in UniversalDespawnTimer itself. Problems in
Bukkit, Spigot, Paper, LifeStealZ, Java, or another dependency should be
reported to that project's maintainers unless UniversalDespawnTimer uses the
dependency in an unsafe way.

Reports about ordinary bugs, configuration questions, or feature requests do
not need private handling and may be submitted through the project's normal
issue tracker.
