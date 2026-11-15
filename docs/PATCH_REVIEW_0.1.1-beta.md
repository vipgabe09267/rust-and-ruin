# Rust & Ruin 0.1.1-beta - boss and medical patch

This patch addresses the twelve reported issues. The version remains **0.1.1-beta**; replace the earlier JAR with the newly verified artifact after testing.

| Request | Result | Verification |
| --- | --- | --- |
| Special Wither star | A Tetanized Wither replaces its ordinary star with one Tetanized Wither Star. Ordinary Withers retain their normal star. Wire-brushing still cleans the special star. | Native death/loot regression and actual Creative/Survival client kills. |
| Wither minions | Minions have 20 HP. Creative observers can see natural waves without being targeted. Placement finds safe ground near the player, including beneath cave roofs. First wave is 10-15 seconds after spawn protection; later waves remain 30-60 seconds apart, at most two per wave and six alive. | Native timed waves, blocked-floor recovery, saved-health migration and both visible fights. |
| Writer font | The writer uses crisp, unshadowed native Minecraft foreground text with clear Paper, Ink and Result labels. | Actual crafting screens in plain Minecraft, JEI and REI. |
| Writers in medical structures/trades | Each newly generated clinic and medic camp includes a supported writer facing its aisle. Nurse/Practitioner supply pools can sell one writer per delivery. | All five village themes and the camp, each at four rotations, with actual Survival raycast/click access; genuine generation tests. |
| Medic balance | Rank-appropriate treatment/supply pools retain two randomized offers per rank. Treatments stay scarce; emergency prescriptions are capped at two in villages and one in camps. Native Hero/gossip discounts apply. | Rank/stock/discount tests and real trades. Migration preserves remaining stock by resource identity and keeps the saved delivery time. |
| Pillar crystals | Four special ritual crystals regenerate all ten pillar crystals as infected variants. Mixed rituals retain ordinary pillars. Only accepted native explosion damage can spread Tetanus. Beams, protection, healing and native reset behavior remain intact. | Both actual End rituals, native crystal save/reload and real explosion damage. Completed/aborted rituals cannot continue converting crystals. |
| Dragon grab chance | A close pass rolls 15%, with the existing 120-second cooldown after a successful grab. | Native chance boundaries and actual automatic client carry/drop. |
| Dragon boss bar | A distinct rust-colored bar follows the native 1,100-HP fight. An ordinary successor restores its own title and style. Unrelated dragon UUIDs cannot alter the canonical bar. | Native bar/health tests and actual in-game capture. |
| Dragon lift | The target is ten blocks of actual mouth rise. A steep flight-to-hover transition cannot shorten the requested lift. Obstructions/ceiling limits still release safely. | Native transition test; actual player position measured every server tick reached 9.99 blocks before release. |
| Dragon loot and sound | One shared pre-enchanted sword and one special egg land on solid ground outside the exit portal, glow and do not despawn. Spawn-egg/operator dragons drop rewards by their actual location. The native instant-kill path also awards trophies once. A local slowed/reverberating victory voice plays once. | Native twelve-second portal-loss regression, instant-kill test, actual collection, 500 native respawn XP, and one resolved positional client sound. |
| Egg appearance | The trophy retains the vanilla black/purple shell and shape, with small modeled rust chips. | The earned egg was placed and its original Minecraft screenshot inspected. |
| Beacon UI | A compact steel panel separates effect choice, II/III/IV strength, preview and payment. Unpaid IV activation is blocked; actual IV selection consumes one ingot and one extra star. | Twenty native menu captures at uncapped GUI scales 2, 3, 4 and 6; slot/control geometry, real Haste IV and restoration verified. |

Existing generated buildings are not overwritten. Their medics can offer the writer, or players can craft one. New trade resources wait for the inherited delivery rather than granting fresh stock when a saved medic is upgraded.

## Checks and evidence

- Final complete native server suite: **423/423 passed**, including five final-shot firing regressions. The earlier 418-case patch run remains archived.
- Actual visible Minecraft checks: beacon, prescriptions/trading, Creative/Survival Withers, mixed/special End rituals, actual damage contribution, trophy collection/placement and victory audio.
- All **16 final visible-client reports passed**, including six interaction modules and medical/forge flows with both JEI and REI. The [current evidence gallery](../validation/release-ready-2026-11-14/index.html) preserves original screenshots and fixtures.
- Full resource regeneration: **409 files byte-identical**.
- [Current readiness gallery and retained failed attempts](../validation/release-ready-2026-11-14/index.html); the [earlier patch gallery](../validation/boss-fixes-2026-11-14/index.html) remains historical.
- The current artifact is **3,557,756 bytes**, SHA-256 `E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`. [Readiness scope and limits](RELEASE_READINESS_0.1.1-beta.md) identify its receipts; the [earlier patch release verification](../validation/boss-fixes-2026-11-14/final-release-verification.json) remains archived.

Graphical fixtures supply materials, use controlled camera flight/invulnerability, seed close-pass approaches and reduce the boss to one HP for the final hit after verifying its full health. These checks verify functionality; they are not a complete Survival balance playthrough.

## Additional defects found during review

- A durability-breaking bow/crossbow shot lost its Tetanus snapshot, including later Multishot projectiles. Read only the actual weapon object's retained enchantment data and clamp the matched level; five real native firing regressions cover broken/plain weapons, every arrow/firework, saved projectiles and ordinary trident/unbroken controls.

- Sold-out Antitoxin could inherit unused Vaccine stock when a randomized catalogue changed positions. Migration now matches actual resources, with explicit swapped-stock and unrelated-purchase regressions.
- A stale ritual crystal list survived completion/abort and kept the pillar converter running. Conversion now requires an active respawn state.
- Native dragon fight updates rejected mismatched UUIDs but the added bar hook did not. It now checks identity and also clears a special title when returning to an ordinary fight.
- A body-height grab cap did not account for the native head pose changing during a steep approach. Lift completion follows the actual mouth height instead.
- The dragon spawn-egg acquisition test used a fixture too small for its complete body. It now uses a properly isolated arena without weakening production placement safety.
- The graphical grab sampler originally used a pre-approach starting height and four-tick samples. Its corrected observer reads the successful-grab height and actual passenger position every native server tick; the lift assertion remains strict.
- GUI-scale testing exposed a window-focus helper that resized the framebuffer, then a minimized-window startup wait. The test helper preserves size and records measured window state with bounded recovery. Production GUI behavior was unchanged by those harness corrections.
