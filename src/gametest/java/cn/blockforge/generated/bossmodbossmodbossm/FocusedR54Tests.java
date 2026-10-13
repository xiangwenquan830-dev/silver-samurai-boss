package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r54")
@PrefixGameTestTemplate(false)
public class FocusedR54Tests {
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void growth_is_continuous_and_rooted(GameTestHelper h) {
        h.assertTrue(ClawGrowth.SPEED == .5f && ClawGrowth.GROW_TICKS == 48, "再生播放速度错误");
        for (float x : new float[] {-1, 0, 1}) {
            h.assertTrue(Math.abs(ClawGrowth.tip(x, 600) - 11.55f) < .0001, "断裂起点跳变");
            h.assertTrue(Math.abs(ClawGrowth.tip(x, 594) - ClawCut.end(x)) < .0001, "残爪长度不对");
            h.assertTrue(ClawGrowth.tip(x, 560) == ClawCut.end(x), "未保持两秒残爪");
            h.assertTrue(
                    ClawGrowth.tip(x, 552) == 0 && ClawGrowth.tip(x, 1) == 0, "残爪未收回或计时结束自动长出");
            float previous = 0;
            for (float t = 48; t >= 0; t -= .125f) {
                float now = ClawGrowth.grownTip(t);
                h.assertTrue(now >= previous - .0001 && now <= 11.5501, "再生倒退或超长");
                h.assertTrue(ClawGrowth.height(0, 2, now) == Math.min(2, now), "根部不连续");
                previous = now;
            }
            h.assertTrue(Math.abs(previous - 11.55f) < .001, "未长回原尺寸");
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void growth_sound_stages_and_cleanup(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var p = SamuraiGameTests.player(h, 5, 1, 5);
        ClawSeverance.begin(p, true);
        ClawSeverance.sound(p);
        h.assertTrue(!p.getPersistentData().contains(ClawSeverance.SOUND), "过早播放生长声");
        p.getPersistentData().putLong(ClawSeverance.GROW, ClawSeverance.now(p) + 48);
        ClawSeverance.sound(p);
        ClawSeverance.sound(p);
        h.assertTrue(p.getPersistentData().getInt(ClawSeverance.SOUND) == 1, "生长起始音未去重");
        p.getPersistentData().putLong(ClawSeverance.GROW, ClawSeverance.now(p) + 10);
        ClawSeverance.sound(p);
        h.assertTrue(p.getPersistentData().getInt(ClawSeverance.SOUND) == 2, "未播放收尾声");
        ClawSeverance.clear(p);
        h.assertFalse(p.getPersistentData().contains(ClawSeverance.SOUND), "生长声音标记未清理");
        p.discard();
        h.succeed();
    }
}
