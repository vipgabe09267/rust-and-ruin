# 0.1.0-beta - disease and creature fixes

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
