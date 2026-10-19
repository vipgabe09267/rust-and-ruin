# Crafting and brewing guide

All recipes are for Rust & Ruin **0.1.0-beta**, Minecraft 1.20.1 Fabric. JEI and REI show crafting, cooking, brewing, and the custom Rusting category. Crafting recipes unlock when iron is acquired.

## Weapons and equipment

| Result | Ingredients | Layout |
| --- | --- | --- |
| Tetanus Vaccine | Tetanus Antitoxin + golden apple + glowstone dust + honey bottle | Shapeless; returns both glass bottles |
| Protective Gloves | 2 leather + 2 iron nuggets + copper ingot + string | Leather in upper corners; nugget/copper/nugget across middle; string below center |
| Field Guide | Book + Rust + feather | Shapeless; right-click to read |
| 4 Nails | 2 iron ingots + flint | Two vertical ingots; flint right of the upper ingot |
| Rebar | 2 iron ingots + chain | Iron top left, chain center, iron bottom right |
| Sharpened Rebar | Rebar + flint + amethyst shard | Flint upper right; rebar lower left, amethyst lower right |
| Bent Nail | Nail + andesite | Shapeless |
| Metal Pipe | 2 iron ingots + copper ingot | Iron, copper, iron across one row |
| 4 Caltrops | 4 nails + iron nugget | Shapeless; place anywhere in the crafting grid |
| Wire Brush | 3 iron nuggets + copper ingot + stick | Nuggets across top; copper center, stick below |
| Tetanus Antitoxin | Honey bottle + golden carrot + fermented spider eye + glass bottle | First three across top; glass bottle below the carrot |
| Up to 64 Tetanized Arrows | One Rust + 1–64 ordinary arrows | Smithing table: empty template; arrows in base; Rust in addition |
| Rusty Hookline | Rusty Fishing Hook + chain + string + stick | Shapeless |
| Tetanized Soul Sand | Rust + soul sand + iron ingot | Shapeless; produces one block |

The hookline works up to 12 blocks away. Right-click while aiming at a creature to pull it toward you and inflict Tetanus on a successful hit. It costs one durability and has a two-second cooldown. Solid blocks obstruct the hookline.

## Collecting and salvaging

- **Rust:** wire-brush rusty equipment. One cleaning action produces one Rust, even when cleaning a stack. Mutated hostile mobs also drop it.
- **Rusty Fishing Hook:** leave a dropped fishing rod touching water for five consecutive seconds. It breaks into two sticks and two string, with a 15% hook chance.
- **Cow Manure:** use a glass bottle on a cow. This consumes the bottle and gives a bottled manure ingredient. Each cow has a one-minute collection cooldown.
- **Infected Meat:** permanently tetanized animal variants drop only 1–3 infected meat. Collecting it unlocks Strange Meat. A furnace, smoker, or campfire makes it safe to eat.

## Brewing

Use a standard brewing stand with blaze powder fuel. Each ingredient can process up to three bottles in the usual brewing cycle.

| Input bottle | Ingredient | Output |
| --- | --- | --- |
| Water Bottle | Cow Manure | Cow Manure Potion |
| Cow Manure Potion | Rust | Potion of Tetanus |
| Cow Manure Potion | Infected Meat | Potion of Salmonella |
| Cow Manure Potion | Rabid Meat | Potion of Rabies |
| Potion of Tetanus | Pufferfish | Potion of Lockjaw Sonata |
| Potion of Tetanus | Gunpowder | Splash Potion of Tetanus |
| Potion of Salmonella | Gunpowder | Splash Potion of Salmonella |

Tetanus potion tooltips show the configured duration range. Drinking or receiving a disease splash/cloud rolls a **5–10 minute infection**. The final **2–4 minutes** are continuous Lockjaw Sonata. Salmonella lasts one minute before vanilla splash-distance scaling. Cow Manure Potion causes five seconds of nausea and is a brewing base. Vanilla splash and lingering conversions remain available. Milk clears other ordinary effects but preserves Tetanus and Lockjaw; Tetanus Antitoxin cures both immediately and returns a glass bottle.

## Tetanized Wither rituals

Use the usual T-shaped structure with three Wither Skeleton Skulls on top.

| Structure | Mutation chance |
| --- | --- |
| 4 ordinary soul sand/soul soil blocks | 0%; summons an ordinary Wither |
| 4 Tetanized Soul Sand blocks | 5% |
| 3 Tetanized Soul Sand blocks, with a netherite block replacing the upper center soul sand | 30% by default |

The netherite block adds **25 percentage points** to the configured 5% base chance. A failed mutation roll still summons a normal Wither. Both lower corner spaces must be empty. All structure blocks, including the netherite upgrade, are consumed.

A Tetanized Wither has 600 health. It drops its ordinary Nether Star, a Tetanized Wither Star, guaranteed Tetanus I, a 50% additional chance of Tetanus II, experience bottles, and one randomized bonus reward. Wire-brushing the special star converts it into an ordinary Nether Star; keep it infected if you want the special beacon.

## Tetanized Beacon

Use the ordinary beacon recipe, replacing the Nether Star with a **Tetanized Wither Star**:

```text
Glass    Glass             Glass
Glass    Tetanized Star    Glass
Obsidian Obsidian          Obsidian
```

Build the pyramid entirely from **Rusted Iron Blocks**. Craft each with one iron block and four Rust in a cross. All previous base materials are retired for the special beacon.

The layer sizes are the same as a normal beacon: 3x3, then 5x5, 7x7, and 9x9 for the full pyramid. The beam needs the usual clear path upward.

- Payments accept **only netherite ingots**. Confirming a power selection consumes one ingot, as with a normal beacon payment.
- Primary powers start at **level II**. Selection still requires the normal pyramid tier: Speed/Haste at one layer, Resistance/Jump Boost at two, Strength at three. The server rejects invalid selections without consuming payment.
- A full pyramid can add **Regeneration II** as the secondary power.
- Selecting the primary power again as the full-pyramid upgrade gives **level III** of that power instead.
- Range and duration refresh follow normal beacon behavior. Changes to pyramid blocks or beam obstruction update on the normal beacon cycle.

## Rusted iron golem

Right-click an iron golem with Rust. Each Survival attempt consumes one Rust and has an **18% chance** of conversion. A tetanized iron golem has **200 maximum health**, keeps its mutation when saved, and uses a rusty version of its original texture. Success awards **Rusty Guardian** and **Tetanized Mob**. Existing tetanized golems cannot consume repeated Rust attempts.
## Prevention

Drink a vaccine for 20 minutes of Tetanus Resistance: 80% lower Tetanus risk by default. It does not cure an existing infection or prevent Salmonella. Drinking a dose consumes one and returns one bottle; Creative use does not create extra bottles. Milk removes resistance while preserving Tetanus and Lockjaw.

Gloves must be equipped in the dedicated slot above the offhand. Right-click or shift-click to equip them. They reduce rusty-contact infection risk by 50% and have 192 durability, charging one base durability per exposure. Gloves carried in ordinary inventory or hands do not protect, and gloves do not stop contaminated food or disease potions. Combined vaccine and glove protection is 90% by default; weapon damage is unchanged.

The guide is reusable and has ten scrollable chapters. It shows the connected server's prevention settings. Configuration keys: `vaccineDurationSeconds` (default 1200), `vaccineProtectionChance` (0.8) and `gloveProtectionChance` (0.5).

## Expansion crafting layouts

Each layout uses a 3x3 crafting grid. `_` means an empty space.

| Result | Rows, top to bottom | Key |
| --- | --- | --- |
| Iron Spear | `_II` / `_CI` / `S__` | I = iron ingot, C = chain, S = stick |
| Rusted Iron Block | `_R_` / `RIR` / `_R_` | R = Rust, I = iron block |
| Bear Trap | `INI` / `IPI` / `_C_` | I = iron ingot, N = nail, P = heavy weighted pressure plate, C = chain |
| Trap Reset Tool | `I_I` / `ICI` / `_S_` | I = iron nugget, C = copper ingot, S = stick |
| 2 Barbed Wire | `NNN` / `CSC` / `NNN` | N = nail, C = chain, S = string |
| Medicine Cabinet | `PPP` / `HBF` / `PPP` | P = birch planks, H = honey bottle, B = barrel, F = fermented spider eye |
| Rabies Vaccine | Shapeless | Tetanus Vaccine + Rabid Meat + golden carrot; ingredient bottles are returned |

Rust the spear, bear trap, or barbed wire by dropping the item in water/rain. Placed bear traps and barbed wire also weather. A closed trap retains its victim when it rusts; resetting releases the victim and rearms it. Wire-brushing cleans their item forms and returns Rust. Existing rusty iron tools remain usable and cleanable but are retired from new acquisition.

For smithing arrows, **leave the template empty**. One Rust converts the arrow stack, including a smaller stack than 64. Shift-click transfers only what fits and consumes the corresponding arrows plus one Rust; a full inventory consumes nothing.

A Tetanized Arrow alone applies one minute of Tetanus. Ordinary arrows from a Tetanus-enchanted bow or crossbow apply full duration. Combining infected arrows with the enchantment extends that duration by 25%. Tetanus II adds stronger symptoms for the infection duration, including the combination bonus. Milk cannot strip those symptoms while Tetanus II remains; antitoxin clears them. Nails and hooklines cannot accept either Tetanus enchantment.

Rabies and Lockjaw also support ordinary gunpowder splash and dragon's breath lingering conversions. Drinkable Rabies is permanent; splash/cloud Rabies retains that duration. Lockjaw Potion lasts sixty seconds before ordinary splash/cloud scaling. Antitoxin cures standalone Lockjaw, and milk cannot.

Only rabid animals drop Rabid Meat. Cook it in a furnace (10 seconds), smoker (5 seconds), or campfire (30 seconds). Cooked Rabid Meat is safe. Rabies Cure is purchased from a medic; Rabies Vaccine is both craftable and traded.

## Village trades

Prices below are base prices before reputation, demand, discounts, and curing effects.

| Villager | Level | Added offers |
| --- | --- | --- |
| Medic | Novice | Antitoxin 6 emeralds; Tetanus Vaccine 8; Gloves 6; Rabies Cure 10; Rabies Vaccine 12. All five are available. |
| Medic | Apprentice | Medicine Cabinet 5 emeralds; buys 3 honey bottles for 1 emerald |
| Medic | Journeyman | Tetanus Protection shield book 16 emeralds |
| Weaponsmith | Apprentice | Iron Spear 7; Rusted Spear 9; Rebar 3; Rusted Rebar 4 emeralds |
| Toolsmith | Apprentice | Wire Brush or Trap Reset Tool, 4 emeralds |
| Armorer | Apprentice | Bear Trap 6 or Rusted Bear Trap 7 emeralds |
| Fisherman | Apprentice | Rusty Fishing Hook 3 emeralds |

Non-novice offers participate in ordinary random trade selection. Clinics are weighted additions to village house pools, not guaranteed buildings. Placing a Medicine Cabinet lets an unemployed villager acquire the medic profession outside a clinic.

## Shield enchantment and beacon updates

Apply **Tetanus Protection** to a shield at an anvil. While held, it reduces infection risk from Tetanus I by 65% or Tetanus II by 40%, stacking with other applicable prevention. An actual shield block prevents the attack itself. Tetanus I/II can be applied to swords, axes, spears, industrial melee weapons, bows, crossbows, and tridents.

The active Tetanized Beacon grants Tetanus Resistance automatically, independent of the selected effect. Its normal II powers and full-pyramid III upgrade use netherite payments. It has separate placement/full-activation advancements and does not complete vanilla beacon advancements.