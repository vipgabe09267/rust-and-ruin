# Medical cabinet and worn glove design

The medical cabinet is a folded stainless-steel case, two blocks tall, with a dark recessed back, thin steel shelves, lower ventilation slots, pulls and a small medical badge. The face is deliberately clear cutout glass so medicines remain visible. Each bottle has a separate liquid volume, shoulders, a narrow glass neck, a ribbed screw cap and a printed label. Three muted liquid colors avoid the previous solid concrete-brick appearance. The doors swing outward on their actual left or right hinge; front and side surfaces remain separate to avoid coplanar flicker.

Both village and field cabinets share this geometry. The inventory model shows the entire cabinet. Glass is drawn in the cutout layer rather than relying on unstable full-scene translucency sorting.

Protective gloves cover the hand and a short stitched cuff, leaving the forearm exposed. The 64-pixel atlas separates palm and back panels, seams, cuff and thumb. Standard and slim arms use their native widths with minimal surface clearance, rather than the previous oversized plain brown sleeve. Both first-person and third-person views use the same model and preserve enchantment glint.

The glove surfaces clear the native 0.25-unit outer skin sleeve: the hand uses 0.275 units of dilation, the short cuff 0.30, and the thumb 0.15. The fitted dimensions, UVs and poses remain unchanged. A controlled native-client comparison with opaque blue sleeves and the actual Night Vision effect confirmed visible hands and cuffs in all eight standard/slim, first-person right/left and third-person front/back views. The original PNGs and metrics are preserved in `validation/playtest-2026-10-24/gloves/index.html`.

All assets are generated deterministically by `tools/Generate-Medical-Visuals.ps1`, which the refresh generator invokes. These are native Minecraft cuboids with intentionally drawn pixel textures; no image generation or external asset license is involved.
