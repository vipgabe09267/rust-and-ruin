# Requested feature audit — 0.1.0-beta expansion

The latest instructions take precedence over earlier versions of a feature. In particular, temporary animal infections no longer change drops, the beacon now uses only Rusted Iron Blocks, and rusty iron tools are preserved for old saves but retired from new acquisition.

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
| Tetanus II | Additional 50% Wither book drop; I remains guaranteed. Anvil tests cover swords, axes, spears, rebar, pipes, bows, crossbows, and tridents; nails, hooklines, and mining tools are rejected. II causes disorientation, random wandering/missed mob attacks, and 2 HP nonlethal damage every ten seconds. |
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
