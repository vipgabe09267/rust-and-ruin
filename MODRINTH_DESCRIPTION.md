# Rust & Ruin

Throw nails, set rusty traps, and fight infected creatures. **Rust & Ruin** adds weathering weapons, three diseases, village medics, and Tetanized Wither and Ender Dragon fights to **Minecraft 1.20.1 Fabric**.

## Weapons that weather

Fight with throwable nails, rebar, metal pipes, and spears. Leave supported metal equipment in water for **five seconds** or exposed rain for **fifteen seconds** to rust it. Rusty weapons can spread Tetanus. A wire brush cleans them and leaves Rust for crafting or placing on floors and walls. Wax clean equipment with honeycomb to delay corrosion.

Set caltrops and barbed wire, or catch creatures and Survival players in **bear traps**. A trap snaps shut, holds its victim with Slowness X, and needs a **Trap Reset Tool** before it can catch someone else. Resetting opens the jaws and gives three seconds to move clear. Rusty traps can infect their victims.

Craft **Tetanus TNT** with one TNT in the center, **four Rust in the corners**, and **four gunpowder in the remaining slots**. It ignites like vanilla TNT and has twice the blast power. The explosion plays **Tetanus. Nice One!** and leaves a rust-colored particle cloud for a random **5–15 seconds**. The cloud is cosmetic.

Right-click a **Rust Magnet** to pull loose Rust, nails, rebar, and iron or copper salvage from up to seven blocks away. Solid blocks stop the pull. Each successful pulse spends one durability and brings items toward you for normal pickup.

The **Iron Spear** has seven base damage, a slower swing than an iron sword, and a right-click thrust with extra reach. Salvage a **Rusty Fishing Hook** from a waterlogged rod and craft a hookline to pull enemies toward you.

## Three diseases, different problems

- **Tetanus** lasts 5-10 minutes, causing stiffness, spasms, and rusty brown hearts. During the final 2-4 minutes, **Lockjaw Sonata** prevents eating. Tetanus Antitoxin cures both; milk cannot.
- **Salmonella** causes vomiting, nausea, hunger loss, and light nonlethal damage. Raw Infected Meat causes Salmonella only. Cook it before eating.
- **Rabies** lasts until death or treatment with Rabies Cure. It removes one heart of maximum health each minute until two hearts remain, then causes recurring nausea. Rabies deals no direct damage. Hearts turn purple-red.

Equip **Protective Gloves** in their dedicated inventory slot. They have 192 durability, appear on your character, and accept Unbreaking and Mending. Separate Tetanus and Rabies vaccines lower the risk of a new infection; they do not cure an existing one. Vaccination is protection, not immunity.

The **Tetanus Protection** shield enchantment reduces infection risk from Tetanus I by 65% and Tetanus II by 40%.

Use a **Field Splint** during Tetanus to stop the current movement spasm and reduce further spasms for **90 seconds**. It works during Lockjaw. Tetanus Antitoxin cures the infection.

## Infected creatures

Tetanized mobs have rusty skins, increased strength, and extra salvage drops. Tetanized animals move slower and drop only **Infected Meat**. An ordinary animal with temporary Tetanus keeps its normal drops. Saddles, equipped items, and stored cargo still follow vanilla rules.

Default permanent Tetanized spawn chances are **0.5% / 1% / 2%** on Easy / Normal / Hard. Mob spawners use **3% / 5% / 8%**. Bosses use their own rituals. Wild variants retain their species' normal spawning, despawning, and mob-cap rules.

**Rabid creatures are a separate variant.** Look for a sickly purple tint and aggressive behavior among wolves, foxes, bats, livestock, zombies, husks, drowned, and other eligible mammals and undead in their usual habitats. Their default natural-spawn roll is **0.3% / 0.6% / 1%** on Easy / Normal / Hard. Rusty attacks do not make an animal rabid. Only rabid animals drop **Rabid Meat**; raw meat can transmit Rabies, while cooking makes it safe.

Species-colored variant eggs have their own **Rust & Ruins Spawn Eggs** Creative tab. A **Veterinary Kit** can cure a permanently tetanized or rabid animal and restore its normal appearance, behavior, and drops.

## Visit the medic

Find a medic in a village clinic, or place a **Medicine Cabinet** to give an unemployed villager the job. Each rank adds one randomized treatment and one supply or barter offer. Village medics can stock honeycomb, brewing supplies, or rare medical provisions. Cave medics can buy Rust or offer equipment like lanterns, Wire Brushes, and Trap Reset Tools. Higher ranks unlock additional treatments. Hero of the Village discounts apply.

Treatment trades require emeralds **and one Medical Prescription**. The **Prescription Writer** is the only survival source for prescriptions. Use **one paper + one ink sac** in the Writer. Craft the table, find it in a newly generated clinic or cave camp, or buy the table from a medic who stocks it. Village medicine offers have **2–4 purchases per delivery** and restock at most once per in-game day.

Use a **Clinic Notice Board** to choose a case and hover over it to preview both objectives. Boards are craftable and appear in newly generated clinics and cave camps. You can also accept a rotating case by sneaking and right-clicking a medic. Bring requested items in your **main hand** and sneak-interact, using partial stacks if needed. After fieldwork, report back to any medic of the case's kind. Use **`/mediccase`** to check your objective or **`/mediccase abandon`** to drop the case. Complete one case per in-game day for emeralds, supplies, a discount with that medic, and **Special Delivery**.

New villages have a **55% clinic roll** where terrain and space allow. Clinics and uniforms match plains, desert, savanna, snowy, and taiga villages. Buildings have treatment beds, a two-block stainless-steel and glass cabinet, and a supply chest.

Explore roomy, dry Overworld caves for rare **medic camps** with a fenced, lit courtyard, two beds, a field medic, a support farmer and small irrigated farm, a supply chest, and a persistent Tetanized Iron Golem. Field medics have at most **two purchases per offer** and restock no more often than once every three in-game days. Camps stay more than **2,000 blocks apart**. Clinics and camps generate in new terrain; existing chunks are not retrofitted.

Operators can use `/locate structure rustandruin:medic_clinic`, `/locate structure rustandruin:medic_tent`, or `/rustandruin locate medic` and `/rustandruin locate tent`. Branded results include the entrance height. Use structure searches for distant camps; `/locate poi rustandruin:medic_tent` only searches nearby workstations.

## Brew it or shoot it

Use a glass bottle on a cow to collect **Cow Manure**, then brew it with water. The base potion causes five seconds of nausea.

| Base potion | Ingredient | Result |
| --- | --- | --- |
| Cow Manure | Rust | Tetanus |
| Cow Manure | Infected Meat | Salmonella |
| Cow Manure | Rabid Meat | Rabies |
| Tetanus | Pufferfish | Lockjaw Sonata |

Add gunpowder for splash potions or dragon's breath for lingering versions.

Make **Tetanized Arrows** at a smithing table: leave the template slot empty, use ordinary arrows as the base, and add one Rust. One Rust coats up to 64 arrows. They cause one minute of Tetanus and are consumed even with Infinity.

**Tetanus I and II** work on combat weapons, including swords, axes, spears, rebar, pipes, bows, crossbows, and tridents. Nails and hooklines cannot receive them. Enchanted ranged hits apply full-duration infection; combining them with Tetanized Arrows extends it by 25%. Tetanus II adds disorientation, erratic mob movement, missed attacks, and periodic nonlethal damage. The shot keeps its enchantment even if firing it breaks the bow or crossbow, including every projectile in a Multishot volley.

## A tougher Wither

Build the usual Wither structure using four **Tetanized Soul Sand** blocks for a **75%** chance of the special variant. Replacing the upper center block with a netherite block raises the default chance to **100%**. Ritual blocks are consumed, including the netherite upgrade.

The **Tetanized Wither** has **650 health**, a custom appearance and boss bar, and summons tiny Tetanized Wither Skeleton minions. The first wave arrives **10-15 seconds after its spawn shield ends** while a player is nearby, with later waves every random **30-60 seconds**. Waves contain up to two **20-health** minions, with no more than six alive per Wither. They carry iron swords and can cause Wither and Tetanus. Nearby Creative observers can see the summons too.

The Wither deals **double damage** with its boss-owned attacks, including skull hits and blasts. Its skulls inflict **Wither III** on Normal and Hard. **Rustquake** hits a marked orange circle and launches nearby players upward. **Skull Barrage** fires three skulls at the marked spot. At half health, the Wither enrages and the wait after each special attack drops from **15 to 9 seconds**.

Defeating it drops native **50 XP**, one **Tetanized Wither Star**, bonus loot, and exactly **one** Tetanus book: **50% Tetanus II, otherwise Tetanus I**.

Wire-brush the special star if you need an ordinary Nether Star; the boss does not drop both.

## Forge a second End fight

Craft a **Tetanus Wither Forging Table**:

```text
Netherite Block  Eye of Ender          Netherite Block
Blaze Powder     Tetanized Wither Star Blaze Powder
Netherite Block  Anvil                 Netherite Block
```

At the table, combine **one End Crystal + one Rust + one netherite ingot** into one **Tetanized End Crystal**. Make four and place them in the ordinary dragon-respawn positions around the End exit portal. **All four must be tetanized**; mixed crystals summon an ordinary dragon. A special ritual also converts the ten pillar-top healing crystals. Their explosions can spread Tetanus only when the blast actually damages a creature.

The **Tetanized Ender Dragon** has **1,100 health**, a rust-and-purple boss bar, and attacks that spread Tetanus. A close pass has a **15%** chance to grab a nearby Survival player, lift them **ten blocks**, and drop them. A successful grab starts a **120-second cooldown**. Its native dragon XP sequence is preserved.

The dragon deals **double damage** with its boss-owned hits, including bites, wing hits, and breath. **Rustfire** leaves a damaging breath pool for **seven seconds**. **Wingstorm** hits a wider circle and knocks players outward. Both give a sound cue and a purple ring before they land. At half health, the dragon enrages and the wait after each special attack drops from **18 to 11 seconds**.

With mob loot enabled, every special-dragon defeat drops one placeable **Tetanized Dragon Egg** and one **Tetanus Overload Sword**, including repeat fights. These are shared boss drops, placed on solid ground outside the exit fountain. They glow and do not despawn. The egg keeps the vanilla black shape with small rust chips, and a slower, reverberant victory cue plays nearby.

The sword has **12 attack damage**, **2.3 attack speed**, **4,062 durability**, and **Tetanus II** already applied. Add compatible sword enchantments and repair it with **netherite blocks at an anvil** or Mending. Netherite ingots do not repair it.

## Power the Tetanized Beacon

Craft a **Tetanized Beacon** with a Tetanized Wither Star in the normal beacon recipe. Build its pyramid entirely from **Rusted Iron Blocks**, made with an iron block and four Rust, and leave a clear path for the beam.

The steel-panel menu separates the primary effect from its **II / III / IV** level controls and displays the required payment. Pay with a **netherite ingot** to select level-II primary powers. A full four-layer pyramid can add Regeneration II or upgrade the selected primary to level III. To choose **level IV**, add a **Tetanized Wither Star** in the extra payment slot as well. Every confirmed IV selection consumes a new star and an ingot.

An active beacon automatically grants **Tetanus Resistance**, independently of your chosen power.

## Advancements for questionable decisions

Press **L** by default and select **Rust & Ruin**. The tab appears after you join a world. Drag or scroll to move around, hold **Shift** while scrolling to move sideways, and press **Home** to return to the start when Home is available. Hover an icon for its goal.

| Advancement | How to earn it |
| --- | --- |
| **Rusty Business** | Drop a Nail into water or exposed rain and let it turn into a Rusted Nail. |
| **Hands Off** | Wear Protective Gloves and have them successfully prevent a Tetanus exposure. |
| **Not Today, maybe tomorrow** | Raise a shield with Tetanus Protection and block a Tetanus-inflicting attack. |
| **A Dose of Common Sense** | Drink Tetanus Antitoxin while Tetanus or Lockjaw Sonata is active. An early cure counts. |
| **Special Delivery** | Sneak and right-click a medic, complete both case objectives, and report back if the final step was fieldwork. |
| **Why did I do that?** | Throw a Rusted Nail upward and die to that same nail when it comes back down. |
| **Forged in Infection** | Take a Tetanized End Crystal from the Tetanus Wither Forging Table. |
| **Tetanus, Again?** | Be in the End within 256 horizontal blocks of the center portal when four Tetanized End Crystals summon the dragon. |
| **The End of Tetanus?** | Earn this when the Tetanized Dragon dies after you deal at least 20 points of damage to its health. |
| **Rust Never Sleeps** | Confirm a power on a Tetanized Beacon over a full four-layer Rusted Iron Block pyramid. |

See every milestone and its unlock steps in the [complete advancement guide](https://github.com/vipgabe09267/rust-and-ruin/blob/main/docs/ADVANCEMENTS.md).

## Requirements

**Minecraft Java 1.20.1 | Fabric Loader 0.16.14+ | Fabric API | Java 17**

Install on **both client and server**. **JEI and REI are optional alternatives**, with crafting, cooking, brewing, rusting, coating, arrow smithing, prescription writing, and crystal-forging displays. Install your chosen viewer's dependencies too. Rust & Ruin itself requires no additional mod beyond Fabric API.

The reusable **Field Guide** explains recipes, diseases, and progression in-game. Server owners can configure mutation rates, disease duration, prevention, rust timing, sound volume, and other behavior.

[Source code and recipe guide](https://github.com/vipgabe09267/rust-and-ruin) | [Modrinth downloads](https://modrinth.com/mod/rust-ruin)
