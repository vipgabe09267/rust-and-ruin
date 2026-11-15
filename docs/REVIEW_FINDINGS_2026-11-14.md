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

# Expansion review findings — November 14, 2026

**Historical version-only verification, superseded by the patch above.** The [metadata-only rebuild is verified](../validation/version-0.1.1-beta/verification.json): only the version in `fabric.mod.json` changed; all **267 compiled classes** and every other resource entry are byte-identical to the tested **0.1.0-beta baseline**. Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,501,095 bytes**, SHA-256 **`957F54966E70FFA9B3B27136D337EBF3D9A1E6551DF112D13DA39A7BC808BE2A`**. The original 399-test and graphical receipts retain their original metadata and hashes; no additional gameplay rerun was performed for this version change.

The graphical expansion checks are complete: **12 fresh result reports and 318 original Minecraft screenshots** are preserved in the [gallery](../validation/expansion-2026-11-14/index.html) and [manifest](../validation/expansion-2026-11-14/manifest.json). **The final native suite passed all 399 cases with zero failures**, completing in 9 minutes 24 seconds. The three earlier failed runs and the last test-fixture correction remain recorded below. The tested gameplay baseline is ready for owner playtesting.

## Source-review scope

An independent reviewer read every line of **24 dragon, forging-menu, recipe-viewer, and dragon-related mixin files: 696 physical lines** in that source snapshot. The [per-file manifest](../validation/expansion-2026-11-14/reviews/dragon-independent-review-2026-11-14.json) records paths, ranges, and SHA-256 hashes. A [separate health/controller peer review](../validation/expansion-2026-11-14/reviews/dragon-health-controller-peer-review.json) records its own bounded scope.

This is not a claim that all 276 Java files in the automated inventory were manually reviewed line by line during this pass. Medical transactions/cures, Wither behavior, beacon changes, spawn rules, and health handling also received separate implementation and regression work. Native bytecode comparisons checked hover/save/load, crystal placement, complete respawn ordering, death-counter/XP ordering, egg teleportation, and death rendering; those local Minecraft references were not published.

## Confirmed product defects and corrections

| Finding | Correction and evidence |
| --- | --- |
| Saving a dragon while carrying a player retained native `HOVER` but lost its transient grab owner, potentially leaving the reloaded boss hovering indefinitely. | Persist an interrupted-grab marker. The first server tick dismounts restored players and returns only an interrupted `HOVER` to `HOLDING_PATTERN`, preserving its cooldown. The native NBT regression also verifies that unrelated hover states remain unchanged. |
| The custom dragon death decal used an ordinary cutout layer and could draw outside the native dissolve mask. | Preserve `getEntityDecal` for the custom death texture. The actual End fight now includes a native death-animation capture and complete 200-tick death sequence. |
| The requested 1,100-health dragon hit Minecraft's native 1,024 maximum-health attribute ceiling. | Correct only the exact mutation modifier's computed health, retaining native modifier order/debuffs and a 1,100 upper bound without widening the global attribute. Five native cases cover cached/current health, partial-health NBT reload, legacy migration, ordinary/unrelated modifiers, and bounded modifier ordering. The genuine special ritual also produced 1,100 raw/current health. |
| Early antitoxin treatment could cure Tetanus without awarding **A Dose of Common Sense**. | Make the milestone reachable from infection, recognize real antitoxin consumption during Tetanus or standalone Lockjaw, and award only after a successful cure. Timed cure, statistics, and native advancement completion passed in plain, JEI, and REI clients. |
| Special-Wither skull damage did not consistently perform the intended Tetanus exposure. | Successful special-Wither/minion damage uses normal exposure and prevention. Vaccination lowers risk rather than bypassing every exposure. Native positive and protected cases distinguish successful damage from spawn immunity. |
| The special Wither lacked the required native XP behavior and could drop both book levels. | Award native 50 XP and exactly one book: 50% Tetanus II, otherwise Tetanus I. Ordinary and special stars remain separate rewards. Dedicated native XP and exclusive-book cases exercise the behavior. |
| Replacing surrounding cave water could reevaluate authored wheat before native lighting settled, destroying the eight crop states. | Add native `FORCE_STATE` flag 16 only to the medic tent's authored placement. The flooded-camp regression checks crop retention without changing structure placement globally. |
| Native skeleton initialization could randomly enable loot pickup on temporary minions, whose empty equipment drops could then erase stolen player gear. | Disable pickup in construction, after native initialization, and after NBT loading. A regression checks initialization, item pickup, and reload; temporary summons do not collect player equipment. |
| Prescription recipe dimensions could be accepted incorrectly through invalid dimensions or integer multiplication overflow. | Require positive dimensions and multiply using `long`; valid grids still fit while negative and overflow-shaped invalid requests are rejected. This does not change the writing recipe or payments. |

The requested balance values are **650 health for the Tetanized Wither**, **1,100 for the Tetanized Ender Dragon**, and a **75%** special-soul-sand ritual chance, raised to **100%** with the upper-center netherite upgrade. These are requested behavior changes; they are not all preexisting security defects.

## Menu and test-controller corrections

- The medical and forging controllers could clear a completed restoration future, wait for more frames, then wait forever for the cleared future. Their final phases now handle the future separately from the frame wait. This was a test-controller bug.
- JEI and REI forging displays now show the missing second plus sign before the netherite ingredient.
- The existing advancement journey now requires a fresh actor instead of revoking progress or resetting its recipe book. The same saved actor completed the **28 existing actions plus all three new dragon/forge milestones**, without direct criterion grants or revocations.
- Ordinary crafting explicitly defers the two custom workstation recipes to real menu tests. Unknown recipe types still fail; the custom paths are independently exercised rather than silently skipped.

## Recipe-book behavior and obsolete assertions

`prescription_writing` and `crystal_forging` are intentionally **ignored by the native recipe book**. They are performed through their workstations and shown by dedicated JEI/REI categories. The generator includes the prescription unlock reward, but an ignored custom recipe does not become a normal crafting-book entry. The older test demanding native-book visibility for every runtime recipe was too broad; its failure did not show that prescriptions were uncraftable.

Other obsolete fixtures expected only one custom recipe type, ordinary rusty-contact conversion of bosses, and only the original 28 advancement definitions. Updated fixtures distinguish ordinary mutation from boss rituals and the current 31 active advancements. Those corrections are separate from product fixes.

## Native suite history and completed rerun

| Run | Total cases | Passed | Failed |
| --- | --- | --- | --- |
| First expansion run | 378 | 365 | 13 |
| Second expansion run | 391 | 383 | 8 |
| Third expansion run | 399 | 398 | 1 |
| Corrected full rerun | 399 | 399 | 0 |

The saved receipts are `.tools/expansion-2026-11-14/server-first.xml`, `server-second.xml`, and `server-third.xml`. The first three runs were not all-green; the corrected full rerun passed, as recorded in the [final native JUnit receipt](../validation/expansion-2026-11-14/reports/native-final.xml) and [complete log](../validation/expansion-2026-11-14/reports/server-final-repeat.log). The initial failures mixed real defects with outdated fixtures; they were not 13 independently confirmed product bugs.

The third run's remaining failure was `nativeDragonDamageInfectsOnlyForSpecialBossAndCannotBiteItsRider`: the newly added test attacked a freshly connected player during Minecraft's **60-tick join invulnerability**, so the supposed positive hit was rejected. The fixture now first verifies that protected rejection, clears that native test-only timer through its accessor, then performs the real positive damage/infection case. The complete corrected 399-case rerun passed with zero failures. Production spawn immunity was not disabled to make this test pass.

## Completed graphical evidence

The [12 reports](../validation/expansion-2026-11-14/manifest.json) and [318-frame gallery](../validation/expansion-2026-11-14/index.html) cover:

- **Plain, JEI, and REI workstations:** native writing, prescription purchases, exact main-hand supply delivery, timed early cure and advancement, full-inventory forging refusal, two genuine crystal outputs, and safe preview/leftover returns. JEI and REI both opened the new prescription and forging categories.
- **All advancement nodes:** all 30 visible mod nodes at GUI scales 2, 3, 4, and 6, with causal progression for all 31 active milestones on the same saved actor. The hidden recipe helper is included in the total; retired `variant_egg` is excluded. Existing criterion dates were preserved. Vanilla tabs unlocked through actual gameplay actions.
- **Every recipe route:** 31 ordinary crafting/cooking/smithing recipes and the two dedicated workstation recipes, using actual native result acquisition. Ingredient fixtures and accelerated native cooking ticks are disclosed.
- **Actual End rituals:** four native placement packets and full respawn animation for both a mixed set yielding a 200-health ordinary dragon and four special crystals yielding a 1,100-health Tetanized Dragon. No respawn-state timers were written to skip the ritual.
- **Automatic grab/drop:** native ticks observed an approximately **7.46-block lift**, **1.07-block fall**, and **2,400-tick cooldown**, with no direct calls to the grab helper. Controlled approaches seeded ordinary dragon RNG; this does not measure live-world encounter frequency.
- **Combat contribution and rewards:** seven real head-attack packets recorded **20 actual damage**, then a native finishing attack completed the **200-tick death**, **500 repeat-fight XP**, **one sword**, and **one egg**. The earned egg was actually placed; sword stats, durability, and Tetanus II were checked.
- **Beacon and appearance:** native level-IV menu payments, a new star required per confirmation, unpaid rejection, downgrade after vanilla effect expiry, unused currency returns, and the Enderman/Wither/minion/dragon appearances.

The combat fixture is invulnerable and controlled. It used a vanilla netherite sword with Sharpness V, then reduced the dragon to one HP before a native finishing hit. The separate appearance specimens had AI paused. Earlier composite sessions had disclosed portal/layout fixture failures; their failed overall runs are not relabeled PASS. Later successful continuation sessions retained the same world's earned progression. This is functional coverage, not a Survival balance playthrough or universal modpack guarantee.

## Tested 0.1.0-beta baseline artifact and checks

Packaging and resource-refresh checks passed for `rust-and-ruin-0.1.0-beta.jar`: **3,501,095 bytes**, SHA-256 **`7F1D6F6AB1C4001A7C09EFF7D3CD839345BB4F4CE2B9D56D494D4BD714B8AA25`**. An automated file-format/integrity checkpoint passed for **1,844 files** before the final review copies and receipts; that automated count is not a manual semantic review count.

The corrected native suite, graphical flows, packaging, and resource refresh passed for the same JAR hash. The tested gameplay baseline is ready for owner playtesting. Earlier October receipts remain historical evidence for their own artifacts. No additional confirmed arbitrary-output, inventory-duplication, ritual-ordering, repeat-reward, or damage-credit vulnerability was found in the independent 24-file scope; long-running remote-server and arbitrary modpack behavior remain outside this pass.

See [release notes](RELEASE_NOTES_2026-11-14.md), [recipes](RECIPES.md), and [validation](../VALIDATION.md).
