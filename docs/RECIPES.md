# Crafting and brewing guide

All recipes are for Rust & Ruin **0.1.1-beta**, Minecraft 1.20.1 Fabric. JEI and REI show crafting, cooking, brewing, rusting, wax coating, arrow smithing, prescription writing, and crystal forging. Crafting recipes unlock when iron is acquired.

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
| 4 Tetanized Soul Sand blocks | 75% |
| 3 Tetanized Soul Sand blocks, with a netherite block replacing the upper center soul sand | 100% by default |

The netherite block adds **25 percentage points** to the configured 75% base chance. A failed mutation roll still summons a normal Wither. Both lower corner spaces must be empty. All structure blocks, including the netherite upgrade, are consumed.

A Tetanized Wither has **650 health**. It drops one **Tetanized Wither Star**, native **50 XP**, bonus loot, and exactly one enchanted book: **50% Tetanus II, otherwise Tetanus I**. Its first wave arrives 10-15 seconds after the spawn shield ends while a player is nearby; later waves use random 30-60-second intervals. Waves contain at most two **20-health** Tetanized Wither Skeletons, with a six-live-minion cap. Creative observers can see these waves too. Wire-brushing the special star converts it into an ordinary Nether Star; keep it tetanized for a special beacon, crystal-forging table, or level-IV beacon payment.

## Tetanized Beacon

Use the ordinary beacon recipe, replacing the Nether Star with a **Tetanized Wither Star**:

```text
Glass    Glass             Glass
Glass    Tetanized Star    Glass
Obsidian Obsidian          Obsidian
```

Build the pyramid entirely from **Rusted Iron Blocks**. Craft each with one iron block and four Rust in a cross. All previous base materials are retired for the special beacon.

The layer sizes are the same as a normal beacon: 3x3, then 5x5, 7x7, and 9x9 for the full pyramid. The beam needs the usual clear path upward.

- Confirming an ordinary power selection consumes **one netherite ingot**. Level IV additionally requires **one Tetanized Wither Star** in its dedicated payment slot.
- Primary powers start at **level II**. Selection still requires the normal pyramid tier: Speed/Haste at one layer, Resistance/Jump Boost at two, Strength at three. The server rejects invalid selections without consuming payment.
- A full pyramid can add **Regeneration II** as the secondary power.
- Selecting the primary power again as the full-pyramid upgrade gives **level III** of that power instead.
- Use the menu's separate **II / III / IV** controls to choose primary strength. A full pyramid can upgrade the selected primary to **level IV** with a netherite ingot **plus a Tetanized Wither Star**. Both are consumed on confirmation; every subsequent IV selection requires another star. Invalid selections consume neither payment.
- Range and duration refresh follow normal beacon behavior. Changes to pyramid blocks or beam obstruction update on the normal beacon cycle.

## Rusted iron golem

Right-click an iron golem with Rust. Each Survival attempt consumes one Rust and has an **18% chance** of conversion. A tetanized iron golem has **200 maximum health**, keeps its mutation when saved, and uses a rusty version of its original texture. Success awards **Tetanized Guardian** and **Tetanized Mob**. Existing tetanized golems cannot consume repeated Rust attempts. Rusty projectile hits also use a dedicated **18%** permanent golem-conversion roll by default, configurable with `golemConversionChance`; the general mob rate remains unchanged. Setting the general mob conversion rate to zero also disables projectile conversion of golems.
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
| Medicine Cabinet | `IGI` / `IBI` / `IGI` | I = iron ingot, G = glass pane, B = barrel |
| Rabies Vaccine | Shapeless | Tetanus Vaccine + Rabid Meat + golden carrot; ingredient bottles are returned |

Rust the spear, bear trap, or barbed wire by dropping the item in water/rain. Placed bear traps and barbed wire also weather. A closed trap retains its victim when it rusts; resetting releases the victim and gives three seconds to move clear before rearming. Wire-brushing cleans their item forms and returns Rust. Existing rusty iron tools remain usable and cleanable but are retired from new acquisition.

For smithing arrows, **leave the template empty**. One Rust converts the arrow stack, including a smaller stack than 64. Shift-click transfers only what fits and consumes the corresponding arrows plus one Rust; a full inventory consumes nothing.

A Tetanized Arrow alone applies one minute of Tetanus. Ordinary arrows from a Tetanus-enchanted bow or crossbow apply full duration. Combining infected arrows with the enchantment extends that duration by 25%. Tetanus II adds stronger symptoms for the infection duration, including the combination bonus. Milk cannot strip those symptoms while Tetanus II remains; antitoxin clears them. Nails and hooklines cannot accept either Tetanus enchantment.

Rabies and Lockjaw also support ordinary gunpowder splash and dragon's breath lingering conversions. Drinkable Rabies is permanent; splash/cloud Rabies retains that duration. Lockjaw Potion lasts sixty seconds before ordinary splash/cloud scaling. Antitoxin cures standalone Lockjaw, and milk cannot.

Only rabid animals drop Rabid Meat. Cook it in a furnace (10 seconds), smoker (5 seconds), or campfire (30 seconds). Cooked Rabid Meat is safe. Rabies Cure is purchased from a medic; Rabies Vaccine is both craftable and traded.

## Village trades

Prices below are base prices before reputation, demand, discounts, and curing effects.

| Villager | Level | Added offers |
| --- | --- | --- |
| Medic | Nurse | Antitoxin or Tetanus Vaccine, plus a supply/barter offer. A Writer or emergency prescription can appear. Medicine stock 2-4. |
| Medic | Nurse Practitioner | Two more offers; Rabies Cure joins the treatment pool. Supplies can include a Writer, emergency prescription, brewing equipment, or barter. |
| Medic | Medical Doctor | Two more offers; treatment, veterinary kits, shield protection books, golden apples, diamonds or stronger brewing ingredients. |
| Medic | Attending Physician | Two final offers; stronger supplies and a possible enchanted golden apple for 48 emeralds plus 2 diamonds, one per delivery. |
| Weaponsmith | Apprentice | Iron Spear 7; Rusted Spear 9; Rebar 3; Rusted Rebar 4 emeralds |
| Toolsmith | Apprentice | Wire Brush or Trap Reset Tool, 4 emeralds |
| Armorer | Apprentice | Bear Trap 6 or Rusted Bear Trap 7 emeralds |
| Fisherman | Apprentice | Rusty Fishing Hook 3 emeralds |

Each medic rank adds one randomized treatment and one randomized supply/barter offer; higher ranks unlock stronger options. Repeated medicines, prescription stock, and writers do not stack across ranks. Treatments cost emeralds **and one Medical Prescription**, made from paper and an ink sac at a Prescription Writer. Hero and gossip discounts apply to the native emerald price; they do not remove the prescription requirement. Village stock replenishes at most once per 24,000 world ticks; field medics have at most two uses per offer and a 72,000-tick delivery interval. Clinics use a 55% roll on eligible village-grid sites with suitable terrain. Placing a Medicine Cabinet lets an unemployed villager acquire the medic profession outside a clinic.

## Shield enchantment and beacon updates

Apply **Tetanus Protection** to a shield at an anvil. While held, it reduces infection risk from Tetanus I by 65% or Tetanus II by 40%, stacking with other applicable prevention. An actual shield block prevents the attack itself. Tetanus I/II can be applied to swords, axes, spears, industrial melee weapons, bows, crossbows, and tridents.

The active Tetanized Beacon grants Tetanus Resistance automatically, independent of the selected effect. Its normal II powers and full-pyramid III upgrade use netherite payments; the full-pyramid IV option additionally consumes a Tetanized Wither Star for each confirmed selection. It has separate placement/full-activation advancements and does not complete vanilla beacon advancements.
## Veterinary care and wax coating

Veterinary Kit, crafting table:

```text
Leather        Golden Apple   Leather
Antitoxin      Rabies Cure    Antitoxin
(empty)        String         (empty)
```

Produces one single-use kit. Right-click a permanently tetanized or rabid animal to cure its mutation and earn **Clean Bill of Health**. Healthy animals, hostile mobs, players, spectators, and empty kits cannot consume treatment. Existing names, ownership, and vanilla persistence rules remain.

Waxing uses no crafting station: hold honeycomb in one hand and clean, supported metal in the other, then use the honeycomb. Cost: one honeycomb per item in the metal stack. Protection: 12000 wet-weather ticks by default, plus 20 ticks spent per point of durability use. Only dropped equipment weathers; coatings do not transfer to placed traps. View both inputs and the coated output in JEI/REI's Wax coating category.

## Medic supply requests and workshop salvage

Each medic rotates among **12 glass bottles, 3 honey bottles, 8 glow berries, and 8 white wool**, offset by its UUID. Sneak and right-click to see the requested item, exact amount, and the number held. To complete it, hold at least that amount in your **main hand**, then sneak and right-click the same medic. This earns **Special Delivery**; no merchant trade is required. One delivery is accepted per medic per 24000 world ticks, shared between players. Honey bottles return their glass bottles. The reward is 30 minor-positive reputation with that medic, used by vanilla trade discounts and decay. Discounts do not refresh daily stock or bypass ordinary trade limits.

Abandoned workshop barrels roll 3-5 rewards from Rust, copper, string, coal, rusty nails, a worn rusty rebar, or rare antitoxin/veterinary kits. Naturally placed workshops use a 32-chunk random-spread grid with 16-chunk separation in eight temperate Overworld biomes. Like other structures, exploration availability depends on seed and previously generated chunks.

## Current disease and medic behavior

Raw Infected Meat causes Salmonella only (100% by default, configurable); raw Rabid Meat causes Rabies and immediate nausea unless a Rabies vaccine prevents infection. Cooking either is safe. Temporary Tetanus never changes animal drops. Rabies itself deals no damage and will not reduce maximum health below two hearts.

The stainless-steel Medicine Cabinet uses six iron ingots, two glass panes and a barrel. The placed block is two blocks tall; leave headroom. Right-click a legacy one-block cabinet to upgrade it if the space above is empty. Both halves form one villager job site and breaking either returns one cabinet.

Gloves still have 192 durability; Unbreaking reduces wear, and Mending repairs them from XP while in the dedicated slot. A Veterinary Kit now produces treatment particles and a sound.

Village clinic chests roll 3-5 ordinary supplies: apples, bread, torches, paper, iron, emeralds, or occasional golden apples. Cave camps use the same modest supply pool. Their field medic has scarce two-use supplies and a three-day delivery interval; the persistent 200-HP golem defends a fenced, lit courtyard. Camps also have two beds and a support farmer with an irrigated crop patch. Locate nearby generated tents with `/locate poi rustandruin:medic_tent`, or search their structure grid with `/locate structure rustandruin:medic_tent`. The POI search is limited to 128 blocks; branded clinic and tent structure results also show the exact entrance, including height.

Infection announcements are positional and limited to nearby listeners (16 blocks). Existing infections, natural mutations, chunk loading and inherited mutations stay quiet. Medics have their own short vial-and-supplies work sound, audible within 12 blocks.

## Prescription Writer

Craft the workstation in an ordinary crafting table:

```text
Iron Ingot    Feather       Iron Ingot
Planks        Book          Planks
Iron Ingot    Copper Ingot  Iron Ingot
```

Any planks accepted by the recipe tag work. Place the block and right-click it. Put **one paper** in the paper slot and **one ink sac** in the ink slot; taking the output consumes both and gives **one Medical Prescription**. A glow ink sac is not a substitute. Shift-click batches only complete prescriptions that fit in the inventory. Closing the menu returns unused input items; it does not grant its untaken output preview.

Writers are also placed in newly generated village clinics and cave camps, and can appear in lower-rank medic trades: 10 emeralds at Nurse or 8 at Nurse Practitioner, one per delivery. The menu labels its paper, ink, and prescription slots. Bring the finished prescription and listed emerald payment to a medic for treatment. An emergency prescription costs 3 emeralds and has at most two purchases per village restock, or one for a field medic. This is separate from daily supply deliveries; the writer does not assign a villager profession or restock medicines.

## Tetanus Wither Forging Table and End crystals

The crafting-table recipe follows this exact layout:

```text
Netherite Block  Eye of Ender          Netherite Block
Blaze Powder     Tetanized Wither Star Blaze Powder
Netherite Block  Anvil                 Netherite Block
```

Use an **anvil**, not a chipped/damaged anvil, and a **Tetanized Wither Star**, not an ordinary Nether Star. Place the table, then combine these inputs in its menu:

| Input slot | Amount |
| --- | --- |
| End Crystal | 1 |
| Rust | 1 |
| Netherite Ingot | 1 |
| Output | 1 Tetanized End Crystal |

Inputs are consumed when the finished crystal is taken. Shift-click can forge several crystals if every ingredient and enough output space are present. Closing the table returns unused inputs. JEI and REI have a dedicated crystal-forging display.

Defeat the ordinary dragon first. At the End exit portal, place a Tetanized End Crystal in each of the four normal dragon-respawn positions. **All four crystals must be special**: mixing ordinary and tetanized crystals produces an ordinary dragon. The native respawn consumes the crystals. Exploding a special crystal is not an alternative ritual.

A special ritual converts all ten pillar-top healing crystals to Tetanized End Crystals; mixed rituals leave ordinary pillars. Only creatures that take actual damage from a special crystal explosion can receive Tetanus from it. The Tetanized Ender Dragon has **1,100 health**, a custom rust-and-purple boss bar, and a **15%** grab roll on a close pass. It lifts a caught Survival player **ten blocks** before dropping them, then waits **120 seconds** before another successful grab. The ordinary first-fight/respawn XP rules remain in place. With mob loot enabled, each special-dragon defeat drops a **Tetanized Dragon Egg** and the **Tetanus Overload Sword**; these rewards remain available on repeated special fights. They land on solid ground outside the exit fountain, glow, and do not despawn. The egg retains the vanilla black shape with rust chips. A slowed, reverberant victory cue plays within 32 blocks.

The sword's default stack already has **Tetanus II**. Its total attack damage is **12**, attack speed **2.3**, and durability **4,062**. It supports compatible sword enchantments. Anvil repairs use **netherite blocks**, never ingots; Mending also works.

Forging a special crystal earns its progression advancement. The special summon awards **Tetanus, Again?** to nonspectating players within 256 horizontal blocks of the central End portal. A player must contribute at least **20 points of actual health damage** to qualify for **The End of Tetanus?** at the special dragon's death.

## Finding rabid variants

Rabies is a separate permanent variant, not a stage of Tetanus. Explore the usual habitats of eligible mammals and undead on a non-Peaceful difficulty and look for the purple, sickly tint and aggressive behavior. On each eligible natural spawn the default Rabid roll is **0.3% / 0.6% / 1%** on Easy / Normal / Hard; finding one can take exploration. Existing healthy creatures do not reroll continuously, and rusty attacks do not turn them rabid.

Only **rabid animals** drop Rabid Meat. Cook it before eating, or use it as the brewing ingredient for Rabies and the craftable Rabies Vaccine. Raw Rabid Meat causes Rabies and immediate nausea unless vaccine prevention succeeds. For Creative testing, choose a purple-tinted Rabid spawn egg from the **Rust & Ruins Spawn Eggs** tab; the tetanizing operator command does not create Rabid variants.