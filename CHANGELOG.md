# r61 - Skirt and belt buckle correction

r61 - Skirt and belt buckle animation fix (Beta)

Reduces excessive front-skirt rotation and abrupt forward displacement in climbing-related animations for both phases. Repositions the belt buckle above the skirt and trims hidden excess volume behind the upper skirt plate. Combat timing, non-skirt animation tracks, textures, configuration and Java gameplay classes are unchanged.

Offline checks confirmed matching skirt poses at the main climbing clip boundaries and no buckle/skirt intersections in the sampled clips. Some skirt/leg armor contact remains in raised-knee poses; this is not a zero-clipping release. No new in-game or multiplayer validation is claimed. The JAR is an asset-only update of the built r60 release, with model resources and version metadata replaced.

Minecraft 1.20.1 / Forge 47.4.0+ / Java 17. Replace the previous Silver Samurai JAR on both client and server; keep only one version and restart fully. Existing saves and settings are retained. Wolverine remains optional.

中文：修正双阶段攀爬前裙甲过度上翻和突出的平移，调整腰扣位置，保留原有战斗、贴图和配置。模型离线检查通过，局部抬膝仍有腿甲接触；本轮未进行游戏内或联机复测。替换旧版银武士JAR并完整重启，不要同时安装两个版本。

---

# 1.0.0-r60-landing (Beta)

- Slightly deeper landing compression.
- Wider dual-sword landing arm stance.
- Single-sword elbow spacing retains the two-handed handle grip.
- Fixed intermediate-frame hand-spacing error found in regression testing.
- Retains r59 roof pursuit and all existing combat systems.
