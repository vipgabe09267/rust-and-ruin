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
| Tetanized Arrow | Rotten flesh + rusted nail + arrow | Across one row, in that order |
| Rusty Hookline | Rusty Fishing Hook + chain + string + stick | Shapeless |
| Tetanized Soul Sand | Rust + soul sand + iron ingot | Shapeless; produces one block |

The hookline works up to 12 blocks away. Right-click while aiming at a creature to pull it toward you and inflict Tetanus on a successful hit. It costs one durability and has a two-second cooldown. Solid blocks obstruct the hookline.

## Collecting and salvaging

- **Rust:** wire-brush rusty equipment. One cleaning action produces one Rust, even when cleaning a stack. Mutated hostile mobs also drop it.
- **Rusty Fishing Hook:** leave a dropped fishing rod touching water for five consecutive seconds. It breaks into two sticks and two string, with a 15% hook chance.
- **Cow Manure:** use a glass bottle on a cow. This consumes the bottle and gives a bottled manure ingredient. Each cow has a one-minute collection cooldown.
- **Infected Meat:** animals with Tetanus, temporary or permanent, drop only 1–3 infected meat. Collecting it unlocks Strange Meat. A furnace, smoker, or campfire makes it safe to eat.

## Brewing

Use a standard brewing stand with blaze powder fuel. Each ingredient can process up to three bottles in the usual brewing cycle.

| Input bottle | Ingredient | Output |
| --- | --- | --- |
| Water Bottle | Cow Manure | Cow Manure Potion |
| Cow Manure Potion | Rust | Potion of Tetanus |
| Cow Manure Potion | Infected Meat | Potion of Salmonella |
| Potion of Tetanus | Gunpowder | Splash Potion of Tetanus |
| Potion of Salmonella | Gunpowder | Splash Potion of Salmonella |

Tetanus potion tooltips show the configured duration range. Drinking or receiving a disease splash/cloud rolls a **5–10 minute infection**. The final **2–4 minutes** are continuous Lockjaw Sonata. Salmonella lasts one minute before vanilla splash-distance scaling. Cow Manure Potion is a brewing base. Vanilla splash and lingering conversions remain available. Milk clears other ordinary effects but preserves Tetanus and Lockjaw; Tetanus Antitoxin cures both immediately and returns a glass bottle.

## Tetanized Wither rituals

Use the usual T-shaped structure with three Wither Skeleton Skulls on top.

| Structure | Mutation chance |
| --- | --- |
| 4 ordinary soul sand/soul soil blocks | 0%; summons an ordinary Wither |
| 4 Tetanized Soul Sand blocks | 5% |
| 3 Tetanized Soul Sand blocks, with a netherite block replacing the upper center soul sand | 30% by default |

The netherite block adds **25 percentage points** to the configured 5% base chance. A failed mutation roll still summons a normal Wither. Both lower corner spaces must be empty. All structure blocks, including the netherite upgrade, are consumed.

A Tetanized Wither has 600 health. It drops its ordinary Nether Star, a Tetanized Wither Star, Tetanus I, experience bottles, and one randomized bonus reward. Wire-brushing the special star converts it into an ordinary Nether Star; keep it infected if you want the special beacon.

## Tetanized Beacon

Use the ordinary beacon recipe, replacing the Nether Star with a **Tetanized Wither Star**:

```text
Glass    Glass             Glass
Glass    Tetanized Star    Glass
Obsidian Obsidian          Obsidian
```

Build a pyramid with **gold blocks, copper blocks, netherite blocks, or a mixture of those three**. Other blocks, including iron, emerald, and diamond, do not count. Full copper blocks in every oxidation stage, including waxed versions, count. Cut copper and other shaped variants do not.

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

Gloves must be equipped in the offhand. They reduce rusty-contact infection risk by 50% and have 192 durability, charging one base durability per exposure. Main-hand/inventory gloves do not protect, and gloves do not stop contaminated food or disease potions. Combined vaccine and glove protection is 90% by default; weapon damage is unchanged.

The guide is reusable and has ten scrollable chapters. It shows the connected server's prevention settings. Configuration keys: `vaccineDurationSeconds` (default 1200), `vaccineProtectionChance` (0.8) and `gloveProtectionChance` (0.5).