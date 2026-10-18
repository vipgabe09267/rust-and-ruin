# Requested feature audit — 0.1.0-beta

Checked against the requested changes on October 17, 2026. Later requests take precedence over earlier probabilities and durations. This records the implemented behavior; detailed test results and limits are in [VALIDATION.md](../VALIDATION.md).

| Request | Implemented behavior and verification |
| --- | --- |
| Rust & Ruin, Fabric 1.20.1, still version 0.1 | Project branding, mod metadata, JAR name, logo, README and Modrinth copy agree. Internal `tetanusmod` IDs remain for existing saves. Both sides require the mod and Fabric API; JEI/REI are optional. |
| Craftable throwable nails and melee rebar | Nails use two iron plus flint, deal one heart and consume ammunition. Rebar deals half a heart on a fully charged ordinary hit without armor. Crafting, damage and projectile recovery have GameTests. |
| Rust in water; iron tools also rust in rain | Five consecutive seconds touching water, including floating at the surface; 15 seconds exposed to rain. Stack count and item data survive conversion. Floating nails, interrupted exposure and roofs are tested. |
| Disease lasting 120–300 seconds and affecting mobs | Rusty hits refresh a random duration in that range. Symptoms progress through stiffness and spasms. Mobs take mild nonlethal periodic damage. Permanent variants retain an infinite effect after additional hits. |
| Visible Lockjaw Sonata and advancement | A separate icon/timer and action-bar message appear during four-second late-stage episodes, recurring every 20 seconds. Food is blocked, drinks remain usable, and curing Tetanus clears it. Server assertions and both graphical clients check onset. |
| Louder supplied sound, correct positions, playsound | Normalized audio and increased default volume; mobs play the sound too. Arrows play it at impact and caltrops at the trap. All three sound events are registered. Subjective speaker loudness remains a playtest judgment. |
| Rusty health hearts, including player list | HUD and tab-list health objectives use rusty hearts while infected. Infection presence is synchronized and checked by both graphical runs. |
| Less tooltip clutter, clearer artwork and logo | Short item descriptions, hook details, isolated inventory sprites with transparent padding, dedicated disease icons, retained user logo. Screenshots and texture references were inspected. |
| JEI/REI and more distinctive recipes | Native crafting/cooking/brewing plus 11 Rusting displays. Caltrops use four nails and an iron nugget, shapeless. Both actual viewer clients check the recipe and brewing outputs. Full layouts are in [RECIPES.md](RECIPES.md). |
| Brushing returns Rust | One Rust per cleaning action, durability charged per cleaned item. Special stars can also be cleaned. Regression checks cover conversion and item preservation. Rust gained later requested crafting and golem uses. |
| More mob types, better textures and varied drops | All creatable vanilla mob types support permanent mutation, including spiders. Species textures preserve original UV layouts; special layers have additional handling. Hostile salvage includes Rust plus iron nuggets, copper, flint, string, coal or bone. |
| Animal frequency and infected meat | Latest rate is 15% per eligible rusty projectile/hookline hit; other mobs use 5%. Animals are slower and replace ordinary item drops with infected meat only. Cooking makes the meat safe; raw meat has a 10% Salmonella chance. |
| Salmonella and Strange Meat | Vomiting, hunger loss and light nonlethal damage; advancement awarded on infection. Cooking and symptom behavior are tested. |
| Rusty fishing hook and something useful to craft | Water breaks a fishing rod after five seconds into sticks/string, with a 15% hook chance and advancement. Hook + chain + string + stick makes a 12-block Rusty Hookline with pull, infection, cooldown and durability. |
| Commands to summon variants | Operator `/rustandruin summon` and `/rustandruin tetanize` support vanilla mobs. Withers can be forced by command without a ritual roll. |
| Rust-driven iron golem conversion | 18% per Survival Rust use, double health to 200, rusty texture and Rusty Guardian advancement. Old 150-health saves migrate without modifier stacking. Spectators cannot perform conversion or cow collection. |
| Tetanized Wither ritual and upgrade | Four Tetanized Soul Sand blocks give 5%; a netherite block replacing upper center adds 25 percentage points, for 30% by default. Ordinary soul sand gives no mutation. Structure consumption, 600 health, drops and ritual paths are tested. |
| Wither rewards and advancement | Normal star plus Tetanized Wither Star, Tetanus I, bonus loot and experience bottles; Tetanus. Nice One! advancement. The special star has normal Nether Star explosion protection. |
| Tetanus I and arrows | Treasure enchantment applies Tetanus through melee hits. Tetanized arrows use normal bow/crossbow mechanics, infect on impact and consume ammo even with Infinity. Rusted iron tools have a 20% melee infection chance. |
| Cow manure and both disease potion chains | Bottle a cow; water + manure gives base potion; add Rust for Tetanus or infected meat for Salmonella; gunpowder makes splash potions. All five brewing outputs are checked in JEI and REI. |
| Tetanized Beacon | Normal beacon recipe with corrupted star, cracked glass, gold/copper/netherite bases, netherite-only payment, primary II and full-pyramid Regeneration II or primary III. Actual block ticks, payment consumption, saves and GUI are checked. |
| Infection and rust advancements | A Painful Lesson, Rusty Business, Hook, Line and Tetanus, Strange Meat, Tetanized Mob, Rusty Guardian, Lockjaw Sonata and Tetanus. Nice One! are present with corresponding award paths. |
| Repository and Modrinth text | Public source, logo, recipe guide, version changelog and [paste-ready description](../MODRINTH_DESCRIPTION.md); README links the supplied Modrinth page. |

Permanent mutations also survive the vanilla mob conversion path, such as a zombie becoming a drowned. This does not promise support for arbitrary transformations implemented by other mods.

The audit combines code/resource inspection, dedicated-server GameTests and graphical clients with each recipe viewer. It cannot prove every modpack, network condition or emergent encounter bug-free. No version bump beyond 0.1 was made.
