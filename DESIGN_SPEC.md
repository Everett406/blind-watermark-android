# 隐印 — 原生 Android 盲水印工具

> 版本：v1.0-draft  
> 日期：2026-06-09  
> 状态：设计定稿

---

## 一、项目概述

### 1.1 背景

基于开源项目 [guofei9987/blind_watermark](https://github.com/guofei9987/blind_watermark)（13k Star），将其核心盲水印算法以 **纯原生 Android（Kotlin + Jetpack Compose）** 重写，打造一款简洁好用的移动端盲水印工具。

### 1.2 核心功能

- **嵌入水印**：将不可见水印嵌入图片（文本 / 图片 / 比特数组）
- **提取水印**：从带水印的图片中提取水印信息（无需原图）
- **批量处理**：支持批量嵌入水印
- **历史记录**：保存操作历史，方便回看

### 1.3 算法

**DWT-DCT-SVD**（离散小波变换 + 离散余弦变换 + 奇异值分解）

- 嵌入流程：BGR→YUV → DWT → 分块 → 加密打乱 → DCT → SVD（修改奇异值）→ 逆变换
- 提取流程：同嵌入前半部分 → 通过奇异值判断比特 → K-means 聚类确定阈值
- 鲁棒性：抗旋转、裁剪、遮挡、缩放、噪声、亮度调整

### 1.4 设计哲学

- **纯白极简**：界面干净，让图片成为视觉焦点
- **iOS 风格**：借鉴 iOS 设计语言，区别于 Material Design
- **专注工具**：减少一切干扰，快速完成操作

### 1.5 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose |
| 架构 | MVVM + Repository |
| 依赖注入 | Hilt |
| 本地数据库 | Room（SQLite）|
| 数据存储 | DataStore（Preferences）|
| 异步 | Kotlin Coroutines + Flow |
| 图片处理 | OpenCV Android SDK |
| 矩阵运算 | OpenCV Core（SVD、DCT）+ 自写 Haar 小波 |
| 导航 | Jetpack Navigation Compose |

---

## 二、设计风格规范

### 2.1 整体视觉方向

- **风格**：纯白极简，iOS 设计语言
- **拒绝**：Material You 动态取色、蓝紫渐变、花哨装饰
- **核心**：白底、黑字、蓝色强调、大圆角、充足留白

### 2.2 配色方案

#### 浅色模式（默认）

| 用途 | 色值 | 说明 |
|------|------|------|
| 背景 | `#F2F2F7` | iOS 系统灰 |
| 卡片/表面 | `#FFFFFF` | 纯白 |
| 主文字 | `#000000` | 纯黑 |
| 次要文字 | `#8E8E93` | iOS 系统灰 |
| 强调色 | `#007AFF` | iOS 系统蓝 |
| 成功 | `#34C759` | iOS 系统绿 |
| 错误 | `#FF3B30` | iOS 系统红 |
| 分隔线 | `#E5E5EA` | 极浅灰 |

#### 深色模式

| 用途 | 色值 | 说明 |
|------|------|------|
| 背景 | `#000000` | 纯黑 |
| 卡片/表面 | `#1C1C1E` | iOS 深色卡片 |
| 主文字 | `#FFFFFF` | 纯白 |
| 次要文字 | `#8E8E93` | 同浅色 |
| 强调色 | `#0A84FF` | iOS 深色模式蓝 |
| 成功 | `#30D158` | iOS 深色模式绿 |
| 错误 | `#FF453A` | iOS 深色模式红 |

### 2.3 字体

- 使用系统默认字体（Noto Sans SC / Roboto）
- 支持系统级字体大小调整

| 层级 | 大小 | 字重 | 用途 |
|------|------|------|------|
| 大标题 | 28sp | Bold | 页面标题 |
| 标题 | 20sp | Semibold | 卡片标题 |
| 正文 | 16sp | Regular | 输入内容、描述 |
| 辅助 | 14sp | Regular | 提示文字 |
| 微小 | 12sp | Regular | 标签 |

### 2.4 圆角与间距

| 元素 | 圆角 |
|------|------|
| 大卡片 | 20dp |
| 小卡片/按钮 | 12dp |
| 输入框 | 10dp |
| Tab 指示器 | 全圆角（胶囊形）|

- 页面边距：16dp
- 卡片内边距：16-20dp
- 卡片间距：12dp

---

## 三、页面结构与导航

### 3.1 整体布局

**单页 Tab 切换**，顶部 Tab 在「嵌入」和「提取」之间切换。

```
┌─────────────────────────┐
│                         │
│   隐印                  │  ← 大标题，28sp Bold
│   给图片嵌入隐形水印     │  ← 副标题，14sp 灰色
│                         │
├─────────────────────────┤
│  ┌──────┐  ┌──────┐    │
│  │ 嵌入  │  │ 提取  │    │  ← Tab 切换（胶囊形）
│  └──────┘  └──────┘    │
├─────────────────────────┤
│                         │
│  [图片预览区域]          │  ← 大圆角卡片
│                         │
│  水印内容               │
│  ┌───────────────────┐  │
│  │ @我的水印          │  │  ← 输入框
│  └───────────────────┘  │
│                         │
│  水印类型               │
│  ┌──────┐┌──────┐┌──────┐│
│  │ 文本  ││ 图片  ││ 比特  ││  ← 类型选择
│  └──────┘└──────┘└──────┘│
│                         │
│  密码（可选）            │
│  ┌───────────────────┐  │
│  │                   │  │  ← 可折叠，默认隐藏
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │    开始嵌入        │  │  ← 主按钮，蓝色
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

### 3.2 页面层级

```
主页（Tab 切换）
├── 嵌入 Tab
│   ├── 单张嵌入 → 预览 → 保存
│   └── 批量嵌入 → 预览列表 → 保存
├── 提取 Tab
│   └── 选择图片 → 自动检测 → 显示结果
├── 历史记录（从主页右上角进入）
│   └── 操作列表（时间倒序）
└── 设置（从主页右上角进入）
    ├── 主题（跟随系统 / 浅色 / 深色）
    ├── 关于隐印
    └── 清除所有数据
```

---

## 四、核心页面设计

### 4.1 嵌入 Tab

**布局**（从上到下）：

1. **图片选择区**：大圆角卡片，点击弹出底部选择（相册 / 拍照）
   - 未选择时：灰色占位 + 图标 + 「选择图片」文字
   - 已选择时：显示图片预览缩略图 + 文件名 + 图片尺寸
   - 批量模式：显示已选数量 + 缩略图网格

2. **水印内容输入**：
   - 文本模式：单行文本输入框
   - 图片模式：点击选择水印图片（二值/灰度图）
   - 比特模式：输入 0/1 序列（如 `101101`）

3. **水印类型选择**：三个胶囊按钮（文本 / 图片 / 比特）

4. **密码输入**（可折叠，默认隐藏）：
   - 展开/折叠动画
   - 数字输入框
   - 提示：「设置密码后，提取时需要输入相同密码」

5. **操作按钮**：「开始嵌入」（蓝色大按钮）

### 4.2 嵌入结果页

**布局**：

```
┌─────────────────────────┐
│  ← 嵌入结果              │
├─────────────────────────┤
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │   [处理后的图片]   │  │  ← 大预览
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  水印内容：@我的水印     │
│  图片尺寸：1920x1080     │
│  处理耗时：1.2s          │
│                         │
│  ┌───────────────────┐  │
│  │    保存到相册      │  │  ← 主按钮
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │    分享            │  │  ← 次要按钮
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

### 4.3 提取 Tab

**布局**：

1. **图片选择区**：同嵌入 Tab
2. **密码输入**（可折叠，默认隐藏）
3. **操作按钮**：「开始提取」

### 4.4 提取结果页

**布局**：

```
┌─────────────────────────┐
│  ← 提取结果              │
├─────────────────────────┤
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │   [带水印的图片]   │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  提取成功               │
│                         │
│  ┌───────────────────┐  │
│  │  @我的水印         │  │  ← 提取结果（大字）
│  └───────────────────┘  │
│                         │
│  水印类型：文本          │
│  水印长度：6 字符        │
│                         │
│  ┌───────────────────┐  │
│  │    复制文本        │  │
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

### 4.5 历史记录页

**布局**：iOS 风格分组列表

| 字段 | 说明 |
|------|------|
| 时间 | 操作时间 |
| 类型 | 嵌入 / 提取 |
| 水印内容 | 文本预览或图片缩略图 |
| 图片缩略图 | 小图预览 |

点击可查看详情（图片 + 水印内容 + 密码）。

### 4.6 设置页

**布局**：iOS 风格分组列表

| 分组 | 设置项 |
|------|--------|
| 外观 | 主题（跟随系统 / 浅色 / 深色）|
| 关于 | 版本号（只读）|
| 关于 | 开源项目（跳转 GitHub）|
| 危险 | 清除所有数据（二次确认）|

---

## 五、交互与动效

### 5.1 页面转场

| 场景 | 动画 |
|------|------|
| Tab 切换 | 内容淡入淡出 + 轻微滑动 |
| 进入结果页 | 从右向左推入 |
| 返回 | 从左向右推出 |
| 底部弹窗（选择图片） | 从底部滑入 + 遮罩淡入 |

### 5.2 核心动效

| 场景 | 动画 |
|------|------|
| 选择图片 | 图片从缩略图放大到预览区（共享元素过渡）|
| 处理中 | 圆形进度指示器 + 按钮文字变为「处理中...」|
| 嵌入完成 | 图片从模糊恢复清晰 + 成功图标动画 |
| 提取成功 | 文字逐字打出效果（Typewriter）|
| 提取失败 | 震动 + 红色提示 |
| 按钮点击 | 缩放至 0.95，松开后恢复 |
| 密码区域展开/折叠 | 平滑高度动画 |

---

## 六、数据模型

### 6.1 Room 数据库

```kotlin
// 操作记录表
@Entity
data class HistoryRecord(
    @PrimaryKey val id: String,
    val type: OperationType,  // EMBED, EXTRACT
    val watermarkType: WatermarkType,  // TEXT, IMAGE, BIT
    val watermarkContent: String,  // 文本内容 / 图片路径 / 比特序列
    val password: Int?,  // 密码（可为空）
    val imagePath: String,  // 原图路径
    val resultPath: String?,  // 结果图路径（嵌入时）
    val imageWidth: Int,
    val imageHeight: Int,
    val processingTime: Long,  // 处理耗时（毫秒）
    val createdAt: Long
)
```

### 6.2 DataStore

| 键 | 类型 | 说明 |
|---|------|------|
| `theme` | enum | 跟随系统 / 浅色 / 深色 |
| `default_watermark` | string | 默认水印文本 |
| `default_password` | int? | 默认密码（可为空）|

---

## 七、算法移植方案

### 7.1 依赖替代

| Python 原始依赖 | Android 替代方案 |
|-----------------|----------------|
| numpy | OpenCV Core（Mat 操作、SVD）|
| opencv-python | OpenCV Android SDK |
| PyWavelets (dwt2) | 自写 Haar 小波变换（~50 行 Kotlin）|

### 7.2 Haar 小波实现

Haar 小波变换非常简单，核心就是相邻元素的平均和差值：

```kotlin
// 一维 Haar 小波
fun haar1D(input: DoubleArray): Pair<DoubleArray, DoubleArray> {
    val n = input.size
    val approx = DoubleArray(n / 2)
    val detail = DoubleArray(n / 2)
    for (i in 0 until n / 2) {
        approx[i] = (input[2 * i] + input[2 * i + 1]) / 2.0
        detail[i] = (input[2 * i] - input[2 * i + 1]) / 2.0
    }
    return Pair(approx, detail)
}

// 二维 Haar 小波（dwt2）
fun dwt2(input: Mat): Array<Mat> {
    // 先对行做 Haar，再对列做 Haar
    // 返回 [ca, ch, cv, cd] 四个子带
}
```

### 7.3 核心流程

```
嵌入：
1. BGR → YUV（OpenCV cvtColor）
2. DWT（自写 Haar）
3. 分块 4x4
4. 随机打乱（Kotlin Random，相同种子）
5. DCT（OpenCV Core.dct）
6. SVD（OpenCV Core.SVD）
7. 修改奇异值嵌入水印比特
8. 逆 SVD → 逆 DCT → 逆打乱 → 逆 DWT → YUV → BGR

提取：
1. 同嵌入步骤 1-6
2. 通过 s[0] % d1 > d1/2 判断比特
3. 三通道平均 + K-means 聚类
```

---

## 八、功能规格

### 8.1 MVP 功能

- [ ] 文本水印嵌入
- [ ] 文本水印提取（自动检测水印长度）
- [ ] 图片水印嵌入
- [ ] 图片水印提取
- [ ] 比特数组水印嵌入/提取
- [ ] 密码加密（可选）
- [ ] 相册选择 + 拍照
- [ ] 预览后手动保存到相册
- [ ] 批量嵌入（统一水印和密码）
- [ ] 历史记录
- [ ] 主题切换（浅色/深色）

### 8.2 进阶功能（后续迭代）

- [ ] 图片压缩选项
- [ ] 分享功能
- [ ] 水印强度调节（d1/d2 参数）
- [ ] 对比预览（左右滑动对比原图和水印图）
- [ ] 数据导出/备份

---

## 九、开发计划

### Phase 1：基础框架（Week 1）

- [ ] 创建 Android 项目（Kotlin + Compose）
- [ ] 配置依赖（Hilt、Room、Navigation、OpenCV）
- [ ] 搭建 MVVM 架构
- [ ] 配置主题（浅色/深色）
- [ ] 实现主页 Tab 切换布局

### Phase 2：算法移植（Week 1-2）

- [ ] 实现 Haar 小波变换
- [ ] 移植 DWT-DCT-SVD 嵌入算法
- [ ] 移植提取算法（含 K-means）
- [ ] 单元测试验证算法正确性

### Phase 3：核心页面（Week 2-3）

- [ ] 嵌入 Tab 完整流程
- [ ] 提取 Tab 完整流程
- [ ] 图片选择（相册 + 拍照）
- [ ] 结果预览页
- [ ] 保存到相册
- [ ] 批量嵌入

### Phase 4：辅助功能（Week 3）

- [ ] 历史记录
- [ ] 设置页
- [ ] 主题切换
- [ ] 引导页（首次启动）

### Phase 5：构建与发布（Week 4）

- [ ] GitHub Actions 自动构建 APK
- [ ] Logo 和图标设计
- [ ] 测试与修复
- [ ] 发布首个 Release

---

## 十、构建与发布

### 10.1 GitHub Actions 工作流

```yaml
# .github/workflows/build.yml
name: Build APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Release APK
        run: ./gradlew assembleRelease

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: app/build/outputs/apk/release/app-release.apk

  release:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Sleep for build verification
        run: sleep 30

      - name: Download APK artifact
        uses: actions/download-artifact@v4
        with:
          name: app-release

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: app-release.apk
```

### 10.2 构建规则

- 打 tag 触发构建（如 `v1.0.0`）
- 构建完成后 sleep 30 秒再创建 Release
- 使用 GitHub MCP 或 access token 查询构建状态

### 10.3 签名配置

- Release APK 使用 keystore 签名
- keystore 文件和密码通过 GitHub Secrets 存储
- 不将 keystore 提交到仓库

---

## 十一、确认事项汇总

| 类别 | 事项 | 结论 |
|------|------|------|
| **App** | 名称 | 隐印 |
| **风格** | 视觉方向 | 纯白极简，iOS 风格 |
| **布局** | 导航方式 | 单页 Tab 切换（嵌入/提取）|
| **图标** | 图标风格 | 矢量图标，不用 Emoji |
| **图片** | 选择方式 | 相册 + 拍照 |
| **水印** | 支持类型 | 文本 / 图片 / 比特数组 |
| **密码** | 是否必填 | 可选，非必填 |
| **保存** | 保存位置 | 保存到相册（手动保存）|
| **批量** | 批量嵌入 | 支持，统一水印和密码 |
| **提取** | 水印长度 | 自动检测 |
| **压缩** | 图片压缩 | MVP 不包含，后续迭代 |
| **历史** | 操作记录 | 支持 |
| **主题** | 深色模式 | 支持（跟随系统/浅色/深色）|
| **技术** | 最低版本 | Android 8.0（API 26）|
| **技术** | 屏幕适配 | 仅手机，锁定竖屏 |

---

## 十二、参考资源

- [guofei9987/blind_watermark](https://github.com/guofei9987/blind_watermark) — 原始 Python 项目
- [iOS Design Guidelines](https://developer.apple.com/design/human-interface-guidelines) — iOS 设计规范
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose) — Compose 官方文档
- [OpenCV Android](https://docs.opencv.org/master/d9/dfb/android_new_installguide.html) — OpenCV Android SDK

---

> **文档状态**：v1.0 定稿，所有讨论事项已确认。
