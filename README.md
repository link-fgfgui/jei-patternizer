# JEI Patternizer

A pure-client Minecraft 1.20.1 (Forge + Fabric) mod that encodes Applied Energistics 2 /
Refined Storage patterns from a [JEI Crafter](../jeicrafter) recipe tree — the JEI counterpart
of [EMI Patternizer](EMI-Patternizer-master).

This repository currently contains a compiling MultiLoader skeleton copied from JEI Crafter.
The encode pipeline is not implemented yet.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.30 / Fabric Loader 0.16.10 + Fabric API
- JEI 15.21.0.148+
- JEI Crafter 1.0.0+ (required predecessor)

## Build

JEI Crafter is resolved from `mavenLocal`. Publish it first:

```bash
cd ../jeicrafter && ./gradlew publishToMavenLocal
cd ../jei-patternizer && ./gradlew build
```

## License

GNU Lesser General Public License v3.0 (LGPL-3.0-only). See [LICENSE](LICENSE).
