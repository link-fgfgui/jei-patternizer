# JEI Patternizer 实现计划

> 前置已完成：MultiLoader MDK（抄自 jeicrafter）已能 `./gradlew build` 通过（common / fabric / forge）。
> 本文只规划编码功能，不在本阶段落代码。

## 0. 现状与边界

| 项 | 状态 |
| --- | --- |
| 加载器 | 1.20.1 Forge 47.2.30 + Fabric 0.16.10，Java 17 |
| 前置 | JEI 15.21.0.148+、**JEI Crafter 1.0.0+**（`mavenLocal`） |
| 运行时 | 纯客户端；合法 GUI 交互，不发作弊包 |
| 已有骨架 | 平台 SPI、JSON 配置（延时/音效/模拟点击）、N 键、Screen 键盘钩子 |
| 未实现 | 配方树收集、JEI 转移、AE2/RS 编码、去重记忆、防关 GUI |

EMI-Patternizer 是 1.21.1 NeoForge + EMI BoM。本模组是 **1.20.1 + JEI + jeicrafter**，不能照搬类名，只复用思想。

## 1. 设计哲学（从 EMI-Patternizer 继承）

1. **极致减负**：一次按键把整棵配方树编成样板，而不是玩家逐条点 `+` / Encode。
2. **纯客户端合法交互**：Fill → Encode → QUICK_MOVE，全部走原有 GUI / JEI transfer。
3. **软依赖**：AE2 / RS 任一缺失都不崩溃（`Class.forName` 缓存 + Mixin plugin）。
4. **增量去重**：`EncodedItems` 记住已编码产物；打开样板访问终端时反向灌入。

和 EMI 版的关键差异：

| EMI-Patternizer | JEI Patternizer |
| --- | --- |
| `BoM.tree` + craftingMode | **jeicrafter `RecipeGraph`**（默认走 JEI 配方书签树） |
| `EmiRecipeFiller.performFill` | JEI `RecipeTransferUtil.transferRecipe` / `IRecipeTransferManager`（jeicrafter 已验证可联机） |
| 固定 `CompletableFuture` 延时链 | **主线程状态机**（jeicrafter `BookmarkActionExecution.tick` 同款），槽位变化推进，延时只作超时兜底 |
| RS2 `PatternGridScreen` | 1.20.1 是 **RS 1.x**，API 与 RS2 完全不同，需单独适配 |

**不要**把编码挂进 `JeiCrafterApi.registerAction`：Crafter 的 action 会先 `MaterialAnalyzer` 等背包材料齐了再合成。样板编码是往编码终端填**幽灵配方**，不消耗背包材料。编码流水线是独立会话。

## 2. 数据流

```
玩家在 JEI 书签出一棵配方树
        │
        ▼
打开 AE2 Pattern Encoding Terminal / RS Pattern Grid
        │
按 N（已接线 PatternizeInput）
        │
        ▼
RecipeTreeCollector  ← jeicrafter RecipeGraph（可替换）
  · 根：当前 JEI 配方页 / 焦点物品
  · DFS 展开 children（RecipeRequest.forDependency）
  · identity 去重、按 category 聚类、过滤 EncodedItems
        │
        ▼
PatternizerStateMachine（主线程 tick）
  IDLE → FILL → WAIT_FILL → ENCODE → WAIT_ENCODED → MOVE → NEXT → DONE
        │
        ├── PatternTerminalApi.encode()
        ├── handleInventoryMouseClick(QUICK_MOVE, encodedSlot)
        └── EncodedItems.add(output ids)
        │
打开 Pattern Access Terminal / RS Crafter Manager
        │
        ▼
ReloadMemory：延时读取已有样板 → 灌 EncodedItems
```

## 3. 模块切分

建议包结构（都在 `common`，平台只留入口）：

```
io.github.linkfgfgui.jeipatternizer
  client/
    JeiPatternizerKeys          // 已有
    PatternizeInput             // 已有钩子 → 改成启动会话
    PatternizerSession          // 状态机
    RecipeTreeCollector         // 调 jeicrafter RecipeGraph
    EncodedItems                // HashSet<String> 产物 id
    ReloadMemory                // 打开访问终端时同步
  integrated/                   // 注意 EMI 源码拼写是 intergrated，这里改正确
    PatternTerminalApi          // encode / slot / count
    AppliedEnergistics2
    RefinedStorage
  mixin/
    MixinPlugin                 // shouldApplyMixin(ae2/refinedstorage)
    AbstractContainerScreenMixin
    ae2/*Accessor
    refinedstorage/*Accessor
    jei/MixinJeiRecipesGui      // 拿“当前配方页”（可参考 jeicrafter 同名 mixin）
```

Gradle：AE2 / RS 一律 `compileOnly`；`fabric`/`forge` 的 run 用 `modLocalRuntime` / `modRuntimeOnly` 可选拉一份方便调试。Mixin json 挂 `plugin`。

## 4. 分阶段任务

### 阶段 A — 配方树收集（不碰终端）

**目标**：按 N 时能在日志里打出将要编码的 `RecipeStep` 列表。

1. 根选择（优先级）：
   - JEI 当前配方页（`IRecipesGui` / mixin，与 jeicrafter `MixinJeiRecipesGui` 同类）；
   - 否则焦点物品 `RecipeRequest.forItem`；
   - 否则遍历全部配方书签作为多个根。
2. 用 `JeiCrafterApi.builtinRecipeGraph()`（或已注册的更高优先级 graph）递归：
   - `supports` 为真的 graph **独占整棵会话**（jeicrafter 语义，不要自己再 fallthrough）。
   - 子节点：`RecipeRequest.forDependency(material, parent)`。
   - 环与深度：复用 jeicrafter 的 `identity()` + MAX_DEPTH。
3. 收集策略与 EMI 对齐：
   - 扁平化整棵树；
   - `Comparator` 按 `recipeCategory().getRecipeType()` 聚类；
   - 输出物已在 `EncodedItems` 则跳过；
   - 同一 `identity` 只编码一次。
4. **不**调用 `JeiCrafterApi.run()`，避免走进合成台 / 工作站 action。

验收：书签「木板←原木←橡木原木」时，日志按层列出 2～3 条配方，重复书签不重复列出。

### 阶段 B — JEI 配方填入编码终端

**目标**：在 AE2 编码终端打开时，把一条 `RecipeStep` 填进编码格。

1. 确认当前 `Screen` 是合法编码界面（`PatternTerminalApi.isValidEncodingScreen`）。
2. 调用 JEI transfer：

   ```java
   RecipeTransferUtil.transferRecipe(
       runtime.getRecipeTransferManager(),
       player.containerMenu,
       step.recipeLayout(),
       player,
       false
   );
   ```

   AE2 的 JEI handler 会把配方写进 encoding slots（幽灵物品，不扣背包）。
3. 失败则聊天栏提示并中止会话（空白样板不足、类别不被终端支持等）。
4. 配置 `playSound`：fill 成功播 `UI_BUTTON_CLICK`。

验收：手动打开编码终端，书签一条工作台配方，按 N 后编码格出现对应输入/输出（哪怕还没点 Encode）。

### 阶段 C — 终端抽象 + 编码 + 取出

**目标**：Fill 之后真正产出一张样板并 QUICK_MOVE 到背包。

`PatternTerminalApi`：

```java
void encode(boolean simulateClick);
int getEncodedPatternSlot();
long importExistingPatterns(Level level); // 写入 EncodedItems，返回样板数
```

**AE2 1.20.1（优先）**

- 界面：`appeng.client.gui.me.items.PatternEncodingTermScreen`
- 编码：`PatternEncodingTermMenu.encode()`；`simulateClick=true` 时点 `encodePattern` 按钮（WidgetContainer accessor）
- 槽位：`SlotSemantics.ENCODED_PATTERN` via `AEBaseMenu.slotsBySemantic`
- 记忆：`PatternAccessTermScreen` + `PatternDetailsHelper.decodePattern`
- Mixin 与 EMI 版同类：`AEBaseMenuAccessor` / `AEBaseScreenAccessor` / `PatternAccessTermScreenAccessor` / `WidgetContainerAccessor`
- Maven：`org.appliedenergistics:appliedenergistics2`（具体 15.x 版本在加依赖时再钉；`compileOnly`）
- MixinPlugin：`isModLoaded("ae2")`

**RS 1.20.1（第二优先，API 与 EMI 的 RS2 不同）**

- 1.20.1 是 Refined Storage **1.12.x**，没有 RS2 的 `PatternGridScreen` / `sendCreatePattern`。
- 需要反编译/对照 1.12.x：Pattern Grid 的创建按钮、输出槽、Crafter Manager 读样板。
- 在 AE2 通路稳定前，RS 可以只做 `isValidEncodingScreen=false` 的空实现，避免半吊子 mixin 崩客户端。

**状态机（不要复制 EMI 的三重 delayedExecutor）**

| 状态 | 进入条件 | 动作 | 下一状态 |
| --- | --- | --- | --- |
| FILL | 有下一条 recipe | JEI transfer | WAIT_FILL |
| WAIT_FILL | 编码格出现输入 **或** `delayPerOperation` 超时 | — | ENCODE |
| ENCODE | — | `api.encode(simulateClick)` | WAIT_ENCODED |
| WAIT_ENCODED | 输出槽出现编码样板 **或** 超时 | — | MOVE |
| MOVE | — | `handleInventoryMouseClick(QUICK_MOVE)` | WAIT_MOVE |
| WAIT_MOVE | 输出槽清空 **或** 超时 | 记入 EncodedItems | NEXT / DONE |

额外：

- 每步前检查空白样板 count > 0、背包有空位；否则 Toast/聊天栏中止。
- `operating=true` 时 mixin 取消 `AbstractContainerScreen.onClose`（至少 AE2 编码屏）。
- 超时用 `delayPerOperation` + `delayAdditionalPerPattern`，默认 60+20ms，与 EMI 配置项对齐（已写入 `JeiPatternizerConfig`）。
- 挂在 `Minecraft.tick` 尾部（自写 mixin，或若 jeicrafter 暴露 tick 钩再复用——**不要**去改 jeicrafter 的 AutoCraftManager 会话）。

验收：Powah / 原版合成树，编码终端里连续产出多张不重复样板，ESC 在会话中被挡住。

### 阶段 D — 记忆同步与体验

1. `ScreenEvent.Opening` / Fabric `ScreenEvents.BEFORE_INIT`：访问终端打开后 `delayBeforeRead`（默认 1000ms）再扫样板。
2. 聊天栏 `chat.jeipatternizer.loaded`（语言文件已有）。
3. 会话开始/结束日志；可选进度（`encoding 3/17`）。
4. 清空记忆：再次打开访问终端先 `EncodedItems.clear()` 再导入（EMI 行为）。

### 阶段 E — 鲁棒性（可后置）

- 流体/化学品样板：第一版只编码 `ItemStack` 输出；RS 若遇到空 ItemStack 直接 skip（EMI 对 RS 已有这个 guard）。
- 网络抖动：WAIT_* 以槽位为准，延时只是上限。
- 配置热重载：每次按 N 时 `JeiPatternizerConfig.load()`。
- 运行配置：forge/fabric `run/mods` 放 AE2 + JEI + jeicrafter + taglib。

## 5. 明确不做（本功能范围外）

- 自己实现全局 RecipeManager DAG（阶段 A 不够用再考虑）；优先书签树，这正是 jeicrafter 存在的理由。
- 把编码伪装成 `BookmarkAction` 去“合成”。
- 服务端模组、非法改容器内容。
- 1.21 / NeoForge 端口。
- 第一期就上 RS 1.x 完整适配（接口留好，实现可空）。

## 6. 风险

1. **AE2 JEI transfer 是否接受 Pattern Encoding Terminal**：jeicrafter 已在合成台/工作站上跑通 transfer；编码终端是 AE2 自己的 handler，需在阶段 B 用真实 AE2 验证。若 handler 要求 `maxTransfer` 或 processing/crafting 模式切换，要在 fill 前切到正确模式（crafting vs processing）。
2. **Processing 配方**：熔炉等需要 AE2 processing 模式；可能要按 `RecipeTypes.CRAFTING` vs 其它 category 点终端模式按钮（accessor）。
3. **RS 1.x 类名不确定**：阶段 C 先 AE2。
4. **jeicrafter 未发布远端 Maven**：构建依赖 `publishToMavenLocal`（README 已写）。
5. **JEI 内部类**：`RecipeTransferUtil` 在 `mezz.jei.common.transfer`，jeicrafter 已经 compileOnly 了 jei-common/gui/lib，本模组同样依赖即可。

## 7. 建议实施顺序（落地时）

1. MixinPlugin + AE2 compileOnly + 空 `PatternTerminalApi`。
2. `RecipeTreeCollector` + 把 `PatternizeInput` 接到收集器（只 log）。
3. 状态机 FILL + AE2 encode + MOVE。
4. EncodedItems + ReloadMemory。
5. 空白样板/背包检查 + 防关屏。
6. （可选）RS 1.x。
