<p align="center"><img src="docs/branding/logo.png" width="200" alt="Rust & Ruin logo"></p>

# Rust & Ruin

Throw nails, set rusty traps, and fight infected creatures. Rust & Ruin adds weathering weapons, three diseases, village medics, and a tougher Wither with its own beacon.

**Minecraft 1.20.1 Fabric · version 0.1.0-beta**

[Download on Modrinth](https://modrinth.com/mod/rust-ruin) · [Recipes](docs/RECIPES.md) · [Validation](VALIDATION.md) · [Requested features](docs/REQUEST_AUDIT.md) · [Security review](docs/SECURITY_AUDIT.md) · [File audit](docs/FULL_AUDIT.md)

![Rust & Ruin in Minecraft](validation/screenshots/rust-and-ruin-hud.png)

## Install

Put `rust-and-ruin-0.1.0-beta.jar` in `mods` on **both client and server**, replacing older copies. Requires **Java 17, Fabric Loader 0.16.14+, and Fabric API for 1.20.1**.

JEI and REI are optional alternatives. Both display crafting, cooking, disease brewing, nine rusting conversions, and batch arrow smithing. Install your chosen viewer with its own dependencies; Rust & Ruin itself only requires Fabric API.

## Weapons and rust

Nails deal one heart per throw; rebar deals half a heart on a fully charged unarmored hit. Metal pipes, sharpened rebar, hooklines, and a new **Iron Spear** offer other options. The spear has seven base damage, a slower attack than an iron sword, and a right-click thrust reaching 4.5 blocks. Solid walls block the thrust.

Dropped supported metal rusts after **five consecutive seconds touching water** or **fifteen seconds in exposed rain**. Conversion preserves item names, damage, enchantments, and stack counts. Brush it clean to collect Rust, or place Rust on floors and walls. Existing rusty iron tools retain their IDs and behavior, but are retired from new rusting and the Creative tab in favor of the spear.

Caltrops and barbed wire damage creatures that step through them. **Bear Traps** close after one activation, inflicting Slowness X; use a **Trap Reset Tool** to release the victim and rearm them. Rusty traps spread Tetanus. Traps play infection sounds at their position.

Fishing rods left in water break into two sticks and two string, with a 15% Rusty Fishing Hook chance. Craft a hookline from the hook, chain, string, and a stick to pull and infect creatures up to twelve blocks away.

## Diseases and protection

| Disease | Symptoms | Treatment |
| --- | --- | --- |
| Tetanus | Random 5–10 minutes; stiffness, spasms, rusty HUD and tab-list hearts | Tetanus Antitoxin; milk cannot cure it |
| Lockjaw Sonata | Blocks eating during the final random 2–4 minutes of ordinary Tetanus; standalone potion lasts one minute | Tetanus Antitoxin; milk cannot cure it |
| Salmonella | Vomiting, nausea, hunger loss, and light nonlethal damage | Natural recovery or ordinary milk treatment |
| Rabies | Permanent; loses one heart of maximum health per minute down to two hearts, then ten seconds of nausea per minute; purple-red hearts | Rabies Cure from a medic, or death; milk and Tetanus Antitoxin do not cure it |

The Tetanus Vaccine grants twenty minutes of 80% prevention. Gloves halve rusty-contact risk and have 192 durability. Equip them by right-clicking or using their **dedicated inventory slot** above the offhand slot. Combined vaccine/glove protection is 90% by default. Gloves do not protect against contaminated food or potions.

**Tetanus Protection** is shield-only: a held enchanted shield adds 65% prevention against Tetanus I or 40% against II. Blocking the attack still prevents its normal damage. **Rabies Vaccine** provides twenty minutes of 90% prevention and is craftable or sold by medics. Vaccines prevent new infections; they do not cure existing ones. Milk clears beneficial vaccine effects.

Craft the reusable **Field Guide** from a book, Rust, and a feather. Its ten scrollable chapters include the connected server's prevention settings.

## Creatures and village medics

Permanent tetanized hostiles gain 50% maximum health and extra attack damage where supported. Animals move slower. Skins retain each creature's original texture layout. Only **permanently tetanized animal variants** replace their ordinary drops with 1–3 Infected Meat; temporary Tetanus does not change animal loot. Hostiles keep normal drops and add Rust plus a modest randomized salvage reward.

| Difficulty | Natural or world-generation Tetanized chance | Spawner chance |
| --- | --- | --- |
| Peaceful | 0% | 0% |
| Easy | 10% | 25% |
| Normal | 15% | 35% |
| Hard | 20% | 45% |

Withers and the Ender Dragon are excluded from these automatic rolls. Rusty contact retains the configurable default mutation chances of 15% for animals and 5% for other ordinary mobs. Certain unconverted mammals and undead can instead become rabid, at 1%/2%/3% on Easy/Normal/Hard. Rabid creatures have thirty extra health points, seven extra armor points, custom skins, and five-point melee attacks before armor. Only rabid animals drop Rabid Meat. Both contaminated meats become safe when cooked.

A **medic** sells antitoxin, both vaccines, Rabies Cure, and gloves from novice level. Small clinics can appear in plains, desert, savanna, snowy, and taiga villages. A crafted **Medicine Cabinet** also gives an unemployed villager the profession without a clinic. Other trades offer selected weapons, traps, tools, and hooks. Creative variant spawn eggs are available in the Rust & Ruin tab.

## Brewing and enchanted arrows

Bottle cow manure and brew it with water for a five-second Nausea potion. Add Rust for Tetanus, Infected Meat for Salmonella, or Rabid Meat for Rabies. Brew Tetanus with a pufferfish for Lockjaw Sonata. Gunpowder and dragon's breath make splash and lingering variants. [Exact recipes](docs/RECIPES.md).

Tetanized Arrows now use a **smithing table**: empty template, ordinary arrows as base, one Rust as addition. One Rust coats up to 64 arrows. They cause one minute of Tetanus alone and consume ammunition even with Infinity. **Tetanus I/II** books work on swords, axes, spears, industrial melee weapons, bows, crossbows, and tridents. Nails, hooklines, and mining tools cannot accept them. Enchanted shots cause full-duration infection; combining them with infected arrows adds 25% duration. Tetanus II keeps its stronger symptoms for the infection duration: it disorients targets, makes mobs wander and sometimes miss attacks, and deals two points of nonlethal damage every ten seconds.

## Wither and beacon progression

Use Rust on an iron golem for an 18% conversion chance; the rusty guardian has 200 maximum health. Each Survival attempt consumes one Rust.

Four Tetanized Soul Sand blocks in the ordinary Wither ritual give a 5% Tetanized Wither chance. A netherite block replacing the upper center soul sand raises the default chance to 30%. A failed roll still summons an ordinary Wither and consumes the ritual.

The special Wither has 600 health, a custom boss bar and appearance, extra loot, its normal star, a Tetanized Wither Star, guaranteed Tetanus I, and a 50% additional Tetanus II book chance. Wire-brush the special star for an ordinary Nether Star, or craft it into a Tetanized Beacon.

The beacon requires a pyramid made **only of Rusted Iron Blocks** and netherite ingot payments. Its rusty menu selects level II powers; a full pyramid offers Regeneration II or a level III primary upgrade. An active beam automatically grants Tetanus Resistance. Placement and full-strength activation have separate mod advancements and do not award vanilla beacon advancements.

## Advancements, commands, and sounds

The rusty advancement tab includes disease milestones, Strange Meat for collecting Infected Meat, Why did I eat that? for Salmonella, shield/glove prevention, medicine use, the medic, traps, variant eggs, and boss/beacon progression. Why did I do that? requires dying to your own upward-thrown rusty nail and has its own death message.

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
