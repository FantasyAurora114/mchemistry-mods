# Chemistry Mod（NeoForge 框架）

一个面向 Minecraft 1.21.10 / NeoForge 21.10 的化学模组框架骨架。当前版本的目标是：

- 跑通完整的开发流程（编译、运行、资源加载）
- 展示模组的标准分层结构
- 提供可扩展的元素 / 化合物 / 合成配方数据模型

## 已实现内容

- 50 种常见元素（氢到铀：11 种气体、28 种金属、5 种类金属、6 种非金属）
  - 金属 / 类金属：**锭 / 粉 / 粒** 三种形态
  - 非金属：**粉 / 粒**（无锭）
  - 11 种气体元素（H、He、N、O、F、Ne、Cl、Ar、Kr、Xe、Rn）：仅 **玻封管**
  - 碱金属（Li / Na / K）：**锭 / 粒**（无粉）
- 同素异形体：红磷、白磷、石墨、臭氧
- 化学仪器：105 种（烧杯、各类烧瓶、量筒、滴定管、漏斗、冷凝管、
  坩埚、铁架台、天平、pH 计、色谱仪、滤纸/试纸等），图标由
  `work/gen_instruments.py` 批量生成
- 化合物、试管、烧杯、实验台（含 GUI 与合成）、化学水流体
- 元素 / 化合物 / 配方全部数据驱动，注册表与 JSON 由生成脚本保持同步

## 玩法设定

- **碱金属遇水爆炸**：把 ≥16 个锂 / 钠 / 钾锭丢进水里，会先冒气泡，
  约 2 秒后爆炸，锭越多威力越大（上限接近苦力怕）。

## 环境要求

- JDK 21
- Minecraft 1.21.10，NeoForge 21.10.64（由 Gradle 自动下载）

## 如何运行

```bash
./gradlew runClient   # 启动游戏
./gradlew runServer   # 启动服务端
./gradlew build       # 打包出 jar
```

首次运行会下载 Gradle 和 Minecraft/NeoForge 依赖，需要几分钟。

## 项目结构

```
src/main/java/com/example/chemistry/
├── ChemistryMod.java          # 模组入口，注册所有 DeferredRegister
├── ChemistryModClient.java    # 客户端入口（配置界面）
├── Config.java                # ModConfigSpec 配置
├── ModEvents.java             # 游戏事件（服务端启动时加载数据）
├── registry/
│   ├── ModItems.java          # 物品注册：元素、化合物、工具、流体桶
│   ├── ModBlocks.java         # 方块注册：实验台、流体方块
│   ├── ModBlockEntities.java  # 方块实体注册
│   ├── ModMenus.java          # 容器菜单注册
│   ├── ModFluids.java         # 流体注册
│   ├── ModFluidTypes.java     # 流体类型注册
│   └── ModCreativeTabs.java   # 创造模式物品栏
├── item/                      # ElementItem / CompoundItem
├── block/                     # LabTableBlock（带朝向的方块）
├── blockentity/               # LabTableBlockEntity（库存 + 合成逻辑）
├── menu/                      # LabTableMenu（容器逻辑）
├── client/                    # 界面渲染、流体渲染注册
└── data/                      # 元素/化合物/合成配方数据模型与加载器

src/main/resources/
├── assets/chemistry/          # 材质、模型、GUI、语言文件
└── data/chemistry/chemistry/  # elements.json / compounds.json / synthesis.json
```

## 数据文件格式

`data/chemistry/chemistry/synthesis.json` 定义实验台合成配方：

```json
[
  {
    "inputA": "chemistry:element_hydrogen_tube",
    "inputB": "chemistry:element_oxygen_tube",
    "output": "chemistry:compound_water",
    "count": 1
  }
]
```

配方在服务端启动时加载并缓存，`LabTableBlockEntity` 合成时查询。

元素数据（符号、中英文名、类别、状态、稳定性、颜色）由
`work/gen_elements.py` 生成，同时产出物品注册表 `Elements.java`、
`elements.json`、全部材质与模型。脚本内保留了完整 118 种元素的数据，
通过 `KEEP` 清单控制实际启用哪些；新增或修改元素请改脚本后重新生成。

## 下一步计划

- 化合物库扩展（数据驱动）
- 玻璃器皿 3D 模型（Blockbench）与液体容量
- 加热 / 冷却等反应条件
- 材料分解器、电解槽等机器

## 版权与作者标志

MChemistry / MCI 为闭源项目，原创内容保留所有权利。作者：
**FantasyAurora（FantasyAurora114）**；个人标志：**Observer / Anastasiya**。
署名出现在源码、模组信息和构建清单中，用于归属标识，不是密码学防伪。

正式发布的模组文件允许个人游戏和私人服务器使用；源码与素材的复制、修改、
再发布及商业使用需另获作者书面许可。详见仓库根目录 `LICENSE`。
第三方内容继续适用其原许可，详见 `THIRD_PARTY_NOTICES.md`。
