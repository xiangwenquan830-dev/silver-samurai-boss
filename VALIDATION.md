# r61 validation

This is an asset-only revision of r60. Every Java class and every unchanged JAR entry was checked byte-for-byte against the r60 release. Only the two runtime model resources and version metadata differ.

Both phases were sampled at approximately 60 Hz in the audited traversal/fall clips. Main sheath/grab/climb/mantle/draw skirt joins have a maximum transform-component difference of 0.0; climb-loop endpoints also match. No buckle/skirt contacts were found in those sampled clips. All non-skirt animation tracks, non-traversal animations, group hierarchy and texture data were checked unchanged. Models use the same runtime field filtering as the Gradle build.

Some skirt/leg armor contacts remain. No zero-intersection guarantee is made. No new client, multiplayer or gameplay-suite execution was performed for r61. Published as Beta.

## Previous release checks

# r60 validation

The initial 335-test full run had 334 passes and one two-handed landing-grip interpolation failure. The animation was corrected; the final 18-test r49 jump suite passed. A separate 240 Hz pose sampling check measured maximum relative two-hand matrix error 0.000412 (threshold 0.005). The complete 335-test suite was not repeated after that final animation correction.

Contact-sheet views were inspected. Full multiplayer visual acceptance, arbitrary modpack compatibility, and long-term terrain stress are not claimed. Published as Beta.
