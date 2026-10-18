# Complete project audit for user testing

This audit covers every tracked project file and every new nonignored file in this working copy. It began from commit `ffe5e1c5da80e685cf6e90ee6c6c49e00dd94ad0`. The owner authorized committing these audit changes on October 18, 2026.

## Findings fixed

- **Beacon selections:** a modified client could submit powers above its pyramid tier, or select an unrelated secondary power. The server now validates the primary tier and secondary choice before spending netherite. Runtime application also rejects invalid saved powers. Valid tier II/III behavior remains tested.
- **Offhand hookline:** a rusty weapon in the main hand could also trigger its melee infection callback during hookline damage, rolling exposure twice and charging the victim's gloves twice. Active-item damage now scopes out the extra callback, with nested/exception cleanup tested.
- **Reflected damage:** Thorns damage could inherit Tetanus from the armor wearer's held rusty weapon. Reflected armor damage no longer counts as a rusty melee strike; real melee still infects.
- **Antitoxin accounting:** wrong/empty stacks and spectator consumption are rejected; consumption is server-authoritative. Stacked, final-dose, and full-inventory bottle returns are verified.
- **Test stability:** the exact melee-damage fixture now isolates mutation healing, and the hookline cooldown fixture has an explicit unobstructed sightline. These changes keep genuine damage/cooldown assertions rather than accepting the wrong result.
- **Release inspection:** checks now decode the packaged PNGs, check every item/status sprite border, detect duplicate ZIP names, and compare every packaged resource to the source. Version expansion and Loom's added refmap are checked structurally. The old recipe-guide claim of a visible 7:30 Tetanus duration was corrected.

## File coverage

`tools/Audit-Files.ps1` inventories every tracked and new nonignored file, records SHA-256 and size, and validates the applicable format. The full per-file report is generated locally at `.tools/final-file-inventory.json`; it stays outside its own input inventory to avoid a self-referential hash.

- All **81 production Java files** were reviewed across initialization, registries, diseases, state persistence, prevention, items, projectiles, traps, mobs, commands, beacons, networking, rendering and viewer integration. Production and test Java are compiled by the final build.
- All test Java, test descriptors and test mixins were reviewed for meaningful assertions, fixture isolation and failure handling. Development code stays outside the distributable JAR.
- Every JSON file is parsed. Gradle and live Minecraft reloads check resource references, recipes, loot, tags, advancement criteria, models, metadata and mixins. Regeneration rebuilt every generated resource; all localization values remained identical.
- Every retained PNG, including source art, logo and screenshots, is decoded. Every runtime item/status sprite has transparent outer margins. Both original MP3 and packaged Vorbis audio are checked with FFprobe and fully decoded with FFmpeg.
- Every PowerShell script is syntax-parsed and its file operations reviewed. The C# sprite isolator is compiled and exercised by full regeneration. Gradle configuration and Windows wrapper are exercised by builds; the POSIX wrapper is source-reviewed on this Windows host. Wrapper JAR and distribution hashes are checked.
- Documentation, prompt provenance, Git attributes/ignores, properties, validation XML and release metadata are inspected. UTF-8 decoding is strict and local Markdown links are checked. Build caches, downloaded tools, `.git` internals and generated game saves are excluded from the project-file inventory.

## Final verification

Results are recorded in [VALIDATION.md](../VALIDATION.md), [server XML](../validation/gametest-results.xml), [client summaries](../validation/client-results.txt) and [release audit](../validation/release-audit.json). Final checks include the complete dedicated-server suite twice, graphical clients separately with JEI, with REI, and with neither viewer, then packaging/file checks again. The release auditor rejects deliberately altered disposable JAR copies, duplicate ZIP entry names and an absolute Windows drive path. The final server suite passes all 103 tests on both complete runs. The file inventory covers 289 files.

## User playtest

Use the JAR linked in VALIDATION.md on both client and server with Fabric API. Test in a copy of a world first. Prioritize real multiplayer: nail/rebar damage, floating-water rusting, vaccine/glove prevention, long Lockjaw and milk/antitoxin handling, infected animal drops and both meat advancements, potion symptoms, hookline cooldown, Wither rituals and beacon payments. Check your own resource pack and other mods, if any.

A file audit and passing regression suite reduce risk; they cannot establish absolute freedom from bugs, every network condition, long-running world behavior or compatibility with every modpack. The recorded final checks passed. These changes are committed at the owner's request; further multiplayer playtesting remains useful.