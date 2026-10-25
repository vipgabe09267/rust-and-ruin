<p align="center"><img src="docs/branding/logo.png" width="200" alt="Rust & Ruin logo"></p>

# Rust & Ruin

Throw nails, set rusty traps, and fight infected creatures. Rust & Ruin adds weathering weapons, three diseases, village medics, and a tougher Wither with its own beacon.

**Minecraft 1.20.1 Fabric · version 0.1.0-beta**

[Download on Modrinth](https://modrinth.com/mod/rust-ruin) · [Recipes](docs/RECIPES.md) · [Validation](VALIDATION.md) · [Latest playtest fixes](docs/PLAYTEST_FIXES_2026-10-25.md) · [Requested features](docs/REQUEST_AUDIT.md) · [Security review](docs/SECURITY_AUDIT.md) · [File audit](docs/FULL_AUDIT.md)

![Rust & Ruin in Minecraft](validation/screenshots/rust-and-ruin-hud.png)

## Install

Put `rust-and-ruin-0.1.0-beta.jar` in `mods` on **both client and server**, replacing older copies. Requires **Java 17, Fabric Loader 0.16.14+, and Fabric API for 1.20.1**.

JEI and REI are optional alternatives. Both display crafting, cooking, disease brewing, nine rusting conversions, and batch arrow smithing. Install your chosen viewer with its own dependencies; Rust & Ruin itself only requires Fabric API.

## Weapons and rust

Nails deal one heart per throw; rebar deals half a heart on a fully charged unarmored hit. Metal pipes, sharpened rebar, hooklines, and a new **Iron Spear** offer other options. The spear has seven base damage, a slower attack than an iron sword, and a right-click thrust reaching 4.5 blocks. Solid walls block the thrust.

Dropped supported metal rusts after **five consecutive seconds touching water** or **fifteen seconds in exposed rain**. Conversion preserves item names, damage, enchantments, and stack counts. Brush it clean to collect Rust, or place Rust on floors and walls. Existing rusty iron tools retain their IDs and behavior, but are retired from new rusting and the Creative tab in favor of the spear.

Caltrops and barbed wire damage creatures that step through them. **Bear Traps** close after one activation, holding the victim in place with Slowness X; use a **Trap Reset Tool** to release it, with three seconds to move clear before rearming. Rusty traps spread Tetanus. Traps play infection sounds at their position.

Fishing rods left in water break into two sticks and two string, with a 15% Rusty Fishing Hook chance. Craft a hookline from the hook, chain, string, and a stick to pull and infect creatures up to twelve blocks away.

## Diseases and protection

| Disease | Symptoms | Treatment |
| --- | --- | --- |
| Tetanus | Random 5–10 minutes; stiffness, spasms, rusty HUD and tab-list hearts | Tetanus Antitoxin; milk cannot cure it |
| Lockjaw Sonata | Blocks eating during the final random 2–4 minutes of ordinary Tetanus; standalone potion lasts one minute | Tetanus Antitoxin; milk cannot cure it |
| Salmonella | Vomiting, nausea, hunger loss, and light nonlethal damage | Natural recovery or ordinary milk treatment |
| Rabies | Permanent; loses one heart of maximum health per minute down to two hearts, then ten seconds of nausea per minute; purple-red hearts | Rabies Cure from a medic, or death; milk and Tetanus Antitoxin do not cure it |

The Tetanus Vaccine grants twenty minutes of 80% prevention. Gloves halve rusty-contact risk and have 192 durability. Equip them by right-clicking or using their **dedicated inventory slot** above the offhand slot. Combined vaccine/glove protection is 90% by default. Gloves appear on your character, support Unbreaking and Mending, and repair from XP while equipped. They do not protect against contaminated food or potions.

**Tetanus Protection** is shield-only: a held enchanted shield adds 65% prevention against Tetanus I or 40% against II. Blocking the attack still prevents its normal damage. **Rabies Vaccine** provides twenty minutes of 90% prevention and is craftable or sold by medics. Vaccines prevent new infections; they do not cure existing ones. Milk clears beneficial vaccine effects.

Craft the reusable **Field Guide** from a book, Rust, and a feather. Its ten scrollable chapters include the connected server's prevention settings.

## Creatures and village medics

Permanent tetanized hostiles gain 50% maximum health and extra attack damage where supported. Animals move slower. Skins retain each creature's original texture layout. Only **permanently tetanized animal variants** replace their ordinary drops with 1–3 Infected Meat; temporary Tetanus does not change animal loot. Equipped items and stored cargo still follow vanilla drop rules. Hostiles keep normal drops and add Rust plus a modest randomized salvage reward.

| Difficulty | Natural or world-generation Tetanized chance | Spawner chance |
| --- | --- | --- |
| Peaceful | 0% | 0% |
| Easy | 0.5% | 3% |
| Normal | 1% | 5% |
| Hard | 2% | 8% |

Withers and the Ender Dragon are excluded from these automatic rolls. Rusty contact retains the configurable default mutation chances of 2% for animals and 1% for other ordinary mobs. Certain unconverted mammals and undead can instead become rabid, at 0.3%/0.6%/1% on Easy/Normal/Hard. Rabid creatures have thirty extra health points, seven extra armor points, custom skins, and five-point melee attacks before armor. Only rabid animals drop Rabid Meat. Raw Infected Meat causes Salmonella only; raw Rabid Meat causes Rabies and immediate nausea unless vaccination prevents infection. Both meats become safe when cooked.

Medics progress from **Nurse** through **Nurse Practitioner**, **Medical Doctor**, and **Attending Physician**. Each rank adds two randomized treatment and supply/barter offers. Distinct medicines have 2-4 purchases per delivery; village supplies replenish at most once per 24,000 world ticks. Higher ranks can trade potion ingredients, protection books, golden apples and a scarce enchanted golden apple. Native Hero of the Village discounts apply.

New village locations roll a **55% clinic chance**, with suitable terrain and a free plot required. Five biome themes use local materials, medical uniforms and supply chests. A crafted **stainless-steel Medicine Cabinet** also gives an unemployed villager the profession without a clinic. Field medics in cave tents have at most two purchases per offer and replenish at most once per 72,000 world ticks. Other villagers offer selected weapons, traps, tools and hooks. Species-colored variant eggs have their own **Rust & Ruins Spawn Eggs** tab.

## Brewing and enchanted arrows

Bottle cow manure and brew it with water for a five-second Nausea potion. Add Rust for Tetanus, Infected Meat for Salmonella, or Rabid Meat for Rabies. Brew Tetanus with a pufferfish for Lockjaw Sonata. Gunpowder and dragon's breath make splash and lingering variants. [Exact recipes](docs/RECIPES.md).

Tetanized Arrows now use a **smithing table**: empty template, ordinary arrows as base, one Rust as addition. One Rust coats up to 64 arrows. They cause one minute of Tetanus alone and consume ammunition even with Infinity. **Tetanus I/II** books work on swords, axes, spears, industrial melee weapons, bows, crossbows, and tridents. Nails, hooklines, and mining tools cannot accept them. Enchanted shots cause full-duration infection; combining them with infected arrows adds 25% duration. Tetanus II keeps its stronger symptoms for the infection duration: it disorients targets, makes mobs wander and sometimes miss attacks, and deals two points of nonlethal damage every ten seconds.

## Wither and beacon progression

Use Rust on an iron golem for an 18% conversion chance; rusty projectile hits also have a dedicated 18% conversion chance by default. The rusty guardian has 200 maximum health. Each Survival Rust attempt consumes one Rust.

Four Tetanized Soul Sand blocks in the ordinary Wither ritual give a 5% Tetanized Wither chance. A netherite block replacing the upper center soul sand raises the default chance to 30%. A failed roll still summons an ordinary Wither and consumes the ritual.

The special Wither has 600 health, a custom boss bar and appearance, extra loot, its normal star, a Tetanized Wither Star, guaranteed Tetanus I, and a 50% additional Tetanus II book chance. Wire-brush the special star for an ordinary Nether Star, or craft it into a Tetanized Beacon.

The beacon requires a pyramid made **only of Rusted Iron Blocks** and netherite ingot payments. Its rusty menu selects level II powers; a full pyramid offers Regeneration II or a level III primary upgrade. An active beam automatically grants Tetanus Resistance. Placement and full-strength activation have separate mod advancements and do not award vanilla beacon advancements.

## Advancements, commands, and sounds

The rusty advancement tab includes disease milestones, Strange Meat for collecting Infected Meat, Why did I eat that? for Salmonella, shield/glove prevention, medicine use, the medic, traps, and boss/beacon progression. Why did I do that? requires dying to your own upward-thrown rusty nail and has its own death message.

Operator permission level 2:

```mcfunction
/rustandruin summon minecraft:spider ~ ~ ~
/rustandruin summon minecraft:wither ~ ~ ~
/rustandruin tetanize @e[type=minecraft:zombie,distance=..10]
/give @s tetanusmod:iron_spear
/give @s tetanusmod:rabid_cow_spawn_egg
/playsound tetanusmod:tetanus_effect master @a ~ ~ ~ 1 1
/playsound tetanusmod:spasm master @a ~ ~ ~ 1 1
/playsound tetanusmod:vomit master @a ~ ~ ~ 1 1
```

The canonical save namespace remains `tetanusmod`; `/give` also suggests matching mod item paths without needing the namespace typed first.

## Configure, build, and test

Launch once to create `config/tetanusmod.json`. Stop the instance before editing it. Existing settings control rust timing, disease duration, damage, sound volume, camera shaking, rusty-contact mutation chances, and Tetanus vaccine/glove protection. Automatic spawn rates and Rabies behavior are the fixed defaults documented above. Custom duration settings remain intact; older default durations migrate to 300–600 seconds.

With JDK 17:

```powershell
.\gradlew.bat build runGameTest
.\gradlew.bat runClientSmoke
.\gradlew.bat -PwithJei runClientSmoke
.\gradlew.bat -PwithRei runClientSmoke
.\tools\Audit-Files.ps1 -Report .tools/file-audit.json
.\tools\Audit-Release.ps1
```

Use `--offline` if dependencies are already cached. The distributable is `build/libs/rust-and-ruin-0.1.0-beta.jar`; test helpers and recipe viewer implementations are excluded. See [validation](VALIDATION.md) for the checks actually run and their limits.

Wild tetanized and rabid variants use their species' normal despawn and mob-cap rules. Previously affected saves are repaired on load while recognizable player-kept creatures retain persistence. See the [October 19 source and despawn audit](docs/DESPAWN_AUDIT.md).

## Animal care, supplies, and exploration

A single-use **Veterinary Kit** cures a permanently tetanized or rabid animal, restoring its normal movement, attributes, appearance, behavior, and drops. Treating one earns **Clean Bill of Health**. Craft a kit from two leather, a golden apple, two antitoxins, Rabies Cure, and string.

Use **honeycomb** with clean, rustable equipment in your other hand to wax it. Coating costs one honeycomb per item and protects dropped equipment for ten minutes of water/rain exposure by default. Each point of equipment durability use also consumes one second of coating. The coating survives saving and item splitting. It does not clean already rusty items or protect traps after placement. JEI and REI have dedicated coating displays.

Sneak and right-click a **medic** to see the current supply request. Deliver paper, honey bottles, glow berries, or white wool as requested for a reputation discount on medicine. Each medic accepts one delivery per world day; requests rotate daily and completed deliveries survive reloads. Vanilla reputation decay and price limits still apply. Deliveries award **Just What the Doctor Ordered**.

**Abandoned workshops** can generate in new chunks in plains, sunflower plains, forest, birch forest, taiga, meadow, savanna, and dark forest biomes. Their ruined forges contain rusty traps, modest salvage, worn weapons, and occasional medicine. They add no creature spawners.

Natural mutation settings now separate animals, other mobs, and dungeon spawners for each difficulty. Current defaults are 0.5%/1%/2% natural and 3%/5%/8% spawners; matching old defaults migrate automatically, while other customized rates are retained. Configure `naturalAnimalEasy/Normal/Hard`, `naturalMobEasy/Normal/Hard`, and `spawnerEasy/Normal/Hard` in `config/tetanusmod.json` and restart the server. `coatingWeatherTicks` defaults to 12000.

Operators can use `/rustandruin inspect <entity>` to inspect mutation and despawn state, or `/rustandruin population [1-128]` to count nearby loaded mobs. These commands do not load additional chunks. See the [fresh numbered audit](docs/LINE_AUDIT.md) for review coverage, fixes, and limits.

## Medic cave camps and armed monsters

Medic tents can appear in roomy, dry Overworld caves, using a 15% roll for each suitable cave site within a widely spaced candidate chunk. Natural floor support, an open interior and an entrance are required; this is not a 15% chance per cave or chunk. Camps remain over 2,000 blocks apart and contain a medic, a loot chest, and a defensive 200-health Tetanized Iron Golem. Clinics and camps generate in new terrain; existing villages are not retrofitted.

Operators can use `/locate structure tetanusmod:medic_clinic`, `/locate structure tetanusmod:medic_tent`, or `/locate structure #tetanusmod:medics`. Shortcuts: `/rustandruin locate medic` and `/rustandruin locate tent`. Disease commands use the same namespace: `/effect give @s tetanusmod:rabies infinite`; short names such as `/effect give @s rabies` also work. These operator commands bypass vaccine prevention.

Skeletons can trade their bow for rusty nails; zombies can carry rebar, a rusty pipe, or a rusty spear. Weapon chances are 2%/4%/6% on Easy/Normal/Hard. Nail skeletons respect line of sight, and their missed shots cannot be collected for free ammunition. Rusty player-thrown nails now have a 0.7-second cooldown.

See the [October 24 playtest fixes and graphical verification](docs/PLAYTEST_FIXES_2026-10-24.md) for current changes and test limits. The [24-request refresh](docs/REFRESH_2026-10-23.md) records the earlier work.

The earlier [full-project audit](docs/REAUDIT_2026-10-23.md) records stability fixes, including pack-animal cargo preservation, End-return disease persistence, glove synchronization, trap contact checks, and native Wither ritual compatibility.
