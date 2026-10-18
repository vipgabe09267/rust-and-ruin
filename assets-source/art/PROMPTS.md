# Artwork prompts

## Salvage and Salmonella atlas

Built-in image generation; source: `salvage-atlas.png`. Import crops the four cells to Rust, Rusty Fishing Hook, Tetanized Soul Sand, and the Salmonella status icon.

Minecraft survival mod inventory sprite atlas, transparent background, square canvas evenly divided into 2 by 2 grid with generous transparent gutters, no lines or text. Exactly four separate pixel-art game icons each entirely within its quadrant: top left a small irregular pile of flaky reddish orange iron rust with a few dark steel flecks; top right a single old curved barbed fishing hook, orange rust crust over pitted dark iron with clear eyelet, diagonal; bottom left a square seamless flat tile of dark brown Minecraft soul sand, cracked rusty red grains with subtle distressed soul faces, orthographic flat texture fills inner square; bottom right a compact sickly green stomach symbol with three small bubbles for a salmonella status effect. Detailed intentional crisp pixel clusters, limited earthy Minecraft palette, convincing material highlights, coherent pixel-art style, readable at 64 pixels, no smooth painterly blur, no letters, no frame, no scenery, each of four designs isolated.

Built-in image generation was used. Source atlases are retained here; tools/Import-Art.ps1 extracts and downsamples the inventory cells for the game.

## Inventory atlas

Use case: stylized-concept. Asset type: one production Minecraft mod inventory sprite atlas, EXACTLY 6 equal columns and 4 equal rows, 24 icons. Canvas ideally 1536x1024, each cell exactly 256x256, transparent background throughout including gutters. Every cell contains ONE isolated object, centered with at least 24px transparent margin. No text, labels, letters, cell borders, backdrop, shadows outside the object, watermark or extra fragments. Style: premium coherent Minecraft 64x64 pixel-art item sprites enlarged cleanly. Deliberate crisp pixel clusters and readable iconic silhouettes. Dark forged iron, restrained blue-gray highlights, warm orange-brown rust concentrated at pitting and seams, wrapped grips where appropriate. A skilled game artist aesthetic, no generic glossy AI fantasy ornamentation. Inventory objects have generous substantial width, not one-pixel lines. Long tools face lower left to upper right. Small amber medical bottle with wax cap and turquoise liquid. Rusty items retain recognizable underlying iron with mottled corrosion, not uniformly orange.
The 24 cells in precise row-major order:
ROW 1: (1) forged iron square-headed nail; (2) matching corroded rusty nail; (3) industrial ridged steel rebar; (4) matching rusty rebar; (5) rebar sharpened into a distinct chisel point; (6) matching rusty sharpened rebar.
ROW 2: (1) bent iron nail with obvious bend; (2) matching rusty bent nail; (3) short hollow metal pipe with visible opening and wrapped grip; (4) rusty matching pipe; (5) wooden-handled wire cleaning brush with bristles; (6) antitoxin medicine bottle with wax cap and turquoise liquid.
ROW 3: (1) long tetanus arrow with barbed rusty point, wood shaft and brown feathers; (2) raw infected meat with subtle sickly discoloration, no gore; (3) clearly cooked browned meat steak; (4) three-dimensional crossed iron caltrop spikes; (5) rusty matching caltrop; (6) rusty iron sword with clear crossguard and leather grip.
ROW 4: (1) rusty iron pickaxe with wooden handle; (2) rusty iron axe with wooden handle; (3) rusty iron shovel with wooden handle; (4) rusty iron hoe with wooden handle; (5) small clenched-fist tetanus status icon in rust and ochre; (6) leather enchanted book with rust-colored clasp and amber page mark.
Keep exact grid alignment and no objects crossing cell boundaries. These must be actual attractive usable in-game object sprites, not a poster or presentation of icons. Actual alpha transparency.

## Tetanized skin material

Use case: stylized-concept. Asset type: a single seamless square pixel-art material overlay for Minecraft 64x64 mob skin UV textures. Fill the square with intricate irregular clusters of dark orange-brown rust scabs, sickly pale olive patches, desaturated stone-gray cracks, tiny amber pitting, dark vein-like splits. NO face, eyes, limbs, body, logos, text, object, outlines or vignette. Actual transparent gaps covering about 40 percent of the material, showing the original mob skin below when this texture is used as a skin overlay. Fragmented thin clusters distributed edge to edge as a tileable material swatch, not a central isolated object. Sophisticated controlled game pixel-art texture with believable corroded surface and disease, dark restrained palette, no bright green glow, no smooth gradients, no plastic or photographic rendering. Every mark should read as a deliberate 64x64 pixel cluster. Transparent background.


## Corrected inventory atlas

Built-in image generation; `inventory-atlas-v2.png`. The importer retains the largest connected sprite in each cell, trims neighboring fragments, preserves aspect ratio, and adds three pixels of transparent padding at 64x64. Old atlases are retained for provenance.

Create a production Minecraft pixel art sprite atlas with genuine alpha transparency, exact 6 columns x 4 rows, no background, no text. Strict grid: every cell contains one whole item centered inside the MIDDLE 60% of its cell. The outer 20% on ALL FOUR sides of EVERY cell MUST be completely transparent. Huge empty transparent gutters, objects never touch boundaries. Each item diagonal lower left to upper right where appropriate. Crisp sophisticated pixel art readable as 64x64 inventory sprites, iron with distinct cold highlights, realistic orange rust patches, wrapped grips. Row1 clean forged nail, rusted nail, steel rebar, rusted rebar, sharpened steel rebar, rusted sharpened rebar. Row2 bent clean nail, bent rusted nail, hollow steel pipe, hollow rusty pipe, wood-handled wire brush, sealed turquoise antitoxin medicine bottle. Row3 rusty tipped arrow, raw infected meat, cooked meat steak, clean iron caltrop, rusty caltrop, rusty iron sword. Row4 rusty iron pickaxe, rusty iron axe, rusty iron shovel, rusty iron hoe, clenched rusty orange fist status icon, leather book with rusty clasp. Exactly 24 separate complete objects, transparent generous padding even at canvas edge, no drop shadows, no backdrop or contact sheets, do not put adjacent item fragments inside any cell.

## Brewing and beacon progression atlas

Built-in image generation; `progression-atlas.png`. Hookline, special star, manure bottle, and cracked beacon glass. The importer keeps glass translucent and item backgrounds transparent.

Minecraft pixel-art production inventory atlas, transparent background, exact 2 by 2 equal grid with generous transparent gutters. Top left isolated rusty fishing hook on a coiled metal chain and short wrapped wood grip, a practical grappling hookline tool with clear curved barbed hook. Top right isolated rare corrupted Nether Star item, five-point jagged pale bone star with dark iron outline and mottled orange rust, ominous amber center, recognizable Minecraft nether star silhouette. Bottom left isolated small clear glass bottle containing dark brown cow manure with straw flecks and cork, readable inventory item, no gore. Bottom right flat square seamless tile for beacon glass: smoky transparent glass with thin copper-brown frame at its square perimeter, fine angular fracture lines, sparse rust chips, translucent clear center, perfectly straight-on orthographic texture tile. Each icon fully within inner 65% of own quadrant, separate complete items, no text, no letters, no surrounding scenery, crisp pixel clusters, detailed authentic Minecraft sprites that read at 64x64.

## Lockjaw status icon

Built-in image generation; `lockjaw.png`.

One Minecraft status effect icon, transparent background, a clenched jaw with shut ivory teeth framed by a rusty orange-brown jaw silhouette, compact symmetrical readable pixel art at 64x64. A small iron padlock shape beneath the chin suggests lockjaw, integrated into a single connected silhouette. Crisp carefully shaded pixel clusters, dark brown outlines, no letters, no text, no gore, centered with 15 percent transparent padding on every side.

## Mob skins

Mob skins are generated at runtime by recoloring the original texture, retaining its dimensions, alpha, UV mapping, and recognizable surface details. Dynamic textures receive a color tint fallback; sheep wool is tinted separately. This also follows resource packs where an original PNG is available.
