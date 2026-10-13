package cn.blockforge.generated.bossmodbossmodbossm;

/** Existing movement ownership precedence, shared in one place. true consumes this AI tick. */
final class TraversalCoordinator {
    static boolean tick(SamuraiEntity boss) {
        if (boss.pursuitJump().active()) {
            if ((boss.tickCount + boss.getId()) % BossConfig.TARGET_SCAN.get() == 0)
                boss.acquire(true);
            boss.pursuitJump().tick();
            return true;
        }
        if (boss.wallTraversal().active()) {
            boss.wallTraversal().tick();
            return true;
        }
        if (!boss.wallApproach().active()
                && !boss.terrainNavigation().terrainMotion()
                && boss.pursuitJump().tryStart()) {
            boss.pursuitJump().tick();
            return true;
        }
        if (boss.wallApproach().active()
                && !boss.terrainNavigation().terrainMotion()
                && !boss.onGround()
                && boss.fallDistance > .8f
                && !boss.terrainNavigation().support(SamuraiNavigation.point(boss.position())))
            boss.wallApproach().clear();
        if (!boss.wallApproach().active() && boss.wallTraversal().tryStart()) {
            boss.wallTraversal().tick();
            return true;
        }
        // 地形动作拥有移动直到安全落脚；没有观察者也不能半空切休眠或转场。
        if (boss.state() == SamuraiEntity.VAULT
                || boss.state() == SamuraiEntity.CLIMB
                || boss.terrainNavigation().terrainMotion()) {
            // Perception keeps running; a new target must not cancel a half-completed safe landing.
            if ((boss.tickCount + boss.getId()) % BossConfig.TARGET_SCAN.get() == 0)
                boss.acquire(true);
            if (boss.state() == SamuraiEntity.VAULT || boss.state() == SamuraiEntity.CLIMB)
                boss.terrainNavigation().traversalTick();
            else boss.terrainNavigation().finishTerrainStep();
            return true;
        }
        return false;
    }

    private TraversalCoordinator() {}
}
