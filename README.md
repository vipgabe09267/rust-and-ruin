<p align="center"><img src="docs/branding/logo.png" width="200" alt="Rust & Ruin logo"></p>

# Rust & Ruin

Rusting weapons, infected creatures, and a Wither worth preparing for. A Minecraft **1.20.1 Fabric** mod, version **0.1.0-beta**.

[Download on Modrinth](https://modrinth.com/mod/rust-ruin) · [Recipes and progression](docs/RECIPES.md) · [Test results](VALIDATION.md) · [Security review](docs/SECURITY_AUDIT.md) · [Requested feature audit](docs/REQUEST_AUDIT.md)

![Rust & Ruin in Minecraft](validation/screenshots/rust-and-ruin-hud.png)

## Install

Put `rust-and-ruin-0.1.0-beta.jar` in the `mods` folder on **both the client and server**. Requires **Java 17**, **Fabric Loader 0.16.14+**, and **Fabric API for 1.20.1**. Replace older copies of the mod.

**JEI and REI are optional alternatives.** Each supports crafting, cooking, brewing, and all 11 Rusting recipes. Install one recipe viewer with its own dependencies. Rust & Ruin itself only requires Fabric API.

## Prevention and field guide

**Tetanus Vaccine** grants Tetanus Resistance for **20 minutes**, reducing new Tetanus infection risk by **80%**. It prevents rather than cures: existing Tetanus and Lockjaw remain. It does not prevent Salmonella. Milk removes this beneficial protection.

Equip **Protective Gloves in your offhand** for **50% lower risk from rusty contact**. Each exposure costs one base durability out of 192; ordinary enchantment rules still apply. Gloves do not prevent infected-food or potion exposure and do not reduce weapon damage. Vaccine and gloves combine to give **90% contact protection** by default.

Craft a **Rust & Ruin Field Guide** from a book, Rust and a feather. Right-click to open its ten chapters, then use the mouse wheel or arrow buttons to scroll. It covers recipes, symptoms, prevention, creatures, salvage, brewing, commands and Wither/beacon progression. Prevention values use the connected server settings. Reading never consumes the book.

## Weapons, rust, and salvage

Throw nails, swing rebar and metal pipes, or place caltrops. A nail deals one heart per throw; rebar deals half a heart on a fully charged hit against an unarmored target. Armor, enchantments, critical hits, and attack cooldowns still apply.

Dropped metal rusts after **five consecutive seconds touching water, including the surface**, or **15 seconds exposed to rain**. The five iron tools have rusted variants. Rusting preserves names, durability, enchantments, and stack counts; inventories do not rust.

Rusted industrial weapons inflict Tetanus. Rusted iron tools have a 20% infection chance on melee hits. Tetanized arrows work with bows and crossbows and **consume ammunition even with Infinity**. Their infection sound plays at impact; caltrops play it at the trap.

Use a wire brush with rusty equipment in the opposite hand to clean it. Cleaning costs one brush durability per item and returns one Rust per action. A grindstone also removes rust, with normal enchantment removal rules.

Dropped fishing rods break after five seconds in water, returning two sticks and two string, with a **15% chance of a Rusty Fishing Hook**. Craft the hook into a **Rusty Hookline** to pull and infect creatures up to 12 blocks away.

## Infections and variants

Ordinary Tetanus lasts **5–10 minutes** per hit. Symptoms develop in stages: stiffness, spasms, and stronger movement disturbances. Player hearts turn rusty brown on the HUD and in tab-list health objectives.

**Lockjaw Sonata** has a visible status icon and timer. It continuously blocks eating during the final randomly selected **2–4 minutes** of the infection. An action-bar message explains the restriction. **Milk cannot cure Tetanus or Lockjaw Sonata; drink Tetanus Antitoxin.** Natural recovery clears both effects.

Rusty attacks (including melee, traps, projectiles, and hooklines) have a **15% mutation chance for animals** and **5% for other mobs**. All vanilla mob types can be tetanized, including spiders. Withers retain their separate ritual rules. Skins preserve each creature's own texture layout and details instead of applying a shared texture across unrelated models.

Permanent hostile variants gain 50% maximum health and additional attack damage where supported. Permanent animal variants move slower. **Any animal with Tetanus**, temporary or permanent, replaces its ordinary item drops with **only 1–3 Infected Meat**. Hostiles retain normal loot and add Rust plus a small random reward: iron nuggets, a copper ingot, flint, string, coal, or a bone.

Raw infected meat causes Tetanus and has a **10% chance of Salmonella**. Salmonella causes periodic vomiting, nausea, hunger loss, and light nonlethal damage. Cooking the meat makes it safe. Collect cow manure with a glass bottle and use it as the base for **Tetanus and Salmonella potions**, including splash variants.

## Golems, Withers, and beacons

Use Rust on an iron golem for an **18% chance** to create a rusty, **200-health** guardian. Each Survival attempt consumes one Rust.

Summon a Wither with four **Tetanized Soul Sand** blocks for a **5% chance** of a Tetanized Wither. Replacing the upper center block with netherite raises the default chance to **30%**. The variant has 600 health, a custom boss bar, bonus loot, Tetanus I, and a **Tetanized Wither Star**.

Use that special star in the normal beacon recipe to craft a **Tetanized Beacon** with cracked glass. It needs a gold/copper/netherite pyramid and accepts **only netherite ingot payments**. Powers start at level II; a full pyramid offers Regeneration II or a level III upgrade of the primary power. Wire-brush the special star if you prefer an ordinary Nether Star.

Exact crafting layouts, brewing ingredients, ritual diagrams, and beacon rules are in the **[recipe guide](docs/RECIPES.md)**.

## Advancements

The Rust & Ruin tab uses a rusty metal background.

- **A Painful Lesson:** contract Tetanus.
- **Rusty Business:** make a dropped nail rust.
- **Hook, Line and Tetanus:** recover a rusty fishing hook.
- **Lockjaw Sonata:** experience late-stage lockjaw.
- **Strange Meat:** collect Infected Meat.
- **Why did I eat that?:** contract Salmonella.
- **Tetanized Mob:** permanently convert a creature through a rusty attack.
- **Rusty Guardian:** convert an iron golem with Rust.
- **Tetanus. Nice One!:** defeat a Tetanized Wither.

## Commands and sounds

Operator permission level 2:

```mcfunction
/rustandruin summon minecraft:spider ~ ~ ~
/rustandruin summon minecraft:iron_golem ~ ~ ~
/rustandruin summon minecraft:wither ~ ~ ~
/rustandruin tetanize @e[type=minecraft:zombie,distance=..10]
```

Summon commands bypass random conversion chances. Existing mob AI and models remain in use.

```mcfunction
/playsound tetanusmod:tetanus_effect master @a ~ ~ ~ 1 1
/playsound tetanusmod:spasm master @a ~ ~ ~ 1 1
/playsound tetanusmod:vomit master @a ~ ~ ~ 1 1
```

## Configuration

Launch once to create `config/tetanusmod.json`. Stop the instance before editing it. Settings include infection duration, rust timing, damage, camera shaking, audio volume, mutation probabilities, vaccine duration, and both protection chances. Defaults: hostile 0.05, animal 0.15, special Wither 0.05, raw-meat Salmonella 0.10. The netherite ritual adds 0.25 to the Wither probability, capped at 1.

Previous default animal rates migrate to 15%, and the old default 120–300 second duration migrates to 300–600 seconds. Custom settings are retained. Internal IDs remain `tetanusmod` for save compatibility.

## Build and test

With JDK 17:

```powershell
.\tools\Build.ps1 -Test
# Or:
.\gradlew.bat build runGameTest --console plain
```

Linux/macOS: `./gradlew build runGameTest`. The release JAR is written to `build/libs/`.

Graphical test configurations are separate:

```powershell
.\gradlew.bat -PwithJei runClientSmoke --console plain
.\gradlew.bat -PwithRei runClientSmoke --console plain
```

See [VALIDATION.md](VALIDATION.md) for setup and results. Development test classes are excluded from the release JAR.

`tools/Generate-Resources.ps1` rebuilds data and imports retained artwork. [Source art and prompts](assets-source/art/PROMPTS.md) document the generated sprites; the supplied logo and audio are preserved.