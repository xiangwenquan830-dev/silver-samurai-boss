# Silver Samurai: Battle-Damaged Red Blade | 银色武士：战损红刃

A large two-phase boss for **Minecraft Java 1.20.1 / Forge 47.4.0+ / Java 17**.
Standalone gameplay with optional Wolverine: Claws & Healing integration.

## Installation
Put the release JAR into `mods`, remove previous Silver Samurai JARs, and restart the game. Install on both client and server. Wolverine is optional; no other mod JAR is bundled here.

## Summoning
```mcfunction
/summon silver_samurai:silver_samurai ~ ~ ~
```
This release is command-summoned. Natural structures, activation devices, survival summoning recipes and dedicated boss loot are **not implemented**.

## Features
- Dual-sword first phase and battle-damaged, two-handed single-sword second phase.
- Authored drawing, sheathing, combat, transition, traversal and death animations.
- Red-blade attacks, configurable terrain interaction, target pursuit, wall climbing and pursuit jumps.
- Configurable combat and targeting. Optional Wolverine claw-breaking and regeneration interaction.
- r61 reduces excessive front-skirt flipping during climbing and separates the belt buckle from the skirt. The r60 landing improvements are retained.

## Configuration
Server settings: `silver-samurai-server.toml` in the world's `serverconfig` directory. See [CONFIG.md](CONFIG.md). Back up worlds before changing terrain-destruction settings. Some boss attacks can modify blocks when enabled.

## Build
```sh
./gradlew build
```
Windows: `gradlew.bat build`. The first build requires internet access to resolve Forge and Gradle dependencies. Authoring models are in `model-source`; the build extracts runtime data without editing originals.

## Tests
```sh
./gradlew runGameTestServer -PtestSuite=full -PgameTestDir=run-regression-fresh
```
Use a new test directory for each run. Optional compatibility tests require separately obtained dependencies; they are not included. Read [VALIDATION.md](VALIDATION.md) for the actual release verification boundary.

## 中文
银色武士双阶段Boss，当前以指令召唤。支持独立游玩；金刚狼联动为可选功能。现阶段尚无自然生成建筑、生存召唤配方和专属掉落。安装时只保留一个版本，客户端与服务器均需安装。模型源文件和测试源码均在本仓库中。

## Credits and license
Original project metadata credits AutoMods. Published and maintained by this repository owner. Existing All Rights Reserved licensing is retained; public source visibility is not an open-source license. Third-party notices, including the Gradle wrapper notices, remain applicable to their files.

Unofficial fan project; not affiliated with Marvel, Mojang or Microsoft.
