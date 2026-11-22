# 0.1.2-beta

- Remove Medical Prescription sales from current and saved medic catalogues. Write prescriptions at a Prescription Writer and use them to pay for treatment.
- Add six personal, two-stage medic cases across village clinics and cave camps. Deliver in partial stacks, complete fieldwork, and report back for supplies, emeralds, a reputation discount, and Special Delivery.
- Give field medics their own gear and salvage trades. Village medics focus their supply trades on clinic, brewing, and higher-rank provisions.
- Add Tetanus TNT with twice the blast power of vanilla TNT, the Tetanus. Nice One! sound, and a cosmetic rust cloud that lasts 5–15 seconds.
- Let spreading fire prime Tetanus TNT using its own fuse and blast.
- Add craftable Clinic Notice Boards that let players choose and preview village or cave cases.
- Add the Rust Magnet for collecting loose metal salvage and the Field Splint for temporary Tetanus spasm relief.
- Fix lost medic rewards when an inventory could only fit part of a stack.
- Fix cave-camp villagers getting stuck on the raised Prescription Writer while walking to their beds.
- Double boss-attributed damage from the Tetanized Wither and Ender Dragon. Add Wither III skull hits, Rustquake, a three-skull barrage, Rustfire pools, and Wingstorm. Both bosses enrage at half health and cast more often.
- Add advancement unlock steps to the Modrinth description and a complete advancement guide.

# 0.1.1-beta - boss and medic fixes (November 14, 2026)

- Fix Tetanus-enchanted bows and crossbows losing infection on the shot that breaks the weapon, including every arrow or firework in a Multishot volley. Five new native firing regressions passed in the final **423/423** suite.
- The final candidate is **3,557,756 bytes**, SHA-256 `E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`. Replace the previous 0.1.1-beta JAR on both sides. All **16 final visible-client reports passed**. [Readiness, evidence and limits](docs/RELEASE_READINESS_0.1.1-beta.md).

- Tetanized Withers drop only the special star; brush it for an ordinary Nether Star. Minions now have 20 health, an initial 10-15-second wave after the shield, later 30-60-second waves, and visible summons for nearby Creative observers.
- Add Prescription Writers to newly generated clinics/camps and possible medic trades; clean up their menu. Give medic ranks distinct treatment/supply pools, scarce emergency prescriptions, and saved stock limits.
- Convert all ten healing crystals during a special Dragon ritual; special crystal explosions infect only after accepted native blast damage.
- Raise Dragon grabs to 15%, lift caught players ten blocks, and retain the 120-second successful-grab cooldown. Add a custom Dragon bar, black egg with rust chips, and local slowed/reverberant victory audio.
- Move glowing, non-despawning shared Dragon rewards outside the exit fountain. Redesign the beacon menu with explicit II/III/IV controls and clear ingot/star payments.
- Keep the 650-HP Wither, 1,100-HP special Dragon, forging progression, single replacement-tier Tetanus book, and combat/medicine advancements.
- Current receipts and limits are tracked in [validation](VALIDATION.md); see [release notes](docs/RELEASE_NOTES_2026-11-14.md). The prior metadata-only version correction and its original receipts remain archived.

# 0.1.0-beta - medics, equipment, and disease fixes (October 23 refresh)

- Add five biome-themed clinics with a 55% eligible village-location roll, supply chests, and seven medic uniform palettes.
- Add locatable cave medic tents with a 15% candidate roll, over 2,000-block spacing, loot, and a defensive Tetanized Iron Golem.
- Add native locate entries and /rustandruin locate medic/tent shortcuts.
- Move species-colored, tinted variant eggs to Rust & Ruins Spawn Eggs; retire the Creative-only Bad Egg advancement.
- Improve the glove slot, show equipped gloves on players, and support Unbreaking/Mending and XP repairs in the dedicated slot.
- Replace the medicine cabinet with a two-block model with shelves, glass, vials, and opening doors; migrate old cabinets without overwriting ceilings.
- Keep Rabies nonlethal with a strict two-heart capacity floor; raw Infected Meat now causes Salmonella only, while raw Rabid Meat causes Rabies and immediate nausea unless vaccination prevents infection.
- Reveal advancements without first requiring an iron pickup; award protected shield blocks for ordinary rusty weapons too.
- Add operator disease aliases and ensure /effect bypasses random vaccine prevention.
- Add a failed eating gesture for Lockjaw and restore native arrow texture transparency.
- Replace veterinary kit, spear, rusty spear, and trap-reset-tool art; add treatment particles and a positional sound.
- Add rare skeleton nail throwers and zombies with rebar, rusty pipes, or rusty spears (2%/4%/6% by difficulty); preserve equipment after vanilla spawn initialization.
- Slightly increase rusty nail cooldown to 14 ticks; keep skeleton ammunition unrecoverable.
- Lower natural Tetanized rates to 0.5%/1%/2%, spawners to 3%/5%/8%, and eligible natural Rabies rates to 0.3%/0.6%/1%. Rust-contact conversion now defaults to 2% for animals, 1% for other mobs.
- See docs/REFRESH_2026-10-23.md for verification, findings, and remaining owner-testing limits.

# 0.1.0-beta - animal care, supply requests, and workshops

- Add separate natural animal, other mob, and spawner mutation settings for each difficulty.
- Add rotating medic supply deliveries with saved daily limits, bottle returns, reputation discounts, and an advancement.
- Add operator mob inspection and bounded nearby population counts without chunk loading.
- Add honeycomb rust-proofing with saved wet-weather/use wear, short tooltips, and JEI/REI displays.
- Add a craftable Veterinary Kit for permanently tetanized/rabid animals and Clean Bill of Health.
- Add naturally generated abandoned workshops with traps, worn equipment, balanced salvage, and occasional medicine.
- Fix Salmonella symptoms on effects longer than a minute, keep REI coating output durations current after config sync, and skip weather checks on non-rustable dropped items.
- Read executable project source in numbered sections; retain per-file ranges, hashes, review notes, test evidence, and stated limitations in the new audit.
# 0.1.0-beta - despawn and source audit (October 19, 2026)

- Remove forced persistence from tetanized and rabid conversion so wild variants follow native despawn and mob-cap rules.
- Repair affected legacy variant saves once, preserving recognizable named, owned, leashed, bucketed, mounted/passenger, NoAI and picked-equipment creatures; preserve intentional persistence in new saves.
- Prevent weaker infections from shortening stronger/infinite Tetanus or clearing its Lockjaw progress, including near recovery. Keep Disorientation aligned with the retained infection.
- Honor configured infection durations up to one hour, including the arrow combination bonus. Calculate that bonus before merging durations to avoid compounding.
- Preserve vanilla invalid-movement rejection during spasms and correct the operator conversion command's success count.
- Add sixteen regressions and a complete source-review inventory. See [despawn audit](docs/DESPAWN_AUDIT.md) for findings, migration details and testing limits.
# 0.1.0-beta — diseases, medics, traps, and progression

- Add persistent Rabies, rabid creatures and meat, a cure, vaccine, purple-red HUD/tab hearts, and disease brewing.
- Add village medics, five village clinic pool entries, a craftable workstation, and selected smith/fisherman trades.
- Add Iron/Rusted Iron Spears, bear traps, barbed wire, a reset tool, wall/floor Rust piles, and Rusted Iron Blocks.
- Retire new acquisition and Creative listings of rusty iron tools while retaining existing item IDs and behavior.
- Add Tetanus II and shield-only Tetanus Protection, snapshot ranged enchantments at firing, and replace arrow crafting with batch smithing.
- Add a synchronized glove inventory slot with save migration, death/respawn handling, and Creative support.
- Add difficulty-based natural and spawner variants plus specific variant spawn eggs.
- Replace beacon base materials with Rusted Iron Blocks, add automatic resistance, exact effect labels, a rusty menu, and separate advancements.
- Add protection, cure, Rabies, self-nail death, clinic/medic, trap, smithing, and progression advancements and custom death messages.
- Restore variant-only animal meat drops; temporary Tetanus no longer changes ordinary animal loot.
- Add standalone Lockjaw brewing and five-second manure nausea. Expand optional JEI/REI integration.
- Fix batch shift-click duplication, preserve a trapped victim through rusting, and cover direct Rabies potion exposure with its vaccine.
- Restrict Tetanus I/II to combat weapons; nails, hooklines, and mining tools cannot accept them. Keep II symptoms for the infection duration and cure them with antitoxin.
- Preserve the clinic medic profession through AI ticks and fail client smoke tests on queued server-task errors.
- Keep version 0.1.0-beta. The owner authorized committing this expansion on October 18, 2026; playtesting remains recommended.
# 0.1.0-beta - final stability audit

- Validate beacon pyramid tiers and secondary choices on the server before consuming netherite; reject invalid saved powers.
- Prevent duplicate infection/glove wear from an offhand hookline used alongside a rusty main-hand weapon.
- Exclude reflected Thorns armor damage from rusty melee infection.
- Make antitoxin consumption server-authoritative and verify wrong/empty items, spectator use and bottle accounting.
- Add seven regression tests, remove mutation-healing flakiness from damage assertions, and test graphical clients without a recipe viewer.
- Audit every project file and packaged resource; detect duplicate JAR entries and stale assets, and check every item/status sprite border.
- Remain on 0.1.0-beta. Commit authorized by the owner after automated validation.
# 0.1.0-beta - prevention and field guide

- Added a craftable Tetanus Vaccine: 20 minutes of 80% protection, with a visible Tetanus Resistance effect. It does not cure existing infections or prevent Salmonella.
- Added offhand Protective Gloves: 50% protection from rusty contact and 192 durability. Combined vaccine/glove protection is 90% by default.
- Added a reusable ten-chapter Field Guide with recipes, symptoms, server protection settings, creatures, salvage, brewing, commands and boss progression.
- Added item artwork, crafting recipes, recipe unlocks and optional JEI/REI displays. No new mandatory dependency.
- Hardened client custom-packet parsing, bounded the infection cache, checked command helper permissions/coordinates, and enforced hookline cooldowns in the item itself.
- Scoped prevention checks to individual entities, including nested callbacks and failure cleanup. Blocked hits cannot reset disease or permanently mutate a mob.
- Pinned the official Gradle distribution checksum and added a reproducible release integrity audit.
- Retained version 0.1.0-beta. Replace the JAR on both clients and server.

## Earlier 0.1 disease and creature fixes

- Tetanus now lasts a random 5-10 minutes, including infections from drinkable, splash and lingering disease potions.
- Lockjaw Sonata now lasts continuously through the final randomly chosen 2-4 minutes. Its duration survives saving and reloading.
- Milk preserves Tetanus, Lockjaw and their progression. Antitoxin immediately clears both; natural recovery still works.
- Animals with temporary Tetanus now drop infected meat, just like permanent variants, without ordinary animal loot.
- Strange Meat unlocks from collecting infected meat. Added Why did I eat that? only for contracting Salmonella; eating without infection does not grant it.
- Rusty melee weapons, enchanted weapons, rusted iron tools and caltrops can roll permanent variants, alongside nails, arrows and hooklines. Existing 15% animal / 5% other mob rates remain.
- Replaced the generic skin overlay with species-specific base render textures; expanded client checks across vanilla renderer types.
- Old default infection settings migrate automatically; customized durations remain intact.
- Salmonella vomiting now includes nausea, with hunger loss and mild nonlethal damage.
- Disease potion tooltips describe their actual symptoms and random Tetanus duration instead of only listing a movement penalty.
- Added a rusty metal wallpaper to the advancement tab.
- Kept version 0.1.0-beta. Replace the JAR on both clients and servers.

## Earlier 0.1 progression update

- Added disease brewing: collect cow manure with a bottle, brew with water, then add Rust for Tetanus or infected meat for Salmonella. Gunpowder makes splash variants.
- Added Lockjaw Sonata as a visible late-stage symptom and advancement. It blocks eating for four seconds every 20 seconds during the final stage.
- Added Strange Meat, Tetanized Mob, and Rusty Guardian advancements.
- Added the Rusty Hookline recipe and hook tooltip.
- Added 18% rust-driven iron golem conversion with 200 health.
- Expanded permanent variants to all vanilla mob types and raised animal conversion to 15%.
- Replaced shared skin overlays with species-aware recoloring and added more hostile salvage variety.
- Added the netherite Wither ritual upgrade: 5% base chance becomes 30% by default.
- Added Tetanized Wither Stars and the cracked-glass Tetanized Beacon, with netherite payments, level II powers, and a level III upgrade.
- Added rusty hearts to tab-list health objectives.
- Fixed water-surface rusting, neighboring icon fragments, and the caltrop recipe. Four nails plus an iron nugget now make four caltrops, shapeless.
- Added native REI Rusting displays alongside JEI and verified disease brewing in both.
- Retained the 0.1.0-beta version. Install the updated JAR on both clients and the server.
## Final audit fixes

- Preserve permanent infection when a rusty hit refreshes Tetanus, and carry mutations through vanilla mob conversions such as zombie to drowned.
- Migrate older tetanized iron golems to 200 maximum health without stacking modifiers.
- Block spectator interactions with Rust on golems and manure collection from cows.
- Give Tetanized Wither Stars the same explosion protection as ordinary Nether Stars.
- Add regression reproductions and a request-by-request implementation audit.
