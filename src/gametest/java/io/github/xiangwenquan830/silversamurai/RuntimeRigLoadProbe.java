package io.github.xiangwenquan830.silversamurai;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Run each mode in a fresh JVM with the same heap; never shipped in the release JAR. */
public final class RuntimeRigLoadProbe {
    private static final SamuraiRig[] KEEP = new SamuraiRig[2];

    public static void main(String[] args) throws Exception {
        boolean legacy = args.length > 0 && args[0].equals("legacy");
        var memory = ManagementFactory.getMemoryMXBean();
        var running = new AtomicBoolean(true);
        var peak = new AtomicLong();
        Thread monitor = new Thread(() -> {
            while (running.get()) {
                peak.accumulateAndGet(memory.getHeapMemoryUsage().getUsed(), Math::max);
                try { Thread.sleep(1); }
                catch (InterruptedException ignored) { return; }
            }
        }, "rig-load-memory-probe");
        monitor.setDaemon(true);
        monitor.start();
        long start = System.nanoTime();
        for (int phase = 1; phase <= 2; phase++) {
            if (legacy) {
                try (var in = RuntimeRigLoadProbe.class.getResourceAsStream(
                        "/assets/silver_samurai/models/phase" + phase + ".bbmodel");
                        var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    KEEP[phase - 1] = new SamuraiRig(phase, JsonParser.parseReader(reader).getAsJsonObject());
                }
            } else KEEP[phase - 1] = SamuraiRig.get(phase);
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        running.set(false);
        monitor.join();
        System.gc();
        long keys = 0;
        for (var rig : KEEP) for (var clip : rig.clips.values()) for (var track : clip.tracks.values())
            keys += track.position.size() + track.rotation.size() + track.scale.size();
        System.out.printf(java.util.Locale.ROOT,
                "RIG_LOAD mode=%s elapsed_ms=%d sampled_peak_bytes=%d retained_heap_bytes=%d keys=%d%n",
                legacy ? "legacy" : "stream", elapsed, peak.get(), memory.getHeapMemoryUsage().getUsed(), keys);
    }
}
