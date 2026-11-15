# Rust & Ruin 0.1.1-beta patch evidence

Status: **SCOPED_CHECKS_COMPLETE**. Current complete native release run: **PASS**. Open [the local gallery](index.html) or inspect [manifest.json](manifest.json) for original receipt paths, dates, byte counts and SHA256 hashes.

## Completed graphical evidence

The beacon report records uncapped GUI scales 2, 3, 4, and 6 and 20 original screenshots: II, III, IV without its star, IV with payment, and Regeneration at each scale. It verifies all 10 controls and 38 native slots fit without overlap, blocks unpaid IV confirmation, consumes exactly one netherite ingot and one Tetanized Wither Star through the real menu, applies Haste IV, and restores the fixture. No advancement criteria are granted or reset.

Completed reports:

- [graphical-beacon-ui-plain.json](reports/graphical-beacon-ui-plain.json): 20 original screenshot references.
- [graphical-medical-plain.json](reports/graphical-medical-plain.json): 8 original screenshot references.
- [graphical-wither-patch-plain.json](reports/graphical-wither-patch-plain.json): 8 original screenshot references.
- [graphical-dragon-ritual-plain.json](reports/graphical-dragon-ritual-plain.json): 17 original screenshot references.
- [graphical-medical-jei.json](reports/graphical-medical-jei.json): 8 original screenshot references.
- [graphical-forge-jei.json](reports/graphical-forge-jei.json): 6 original screenshot references.
- [graphical-medical-rei.json](reports/graphical-medical-rei.json): 8 original screenshot references.
- [graphical-forge-rei.json](reports/graphical-forge-rei.json): 6 original screenshot references.

Audio file decoding/metadata also passed: [original JSON](reports/audio-verification.json). Both files are mono 44.1kHz Vorbis; the source is 4.300000 seconds and the processed cue 5.976327 seconds. This does not claim in-game playback.

Repeated generation preserved all 409 resource files byte for byte: [original equality receipt](reports/resource-reproducibility.json). The repeat preceded the final REI run; the final build followed REI.

## Scope and remaining work

Only completed reports explicitly added to this snapshot count as passing. Pending receipts: No required receipts pending. Completed graphical phases are listed in the manifest and gallery; the combined visible-client log is archived only after it closes. A previous 414/415 run is retained as **failed**, and the later interrupted native invocation is **cancelled**. The cancelled run has no XML here: its apparent XML was stale and is deliberately excluded.

Both failed beacon attempts remain: one external helper resized the live viewport, and the other client was minimized with zero client area until startup timed out. These are recorded as failed harness attempts, followed by the independent successful matrix. The closed combined playtest also remains failed overall: its medical and Wither phases passed separately, followed by a grab-height measurement failure. Its original log and exact fixture explanation are retained; the independent Dragon confirmation is recorded separately when complete.

All PNGs are original native framebuffer captures, copied byte for byte. Logs are additionally normalized to UTF-8 for reading; logs/raw contains the unmodified originals. The manifest records source and archived hashes. Generated HTML, Markdown and the manifest itself are indexes, not native receipts.

The beacon uses controlled player abilities, a manually constructed pyramid and ingredient fixtures. It is a focused UI/payment check, not an ordinary survival playthrough or a guarantee of every modpack combination. The current Wither proof uses actual Creative and Survival rituals and timed waves, but has camera-flight/invulnerability fixtures and reduces each boss to one HP only for the finishing attack. Its native loot/XP checks do not claim a full survival balance fight. The medical proof uses a placed writer and an AI-paused medic; outputs, deliveries, trades, cure use and its advancement are real native actions.