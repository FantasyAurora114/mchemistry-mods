# MCI 物理拆包实施清单

框架：主体 **MChemistry（化学时代）**（实验室内容）+ 附属
**MCI（化学时代·化工）**（工业/矿石内容）。MCI 的前置模组是 MChemistry。

## 1. MCI 工程的前置依赖声明

MCI 的 `neoforge.mods.toml` 必须声明 MChemistry 为 required 依赖，且
`ordering = "AFTER"`（主体先加载，MCI 后加载，保证 ChemistryAPI 已注册）：

```toml
modId="mchemistry"
type="required"
versionRange="[0.1.0,)"
ordering="AFTER"
side="BOTH"
```

（此外 MCI 同样声明 neoforge / minecraft required；Mekanism 保持 optional。）

## 2. 代码迁移边界

### 留在主体 MChemistry（现状包 `com.example.chemistry`）
- 物质体系：元素/固体/液体/气体数据 + `ChemistryAPI` / `CoreSubstances` 种子
- 实验室仪器：试管/烧杯/烧瓶/坩埚/蒸发皿、酒精灯/铁架台/三脚架、洗气瓶、
  水槽、橡胶管/玻璃导管/橡胶塞、胶头滴管、药匙/镊子、燃烧匙、护目镜
- 实验室系统：`ReactionEngine`、`TemperatureSystem`、`VesselHeating`、
  `GasFlowEngine`、反应现象、闻试剂、振荡、解锁记录 `ReactionUnlocks`
- 基础容器物品族生成（`DynamicItemRegistrar`）

### 迁往附属 MCI
- 矿石全部内容：`Ores.java`、矿石方块/物品/世界生成 JSON、`VANILLA_ORES`
  原版铁铜金掉落覆盖与化学链、生成器 `work/gen_ores.py` + `clean_ores.py`
- 工业合成塔：`SynthesisTowerBlockEntity/Menu/Screen`、塔内气体/液体罐、
  Mekanism 塔集成、`ModMenus` 对应注册
- MCI 专属物品族：矿石散装/广口瓶物品（由 MCI 自己的 DynamicItemRegistrar 生成）

### 共享 API（留在主体，供 MCI 调用）
- `ChemistryAPI`：registerElement/Solid/Liquid/Gas/Reaction（MCI 在自身
  构造器里注册矿石反应）
- `ReactionUnlocks`：主体在实验室反应完成时记录解锁，MCI 合成塔读取
- `api.goggles`：MCI 方块可复用护目镜信息接口

## 3. 主体 mod id 迁移注意

- 拆包时建议主体 mod id 改为 `mchemistry`（现为 `chemistry`），mod 显示名
  已改为 "MChemistry（化学时代）"
- mod id 改动会影响存档里的注册表 id（`chemistry:...` → `mchemistry:...`），
  需要数据迁移或接受旧存档失效；若想保留旧存档，可暂不换 mod id，
  MCI 依赖声明的 modId 用实际主体 id 即可

## 4. 构建方式

- 两个独立 Gradle 工程；MCI 通过 `compileOnly project(":mchemistry")` 或
  maven 依赖主体 jar
- MCI 开发环境 `runs` 里把主体 jar 放入 mods 目录（`neoForge.runs...mods`）

## 5. 已就位的部分

- `ChemistryAPI` 已是主体/附属解耦入口（附属构造器里注册数据）
- `MekanismBridge` 的"反射 + ModList.isLoaded 运行时隔离"模式可复制给
  MCI 检测主体是否加载
- `ReactionUnlocks` 按玩家持久化，MCI 合成塔已实现"实验室解锁后才能用"
- 矿石数据集中在生成器表（`ORES` / `VANILLA_ORES`），迁移 = 拷贝数据表 +
  把生成器输出目录指向 MCI 资源目录
