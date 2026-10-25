# Clinic discovery and advancement navigation fixes — October 25, 2026

This follow-up addresses clinic locate results that appeared to point at empty ground and an advancement screen that was difficult to navigate. It preserves the existing cave-camp and infection-audio fixes. The revised native server suite has passed **341 tests**. The final forced rebuild and a second 341-test suite passed; actual clients verified all five clinics and the advancement UI at four GUI scales. See the final records below.

## Findings and changes

| Finding | Change and evidence |
| --- | --- |
| A clinic can be away from the coordinate shown by native structure locate. | The clinic shares its village's saved candidate chunk, then finds a free plot 24–96 blocks from the village root. Native locate reports the candidate chunk rather than the building. In the actual seed-0 sample, the Savanna result identifies `[544, ~, -1056]`, while the clinic's template begins at `[592, 61, -1106]`. Branded clinic commands now also provide its exact walkable porch, including Y. |
| A sufficiently displaced footprint could escape Minecraft's structure-reference search. | New clinic candidates must keep their complete 9 × 13 footprint within the native eight-chunk reference window around the saved start. Boundary regressions cover positive and negative coordinates. This is a placement safeguard; an old out-of-range clinic was not reproduced in this pass. |
| The mod advancement tree had excessive vertical spread for the native viewport. | Its 27 displayed advancements now form four main branches occupying seven columns and eight rows. Native loaded-layout checks verify frame separation and the tree bounds. IDs, criterion keys and rewards remain unchanged; parent links organize the display without adding gameplay prerequisites. |
| Finding a node required awkward dragging. | The mod tab initially focuses its root and supports mouse-wheel movement, Shift + wheel for sideways movement, and Home to return to the root. Vanilla advancement tabs retain their native wheel behavior. |
| Home could intercept the player's chosen Advancements key. | The Home shortcut now yields when Home is bound to the normal Advancements action, preserving the native close-screen behavior. |
| A supply advancement title was truncated at actual GUI scale 6. | The `supply_run` advancement is now titled **Special Delivery**, with the description “Fulfil a medic supply request.” Its saved ID and criterion are unchanged. The graphical fixture checks the native rendered title width and tooltip bounds. Final native-frame screenshots confirm the revised text is complete and inside the screen at GUI scale 6. |

The original reported world was deleted before its seed and saved structure data could be inspected. The displaced native pointer is confirmed independently, but this does not prove why that specific Taiga village appeared empty.

## Finding a clinic

Use either branded command:

```text
/rustandruin locate medic
/locate structure rustandruin:medic_clinic
```

Both require operator permission level 2. Their native result is followed by an exact porch-entrance message. Use the full XYZ entrance coordinates to reach the building; clicking them prepares a teleport command rather than teleporting automatically. Camp commands keep their existing underground-entrance behavior.

Direct legacy registry searches and native structure-tag searches retain Minecraft's ordinary start-coordinate behavior. Nearby `/locate poi` searches inspect indexed workstations within 128 blocks; they are not a global clinic search.

The 55 percent setting is a roll at eligible village candidate locations. A suitable dry, nearly level, unoccupied plot is also required. It does not guarantee a clinic in every village or an exact 55 percent measured population.

## Genuine generation coverage

The native test creates a fresh normal-Overworld noise world, finds actual eligible candidates on the native village grid, and verifies each selected village's real saved start. It then generates the parent and all clinic footprint chunks through **FULL**. No clinic template, structure start, POI or resident is inserted manually.

The passing seed-0 examples are:

| Theme | Saved start chunk X, Z | Template origin X, Y, Z | Porch entrance X, Y, Z |
| --- | --- | --- | --- |
| Plains | `106, 226` | `1632, 117, 3614` | `1636, 119, 3615` |
| Desert | `24, -200` | `399, 70, -3211` | `403, 72, -3210` |
| Savanna | `34, -66` | `592, 61, -1106` | `596, 63, -1105` |
| Snowy | `-54, 222` | `-847, 62, 3573` | `-843, 64, 3574` |
| Taiga | `-45, 260` | `-680, 77, 4110` | `-676, 79, 4111` |

For every theme, the native case checks the complete 77-block foundation, roof ridge, walkable porch, both cabinet halves and facing, unopened loot chest, one established medic of the correct villager type, one lower-half workstation POI, and no upper-half duplicate. Connected test observers engage Minecraft's normal chunk and entity tracking; they are removed after the checks.

A separate planning regression found all five themes using native Overworld noise and village generation at seeds **0** and **246813579**. Those extra planned sites are not claimed as ten fully generated or graphically visited clinics. Sampled worlds cannot establish correctness for every seed or terrain configuration.

## Advancement and graphical methods

Vanilla advancement definitions remain loaded. Their five tabs keep the normal unlock criteria. The graphical fixture exercises those criteria through real crafting-table item pickup, apple consumption, a client attack killing a cow, and native dimension-change commands for the Nether and End. The dimension commands are operator test setup; this is not a claim of a Survival portal playthrough. It does not grant or revoke advancement criteria directly.

The mod's ordinary advancement journey precedes the layout checks. The layout fixture then opens actual tabs, drags the tree, uses wheel and Home navigation, and checks completed native widgets. It preserves existing earned-criterion dates. To test actual GUI scales 2, 3, 4 and 6, it temporarily requests a 1920 × 1440 native window, waits for the framebuffer to settle, and requires the applied scale to match the requested value. Original window settings, key binding and player setup are restored.

A smoke-only probe observes completed native tooltip drawing. It does not change hit testing, progress or rendering. Controller success, native tooltip drawing and a visually correct captured frame are separate checks; final image inspection remains necessary.

## Test-fixture corrections

These changes repair the development tests and do not add gameplay behavior:

- **Premature POI assertion:** a FULL chunk can still await native palette and POI indexing. The first 341-case run failed its clinic POI check before creating a connected observer, after the foundation and cabinet blocks already existed. The corrected fixture retains immediate physical checks, waits for real player presence, every footprint chunk's ticking future and the established medic, then applies the same strict POI assertions. It never inserts a POI.
- **Wrong locate neighborhood:** a clinic's displaced center can cross a native grid-region boundary. Graphical observers now begin at the saved start chunk's center so native first-ring locate selects the intended candidate. The real Savanna sample demonstrates this boundary crossing.
- **Resident readiness:** nearby unemployed villagers may choose the Medic profession. Readiness and final checks identify the established medic by profession, positive experience and expected biome type, avoiding premature readiness from an unrelated novice.
- **Following an active medic:** portrait setup chooses a collision-free observer position with real line of sight and follows the actual medic. The villager is not repositioned, respawned or given disabled AI to manufacture a portrait.
- **Requested versus applied GUI scale:** an earlier smaller-window run requested several scales that Minecraft capped to the same applied scale. The stricter fixture requires four distinct requested/applied scales after native resizing.
- **Tooltip and frame synchronization:** an earlier controller could finish while its screenshots lacked actual tooltips. Native mouse input is now acknowledged through Minecraft's cursor callback and completed tooltip rendering is observed separately. A capture must still be reviewed as an image; a state assertion alone does not prove that the matching tooltip or camera view was present in that captured frame.

## Verification status

| Check | Current result |
| --- | --- |
| First complete native suite | 340/341 passed; sole failure was the premature clinic POI fixture assertion described above. |
| Revised complete native suite | **PASS — 341 tests, zero failures or skips**, including all five genuine FULL clinic themes and the two-seed planning case. Local evidence: `.tools/clinic-advancements-2026-10-25/server-second.xml`. |
| Final forced native suite and rebuild | **PASS - 341/341**, zero failures or skips; same JAR checksum after forced rebuild. |
| Final graphical advancement navigation, actual four scales and tooltip/title review | **PASS - 140 native post-render frames**, actual scales 2/3/4/6, all 27 visible nodes per scale, 108 same-frame in-bounds tooltips; 91 existing criterion dates preserved, no grants/revocations. Window, cursor, GUI and keybinding restored. |
| Final graphical natural-clinic discovery | **PASS - five real biome clinics**, 10 connected-client commands, 25 screenshots, exact entrances, matching native/client resident UUIDs, lower-only cabinet POIs and native loot menus. No manually inserted buildings, POIs or residents. |
| Final resource/package/reproducibility checks | **PASS - 382 resources identical twice**, source/package agreement, three malformed archives rejected. JAR 3,372,740 bytes, SHA-256 `734287F34713BB1DDA09EB83B024DBFE32D13678EE836D9FDA35A89F6721CC36`. |

Package audits check the packaged grid settings and template files. They do not substitute for the separate native FULL-generation and graphical checks.

## Scope and implementation

The relevant production code is [ClinicStructure](../src/main/java/com/tetanusmod/world/ClinicStructure.java), [LocateAliases](../src/main/java/com/tetanusmod/command/LocateAliases.java), [MedicStructurePiece](../src/main/java/com/tetanusmod/world/MedicStructurePiece.java), and the two advancement navigation mixins. [Generate-Advancement-Layout.ps1](../tools/Generate-Advancement-Layout.ps1) reproduces the display ancestry and shorter title; [Generate-Refresh.ps1](../tools/Generate-Refresh.ps1) preserves the command text and world-generation resources.

[ClinicDiscoveryGameTests](../src/gametest/java/com/tetanusmod/test/ClinicDiscoveryGameTests.java) covers genuine native placement; [AdvancementLayoutRegressionGameTests](../src/gametest/java/com/tetanusmod/test/AdvancementLayoutRegressionGameTests.java) checks loaded definitions, normal pickup and saved criterion dates. The two graphical controllers record actual client interaction and screenshots separately.

World-generation changes apply to **newly generated terrain**. Existing buildings and saved starts are not rebuilt, moved or rerolled. **No retrogen was added.** Adding clinics safely to an existing village would require a separate opt-in design with collision, ownership, duplicate-resident and save-backup handling.

This was a targeted review of the changed generation, locator, layout and test code. It is not a claim that every line of the entire mod was audited again, that every possible bug is absent, or that long-running remote multiplayer/modpack behavior has been established.
[Original-frame gallery](../validation/clinic-advancements-2026-10-25/index.html) and [final verification records](../validation/clinic-advancements-2026-10-25/verification/results.json).
