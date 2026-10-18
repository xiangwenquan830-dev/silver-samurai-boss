package io.github.xiangwenquan830.silversamurai;

import java.util.*;

/** 有预算的三维A*。世界适配器必须以完整碰撞箱检测，测试和游戏共用此规划器。 */
public final class RoutePlanner {
    public record Point(double x, double y, double z) {
        public double distance(Point b) {
            return Math.sqrt(square(x - b.x) + square(y - b.y) + square(z - b.z));
        }

        public Point mix(Point b, double f) {
            return new Point(x + (b.x - x) * f, y + (b.y - y) * f, z + (b.z - z) * f);
        }
    }

    public enum Kind {
        WALK,
        VAULT,
        CLIMB,
        CLEAR
    }

    public record Edge(Point from, Point to, Kind kind, double crest) {
        public List<Point> points() {
            if (kind == Kind.VAULT || kind == Kind.CLIMB)
                return List.of(
                        from, new Point(from.x, crest, from.z), new Point(to.x, crest, to.z), to);
            if (to.y > from.y + .05) return List.of(from, new Point(from.x, to.y, from.z), to);
            if (to.y < from.y - .05) return List.of(from, new Point(to.x, from.y, to.z), to);
            return List.of(from, to);
        }
    }

    public interface World {
        boolean fits(Point p);

        boolean support(Point p);

        boolean clearable(Point p, double supportY);

        boolean wall(Point p, int dx, int dz, double height);

        boolean goal(Point p, Point target);

        boolean banned(Edge e);

        default Point floor(double x, double z, double y, double up, double down) {
            for (double h = Math.floor((y + up) * 2) / 2; h >= y - down - .001; h -= .5) {
                Point p = new Point(x, h, z);
                if (fits(p) && support(p)) return p;
            }
            return null;
        }
    }

    public record Settings(
            double safeDrop, double vault, double climb, double clear, int total, int tier) {}

    private record Key(int x, int y, int z) {}

    private record Node(Key key, Point p, double cost, double rank, Node parent, Edge edge) {}

    private final World world;
    private final Settings settings;
    private final Point start, target;
    private final PriorityQueue<Node> open =
            new PriorityQueue<>(Comparator.comparingDouble(Node::rank));
    private final Map<Key, Double> cost = new HashMap<>();
    private final Set<Key> closed = new HashSet<>();
    private List<Edge> result;
    private boolean done, partial;
    private int expanded;

    // A bounded search can return a safe forward segment without declaring the final goal reached.
    private boolean segmentReached(Point p) {
        boolean distant =
                Math.abs(target.x - start.x) > 30
                        || Math.abs(target.z - start.z) > 30
                        || Math.abs(target.y - start.y) > 10;
        return distant
                && (Math.hypot(p.x - start.x, p.z - start.z) >= 24 || Math.abs(p.y - start.y) >= 8)
                && heuristic(start) - heuristic(p) >= 8;
    }

    public boolean partial() {
        return partial;
    }

    private void finish(Node node) {
        LinkedList<Edge> path = new LinkedList<>();
        for (Node c = node; c.parent != null; c = c.parent) path.addFirst(c.edge);
        result = path;
        done = true;
    }

    public RoutePlanner(World world, Point start, Point target, Settings settings) {
        this.world = world;
        this.start = start;
        this.target = target;
        this.settings = settings;
        Key key = key(start);
        cost.put(key, 0d);
        open.add(new Node(key, start, 0, heuristic(start), null, null));
    }

    private static double square(double n) {
        return n * n;
    }

    private Key key(Point p) {
        return new Key(
                (int) Math.round(p.x - start.x),
                (int) Math.round(p.y * 8),
                (int) Math.round(p.z - start.z));
    }

    private double heuristic(Point p) {
        return Math.max(0, Math.hypot(p.x - target.x, p.z - target.z) - 3)
                + Math.abs(p.y - target.y) * 1.5;
    }

    public int step(int budget) {
        int used = 0;
        while (!done && used < budget) {
            Node n = open.poll();
            if (n == null || expanded >= settings.total) {
                done = true;
                break;
            }
            if (!closed.add(n.key)) continue;
            expanded++;
            used++;
            if (world.goal(n.p, target)) {
                finish(n);
                break;
            }
            if (segmentReached(n.p)) {
                partial = true;
                finish(n);
                break;
            }
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dz != 0) {
                        Point next = floor(n.p.x + dx, n.p.z + dz, n.p.y, 1.25, settings.safeDrop);
                        if (next != null) {
                            Edge e = new Edge(n.p, next, Kind.WALK, Math.max(n.p.y, next.y));
                            if (sweep(e, false))
                                offer(n, e, Math.hypot(dx, dz) + Math.abs(next.y - n.p.y) * .7);
                        }
                        if (settings.tier == 0 || dx != 0 && dz != 0) continue;
                        // 正常邻接被挡才考虑机械动作；禁止在平地跳着追人。
                        if (world.fits(new Point(n.p.x + dx, n.p.y, n.p.z + dz))) continue;
                        for (int distance = 2;
                                distance <= Math.max(settings.vault, settings.clear);
                                distance++) {
                            if (distance <= settings.vault) {
                                Point land =
                                        floor(
                                                n.p.x + dx * distance,
                                                n.p.z + dz * distance,
                                                n.p.y,
                                                settings.climb,
                                                settings.safeDrop);
                                if (land != null) {
                                    double crest = Math.max(n.p.y + 1.6, land.y + .1);
                                    Kind kind = crest - n.p.y <= 1.65 ? Kind.VAULT : Kind.CLIMB;
                                    if ((kind == Kind.VAULT
                                                    || settings.tier >= 2
                                                            && land.y - n.p.y <= settings.climb)
                                            && world.wall(
                                                    n.p,
                                                    dx,
                                                    dz,
                                                    kind == Kind.CLIMB ? land.y - n.p.y : 1)) {
                                        Edge e = new Edge(n.p, land, kind, crest);
                                        if (sweep(e, false))
                                            offer(
                                                    n,
                                                    e,
                                                    kind == Kind.VAULT
                                                            ? 6 + distance
                                                            : 11 + distance + land.y - n.p.y);
                                    }
                                }
                            }
                            if (settings.tier >= 3 && distance <= settings.clear) {
                                Point exit =
                                        new Point(
                                                n.p.x + dx * distance,
                                                n.p.y,
                                                n.p.z + dz * distance);
                                if (world.support(exit) && world.clearable(exit, n.p.y)) {
                                    Edge e = new Edge(n.p, exit, Kind.CLEAR, n.p.y);
                                    if (sweep(e, true)) offer(n, e, 22 + distance * 3);
                                }
                            }
                        }
                    }
        }
        return used;
    }

    private Point floor(double x, double z, double y, double up, double down) {
        return world.floor(x, z, y, up, down);
    }

    private void offer(Node n, Edge edge, double extra) {
        Point p = edge.to;
        if (Math.abs(p.x - start.x) > 36
                || Math.abs(p.z - start.z) > 36
                || Math.abs(p.y - start.y) > 12
                || world.banned(edge)) return;
        Key k = key(p);
        double c = n.cost + extra;
        if (c >= cost.getOrDefault(k, Double.POSITIVE_INFINITY)) return;
        cost.put(k, c);
        open.add(new Node(k, p, c, c + heuristic(p) * 1.15, n, edge));
    }

    public boolean sweep(Edge edge, boolean clear) {
        return sweep(world, edge, clear);
    }

    static boolean sweep(World world, Edge edge, boolean clear) {
        List<Point> points = edge.points();
        for (int j = 1; j < points.size(); j++) {
            Point a = points.get(j - 1), b = points.get(j);
            int count = Math.max(1, (int) Math.ceil(a.distance(b) / .2));
            for (int i = 1; i <= count; i++) {
                Point p = a.mix(b, i / (double) count);
                if (clear) {
                    if (!world.clearable(p, edge.from.y) || !world.support(p)) return false;
                } else if (!world.fits(p)) return false;
            }
        }
        return world.support(edge.to);
    }

    public boolean done() {
        return done;
    }

    public List<Edge> result() {
        return result;
    }

    public int expanded() {
        return expanded;
    }
}
