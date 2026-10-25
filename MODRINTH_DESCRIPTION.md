# Rust & Ruin

Throw nails, set traps, and put rusty metal to use. **Rust & Ruin** adds weapons, diseases, infected creatures, village medics, and a tougher Wither to **Minecraft 1.20.1 Fabric**.

## Weapons, rust, and traps

Fight with throwable nails, rebar, metal pipes, and spears. Leave metal equipment in water for five seconds or exposed to rain for fifteen seconds to rust it. Rusty weapons can spread Tetanus; a wire brush cleans them and gives you Rust for crafting or placing on floors and walls.

Set caltrops and barbed wire, or catch enemies in **bear traps**. Bear traps snap shut and hold victims in place with Slowness X. A Trap Reset Tool opens the jaws and gives three seconds to move clear before rearming. Rusted traps can infect their victims.

The Iron Spear has a slower swing than an iron sword, seven base damage, and a right-click thrust with extra reach.

## Three diseases, different problems

- **Tetanus** lasts 5-10 minutes and causes stiffness, spasms, and rusty brown hearts. During the final 2-4 minutes, **Lockjaw Sonata** prevents eating. Antitoxin cures both; milk cannot.
- **Salmonella** causes vomiting, nausea, hunger loss, and light damage. Raw Infected Meat causes Salmonella only. Cooking it makes it safe to eat.
- **Rabies** lasts until death or treatment with Rabies Cure. It removes one heart of maximum health each minute until only two hearts remain, then causes recurring nausea. Rabies deals no direct damage and stops reducing capacity at two hearts. Your hearts turn purple-red.

Equip **Protective Gloves** in their dedicated inventory slot, carry antitoxin, and vaccinate before a fight. Tetanus and Rabies have separate vaccines. Gloves have 192 durability, appear on your character, and accept Unbreaking and Mending, including XP repairs while equipped. The **Tetanus Protection** shield enchantment reduces infection risk from Tetanus I by 65% and Tetanus II by 40%.

## Infected creatures

Tetanized mobs have rusty skins, increased strength, and extra salvage drops. Tetanized animals move slower and drop only **Infected Meat**. Ordinary animals with temporary Tetanus keep their normal drops. Saddles, equipped items, and stored cargo still follow normal drop rules.

Naturally spawned creatures have a **0.5% / 1% / 2%** chance of being tetanized on Easy / Normal / Hard. Mob spawners use **3% / 5% / 8%**. Some mammals and undead can also become rarer **rabid variants**, with sickly skins, extra health, armor, and aggressive attacks. Only rabid animals drop Rabid Meat.

Creative variant eggs keep each species recognizable, with rusty or purple tints and their own **Rust & Ruins Spawn Eggs** tab.

## Village medics

Find a medic in a village clinic or place a crafted **Medicine Cabinet** to give an unemployed villager the job. Medics progress through **Nurse**, **Nurse Practitioner**, **Medical Doctor**, and **Attending Physician**, with two randomized offers per rank. Treatments have only **2-4 purchases** per delivery; village supplies replenish at most once per in-game day. Barters use emeralds, iron, flint, honey and other supplies. Higher ranks can offer potion ingredients, protection books, golden apples and a scarce enchanted golden apple. Hero of the Village discounts apply.

New villages roll a **55% clinic chance** where terrain and space allow. Clinics and medic uniforms match plains, desert, savanna, snowy, and taiga villages, with treatment beds, a two-block stainless-steel and glass medicine cabinet, and a loot chest. Smiths and fishermen also offer selected equipment and salvage. A reusable **Field Guide** explains recipes and progression in-game.


Explore roomy, dry Overworld caves for rare **medic camps**: a fenced and lit tent courtyard, two beds, a field medic, a support farmer with a small irrigated farm, supplies, and a persistent Tetanized Iron Golem. Field medics carry at most **two purchases per offer** and receive supplies no more often than once every three in-game days. Each suitable cave site within a widely spaced candidate chunk rolls 15%; camps stay more than 2,000 blocks apart and need a natural floor and open entrance. A wider cave search makes camps easier to find while keeping the same spacing. New structures appear in newly generated terrain; existing camps are not rebuilt automatically.

Operators can find them with `/locate structure rustandruin:medic_clinic`, `/locate structure rustandruin:medic_tent`, or `/rustandruin locate medic` and `/rustandruin locate tent`. Branded clinic and tent results include the exact entrance coordinates, including height. Follow the clinic entrance result to reach its porch. `/locate poi rustandruin:medic_tent` searches only 128 blocks around you; use the structure command for distant camps. Older command names remain supported.

Infection announcements are positional and limited to nearby listeners (16 blocks). Existing infections, natural mutations, chunk loading and inherited mutations stay quiet. Medics have their own short vial-and-supplies work sound, audible within 12 blocks.

## Brewing and enchanted weapons

Collect Cow Manure using a glass bottle on a cow, then brew it with water. The resulting potion causes five seconds of nausea and serves as a disease-potion base.

| Base potion | Ingredient | Result |
| --- | --- | --- |
| Cow Manure | Rust | Tetanus |
| Cow Manure | Infected Meat | Salmonella |
| Cow Manure | Rabid Meat | Rabies |
| Tetanus | Pufferfish | Lockjaw Sonata |

Add gunpowder for splash potions or dragon's breath for lingering versions.

Make **Tetanized Arrows** at a smithing table: leave the template slot empty, add ordinary arrows and one Rust, and convert up to 64 arrows at once. They cause one minute of Tetanus and are consumed even with Infinity.

**Tetanus I and II** apply to combat weapons, including swords, axes, spears, rebar, pipes, bows, crossbows, and tridents. Enchanted ranged weapons apply full-duration infection; combining them with Tetanized Arrows extends it by 25%. Tetanus II adds disorientation, erratic mob movement, missed attacks, and periodic damage throughout the infection.

Skeletons occasionally throw rusty nails in place of shooting a bow, while zombies can carry rebar, rusty pipes, or rusty spears. Equipment chances are **2% / 4% / 6%** on Easy / Normal / Hard. Rusty nails have a slightly longer throwing cooldown.

## The Tetanized Wither

Build a Wither using four **Tetanized Soul Sand** blocks for a **5%** chance of the special variant. Replacing the upper center block with netherite raises the default chance to **30%**.

The Tetanized Wither has **600 health**, a custom appearance and boss bar, bonus loot, a normal Nether Star, and a **Tetanized Wither Star**. It guarantees a Tetanus I book and has an additional **50% chance** to drop Tetanus II.

Use its special star to craft a **Tetanized Beacon**. Build the pyramid from **Rusted Iron Blocks** and pay with netherite ingots. Primary powers start at level II; a full pyramid offers Regeneration II or an upgrade to level III. An active beacon also grants Tetanus Resistance automatically.

## Advancements

Earn advancements for preventing infection with gloves or a protected shield, recovering with antitoxin, contracting and curing Rabies, and completing the Wither and beacon progression. Drinking either vaccine awards **Faucci Ouchie**; successfully converting an iron golem awards **Tetanized Guardian**.

Pick up Infected Meat for **Strange Meat**, contract Salmonella for **Why did I eat that?**, or die to your own upward-thrown rusty nail for **Why did I do that?**

## Requirements

**Minecraft Java 1.20.1 | Fabric Loader 0.16.14+ | Fabric API | Java 17**

Install on **both client and server**. **JEI and REI are optional alternatives**, with crafting, cooking, brewing, rusting, and arrow smithing displays. Install your chosen viewer's dependencies too. Rust & Ruin itself requires no additional mod beyond Fabric API.

[Source code and recipe guide](https://github.com/vipgabe09267/rust-and-ruin) | [Modrinth downloads](https://modrinth.com/mod/rust-ruin)

## Keeping the rust under control

Wax clean equipment with honeycomb to delay rust, or use a **Veterinary Kit** to bring a permanently infected animal back to normal. Treating an animal earns **Clean Bill of Health**.

Village medics accept a rotating daily supply delivery. Sneak and right-click one to see what they need; completing the request earns better medicine prices and **Just What the Doctor Ordered**.

Explore **abandoned workshops** for rusty traps, scrap, worn weapons, and occasional medicine. Server owners can configure natural animal, other mob, and spawner mutation rates separately. Operators also have nearby population counts and individual despawn diagnostics.
