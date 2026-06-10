# 隐印 - 设计规格

## 概述

隐印是一款盲水印工具 App，允许用户在图片中嵌入和提取不可见的文本水印。

## 设计风格

采用 iOS 风格的简洁设计语言，支持亮色/暗色模式。

## 颜色系统

### 亮色模式
| 名称 | 色值 | 用途 |
|------|------|------|
| BackgroundLight | #F2F2F7 | 页面背景 |
| White | #FFFFFF | 卡片/表面 |
| AccentBlue | #007AFF | 主色调/按钮 |
| TextPrimary | #000000 | 主要文字 |
| TextSecondary | #8E8E93 | 次要文字 |
| SuccessGreen | #34C759 | 成功状态 |
| ErrorRed | #FF3B30 | 错误状态 |
| Separator | #E5E5EA | 分隔线 |

### 暗色模式
| 名称 | 色值 | 用途 |
|------|------|------|
| BackgroundDark | #000000 | 页面背景 |
| SurfaceDark | #1C1C1E | 卡片/表面 |
| AccentBlueDark | #0A84FF | 主色调/按钮 |
| SuccessGreenDark | #30D158 | 成功状态 |
| ErrorRedDark | #FF453A | 错误状态 |

## 页面结构

### 主页面 (MainScreen)
- 应用标题「隐印」+ 副标题
- Tab 切换器：嵌入 / 提取
- 内容区域根据 Tab 切换

### 嵌入页面 (EmbedScreen)
- 图片选择区域（卡片样式，圆角 20dp）
- 水印文本输入框
- 可折叠密码设置区域
- 嵌入按钮（全宽，圆角 12dp）

### 提取页面 (ExtractScreen)
- 图片选择区域
- 可折叠密码输入区域
- 提取按钮

## 圆角规范
- 页面卡片：20dp
- Tab 切换器：12dp
- Tab 选项：10dp
- 输入框：10dp
- 按钮：12dp

## 字体规范
- 应用标题：28sp Bold
- 副标题：14sp Regular
- Tab 文字：15sp SemiBold（选中）/ Normal（未选中）
- 区域标题：14sp Medium
- 按钮文字：16sp
- 提示文字：12sp
