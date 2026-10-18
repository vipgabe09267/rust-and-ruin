# 0.1.0-beta - progression update

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
