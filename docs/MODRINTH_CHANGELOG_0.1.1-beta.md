# Rust & Ruin 0.1.1-beta

For **Minecraft 1.20.1 Fabric**. Replace the older JAR on **both client and server**. Requires Java 17, Fabric Loader 0.16.14+, and Fabric API. JEI and REI remain optional alternatives.

## Bosses and progression

- The Tetanized Wither ritual now has a **75%** chance with four Tetanized Soul Sand blocks, or **100%** with a netherite block in the upper center. The boss has **650 health** and awards native **50 XP**, bonus loot, and one Tetanized Wither Star. Brush the star for an ordinary Nether Star.
- It drops exactly **one Tetanus book**: **50% Tetanus II, otherwise Tetanus I**. Minions have **20 health** and iron swords. The first wave arrives 10-15 seconds after the spawn shield ends; later waves follow every 30-60 seconds, with at most two per wave and six living minions per Wither.
- Craft the **Tetanus Wither Forging Table** from four netherite blocks, an Eye of Ender, two blaze powder, a Tetanized Wither Star, and an anvil. Combine **End Crystal + Rust + netherite ingot** at the table to make a Tetanized End Crystal.
- Respawn the dragon with **four special crystals** to summon the **1,100-health Tetanized Ender Dragon**. Mixed crystals produce an ordinary dragon. Special rituals also infect all ten pillar crystals; their explosions spread Tetanus only after accepted blast damage.
- The special dragon has a custom boss bar and a **15% close-pass grab chance**. It lifts a caught Survival player ten blocks before dropping them, with a **120-second cooldown** after a successful grab.
- Every special-dragon victory with mob loot enabled yields one shared **Tetanized Dragon Egg** and **Tetanus Overload Sword**. Rewards land outside the exit fountain, glow, and do not despawn. The sword has **12 attack damage, 2.3 attack speed, 4,062 durability**, and Tetanus II. Repair it with netherite blocks at an anvil or Mending.
- Add forging, dragon-summoning, and dragon-defeat advancements. Defeat credit requires at least **20 actual damage** during the fight.

## Medicine and the beacon

- Add the **Prescription Writer** and **Medical Prescription**. One paper and one ink sac make a prescription; medic treatments cost emeralds plus one prescription. Writers appear in newly generated clinics and cave camps and can be sold by lower-rank medics.
- Give each medical rank one randomized treatment and one supply/barter offer. Village medicines have **2-4 purchases per delivery**; field supplies are scarcer and restock every three in-game days. Emergency prescriptions are limited to two purchases in villages or one in camps. Native Hero discounts apply, and saved catalogue updates preserve remaining stock and delivery time.
- Clarify supply requests and how to find rabid creatures. Fix early antitoxin cures awarding **A Dose of Common Sense** and improve mod advancement icons.
- Redesign the **Tetanized Beacon** menu with separate effect and **II / III / IV** controls. Build the pyramid from Rusted Iron Blocks and pay with netherite. **Every confirmed level-IV selection also consumes a Tetanized Wither Star**. Active beacons automatically grant Tetanus Resistance.

## Fixes and checks

Fix Tetanus being lost on the bow or crossbow shot that breaks the weapon, including later Multishot arrows and fireworks. Also fix stale ritual conversion, dragon grab/reload cleanup, special health limits, recoverable boss rewards, minion item pickup, and stock being transferred between unrelated medicine offers. Preserve native lighting, collision, despawning, and Creative protection rules.

The final candidate passed **423 native tests** and **16 visible-client reports**, including real crafting, causal advancements, boss rituals, workstation transactions, and JEI/REI flows. Controlled fixtures and remaining testing limits are recorded in the repository's release-readiness evidence.

Existing clinics and camps are not rebuilt; generate new terrain for the updated layouts and built-in writers. Existing items and advancement progress are preserved. Customized server settings can differ from the defaults above.

[Source, recipes, and test evidence](https://github.com/vipgabe09267/rust-and-ruin) | [Modrinth project](https://modrinth.com/mod/rust-ruin)
