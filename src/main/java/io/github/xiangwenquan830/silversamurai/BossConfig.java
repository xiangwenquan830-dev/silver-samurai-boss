package io.github.xiangwenquan830.silversamurai;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/** 服务端世界配置；/samurai reload 将变更应用到已经存在的 Boss。 */
public final class BossConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue SIDE_SWEEP =
            flag("sideSweepEnabled", true, "高速近身目标启用单手侧向截击");
    public static final ForgeConfigSpec.IntValue SIDE_COOLDOWN =
            integer("sideSweepCooldownTicks", 24, 20, 400, "侧挥独立冷却，包含准备和收势");
    public static final ForgeConfigSpec.BooleanValue STOMP =
            flag("crowdStompEnabled", true, "贴身敌群触发抬腿震地，只伤害实体，永不破坏地形");
    public static final ForgeConfigSpec.IntValue STOMP_COUNT =
            integer("crowdStompMinimumEnemies", 3, 2, 32, "脚边达到此数量时触发震地");
    public static final ForgeConfigSpec.IntValue STOMP_COOLDOWN =
            integer("crowdStompCooldownTicks", 100, 40, 1200, "震地冷却，包含抬腿和收势");
    public static final ForgeConfigSpec.DoubleValue STOMP_TRIGGER_RANGE =
            number("crowdStompTriggerRadius", 2.4, 1, 4, "脚边敌群触发半径");
    public static final ForgeConfigSpec.DoubleValue STOMP_RADIUS =
            number("crowdStompRadius", 3.2, 1, 5, "落脚瞬间震地范围");
    public static final ForgeConfigSpec.DoubleValue STOMP_DAMAGE =
            number("crowdStompDamageMultiplier", .65, 0, 3, "基础攻击力倍率，仍受单次伤害上限约束，使用普通物理伤害");
    public static final ForgeConfigSpec.DoubleValue STOMP_PUSH =
            number("crowdStompKnockback", 1.2, 0, 3, "径向击退强度，尊重目标抗击退");
    public static final ForgeConfigSpec.DoubleValue HEALTH =
            number("health", 600, 1, 1024, "总生命值；遵守原版最大生命属性上限1024");
    public static final ForgeConfigSpec.DoubleValue PHASE_HEALTH =
            number("phaseThreshold", 300, 0, 1024, "二阶段阈值；不回血，应低于总生命值");
    public static final ForgeConfigSpec.DoubleValue ATTACK =
            number("attackDamage", 12, 0, 10000, "基础攻击力");
    public static final ForgeConfigSpec.DoubleValue FIRST =
            number("firstHitMultiplier", 1, 0, 100, "第一击倍率");
    public static final ForgeConfigSpec.DoubleValue SECOND =
            number("secondHitMultiplier", 1, 0, 100, "第二击倍率");
    public static final ForgeConfigSpec.DoubleValue FINISH =
            number("finisherMultiplier", 2.5, 0, 100, "终结倍率");
    public static final ForgeConfigSpec.DoubleValue RED =
            number("redBladeMultiplier", 2, 0, 100, "红刀额外倍率");
    public static final ForgeConfigSpec.DoubleValue PHYSICAL_REDUCTION =
            number("physicalReduction", .99, 0, 1, "一阶段物理减伤比例");
    public static final ForgeConfigSpec.DoubleValue ARMOR =
            number("phaseTwoArmor", 40, 0, 1000, "二阶段护甲");
    public static final ForgeConfigSpec.DoubleValue CLAW_BYPASS =
            number("fullRageClawBypass", 1, 0, 1, "满怒真钢爪绕过装甲比例");
    public static final ForgeConfigSpec.DoubleValue TRANSITION_REDUCTION =
            number("transitionReduction", 1, 0, 1, "转场减伤，1为无敌；虚空和清除始终有效");
    public static final ForgeConfigSpec.IntValue INVULNERABLE_TICKS =
            integer("transitionProtectionTicks", 120, 0, 120, "转场保护时长");
    public static final ForgeConfigSpec.DoubleValue ACQUIRE =
            number("acquireRange", 28, 1, 128, "主动索敌距离");
    public static final ForgeConfigSpec.DoubleValue RETAIN =
            number("retainRange", 40, 1, 192, "保持战斗距离");
    public static final ForgeConfigSpec.DoubleValue ACTIVATE =
            number("activationRange", 64, 1, 256, "附近非旁观玩家维持活动距离；创造是否维持警戒由creativeKeepsAlert控制");
    public static final ForgeConfigSpec.BooleanValue CREATIVE_ALERT =
            flag("creativeKeepsAlert", true, "创造玩家只维持观察活动，绝不作为伤害目标；无实际敌人超过脱战时长仍会收刀；旁观默认不维持");
    public static final ForgeConfigSpec.IntValue LAST_SEEN =
            integer(
                    "lastSeenTicks",
                    80,
                    0,
                    1200,
                    "失去视线后继续追向最后已知位置的刻数；到时持刀观察，lostTargetTicks决定放弃目标");
    public static final ForgeConfigSpec.IntValue LOST =
            integer("lostTargetTicks", 160, 20, 2400, "脱战时长");
    public static final ForgeConfigSpec.IntValue RECOVERY =
            integer("extraRecoveryTicks", 4, 0, 200, "动画慢收后额外反击窗口");
    public static final ForgeConfigSpec.DoubleValue RANGE_ONE =
            number("phaseOneRange", 5.5, 1, 12, "刀路距离上限，不扩大原始刀身");
    public static final ForgeConfigSpec.DoubleValue RANGE_TWO =
            number("phaseTwoRange", 6.5, 1, 14, "二阶段刀路上限");
    public static final ForgeConfigSpec.DoubleValue RANGE_HEAVY =
            number("heavyRange", 7, 1, 16, "重劈刀路上限");
    public static final ForgeConfigSpec.DoubleValue DASH =
            number("slashLunge", 1.25, 0, 3, "普通斩击总位移");
    public static final ForgeConfigSpec.DoubleValue HEAVY_DASH =
            number("heavyLunge", 1.75, 0, 4, "终结斩总位移");

    @Deprecated
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK =
            number("knockbackResistance", 1, 0, 1, "兼容旧配置保留，已停用：银色武士抗击退固定为1，修改此项不生效");

    public static final ForgeConfigSpec.IntValue SHIELD =
            integer("shieldDisableTicks", 60, 0, 1200, "普通盾牌失效时长");

    @Deprecated
    public static final ForgeConfigSpec.IntValue GUARD =
            integer("guardBreakFallbackTicks", 60, 1, 1200, "兼容旧配置保留，已停用：不再使用旧破防时长回退，断爪由独立机制管理");

    public static final ForgeConfigSpec.BooleanValue BYPASS_RESISTANCE =
            flag("redBypassResistance", false, "是否绕过抗性；默认否");
    public static final ForgeConfigSpec.BooleanValue BYPASS_ABSORPTION =
            flag("redBypassAbsorption", false, "是否绕过吸收生命；默认否");
    public static final ForgeConfigSpec.BooleanValue BYPASS_EFFECTS =
            flag("redBypassEnchantmentEffects", false, "是否绕过附魔减伤；默认否，不强制取消其他模组事件");
    public static final ForgeConfigSpec.BooleanValue PLAYERS =
            flag("hostileToPlayers", true, "主动攻击生存冒险玩家");
    public static final ForgeConfigSpec.BooleanValue MOB_AGGRO =
            flag("monstersTargetBoss", true, "让通常敌对玩家的生物选择Boss");
    public static final ForgeConfigSpec.BooleanValue RETALIATE = flag("retaliate", true, "受到攻击后反击");
    public static final ForgeConfigSpec.BooleanValue HUNT_MOBS =
            flag("huntMonsters", true, "主动猎杀敌对生物；旧世界请手动将原有false改成true");
    public static final ForgeConfigSpec.BooleanValue SAME_KIND =
            flag("friendlyFire", false, "允许同类互攻");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FRIENDS =
            list("friendlyEntities", List.of(), "友方实体ID，优先于敌方");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENEMIES =
            list("enemyEntities", List.of(), "额外敌方实体ID");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLASSIFICATION =
            list(
                    "damageClasses",
                    List.of(),
                    "覆盖格式 namespace:id=physical/fire/magic/other/bypass；未知伤害默认正常伤害");
    public static final ForgeConfigSpec.BooleanValue TERRAIN =
            flag("terrainDestruction", true, "红刀破坏地形，建议先在备份测试世界尝试");
    public static final ForgeConfigSpec.DoubleValue BREAK_RANGE =
            number("terrainRange", 7, 0, 16, "破坏距Boss的最大距离，仍需处在真实刀路");
    public static final ForgeConfigSpec.IntValue BREAK_MAX =
            integer("maxBlocksPerStrike", 24, 0, 256, "每击最大破坏数");
    public static final ForgeConfigSpec.IntValue BREAK_TICK =
            integer("maxBlocksPerTick", 6, 0, 32, "每刻最大破坏数");
    public static final ForgeConfigSpec.IntValue SCAN_TICK =
            integer("maxBlockChecksPerTick", 192, 8, 1024, "每刻最多检查的刀路方块");
    public static final ForgeConfigSpec.BooleanValue DROPS =
            flag("terrainDrops", false, "破坏是否掉落，默认关闭以减少卡顿");
    public static final ForgeConfigSpec.BooleanValue CONTAINERS =
            flag("breakContainers", false, "允许破坏容器，还需允许方块实体");
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITIES =
            flag("breakBlockEntities", false, "允许破坏带数据的方块");
    public static final ForgeConfigSpec.BooleanValue GRIEFING =
            flag("respectMobGriefing", true, "遵守世界mobGriefing规则");
    public static final ForgeConfigSpec.BooleanValue CLAIM_EVENTS =
            flag("respectProtectionEvents", true, "发送实体破坏和虚拟玩家BreakEvent，领地模组须支持这些事件");
    public static final ForgeConfigSpec.BooleanValue DENY_UNCERTAIN =
            flag("disableTerrainForUnknownClaims", false, "领地保护不确定时完全禁用红刀破坏");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PROTECTED =
            list(
                    "protectedBlocks",
                    List.of(
                            "minecraft:bedrock",
                            "minecraft:end_portal_frame",
                            "minecraft:reinforced_deepslate"),
                    "禁止破坏方块ID");
    public static final ForgeConfigSpec.IntValue DEBRIS =
            integer("debrisLifetimeTicks", 200, 20, 1200, "碎片落地后存留时长");
    public static final ForgeConfigSpec.IntValue PARTICLES =
            integer("particlesPerTick", 12, 0, 64, "单个Boss每刻刀光粒子上限");
    public static final ForgeConfigSpec.IntValue TARGETS =
            integer("maxTargetsPerStrike", 24, 1, 128, "单次挥砍结算目标上限");
    // 保持用户 r4 的追击速度、攻击速度和真实位移步幅。
    public static final ForgeConfigSpec.DoubleValue MOVE_SPEED =
            number("baseMovementSpeed", .3, .05, 1, "基础移动速度，加载与reload更新");
    public static final ForgeConfigSpec.DoubleValue CHASE_SPEED =
            number("chaseSpeedMultiplier", 1.25, .1, 3, "一阶段追击倍率");
    public static final ForgeConfigSpec.DoubleValue PHASE2_SPEED =
            number("phaseTwoSpeedMultiplier", 1.4, .1, 3, "二阶段追击倍率");
    public static final ForgeConfigSpec.DoubleValue ATTACK_RATE =
            number("attackPlaybackRate", 1.18, .5, 2, "沿用r4连招速度，刀路与位移同步");
    public static final ForgeConfigSpec.DoubleValue STRIDE_SCALE =
            number("walkStrideScale", 1.7, 1, 2.2, "重装步幅倍率；与原 walkCycleDistance 相乘，旧配置同样生效");
    public static final ForgeConfigSpec.DoubleValue STRIDE =
            number("walkCycleDistance", 1.9, 1, 10, "左右脚一次循环对应实际移动距离");
    public static final ForgeConfigSpec.IntValue CORPSE =
            integer("corpseHoldTicks", 100, 0, 2400, "3.8秒死亡动作播完后额外保留尸体；生成死亡状态时锁定");
    public static final ForgeConfigSpec.IntValue DEBRIS_FADE =
            integer("debrisFadeTicks", 60, 10, 400, "残骸像素溶解时长，结束后才移除");
    public static final ForgeConfigSpec.IntValue DEBRIS_LIMIT =
            integer("debrisMaxPerBoss", 8, 0, 8, "每次转场零件上限，包含不可拾取落刀；0关闭残骸");
    public static final ForgeConfigSpec.IntValue DEBRIS_WORLD_LIMIT =
            integer("debrisMaxLoadedPerLevel", 128, 0, 1024, "每个维度已加载残骸上限；达到上限跳过新生成，不删旧残骸");
    public static final ForgeConfigSpec.BooleanValue FEEDBACK =
            flag("combatFeedback", true, "金属脚步、装甲火花和接触地面的少量尘土；不晃动镜头");
    public static final ForgeConfigSpec.DoubleValue INCOMING_CAP =
            number("incomingDamageCap", 30, 0, 1000000, "最终单次扣血上限，正常减伤后应用；0关闭，虚空和管理员清除不受限");
    public static final ForgeConfigSpec.DoubleValue OUTGOING_CAP =
            number("outgoingDamageCap", 60, 0, 1000000, "基础攻击乘连招和红刀倍率后的输出上限；0关闭");
    public static final ForgeConfigSpec.IntValue REPATH =
            integer("repathTicks", 16, 8, 100, "重新规划最小间隔；移动目标超过2格才提前申请，仍受预算约束");
    public static final ForgeConfigSpec.IntValue NAV_NODES =
            integer("navigationNodesPerTick", 12, 2, 64, "每只Boss每刻最多展开的路线节点");
    public static final ForgeConfigSpec.IntValue NAV_WORLD =
            integer("navigationNodesPerLevelTick", 96, 8, 512, "单维度每刻全部Boss共享的路线节点上限");
    public static final ForgeConfigSpec.IntValue NAV_TOTAL =
            integer("navigationSearchLimit", 900, 100, 4000, "一次路线搜索总节点上限；只读取已加载区块");
    public static final ForgeConfigSpec.DoubleValue VAULT =
            number("vaultDistance", 3, 1.5, 4, "低障碍翻越最大水平距离");
    public static final ForgeConfigSpec.DoubleValue CLIMB =
            number("climbHeight", 4, 0, 5, "短机械攀爬最高格数；0禁用");
    public static final ForgeConfigSpec.IntValue TRAVERSE_COOLDOWN =
            integer("traversalCooldownTicks", 50, 10, 300, "翻越和攀爬共用冷却");
    public static final ForgeConfigSpec.DoubleValue SAFE_DROP =
            number("safeDropHeight", 3, .5, 5, "允许的最大下落高度，不跳向无支撑落点");
    public static final ForgeConfigSpec.BooleanValue CLEAR =
            flag("activeRedClearing", true, "二阶段主动清障，仍服从terrainDestruction及所有保护限制");
    public static final ForgeConfigSpec.DoubleValue CLEAR_RANGE =
            number("clearingDistance", 3, 1, 5, "逐段清障路线的单段距离，仍需真实刀路接触");
    public static final ForgeConfigSpec.IntValue CLEAR_STRIKE =
            integer("clearingBlocksPerStrike", 12, 1, 64, "清障每刀最多方块，还受总破坏上限限制");
    public static final ForgeConfigSpec.IntValue CLEAR_TICK =
            integer("clearingBlocksPerTick", 3, 1, 16, "清障每刻最多方块，还受总破坏上限限制");
    public static final ForgeConfigSpec.IntValue BREAK_WORLD =
            integer("terrainBlocksPerLevelTick", 24, 1, 256, "单维度所有Boss共享每刻破坏上限，攻击与清障共用");
    public static final ForgeConfigSpec.IntValue CLEAR_TIMEOUT =
            integer("clearingAttemptTicks", 180, 30, 600, "单段清障最长尝试时间，超时暂避该路线");
    public static final ForgeConfigSpec.IntValue FAILED_RETRY =
            integer("failedRouteRetryTicks", 120, 40, 1200, "失败路线和不可达目标的冷却；目标显著移动或障碍改变可提前重试");
    public static final ForgeConfigSpec.DoubleValue SPACING_STEP =
            number("spacingStepDistance", .7, .3, .9, "过近时单次短后撤/斜步距离；整段检查落脚、墙体和其他实体，不增加攻击距离");
    public static final ForgeConfigSpec.IntValue HIT_REACTION_COOLDOWN =
            integer("hitReactionCooldownTicks", 24, 12, 100, "局部受力反馈最短间隔，按实际扣血判断，不反复打断挥砍或攀爬");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IMMUNE_EFFECTS =
            list("immuneEffects", List.of("hordes:infected"), "Boss拒绝并清理的状态效果注册名，不影响其他实体");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IMMUNE_DAMAGE =
            list("immuneDamageTypes", List.of("hordes:infection"), "Boss免疫的专用伤害类型，不屏蔽虚空或管理员清除");
    public static final ForgeConfigSpec.BooleanValue CLOSE_DEFENSE =
            flag("closeDefenseEnabled", true, "单个近身敌人触发短踢，不破坏方块");
    public static final ForgeConfigSpec.IntValue CLOSE_COOLDOWN =
            integer("closeDefenseCooldownTicks", 80, 30, 400, "单体短踢冷却，还受近身动作共享冷却限制");
    public static final ForgeConfigSpec.DoubleValue RANGED_REDUCTION =
            number("rangedDamageReduction", .90, 0, 1, "远程伤害减免；与一阶段物理减免取较强者");
    public static final ForgeConfigSpec.DoubleValue PERIODIC_REDUCTION =
            number("periodicDamageReduction", .75, 0, 1, "持续负面伤害减免；与其他百分比减免取较强者");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RANGED_TYPES =
            list("rangedDamageTypes", List.of(), "补充远程伤害类型注册ID，适配不带投射物标记的光束等");
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PERIODIC_TYPES =
            list(
                    "periodicDamageTypes",
                    List.of("minecraft:wither", "minecraft:on_fire"),
                    "持续伤害类型注册ID；效果自身每刻造成的伤害也会识别，普通瞬间魔法不按持续伤害处理");
    public static final ForgeConfigSpec.IntValue CLOSE_SHARED =
            integer(
                    "closeDefenseSharedCooldownTicks",
                    80,
                    40,
                    1200,
                    "短踢与群体解围共享冷却，防止交替踢腿；冷却不阻止挥刀和换位");
    public static final ForgeConfigSpec.BooleanValue RED_WOUND =
            flag("redBladeHealingSuppression", true, "有效红刃伤害施加再生抑制，包括经过治疗事件的金刚狼自愈");
    public static final ForgeConfigSpec.DoubleValue RED_WOUND_REDUCTION =
            number("redBladeHealingReduction", .75, 0, 1, "再生抑制比例，0.75为治疗仅保留25%；多个来源不重复相乘");
    public static final ForgeConfigSpec.IntValue RED_WOUND_TICKS =
            integer("redBladeHealingSuppressionTicks", 120, 1, 2400, "再生抑制持续刻数，命中刷新、不累加时长；20刻为1秒");
    public static final ForgeConfigSpec.DoubleValue COMBAT_TEMPO =
            number(
                    "combatTempoMultiplier",
                    1.15,
                    1,
                    1.5,
                    "连招整体节奏倍率，乘以既有attackPlaybackRate；新字段让旧世界也采用加快节奏，设1恢复旧倍率");
    public static final ForgeConfigSpec.IntValue TARGET_SCAN =
            integer("targetScanIntervalTicks", 4, 2, 20, "已有目标时重新评估间隔；无目标时每2刻检查，当前挥刀期间不切换目标");
    public static final ForgeConfigSpec.DoubleValue STRIKE_PAUSE =
            number(
                    "minimumInterStrikePauseSeconds",
                    .50,
                    .50,
                    .8,
                    "两刀有效窗口之间最小间隔；至少0.5秒保留正常受伤保护，不靠清除无敌帧加速");
    public static final ForgeConfigSpec.BooleanValue WALL_CLIMB =
            flag("wallClimbEnabled", true, "启用高楼攀爬及下坠抓握；仅检查已加载区块");
    public static final ForgeConfigSpec.IntValue WALL_HEIGHT =
            integer("wallClimbMaxHeight", 48, 5, 128, "单段高楼攀爬最高格数");
    public static final ForgeConfigSpec.IntValue WALL_TIME =
            integer("wallTraversalTimeoutTicks", 1800, 200, 6000, "墙面动作总时限，失败后恢复重力并重规划");
    public static final ForgeConfigSpec.DoubleValue RANGED_SECOND =
            number("rangedDamagePerSecondCap", 6, 0, 10000, "任意连续20刻最终远程伤害总上限；0关闭；虚空与清除不受限");
    public static final ForgeConfigSpec.BooleanValue UNLIMITED_PURSUIT_DROP =
            flag(
                    "unlimitedPursuitDrop",
                    true,
                    "追击下跳不设高度上限，仍须确认完整落点并避开火焰、深水和虚空；关闭后使用pursuitDropHeight");
    public static final ForgeConfigSpec.DoubleValue PURSUIT_DROP =
            number("pursuitDropHeight", 20, 3, 64, "追击允许跳下的最大落差；需要确认安全落点");
    public static final ForgeConfigSpec.DoubleValue PLAYER_ACQUIRE =
            number("playerAcquireRange", 96, 1, 192, "玩家独立发现范围；仍需视线，不扩大怪物扫描体积");
    public static final ForgeConfigSpec.DoubleValue PLAYER_RETAIN =
            number("playerRetainRange", 160, 1, 256, "已发现玩家的保留范围，至少不低于玩家发现范围");
    public static final ForgeConfigSpec.IntValue PLAYER_MEMORY =
            integer("playerLastSeenTicks", 400, 0, 2400, "遮挡后追踪玩家最后可见位置的时长，20刻为一秒；不会隔墙实时读取目标新位置");
    public static final ForgeConfigSpec.IntValue PLAYER_LOST =
            integer("playerLostTargetTicks", 600, 20, 3600, "玩家持续不可见时放弃目标的时长，不低于其记忆时长");
    public static final ForgeConfigSpec.DoubleValue PLAYER_PRIORITY =
            number("playerTargetPriority", 24, 0, 60, "可见玩家的选择加权；近身可攻击敌人的紧急优先级仍保留");
    public static final ForgeConfigSpec.DoubleValue RANGED_HIT =
            number("rangedDamagePerHitCap", 3, 0, 10000, "最终单发远程扣血上限，0关闭；多名射手仍共用每秒总上限");
    public static final ForgeConfigSpec SPEC = B.build();

    static double retentionRange(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof net.minecraft.world.entity.player.Player
                ? Math.max(PLAYER_ACQUIRE.get(), PLAYER_RETAIN.get())
                : retentionRange();
    }

    static int memoryTicks(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof net.minecraft.world.entity.player.Player
                ? PLAYER_MEMORY.get()
                : LAST_SEEN.get();
    }

    static int lostTicks(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof net.minecraft.world.entity.player.Player
                ? Math.max(PLAYER_LOST.get(), PLAYER_MEMORY.get())
                : LOST.get();
    }

    static float attackRate() {
        return (float) Math.max(.5, Math.min(2, ATTACK_RATE.get() * COMBAT_TEMPO.get()));
    }

    /** An acquired target must not become invalid on the very next tick. */
    static double retentionRange() {
        return Math.max(ACQUIRE.get(), RETAIN.get());
    }

    private static ForgeConfigSpec.DoubleValue number(
            String k, double v, double lo, double hi, String c) {
        return B.comment(c).defineInRange(k, v, lo, hi);
    }

    private static ForgeConfigSpec.IntValue integer(String k, int v, int lo, int hi, String c) {
        return B.comment(c).defineInRange(k, v, lo, hi);
    }

    private static ForgeConfigSpec.BooleanValue flag(String k, boolean v, String c) {
        return B.comment(c).define(k, v);
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> list(
            String k, List<String> v, String c) {
        return B.comment(c).defineListAllowEmpty(List.of(k), () -> v, x -> x instanceof String);
    }

    private BossConfig() {}
}
