# 隐印 (Blind Watermark)

> 给图片嵌入隐形水印，保护你的图片版权

基于 DWT-DCT-SVD 算法，支持文本、图片、比特数组三种水印模式。嵌入的水印肉眼不可见，提取时无需原图。

## 功能

- **嵌入水印** — 将文本、图片或比特数组嵌入到图片中
- **提取水印** — 从带水印的图片中提取隐藏信息
- **批量处理** — 批量为多张图片嵌入统一水印
- **密码保护** — 可选密码加密，双重保护
- **历史记录** — 保存操作历史，方便回看

## 技术栈

- Kotlin + Jetpack Compose
- MVVM + Hilt + Room
- OpenCV Android SDK
- 自写 Haar 小波变换

## 设计文档

详见 [DESIGN_SPEC.md](DESIGN_SPEC.md)

## 开源参考

- [guofei9987/blind_watermark](https://github.com/guofei9987/blind_watermark) — 原始 Python 盲水印库

## License

MIT
