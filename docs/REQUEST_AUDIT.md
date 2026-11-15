# Final 0.1.1-beta readiness rerun - November 14, 2026

The final complete native server suite passed **423/423 tests**, including five new regressions for Tetanus-enchanted bows/crossbows breaking on their firing shot and for every projectile in a Multishot volley. The fix preserves the actual firing weapon's retained enchantment data without a global shot cache; plain and saved-projectile controls are included.

Current artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,557,756 bytes**, SHA-256 **`E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`**. Replace the older JAR on both client and server. The [final evidence gallery](../validation/release-ready-2026-11-14/index.html) and [readiness scope and limits](RELEASE_READINESS_0.1.1-beta.md) distinguish the current candidate from earlier artifacts.

All **16 final visible-client reports passed**: the plain baseline, 31-recipe crafting session, causal advancement journey, forging, layout, actual mixed/special Dragon rituals, six interaction modules, and medical/forge flows in both JEI and REI. The linked gallery retains their original screenshots and disclosed fixtures. The packaged-server probe stopped at the EULA=false guard and did not create a production world. Remote multiplayer/modpack endurance and a full Survival balance playthrough remain outside this pass.

The sections below are historical snapshots for their own artifacts and run stages. Their older hashes, counts, and then-pending checks are retained as history, not the current verdict.

---
# Historical boss/medical patch snapshot (before the final readiness rerun)

The boss/medical patch passed **418/418 native server tests** after the reported fixes and additional source review. **Eight fresh graphical reports and 81 original screenshots** cover the redesigned beacon, medical trades, actual Wither/Dragon encounters and JEI/REI workstation flows. The [patch gallery](../validation/boss-fixes-2026-11-14/index.html) preserves the evidence; the [patch review](PATCH_REVIEW_0.1.1-beta.md) maps all twelve requests to behavior and verification.

Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,557,371 bytes**, SHA-256 **`D287996D5E8E73CE7C4C75CDEB186677EF4B3B328A5B779073505FF744A9348D`**. The [release receipt](../validation/boss-fixes-2026-11-14/final-release-verification.json) identifies this rebuilt gameplay patch. All **409 generated resources** were regenerated with identical bytes. Controlled graphical fixtures are disclosed; this is not a Survival balance playthrough.

The evidence below describes earlier artifacts and remains historical, including the earlier metadata-only rebuild with this same version number.

---

# Current expansion scope — November 14, 2026

**Historical version-only verification, superseded by the patch above.** The [metadata-only rebuild is verified](../validation/version-0.1.1-beta/verification.json): only the version in `fabric.mod.json` changed; all **267 compiled classes** and every other resource entry are byte-identical to the tested **0.1.0-beta baseline**. Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,501,095 bytes**, SHA-256 **`957F54966E70FFA9B3B27136D337EBF3D9A1E6551DF112D13DA39A7BC808BE2A`**. The original 399-test and graphical receipts retain their original metadata and hashes; no additional gameplay rerun was performed for this version change.

The [November expansion notes](RELEASE_NOTES_2026-11-14.md) and [recipe guide](RECIPES.md) describe the latest requested behavior. The [graphical expansion gallery](../validation/expansion-2026-11-14/index.html) now records 12 fresh PASS reports and 318 original images: all 30 visible advancement nodes at four GUI scales, all 31 ordinary recipes, both custom stations, JEI/REI, actual End rituals, and the earned dragon rewards. The corrected full native suite passed **399/399 tests**; the gameplay baseline is ready for owner playtesting. Older rows below retain their historical scope.

| Current requirement | Implemented behavior to verify |
| --- | --- |
| Wither ritual and difficulty | 75% default special-soul-sand ritual; 100% with upper-center netherite; 650 health; limited 10-health minions. |
| Wither loot and XP | Native 50 XP; normal and Tetanized stars; exactly one book, 50% II instead of I. |
| Crystal-forging progression | Expensive screenshot-layout table; End Crystal + Rust + netherite ingot; four special respawn crystals produce a 1,100-health special dragon. |
| Dragon rewards and credit | Repeatable shared egg/sword drops; ordinary dragon XP sequence; summon credit within 256 horizontal blocks; defeat credit at 20 actual damage. |
| Overload Sword | 12 attack damage; 2.3 attack speed; 4,062 durability; default Tetanus II; compatible sword enchantments; netherite-block anvil repairs or Mending. |
| Level-IV beacon | Full pyramid; consumes one Tetanized Wither Star plus a netherite ingot for every confirmed IV selection. |
| Prescription trading | Writer consumes paper + ink sac; treatment offers consume emeralds + prescription; existing offer migration retains cooldown. |
| Clear supplies and Rabies sources | Exact requested amounts and main-hand delivery instructions; separate rare natural Rabid variants; Rabid Meat only from animals. |
| Early antitoxin advancement | Curing Tetanus before Lockjaw, or curing standalone Lockjaw, qualifies for A Dose of Common Sense. |
| Appearance and spawning | Matching mod advancement icons, Enderman overlay, normal spawn-environment validation retained. |

The current active definition set is 31 advancements, with 30 visible and one existing hidden recipe helper. Retired `variant_egg` remains hidden for save compatibility. The existing 28-action journey and all three new forge/dragon actions were exercised on the same saved graphical actor without grants or revocations. Their separate reports preserve the causal evidence. The third native suite passed 398/399; its sole remaining join-immunity fixture was corrected, and the full final rerun passed **399/399**. The test retained its protected negative control before clearing its own timer; production spawn immunity was unchanged. See [current findings](REVIEW_FINDINGS_2026-11-14.md) for real fixes versus obsolete assumptions.
# Requested feature audit — 0.1.0-beta expansion

The latest instructions take precedence over earlier versions of a feature. In particular, temporary animal infections no longer change drops, the beacon uses only Rusted Iron Blocks, and rusty iron tools are preserved for old saves but retired from new acquisition.

## Gameplay

| Request | Implementation and verification |
| --- | --- |
| Custom Tetanus death messages | Custom damage types/translations for Tetanus, rusty nails, traps, wire, and disorientation; self-nail has a unique message. Default disease damage remains nonlethal where documented. |
| Upward rusty-nail death advancement | `why_did_i_do_that`, “Why did I do that?”; records upward toss in projectile NBT and awards on actual player death to that owned projectile. Regression checks positive and negative cases. Server PvP rules still apply to player-owned projectiles. |
| Correct beacon III labels and themed screen | Dedicated rust/copper menu; selected upgraded primary displays III. Visually checked in graphical clients. |
| Separate beacon advancements | `beacon_placed` and `beacon_full`; real beacon ticking test confirms custom completion and absence of both vanilla beacon advancements. |
| Automatic beacon resistance | Active custom beam grants Tetanus Resistance even without a selected primary; tests cover passive-only and full Haste III behavior. |
| New beacon pyramid | Only `rusted_iron_block`; iron block plus four Rust in a cross. Old materials fail validation; payments remain netherite. |
| Variant-only infected meat | Permanent tetanized animals replace normal loot with Infected Meat; temporary Tetanus leaves normal loot unchanged. Tests cover animal families and ordinary versus permanent variants. |
| Five-second manure nausea | Actual Nausea status for 100 ticks, verified by drinking and ticking through expiration. |
| `/give` discovery | Canonical `tetanusmod` IDs retained; matching item paths are additionally suggested without a typed namespace. Real dispatcher suggestion/command tests. |
| Tetanus II | Current Wither drop is exactly one book: 50% II, otherwise I. Anvil tests cover swords, axes, spears, rebar, pipes, bows, crossbows, and tridents; nails, hooklines, and mining tools are rejected. II causes disorientation, random wandering/missed mob attacks, and 2 HP nonlethal damage every ten seconds. |
| Enchanted ranged weapons | Bow, crossbow, and trident snapshot enchantment level when fired and save it in projectile NBT. Tests fire each weapon and verify infection after switching weapons. |
| Dedicated glove slot | Native survival and Creative inventory slots, server synchronization, right-click/shift-click equip, persistence, old offhand migration, and death/respawn rules. Creative packets validate mode, item, and count. No added dependency. |
| Batch arrows in smithing table | Empty template, ordinary arrow base, Rust addition; one Rust for up to 64. Tests cover 1/16/64, shift-click, partial/full inventories, invalid inputs, and advancement timing. JEI/REI custom displays are opened in real clients. |
| Standalone Lockjaw potion | Tetanus + pufferfish; sixty seconds before vanilla splash/cloud scaling. Milk cannot cure it; antitoxin can. Ordinary Tetanus still includes its final random 2–4 minute Lockjaw phase. |
| Villager items and medic | Medic novice always offers both vaccines, Rabies Cure, antitoxin, and gloves. Smiths/armorer/fisherman gain selected equipment offers. Medicine Cabinet is a registered acquirable job site. |
| Clinic generation and medic skin | Compressed 9x11x13 timber-and-calcite clinic with a gabled spruce roof, covered porch, chimney, hanging medical banner, two beds, supplies, workstation, and medic; added to all five village house pools. Tests load/place the template and inspect its medic and cabinet. Custom profession clothing renders through vanilla villager features. Clinics are weighted, not guaranteed per village. |
| Natural/dungeon variants | Accepted Easy/Normal/Hard natural rates 10/15/20%; spawner rates 25/35/45%; Peaceful 0%. All spawners use the difficulty-based rule, including dungeon spawners. Boss rituals excluded. Initialization tests verify the hook and exempt command/egg/breeding spawns. |
| Persistent Rabies | Infinite effect, saved progression, one heart of maximum health lost per minute down to two hearts, then periodic ten-second nausea. Rabies Cure or death removes it. Milk/antitoxin do not. Save, cure, vaccine, duration, and respawn tests. |
| Rabid variants | Eligible mammals and undead gain +30 HP, +7 armor, aggressive 5 HP melee, species-aligned sickly skins, and persistence. Rare additional natural rolls are 1/2/3% on Easy/Normal/Hard. Native mob conversions retain the variant. |
| Rabid meat and potions | Rabid animals alone drop Rabid Meat; rabid hostiles keep ordinary loot. Manure + Rabid Meat brews Rabies; native splash/lingering conversions. Cooked meat is safe. Rabies vaccine is craftable and traded. |
| Purple-red Rabies hearts | HUD, tab-list hearts, and numeric health coloring; client flags synchronize separately from Tetanus and clear on disconnect. Real client checks and screenshots. |
| Spear replacing new rusty tools | Seven base damage, slower attack, 4.5-block right-click thrust blocked by walls; costlier iron/chain/stick recipe. Rusted spear infects. Existing rusty iron tool IDs, NBT, use, and cleaning preserved; absent from new rusting/creative lists. |
| More traps and reset tool | Barbed Wire/Rusted Barbed Wire; Bear Trap/Rusted Bear Trap; one snap, Slowness X, reset/rearm, rusting, and captured-victim persistence. |
| Placeable Rust | Existing item ID becomes a floor/wall BlockItem. Placement, consumption, and support shapes checked. |
| Shield protection enchantment | Shields only, described in book/item tooltip; held shield prevents 65% of I exposure or 40% of II exposure, combining with applicable vaccine/glove protection. Boundary and actual blocking tests. |
| Items in creative tab | All current items, diseases, books, and variant eggs included. Legacy rusty iron tools excluded per owner's answer. Client checks every registered mod item against the tab. |
| Spawn eggs | Tetanized vanilla creature variants, including Wither and dragon, plus eligible rabid variants; native tinted egg models. Specific variant is created before spawning. |
| Recipe viewers | Crafting/cooking/brewing plus nine current Rusting conversions and dedicated batch arrow smithing. JEI and REI are optional alternatives; Fabric API remains the only required mod dependency. |

## Advancements

All additions appear under the existing rusty-background Rust & Ruin tab; vanilla beacon advancements remain separate.

| Name | Trigger |
| --- | --- |
| Why did I do that? | Die to your own upward-thrown rusty nail |
| Not Today | Block a Tetanus-enchanted attack with a protected shield, or have its infection prevented by shield protection |
| Hands Off | Gloves actually prevent a contact infection; simply equipping them does not count |
| Foaming at the Mouth | Contract Rabies, including potion exposure |
| A Dose of Common Sense | Use antitoxin while Tetanus and/or Lockjaw is present |
| Back to Yourself | Use Rabies Cure while infected |
| A Beacon of Bad Ideas | Place a Tetanized Beacon |
| Rust Never Sleeps | Receive a selected power from a full-strength active custom beacon |
| A Pointed Upgrade | Successfully collect a smithing arrow batch |
| Keep Your Distance | Land a spear thrust |
| Mind Your Fingers | Reset a closed bear trap |
| Bad Egg | Spawn a variant with its egg |
| Doctor's Orders | Trade with a medic |
| The Doctor Is In | Place a Medicine Cabinet |

Earlier milestones remain: A Painful Lesson, Rusty Business, Hook, Line and Tetanus, Lockjaw Sonata, Strange Meat for collecting Infected Meat, Why did I eat that? for Salmonella, Tetanized Mob, Rusty Guardian, and Tetanus. Nice One!

## Delivery

Version remains **0.1.0-beta**. The updated summary is in [MODRINTH_SUMMARY.txt](../MODRINTH_SUMMARY.txt); the full listing is in [MODRINTH_DESCRIPTION.md](../MODRINTH_DESCRIPTION.md). Expanded crafting and trade details are in [RECIPES.md](RECIPES.md). Check [VALIDATION.md](../VALIDATION.md) for actual results and limits. This expansion is not committed or pushed yet.
