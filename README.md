# 隐印 - Blind Watermark

一款基于 Android 的盲水印工具应用，使用 Kotlin + Jetpack Compose 构建。

## 功能

- **嵌入水印**：将不可见的文本水印嵌入到图片中
- **提取水印**：从带水印的图片中提取隐藏的文本信息
- **密码保护**：可选密码加密，增强水印安全性

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material 3
- **依赖注入**：Hilt
- **导航**：Navigation Compose
- **图像处理**：OpenCV
- **本地存储**：Room + DataStore
- **构建工具**：Gradle (Kotlin DSL)

## 系统要求

- Android 8.0 (API 26) 及以上
- 目标 SDK 34

## 项目结构

```
app/src/main/java/com/everett/blindwatermark/
├── BlindWatermarkApp.kt    # Application 类
├── MainActivity.kt         # 主 Activity
├── AppNavigation.kt        # 导航图
└── ui/
    ├── theme/
    │   ├── Color.kt         # 颜色定义
    │   └── Theme.kt         # 主题配置
    └── screens/
        ├── MainScreen.kt    # 主页面（Tab 切换）
        ├── EmbedScreen.kt   # 嵌入水印页面
        └── ExtractScreen.kt  # 提取水印页面
```

## 构建

```bash
./gradlew assembleRelease
```

## 版本

- 版本号：1.0.9
- 版本代码：10
