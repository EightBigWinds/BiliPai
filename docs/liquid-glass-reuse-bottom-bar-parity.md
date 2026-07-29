# 液态玻璃复用：底栏 / Dock / 指示器调研与 1:1 对齐规范

> 调研日期：2026-07-30  
> 权威实现以当前代码为准；历史文档 `docs/bottom-bar-liquid-glass-indicator.md` 中仍描述 Kyant-only / `LiquidIndicator` 主路径的部分已过时。

## 1. 开关与复用语义

| 概念 | 代码入口 | 行为 |
| --- | --- | --- |
| 全局复用总开关「安卓原生液态玻璃」 | `HomeSettings.androidNativeLiquidGlassEnabled` → `resolveGlobalLiquidGlassReuseEnabled` | 开启后，所有可复用 chrome 面强制走底栏液态材质 |
| 共享 chrome 是否启用 | `resolveSharedLiquidGlassChromeEnabled(individual, uiPreset, androidNative)` | 总开关 ON → 全开；OFF → 回退各面独立开关 + 预设门闩 |
| 分段控件强制液态 | `BottomBarLiquidSegmentedControl.forceLiquidChrome` / `AppSegmentedControl.forceLiquidIndicator` | 与总开关等价：`force \|\| androidNativeLiquidGlassEnabled` |

开启复用后的目标：**不自研第二套参数**。壳层、指示器形变、折射采样拓扑以首页悬浮底栏 `KernelSuAlignedBottomBar` 为唯一真相源。

## 2. 生产路径（底栏真相源）

### 2.1 入口

- UI 宿主：`FrostedBottomBar` → `KernelSuAlignedBottomBar`（`BottomBar.kt`）
- 运动规格：`BottomBarMotionProfile.ANDROID_NATIVE_FLOATING` via `resolveBottomBarMotionSpec`
- 拖拽内核：`rememberDampedDragAnimationState` + `horizontalDragGesture`（整条 dock 可拖，不限胶囊）
- 壳层材质：`kernelSuMiuixFloatingDockSurface`（Miuix `drawBackdrop`）
- 指示器：`KernelSuMiuixBottomBarIndicatorLayer`
- 隐藏染色导出：`miuixLayerBackdrop(tabsBackdrop)` + 固定 24dp capture lens
- 指示器采样：`rememberMiuixCombinedBackdrop(page, tabs)`（page 在前，tinted tabs 在后）

### 2.2 层叠顺序（z 从底到顶）

1. **Dock 壳** — vibrancy + blur(ExtraSmall) + lens(ExtraLarge)；按压时 shell 整体 bump scale  
2. **可见图标/文字** — 玻璃路径下选中色中性化（主题色只从指示器折射透出）  
3. **隐藏 export capture**（alpha=0）— monochrome glyphs + `ColorFilter.tint(theme)`，录制到 `tabsBackdrop`  
4. **指示器胶囊** — 采样 CombinedBackdrop，速度形变 + 拖拽放大  
5. **透明 hit / drag 层** — 手势与点击

### 2.3 上下滑动（列表纵向滚动）对液态玻璃的影响

底栏通过 `isFeedScrollInProgress` → 动画化的 `materialScrollProgress` 接入：

| 参数 | 值 |
| --- | --- |
| 滚动中 duration | `140ms`（`resolveBottomBarMaterialScrollAnimationDurationMillis(true)`） |
| 停稳 duration | `420ms` |
| easing | `AppMotionEasing.Continuity` |
| 材质消费 | `resolveBottomBarGlassMaterialSpec(..., scrollProgress)` |

滚动材质效果（Kyant 壳层链 / `kernelSuFloatingDockSurface`）：

- **BILIPAI_TUNED**：滚动不改变 blur/lens 配方（固定 4dp blur、24dp shell lens）
- **IOS26_REFINED**：`innerRimGlow.alpha` 在 `0.09 → 0.16` 随 `scrollLift` 缓动抬升

注意：

- 生产底栏 Miuix 壳层实现当前**接受** `materialScrollProgress` 但效果主路径固定 vibrancy/blur/lens；滚动材质抬升主要在 **Kyant matched surface**（顶部 Dock / 搜索胶囊 / 复用分段壳）上可见。
- 顶部 Dock 复用入口：`homeTopBottomBarMatchedSurface` → `kernelSuFloatingDockSurface`，必须透传 `isScrolling` / 动画化的 `materialScrollProgress`，否则上下滑时与底栏脱节。
- 旧式 `LiquidGlassTuning.scrollCoupledRefractionAmount` 仅用于非 matched 的 `homeTopChromeSurface` 折射/透明度，开启全局复用后 liquid 模式已改走 matched 底栏材质。

纵向滚动**不**驱动指示器左右形变；也不把列表 scroll 叠到 shell/capture 的缩放上。

### 2.4 左右滑动指示器形变

| 效果 | 公式 / 常量 | 代码 |
| --- | --- | --- |
| 拖拽放大 | 静止 `1.0` → 拖拽 `88/56` | `BOTTOM_BAR_INDICATOR_DRAG_SCALE_TARGET` |
| 放大动画 | 按下 90ms EaseOut / 抬起 220ms FastOutSlowIn | `rememberBottomBarIndicatorDragScaleProgress` |
| 速度形变 X | `baseScaleX / (1 - clamp(v*0.75, ±0.2))` | `resolveBottomBarIndicatorLayerTransform` |
| 速度形变 Y | `baseScaleY * (1 - clamp(v*0.25, ±0.2))` | 同上 |
| 速度归一 | `velocityItemsPerSecond / 10` | `KSU_INDICATOR_VELOCITY_NORMALIZATION_DIVISOR` |
| 面板位移 | `ExtraSmall * sign(offset/dockW) * EaseOut(|f|)` | 与 `resolveSharedLiquidIndicatorPanelOffsetPx` 一致 |
| 面板三分量 | 当前预设下 visible/export/indicator **同 offset** | `resolveBottomBarPresetPanelOffsets` |
| 指示器 lens | `10dp/14dp * pressProgress` | `resolveBottomBarBackdropPresetIndicatorLens` |
| Capture lens | 玻璃开启即 **固定 24dp**（ExtraLarge） | `shouldUseBottomBarCaptureLens` + capture 层 |
| 边缘过拉压缩 | shell `scaleX` 最多压 3.5% | `resolveBottomBarEdgeCompressionScaleX` |
| 点按脉冲 | click pulse / settle rebound 曲线 | `rememberBottomBarClickPulseTransform` 等 |
| 运动规格 | `ANDROID_NATIVE_FLOATING`：selection spring 0.68/520；deformation 0.40/0.54；scale spring 0.46/620 | `BottomBarMotionSpec.kt` |

`motionProgress = max(press, refractionProgress)`，其中 refraction 由分数位位置 + 速度 + drag floor 决定（`resolveBottomBarRefractionMotionProfile`）。

## 3. 顶部 Dock 与搜索

| 表面 | 复用入口 | 滚动耦合 | 左右指示器 |
| --- | --- | --- | --- |
| 顶部标签 Dock 壳 | `HomeTopTabChrome` → `homeTopBottomBarMatchedSurface` | 必须传 `isScrolling` + 与底栏相同的 scroll progress 动画 | 胶囊用 `KernelSuBottomBarIndicatorLayer` |
| 搜索胶囊 / 右侧按钮 | `HomeHeader` → `homeTopBottomBarMatchedSurface` | 同上 | 无横向指示器 |
| 顶部指示器 | `LightweightHomeTopTabs` | 不直接消费纵向 scroll | 速度形变 + 拖拽放大与底栏同一套；pager 位姿可驱动 velocity |

顶部指示器额外点：

- 运动规格必须 `resolveSegmentedControlMotionSpec()` ≡ `ANDROID_NATIVE_FLOATING`
- 采样：glass 开启时 `CombinedBackdrop(page, tabsCapture)` 作为 **contentBackdrop**（与底栏 Combined 语义一致）
- 无手指按压、仅 pager 滑动时，用 `resolveSharedLiquidIndicatorLensProgress` 补 lens 进度（底栏没有 pager，靠 drag press）

## 4. 开启复用后的组件清单

统一走 `BottomBarLiquidSegmentedControl` / 底栏 matched 壳 的面：

| 区域 | 文件 |
| --- | --- |
| 首页热门二级分类 | `HomeCategoryPage.kt` |
| 收藏 / 历史 / 通用列表筛选 | `CommonListScreen.kt` |
| 设置分段 | `AppSegmentedComponents.kt` + 各 Settings 屏 |
| 视频简介/评论、评论排序 | `VideoContentSection.kt`, `CommentSortFilterBar.kt` |
| 评论底输入条壳 | `BottomInputBar.kt` → `kernelSuFloatingDockSurface` |
| 空间 Tab | `SpaceScreen.kt` |
| 个人页 | `ProfileScreen.kt` |
| 直播分区 / 列表 / 播放器 | `LiveAreaScreen.kt`, `LiveListScreen.kt`, `LivePlayerScreen.kt` |
| 番剧筛选 / 我的追番 / 播放器 | `BangumiFilterComponents.kt`, `MyBangumiScreen.kt`, `BangumiPlayerContent.kt`, `BangumiScreen.kt` |
| 听视频 / 音乐播放器 | `ListenVideoScreen.kt`, `MusicPlayerContent.kt` |
| 今日待看插件 | `TodayWatchPlugin.kt` |
| 分区侧栏指示器 | `PartitionScreen.kt` → `KernelSuBottomBarIndicatorLayer`（`swapMotionAxes=true`） |
| 顶栏 Dock / 搜索 / 指示器 | `HomeTopTabChrome.kt`, `HomeHeader.kt`, `TopBar.kt` |

动态顶栏目前 **刻意不** 走液态复用（结构测试 `dynamic top tabs temporarily opt out`）。

## 5. 1:1 对齐硬规则（禁止自研）

对所有开启复用的组件：

1. **运动**：只用 `resolveSegmentedControlMotionSpec()` / `ANDROID_NATIVE_FLOATING`，禁止另写 spring、scale 系数。  
2. **指示器层**：只用 `KernelSuBottomBarIndicatorLayer` / `KernelSuMiuixBottomBarIndicatorLayer`，禁止新写 `LiquidIndicator` / 自绘渐变胶囊。  
3. **形变**：只用 `resolveBottomBarIndicatorLayerTransform` + `rememberBottomBarIndicatorDragScaleProgress`；`indicatorLayerScaleTransform = null`。  
4. **面板位移**：只用 `resolveSharedLiquidIndicatorPanelOffsetPx` + `resolveBottomBarPresetPanelOffsets`。  
5. **壳层**：只用 `kernelSuFloatingDockSurface` / `kernelSuMiuixFloatingDockSurface` / `homeTopBottomBarMatchedSurface`。  
6. **纵向滚动材质**：复用底栏 scroll progress 动画时长与 `resolveBottomBarGlassMaterialSpec`，禁止第二套 scroll lift。  
7. **主题色路径**：玻璃滑动时 visible 中性 + export monochrome + `SrcIn` tint，禁止手绘蓝粉渐变。  
8. **拓扑例外**（仅 HyperOS 安全）：分段控件禁止 `CombinedBackdrop(page, tabs)` 自采样（会炸 RenderThread）；指示器 `contentBackdrop = tabsCapture`、`backdrop = page`，由 `KernelSuBottomBarIndicatorLayer` 按预设取 content。底栏 Miuix 路径使用 `rememberMiuixCombinedBackdrop` 是唯一 Combined 真相源。  
9. **尺寸可灵活，渲染不可自研**：  
   - 浮底栏几何：`FloatingBottomBarSegmented* = 58/56`（仅真底栏）  
   - 内容区几何：`InContentLiquidSegmented* = 40/32`（评论/简介 Tab 等可按场景覆盖）  
   - 指示器 lens / 速度形变 / Combined 采样必须跟底栏同源；禁止自绘 solid 选中 pill  
   - `tapPressRefractionEnabled` 默认 `true`（与底栏 press 路径一致）  
10. **全局复用分段 = 底栏同一合成器**（`BottomBarLiquidSegmentedControl`）：  
    - 壳色：`resolveKernelSuBottomBarContainerColor`（半透白/深 0.4，禁止 cardContainer 自研灰）  
    - 指示器采样：`CombinedBackdrop(page, tabsExport)`（与 InstallerX / 底栏相同）  
    - 玻璃开启时可见字中性、主题色只经 export+胶囊透出（idle 也开）  
    - capture 固定 ExtraLarge lens + vibrancy/blur  
    - 页面优先 Miuix；Kyant 仅作无 Miuix 时的 fallback，仍用 Combined 拓扑  
11. **评论区等只接线，不自研**：  
    - `rememberMiuixLayerBackdrop()` + `miuixLayerBackdrop`  
    - `BottomBarLiquidSegmentedControl(miuixBackdrop = …)` / `kernelSuMiuixFloatingDockSurface`  

## 9. 后续扩组件与维护

推荐模式（最低维护成本）：

1. 滚动内容上挂 **一个** `rememberMiuixLayerBackdrop()`  
2. 分段只调 `BottomBarLiquidSegmentedControl(miuixBackdrop = page)`；壳用 `kernelSuMiuixFloatingDockSurface`  
3. **禁止**自研颜色/Combined/lens；几何用 `height`/`indicatorHeight` 灵活覆盖  
4. 一改 `BottomBarLiquidSegmentedControl` = 全局所有复用面一起更新

## 6. 曾发现的脱节点（对齐目标）

| 脱节 | 表现 | 对齐方式 |
| --- | --- | --- |
| 顶栏 matched dock 未传 `isScrolling` | 上下滑时顶栏玻璃不跟底栏材质 | 透传并动画化 materialScrollProgress |
| 搜索 / 右侧 matched 壳未传滚动 | 同上 | 同上 |
| 顶栏指示器 Combined 算了却塞进 `backdrop` | 组合采样被丢弃（预设总是优先 content） | Combined 作为 `contentBackdrop` |
| 分段壳未传 press/motion 到 shell | 按压 bump 与底栏不一致 | 传 `materialPressProgress` / `materialMotionProgress` |
| 分段 capture lens 按 progress 缩放 | 与底栏固定 24dp 不一致 | glass 开启时固定 ExtraLarge |

## 7. 验证建议

```bash
./gradlew :app:testDebugUnitTest --tests 'com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControlStructureTest' \
  --tests 'com.android.purebilibili.feature.home.components.BottomBarMiuixStructureTest' \
  --tests 'com.android.purebilibili.feature.home.components.BottomBarIndicatorPolicyTest' \
  --tests 'com.android.purebilibili.feature.home.components.HomeChromeLiquidSurfaceStructureTest' \
  --tests 'com.android.purebilibili.feature.home.components.HomeHeaderVisualPolicyTest'
```

真机回归：开启「安卓原生液态玻璃」后对比

1. 首页列表上下滑：底栏与顶部 Dock / 搜索胶囊材质是否同步  
2. 底栏左右拖：胶囊 88/56 放大 + 速度拉长压扁 + 松手弹簧  
3. 顶栏标签左右滑 / 拖：指示器形变与底栏同感  
4. 视频详情简介/评论、设置分段、直播分区、空间 Tab：同一套形变与折射  

## 8. 主文件索引

- `app/.../home/components/BottomBar.kt` — 底栏真相源  
- `app/.../home/components/BottomBarLiquidSegmentedControl.kt` — 分段复用控件  
- `app/.../home/components/BottomBarGlassMaterialPolicy.kt` — 滚动材质  
- `app/.../home/components/TopBar.kt` — 顶栏指示器 + matched surface  
- `app/.../home/components/HomeTopTabChrome.kt` / `HomeHeader.kt` — Dock / 搜索  
- `design-system/.../motion/BottomBarMotionSpec.kt` — 弹簧与形变常量  
- `app/.../core/store/HomeSettingsUiPresetPolicy.kt` — 复用总开关  
