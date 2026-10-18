# r62 validation — 2026-10-18

## Executed

- `compileJava`, `compileGametestJava`, `build` and `reobfJar`: passed with Forge 47.4.0 / Java 17. Existing deprecation warnings remain; no Forge minimum-version changes.
- Standalone maintenance suite: **219 required tests passed**. Wolverine-specific tests intentionally skip when its JAR is absent; DeceasedBeast was absent.
- Full suite with the actually installed `wolverine-forge-1.20.1-r72-claws-rage2s.jar`: **338 required tests passed**, with no COMPAT_SKIP. This runs the real claw-sever/regrowth and healing-suppression integration checks. The DeceasedBeast-specific large-target test was skipped because that separate mod was not loaded.
- New regressions verify shield destruction preserves spare shields, the server actually emits `ClientboundCooldownPacket`, late replacement use is rejected/synchronized, expired locks clear, invalid reflection signatures/fields are rejected, and preload reuses existing rigs.
- Original full-tree parser vs streamed runtime loader: **108,793 + 145,153 = 253,946 exact keyframes**, including time/value/interpolation, **535 + 434 = 969 exact sampled poses**, including bone matrices and visibility. Geometry, hierarchy and UVs also compared by the existing r52 equivalence tests.
- After the final client-only hardening edits: rebuilt successfully and ran `ClawCacheProbe` against the deobfuscated installed r72. **384 geometry-growth samples**, fixed original roots, increasing tips, HUD/hand/wound interface resolution and **10,000 cached field lookups** passed. This is a headless interface/geometry probe, not an in-game visual test.
- Static release audit: all **138** production class entries are under the new unique package, no test/probe classes in the JAR, both model-source files are byte-identical to r61. Of the original 75 production Java files, 68 changed only their package/import references. Seven received targeted edits; RuntimeRigReader is the only new production class.

## Isolated loading measurement

One fresh JVM per mode, Java 17, `-Xms64m -Xmx512m`, identical generated runtime resources, 1 ms heap sampling:

| Loader | Elapsed | Sampled peak used heap | Used heap after GC |
|---|---:|---:|---:|
| Full JSON tree (reference) | 842 ms | 343,932,928 bytes (~328 MiB) | 24,127,040 bytes |
| Streamed keyframes | 624 ms | 131,066,112 bytes (~125 MiB) | 24,209,416 bytes |

This single-run probe demonstrates lower transient heap use, not a guaranteed speedup or a reduction in persistent rig storage. Whole-game memory and other machines were not benchmarked. Common-setup preload in the standalone suite took 558 ms. Source: RuntimeRigLoadProbe.java; raw logs accompany the delivery.

## Boundaries

Validation did not modify an actual user save or install a new JAR in the user's running Minecraft instance. Publication status is recorded separately on the release pages. Client/server must both update and fully restart when installing.

No NeoForge support work. No public Wolverine API or paired Wolverine release yet; the legacy adapter remains, with stricter structural validation. Future semantic changes still require compatibility testing. No tracking/network-rate tuning without a packet measurement baseline. No broad state-machine/navigation rewrite, animation decimation, or removal of server-side input enforcement.

Actual multiplayer visual perception, Epic Fight / First-person Model / 3D skin layers, arbitrary modded shields and long-term large-modpack stress were not verified. Existing minor armor contacts documented for r61 remain.

## Earlier validation records

# r61 validation

This is an asset-only revision of r60. Every Java class and every unchanged JAR entry was checked byte-for-byte against the r60 release. Only the two runtime model resources and version metadata differ.

Both phases were sampled at approximately 60 Hz in the audited traversal/fall clips. Main sheath/grab/climb/mantle/draw skirt joins have a maximum transform-component difference of 0.0; climb-loop endpoints also match. No buckle/skirt contacts were found in those sampled clips. All non-skirt animation tracks, non-traversal animations, group hierarchy and texture data were checked unchanged. Models use the same runtime field filtering as the Gradle build.

Some skirt/leg armor contacts remain. No zero-intersection guarantee is made. No new client, multiplayer or gameplay-suite execution was performed for r61. Published as Beta.

## Previous release checks

# r60 validation

The initial 335-test full run had 334 passes and one two-handed landing-grip interpolation failure. The animation was corrected; the final 18-test r49 jump suite passed. A separate 240 Hz pose sampling check measured maximum relative two-hand matrix error 0.000412 (threshold 0.005). The complete 335-test suite was not repeated after that final animation correction.

Contact-sheet views were inspected. Full multiplayer visual acceptance, arbitrary modpack compatibility, and long-term terrain stress are not claimed. Published as Beta.
