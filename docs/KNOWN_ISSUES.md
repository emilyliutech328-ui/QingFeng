# 问题与待办记录

> 用法：新问题往下追加；改完移到「已解决」并注明 commit。
> 最后更新：2026-11-17

## 当前基线

| 项 | 状态 |
|---|---|
| 最新提交 | `034cbee`（玻璃 P0 修复），main 已同步远端 |
| 最新发版 | v1.0.2（2026-10-27）；v1.0.3 发布中（M3 引擎/dependabot/玻璃修复/web 白名单） |
| CI | 通过（最近一次 5m35s，含 M3 引擎） |
| 开放 PR / 分支 | 0 / 仅 main |

---

## 一、待处理

### P0 · 影响正常使用

*(暂无)*

### P1 · 品牌一致性（收尾）

#### 代码注释 / 颜色变量名仍写 KK
- **位置**：`values/colors.xml:5-8`（`kk_primary` / `kk_primary_dark` /
  `kk_accent` / `kk_accent_night`，**13 处引用**）
- **现状**：可见文案与注释已全部清零（`KK读书` 在 `app/src/main` 下 0 命中）；
  仅剩这组颜色变量名。
- **决策待定**：改名成本 13 处引用 + 主题 JSON 里的引用需同步，纯维护性收益低。
  要么一次改净（含 `themeConfig.json`），要么明确保留并在此记录不再处理。

### P2 · 交互与体验

*(书架热区重叠、订阅说明措辞两项已解决，见下方「已解决」)*

#### 翻页热区删除后的替代手势未验证
- 删除隐形热区后，翻书只剩「横向拖动」和「点其它书脊」。
- 需在真机确认：单击书脊区域切换是否顺手，虚线区是否会被误认为可翻页。

### P3 · 功能增强（可选）

#### 内置「轻小说文库」书源 `searchUrl` 为空
- **现象**：搜书时抛「搜索url不能为空」（`WebBook.kt:57`），每次搜索多一条错误项。
- **背景**：站点对未登录访客关闭了列表/搜索接口，这条源目前只能打开已知书页链接
  （详情/目录/正文正常）。
- **选项**：① 配置登录信息恢复站内搜索；② 从搜索范围过滤掉空 `searchUrl` 的源（改动小）。

#### 封面生成版式改良
- **状态**：设计已完成，见 `docs/DESIGN/03-封面生成设计.md`，未实施。
- **规划**：横排自适应 + 边框纹理 + 8 种图案（game-icons.net CC BY 3.0 + Lucide ISC，
  需转 VectorDrawable 并附 `assets/cover_motifs/LICENSE.md`）+ 20 个占位作者。
- **已决策**：不提取副标题、占位作者不加提示、保留旧竖排版式。

#### 玻璃拟态覆盖范围
- **现状**：只做了阅读菜单（`ReadMenu.kt:212/218`）。
- **待定**：是否铺到书架 / 发现页 / 对话框（需动大量布局）。
- **依赖**：建议先解决 P0 的子树模糊 bug，再谈扩展。

### P4 · 工程与环境

*(web 构建白名单已解决，见「已解决」区)*

### P5 · 发版

*(进行中：v1.0.3 正在发布，含 M3 引擎/dependabot/玻璃修复/web 白名单)*

---

## 二、已解决

### 立体书架底部虚线区与书底距离偏近
✅ commit `23d7c69`：`baseline` 由 `shelfBottom - 6dp` 改为 `- 13dp`，间距 14dp → 21dp。

### 选中书「缩回书架深处」，未浮出并遮挡邻书
✅ commit `23d7c69`，四个叠加原因全部修复：

| 原因 | 原实现 | 修复 |
|---|---|---|
| 上浮量被压到几乎为零 | `min(书脊*0.42, baseline-书脊*1.14-4dp)` → 仅 ~27dp | 书脊上限 0.78→0.70，上浮目标 50%，实测 26.9dp→61.2dp（2.3 倍） |
| 夹紧约束语义错误 | `maxLiftEdge = baseline-4dp` 禁止浮出书架 | 只保留「不超出控件顶（留 2dp）」 |
| 绘制顺序遮挡错误 | 从左到右，右邻压住中间 | 选中书最后画 |
| 横移与上提耦合 | 共用 `pull` | `shiftX = pull*0.30` |

### versionCode 恒为 20000，新版本无法覆盖安装
✅ commit `6241037`：改 `300_000 + yyMMddHH`，不依赖 git。实测 APK `versionCode=26402623`。

### 构建过慢（全量 11.5 分钟）
✅ Gradle 缓存迁至 `D:\gradle-home`（与项目同盘）+ 开 parallel/caching/incremental。
全量 11.5min → 冷启 2.3min → **增量 19.7s**。

### CI 无法运行（4 个独立原因）
✅ 已修复并连续通过：
1. `gradlew` 丢可执行位（Windows 不保留 mode）→ exit 126
2. KSP 版本未与 Kotlin 配对（2.3.4 vs 2.3.10）
3. `google{}` 的 `includeGroupByRegex("com\\.google.*")` 锁死 KSP 插件
4. 阿里云镜像返回 502，Gradle 遇 5xx 中止尝试后续仓库
详见 `docs/BUILD.md`。

### 仓库堆积 dependabot PR（两轮）
✅ 第一轮 25 个危险大版本 PR 已关闭；第二轮（2026-11-01 又冒 5 个，其中 3 个违反
「只收补丁」）暴露通配 ignore 在 version catalog 别名匹配下失效，
最终 commit `8937d05` 关闭全部自动 PR（`open-pull-requests-limit: 0`），依赖升级改人工验证。

### 桌面图标仍是「KK」字样
✅ commit `4b686ac`（v1.0.1）：新图标「翻开的书 + 三道上升风纹 + 金色书签」，
adaptive 三层 + 五档密度，内容自动测量收进 66dp 安全区（余量 8.6px），已从 APK 提取验证。

### 关于页 / 分享文案写死「KK读书」
✅ commit `4b686ac`（v1.0.1）：四处文案改为「轻风阅读」，连带修复 AboutActivity
品牌词高亮失效（`indexOf(app_name)` 现在能命中）。`app/src/main` 下 `KK读书` 已 0 命中。

### 立体书架底部：翻页热区与虚线区重叠
✅ v1.0.2（commit `8073732`）：右下角本未绘制「‹ ›」箭头，隐形热区与虚线框重叠 8dp。
已删除该热区，底部虚线区点击 = 打开当前书，翻书用拖动/点书脊。

### 订阅页使用说明与实际不符
✅ v1.0.2（commit `8073732`）：`rss_guide_body` 重写为与实际一致——
新增入口（管理页右上角菜单「添加订阅源」+ 三种导入）、
条目操作（开关/✎编辑/⋮置顶置底删除，无长按菜单）、
排查走编辑页「调试」（RSS 无校验）。保留已核实的 `group:` 过滤与星标收藏。

### 玻璃拟态把面板内容一起模糊（文字/按钮糊掉）
✅ commit `034cbee`（v1.0.3）：根因是 `setRenderEffect(createBlurEffect)` 模糊的是
「该 View 及其整个子树」而非背后内容。修复：去掉 RenderEffect，玻璃感改由
半透明底色透出 + 顶部高光渐变 + 1px 描边的纯视觉近似实现（全版本一致、零开销）；
`PerfHelper.glassBlurSupported()` 不再要求 12+/非低端；四语言设置文案同步去掉「模糊」。
截图模糊 / Android 15 窗口级模糊作为后续增强另议。

### web 模块 pnpm 12 构建白名单
✅ commit `2cceb98`（v1.0.3）：`pnpm-workspace.yaml` 用 `allowBuilds:` map 修复
`ERR_PNPM_IGNORED_BUILDS`（pnpm 10 的 `onlyBuiltDependencies` 写法在 12 已失效）；
package.json 无效字段已删（回到原状）；pnpm-lock.yaml 入库保证可复现。

### 莫奈取色升级为 Material You / M3 官方引擎
✅ commit `b654642`（CI 通过，未发版）：引入 material-color-utilities
（@ `4dbd965df6`，Apache-2.0，19 类）到 `lib/theme/m3/`；`Monet.kt` 改为官方流水线
（量化→打分→HCT→Scheme 27 token），token 映射进现有 ThemeStore 链路，
公开 API 签名不变；新增 `M3ColorTest` 7 项。方案与 Phase 2 路线见
`docs/DESIGN/04-M3动态取色引擎.md`。

---

## 三、已核实无误（不需要动）

- 书源实测可用：`/book/{aid}.htm`、`/novel/.../index.htm`、章节页均 200
- 内置书源导入链路完整：`App.kt` → `DefaultData.upVersion()` → `importBuiltinWenku8Source()`
- 权限名已同步 `io.qingfeng.reader.READ_WRITE`
- 书城删除后无任何悬空引用
- 莫奈取色走 `recreateActivities()` 重建，配色能生效
- 订阅说明面板折叠状态持久化正确
- Edge 协议算法与独立实现逐字符一致（`EdgeProtocolTest` 3 项）
- 书源规则在真实页面命中（`Wenku8SourceRuleTest` 4 项）
- M3 引擎 tone 档位/明暗差异/色相保留/量化打分/container 层级（`M3ColorTest` 7 项）
- 网页版 dev server 可运行（vite 5.4.21，端口 8080，HTTP 200）

## 四、环境备忘

- **网络代理**：git 全局配的 `127.0.0.1:10808` 已失效；当前可用 **FlClash `127.0.0.1:7890`**。
  推送：`git -c http.proxy=http://127.0.0.1:7890 push origin main`；
  gh CLI：`$env:HTTPS_PROXY="http://127.0.0.1:7890"`。
- **构建**：JDK 17 / Gradle 8.14.3（`D:\gradle-home`）/ Android SDK 36，
  详见 `docs/BUILD.md`。
- **签名**：keystore 在 `C:\Users\01\WorkBuddy\...\keystore\lanying-reader.jks`，
  GitHub Secrets 已配 4 项；⚠️ 务必离线备份密钥库。
