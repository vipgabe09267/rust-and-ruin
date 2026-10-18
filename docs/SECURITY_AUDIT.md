# Rust & Ruin 0.1 security review

Review date: October 17, 2026. Scope: this mod's Java code, custom protocol, prevention mechanics, commands, crafting/container behavior, retained resources and distributable JAR. Minecraft, Java, Fabric internals and arbitrary third-party mods are not a complete dependency penetration-test target here.

## Results and fixes

| Area | Finding or risk checked | Implemented control and evidence |
| --- | --- | --- |
| Client packet handling | Malformed server messages could throw while reading fields. | Decoder enforces lengths, flags and stage bounds; handlers reject RuntimeExceptions. Tests cover valid/invalid shapes, truncated input, trailing bytes and 2,000 deterministic random payloads. Graphical clients receive four malformed messages through actual networking handlers. |
| Configuration JSON | Unvalidated synchronized configuration could carry invalid probabilities or deeply nested input. | 8 KiB payload ceiling, flat primitive-only JSON, quoted-string-aware depth checks, no trailing bytes, and normal ModConfig validation. Tests include deeply nested JSON, invalid VarInts, scalar/null inputs and out-of-range settings. |
| Client state growth and logging | Unbounded infection UUID entries and repeated warning logs could grow under a hostile server. | Infection cache is capped at 4,096 UUIDs and cleared on disconnect. Invalid-packet warnings are limited to one per connection. The graphical run checks continued normal operation after rejected packets. |
| Prevention authority | Client-controlled duration or chance would permit cheating. | Server reads its own validated configuration. The vaccine ignores forged item NBT duration. Gloves must actually occupy the offhand and lose durability on the server. No new client-to-server gameplay messages exist. |
| Nested infection callbacks | A global bypass flag could accidentally skip prevention for another entity. | Checked scopes are keyed to one entity, restore nested parent scopes and clear in finally blocks. Regression asserts cross-entity isolation and exception cleanup. |
| Mutation and refresh | A blocked infection must not cure, refresh or permanently mutate a creature. | Exposure checks happen before replacing the illness; blocked contact does not roll mutation. Tests preserve existing timers and reject mutation under guaranteed prevention. Status-immune mob types retain the ordinary mutation path after an allowed exposure. |
| Container accounting | Crafting/consuming doses could duplicate or lose bottles. | Vaccine crafting returns exactly the antitoxin and honey containers. Each Survival drink consumes one dose and returns one bottle. Creative, empty/wrong-item, full-inventory and normal use-tick cases are tested. |
| Commands | A helper called outside the command tree could bypass permissions or accept non-finite coordinates. | Both tree and summon helper enforce operator level 2. Helpers reject NaN/infinity and invalid world positions. Tests cover non-operators and all non-finite coordinate cases. No arbitrary NBT command argument is accepted. |
| Hookline | Repeated direct use could bypass reliance on the interaction manager's cooldown. | Item use checks cooldown, spectator state, life state and held item; raycast respects solid walls. Tests reset damage immunity and attempt repeated use, plus a blocked-wall hit. |
| Field guide | A book must not execute NBT commands, URLs or scripts. | The guide is a local read-only screen from trusted localization text. It sends no gameplay packets, parses no user book NBT, never consumes its item and bounds chapter/scroll indices. Every chapter and extreme indices are exercised graphically. |
| Release and build integrity | Development classes, private files or altered wrappers must not enter the release. | Audit-Release.ps1 checks archive paths/content, metadata, mandatory dependencies, required assets, sprite borders, advancement conditions and the official wrapper checksum. The Gradle distribution checksum is pinned. |

## Additional final-audit controls

The [complete-file audit](FULL_AUDIT.md) adds beacon selection validation on the server: clients cannot select locked powers or unrelated secondaries, and invalid requests do not consume payment. Runtime application rejects invalid saved powers. Offhand hookline damage cannot inherit a second exposure from the main-hand weapon, and reflected Thorns damage cannot inherit rusty melee infection. Antitoxin consumption now rejects wrong/empty items and spectators and handles full inventories on the server.

The release auditor compares every source resource with its packaged bytes, accounting explicitly for version expansion and Loom's refmap field. It decodes all packaged PNGs, checks all item/status margins and rejects duplicate ZIP entry names. Disposable copies with altered PNG data and duplicate entries are rejected as expected. The file auditor covers every tracked/new nonignored project file; the complete resource generator is exercised, all PNGs decoded, and both audio files fully decoded.

## Protocol limits

- Infection presence: exactly 17 bytes, UUID plus strict 0/1 flag.
- Symptom: exactly 2 bytes, stage 0–2 plus strict 0/1 flag.
- Configuration: at most 8,192 payload bytes, a bounded string, flat JSON object and validated scalar fields.
- Only the client registers mod packet receivers. No new custom client-to-server channel is introduced by the vaccine, gloves or guide.

Malformed payload tests exercise the real decoder, not a second implementation. Parser fuzzing catches RuntimeExceptions as expected rejections; fatal Errors are not swallowed by the test. In the graphical runs the client receives malformed config, nested config, short presence and invalid-stage symptom messages, logs one warning, and then continues its normal feature checks.

## Prevention and abuse invariants

Default vaccine protection is 80% for 1,200 seconds; gloves provide 50% contact protection with 192 base durability. Combined contact prevention is 90%, calculated without independent double rolls. Tests cover exact boundaries, the final glove durability use, main-hand/inventory exclusions, food/potion exclusions for gloves, saved equipment/status state and natural vaccine expiry.

Vaccine duration is bounded to 1–7,200 seconds; protection chances are finite and clamped to 0–1. Vaccination does not cure Tetanus/Lockjaw, block Salmonella or reduce weapon damage. Milk removes the beneficial vaccine status while retaining the existing Tetanus cure restriction. Ordinary enchantment durability rules remain applicable.

## Tooling and upstream lookup

The checked-in Gradle 8.6 wrapper JAR matches the official SHA-256:

`d3b261c2820e9e3d8d639ed084900f11f4a86050a8f83342ade7b6bc9b0d2bdd`

The distribution download is pinned to:

`9631d53cf3e74bfa726893aee1f8994fee4e060c401335946dba2156f440f24c`

Both values come from the [official Gradle checksum reference](https://gradle.org/release-checksums/). The download pin applies to wrapper downloads; it is not a claim that every external cached file was independently re-downloaded and audited.

The [Fabric Loader](https://github.com/FabricMC/fabric-loader/security/advisories) and [Fabric API](https://github.com/FabricMC/fabric-api/security/advisories) maintainer advisory pages showed no published advisories when checked. That limited lookup does not establish that Minecraft, Java, transitive libraries or every mod are vulnerability-free; it is not a full software-composition scan.

No runtime process execution, arbitrary remote HTTP calls, Java object deserialization, or executable book links were found in this mod's production source review. Configuration file access uses the fixed local mod config path. Normal Minecraft/Fabric networking and resource APIs remain in use.

## Reproduce and limits

```powershell
.\tools\Build.ps1 -Test
.\gradlew.bat -PwithJei runClientSmoke --console plain
.\gradlew.bat -PwithRei runClientSmoke --console plain
.\tools\Audit-Release.ps1
```

The complete functional suite is run again before user testing. The owner authorized committing the final audit fixes on October 18, 2026. Read [VALIDATION.md](../VALIDATION.md) for exact results, screenshots and artifact hash.

This review reduces specific identified risks. It cannot prove 100% correctness or security, replace independent penetration testing, or establish compatibility with arbitrary modpacks and network conditions. No known unresolved issue was observed in the reviewed mod paths after the recorded checks; upstream game/runtime vulnerabilities require separate monitoring.
