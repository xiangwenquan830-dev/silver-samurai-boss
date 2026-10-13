package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.nio.file.*;

/** 独立验收入口，必须显式-DsilverSamurai.selfTest=true；正式游戏完全不运行。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class SamuraiTestServer {
    private static MultipleTestTracker tracker;
    private static final GameTestTicker TICKER = new GameTestTicker();

    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        if (!Boolean.getBoolean("silverSamurai.selfTest")) return;
        // 仅显式自动验收使用内存配置；开关测试不再写盘触发文件监听与恢复值竞争。
        BossConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        GameTestRegistry.register(SamuraiGameTests.class);
        GameTestRegistry.register(StabilityGameTests.class);
        String filter = System.getProperty("silverSamurai.testFilter", "");
        tracker =
                new MultipleTestTracker(
                        GameTestRunner.runTests(
                                GameTestRegistry.getAllTestFunctions().stream()
                                        .filter(t -> t.getTestName().contains(filter))
                                        .toList(),
                                new BlockPos(0, 100, 0),
                                Rotation.NONE,
                                event.getServer().overworld(),
                                TICKER,
                                1));
        tracker.addFailureListener(info -> GeneratedMod.LOG.error("[实测失败] {}", info));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (tracker == null || event.phase != TickEvent.Phase.END) return;
        TICKER.tick();
        if (tracker.isDone()) {
            String result =
                    "游戏服务器自动测试：总数="
                            + tracker.getTotalCount()
                            + "，失败="
                            + tracker.getFailedRequiredCount()
                            + "，进度="
                            + tracker.getProgressBar();
            GeneratedMod.LOG.info(result);
            try {
                Files.writeString(
                        Path.of("samurai-test-result.txt"),
                        result + "\n" + tracker.getFailedRequired());
            } catch (Exception e) {
                GeneratedMod.LOG.error("测试报告写入失败", e);
            }
            tracker = null;
            event.getServer().halt(false);
        }
    }

    private SamuraiTestServer() {}
}
