# October 24 playtest fixes

Version remains **0.1.0-beta**. These changes follow the owner's screenshots and in-game reports. Final verification passed in four visible Minecraft clients and two final 303-test server runs. These playtest changes remain uncommitted for owner testing.

## Gameplay and interface changes

| Report | Cause and change |
| --- | --- |
| Empty advancement window | The root required iron before the tab could appear. It now unlocks on the native player tick, with its rusty background. |
| Iron achievement did not unlock | Added a separate **Iron Has a Darker Side** inventory advancement, instead of conflating iron acquisition with the tab root. |
| Rusting could leave the tab hidden | Rusting uses the shared root-revealing award path. |
| Golem conversion gave only the generic award | Successful causal conversions now award **Tetanized Guardian** as well as the generic conversion award, including projectile conversions. Already-mutated or Rabid golems do not pretend to convert. |
| Cabinet looked wooden and broken | Rebuilt its model and textures as a stainless-steel case, with clear framed glass doors, labeled bottles, caps, shelves, handles and a medical badge. Both hinges and the whole-cabinet inventory model are included. It opens by hand. |
| Gloves looked like a pot | Replaced the long plain cuff with a fitted hand, finger seams, short stitched cuff and thumb. A real opaque-sleeve A/B test also exposed skin-layer occlusion; the fitted glove now clears the vanilla sleeve. Standard/slim and both hands were inspected. See the [32 native comparison screenshots](../validation/playtest-2026-10-24/gloves/index.html). |
| Clinic sat on a raised base | Corrected its two-layer floor alignment, restricted sites to dry nearly level plots, preserved exterior ground instead of carving a trench, and removed unnecessary terrain deformation. All five biome templates have a flat entrance. |
| Every novice sold everything | Removed the unconditional five-medicine append. Each rank adds one randomly selected treatment and one supply/barter offer. |
| Medicines were too abundant | Each distinct medicine has **2-4 purchases** per delivery. Offers avoid duplicate medicines across ranks; stock, uses and delivery times persist. |
| Trades lacked progression | Four ranks: **Nurse**, **Nurse Practitioner**, **Medical Doctor**, **Attending Physician**. Barters use combinations of emeralds, iron, flint, honey, paper, books and other ingredients. Higher ranks can offer potion supplies, protection books, golden apples and a scarce enchanted golden apple. |
| Hero discounts | Native Hero of the Village, gossip and demand calculations remain in charge of prices. |
| Field medic supplies | New cave-tent medics carry at most **two purchases per offer**, with a delivery interval of **72,000 world ticks**; village medics use **24,000**. Sleeping does not bypass these elapsed-world-time intervals. |
| Trade scrolling | Vanilla displays seven offers; the old five-offer novice list did not need scrolling. Attending medics have eight offers. Actual wheel and scrollbar-drag behavior are tested in the client. Long rank headings fit the native pane. |
| Finding a cave tent | `/locate poi tetanusmod:medic_tent` finds a nearby generated field-cabinet POI. `/locate structure tetanusmod:medic_tent` and `/rustandruin locate tent` search the native structure grid. These remain operator commands. |
| Cave-tent availability | Eligible candidates now try nearby cave positions while retaining the 15% grid roll and more than 2,000 blocks between camps. |
| Bear trap did not trigger or hold | Removed solid collision that kept feet away from the trigger. Captures now restrain walking and jumping, synchronize to clients, and release if the trap breaks or an external teleport moves the victim away. |
| Reset tool appeared ineffective | Reset gives sound/action-bar feedback and three seconds to move away before rearming. It preserves unrelated long or infinite Slowness effects. |
| Advancement names | Shield protection is **Not Today, maybe tomorrow**. Drinking either vaccine awards **Faucci Ouchie**. |

Legacy level-five medics retain their level and display Attending Physician. Existing oversized offer lists migrate once, then retain their new offers and depleted stock on reload. New cave medics have an explicit field-supply marker; older saved tents are not guessed from Y level. Their existing structures remain discoverable through the native structure command.

**World generation:** the floor/template changes apply to newly generated clinics or fresh template placements. Existing buildings and their surrounding terrain are not moved automatically. Use a newly generated clinic when checking this fix.

## Graphical test method

The test opens an actual Minecraft 1.20.1 Fabric client on the owner's desktop. Crafting, furnace/smoker, smithing and brewing checks use native screens and real client inventory-click packets. Creative supplies ingredients and prepares scenes; crafted or brewed outputs are not inserted into the inventory as test results. Furnace and brewing waits are accelerated through native block-entity ticks after their inputs are displayed.

Advancement scenes use the connected player's native gameplay actions. Some scene actions are invoked on the integrated server; they should not be described as physical mouse clicks for every advancement. Progress is cleared first, then the actual client advancement widgets must report completion. The hidden recipe reward is checked against every runtime mod recipe in the actual client recipe book. A prepared test weapon shortens the Wither kill; this proves the causal achievement, rather than the balance of a complete boss fight.

The active inventory is **29 recipes**, **15 potion forms**, **nine rusting/cleaning pairs**, and **28 active advancements** (27 displayed plus the recipe reward). The retired Creative-only **Bad Egg** ID remains hidden solely for save compatibility; it is not restored as an active achievement. The [item inventory](ITEM_ACQUISITION_2026-10-24.md) distinguishes craftable goods from drops, trades, weathering, brewing, Creative spawn eggs and retired tools.

## Verification results

- Visible client: all 29 native recipes crafted, cooked or smithed, with before/after screenshots.
- Visible client: all 15 drinkable/splash/lingering potion forms brewed and acquired; all nine weathering outputs acquired and cleaned through actual client wire-brush use.
- Graphical automatic despawn: six unnamed hostile variants were removed by vanilla distance rules while their chunks remained loaded and entity-ticking. The name-tagged zombie and three cow controls remained. Native removal reasons, server UUID lookup and client visibility were checked after returning.
- Full JEI client: all 28 active advancements earned by the connected player, with 27 completed visible widgets and all 29 recipe rewards verified in the actual client book. No direct criterion grants are used by the journey.
- Server: **303/303 passed twice** after the final fixture fixes. Both forced builds produced the same distributable JAR. All 375 resources regenerated identically twice; package/source agreement, three deliberately altered archive rejections and both full audio decodes passed.

Each complete graphical profile passed the same 29 recipes, 15 potion forms, nine weathering/cleaning pairs, all 28 active advancements, and native automatic despawning:

| Client profile | Result | Fresh native screenshots |
| --- | --- | --- |
| No recipe viewer | PASS | 145 |
| JEI | PASS | 150 |
| REI Light | PASS | 150 |
| REI Dark | PASS | 150 |

The Minecraft windows were visible on the owner's desktop. Window-only recordings were decoded and retain over five minutes per profile locally; the report preserves their hashes and duration. Original REI settings were restored byte for byte.

Open the [interactive graphical evidence](../validation/playtest-2026-10-24/index.html), the [opaque-sleeve before/after comparison](../validation/playtest-2026-10-24/gloves/index.html), or the [complete run records](../validation/playtest-2026-10-24/verification/results.json). The main gallery selects 134 original screenshots from the final JEI pass and provides 105 check cards. It identifies shared weathering overview images rather than presenting them as nine separate captures.

The strict file audit parsed/inventoried **1,077 tracked and newly added files**, including 190 Java sources, 389 JSON files, 18 XML reports, 354 images and 17 PowerShell scripts. Compilation/runtime results and manual code review are separate from these format, encoding, reference and asset checks.

The final JAR is `build/libs/rust-and-ruin-0.1.0-beta.jar`, **3,335,809 bytes**, SHA-256:

```text
C87F9F9DBEE462B889702D92B13B2082C1C62324649A4C46F3BDF103C61A76E4
```

Earlier failed test attempts exposed test-fixture problems separately from production defects: a drifting campfire drop outside a search box, native singleton Dragon Breath remainder behavior, player-only selector syntax for advancement resets, missing fake-connection player ticks, attack cooldown on a prepared Wither test, and holding use after a potion had become an empty bottle, a stale player-menu cursor during brush setup, and inserting scheduled actions while the native test schedule was being iterated. The opaque-sleeve probe initially had an unlit void scene; its accepted BEFORE/AFTER evidence uses actual native Night Vision, not a renderer override. Assertions were corrected or strengthened without injecting results or granting advancements. A separate test-only cleanup leak was fixed by unregistering only each owned mock player before discarding it; other simultaneous test actors are preserved.

No claim of complete bug freedom, every-modpack compatibility, or a long-running remote-server stress test is made. The owner should still playtest the built artifact before publishing.

## Possible later additions

- Cartographer maps to clinics and cave camps, so Survival players can find them without operator commands.
- Configurable medic delivery intervals and stock limits for server owners who want a different medicine economy.
