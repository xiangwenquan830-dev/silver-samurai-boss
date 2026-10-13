package cn.blockforge.generated.bossmodbossmodbossm;

/** Cut positions in the original Wolverine claw coordinate system (full tip is at 11.55). */
final class ClawCut {
    static float end(float x) {
        return x < -.6f ? 4.35f : x > .6f ? 3.85f : 4.9f;
    }

    private ClawCut() {}
}
