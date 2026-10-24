# Despawn and full source review - October 19, 2026

Baseline: local commit `fcef5a6`. This follow-up is pending owner testing and is not committed.

## Review scope

The review inspected all project Java source, including the common code, client code, mixins, optional recipe-viewer integrations, GameTests and graphical harness. It also inspected the resource generators, artwork/import helpers, package/file auditors, C# sprite isolator and Gradle build scripts. The [source inventory](../validation/source-review.json) records each file, line count and SHA-256 so the exact reviewed source can be identified. Counts include blank lines and comments. Generated Gradle wrapper code and third-party libraries are checked separately rather than represented as original mod source.

Source reading is combined with compiler, resource, dedicated-server, integrated-client and archive checks. Reading a file is not a guarantee that every possible behavior in it has been exercised.

## Confirmed issues and fixes

| Issue | Before | After |
| --- | --- | --- |
| Mutation forced persistence | Both Tetanus and Rabies mutation called `setPersistent()` on every mob. This bypassed native despawning and excluded those creatures from ordinary mob-cap accounting. Natural variant spawning could keep adding mobs. | Mutation no longer sets persistence. Wild variants follow the rules of their original species. Existing name, taming, equipment and other vanilla persistence remains intact. |
| Existing saves retained the fault | Simply removing the conversion calls would leave previously saved `PersistenceRequired:1b` variants stuck. | A one-time load repair clears legacy forced persistence for unprotected variants. A saved rules-version marker prevents repeated repair or changing later intentional persistence. |
| Weaker exposures downgraded illness | A weak arrow or lower-level hit removed the current infection, replaced it with a weaker/shorter one, and cleared Lockjaw progress. Infinite command-given infections could become finite. | Weaker exposures preserve an existing stronger infection. Infinite infections stay infinite. Disorientation tracks the effective retained infection. |
| Valid configured duration was truncated | Configuration accepted up to 3,600 seconds, but item/combat infection silently capped at 18,000 ticks (15 minutes). | Valid one-hour settings are honored, with room for the documented 25% enchanted-arrow combination bonus. |
| Movement validation could be bypassed during spasms | The freeze handler could cancel a movement packet and copy invalid rotation values into a teleport before vanilla checked the packet. | Non-finite coordinates or rotations pass through to vanilla validation instead of entering the mod's teleport branch. A real handler regression confirms rejection. |
| Command reported changes it did not make | `/rustandruin tetanize` incremented its success count for rabid mobs even though conversion correctly refused to replace their variant. | The count increases only when a target actually becomes tetanized. |

The arrow combination bonus is now calculated from the incoming hit's duration before merging illnesses. This prevents duration compounding with the new preservation behavior. It is a regression guard for this change, not a separately demonstrated exponential-duration bug in the previous release.

## Existing-world repair behavior

Install the updated JAR on client and server and restart the world/server. Affected entities are repaired as their chunks load. Normal vanilla distance and difficulty checks then control their despawning; the mod does not instantly kill nearby mobs or purge all loaded entities.

The repair preserves legacy variants with recognizable player-retention data: custom names, owners/taming, leashes, passengers, buckets, NoAI, and picked-up equipment with guaranteed drop chances. The special named Wither is preserved. Ordinary non-variant mobs are untouched, and new-format saves retain explicitly requested persistence.

Cows and other naturally persistent livestock still follow Minecraft's ordinary animal rules. Removing the mutation's persistence flag lets eligible variants count toward normal spawning limits; it does not make farm animals disappear on a timer.

Old saves do not record whether an otherwise unmarked persistent variant was deliberately retained by an operator. That historical intent cannot be reconstructed. For an intentionally permanent unnamed legacy variant, an operator can explicitly preserve it with the rules marker and `PersistenceRequired` in its NBT; naming the creature is also recognized by the repair.

## Added regression coverage

Sixteen tests cover fresh normal-cap variants, actual hard-distance despawning, nearby survival, legacy Tetanus/Rabies loading, new intentional persistence, names, picked-up equipment, ownership/leash/bucket/passenger safeguards, ordinary-mob exclusion, livestock/taming, native conversion, weak-hit preservation, repeated combined arrows, long/infinite infection durations, malformed movement, and command accounting.

The random-duration fixture now clears the previous illness before sampling each new infection, avoiding a rare false failure caused by cumulative duration preservation. The complete suite contains 173 required tests. Final run evidence and the current artifact checksum are recorded in [VALIDATION.md](../VALIDATION.md). Earlier expansion tests remain enabled rather than being replaced with the new subset.

## Practical limits

This is a full project-source review plus automated functional and packaging checks, not a proof of zero bugs or a review of every upstream Minecraft/JVM dependency. Real-world mob populations, unloaded chunks, other mods, every village layout, and long-running multiplayer balance still need owner playtesting. The important population change is native despawn and cap participation, not an arbitrary population purge.

## Requested complete repeat

After the fixes were handed over, the owner requested the same checks again. Three further 173-test server runs, the plain/JEI/REI clients, source hashes, file and release audits, adversarial archive checks, and whitespace validation passed. The first repeat forced all build tasks to execute. Audio decoding and the sprite helper also passed. The gameplay source and artifact checksum remained unchanged; no additional runtime failures were observed.
