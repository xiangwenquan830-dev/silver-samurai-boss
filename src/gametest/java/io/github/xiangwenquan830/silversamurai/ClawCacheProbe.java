package io.github.xiangwenquan830.silversamurai;

import java.lang.reflect.Field;
import java.util.Map;

/** Client-only reflection contract probe; no renderer or foreign config state is mutated. */
public class ClawCacheProbe {
    public static void main(String[] args) throws Exception {
        ClawClientProbe.main(args);
        Class<?> bridge = ClientClawBridge.class;
        var getter = bridge.getDeclaredMethod("f", Object.class, String.class);
        getter.setAccessible(true);
        var view =
                Class.forName("cn.blockforge.generated.minecminecraftforge1.ClientState$View")
                        .getConstructor()
                        .newInstance();
        Field first = (Field) getter.invoke(null, view, "notice");
        for (int i = 0; i < 10000; i++) {
            if (getter.invoke(null, view, "notice") != first) {
                throw new AssertionError("field lookup was not reused");
            }
        }
        first.set(view, "live-state");
        if (!"live-state".equals(((Field) getter.invoke(null, view, "notice")).get(view))) {
            throw new AssertionError("cached stale state instead of field metadata");
        }
        var settings = bridge.getDeclaredField("SETTINGS");
        settings.setAccessible(true);
        var entries = (Map<?, ?>) settings.get(null);
        var config = Class.forName("cn.blockforge.generated.minecminecraftforge1.ModConfig");
        if (entries.size() != 7) throw new AssertionError("missing config handles");
        for (var entry : entries.entrySet()) {
            if (config.getField((String) entry.getKey()).get(null) != entry.getValue()) {
                throw new AssertionError("cached config snapshot instead of live handle");
            }
        }
        System.out.println(
                "CLIENT_CACHE_PASS: 10000 lookups reuse metadata; live view and seven config handles preserved");
    }
}
