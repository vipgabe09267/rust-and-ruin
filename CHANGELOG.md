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
