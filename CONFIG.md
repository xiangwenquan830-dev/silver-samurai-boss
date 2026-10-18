# 银色武士 r62 配置说明（Forge 1.20.1）

本页以当前 `BossConfig` 为准。旧版本的调参说明已移至 [历史说明](docs/CONFIG-HISTORY.md)，不要用历史默认值覆盖正在使用的配置。

## 文件位置与生效方式

世界配置位于 `<世界目录>/serverconfig/silver-samurai-server.toml`。单人游戏在对应存档内，独立服务器在其世界目录内。`defaultconfigs` 只用于新世界的初始配置，不能替代既有存档配置。

替换 JAR 后完整重启游戏/服务器，客户端和服务器使用相同版本，只保留一个银色武士 JAR。升级不会自动覆盖你之前修改的数值。Forge 重读配置后，管理员可执行 `/samurai reload` 更新已存在 Boss 的属性；该命令不会治疗 Boss，最大生命降低时会截断当前生命。

本版保持 Forge 47.4.0+ / Java 17 要求，不新增其他加载器支持。

## 自定义伤害分类

**配置键为 `damageClasses`，不是 `damageClassification`。** 格式是 `"namespace:damage_type=分类"`，可用分类为 `physical`、`fire`、`magic`、`other`、`bypass`。ID 必须是伤害类型注册 ID，不是生物、枪械物品或投射物实体 ID。格式不正确的条目不会生效。

```toml
# 示例：将原版溺水显式归为普通伤害；确有需要再使用。
damageClasses = ["minecraft:drown=other"]
```

判定顺序：虚空和清除伤害强制 bypass；然后按列表中第一个匹配 ID 覆盖；之后检查火焰、魔法、物理标签，以及投射物/爆炸/摔落和直接近战来源；剩余伤害归 other。unknown 不应统一改成 physical，否则会把法术等非物理伤害错误减免 99%。

数据包也可向 `silver_samurai:physical`、`silver_samurai:magic` 伤害类型标签追加 ID。针对模组新增 ID 时，建议 `replace: false` 并使用可选条目，避免卸载该模组后缺失注册项。

TaCZ 的 `tacz:bullet`、`tacz:bullet_ignore_armor`、`tacz:bullet_void`、`tacz:bullet_void_ignore_armor` 以及其 bullets 标签已在独立枪弹防御逻辑中识别，通常无需手动添加。伤害分类覆盖并不关闭这条枪弹防御逻辑。其他枪械、炮塔或法术请按其实际注册 ID 配置；不能凭模组名称猜测 ID。

## 远程与持续伤害

`rangedDamageTypes` 可补充未被识别为投射物的远程伤害类型；`periodicDamageTypes` 补充持续伤害类型。减伤比例采用适用项中的最大值，不重复相乘。最终仍受对应单次及滚动 20 tick 远程伤害上限约束，多名射手共享每个 Boss 的窗口。直接修改生命值、移除实体或虚空/清除不等同于普通受伤事件，不能保证被这些配置拦截。

## 破盾与可选联动

红刀销毁正在使用的一面盾，保留背包备用盾；禁盾期间对玩家发送原版物品冷却，现有备用盾也会同步。禁盾后新获得的盾在尝试使用时同样被拒绝并补发剩余冷却。非玩家仍采用服务端禁盾时间戳。

金刚狼不是前置。当前仍使用旧版适配接口，本轮加强了方法返回类型、静态/实例性质和可写字段类型检查，不能证明未来版本语义永远兼容。公开 API 需要双方配套发布；不能仅靠放宽版本范围就宣称支持。独立模式与联动模式的实际测试范围见 VALIDATION.md。

## 当前完整配置表

此表从本版 BossConfig 声明核对生成；数值单位以说明为准，20 tick = 1 秒，减伤比例 0.99 = 99%。

| 配置键 | 默认值 | 范围/类型 | 作用 |
|---|---|---|---|
| `sideSweepEnabled` | `true` | true / false | 高速近身目标启用单手侧向截击 |
| `sideSweepCooldownTicks` | `24` | 20 … 400 | 侧挥独立冷却，包含准备和收势 |
| `crowdStompEnabled` | `true` | true / false | 贴身敌群触发抬腿震地，只伤害实体，永不破坏地形 |
| `crowdStompMinimumEnemies` | `3` | 2 … 32 | 脚边达到此数量时触发震地 |
| `crowdStompCooldownTicks` | `100` | 40 … 1200 | 震地冷却，包含抬腿和收势 |
| `crowdStompTriggerRadius` | `2.4` | 1 … 4 | 脚边敌群触发半径 |
| `crowdStompRadius` | `3.2` | 1 … 5 | 落脚瞬间震地范围 |
| `crowdStompDamageMultiplier` | `.65` | 0 … 3 | 基础攻击力倍率，仍受单次伤害上限约束，使用普通物理伤害 |
| `crowdStompKnockback` | `1.2` | 0 … 3 | 径向击退强度，尊重目标抗击退 |
| `health` | `600` | 1 … 1024 | 总生命值；遵守原版最大生命属性上限1024 |
| `phaseThreshold` | `300` | 0 … 1024 | 二阶段阈值；不回血，应低于总生命值 |
| `attackDamage` | `12` | 0 … 10000 | 基础攻击力 |
| `firstHitMultiplier` | `1` | 0 … 100 | 第一击倍率 |
| `secondHitMultiplier` | `1` | 0 … 100 | 第二击倍率 |
| `finisherMultiplier` | `2.5` | 0 … 100 | 终结倍率 |
| `redBladeMultiplier` | `2` | 0 … 100 | 红刀额外倍率 |
| `physicalReduction` | `.99` | 0 … 1 | 一阶段物理减伤比例 |
| `phaseTwoArmor` | `40` | 0 … 1000 | 二阶段护甲 |
| `fullRageClawBypass` | `1` | 0 … 1 | 满怒真钢爪绕过装甲比例 |
| `transitionReduction` | `1` | 0 … 1 | 转场减伤，1为无敌；虚空和清除始终有效 |
| `transitionProtectionTicks` | `120` | 0 … 120 | 转场保护时长 |
| `acquireRange` | `28` | 1 … 128 | 主动索敌距离 |
| `retainRange` | `40` | 1 … 192 | 保持战斗距离 |
| `activationRange` | `64` | 1 … 256 | 附近非旁观玩家维持活动距离；创造是否维持警戒由creativeKeepsAlert控制 |
| `creativeKeepsAlert` | `true` | true / false | 创造玩家只维持观察活动，绝不作为伤害目标；无实际敌人超过脱战时长仍会收刀；旁观默认不维持 |
| `lastSeenTicks` | `80` | 0 … 1200 | 失去视线后继续追向最后已知位置的刻数；到时持刀观察，lostTargetTicks决定放弃目标 |
| `lostTargetTicks` | `160` | 20 … 2400 | 脱战时长 |
| `extraRecoveryTicks` | `4` | 0 … 200 | 动画慢收后额外反击窗口 |
| `phaseOneRange` | `5.5` | 1 … 12 | 刀路距离上限，不扩大原始刀身 |
| `phaseTwoRange` | `6.5` | 1 … 14 | 二阶段刀路上限 |
| `heavyRange` | `7` | 1 … 16 | 重劈刀路上限 |
| `slashLunge` | `1.25` | 0 … 3 | 普通斩击总位移 |
| `heavyLunge` | `1.75` | 0 … 4 | 终结斩总位移 |
| `knockbackResistance` | `1` | 0 … 1 | 兼容旧配置保留，已停用：银色武士抗击退固定为1，修改此项不生效 |
| `shieldDisableTicks` | `60` | 0 … 1200 | 普通盾牌失效时长 |
| `guardBreakFallbackTicks` | `60` | 1 … 1200 | 兼容旧配置保留，已停用：不再使用旧破防时长回退，断爪由独立机制管理 |
| `redBypassResistance` | `false` | true / false | 是否绕过抗性；默认否 |
| `redBypassAbsorption` | `false` | true / false | 是否绕过吸收生命；默认否 |
| `redBypassEnchantmentEffects` | `false` | true / false | 是否绕过附魔减伤；默认否，不强制取消其他模组事件 |
| `hostileToPlayers` | `true` | true / false | 主动攻击生存冒险玩家 |
| `monstersTargetBoss` | `true` | true / false | 让通常敌对玩家的生物选择Boss |
| `retaliate` | `true` | true / false | 受到攻击后反击 |
| `huntMonsters` | `true` | true / false | 主动猎杀敌对生物；旧世界请手动将原有false改成true |
| `friendlyFire` | `false` | true / false | 允许同类互攻 |
| `friendlyEntities` | `[]` | 字符串列表 | 友方实体ID，优先于敌方 |
| `enemyEntities` | `[]` | 字符串列表 | 额外敌方实体ID |
| `damageClasses` | `[]` | 字符串列表 | 覆盖格式 namespace:id=physical/fire/magic/other/bypass；未知伤害默认正常伤害 |
| `terrainDestruction` | `true` | true / false | 红刀破坏地形，建议先在备份测试世界尝试 |
| `terrainRange` | `7` | 0 … 16 | 破坏距Boss的最大距离，仍需处在真实刀路 |
| `maxBlocksPerStrike` | `24` | 0 … 256 | 每击最大破坏数 |
| `maxBlocksPerTick` | `6` | 0 … 32 | 每刻最大破坏数 |
| `maxBlockChecksPerTick` | `192` | 8 … 1024 | 每刻最多检查的刀路方块 |
| `terrainDrops` | `false` | true / false | 破坏是否掉落，默认关闭以减少卡顿 |
| `breakContainers` | `false` | true / false | 允许破坏容器，还需允许方块实体 |
| `breakBlockEntities` | `false` | true / false | 允许破坏带数据的方块 |
| `respectMobGriefing` | `true` | true / false | 遵守世界mobGriefing规则 |
| `respectProtectionEvents` | `true` | true / false | 发送实体破坏和虚拟玩家BreakEvent，领地模组须支持这些事件 |
| `disableTerrainForUnknownClaims` | `false` | true / false | 领地保护不确定时完全禁用红刀破坏 |
| `protectedBlocks` | `["minecraft:bedrock",
                            "minecraft:end_portal_frame",
                            "minecraft:reinforced_deepslate"]` | 字符串列表 | 禁止破坏方块ID |
| `debrisLifetimeTicks` | `200` | 20 … 1200 | 碎片落地后存留时长 |
| `particlesPerTick` | `12` | 0 … 64 | 单个Boss每刻刀光粒子上限 |
| `maxTargetsPerStrike` | `24` | 1 … 128 | 单次挥砍结算目标上限 |
| `baseMovementSpeed` | `.3` | .05 … 1 | 基础移动速度，加载与reload更新 |
| `chaseSpeedMultiplier` | `1.25` | .1 … 3 | 一阶段追击倍率 |
| `phaseTwoSpeedMultiplier` | `1.4` | .1 … 3 | 二阶段追击倍率 |
| `attackPlaybackRate` | `1.18` | .5 … 2 | 沿用r4连招速度，刀路与位移同步 |
| `walkStrideScale` | `1.7` | 1 … 2.2 | 重装步幅倍率；与原 walkCycleDistance 相乘，旧配置同样生效 |
| `walkCycleDistance` | `1.9` | 1 … 10 | 左右脚一次循环对应实际移动距离 |
| `corpseHoldTicks` | `100` | 0 … 2400 | 3.8秒死亡动作播完后额外保留尸体；生成死亡状态时锁定 |
| `debrisFadeTicks` | `60` | 10 … 400 | 残骸像素溶解时长，结束后才移除 |
| `debrisMaxPerBoss` | `8` | 0 … 8 | 每次转场零件上限，包含不可拾取落刀；0关闭残骸 |
| `debrisMaxLoadedPerLevel` | `128` | 0 … 1024 | 每个维度已加载残骸上限；达到上限跳过新生成，不删旧残骸 |
| `combatFeedback` | `true` | true / false | 金属脚步、装甲火花和接触地面的少量尘土；不晃动镜头 |
| `incomingDamageCap` | `30` | 0 … 1000000 | 最终单次扣血上限，正常减伤后应用；0关闭，虚空和管理员清除不受限 |
| `outgoingDamageCap` | `60` | 0 … 1000000 | 基础攻击乘连招和红刀倍率后的输出上限；0关闭 |
| `repathTicks` | `16` | 8 … 100 | 重新规划最小间隔；移动目标超过2格才提前申请，仍受预算约束 |
| `navigationNodesPerTick` | `12` | 2 … 64 | 每只Boss每刻最多展开的路线节点 |
| `navigationNodesPerLevelTick` | `96` | 8 … 512 | 单维度每刻全部Boss共享的路线节点上限 |
| `navigationSearchLimit` | `900` | 100 … 4000 | 一次路线搜索总节点上限；只读取已加载区块 |
| `vaultDistance` | `3` | 1.5 … 4 | 低障碍翻越最大水平距离 |
| `climbHeight` | `4` | 0 … 5 | 短机械攀爬最高格数；0禁用 |
| `traversalCooldownTicks` | `50` | 10 … 300 | 翻越和攀爬共用冷却 |
| `safeDropHeight` | `3` | .5 … 5 | 允许的最大下落高度，不跳向无支撑落点 |
| `activeRedClearing` | `true` | true / false | 二阶段主动清障，仍服从terrainDestruction及所有保护限制 |
| `clearingDistance` | `3` | 1 … 5 | 逐段清障路线的单段距离，仍需真实刀路接触 |
| `clearingBlocksPerStrike` | `12` | 1 … 64 | 清障每刀最多方块，还受总破坏上限限制 |
| `clearingBlocksPerTick` | `3` | 1 … 16 | 清障每刻最多方块，还受总破坏上限限制 |
| `terrainBlocksPerLevelTick` | `24` | 1 … 256 | 单维度所有Boss共享每刻破坏上限，攻击与清障共用 |
| `clearingAttemptTicks` | `180` | 30 … 600 | 单段清障最长尝试时间，超时暂避该路线 |
| `failedRouteRetryTicks` | `120` | 40 … 1200 | 失败路线和不可达目标的冷却；目标显著移动或障碍改变可提前重试 |
| `spacingStepDistance` | `.7` | .3 … .9 | 过近时单次短后撤/斜步距离；整段检查落脚、墙体和其他实体，不增加攻击距离 |
| `hitReactionCooldownTicks` | `24` | 12 … 100 | 局部受力反馈最短间隔，按实际扣血判断，不反复打断挥砍或攀爬 |
| `immuneEffects` | `["hordes:infected"]` | 字符串列表 | Boss拒绝并清理的状态效果注册名，不影响其他实体 |
| `immuneDamageTypes` | `["hordes:infection"]` | 字符串列表 | Boss免疫的专用伤害类型，不屏蔽虚空或管理员清除 |
| `closeDefenseEnabled` | `true` | true / false | 单个近身敌人触发短踢，不破坏方块 |
| `closeDefenseCooldownTicks` | `80` | 30 … 400 | 单体短踢冷却，还受近身动作共享冷却限制 |
| `rangedDamageReduction` | `.90` | 0 … 1 | 远程伤害减免；与一阶段物理减免取较强者 |
| `periodicDamageReduction` | `.75` | 0 … 1 | 持续负面伤害减免；与其他百分比减免取较强者 |
| `rangedDamageTypes` | `[]` | 字符串列表 | 补充远程伤害类型注册ID，适配不带投射物标记的光束等 |
| `periodicDamageTypes` | `["minecraft:wither", "minecraft:on_fire"]` | 字符串列表 | 持续伤害类型注册ID；效果自身每刻造成的伤害也会识别，普通瞬间魔法不按持续伤害处理 |
| `closeDefenseSharedCooldownTicks` | `80` | 40 … 1200 | 短踢与群体解围共享冷却，防止交替踢腿；冷却不阻止挥刀和换位 |
| `redBladeHealingSuppression` | `true` | true / false | 有效红刃伤害施加再生抑制，包括经过治疗事件的金刚狼自愈 |
| `redBladeHealingReduction` | `.75` | 0 … 1 | 再生抑制比例，0.75为治疗仅保留25%；多个来源不重复相乘 |
| `redBladeHealingSuppressionTicks` | `120` | 1 … 2400 | 再生抑制持续刻数，命中刷新、不累加时长；20刻为1秒 |
| `combatTempoMultiplier` | `1.15` | 1 … 1.5 | 连招整体节奏倍率，乘以既有attackPlaybackRate；新字段让旧世界也采用加快节奏，设1恢复旧倍率 |
| `targetScanIntervalTicks` | `4` | 2 … 20 | 已有目标时重新评估间隔；无目标时每2刻检查，当前挥刀期间不切换目标 |
| `minimumInterStrikePauseSeconds` | `.50` | .50 … .8 | 两刀有效窗口之间最小间隔；至少0.5秒保留正常受伤保护，不靠清除无敌帧加速 |
| `wallClimbEnabled` | `true` | true / false | 启用高楼攀爬及下坠抓握；仅检查已加载区块 |
| `wallClimbMaxHeight` | `48` | 5 … 128 | 单段高楼攀爬最高格数 |
| `wallTraversalTimeoutTicks` | `1800` | 200 … 6000 | 墙面动作总时限，失败后恢复重力并重规划 |
| `rangedDamagePerSecondCap` | `6` | 0 … 10000 | 任意连续20刻最终远程伤害总上限；0关闭；虚空与清除不受限 |
| `unlimitedPursuitDrop` | `true` | true / false | 追击下跳不设高度上限，仍须确认完整落点并避开火焰、深水和虚空；关闭后使用pursuitDropHeight |
| `pursuitDropHeight` | `20` | 3 … 64 | 追击允许跳下的最大落差；需要确认安全落点 |
| `playerAcquireRange` | `96` | 1 … 192 | 玩家独立发现范围；仍需视线，不扩大怪物扫描体积 |
| `playerRetainRange` | `160` | 1 … 256 | 已发现玩家的保留范围，至少不低于玩家发现范围 |
| `playerLastSeenTicks` | `400` | 0 … 2400 | 遮挡后追踪玩家最后可见位置的时长，20刻为一秒；不会隔墙实时读取目标新位置 |
| `playerLostTargetTicks` | `600` | 20 … 3600 | 玩家持续不可见时放弃目标的时长，不低于其记忆时长 |
| `playerTargetPriority` | `24` | 0 … 60 | 可见玩家的选择加权；近身可攻击敌人的紧急优先级仍保留 |
| `rangedDamagePerHitCap` | `3` | 0 … 10000 | 最终单发远程扣血上限，0关闭；多名射手仍共用每秒总上限 |
