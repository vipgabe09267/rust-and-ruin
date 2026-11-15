# Rust & Ruin final release-readiness evidence

Status: **SCOPED_CHECKS_COMPLETE**. Full final native suite: **PASS**.

Open [the local gallery](index.html) or [manifest.json](manifest.json). Final candidate SHA256: E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC. The pre-final D287 candidate is labeled separately; only CombatInfection.class changed between the two packages.

## Completed final graphical reports

- [graphical-baseline-plain.json](reports/final/graphical-baseline-plain.json): 31 original screenshot references.
- [graphical-forge-plain.json](reports/final/graphical-forge-plain.json): 4 original screenshot references.
- [graphical-advancements-plain.json](reports/final/graphical-advancements-plain.json): 28 original screenshot references.
- [graphical-dragon-ritual-plain.json](reports/final/graphical-dragon-ritual-plain.json): 17 original screenshot references.
- [graphical-advancement-layout-plain.json](reports/final/graphical-advancement-layout-plain.json): 152 original screenshot references.
- [graphical-crafting-stations-plain.json](reports/final/graphical-crafting-stations-plain.json): 62 original screenshot references.
- [graphical-trap-regression-plain.json](reports/final/graphical-trap-regression-plain.json): 11 original screenshot references.
- [graphical-combat-regressions-plain.json](reports/final/graphical-combat-regressions-plain.json): 22 original screenshot references.
- [graphical-despawn-plain.json](reports/final/graphical-despawn-plain.json): 3 original screenshot references.
- [graphical-proximity-audio-plain.json](reports/final/graphical-proximity-audio-plain.json): 6 original screenshot references.
- [graphical-wither-patch-plain.json](reports/final/graphical-wither-patch-plain.json): 8 original screenshot references.
- [graphical-beacon-ui-plain.json](reports/final/graphical-beacon-ui-plain.json): 20 original screenshot references.
- [graphical-medical-jei.json](reports/final/graphical-medical-jei.json): 8 original screenshot references.
- [graphical-forge-jei.json](reports/final/graphical-forge-jei.json): 6 original screenshot references.
- [graphical-medical-rei.json](reports/final/graphical-medical-rei.json): 8 original screenshot references.
- [graphical-forge-rei.json](reports/final/graphical-forge-rei.json): 6 original screenshot references.

## Native and earlier attempts

The manifest lists each native run, production probe and retained failure separately. The pre-final native run completed417/418 and remains failed. The initial composite client passed 31 ordinary crafting transactions, 15 potion forms and weathering/cleaning actions, then failed its fresh-actor guard because earlier actions had already awarded rusty_business. Its original PNGs remain in the shared capture directory; they are not substituted for the planned final-candidate crafting pass. Its original logs and station-dispatch JSON are controls, not final gameplay proof.

The 15-case focused native run covers five real final-shot cases and 10 acquisition cases. A complete final native PASS is recorded only after the separate unfiltered 423-case XML and complete build/server log finish and agree.

Production namespace probes use the actual packaged JAR, Fabric API and intermediary Minecraft classes in an isolated cwd. They stop at EULA=false, exit without creating a world, and do not claim full production server initialization or gameplay.

## Source and fixture disclosures

Baseline screenshots use supplied items, effects, controlled mobs and placed templates. They verify actual native render layers, potion tooltips, disease HUD/tab hearts, gloves, cabinet, lockjaw gesture, medic clothing and trade UI. These specimens do not earn all advancements; a separate causal journey is required for that claim.

The final-shot fix reads the current weapon object's retained enchantment NBT, matches the registered Tetanus enchantment and clamps levels 0-2. It uses no global shot cache or cross-action state. Native tests exercise final bow/crossbow shots, every Multishot arrow/firework, normal trident and unbroken controls, saved projectile NBT and actual damage/infection.

All original receipts and PNGs are copied byte for byte. Human-readable logs are UTF-8; logs/raw retains original bytes. The manifest records original and normalized hashes. Generated HTML, Markdown and manifest are indexes, not native receipts. No previous evidence gallery is rewritten.

Pending: None for the explicitly listed scope.