# Rust & Ruin 0.1.1-beta - boss and medic fixes

For **Minecraft 1.20.1 Fabric**. This patch changes gameplay and menus; it is more than the earlier version-label correction. Replace the JAR on **both client and server**. Current build and test receipts belong in [VALIDATION.md](../VALIDATION.md); the earlier baseline results do not establish a pass for this patch.

## What's changed

1. **Wither stars:** the Tetanized Wither drops one Tetanized Wither Star, without an extra ordinary star. Wire-brush it when you need a normal Nether Star. Its 650 health, native 50 XP, and single Tetanus book remain: 50% II, otherwise I.
2. **Earlier, stronger minions:** the first wave is scheduled 10-15 seconds after the spawn shield ends while a player is nearby. Later waves follow random 30-60-second intervals. Each wave has up to two 20-health sword-carrying minions, with six living minions per Wither. Creative observers can see the waves too.
3. **Prescription Writer access:** one paper and one ink sac still make one prescription. The menu has clear input/output labels. Writers appear in newly generated clinics and cave camps, and lower-rank medics can sell them.
4. **Medic progression and scarcity:** each rank adds a randomized treatment and supply/barter offer, without stacking repeated medicine, writer, or prescription stock. Nurse starts with antitoxin or Tetanus Vaccine; Rabies Cure enters at Nurse Practitioner, and stronger treatments and rare supplies enter later. Emergency prescriptions cost 3 emeralds, with two village purchases or one camp purchase per delivery. Medicine limits, slower field restocking, and native Hero/gossip discounts remain.
5. **Special pillar crystals:** a successful all-tetanized End ritual converts all ten pillar-top healing crystals. Mixed rituals retain ordinary crystals and an ordinary dragon.
6. **Crystal infection:** a special crystal explosion can inflict Tetanus only after its blast actually damages the victim. Native cover, distance, damage falloff, and Creative immunity still apply.
7. **Dragon mouth attack:** close passes now have a 15% grab chance. The special dragon lifts its caught Survival player ten blocks, then drops them. A successful grab starts a 120-second cooldown.
8. **Dragon boss bar:** a rust-and-purple health bar follows the 1,100-health special dragon. An ordinary fight keeps its ordinary bar.
9. **Dragon egg:** the trophy keeps the vanilla black egg shape and texture layout, with small rust chips.
10. **Recoverable rewards:** the shared Overload Sword and Tetanized Dragon Egg land on solid ground outside the exit fountain. They glow and do not despawn, avoiding the ordinary portal column.
11. **Victory audio:** the dragon defeat cue plays slower with short reverberation, audible locally within 32 blocks.
12. **Beacon menu:** the steel panel separates primary effects from explicit II / III / IV level controls and shows the payment. Every confirmed IV selection still costs one netherite ingot plus one Tetanized Wither Star.
13. **The final shot still infects:** Tetanus-enchanted bows and crossbows retain their infection on the shot that breaks them, including all arrows or fireworks in a Multishot volley.

The special Wither ritual remains 75% with four Tetanized Soul Sand blocks, or 100% when the upper center is replaced with netherite. The special Dragon still requires four Tetanized End Crystals forged at the Tetanus Wither Forging Table. Its Overload Sword remains 12 attack damage, 2.3 attack speed, 4,062 durability, and Tetanus II; anvil repairs require Netherite Blocks, or use Mending.

## Updating an existing world

Install the updated **0.1.1-beta** JAR on both sides. Fabric API remains the only required mod dependency; JEI and REI are optional alternatives.

Saved medic offers migrate by matching the traded resource, retaining its remaining stock and delivery cooldown. Newly introduced offers start sold out until the next scheduled delivery; migration does not refill a sold-out medicine from another offer. Existing advancement progress and item IDs remain. Newly generated clinics and cave camps include writers; existing structures are not rebuilt. Customized server settings can differ from the documented defaults.

See [recipes and progression](RECIPES.md) and the [current Modrinth about text](MODRINTH_ABOUT_2026-11-14.md).

## Final readiness rerun

The final full native server suite passed **423/423 tests**, including five additional real firing regressions. Current JAR: **3,557,756 bytes**, SHA-256 **`E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`**. It replaces the earlier 0.1.1-beta artifact. All **16 final visible-client reports passed**, covering fresh crafting/causal advancements, baseline appearance, forge, layout, actual End rituals, six interaction modules, and medical/forge flows in both JEI and REI. The [current evidence gallery](../validation/release-ready-2026-11-14/index.html) and [readiness report](RELEASE_READINESS_0.1.1-beta.md) preserve their results and fixtures. The isolated packaged-server probe stopped at the EULA guard; it did not validate a production world or remote multiplayer.

## Earlier verification and version correction

The earlier November 14 gameplay baseline was recorded as 399 passing native tests, 12 graphical reports, and 318 original screenshots. Those raw records, fixture disclosures, and review notes remain in the [earlier graphical gallery](../validation/expansion-2026-11-14/index.html) and [review findings](REVIEW_FINDINGS_2026-11-14.md).

The subsequent [metadata-only 0.1.1-beta rebuild](../validation/version-0.1.1-beta/verification.json) changed only the version in fabric.mod.json: its 267 compiled classes and other resources matched that earlier 0.1.0-beta artifact byte for byte. No extra gameplay run was needed for that label-only correction. **That historical comparison does not describe the gameplay changes in the present patch.**
