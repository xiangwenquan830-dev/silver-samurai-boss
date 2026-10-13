package cn.blockforge.generated.bossmodbossmodbossm;

public class ClawClientProbe {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        Class<?> c = Class.forName("cn.blockforge.generated.bossmodbossmodbossm.ClientClawBridge");
        var init = c.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(null);
        var a = c.getDeclaredField("available");
        a.setAccessible(true);
        if (!a.getBoolean(null))
            throw new AssertionError("r71 client attachment/HUD bridge unavailable");
        if (!BrokenClawLayer.available())
            throw new AssertionError("r71 original claw geometry unavailable");
        var view =
                Class.forName("cn.blockforge.generated.minecminecraftforge1.ClientState$View")
                        .getConstructor()
                        .newInstance();
        for (String name : new String[] {"guardBroken", "noticeTicks", "idle"})
            if (view.getClass().getField(name).getType() != int.class)
                throw new AssertionError(name);
        var feedback = view.getClass().getField("guardFeedback").get(view);
        feedback.getClass().getField("ticks").setInt(feedback, 0);
        var segmentsField = BrokenClawLayer.class.getDeclaredField("segments");
        segmentsField.setAccessible(true);
        var segments = (java.util.List<?>) segmentsField.get(null);
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        float min = Float.NaN, last = -Float.MAX_VALUE;
        for (float left = 47.875f; left >= 0; left -= .125f) {
            Collector vertices = new Collector();
            for (Object segment : segments) {
                var t = segment.getClass();
                var pm = t.getDeclaredMethod("part");
                pm.setAccessible(true);
                var minm = t.getDeclaredMethod("min");
                minm.setAccessible(true);
                var maxm = t.getDeclaredMethod("max");
                maxm.setAccessible(true);
                var xm = t.getDeclaredMethod("x");
                xm.setAccessible(true);
                float lo = (float) minm.invoke(segment),
                        hi = (float) maxm.invoke(segment),
                        x = (float) xm.invoke(segment);
                var part = (net.minecraft.client.model.geom.ModelPart) pm.invoke(segment);
                float height = ClawGrowth.height(lo, hi, ClawGrowth.grownTip(left));
                if (height <= 0) continue;
                part.yScale = height / (hi - lo);
                part.render(pose, vertices, 15728880, 0, 1, 1, 1, 1);
            }
            if (vertices.count < 24 || !Double.isFinite(vertices.max))
                throw new AssertionError("missing/nonfinite blade vertices");
            if (Float.isNaN(min)) min = (float) vertices.min;
            if (Math.abs(vertices.min - min) > .00001 || vertices.max < last - .00001)
                throw new AssertionError("root moved or blade reversed");
            last = (float) vertices.max;
        }
        System.out.println(
                "CLIENT_GEOMETRY_PASS: 384 growth samples, fixed original roots, monotonically increasing tips, HUD state contract");
        System.out.println(
                "CLIENT_INTERFACE_PASS: r71 geometry, hand frame, wound rendering and HUD methods resolved");
    }

    static class Collector implements com.mojang.blaze3d.vertex.VertexConsumer {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        int count;

        public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new AssertionError("nonfinite vertex");
            min = Math.min(min, y);
            max = Math.max(max, y);
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) {
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int a, int b) {
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer uv2(int a, int b) {
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        public void endVertex() {
            count++;
        }

        public void defaultColor(int r, int g, int b, int a) {}

        public void unsetDefaultColor() {}
    }
}
