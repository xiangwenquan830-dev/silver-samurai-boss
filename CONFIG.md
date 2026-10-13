# r34 配置

新增 combatTempoMultiplier=1.15、targetScanIntervalTicks=4、minimumInterStrikePauseSeconds=0.50，启动后自动补充。详情见 R34-GAZE.md。

# r33 配置变更

lastSeenTicks 现已接入实际逻辑；失去视线后最多追踪指定刻数，到时持刀观察，lostTargetTicks控制放弃目标。有效保持范围取 acquireRange 与 retainRange 的较大值。knockbackResistance、guardBreakFallbackTicks仅兼容保留并明确停用；抗击退固定为1。其他配置继承r32，详情见R33-PURSUIT.md。

## 既有配置与历史说明

适用：Minecraft Java 1.20.1 / Forge 47.4.x。先备份世界，再换模组。

## 安装与旧世界必须做的事

1. 关闭游戏或服务器，从 `mods` 中移走旧银色武士 JAR，只保留本轮 r33；联机客户端和服务器使用同一份。
2. 配置在单人 `.minecraft/saves/世界名/serverconfig/silver-samurai-server.toml`，服务器为 `world/serverconfig/silver-samurai-server.toml`。
3. **旧配置不会被新默认值覆盖。** 找到下面两行并改为 `true`，否则旧世界可能仍然只有怪物主动打 Boss，没有 Boss 主动追怪：

```toml
huntMonsters = true
monstersTargetBoss = true
```

4. 新增参数会在加载世界时补全。退出世界后编辑最稳妥；`/samurai reload` 将已加载的配置应用于当前 Boss 属性，不治疗。

## 本轮新增警戒设置

```toml
creativeKeepsAlert = true
```

- 默认 `true`：创造玩家不受攻击，但在 `activationRange` 内维持活动与已经拔出的刀；不会仅为创造观察者反复拔收刀。
- 旁观玩家不维持警戒。有效敌人在 `retainRange` 内时，即使暂时隔墙、不可达，也阻止收刀。
- 仅无观察者且无有效敌人连续达到 `lostTargetTicks`（默认160刻，即8秒），才能收刀；一阶段随后播放约3秒收刀，二阶段始终保留单刀。
- 没有玩家也没有敌人时休眠，不巡逻。按本轮新要求，只要附近存在有效敌人，就不使用旧的“无人立刻停战”分支。
- 新项在旧世界自动补齐，不删除或覆盖既有伤害、速度、阵营和破坏设置。
- `corpseHoldTicks` 仍从死亡动作完成后计时。半空死亡先受重力落地，不将空中等待算作地面死亡进度。
- 老存档缺少持刀字段时：一阶段仍按旧的未拔刀姿态安全恢复；保存过本轮字段后正确恢复持刀。旧版死尸没有表面快照时使用平地兼容值，建议不要把正在死亡的旧尸体作为新效果验收对象。

## 沿用的站位设置（不是本轮新增）

```toml
spacingStepDistance = 0.7
hitReactionCooldownTicks = 24
```

- `spacingStepDistance`：过近时一次后撤/斜步的目标距离，单位格，可选0.3～0.9。默认0.7，不增加攻击距离；前后与中间任何位置不能站稳就不走。
- `hitReactionCooldownTicks`：局部受击姿势最短间隔，单位刻（20刻约1秒），范围12～100。默认24刻约1.2秒；不改变伤害保护时间，不控制金属装甲音效，不让轻伤反复打断连招。
- 新配置进入旧世界时自动补全；不用删除旧配置，避免丢失你改过的伤害和阵营设置。
- 原 `combatFeedback = false` 同时关闭新增局部受力反馈；物理AI的退步、转身不关闭。

### 保持当前工程的速度，不整体提速

```toml
baseMovementSpeed = 0.3
chaseSpeedMultiplier = 1.25
phaseTwoSpeedMultiplier = 1.4
attackPlaybackRate = 1.18
walkCycleDistance = 1.9
extraRecoveryTicks = 4
```

以上是接续工程的原默认值，本轮未改。追击增加渐进起步、急弯减速，不提高最高速度。旧世界若已经自行调成更沉稳的参数，仍使用你的旧值。

`extraRecoveryTicks` 现在是在原连招动画完整慢收之后，叠加一阶段14刻/二阶段8刻的基础安全收势，最少8刻；默认额外收势约0.9/0.6秒。不会压缩有效挥砍或让红刀无间隔三连。

起手判断的距离并非伤害半径：中心间距通常以双刀约2.9格、单刀约3.1格为基准，按目标宽度、斩击和真实刀路修正；每刀伤害仍由原刀刃扫掠与原配置范围上限共同决定。近距带有0.3格退出余量，判断至少保持6刻；短后退16刻，不每刻随机换方向。退路受阻会检查后方、斜侧和纯侧向；两轮失败后等待80刻，再换侧重试，不永久放弃，也不无限空转。

调试状态新增「当前距离」「期望距离」「行为原因」。联机客户端与服务端必须使用同一份新版JAR；同步数据结构已增加，不支持混装旧版客户端。

## 两个独立伤害上限

```toml
incomingDamageCap = 30.0
outgoingDamageCap = 40.0
```

- `incomingDamageCap`：阶段减伤、护甲、抗性、附魔和吸收等正常结算之后，单个最终伤害事件扣血最多30。原本只扣0.2，仍扣0.2；不是保底伤害。虚空和管理员清除不受限。
- `outgoingDamageCap`：读取 Boss 当前基础攻击属性，乘该刀倍率，再乘二阶段红刀倍率，最后封顶40。默认二阶段24/24/36不变。
- **任意一项设为 `0.0`，仅关闭这一项上限。** 两者互不影响。
- 不重置目标全局受伤保护时间。各刀之间留足正常保护窗口；其他生物同一时刻打中目标，仍可能干扰这一刀，调试中会记录。
- 绕甲、破盾、金刚狼破防仍保留；默认不绕抗性、吸收和附魔减伤。

## 真实体型寻路和机械动作

碰撞箱保持1.65格宽、4.5格高。规划和实际移动都检查这一体型，不能把玩家能走的1格通道当成 Boss 通道。

```toml
repathTicks = 16
navigationNodesPerTick = 12
navigationNodesPerLevelTick = 96
navigationSearchLimit = 900
vaultDistance = 3.0
climbHeight = 4.0
traversalCooldownTicks = 50
safeDropHeight = 3.0
failedRouteRetryTicks = 120
```

- 优先普通走路、台阶、绕行；失败后增加翻越、短攀爬，最后才考虑二阶段清障。
- `climbHeight = 0.0` 禁用较高攀爬；低障碍翻越仍保留。
- 只在已加载区块中搜索。单次搜索最多900个节点，搜索边界约水平36格、上下12格，不为追人强行加载远处区块。
- 路线在数刻中分摊计算；全部 Boss 共用维度预算。移动目标相对上次规划点移动超过2格时重新申请，仍遵守最短重算间隔。
- 楼梯允许至少两个分离支点承重；流体、火、危险落点不会作为正常站立路线。
- 翻越与攀爬逐小段碰撞移动，不传送；墙或落点中途改变则中止并重算。机械状态不会同时攻击。
- 失败路线暂避，目标显著移动、附近障碍改变或冷却后再尝试。
- 复杂超长迷宫、极窄旋转楼梯或非标准模组碰撞面可能超出预算或半格落脚采样能力，报告为不可达，不强行穿模。

## 二阶段主动清障

```toml
terrainDestruction = true
activeRedClearing = true
clearingDistance = 3.0
clearingBlocksPerStrike = 12
clearingBlocksPerTick = 3
clearingAttemptTicks = 180
terrainBlocksPerLevelTick = 24
```

- `activeRedClearing = false` 只关闭主动开路；红刀战斗挥砍仍按原有总地形设置破坏。
- `terrainDestruction = false` 同时关闭战斗破坏与主动清障。
- 单段通道依据完整碰撞箱形成，包括脚前、胸前和头顶；使用保持双手握柄的专用单刀短切，按待清位置调整刀刃高度，短起手、有效斩击、短收势。
- 只破坏规划通道中被有效刀路接触的方块，逐段推进；脚底支撑不列入主动清障范围。
- 上限取主动清障设置与原有 `maxBlocksPerStrike` / `maxBlocksPerTick` 中较小值；所有 Boss 还共享维度破坏预算。
- 通道通行后立即停止，超时、禁止破坏或刀路连续够不到时改走其他路线；不会无限砍同一个方块。
- 原有 `protectedBlocks`、容器、方块实体、`mobGriefing` 和保护事件设置继续有效。
- 领地模组必须实际监听 Forge 实体破坏或 `BlockEvent.BreakEvent`。没有测试过你的领地模组时，先用 `disableTerrainForUnknownClaims = true` 或关闭总破坏。

## 双向索敌与休眠

```toml
hostileToPlayers = true
huntMonsters = true
monstersTargetBoss = true
retaliate = true
friendlyFire = false
friendlyEntities = []
enemyEntities = []
activationRange = 64.0
acquireRange = 28.0
retainRange = 40.0
lastSeenTicks = 80
lostTargetTicks = 160
```

- Boss 默认同时敌对生存/冒险玩家和通常敌对玩家的怪物。友方、同队、创造、旁观、死亡及其他维度实体被排除。
- 怪物方向添加目标选择任务，保留怪物本来的移动、近战或射箭方式；不强行抢走它已经有效的其他目标。
- 创造默认维持观察，生存/冒险可作为攻击目标；旁观不维持。附近存在有效敌人时维持活动；只有无观察者且无敌人持续超过延迟才休眠。
- 首次发现需要视线；遮挡后只向最后确认位置追踪，不更新墙后目标坐标。正在执行有效路线或清障时允许有限延长，不变成无限追踪。
- `enemyEntities = ["othermod:monster"]` 或实体标签 `silver_samurai:enemies` 扩展敌对类型；友方列表和 `silver_samurai:friends` 优先。
- 使用完全自定义大脑、硬编码目标或不使用普通目标任务的其他模组生物，不能保证自动接入；需要逐模组实测和专门兼容。

## 如何区分三类问题

```mcfunction
/samurai debug @e[type=silver_samurai:silver_samurai,sort=nearest,limit=1] true
/samurai status @e[type=silver_samurai:silver_samurai,sort=nearest,limit=1]
```

- **刀路未接触**：这一刀的真实刀刃扫掠没有碰到目标。状态中该刀接触数为0；不是靠无限扩大范围补伤害。
- **受伤保护拦截**：确实发生接触，但目标处于原版受伤保护窗口且 `hurt` 拒绝；同时装其他战斗模组时，事件取消也可能造成拒绝，要结合日志判断。
- **索敌失败/暂不可达**：没有发起斩击，报告会显示目标、遮挡时长、寻路层级、停滞和失败情况。
- 日志逐刀记录起始距离、计划突进、朝向、俯仰、接触时动画时间、接受与否、实际生命减少值。
- 调试默认关闭，用完后把同一指令末尾改为 `false`。

## r29：近身反击与收到的伤害

以下条目在原配置所在层级自动补入，无须删除旧配置：

```toml
closeDefenseEnabled = true
closeDefenseCooldownTicks = 40
rangedDamageReduction = 0.60
periodicDamageReduction = 0.75
rangedDamageTypes = []
periodicDamageTypes = ["minecraft:wither", "minecraft:on_fire"]
```

- `closeDefenseEnabled` 控制单目标近身短踢，群体解围仍由原有配置控制。
- 短踢冷却单位为游戏刻，40刻约两秒，允许30–400刻。
- 两项伤害减免范围0–1；与原有一阶段物理减免取较强者，保留后续护甲和最终伤害封顶。
- 投射物自动识别；其他模组不带投射物标签的远程伤害可用 `rangedDamageTypes` 补充**伤害类型注册ID**。
- 效果每刻执行时触发的伤害自动识别；在效果回调外单独触发的自定义持续伤害可用 `periodicDamageTypes` 补充。不是效果名称列表，减伤也不缩短负面效果持续时间。
- 通过直接修改生命值或强制死亡绕开伤害流程的效果，仍需要既有免疫列表或专项兼容，不能靠75%减伤自动拦截。
- 红刃充能时间跟随转场的握稳帧，不另外添加任意提前点亮的配置。

## 原死亡和残骸配置仍然有效

`corpseHoldTicks`、`debrisLifetimeTicks`、`debrisFadeTicks`、`debrisMaxPerBoss`、`debrisMaxLoadedPerLevel` 和 `combatFeedback` 不改名。跪倒停机、落稳后留存与像素渐隐均保留；死亡会中断新增加的机械动作和清障。
